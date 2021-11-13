package com.locibot.locibot.command.event_commands.buttons;

import com.locibot.locibot.core.command.BaseCmdButton;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.events_db.entity.DBEvent;
import com.locibot.locibot.database.events_db.entity.DBEventMember;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

public class AcceptButtonCmd extends BaseCmdButton {
    public AcceptButtonCmd() {
        super(CommandPermission.USER_GLOBAL, "acceptInviteButton");
    }

    @Override
    public Mono<?> execute(Context context) {
        String eventTitle = context.getEvent().getInteraction().getCommandInteraction().orElseThrow().getCustomId().orElse("error_error").split("_")[1];
        Mono<Message> context1 = disableIfNoEvent(context, eventTitle);
        if (context1 != null) return context1;
        return DatabaseManager.getEvents().getDBEvent(eventTitle).flatMap(event -> {
            for (DBEventMember member : event.getMembers()) {
                if (member.getId().asLong() == context.getAuthorId().asLong() && member.getAccepted() != 1) {
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

    @Nullable
    private Mono<Message> disableIfNoEvent(Context context, String eventTitle) {
        if (!DatabaseManager.getEvents().containsEvent(eventTitle)) {
            ActionRow a = (ActionRow) ActionRow.fromData(context.getEvent().getInteraction().getMessage().orElseThrow().getComponents().get(0).getData());
            Button accept = (Button) Button.fromData(a.getChildren().get(0).getData());
            Button decline = (Button) Button.fromData(a.getChildren().get(1).getData());
            return context.createFollowupMessage("Nice try! But you can't join an event that does not exist!").then(context.getEvent().getInteraction().getMessage().get().edit().withComponents(
                    ActionRow.of(
                            accept.disabled(), decline.disabled()
                    )));
        }
        return null;
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
