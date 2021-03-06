package com.locibot.locibot.command.donator;

import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CmdAnnotation;
import com.locibot.locibot.core.command.CommandCategory;

import java.util.List;

@CmdAnnotation
public class DonatorGroup extends BaseCmdGroup {

    public DonatorGroup() {
        super(CommandCategory.DONATOR, "Donator commands",
                List.of(new ActivateRelicCmd(), new RelicStatusCmd()));
    }

}
