package com.locibot.locibot.command.image;

import com.locibot.locibot.api.html.suicidegirl.SuicideGirl;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;

public class SuicideGirlsCmd extends BaseCmd {

    private static final String HOME_URL = "https://www.suicidegirls.com/photos/sg/recent/all/";

    public SuicideGirlsCmd() {
        super(CommandCategory.IMAGE, "suicidegirls", "Show random image from SuicideGirls");
    }

    private static EmbedCreateSpec formatEmbed(Context context, SuicideGirl post) {
        return ShadbotUtil.getDefaultEmbed(
                EmbedCreateSpec.builder().author(EmbedCreateFields.Author.of("SuicideGirls", post.getUrl(), context.getAuthorAvatar()))
                        .description(context.localize("suicidegirls.name").formatted(post.getName()))
                        .image(post.getImageUrl()).build());
    }

    private static Mono<SuicideGirl> getRandomSuicideGirl() {
        return RequestHelper.request(HOME_URL)
                .map(Jsoup::parse)
                .map(SuicideGirl::new);
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.isChannelNsfw()
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return context.createFollowupMessage(Emoji.GREY_EXCLAMATION, context.localize("must.be.nsfw"));
                    }

                    return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("suicidegirls.loading"))
                            .then(SuicideGirlsCmd.getRandomSuicideGirl())
                            .flatMap(post -> context.editFollowupMessage(SuicideGirlsCmd.formatEmbed(context, post)));
                });
    }

}
