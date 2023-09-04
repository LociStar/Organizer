package com.locibot.organizer2.commands.util;

import com.locibot.organizer2.commands.ButtonEvent;
import com.locibot.organizer2.core.ButtonEventContext;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DeleteUserDataUtilEvent implements ButtonEvent {
    @Override
    public String getName() {
        return "deleteUserData";
    }

    @Override
    public Mono<?> handle(ButtonEventContext<ButtonInteractionEvent> context) {
        return context.getEventSubscriptionRepository().deleteAllByUserId(context.getAuthorId().asLong())
                .then(context.getEventRepository().deleteAllByOwnerId(context.getAuthorId().asLong()))
                .then(context.getUserRepository().deleteById(context.getAuthorId().asLong()))
                .then(context.getEvent().reply("Deleted all your user data").withEphemeral(true));
    }
}
