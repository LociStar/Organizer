package com.locibot.locibot.command.register;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.object.ExceptionHandler;
import reactor.core.publisher.Mono;

public class RegisterDm extends BaseCmd {
    protected RegisterDm() {
        super(CommandCategory.REGISTER, CommandPermission.USER_GUILD, "dm", "register to bot DMs");
    }

    @Override
    public Mono<?> execute(Context context) {
        return DatabaseManager.getUsers().getDBUser(context.getAuthorId()).flatMap(user -> {
            boolean botRegister = user.getBean().getDm();
            return user.setDM(!botRegister)
                    .then(context.createFollowupMessageEphemeral(context.getEvent().getInteraction().getUser().getUsername() +
                            (botRegister ? " un-registered" : " registered") +
                            " to bot messages"))
                    .onErrorResume(err -> ExceptionHandler.handleCommandError(err, context)
                            .then(Mono.empty()));
        });
    }
}
