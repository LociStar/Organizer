package com.locibot.locibot.command.gamestats;

import com.locibot.locibot.api.json.gamestats.overwatch.OverwatchProfile;
import com.locibot.locibot.api.json.gamestats.overwatch.profile.Competitive;
import com.locibot.locibot.api.json.gamestats.overwatch.profile.ProfileResponse;
import com.locibot.locibot.api.json.gamestats.overwatch.stats.StatsResponse;
import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.cache.MultiValueCache;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.core.i18n.I18nManager;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.NetUtil;
import com.locibot.locibot.utils.LociBotUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class OverwatchCmd extends BaseCmd {

    private static final String HOME_URL = "https://owapi.io";
    private static final String PROFILE_API_URL = "%s/profile".formatted(HOME_URL);
    private static final String STATS_API_URL = "%s/stats".formatted(HOME_URL);
    private final MultiValueCache<String, OverwatchProfile> cachedValues;

    public OverwatchCmd() {
        super(CommandCategory.GAMESTATS, "overwatch", "Search for Overwatch statistics");
        this.addOption("platform", "User's platform", true, ApplicationCommandOption.Type.STRING,
                DiscordUtil.toOptions(Platform.class));
        this.addOption("battletag", "User's battletag, case sensitive", true,
                ApplicationCommandOption.Type.STRING);

        this.cachedValues = MultiValueCache.Builder.<String, OverwatchProfile>builder()
                .withTtl(Config.CACHE_TTL)
                .build();
        this.setEnabled(false);
    }

    private static EmbedCreateSpec formatEmbed(Context context, OverwatchProfile overwatchProfile, Platform platform) {
        final ProfileResponse profile = overwatchProfile.profile();
        return LociBotUtil.getDefaultEmbed(
                EmbedCreateSpec.builder().author(EmbedCreateFields.Author.of(context.localize("overwatch.title"),
                        "https://playoverwatch.com/en-gb/career/%s/%s"
                                .formatted(platform.getName(), profile.username()),
                        context.getAuthorAvatar()))
                        .thumbnail(profile.portrait())
                        .description(context.localize("overwatch.description")
                                .formatted(profile.username()))
                        .fields(List.of(
                                EmbedCreateFields.Field.of(context.localize("overwatch.level"),
                                        Integer.toString(profile.level()), true),
                                EmbedCreateFields.Field.of(context.localize("overwatch.playtime"),
                                        profile.getQuickplayPlaytime(), true),
                                EmbedCreateFields.Field.of(context.localize("overwatch.games.won"),
                                        context.localize(profile.games().getQuickplayWon()), true),
                                EmbedCreateFields.Field.of(context.localize("overwatch.ranks"),
                                        OverwatchCmd.formatCompetitive(context, profile.competitive()), true),
                                EmbedCreateFields.Field.of(context.localize("overwatch.heroes.played"),
                                        overwatchProfile.getQuickplay().getPlayed(), true),
                                EmbedCreateFields.Field.of(context.localize("overwatch.heroes.ratio"),
                                        overwatchProfile.getQuickplay().getEliminationsPerLife(), true))).build());
    }

    private static String formatCompetitive(Context context, Competitive competitive) {
        final StringBuilder strBuilder = new StringBuilder();
        competitive.damage().rank()
                .ifPresent(rank -> strBuilder.append(context.localize("overwatch.competitive.damage")
                        .formatted(context.localize(rank))));
        competitive.tank().rank()
                .ifPresent(rank -> strBuilder.append(context.localize("overwatch.competitive.tank")
                        .formatted(context.localize(rank))));
        competitive.support().rank()
                .ifPresent(rank -> strBuilder.append(context.localize("overwatch.competitive.support")
                        .formatted(context.localize(rank))));
        return strBuilder.isEmpty() ? context.localize("overwatch.not.ranked") : strBuilder.toString();
    }

    private static String buildUrl(String url, Platform platform, String username) {
        return "%s/%s/global/%s".formatted(url, platform.getName(), username);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Platform platform = context.getOptionAsEnum(Platform.class, "platform").orElseThrow();
        final String battletag = context.getOptionAsString("battletag").orElseThrow();

        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("overwatch.loading"))
                .then(this.getOverwatchProfile(context.getLocale(), battletag, platform))
                .flatMap(profile -> {
                    if (profile.profile().isPrivate()) {
                        return context.editFollowupMessage(Emoji.ACCESS_DENIED, context.localize("overwatch.private"));
                    }
                    return context.editFollowupMessage(OverwatchCmd.formatEmbed(context, profile, platform));
                });
    }

    private Mono<OverwatchProfile> getOverwatchProfile(Locale locale, String battletag, Platform platform) {
        final String username = NetUtil.encode(battletag.replace("#", "-"));

        final String profileUrl = OverwatchCmd.buildUrl(PROFILE_API_URL, platform, username);
        final Mono<ProfileResponse> getProfile = RequestHelper.fromUrl(profileUrl)
                .to(ProfileResponse.class)
                .filter(profile -> !"Error: Profile not found".equals(profile.message().orElse("")))
                .switchIfEmpty(Mono.error(new CommandException(I18nManager.localize(locale, "overwatch.not.found"))));

        final String statsUrl = OverwatchCmd.buildUrl(STATS_API_URL, platform, username);
        final Mono<StatsResponse> getStats = RequestHelper.fromUrl(statsUrl)
                .to(StatsResponse.class);

        return this.cachedValues
                .getOrCache(profileUrl, Mono.zip(Mono.just(platform), getProfile, getStats)
                        .map(TupleUtils.function(OverwatchProfile::new)))
                .filter(overwatchProfile -> overwatchProfile.profile().portrait() != null)
                .switchIfEmpty(Mono.error(new IOException("Overwatch API returned malformed JSON")));
    }

    public enum Platform {
        PC("pc"),
        PSN("psn"),
        XBL("xbl"),
        NINTENDO_SWITCH("nintendo-switch");

        private final String name;

        Platform(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

}
