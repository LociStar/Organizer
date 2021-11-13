package com.locibot.locibot.command.event_commands.buttons;

import com.locibot.locibot.core.command.BaseCmdButton;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.events_db.entity.DBEvent;
import com.locibot.locibot.database.events_db.entity.DBEventMember;
import reactor.core.publisher.Mono;

public class JoinEventButtonCmd extends BaseCmdButton {
    public JoinEventButtonCmd() {
        super(CommandPermission.USER_GUILD, "joinButton");
    }

    @Override
    public Mono<?> execute(Context context) {
        String eventName = context.getEvent().getInteraction().getCommandInteraction().orElseThrow().getCustomId().orElse("error_error").split("_")[1];
        return DatabaseManager.getEvents().getDBEvent(eventName).flatMapIterable(DBEvent::getMembers)
                .collectMap(DBEventMember::getId)
                .flatMap(dbMembers -> {
                            if (!dbMembers.containsKey(context.getAuthorId()))
                                return DatabaseManager.getEvents().getDBEvent(eventName).flatMap(dbEvent ->
                                        context.createFollowupMessageEphemeral("You have joined the event: " + "eventName")
                                                .then(context.getClient().getUserById(context.getAuthorId()).flatMap(user -> dbEvent.addMember(user, 1))));
                            return context.createFollowupMessageEphemeral("You have already joined the event!");
                        }
                );
    }
}
