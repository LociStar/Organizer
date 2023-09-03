package com.locibot.organizer2.commands.event;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.CommandContext;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GetEventsCommand implements SlashCommand {
    @Override
    public String getName() {
        return "event list";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        return context.getEventRepository().findByOwnerId(context.getAuthorId().asLong()).collectList().flatMap(events -> {
            StringBuilder eventNames = new StringBuilder();
            StringBuilder eventTypes = new StringBuilder();
            for (int i = 0; i < events.size(); i++) {
                eventNames.append(events.get(i).getName());
                eventTypes.append(events.get(i).isPublic() ? "Public" : "Private");
                if (i != events.size() - 1) {
                    eventNames.append("\n");
                    eventTypes.append("\n");
                }
            }
            return context.getEvent().reply(InteractionApplicationCommandCallbackSpec.builder()
                    .ephemeral(true)
                    .addEmbed(EmbedCreateSpec.builder()
                            .description(context.localize("event.list.description"))
                            .addField("Name", eventNames.toString(), true)
                            .addField("Type", eventTypes.toString(), true)
                            .build()).build());
        });
    }
}
