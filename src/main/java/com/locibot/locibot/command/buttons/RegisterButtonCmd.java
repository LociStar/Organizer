package com.locibot.locibot.command.buttons;

import com.locibot.locibot.core.command.BaseCmdButton;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.object.ExceptionHandler;
import reactor.core.publisher.Mono;

public class RegisterButtonCmd extends BaseCmdButton {
    public RegisterButtonCmd() {
        super(CommandPermission.USER_GUILD, "registerButton");
    }

    @Override
    public Mono<?> execute(Context context) {
        return DatabaseManager.getGuilds().getDBMember(context.getGuildId(), context.getAuthorId()).flatMap(dbMember -> {
            boolean botRegister = dbMember.getBotRegister();
            return dbMember.setBotRegister(!botRegister)
                    .then(context.createFollowupMessageEphemeral(context.getEvent().getInteraction().getUser().getUsername() +
                            (botRegister ? " un-registered" : " registered") +
                            " to bot messages"))
                    .onErrorResume(err -> ExceptionHandler.handleCommandError(err, context)
                            .then(Mono.empty()));
        });
    }
}
