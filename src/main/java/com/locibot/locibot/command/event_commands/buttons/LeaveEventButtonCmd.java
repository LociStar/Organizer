package com.locibot.locibot.command.event_commands.buttons;

import com.locibot.locibot.core.command.BaseCmdButton;
import com.locibot.locibot.core.command.ButtonAnnotation;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.events_db.entity.DBEvent;
import com.locibot.locibot.database.events_db.entity.DBEventMember;
import reactor.core.publisher.Mono;

@ButtonAnnotation
public class LeaveEventButtonCmd extends BaseCmdButton {
    public LeaveEventButtonCmd() {
        super(CommandPermission.USER_GUILD, "leaveButton");
    }

    @Override
    public Mono<?> execute(Context context) {
        String eventName = context.getEvent().getInteraction().getCommandInteraction().orElseThrow().getCustomId().orElse("error_error").split("_")[1];
        return DatabaseManager.getEvents().getDBEvent(eventName).flatMap(dbEvent ->
                DatabaseManager.getEvents().getDBEvent(eventName).flatMapIterable(DBEvent::getMembers).collectMap(DBEventMember::getId).flatMap(members -> {
                    if (context.getAuthor().getId().asLong() == dbEvent.getOwner().getId().asLong()) {
                        return context.createFollowupMessageEphemeral(context.localize("event.button.leave.error"));
                    }
                    if (members.containsKey(context.getAuthorId()))
                        return dbEvent.removeMember(members.get(context.getAuthorId()))
                                .then(context.createFollowupMessageEphemeral(context.localize("event.button.leave.left")));
                    return context.createFollowupMessageEphemeral(context.localize("event.button.leave.left.error"));

                })
        );
    }
}
