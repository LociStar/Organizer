package com.locibot.locibot.command.game.russianroulette;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.LociBotUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class RussianRouletteCmd extends BaseCmd {

    private final Map<Tuple2<Snowflake, Snowflake>, RussianRoulettePlayer> players;

    public RussianRouletteCmd() {
        super(CommandCategory.GAME, "russian_roulette", "Play russian roulette");
        this.setGameRateLimiter();

        this.players = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<?> execute(Context context) {
        final RussianRoulettePlayer player = this.getPlayer(context.getGuildId(), context.getAuthorId());
        return LociBotUtil.requireValidBet(context.getLocale(), context.getGuildId(), context.getAuthorId(), Constants.PAID_COST)
                .then(Mono.defer(() -> {
                    if (!player.isAlive()) {
                        return context.createFollowupMessage(Emoji.BROKEN_HEART, context.localize("russianroulette.already.dead")
                                .formatted(FormatUtil.formatDurationWords(context.getLocale(), player.getResetDuration())))
                                .then(Mono.empty());
                    }

                    player.fire();

                    final StringBuilder descBuilder = new StringBuilder(context.localize("russianroulette.pull"));
                    if (player.isAlive()) {
                        final long coins = (long) ThreadLocalRandom.current()
                                .nextInt(Constants.MIN_GAINS, Constants.MAX_GAINS + 1) * player.getRemaining();

                        descBuilder.append(context.localize("russianroulette.win")
                                .formatted(context.localize(coins)));

                        Telemetry.RUSSIAN_ROULETTE_SUMMARY.labels("win").observe(coins);
                        return player.win(coins)
                                .thenReturn(descBuilder);
                    } else {
                        descBuilder.append(context.localize("russianroulette.lose"));

                        Telemetry.RUSSIAN_ROULETTE_SUMMARY.labels("loss").observe(player.getBet());
                        return player.bet()
                                .thenReturn(descBuilder);
                    }
                }))
                .map(StringBuilder::toString)
                .map(description -> LociBotUtil.getDefaultEmbed(
                        EmbedCreateSpec.builder().author(EmbedCreateFields.Author.of(context.localize("russianroulette.title"),
                                null, context.getAuthorAvatar()))
                                .addFields(EmbedCreateFields.Field.of(context.localize("russianroulette.tries"),
                                        "%d/6".formatted(player.getRemaining()), false))
                                .description(description).build()))
                .flatMap(context::createFollowupMessage);
    }

    private RussianRoulettePlayer getPlayer(Snowflake guildId, Snowflake userId) {
        return this.players.computeIfAbsent(Tuples.of(guildId, userId), TupleUtils.function(RussianRoulettePlayer::new));
    }

}
