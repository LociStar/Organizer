package com.locibot.locibot.command.event_commands.buttons;

import com.locibot.locibot.command.event_commands.EventUtil;
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
public class AcceptButtonCmd extends BaseCmdButton {
    public AcceptButtonCmd() {
        super(CommandPermission.USER_GLOBAL, "acceptInviteButton");
    }

    @Override
    public Mono<?> execute(Context context) {
        String eventIdString = context.getEvent().getInteraction().getCommandInteraction().orElseThrow().getCustomId().orElse("error_error").split("_")[1];
        ObjectId objectId = new ObjectId(eventIdString);
        Mono<Message> context1 = EventUtil.disableIfNoEvent(context, objectId);
        if (context1 != null) return context1;
        return DatabaseManager.getEvents().getDBEvent(objectId).flatMap(event -> {
            for (DBEventMember member : event.getMembers()) {
                if (member.getUId().asLong() == context.getAuthorId().asLong() && member.getAccepted() != 1) {
                    return context.getEvent().deferReply().onErrorResume(throwable -> Mono.empty()).then(context.createFollowupMessageEphemeral(context.localize("event.button.accept.accept")).then(member.updateAccepted(1))
                            .then(informOwner(context, event)));
                } else if (member.getBean().getId().equals(context.getAuthorId().asLong()) && member.getAccepted() == 1) {
                    return context.getEvent().deferReply().onErrorResume(throwable -> Mono.empty()).then(context.createFollowupMessageEphemeral(context.localize("event.button.accept.accepted")));
                }
            }
            return context.getEvent().deferReply().onErrorResume(throwable -> Mono.empty()).then(context.createFollowupMessageEphemeral(context.localize("event.button.accept.accept.error")));
        });
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
