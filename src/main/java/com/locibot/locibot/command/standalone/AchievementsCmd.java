package com.locibot.locibot.command.standalone;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CmdAnnotation;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.core.i18n.I18nContext;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.users.entity.DBUser;
import com.locibot.locibot.database.users.entity.achievement.Achievement;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.LociBotUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.EnumSet;

@CmdAnnotation
public class AchievementsCmd extends BaseCmd {

    public AchievementsCmd() {
        super(CommandCategory.INFO, "achievements", "Show user's achievements");
        this.addOption(option -> option.name("user")
                .description("If not specified, it will show your achievements")
                .required(false)
                .type(ApplicationCommandOption.Type.USER.getValue()));
    }

    private static EmbedCreateSpec formatEmbed(I18nContext context, EnumSet<Achievement> achievements,
                                               Member member) {
        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
        embed.author(EmbedCreateFields.Author.of(context.localize("achievement.title").formatted(member.getUsername()),
                null, member.getAvatarUrl()));
        embed.thumbnail("https://i.imgur.com/IMHDI7D.png");
        embed.title(context.localize("achievement.progression")
                .formatted(achievements.size(), Achievement.values().length));

        final StringBuilder description = new StringBuilder();
        for (final Achievement achievement : Achievement.values()) {
            description.append(
                    AchievementsCmd.formatAchievement(context, achievement, achievements.contains(achievement)));
        }
        embed.description(description.toString());
        return LociBotUtil.getDefaultEmbed(embed.build());
    }

    private static String formatAchievement(I18nContext context, Achievement achievement, boolean unlocked) {
        final Emoji emoji = unlocked ? achievement.getEmoji() : Emoji.LOCK;
        return "%s **%s**%n%s%n".formatted(emoji, achievement.getTitle(context), achievement.getDescription(context));
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getOptionAsMember("user")
                .defaultIfEmpty(context.getAuthor())
                .flatMap(member -> DatabaseManager.getUsers().getDBUser(member.getId())
                        .map(DBUser::getAchievements)
                        .map(achievements -> AchievementsCmd.formatEmbed(context, achievements, member)))
                .flatMap(context::createFollowupMessage);
    }

}
