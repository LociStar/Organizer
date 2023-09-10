package com.locibot.organizer2.commands.event;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.command.CommandContext;
import com.locibot.organizer2.database.tables.Event;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.*;

import static com.locibot.organizer2.commands.event.EventUtil.createEventEmbed;

@Component
public class CreateEventCommand implements SlashCommand {
    @Override
    public String getName() {
        return "event create";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        String type = context.getOptionAsString("type").orElseThrow();
        String name = context.getOptionAsString("name").orElseThrow();
        String description = context.getOptionAsString("description").orElseThrow();
        String time = context.getOptionAsString("time").orElseThrow();
        String date = context.getOptionAsString("date").orElseThrow();
        String icon = context.getOptionAsString("icon").orElse("");
        // @everyone if empty
        Mono<Role> roleMono = context.getOptionAsRole("role").switchIfEmpty(context.getGuild().flatMap(Guild::getEveryoneRole));

        //check if icon url is valid
        if (!icon.isBlank() && !icon.matches("https?://.*")) {
            return context.getEvent().reply("Invalid icon url").then(Mono.empty());
        }

        Mono<Event> createEvent = context.getUserRepository().findById(context.getAuthorId().asLong())
                .switchIfEmpty(context.getEvent().reply("Please set your time zone first").then(Mono.empty()))
                .flatMap(user -> {
                    if (user.getTime_zone() == null) {
                        return context.getEvent().reply("Please set your time zone first").then(Mono.empty());
                    }
                    ZonedDateTime timestamp;
                    try {
                        timestamp = ZonedDateTime.of(LocalDate.parse(date), LocalTime.parse(time), ZoneId.of(user.getTime_zone()));
                    } catch (Exception e) {
                        return context.getEvent().reply("Invalid date or time").then(Mono.empty());
                    }
                    return context.getEventRepository().save(name, description, timestamp.toEpochSecond(), type.equals("public"), icon, context.getAuthorId().asLong());
                });

        Mono<?> privateEvent = createEvent.flatMap(event -> roleMono.flatMap(role -> context.getEvent().reply(InteractionApplicationCommandCallbackSpec.builder()
                .ephemeral(true)
                .content(context.localize("event.private.created"))
                .addEmbed(createEventEmbed(name, description, icon, event, context))
                .build())));

        Mono<?> publicEvent = createEvent.flatMap(event -> roleMono.flatMap(role -> context.getEvent().reply(InteractionApplicationCommandCallbackSpec.builder()
                .content(role.getMention())
                .addEmbed(createEventEmbed(name, description, icon, event, context))
                .addComponent(ActionRow.of(
                        Button.success("joinEventButton_" + event.getId(), context.localize("event.publish.button.join")),
                        Button.danger("leaveEventButton_" + event.getId(), context.localize("event.publish.button.leave"))))
                .build())));

        if (type.equals("private"))
            return privateEvent;
        return publicEvent;
    }
}
