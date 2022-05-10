package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.events_db.entity.DBEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class DeleteEventCmd extends BaseCmd {
    protected DeleteEventCmd() {
        super(CommandCategory.EVENT, CommandPermission.USER_GUILD, "delete", "delete an event");
        this.addOption("title", "event title", true, ApplicationCommandOption.Type.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {

        Mono<List<ObjectId>> idList = DatabaseManager.getUsers().getDBUser(context.getAuthorId()).map(dbUser -> dbUser.getBean().getEvents());

        Flux<DBEvent> events = idList.flatMapMany(Flux::fromIterable).flatMap(objectId -> DatabaseManager.getEvents().getDBEvent(objectId));

        return Mono.just(context.getOptionAsString("title").orElse("")).map(eventName ->
                events.flatMap(dbEvent -> {
                    if (dbEvent.getEventName().equals(eventName) && dbEvent.getOwner().getUId().equals(context.getAuthorId()))
                        return dbEvent.delete().then(context.createFollowupMessage(context.localize("event.delete.success").formatted(eventName)));
                    return context.createFollowupMessage(context.localize("event.delete.restriction").formatted(eventName));
                }).switchIfEmpty(context.createFollowupMessage(context.localize("event.delete.empty").formatted(eventName))).subscribe()
        );
    }
}
