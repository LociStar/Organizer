package com.locibot.locibot.command.fun;

import com.locibot.locibot.api.json.pandorabots.ChatBotResponse;
import com.locibot.locibot.api.json.pandorabots.ChatBotResult;
import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CmdAnnotation;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.LogUtil;
import com.locibot.locibot.utils.NetUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandOption;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@CmdAnnotation
public class ChatCmd extends BaseCmd {

    private static final Logger LOGGER = LogUtil.getLogger(ChatCmd.class, LogUtil.Category.COMMAND);
    private static final String HOME_URl = "https://www.pandorabots.com/pandora/talk-xml";
    private static final int MAX_CHARACTERS = 250;
    private static final Map<String, String> BOTS = new LinkedHashMap<>(4);

    static {
        BOTS.put("Marvin", "efc39100ce34d038");
        BOTS.put("Chomsky", "b0dafd24ee35a477");
        BOTS.put("R.I.V.K.A", "ea373c261e3458c6");
        BOTS.put("Lisa", "b0a6a41a5e345c23");
    }

    private final Map<Snowflake, String> channelsCustid;

    public ChatCmd() {
        super(CommandCategory.FUN, "chat", "Chat with an artificial intelligence");
        this.addOption(option -> option.name("message")
                .description("The message to send, must not exceed %d characters".formatted(MAX_CHARACTERS))
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue()));

        this.channelsCustid = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String message = context.getOptionAsString("message").orElseThrow();
        if (message.length() > MAX_CHARACTERS) {
            return Mono.error(new CommandException(context.localize("chat.max.characters")
                    .formatted(MAX_CHARACTERS)));
        }

        return this.getResponse(context.getChannelId(), message)
                .flatMap(response -> context.createFollowupMessage(Emoji.SPEECH, response))
                .switchIfEmpty(Mono.error(new IOException("Bots are unreachable")));
    }

    private Mono<String> getResponse(Snowflake channelId, String message) {
        return Flux.fromIterable(BOTS.entrySet())
                .flatMapSequential(bot -> this.talk(channelId, bot.getValue(), message)
                        .map(response -> "**%s**: %s".formatted(bot.getKey(), response)))
                .takeUntil(Predicate.not(String::isBlank))
                .next();
    }

    private Mono<String> talk(Snowflake channelId, String botId, String message) {
        final String url = "%s?".formatted(HOME_URl)
                + "botid=%s".formatted(botId)
                + "&input=%s".formatted(NetUtil.encode(message))
                + "&custid=%s".formatted(this.channelsCustid.getOrDefault(channelId, ""));

        return RequestHelper.fromUrl(url)
                .to(ChatBotResponse.class)
                .map(ChatBotResponse::result)
                .doOnNext(chat -> this.channelsCustid.put(channelId, chat.custid()))
                .map(ChatBotResult::getResponse)
                .onErrorResume(err -> Mono.fromRunnable(() ->
                        LOGGER.info("{} is not reachable, trying another one: {}", botId, err.getMessage())));
    }

}
