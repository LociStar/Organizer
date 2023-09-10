package com.locibot.organizer2.commands.event;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.command.CommandContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.ZoneId;

@Component
public class TimeZoneEventCommand implements SlashCommand {
    @Override
    public String getName() {
        return "event time_zone";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        System.out.println("EXECUTING");
        String zoneId = context.getOptionAsString("zone_id").orElseThrow();

        return context.getUserRepository().setZoneId(context.getAuthorId().asLong(), ZoneId.of(zoneId))
                .then(context.getEvent().reply(context.localize("time.zone.success").formatted(zoneId)).withEphemeral(true));
    }
}
