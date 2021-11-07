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

public class DeclineButtonCmd extends BaseCmdButton {
    public DeclineButtonCmd() {
        super(CommandPermission.USER_GLOBAL, "declineInviteButton");
    }

    @Override
    public Mono<?> execute(Context context) {
        String eventName = context.getEvent().getInteraction().getCommandInteraction().orElseThrow().getCustomId().orElse("error_error").split("_")[1];
        Mono<Message> context1 = disableIfNoEvent(context, eventName);
        if (context1 != null) return context1;
        return DatabaseManager.getEvents().getDBEvent(eventName).flatMap(group -> {
                    for (DBEventMember member : group.getMembers()) {
                        if (member.getId().asLong() == context.getAuthorId().asLong() && member.getAccepted() != 2) {
                            return member.updateAccepted(2)
                                    .then(context.createFollowupMessage("You have declined the invitation!"))
                                    .then(informOwner(context, group, member));
                        }else if (member.getId().asLong() == context.getAuthorId().asLong() && member.getAccepted() == 2){
                            return context.createFollowupMessage("You have already declined the invitation!");
                        }
                    }
                    return context.createFollowupMessage("Nice try! But you are not invited to this event...");
                }
        );
    }

    @Nullable
    private Mono<Message> disableIfNoEvent(Context context, String eventTitle) {
        if (!DatabaseManager.getEvents().containsEvent(eventTitle)) {
            ActionRow a = (ActionRow) ActionRow.fromData(context.getEvent().getInteraction().getMessage().get().getComponents().get(0).getData());
            Button accept = (Button) Button.fromData(a.getChildren().get(0).getData());
            Button decline = (Button) Button.fromData(a.getChildren().get(1).getData());
            return context.createFollowupMessage("Sry! But you cannot decline an event invitation that does not exist!").then(context.getEvent().getInteraction().getMessage().get().edit().withComponents(
                    ActionRow.of(
                            accept.disabled(), decline.disabled()
                    )));
        }
        return null;
    }

    @NotNull
    private Mono<Message> informOwner(Context context, DBEvent event, DBEventMember dbEventMember) {
        return Mono.zip(context.getClient().getUserById(dbEventMember.getId()),
                        context.getClient().getUserById(event.getOwner().getId()))
                .flatMap(TupleUtils.function((declinedUser, owner) ->
                        owner.getPrivateChannel().flatMap(privateChannel -> privateChannel.createMessage(
                                declinedUser.getUsername() + " declined your invitation to " + event.getEventName() + "!"))));
    }
}
