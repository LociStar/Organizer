package com.locibot.locibot.command.game.lottery;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.core.i18n.I18nManager;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.lottery.entity.LotteryGambler;
import com.locibot.locibot.database.lottery.entity.LotteryHistoric;
import com.locibot.locibot.database.users.entity.achievement.Achievement;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.NumberUtil;
import com.locibot.locibot.utils.TimeUtil;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static com.locibot.locibot.LociBot.DEFAULT_LOGGER;

public class LotteryCmd extends BaseCmd {

    public LotteryCmd() {
        super(CommandCategory.GAME, "lottery",
                "Buy a ticket for the lottery or display the current lottery status");
        this.addOption("number", "The number you bet on", false,
                ApplicationCommandOption.Type.INTEGER);
    }

    private static Mono<Message> show(Context context) {
        final Mono<List<LotteryGambler>> getGamblers = DatabaseManager.getLottery()
                .getGamblers()
                .collectList();

        final Mono<Long> getJackpot = DatabaseManager.getLottery()
                .getJackpot();

        final Mono<Optional<LotteryHistoric>> getHistoric = DatabaseManager.getLottery()
                .getHistoric()
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());

        return Mono.zip(getGamblers, getJackpot, getHistoric)
                .map(TupleUtils.function((gamblers, jackpot, historic) -> {
                    final LotteryEmbedBuilder builder = LotteryEmbedBuilder.create(context)
                            .withGamblers(gamblers)
                            .withJackpot(jackpot);

                    return historic.map(builder::withHistoric)
                            .orElse(builder)
                            .build();
                }))
                .flatMap(context::createFollowupMessage);
    }

    public static Duration getDelay() {
        ZonedDateTime nextDate = ZonedDateTime.now()
                .with(DayOfWeek.SUNDAY)
                .withHour(12)
                .withMinute(0)
                .withSecond(0);
        if (nextDate.isBefore(ZonedDateTime.now())) {
            nextDate = nextDate.plusWeeks(1);
        }

        return TimeUtil.elapsed(nextDate.toInstant());
    }

    public static Mono<Void> draw(GatewayDiscordClient gateway) {
        DEFAULT_LOGGER.info("Lottery draw started...");
        final int winningNum = ThreadLocalRandom.current().nextInt(Constants.MIN_NUM, Constants.MAX_NUM + 1);

        return DatabaseManager.getLottery()
                .getGamblers()
                .filter(gambler -> gambler.getNumber() == winningNum)
                .flatMap(winner -> gateway.getMemberById(winner.getGuildId(), winner.getUserId()))
                .collectList()
                .zipWith(DatabaseManager.getLottery().getJackpot())
                .flatMap(TupleUtils.function((winners, jackpot) -> {
                    DEFAULT_LOGGER.info("Lottery draw done (Winning number: {} | {} winner(s) | Prize pool: {})",
                            winningNum, winners.size(), jackpot);

                    final long coins = winners.isEmpty() ? 0 : Math.min(jackpot / winners.size(), Config.MAX_COINS);

                    return Flux.fromIterable(winners)
                            .flatMap(member -> DatabaseManager.getGuilds()
                                    .getDBMember(member.getGuildId(), member.getId())
                                    .flatMap(dbMember -> dbMember.addCoins(coins))
                                    .and(DatabaseManager.getUsers()
                                            .getDBUser(member.getId())
                                            .flatMap(dbUser -> dbUser.unlockAchievement(Achievement.BINGO)))
                                    .then(member.getPrivateChannel()))
                            .cast(MessageChannel.class)
                            .flatMap(privateChannel -> DiscordUtil.sendMessage(
                                    I18nManager.localize(Config.DEFAULT_LOCALE, "lottery.private.message")
                                            .formatted(coins), privateChannel))
                            .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()),
                                    err -> Mono.empty())
                            .then(new LotteryHistoric(jackpot, winners.size(), winningNum).insert())
                            .then(DatabaseManager.getLottery().resetGamblers())
                            .then(Mono.defer(() -> {
                                if (winners.isEmpty()) {
                                    return Mono.empty();
                                }
                                return DatabaseManager.getLottery().resetJackpot();
                            }));
                }))
                .then();
    }

    @Override
    public Mono<?> execute(Context context) {
        final Optional<Long> numberOpt = context.getOptionAsLong("number");
        if (numberOpt.isEmpty()) {
            return LotteryCmd.show(context);
        }

        return DatabaseManager.getGuilds()
                .getDBMember(context.getGuildId(), context.getAuthorId())
                .filterWhen(dbMember -> BooleanUtils.not(DatabaseManager.getLottery().isGambler(dbMember.getId())))
                .switchIfEmpty(Mono.error(new CommandException(context.localize("lottery.already.participating"))))
                .flatMap(dbMember -> {
                    if (dbMember.getCoins() < Constants.PAID_COST) {
                        return Mono.error(new CommandException(context.localize("not.enough.coins")));
                    }

                    final long number = numberOpt.orElseThrow();
                    if (!NumberUtil.isBetween(number, Constants.MIN_NUM, Constants.MAX_NUM)) {
                        return Mono.error(new CommandException(
                                context.localize("lottery.invalid.number")
                                        .formatted(number, Constants.MIN_NUM, Constants.MAX_NUM)));
                    }

                    return dbMember.addCoins(-Constants.PAID_COST)
                            .thenReturn(new LotteryGambler(context.getGuildId(), context.getAuthorId(), (int) number))
                            .flatMap(LotteryGambler::insert)
                            .then(context.createFollowupMessage(Emoji.TICKET, context.localize("lottery.message")
                                    .formatted(number, FormatUtil.formatDurationWords(context.getLocale(), LotteryCmd.getDelay()))));
                });
    }

}
