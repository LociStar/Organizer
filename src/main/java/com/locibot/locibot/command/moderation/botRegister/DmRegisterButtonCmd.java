package com.locibot.locibot.command.moderation.botRegister;

import com.locibot.locibot.core.command.BaseCmdButton;
import com.locibot.locibot.core.command.ButtonAnnotation;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.object.ExceptionHandler;
import reactor.core.publisher.Mono;

@ButtonAnnotation
public class DmRegisterButtonCmd extends BaseCmdButton {
    public DmRegisterButtonCmd() {
        super(CommandPermission.USER_GUILD, "registerButton");
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
