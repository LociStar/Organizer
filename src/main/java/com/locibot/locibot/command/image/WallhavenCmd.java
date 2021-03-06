package com.locibot.locibot.command.image;

import com.locibot.locibot.api.json.image.wallhaven.WallhavenResponse;
import com.locibot.locibot.api.json.image.wallhaven.Wallpaper;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.NetUtil;
import com.locibot.locibot.utils.RandUtil;
import com.locibot.locibot.utils.LociBotUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class WallhavenCmd extends BaseCmd {

    private static final String HOME_URL = "https://wallhaven.cc/api/v1/search";
    private static final Pattern COMPILE = Pattern.compile("[, ]");

    private final String apiKey;

    public WallhavenCmd() {
        super(CommandCategory.IMAGE, "wallhaven", "Search random wallpaper from Wallhaven");
        this.addOption("query", "Search for a wallpaper", false, ApplicationCommandOption.Type.STRING);

        this.apiKey = CredentialManager.get(Credential.WALLHAVEN_API_KEY);
    }

    private static EmbedCreateSpec formatEmbed(Context context, String title, Wallpaper wallpaper) {
        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
        wallpaper.getSource().ifPresent(source -> {
            if (NetUtil.isUrl(source)) {
                embed.description(context.localize("wallhaven.source.url").formatted(source));
            } else {
                embed.addFields(EmbedCreateFields.Field.of(context.localize("wallhaven.source"), source, false));
            }
        });

        embed.author(EmbedCreateFields.Author.of(title, wallpaper.url(), context.getAuthorAvatar()))
                .thumbnail("https://wallhaven.cc/images/layout/logo_sm.png")
                .image(wallpaper.path())
                .addFields(EmbedCreateFields.Field.of(context.localize("wallhaven.resolution"), wallpaper.resolution(), false));
        return LociBotUtil.getDefaultEmbed(embed.build());
    }

    @Override
    public Mono<?> execute(Context context) {
        final String query = context.getOptionAsString("query").orElse("");

        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("wallhaven.loading"))
                .then(Mono.zip(this.getWallpaper(query), context.isChannelNsfw()))
                .flatMap(TupleUtils.function((wallpaper, isNsfw) -> {
                    if (!"sfw".equals(wallpaper.purity()) && !isNsfw) {
                        return context.editFollowupMessage(Emoji.GREY_EXCLAMATION, context.localize("must.be.nsfw"));
                    }

                    final String title = context.localize("wallhaven.title")
                            .formatted(query.isBlank() ? context.localize("wallhaven.random") : query);
                    return context.editFollowupMessage(WallhavenCmd.formatEmbed(context, title, wallpaper));
                }))
                .switchIfEmpty(context.editFollowupMessage(Emoji.MAGNIFYING_GLASS,
                        context.localize("wallhaven.not.found").formatted(query)));
    }

    private Mono<Wallpaper> getWallpaper(final String query) {
        return RequestHelper.fromUrl(this.buildUrl(query))
                .to(WallhavenResponse.class)
                .map(WallhavenResponse::wallpapers)
                .filter(Predicate.not(List::isEmpty))
                .map(RandUtil::randValue);
    }

    private String buildUrl(final String query) {
        final StringBuilder urlBuilder = new StringBuilder(HOME_URL);
        urlBuilder.append("?apikey=%s".formatted(this.apiKey));

        if (query.isBlank()) {
            urlBuilder.append("&sorting=toplist")
                    .append("&purity=100");
        } else {
            final String keywords = FormatUtil.format(
                    COMPILE.split(query),
                    keyword -> "+%s".formatted(NetUtil.encode(keyword.trim())),
                    "");
            urlBuilder.append("&q=%s".formatted(keywords))
                    .append("&sorting=relevance");
        }

        return urlBuilder.toString();
    }

}
