package com.locibot.organizer2.commands.event;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.command.CommandContext;
import com.locibot.organizer2.database.repositories.EventSubscriptionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DeleteEventCommand implements SlashCommand {

    private final EventSubscriptionRepository eventSubscriptionRepository;

    public DeleteEventCommand(EventSubscriptionRepository eventSubscriptionRepository) {
        this.eventSubscriptionRepository = eventSubscriptionRepository;
    }

    @Override
    public String getName() {
        return "event delete";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        String name = context.getOptionAsString("name").orElseThrow();
        return context.getEventRepository().findByNameAndOwnerId(name, context.getAuthorId().asLong())
                .switchIfEmpty(context.getEvent().reply("No event found with that name").then(Mono.empty()))
                .collectList()
                .flatMap(event -> eventSubscriptionRepository.deleteByEventId(event.get(0).getId())
                        .then(context.getEventRepository().delete(event.get(0)).then(context.getEvent().reply("Deleted your event"))));
    }
}
