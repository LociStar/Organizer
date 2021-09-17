package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;

import java.util.List;

public class GroupEvent extends BaseCmdGroup {
    public GroupEvent() {
        super(CommandCategory.EVENT, CommandPermission.USER_GUILD, "event", "Event Commands",
                List.of()
        );
    }
}

