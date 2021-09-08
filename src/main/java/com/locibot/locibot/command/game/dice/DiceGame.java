package com.locibot.locibot.command.game.dice;

import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.core.game.MultiplayerGame;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.LociBotUtil;
import com.locibot.locibot.utils.TimeUtil;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DiceGame extends MultiplayerGame<DicePlayer> {

    private final long bet;
    private Instant startTimer;

    public DiceGame(Context context, long bet) {
        super(context, Duration.ofSeconds(30));
        this.bet = bet;
    }

    @Override
    public Mono<Void> start() {
        return Mono.fromRunnable(() -> {
            this.schedule(this.end());
            this.startTimer = Instant.now();
        });
    }

    @Override
    public Mono<Void> end() {
        final int winningNum = ThreadLocalRandom.current().nextInt(1, 7);
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> {
                    if (player.getNumber() == winningNum) {
                        final long gains = Math.min((long) (this.bet * (this.getPlayers().size() + Constants.WIN_MULTIPLICATOR)),
                                Config.MAX_COINS);
                        Telemetry.DICE_SUMMARY.labels("win").observe(gains);
                        return player.win(gains)
                                .thenReturn(this.context.localize("dice.player.gains")
                                        .formatted(player.getUsername().orElseThrow(), this.context.localize(gains)));
                    } else {
                        Telemetry.DICE_SUMMARY.labels("loss").observe(this.bet);
                        return Mono.just(this.context.localize("dice.player.losses")
                                .formatted(player.getUsername().orElseThrow(), this.context.localize(this.bet)));
                    }
                })
                .collectList()
                .map(results -> String.join("\n", results))
                .flatMap(text -> this.context.createFollowupMessage(Emoji.DICE, this.context.localize("dice.results")
                        .formatted(winningNum, text)))
                .then(Mono.fromRunnable(this::destroy));
    }

    @Override
    public Mono<Message> show() {
        return Mono.
                fromCallable(() -> {
                    EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                            .author(EmbedCreateFields.Author.of(this.context.localize("dice.title"), null, this.getContext().getAuthorAvatar()))
                            .thumbnail("https://i.imgur.com/XgOilIW.png")
                            .description(this.context.localize("dice.description")
                                    .formatted(this.context.getCommandName(), this.context.getSubCommandGroupName().orElseThrow(),
                                            DiceCmd.JOIN_SUB_COMMAND, this.context.localize(this.bet)))
                            .fields(List.of(
                                    EmbedCreateFields.Field.of(this.context.localize("dice.player.title"), FormatUtil.format(this.players.values(),
                                            player -> player.getUsername().orElseThrow(), "\n"), true),
                                    EmbedCreateFields.Field.of(this.context.localize("dice.number.title"), FormatUtil.format(this.players.values(),
                                            player -> Integer.toString(player.getNumber()), "\n"), true)));

                    if (this.isScheduled()) {
                        final Duration remainingDuration = this.getDuration().minus(TimeUtil.elapsed(this.startTimer));
                        embed.footer(EmbedCreateFields.Footer.of(this.context.localize("dice.footer.remaining")
                                .formatted(remainingDuration.toSeconds()), null));
                    } else {
                        embed.footer(EmbedCreateFields.Footer.of(this.context.localize("dice.footer.finished"), null));
                    }
                    return LociBotUtil.getDefaultEmbed(embed.build());
                })
                .flatMap(this.context::editFollowupMessage);
    }

    public long getBet() {
        return this.bet;
    }

}
