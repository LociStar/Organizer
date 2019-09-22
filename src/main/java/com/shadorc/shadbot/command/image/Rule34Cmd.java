package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.image.r34.R34Post;
import com.shadorc.shadbot.api.image.r34.R34Posts;
import com.shadorc.shadbot.api.image.r34.R34Response;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.*;
import discord4j.core.spec.EmbedCreateSpec;
import org.json.XML;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class Rule34Cmd extends BaseCmd {

    private static final int MAX_TAGS_LENGTH = 400;

    public Rule34Cmd() {
        super(CommandCategory.IMAGE, List.of("rule34"), "r34");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        return updatableMsg.setContent("Loading rule34 image...")
                .send()
                .then(context.isChannelNsfw())
                .flatMap(isNsfw -> Mono.fromCallable(() -> {
                    if (!isNsfw) {
                        return updatableMsg.setContent(TextUtils.mustBeNsfw(context.getPrefix()));
                    }

                    final String url = String.format("https://rule34.xxx/index.php?page=dapi&s=post&q=index&tags=%s",
                            NetUtils.encode(arg.replace(" ", "_")));

                    final R34Response r34 = Utils.MAPPER.readValue(XML.toJSONObject(NetUtils.get(url).block()).toString(), R34Response.class);
                    if (!r34.getPosts().map(R34Posts::getCount).map(count -> count != 0).orElse(false)) {
                        return updatableMsg.setContent(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No images were found for the search `%s`",
                                context.getUsername(), arg));
                    }

                    final R34Post post = Utils.randValue(r34.getPosts().map(R34Posts::getPosts).get());

                    final List<String> tags = StringUtils.split(post.getTags(), " ");
                    if (post.hasChildren() || tags.stream().anyMatch(tag -> tag.contains("loli") || tag.contains("shota"))) {
                        return updatableMsg.setContent(
                                String.format(Emoji.WARNING + " (**%s**) I don't display images containing children or tagged with `loli` or `shota`.",
                                        context.getUsername()));
                    }

                    final String formattedtags = org.apache.commons.lang3.StringUtils.truncate(
                            FormatUtils.format(tags, tag -> String.format("`%s`", tag), " "), MAX_TAGS_LENGTH);

                    return updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> {
                                embed.setAuthor(String.format("Rule34: %s", arg), post.getFileUrl(), context.getAvatarUrl())
                                        .setThumbnail("https://i.imgur.com/t6JJWFN.png")
                                        .addField("Resolution", String.format("%dx%s", post.getWidth(), post.getHeight()), false)
                                        .addField("Tags", formattedtags, false)
                                        .setImage(post.getFileUrl())
                                        .setFooter("If there is no preview, click on the title to see the media (probably a video)", null);

                                if (!post.getSource().isEmpty()) {
                                    embed.setDescription(String.format("%n[**Source**](%s)", post.getSource()));
                                }
                            }));
                }))
                .flatMap(UpdatableMessage::send)
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show a random image corresponding to a tag from Rule34 website.")
                .setSource("https://www.rule34.xxx/")
                .addArg("tag", false)
                .build();
    }
}
