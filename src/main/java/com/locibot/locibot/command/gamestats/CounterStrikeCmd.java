package com.locibot.locibot.command.gamestats;

import com.locibot.locibot.api.json.gamestats.steam.player.PlayerSummaries;
import com.locibot.locibot.api.json.gamestats.steam.player.PlayerSummariesResponse;
import com.locibot.locibot.api.json.gamestats.steam.player.PlayerSummary;
import com.locibot.locibot.api.json.gamestats.steam.resolver.ResolveVanityUrlResponse;
import com.locibot.locibot.api.json.gamestats.steam.resolver.Response;
import com.locibot.locibot.api.json.gamestats.steam.stats.PlayerStats;
import com.locibot.locibot.api.json.gamestats.steam.stats.Stats;
import com.locibot.locibot.api.json.gamestats.steam.stats.UserStatsForGameResponse;
import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.cache.MultiValueCache;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.NetUtil;
import com.locibot.locibot.utils.NumberUtil;
import com.locibot.locibot.utils.LociBotUtil;
import com.locibot.locibot.utils.StringUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CounterStrikeCmd extends BaseCmd {

    private static final String PRIVACY_HELP_URL = "https://support.steampowered.com/kb_article.php?ref=4113-YUDH-6401";

    private static final String RESOLVE_VANITY_URL = "http://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/";
    private static final String PLAYER_SUMMARIES_URL = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/";
    private static final String USER_STATS_FOR_GAME_URL = "http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/";

    private final String apiKey;
    private final MultiValueCache<String, String> steamIdCache;
    private final MultiValueCache<String, PlayerSummary> playerSummaryCache;
    private final MultiValueCache<String, String> userStatsCache;

    public CounterStrikeCmd() {
        super(CommandCategory.GAMESTATS, "csgo", "Search for Counter-Strike: Global Offensive statistics");
        this.addOption(option -> option.name("steamid")
                .description("Steam ID, custom ID or profile URL")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue()));

        this.apiKey = CredentialManager.get(Credential.STEAM_API_KEY);
        this.steamIdCache = MultiValueCache.Builder.<String, String>builder().withTtl(Config.CACHE_TTL).build();
        this.playerSummaryCache = MultiValueCache.Builder.<String, PlayerSummary>builder().withTtl(Config.CACHE_TTL).build();
        this.userStatsCache = MultiValueCache.Builder.<String, String>builder().withTtl(Config.CACHE_TTL).build();
        this.setEnabled(false);
    }

    private static String getIdentificator(String arg) {
        // The user provided an URL that can contains a pseudo or an ID
        if (arg.contains("/")) {
            final List<String> splittedUrl = StringUtil.split(arg, "/");
            if (splittedUrl.isEmpty()) {
                throw new IllegalArgumentException();
            }
            return splittedUrl.get(splittedUrl.size() - 1);
        } else {
            return arg;
        }
    }

    private static EmbedCreateSpec formatEmbed(Context context, PlayerSummary player, List<Stats> stats) {
        final Map<String, Integer> statsMap = stats.stream()
                .collect(Collectors.toMap(Stats::name, Stats::value));

        final int kills = statsMap.get("total_kills");
        final int deaths = statsMap.get("total_deaths");
        final float ratio = (float) kills / deaths;

        final int wins = statsMap.get("total_wins");
        final int roundsPlayed = statsMap.get("total_rounds_played");
        final float winRate = (float) wins / roundsPlayed * 100;

        final int mvps = statsMap.get("total_mvps");
        final float timePlayed = statsMap.get("total_time_played") / 3600f;

        final int shotsHit = statsMap.get("total_shots_hit");
        final int shotsFired = statsMap.get("total_shots_fired");
        final float accuracyRate = (float) shotsHit / shotsFired * 100;

        final int killsHeadshot = statsMap.get("total_kills_headshot");
        final float headshotRate = (float) killsHeadshot / kills * 100;

        return LociBotUtil.getDefaultEmbed(
                EmbedCreateSpec.builder().author(EmbedCreateFields.Author.of(context.localize("cs.title"),
                        "http://steamcommunity.com/profiles/%s".formatted(player.steamId()), context.getAuthorAvatar()))
                        .thumbnail(player.avatarFull())
                        .description(context.localize("cs.description").formatted(player.personaName()))
                        .fields(List.of(
                                EmbedCreateFields.Field.of(context.localize("cs.kills"), context.localize(kills), true),
                                EmbedCreateFields.Field.of(context.localize("cs.playtime"), context.localize("counterstrike.time.played")
                                        .formatted(context.localize(timePlayed)), true),
                                EmbedCreateFields.Field.of(context.localize("cs.mvp"), context.localize(mvps), true),
                                EmbedCreateFields.Field.of(context.localize("cs.win"), "%s%%".formatted(context.localize(winRate)), true),
                                EmbedCreateFields.Field.of(context.localize("cs.accuracy"), "%s%%".formatted(context.localize(accuracyRate)), true),
                                EmbedCreateFields.Field.of(context.localize("cs.headshot"), "%s%%".formatted(context.localize(headshotRate)), true),
                                EmbedCreateFields.Field.of(context.localize("cs.ratio"), context.localize(ratio), true))).build());
    }

    @Override
    public Mono<?> execute(Context context) {
        final String steamId = context.getOptionAsString("steamid").orElseThrow();
        final String identificator;
        try {
            identificator = CounterStrikeCmd.getIdentificator(steamId);
        } catch (final IllegalArgumentException err) {
            throw new CommandException(context.localize("counterstrike.invalid.id"));
        }

        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("cs.loading"))
                .then(this.steamIdCache.getOrCache(identificator, this.getSteamId(identificator)))
                .flatMap(id -> this.playerSummaryCache.getOrCache(id, this.getPlayerSummary(id)))
                .flatMap(player -> {
                    if (player.getCommunityVisibilityState() != PlayerSummary.CommunityVisibilityState.PUBLIC) {
                        return context.editFollowupMessage(Emoji.ACCESS_DENIED,
                                context.localize("cs.profile.private").formatted(PRIVACY_HELP_URL));
                    }

                    final String userStatsUrl = "%s?".formatted(USER_STATS_FOR_GAME_URL)
                            + "appid=730"
                            + "&key=%s".formatted(this.apiKey)
                            + "&steamid=%s".formatted(player.steamId());

                    return this.userStatsCache.getOrCache(userStatsUrl, RequestHelper.request(userStatsUrl))
                            .flatMap(body -> {
                                if (body.contains("500 Internal Server Error")) {
                                    return context.editFollowupMessage(Emoji.ACCESS_DENIED,
                                            context.localize("cs.games.private").formatted(PRIVACY_HELP_URL));
                                }

                                return Mono.fromCallable(() -> NetUtil.MAPPER.readValue(body, UserStatsForGameResponse.class))
                                        .map(userStats -> userStats.playerStats()
                                                .flatMap(PlayerStats::stats))
                                        .flatMap(Mono::justOrEmpty)
                                        .flatMap(stats -> context.editFollowupMessage(
                                                CounterStrikeCmd.formatEmbed(context, player, stats)))
                                        .switchIfEmpty(context.editFollowupMessage(Emoji.MAGNIFYING_GLASS,
                                                context.localize("cs.not.playing")));
                            });
                })
                .switchIfEmpty(context.editFollowupMessage(Emoji.MAGNIFYING_GLASS, context.localize("cs.player.not.found")));
    }

    private Mono<String> getSteamId(String identificator) {
        // The user directly provided the ID
        if (NumberUtil.isPositiveLong(identificator)) {
            return Mono.just(identificator);
        }
        // The user provided a pseudo
        else {
            final String url = "%s?".formatted(RESOLVE_VANITY_URL)
                    + "key=%s".formatted(this.apiKey)
                    + "&vanityurl=%s".formatted(NetUtil.encode(identificator));
            return RequestHelper.fromUrl(url)
                    .to(ResolveVanityUrlResponse.class)
                    .map(ResolveVanityUrlResponse::response)
                    .map(Response::steamId)
                    .flatMap(Mono::justOrEmpty);
        }
    }

    private Mono<PlayerSummary> getPlayerSummary(String steamId) {
        final String url = "%s?".formatted(PLAYER_SUMMARIES_URL)
                + "key=%s".formatted(this.apiKey)
                + "&steamids=%s".formatted(steamId);
        return RequestHelper.fromUrl(url)
                .to(PlayerSummariesResponse.class)
                .map(PlayerSummariesResponse::response)
                // Users matching the steamId
                .flatMapIterable(PlayerSummaries::players)
                .next();
    }

}
