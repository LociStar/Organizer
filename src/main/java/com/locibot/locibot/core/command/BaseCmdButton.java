package com.locibot.locibot.core.command;

import discord4j.core.object.command.ApplicationCommandOption;

import java.util.List;

public abstract class BaseCmdButton extends BaseCmd {
    protected BaseCmdButton(CommandPermission permission, String buttonId) {
        super(CommandCategory.BUTTON, permission, null, buttonId, "button", ApplicationCommandOption.Type.UNKNOWN);
    }

    protected BaseCmdButton(CommandPermission permission, List<Requirement> requirements, String buttonId) {
        super(CommandCategory.BUTTON, permission, null, buttonId, "button", ApplicationCommandOption.Type.UNKNOWN);
    }
}
