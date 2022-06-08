package com.locibot.locibot.command.info;

import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CmdAnnotation;
import com.locibot.locibot.core.command.CommandCategory;

import java.util.List;

@CmdAnnotation
public class InfoGroup extends BaseCmdGroup {

    public InfoGroup() {
        super(CommandCategory.INFO, "Show specific information",
                List.of(new BotInfoCmd(), new ServerInfoCmd(), new UserInfoCmd()));
    }

}
