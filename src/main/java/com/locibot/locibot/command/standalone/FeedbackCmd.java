package com.locibot.locibot.command.standalone;

import com.locibot.locibot.LociBot;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CmdAnnotation;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.core.ratelimiter.RateLimiter;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.time.Duration;

@CmdAnnotation
public class FeedbackCmd extends BaseCmd {

    public FeedbackCmd() {
        super(CommandCategory.INFO, "feedback", "Send a feedback");
        this.setRateLimiter(new RateLimiter(1, Duration.ofMinutes(10)));
        this.addOption("message", "Your feedback", true, ApplicationCommandOption.Type.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String message = context.getOptionAsString("message").orElseThrow();
        return context.getClient()
                .getUserById(LociBot.getOwnerId())
                .flatMap(User::getPrivateChannel)
                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.SPEECH, "Feedback from **%s** (User ID: %s, Guild ID: %s):%n%s"
                        .formatted(context.getAuthor().getTag(), context.getAuthorId().asString(),
                                context.getGuildId().asString(), message), channel))
                .then(context.createFollowupMessage(Emoji.INFO, context.localize("feedback.message")));
    }

}
