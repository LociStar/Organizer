package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.*;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.events_db.entity.DBEvent;
import com.locibot.locibot.database.events_db.entity.DBEventMember;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class CreateEventCmd extends BaseCmd {
    protected CreateEventCmd() {
        super(CommandCategory.EVENT, CommandPermission.USER_GUILD, "create", "create a new event", Requirement.DM);
        this.addOption("title", "event name", true, ApplicationCommandOption.Type.STRING);
        this.addOption("description", "add a brief event description", false, ApplicationCommandOption.Type.STRING);
        this.addOption("icon", "icon image url", false, ApplicationCommandOption.Type.STRING);
        for (int i = 0; i < 10; i++) {
            this.addOption("member_" + (i + 1), "Add a member", false, ApplicationCommandOption.Type.USER);
        }
    }

    @Override
    public Mono<?> execute(Context context) {

        if (DatabaseManager.getGuilds().getDBMember(context.getGuildId(), context.getAuthorId()) == null)
            return context.createFollowupMessageEphemeral(context.localize("event.create.restriction"));

        String eventName = context.getOptionAsString("title").orElse("error");
        DBEvent event = new DBEvent(eventName, context.getOptionAsString("description").orElse(null), context.getOptionAsString("icon").orElse(null));
        List<Mono<User>> users = new ArrayList<>();

        for (int i = 1; i < 11; i++) {
            users.add(context.getOptionAsUser("member_" + i));
        }

        Mono<?> insert = event.insertOne().flatMap(insertOneResult -> DatabaseManager.getUsers().getDBUser(context.getAuthorId())
                .flatMap(dbUser -> dbUser.addEvent(insertOneResult.getInsertedId().asObjectId().getValue()))
                // get EventId
                .then(DatabaseManager.getEvents().getDBEvent(insertOneResult.getInsertedId().asObjectId().getValue()).flatMap(dbEvent ->
                        // add Owner
                        new DBEventMember(context.getEvent().getInteraction().getUser().getId().asLong(), dbEvent.getId(), 1, true).insert()
                                // add Members
                                .thenMany(Flux.concat(users).flatMap(user -> new DBEventMember(user.getId().asLong(), dbEvent.getId(), 0, false).insert()))
                                .then(context.createFollowupMessageEphemeral(context.localize("event.create.success"))))));

        return DatabaseManager.getEvents().getDBEvent(context.getAuthorId(), eventName).switchIfEmpty(insert.then(Mono.empty())).flatMap(dbEvent -> {
            if (dbEvent.getId() != null) {
                System.out.println(dbEvent.getEventName());
                return context.createFollowupMessageEphemeral(context.localize("event.create.error"));
            }
            System.out.println("test");
            return insert;
        });
    }
}
