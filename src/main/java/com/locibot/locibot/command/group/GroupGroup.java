package com.locibot.locibot.command.group;

import com.locibot.locibot.command.group.amongUs.AmongUs;
import com.locibot.locibot.command.group.clash.Clash;
import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;

import java.util.List;

public class GroupGroup extends BaseCmdGroup {
    public GroupGroup() {
        super(CommandCategory.FUN, CommandPermission.USER_GUILD, "group", "Group Commands", List.of(new Clash(), new AmongUs()));
    }
}
