package com.locibot.organizer2.commands.util;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.CommandContext;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DeleteUserDataUtilCommand implements SlashCommand {
    @Override
    public String getName() {
        return "util delete_user_data";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        return context.getEvent().reply(InteractionApplicationCommandCallbackSpec.builder()
                .ephemeral(true)
                .content("Are you sure, you want to delete all your userdata?")
                .addComponent(ActionRow.of(
                        Button.danger("deleteUserData_" + context.getEvent().getInteraction().getId(), context.localize("Yes")))
                )
                .build());
    }
}
