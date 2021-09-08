package com.locibot.locibot.command.game.lottery;

import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.lottery.entity.LotteryGambler;
import com.locibot.locibot.database.lottery.entity.LotteryHistoric;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.LociBotUtil;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.List;

public class LotteryEmbedBuilder {

    private final Context context;
    private EmbedCreateSpec embed;

    private LotteryEmbedBuilder(Context context) {
        this.context = context;
        this.embed = LociBotUtil.getDefaultEmbed(
                EmbedCreateSpec.builder().author(EmbedCreateFields.Author.of(context.localize("lottery.embed.title"),
                        null, context.getAuthorAvatar()))
                        .thumbnail("https://i.imgur.com/peLGtkS.png")
                        .description(context.localize("lottery.embed.description")
                                .formatted(FormatUtil.formatDurationWords(context.getLocale(), LotteryCmd.getDelay()),
                                        context.getCommandName(), Constants.MIN_NUM, Constants.MAX_NUM)).build());
    }

    public static LotteryEmbedBuilder create(Context context) {
        return new LotteryEmbedBuilder(context);
    }

    public LotteryEmbedBuilder withGamblers(List<LotteryGambler> gamblers) {
        this.embed = this.embed.withFields(EmbedCreateFields.Field.of(this.context.localize("lottery.embed.participants"),
                this.context.localize(gamblers.size()), false));

        gamblers.stream()
                .filter(lotteryGambler -> lotteryGambler.getUserId().equals(this.context.getAuthorId()))
                .findFirst()
                .ifPresent(gambler -> this.embed = this.embed.withFooter(EmbedCreateFields.Footer.of(this.context.localize("lottery.embed.bet")
                                .formatted(gambler.getNumber()),
                        "https://i.imgur.com/btJAaAt.png")));

        return this;
    }

    public LotteryEmbedBuilder withJackpot(long jackpot) {
        this.embed = this.embed.withFields(EmbedCreateFields.Field.of(this.context.localize("lottery.embed.pool.title"),
                this.context.localize("lottery.embed.pool.coins")
                        .formatted(this.context.localize(jackpot)), false));
        return this;
    }

    public LotteryEmbedBuilder withHistoric(LotteryHistoric historic) {
        final String people = switch (historic.getWinnerCount()) {
            case 0 -> this.context.localize("lottery.nobody");
            case 1 -> this.context.localize("lottery.one.person");
            default -> this.context.localize("lottery.people")
                    .formatted(this.context.localize(historic.getWinnerCount()));
        };

        this.embed = this.embed.withFields(EmbedCreateFields.Field.of(this.context.localize("lottery.embed.historic.title"),
                this.context.localize("lottery.embed.historic.description")
                        .formatted(this.context.localize(historic.getJackpot()), historic.getNumber(), people),
                false));
        return this;
    }

    public EmbedCreateSpec build() {
        return this.embed;
    }

}
