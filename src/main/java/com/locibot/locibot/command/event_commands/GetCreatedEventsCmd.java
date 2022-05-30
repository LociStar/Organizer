package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

public class GetCreatedEventsCmd extends BaseCmd {
    protected GetCreatedEventsCmd() {
        super(CommandCategory.EVENT, CommandPermission.USER_GUILD, "get_events", "Get all of your created events");
    }

    @Override
    public Mono<?> execute(Context context) {
        StringBuilder name = new StringBuilder();
        StringBuilder count = new StringBuilder();
        StringBuilder scheduled = new StringBuilder();
        EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder()
                .title("Your Events")
                //.footer(EmbedCreateFields.Footer.of("If you need more information about a group, use \"/info groups <groupName>\"", "https://img.icons8.com/cotton/344/info--v3.png"))
                .color(Color.GREEN)
                .thumbnail("https://img.icons8.com/cotton/344/info--v3.png");

        DatabaseManager.getEvents().getAllEvents(context.getAuthorId()).flatMap(dbEvent -> {
            name.append(dbEvent.getEventName()).append(System.getProperty("line.separator"));
            count.append(dbEvent.getMembers().size()).append(System.getProperty("line.separator"));
            String time = "---";
            if (dbEvent.getBean().getScheduledDate() != null)
                time = "<t:" + dbEvent.getBean().getScheduledDate() + ">";
            scheduled.append(time).append(System.getProperty("line.separator"));
            return Mono.empty();
        }).collectList().block(); //TODO: Make method non-blocking

        //return getData(name, count, scheduled, context).then(setup(name, count, scheduled, embedCreateSpec)).then(context.createFollowupMessage(embedCreateSpec.build()));
        return setup(name, count, scheduled, embedCreateSpec).then(context.createFollowupMessage(embedCreateSpec.build()));
    }

    public Mono<?> setup(StringBuilder name, StringBuilder count, StringBuilder scheduled, EmbedCreateSpec.Builder embedCreateSpec) {
        if (name.isEmpty() || count.isEmpty()) {
            name.append("---");
            count.append("---");
            scheduled.append("---");
        }
        embedCreateSpec.addFields(EmbedCreateFields.Field.of("Name", name.toString(), true),
                EmbedCreateFields.Field.of("Member Count", count.toString(), true),
                EmbedCreateFields.Field.of("Scheduled", scheduled.toString(), true));
        return Mono.empty();
    }
}
