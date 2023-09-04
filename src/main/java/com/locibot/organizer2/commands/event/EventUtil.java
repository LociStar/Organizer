package com.locibot.organizer2.commands.event;

import com.locibot.organizer2.core.CommandContext;
import com.locibot.organizer2.database.tables.Event;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

public class EventUtil {
    public static EmbedCreateSpec createEventEmbed(String name, String description, String icon, Event event, CommandContext<?> context) {
        return EmbedCreateSpec.builder()
                .color(Color.BLUE)
                .title(name)
                .thumbnail(icon.isBlank() ? context.getAuthor().getAvatarUrl() : icon)
                .footer(EmbedCreateFields.Footer.of(context.localize("event.author").formatted(context.getAuthor().getUsername()), context.getAuthor().getAvatarUrl()))
                .addField(EmbedCreateFields.Field.of(context.localize("event.description"), description + "\n", false))
                .addField(EmbedCreateFields.Field.of(context.localize("event.publish.date"),
                        "<t:" + event.getTimestamp() + ">",
                        false))
                .build();
    }
}
