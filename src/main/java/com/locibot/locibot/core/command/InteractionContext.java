package com.locibot.locibot.core.command;

import com.locibot.locibot.object.Emoji;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

public interface InteractionContext {

    Mono<Void> reply(Emoji emoji, String message);

    Mono<Void> replyEphemeral(Emoji emoji, String message);

    Mono<Message> createFollowupMessage(String message);

    Mono<Message> createFollowupMessage(Emoji emoji, String message);

    Mono<Message> createFollowupMessage(EmbedCreateSpec embed);

    Mono<Message> createFollowupMessage(InteractionFollowupCreateSpec spec);

    Mono<Message> editFollowupMessage(String message);

    Mono<Message> editFollowupMessage(Emoji emoji, String message);

    Mono<Message> editFollowupMessage(EmbedCreateSpec embed);

    Mono<Message> editInitialFollowupMessage(EmbedCreateSpec embed);

}
