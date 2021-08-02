package com.locibot.locibot.command.donator;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.premium.RelicType;
import com.locibot.locibot.database.premium.entity.Relic;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.ShadbotUtil;
import com.locibot.locibot.utils.TimeUtil;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import discord4j.discordjson.possible.Possible;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class RelicStatusCmd extends BaseCmd {

    public RelicStatusCmd() {
        super(CommandCategory.DONATOR, "relic_status", "Your donator status");
    }

    @Override
    public Mono<?> execute(Context context) {
        return DatabaseManager.getPremium()
                .getUserRelics(context.getAuthorId())
                .flatMap(relic -> RelicStatusCmd.getRelicAndGuild(context, relic))
                .map(TupleUtils.function((relic, optGuild) -> {
                    final StringBuilder descBuilder = new StringBuilder(context.localize("relicstatus.id")
                            .formatted(relic.getId()));

                    optGuild.ifPresent(guild ->
                            descBuilder.append(context.localize("relicstatus.guild")
                                    .formatted(guild.getName(), guild.getId().asLong())));

                    descBuilder.append(context.localize("relicstatus.duration")
                            .formatted(FormatUtil.formatDurationWords(context.getLocale(), relic.getDuration())));

                    if (!relic.isExpired()) {
                        relic.getActivation()
                                .ifPresent(activation -> {
                                    final Duration durationLeft = relic.getDuration()
                                            .minus(TimeUtil.elapsed(activation.toEpochMilli()));
                                    descBuilder.append(context.localize("relicstatus.expiration")
                                            .formatted(FormatUtil.formatDurationWords(context.getLocale(), durationLeft)));
                                });
                    }

                    final StringBuilder titleBuilder = new StringBuilder();
                    if (relic.getType() == RelicType.GUILD) {
                        titleBuilder.append(context.localize("relicstatus.legendary.relic"));
                    } else {
                        titleBuilder.append(context.localize("relicstatus.relic"));
                    }
                    titleBuilder.append(' ');
                    if (relic.isExpired()) {
                        titleBuilder.append(context.localize("relicstatus.expired"));
                    } else {
                        titleBuilder.append(context.localize("relicstatus.activated"));
                    }

                    return ImmutableEmbedFieldData.of(titleBuilder.toString(), descBuilder.toString(), Possible.of(false));
                }))
                .collectList()
                .filter(Predicate.not(List::isEmpty))
                .map(fields -> ShadbotUtil.getDefaultEmbed(
                        embed -> {
                            embed.withAuthor(EmbedCreateFields.Author.of(context.localize("relicstatus.title"), null, context.getAuthorAvatar()))
                                    .withThumbnail("https://i.imgur.com/R0N6kW3.png");

                            fields.forEach(field -> embed.withFields(EmbedCreateFields.Field.of(field.name(), field.value(), field.inline().get())));
                        }))
                .flatMap(context::createFollowupMessage)
                .switchIfEmpty(context.createFollowupMessage(Emoji.INFO, context.localize("relicstatus.not.donator")
                        .formatted(Config.PATREON_URL, Emoji.HEARTS)));
    }

    private static Mono<Tuple2<Relic, Optional<Guild>>> getRelicAndGuild(Context context, Relic relic) {
        final Mono<Optional<Guild>> getGuild = Mono.justOrEmpty(relic.getGuildId())
                .flatMap(context.getClient()::getGuildById)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());

        return Mono.zip(Mono.just(relic), getGuild);
    }

}
