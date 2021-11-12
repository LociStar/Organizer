package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class PublishEventCmd extends BaseCmd {
    protected PublishEventCmd() {
        super(CommandCategory.EVENT, CommandPermission.USER_GUILD, "publish", "publish the event, so that everyone can join it. The event needs to be scheduled first.");
        this.addOption("event_title", "Event name", true, ApplicationCommandOption.Type.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        String title = context.getOptionAsString("event_title").orElseThrow();

        if (!DatabaseManager.getEvents().containsEvent(title))
            return context.createFollowupMessage("No event found with the title: " + title);

        return DatabaseManager.getEvents().getDBEvent(title).flatMap(dbEvent -> {
            if (!dbEvent.isScheduled())
                return context.createFollowupMessage("The event must first be scheduled to be published!");
            if (dbEvent.getOwner().getId().asLong() == context.getAuthor().getId().asLong())
                return context.getGuild().flatMap(guild -> guild.getMemberById(dbEvent.getOwner().getId())).flatMap(owner ->
                        context.createFollowupMessage(InteractionFollowupCreateSpec.builder()
                                .content("@everyone")
                                .addEmbed(EmbedCreateSpec.builder()
                                        .color(Color.BLUE)
                                        .title("Event")
                                        .thumbnail(dbEvent.getIcon() == null ? owner.getAvatarUrl() : dbEvent.getIcon())
                                        .description("Join the event **%s**".formatted(dbEvent.getEventName()))
                                        .footer(EmbedCreateFields.Footer.of("Author: %s".formatted(owner.getUsername()), owner.getAvatarUrl()))
                                        .addField(EmbedCreateFields.Field.of("Description", dbEvent.getEventDescription() + "\n", false))
                                        .addField(EmbedCreateFields.Field.of("Date",
                                                "%s Uhr".formatted(ZonedDateTime.ofInstant(
                                                        Instant.ofEpochSecond(dbEvent.getBean().getScheduledDate()),
                                                        ZoneId.of("Europe/Berlin")).format(DateTimeFormatter.ofPattern("dd.MM.yyyy, hh:mm"))),
                                                false))
                                        .build())
                                .addComponent(ActionRow.of(
                                        Button.success("joinButton_" + title, "Join"),
                                        Button.danger("leaveButton_" + title, "Leave")))
                                .build()));
            return context.createFollowupMessage("You are not the event owner, so you can not publish it.");
        });
    }
}
