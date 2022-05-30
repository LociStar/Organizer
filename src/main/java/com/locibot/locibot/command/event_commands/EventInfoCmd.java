package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

public class EventInfoCmd extends BaseCmd {
    protected EventInfoCmd() {
        super(CommandCategory.EVENT, CommandPermission.USER_GUILD, "event_info", "Get info of the event");
        this.addOption("name", "event name", true, ApplicationCommandOption.Type.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        return DatabaseManager.getEvents().getInvitedDBEvent(context.getAuthorId(), context.getOptionAsString("name").orElseThrow())
                .switchIfEmpty(context.createFollowupMessageEphemeral("Event not found").then(Mono.empty()))
                .flatMap(dbEvent -> context.getClient().getUserById(dbEvent.getOwner().getUId()).flatMap(owner -> {
                    Long time = dbEvent.getBean().getScheduledDate();
                    return context.getEvent().deferReply().withEphemeral(true).then(context.createFollowupMessage(InteractionFollowupCreateSpec.builder()
                            .ephemeral(true)
                            .addEmbed(EmbedCreateSpec.builder()
                                    .title("Event: " + dbEvent.getEventName())
                                    .description(dbEvent.getEventDescription())
                                    .author(owner.getUsername(), "", owner.getAvatarUrl())
                                    .color(Color.BLUE)
                                    .addField(EmbedCreateFields.Field.of("Date & Time", time == null ? "---" : ("<t:" + time + ">"), true))
                                    .build())
                            .build()));
                }));
    }
}
