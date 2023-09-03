package com.locibot.organizer2.commands.event;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.CommandContext;
import com.locibot.organizer2.database.tables.Event;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class InviteEventCommand implements SlashCommand {
    @Override
    public String getName() {
        return "event invite";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        String name = context.getOptionAsString("event_name").orElseThrow();
        Flux<User> userFlux = Flux.range(1, 10)
                .flatMap(i -> context.getOptionAsUser("user_" + i))
                .filter(Objects::nonNull);

        Mono<Event> getEvent = context.getEventRepository().findByNameAndOwnerId(name, context.getAuthorId().asLong()).next();

        return getEvent
                .flatMap(event -> userFlux.flatMap(user -> user.getPrivateChannel().flatMap(privateChannel -> privateChannel
                        .createMessage(MessageCreateSpec.builder()
                                .content(context.localize("event.util.invitation"))
                                .addEmbed(EventUtil.createEventEmbed(event.getName(), event.getDescription(), event.getIcon(), event, context))
                                .addComponent(ActionRow.of(
                                        Button.success("joinEventButton_" + event.getId(), context.localize("event.publish.button.join")),
                                        Button.danger("leaveEventButton_" + event.getId(), context.localize("event.publish.button.leave"))))
                                .build())
                        .then(context.getEvent().reply(context.localize("event.add.success").formatted(user.getId().asLong())))
                )).collectList())
                .switchIfEmpty(context.getEvent().reply(context.localize("event.not.found").formatted(name)).then(Mono.empty()));
    }
}
