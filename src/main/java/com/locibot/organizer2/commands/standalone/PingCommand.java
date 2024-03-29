package com.locibot.organizer2.commands.standalone;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.command.CommandContext;
import com.locibot.organizer2.database.repositories.UserRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PingCommand implements SlashCommand {

    public PingCommand() {
    }

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        //We reply to the command with "Pong!" and make sure it is ephemeral (only the command user can see it)
        return context.getEvent().reply()
                .withEphemeral(true)
                .withContent("Pong!");
    }
}
