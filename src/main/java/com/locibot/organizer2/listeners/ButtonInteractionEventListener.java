package com.locibot.organizer2.listeners;

import com.locibot.organizer2.commands.ButtonEvent;
import com.locibot.organizer2.core.ButtonEventContext;
import com.locibot.organizer2.data.Telemetry;
import com.locibot.organizer2.database.repositories.EventRepository;
import com.locibot.organizer2.database.repositories.EventSubscriptionRepository;
import com.locibot.organizer2.database.repositories.GuildRepository;
import com.locibot.organizer2.database.repositories.UserRepository;
import com.locibot.organizer2.object.ExceptionHandler;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Component
public class ButtonInteractionEventListener {

    private final GuildRepository guildRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventSubscriptionRepository eventSubscriptionRepository;

    private final Collection<ButtonEvent> events;

    public ButtonInteractionEventListener(List<ButtonEvent> buttonEvents, GatewayDiscordClient client, GuildRepository guildRepository, UserRepository userRepository, EventRepository eventRepository, EventSubscriptionRepository eventSubscriptionRepository) {
        events = buttonEvents;
        this.guildRepository = guildRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventSubscriptionRepository = eventSubscriptionRepository;

        client.on(ButtonInteractionEvent.class, this::handle).subscribe();
    }


    public Mono<?> handle(ButtonInteractionEvent event) {
        System.out.println("ButtonInteractionEvent: " + event.getCustomId().split("_")[0]);
        System.out.println(events.size());
        //Convert our list to a flux that we can iterate through
        return Flux.fromIterable(events)
                //Filter out all commands that don't match the name this event is for
                .filter(command -> command.getName().equals(event.getCustomId().split("_")[0]))
                //Get the first (and only) item in the flux that matches our filter
                .next()
                //Have our command class handle all logic related to its specific command.
                .flatMap(command -> {
                    ButtonEventContext<ButtonInteractionEvent> buttonEventContext = new ButtonEventContext<>(event, guildRepository, userRepository, eventRepository, eventSubscriptionRepository);
                    // add Locale to context
                    return buttonEventContext.getUncachedLocale()
                            .flatMap(locale -> {
                                System.out.println("LOCALE: " + locale);
                                buttonEventContext.setLocale(locale);
                                return command.handle(buttonEventContext);
                            });
                })
                .onErrorResume(err -> ExceptionHandler.handleCommandError(err, new ButtonEventContext<>(event, guildRepository, userRepository, eventRepository, eventSubscriptionRepository)).then(Mono.empty()))
                .doOnSuccess(__ -> Telemetry.COMMAND_USAGE_COUNTER.labels(event.getCustomId()).inc()); //TODO: Add error handling
    }
}
