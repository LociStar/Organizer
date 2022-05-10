package com.locibot.locibot.command.event_commands.buttons;

import com.locibot.locibot.core.command.BaseCmdButton;
import com.locibot.locibot.core.command.ButtonAnnotation;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.events_db.entity.DBEvent;
import com.locibot.locibot.database.events_db.entity.DBEventMember;
import discord4j.core.object.entity.Message;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

@ButtonAnnotation
public class LeaveEventButtonCmd extends BaseCmdButton {
    public LeaveEventButtonCmd() {
        super(CommandPermission.USER_GUILD, "leaveButton");
    }

    @Override
    public Mono<?> execute(Context context) {
        String eventIdString = context.getEvent().getInteraction().getCommandInteraction().orElseThrow().getCustomId().orElse("error_error").split("_")[1];
        ObjectId objectId = new ObjectId(eventIdString);
        return DatabaseManager.getEvents().getDBEvent(objectId).flatMap(dbEvent ->
                DatabaseManager.getEvents().getDBEvent(objectId).flatMapIterable(DBEvent::getMembers).collectMap(DBEventMember::getUId).flatMap(members -> {
                    if (context.getAuthor().getId().asLong() == dbEvent.getOwner().getUId().asLong()) {
                        return context.createFollowupMessageEphemeral(context.localize("event.button.leave.error"));
                    }
                    if (members.containsKey(context.getAuthorId()))
                        return dbEvent.removeMember(members.get(context.getAuthorId()))
                                .then(context.createFollowupMessageEphemeral(context.localize("event.button.leave.left"))
                                        .then(informOwner(context, dbEvent)));
                    return context.createFollowupMessageEphemeral(context.localize("event.button.leave.left.error"));

                })
        );
    }

    @NotNull
    private Mono<Message> informOwner(Context context, DBEvent event) {
        return Mono.zip(context.getClient().getUserById(context.getAuthorId()),
                        context.getClient().getUserById(event.getOwner().getUId()))
                .flatMap(TupleUtils.function((declinedUser, owner) ->
                        owner.getPrivateChannel().flatMap(privateChannel -> privateChannel.createMessage(
                                context.localize("event.button.decline.declined.to").formatted(declinedUser.getMention(), event.getEventName())))));
    }
}
