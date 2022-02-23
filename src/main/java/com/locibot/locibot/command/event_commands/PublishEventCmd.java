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

public class PublishEventCmd extends BaseCmd {
    protected PublishEventCmd() {
        super(CommandCategory.EVENT, CommandPermission.USER_GUILD, "publish", "publish the event, so that everyone can join it. The event needs to be scheduled first.");
        this.addOption("event_title", "Event name", true, ApplicationCommandOption.Type.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        String title = context.getOptionAsString("event_title").orElseThrow();

        if (!DatabaseManager.getEvents().containsEvent(title))
            return context.createFollowupMessage(context.localize("event.publish.title.error").formatted(title));

        return DatabaseManager.getEvents().getDBEvent(title).flatMap(dbEvent -> {
            if (!dbEvent.isScheduled())
                return context.createFollowupMessage(context.localize("event.publish.schedule"));
            if (dbEvent.getOwner().getId().asLong() == context.getAuthor().getId().asLong())
                //noinspection ConstantConditions
                return context.getGuild().flatMap(guild -> guild.getMemberById(dbEvent.getOwner().getId())).flatMap(owner ->
                        context.createFollowupMessage(InteractionFollowupCreateSpec.builder()
                                .content("@everyone")
                                .addEmbed(EmbedCreateSpec.builder()
                                        .color(Color.BLUE)
                                        .title(context.localize("event.publish.title"))
                                        .thumbnail(dbEvent.getIcon() == null ? owner.getAvatarUrl() : dbEvent.getIcon())
                                        .description(context.localize("event.publish.join").formatted(dbEvent.getEventName()))
                                        .footer(EmbedCreateFields.Footer.of(context.localize("event.author").formatted(owner.getUsername()), owner.getAvatarUrl()))
                                        .addField(EmbedCreateFields.Field.of(context.localize("event.description"), dbEvent.getEventDescription() + "\n", false))
                                        .addField(EmbedCreateFields.Field.of(context.localize("event.publish.date"),
                                                context.localize("event.time").formatted(ZonedDateTime.ofInstant(
                                                        Instant.ofEpochSecond(dbEvent.getBean().getScheduledDate()),
                                                        ZoneId.of("Europe/Berlin")).format(DateTimeFormatter.ofPattern("dd.MM.yyyy, hh:mm"))),
                                                false))
                                        .build())
                                .addComponent(ActionRow.of(
                                        Button.success("joinButton_" + title, context.localize("event.publish.button.join")),
                                        Button.danger("leaveButton_" + title, context.localize("event.publish.button.leave"))))
                                .build()));
            return context.createFollowupMessage(context.localize("event.publish.restriction"));
        });
    }
}
