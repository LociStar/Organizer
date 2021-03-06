package com.locibot.locibot.command.info;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.LociBotUtil;
import com.locibot.locibot.utils.TimeUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

public class UserInfoCmd extends BaseCmd {

    private final DateTimeFormatter dateFormatter;

    public UserInfoCmd() {
        super(CommandCategory.INFO, "user", "Show user info");
        this.addOption("user", "If not specified, it will show your info", false,
                ApplicationCommandOption.Type.USER);

        this.dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.MEDIUM);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Mono<Member> getUser = context.getOptionAsMember("user")
                .defaultIfEmpty(context.getAuthor())
                .cache();

        return Mono.zip(getUser, getUser.flatMapMany(Member::getRoles).collectList())
                .map(TupleUtils.function((user, roles) -> this.formatEmbed(context, user, roles)))
                .flatMap(context::createFollowupMessage);
    }

    private EmbedCreateSpec formatEmbed(Context context, Member member, List<Role> roles) {
        final DateTimeFormatter dateFormatter = this.dateFormatter.withLocale(context.getLocale());

        final StringBuilder usernameBuilder = new StringBuilder(member.getTag());
        if (member.isBot()) {
            usernameBuilder
                    .append(" ")
                    .append(context.localize("userinfo.bot"));
        }
        if (member.getPremiumTime().isPresent()) {
            usernameBuilder
                    .append(" ")
                    .append(context.localize("userinfo.booster"));
        }

        final String idTitle = Emoji.ID + " " + context.localize("userinfo.id");
        final String nameTitle = Emoji.BUST_IN_SILHOUETTE + " " + context.localize("userinfo.name");

        final String creationTitle = Emoji.BIRTHDAY + " " + context.localize("userinfo.creation");
        final LocalDateTime createTime = TimeUtil.toLocalDateTime(member.getId().getTimestamp());
        final String creationField = "%s%n(%s)"
                .formatted(createTime.format(dateFormatter),
                        FormatUtil.formatLongDuration(context.getLocale(), createTime));

        final String joinTitle = Emoji.DATE + " " + context.localize("userinfo.join");
        final LocalDateTime joinTime = TimeUtil.toLocalDateTime(member.getJoinTime().get());
        final String joinField = "%s%n(%s)"
                .formatted(joinTime.format(dateFormatter),
                        FormatUtil.formatLongDuration(context.getLocale(), joinTime));

        final String badgesField = FormatUtil.format(member.getPublicFlags(), FormatUtil::capitalizeEnum, "\n");
        final String rolesField = FormatUtil.format(roles, Role::getMention, "\n");

        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                .author(EmbedCreateFields.Author.of(context.localize("userinfo.title").formatted(usernameBuilder), null, context.getAuthorAvatar()))
                .thumbnail(member.getAvatarUrl())
                .fields(List.of(
                        EmbedCreateFields.Field.of(idTitle, member.getId().asString(), true),
                        EmbedCreateFields.Field.of(nameTitle, member.getDisplayName(), true),
                        EmbedCreateFields.Field.of(creationTitle, creationField, true),
                        EmbedCreateFields.Field.of(joinTitle, joinField, true)));

        if (!badgesField.isEmpty()) {
            final String badgesTitle = Emoji.MILITARY_MEDAL + " " + context.localize("userinfo.badges");
            embed.addFields(EmbedCreateFields.Field.of(badgesTitle, badgesField, true));
        }

        if (!rolesField.isEmpty()) {
            final String rolesTitle = Emoji.LOCK + " " + context.localize("userinfo.roles");
            embed.addFields(EmbedCreateFields.Field.of(rolesTitle, rolesField, true));
        }
        return LociBotUtil.getDefaultEmbed(embed.build());
    }

}
