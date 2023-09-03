package com.locibot.organizer2.commands.event;

import com.locibot.organizer2.commands.ButtonEvent;
import com.locibot.organizer2.core.ButtonEventContext;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.User;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class LeaveButtonEvent implements ButtonEvent {
    @Override
    public String getName() {
        return "leaveEventButton";
    }

    @Override
    public Mono<?> handle(ButtonEventContext<ButtonInteractionEvent> context) {

        return isSubscribed(context.getCommandId(), context.getEvent().getInteraction().getUser().getId().asLong(), context).flatMap(isSubscribed -> {
            if (!isSubscribed) {
                return context.getEvent().reply(context.localize("event.button.leave.left.error")).withEphemeral(true);
            } else {
                Mono<?> delete = context.getEventSubscriptionRepository().delete(context.getCommandId(), context.getEvent().getInteraction().getUser().getId().asLong());
                String username = context.getEvent().getInteraction().getUser().getUsername();
                return delete
                        .then(context.getEventRepository().findById(context.getCommandId()).flatMap(event -> {
                            Mono<User> ownerMono = context.getEvent().getClient().getUserById(Snowflake.of(event.getOwner_id()));
                            return ownerMono.flatMap(owner -> owner.getPrivateChannel().flatMap(privateChannel -> privateChannel.createMessage(
                                    context.localize("event.button.leave.to").formatted(username, event.getName()))));
                        }))
                        .then(context.getEvent().reply(context.localize("event.button.leave.left")).withEphemeral(true));
            }
        });
    }

    public Mono<Boolean> isSubscribed(Long eventId, Long userId, ButtonEventContext<ButtonInteractionEvent> context) {
        return context.getEventSubscriptionRepository().findByEventIdAndUserId(eventId, userId).map(eventSubscription -> true).defaultIfEmpty(false);
    }
}
