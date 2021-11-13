package com.locibot.locibot.core.command;

import discord4j.core.object.command.ApplicationCommandOption;

public  abstract class BaseCmdButton extends BaseCmd{
    protected BaseCmdButton(CommandPermission permission, String buttonId) {
        super(CommandCategory.BUTTON, permission, buttonId, "button", ApplicationCommandOption.Type.UNKNOWN);
    }
}
