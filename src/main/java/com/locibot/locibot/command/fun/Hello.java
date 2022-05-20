package com.locibot.locibot.command.fun;

import com.locibot.locibot.core.command.*;
import discord4j.core.object.command.ApplicationCommandOption;
import reactor.core.publisher.Mono;

@CmdAnnotation
public class Hello extends BaseCmd {
    public Hello() {
        super(CommandCategory.FUN, CommandPermission.USER_GLOBAL, "hello", "You might get greeted", ApplicationCommandOption.Type.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getEvent().getInteractionResponse().createFollowupMessage("Hello");
    }
}
