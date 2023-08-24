package com.locibot.organizer2.commands.info;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.CommandContext;
import com.locibot.organizer2.object.Emoji;
import com.locibot.organizer2.utils.FormatUtil;
import com.locibot.organizer2.utils.TimeUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

@Component
public class InfoUserCommand implements SlashCommand {

    private final DateTimeFormatter dateFormatter;

    public InfoUserCommand() {
        this.dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.MEDIUM);
    }

    @Override
    public String getName() {
        return "info user";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        final Mono<Member> getUser = context.getOptionAsMember("user")
                .defaultIfEmpty(context.getAuthor())
                .cache();

        return Mono.zip(getUser, getUser.flatMapMany(Member::getRoles).collectList())
                .map(TupleUtils.function((user, roles) -> this.formatEmbed(context, user, roles)))
                .flatMap(embedCreateSpecMono -> embedCreateSpecMono.flatMap(embedCreateSpec -> context.getEvent().reply(
                        InteractionApplicationCommandCallbackSpec.builder()
                                .addEmbed(embedCreateSpec)
                                .build())));
    }

    private Mono<EmbedCreateSpec> formatEmbed(CommandContext<?> context, Member member, List<Role> roles) {
        final Mono<DateTimeFormatter> dateFormatterMono = context.getLocale_old().map(this.dateFormatter::withLocale);

        final StringBuilder usernameBuilder = new StringBuilder(member.getTag());
        if (member.isBot()) {
            usernameBuilder
                    .append(" ")
                    .append(context.localize_old("userinfo.bot"));
        }
        if (member.getPremiumTime().isPresent()) {
            usernameBuilder
                    .append(" ")
                    .append(context.localize_old("userinfo.booster"));
        }

        final Mono<String[]> localeMono = context.localize_old("userinfo.id", "userinfo.name", "userinfo.creation", "userinfo.join", "userinfo.title");

        final LocalDateTime createTime = TimeUtil.toLocalDateTime(member.getId().getTimestamp());
        final Mono<String> creationFieldMono = context.getLocale_old().flatMap(locale -> dateFormatterMono.map(dateTimeFormatter -> "%s%n(%s)"
                .formatted(createTime.format(dateTimeFormatter),
                        FormatUtil.formatLongDuration(locale, createTime))));

        final LocalDateTime joinTime = TimeUtil.toLocalDateTime(member.getJoinTime().get());
        final Mono<String> joinFieldMono = context.getLocale_old().flatMap(locale -> dateFormatterMono.map(dateTimeFormatter -> "%s%n(%s)"
                .formatted(joinTime.format(dateFormatter),
                        FormatUtil.formatLongDuration(locale, joinTime))));

        final String badgesField = FormatUtil.format(member.getPublicFlags(), FormatUtil::capitalizeEnum, "\n");
        final String rolesField = FormatUtil.format(roles, Role::getMention, "\n");

        Mono<EmbedCreateSpec.Builder> embedMono = Mono.zip(localeMono, creationFieldMono, joinFieldMono)
                .map(TupleUtils.function((localisedList, creationField, joinField) ->
                        EmbedCreateSpec.builder()
                                .author(EmbedCreateFields.Author.of(localisedList[4].formatted(usernameBuilder), null, context.getAuthorAvatar()))
                                .thumbnail(member.getAvatarUrl())
                                .fields(List.of(
                                        EmbedCreateFields.Field.of(Emoji.ID + localisedList[0], member.getId().asString(), true),
                                        EmbedCreateFields.Field.of(Emoji.BUST_IN_SILHOUETTE + localisedList[1], member.getDisplayName(), true),
                                        EmbedCreateFields.Field.of(Emoji.BIRTHDAY + localisedList[2], creationField, true),
                                        EmbedCreateFields.Field.of(Emoji.DATE + localisedList[3], joinField, true)))));

        if (!badgesField.isEmpty()) {
            return context.localize_old("userinfo.badges").flatMap(badgesTitle -> embedMono.map(embed -> {
                embed.addFields(EmbedCreateFields.Field.of(badgesTitle, badgesField, true));
                return context.getDefaultEmbed(embed.build());
            }));
        }

        if (!rolesField.isEmpty()) {
            return context.localize_old("userinfo.roles").flatMap(rolesTitle -> embedMono.map(embed -> {
                embed.addFields(EmbedCreateFields.Field.of(rolesTitle, rolesField, true));
                return context.getDefaultEmbed(embed.build());
            }));
        }
        return embedMono.map(embed -> context.getDefaultEmbed(embed.build()));
    }
}
