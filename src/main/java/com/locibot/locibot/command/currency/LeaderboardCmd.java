package com.locibot.locibot.command.currency;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.guilds.entity.DBMember;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.ShadbotUtil;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Comparator;

public class LeaderboardCmd extends BaseCmd {

    private static final int USER_COUNT = 10;

    public LeaderboardCmd() {
        super(CommandCategory.CURRENCY, "leaderboard", "Show coins leaderboard for this server");
    }

    @Override
    public Mono<?> execute(Context context) {
        return Flux.fromIterable(context.getDbGuild().getMembers())
                .filter(dbMember -> dbMember.getCoins() > 0)
                .sort(Comparator.comparingLong(DBMember::getCoins).reversed())
                .take(USER_COUNT)
                .flatMapSequential(dbMember -> Mono.zip(
                        context.getClient().getUserById(dbMember.getId()).map(User::getUsername),
                        Mono.just(dbMember.getCoins())))
                .collectList()
                .map(list -> {
                    if (list.isEmpty()) {
                        return context.localize("leaderboard.empty");
                    }
                    return FormatUtil.numberedList(USER_COUNT, list.size(),
                            count -> {
                                final Tuple2<String, Long> tuple = list.get(count - 1);
                                return context.localize("leaderboard.line")
                                        .formatted(count, tuple.getT1(), context.localize(tuple.getT2()));
                            });
                })
                .map(description -> ShadbotUtil.getDefaultEmbed(
                        EmbedCreateSpec.builder().author(EmbedCreateFields.Author.of(context.localize("leaderboard.title"), null, context.getAuthorAvatar()))
                                .description(description).build()))
                .flatMap(context::createFollowupMessage);
    }

}
