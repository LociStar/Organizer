package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CmdAnnotation;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;

import java.util.List;

@CmdAnnotation
public class EventGroup extends BaseCmdGroup {
    public EventGroup() {
        super(CommandCategory.EVENT, CommandPermission.USER_GUILD, "Event Commands",
                List.of(new CreateEventCmd(), new DeleteEventCmd(), new ScheduleEventCmd(), new AddUserEventCmd(),
                        new PublishEventCmd(), new TimeZoneEventCmd(), new GetCreatedEvents(), new GetInvitedEvents())
        );
    }
}

