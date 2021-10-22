package com.locibot.locibot.command.image;

import com.locibot.locibot.api.json.image.r34.R34Post;
import com.locibot.locibot.api.json.image.r34.R34Posts;
import com.locibot.locibot.api.json.image.r34.R34Response;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.NetUtil;
import com.locibot.locibot.utils.RandUtil;
import com.locibot.locibot.utils.LociBotUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;

public class Rule34Cmd extends BaseCmd {

    private static final String HOME_URL = "https://rule34.xxx/index.php";
    private static final int MAX_TAGS_CHAR = 300;

    public Rule34Cmd() {
        super(CommandCategory.IMAGE, "rule34", "Search random image from Rule34");
        this.addOption("query", "Search for a Rule34 image", true, ApplicationCommandOption.Type.STRING);
    }

    private static Mono<R34Post> getR34Post(String tag) {
        final String url = "%s?".formatted(HOME_URL)
                + "page=dapi"
                + "&s=post"
                + "&q=index"
                + "&tags=%s".formatted(NetUtil.encode(tag.replace(" ", "_")));

        return RequestHelper.fromUrl(url)
                .to(R34Response.class)
                .map(R34Response::posts)
                .flatMap(Mono::justOrEmpty)
                .map(R34Posts::post)
                .flatMap(Mono::justOrEmpty)
                .map(RandUtil::randValue);
    }

    private static boolean containsChildren(R34Post post, List<String> tags) {
        return post.hasChildren() || tags.stream().anyMatch(tag -> tag.contains("loli") || tag.contains("shota"));
    }

    private static EmbedCreateSpec formatEmbed(Context context, R34Post post, String tag) {
        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder();
        post.getSource().ifPresent(source -> {
            if (NetUtil.isUrl(source)) {
                embed.description(context.localize("rule34.source.url").formatted(source));
            } else {
                embed.addFields(EmbedCreateFields.Field.of(context.localize("rule34.source"), source, false));
            }
        });

        final String resolution = "%dx%d".formatted(post.width(), post.height());
        final String formattedTags = Rule34Cmd.formatTags(post.getTags());
        embed.author(EmbedCreateFields.Author.of(context.localize("rule34.title").formatted(tag), post.fileUrl(), context.getAuthorAvatar()))
                .thumbnail("https://i.imgur.com/t6JJWFN.png")
                .addFields(
                        EmbedCreateFields.Field.of(context.localize("rule34.resolution"), resolution, false),
                        EmbedCreateFields.Field.of(context.localize("rule34.tags"), formattedTags, false))
                .image(post.fileUrl())
                .footer(EmbedCreateFields.Footer.of(context.localize("rule34.footer"), null));
        return LociBotUtil.getDefaultEmbed(embed.build());
    }

    private static String formatTags(final List<String> tags) {
        final StringBuilder tagsBuilder = new StringBuilder();
        for (final String tag : tags) {
            if (tagsBuilder.length() + tag.length() < MAX_TAGS_CHAR) {
                tagsBuilder.append("`%s` ".formatted(tag));
            } else {
                tagsBuilder.append("...");
                break;
            }
        }
        return tagsBuilder.toString();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String query = context.getOptionAsString("query").orElseThrow();

        return context.isChannelNsfw()
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return context.createFollowupMessage(Emoji.GREY_EXCLAMATION, context.localize("must.be.nsfw"));
                    }

                    return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("rule34.loading"))
                            .then(Rule34Cmd.getR34Post(query))
                            .flatMap(post -> {
                                // Don't post images containing children
                                if (Rule34Cmd.containsChildren(post, post.getTags())) {
                                    return context.editFollowupMessage(Emoji.WARNING, context.localize("rule34.children"));
                                }

                                return context.editFollowupMessage(Rule34Cmd.formatEmbed(context, post, query));
                            })
                            .switchIfEmpty(context.editFollowupMessage(Emoji.MAGNIFYING_GLASS,
                                    context.localize("rule34.not.found").formatted(query)));
                });
    }

}
