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
public class DeclineButtonCmd extends BaseCmdButton {
    public DeclineButtonCmd() {
        super(CommandPermission.USER_GLOBAL, "declineInviteButton");
    }

    @Override
    public Mono<?> execute(Context context) {
        String eventIdString = context.getEvent().getInteraction().getCommandInteraction().orElseThrow().getCustomId().orElse("error_error").split("_")[1];
        ObjectId objectId = new ObjectId(eventIdString);
        Mono<Message> context1 = EventUtil.disableIfNoEvent(context, objectId);
        if (context1 != null) return context1;
        return DatabaseManager.getEvents().getDBEvent(objectId).flatMap(group -> {
                    for (DBEventMember member : group.getMembers()) {
                        if (member.getUId().asLong() == context.getAuthorId().asLong() && member.getAccepted() != 2) {
                            return member.updateAccepted(2)
                                    .then(context.createFollowupMessageEphemeral(context.localize("event.button.decline.declined")))
                                    .then(informOwner(context, group, member));
                        } else if (member.getUId().asLong() == context.getAuthorId().asLong() && member.getAccepted() == 2) {
                            return context.getEvent().deferReply().onErrorResume(throwable -> Mono.empty()).then(context.createFollowupMessageEphemeral(context.localize("event.button.decline.declined.already")));
                        }
                    }
                    return context.getEvent().deferReply().onErrorResume(throwable -> Mono.empty()).then(context.createFollowupMessageEphemeral(context.localize("event.button.decline.error")));
                }
        );
    }

    @NotNull
    private Mono<Message> informOwner(Context context, DBEvent event, DBEventMember dbEventMember) {
        return Mono.zip(context.getClient().getUserById(dbEventMember.getUId()),
                        context.getClient().getUserById(event.getOwner().getUId()))
                .flatMap(TupleUtils.function((declinedUser, owner) ->
                        owner.getPrivateChannel().flatMap(privateChannel -> privateChannel.createMessage(
                                context.localize("event.button.decline.declined.to").formatted(declinedUser.getMention(), event.getEventName())))));
    }
}
