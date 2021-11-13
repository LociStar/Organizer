package com.locibot.locibot.command.game.roulette;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.core.game.GameCmd;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.NumberUtil;
import com.locibot.locibot.utils.LociBotUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import reactor.core.publisher.Mono;

public class RouletteCmd extends GameCmd<RouletteGame> {

    public RouletteCmd() {
        super("roulette", "Start or join a Roulette game");
        this.addOption(option -> option.name("bet")
                .description("Your bet")
                .required(true)
                .type(ApplicationCommandOption.Type.INTEGER.getValue()));
        this.addOption(option -> option.name("place")
                .description("The place of your bet")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .choices(DiscordUtil.toOptions(Place.class)));
        this.addOption(option -> option.name("number")
                .description("The number you're betting on, if the place chosen is 'number'")
                .required(false)
                .type(ApplicationCommandOption.Type.INTEGER.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final long bet = context.getOptionAsLong("bet").orElseThrow();
        final Place place = context.getOptionAsEnum(Place.class, "place").orElseThrow();
        final Long number = context.getOptionAsLong("number").orElse(null);

        return LociBotUtil.requireValidBet(context.getLocale(), context.getGuildId(), context.getAuthorId(), bet)
                .flatMap(__ -> {
                    if (place == Place.NUMBER && (number == null || !NumberUtil.isBetween(number, 1, 36))) {
                        return Mono.error(new CommandException(context.localize("roulette.invalid.number")));
                    }

                    final RoulettePlayer player = new RoulettePlayer(context.getGuildId(), context.getAuthorId(),
                            context.getAuthorName(), bet, place, number);

                    if (this.isGameStarted(context.getChannelId())) {
                        final RouletteGame game = this.getGame(context.getChannelId());
                        if (game.addPlayerIfAbsent(player)) {
                            return player.bet()
                                    .then(game.show())
                                    .doOnError(err -> game.destroy());
                        }
                        return Mono.error(new CommandException(context.localize("roulette.already.participating")));
                    } else {
                        final RouletteGame game = new RouletteGame(context);
                        game.addPlayerIfAbsent(player);
                        this.addGame(context.getChannelId(), game);
                        return game.start()
                                .then(player.bet())
                                .then(game.show())
                                .doOnError(err -> game.destroy());
                    }
                });
    }

    public enum Place {
        NUMBER, RED, BLACK, ODD, EVEN, LOW, HIGH
    }
}
