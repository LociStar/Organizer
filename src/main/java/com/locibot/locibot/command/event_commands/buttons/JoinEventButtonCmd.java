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
public class JoinEventButtonCmd extends BaseCmdButton {
    public JoinEventButtonCmd() {
        super(CommandPermission.USER_GUILD, "joinButton");
    }

    @Override
    public Mono<?> execute(Context context) {
        String eventIdString = context.getEvent().getInteraction().getCommandInteraction().orElseThrow().getCustomId().orElse("error_error").split("_")[1];
        ObjectId objectId = new ObjectId(eventIdString);
        return DatabaseManager.getEvents().getDBEvent(objectId).flatMapIterable(DBEvent::getMembers)
                .collectMap(DBEventMember::getUId)
                .flatMap(dbMembers -> {
                            if (!dbMembers.containsKey(context.getAuthorId()))
                                return DatabaseManager.getEvents().getDBEvent(objectId).flatMap(dbEvent ->
                                        context.createFollowupMessageEphemeral(context.localize("event.button.join.joined").formatted(dbEvent.getEventName()))
                                                .then(context.getClient().getUserById(context.getAuthorId()).flatMap(user -> dbEvent.addMember(user, 1)))
                                                .then(informOwner(context, dbEvent)));
                            return context.createFollowupMessageEphemeral(context.localize("event.button.join.joined.already"));
                        }
                );
    }

    @NotNull
    private Mono<Message> informOwner(Context context, DBEvent event) {
        return Mono.zip(context.getClient().getUserById(context.getAuthorId()),
                        context.getClient().getUserById(event.getOwner().getUId()))
                .flatMap(TupleUtils.function((user, owner) ->
                        owner.getPrivateChannel().flatMap(privateChannel -> privateChannel.createMessage(
                                context.localize("event.button.accept.accepted.to").formatted(user.getMention(), event.getEventName())))));
    }
}
