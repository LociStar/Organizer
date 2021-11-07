package com.locibot.locibot.command.event_commands.buttons;

import com.locibot.locibot.core.command.BaseCmdButton;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.events_db.entity.DBEvent;
import com.locibot.locibot.database.events_db.entity.DBEventMember;
import discord4j.core.object.entity.Message;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

public class AcceptButtonCmd extends BaseCmdButton {
    public AcceptButtonCmd() {
        super(CommandPermission.USER_GLOBAL, "acceptInviteButton");
    }

    @Override
    public Mono<?> execute(Context context) {
        System.out.println(context.getEvent().getInteraction().getCommandInteraction().orElseThrow());
        String eventTitle = context.getEvent().getInteraction().getCommandInteraction().orElseThrow().getCustomId().orElse("error_error").split("_")[1];
        //return context.createFollowupMessage(context.getEvent().getInteraction().getCommandInteraction().get().getCustomId().get());
        if (!DatabaseManager.getEvents().containsEvent(eventTitle)) {
            return context.createFollowupMessage("Nice try! But you can't join an event that does not exist!");
        }
        return DatabaseManager.getEvents().getDBEvent(eventTitle).flatMap(event -> {
            for (DBEventMember member : event.getMembers()) {
                if (member.getId().asLong() == context.getAuthorId().asLong() && member.getAccepted() == 0) {
                    System.out.println(member.getAccepted());
                    return context.createFollowupMessage("You have accepted the invitation! Have fun!").then(member.updateAccepted(1))
                            .then(informOwner(context, event));
                } else if (member.getBean().getId().equals(context.getAuthorId().asLong()) && member.getAccepted() == 1){
                   return context.createFollowupMessage("You have already accepted the invitation! Have fun!");
                }
            }
            return context.createFollowupMessage("Nice try! But you are not invited to this event...");
        });
    }

    @NotNull
    private Mono<Message> informOwner(Context context, DBEvent event) {
        return Mono.zip(context.getClient().getUserById(context.getAuthorId()),
                        context.getClient().getUserById(event.getOwner().getId()))
                .flatMap(TupleUtils.function((user, owner) ->
                        owner.getPrivateChannel().flatMap(privateChannel -> privateChannel.createMessage(
                                user.getUsername() + " accepted your invitation to the event: " + event.getEventName() + "!"))));
    }
}
