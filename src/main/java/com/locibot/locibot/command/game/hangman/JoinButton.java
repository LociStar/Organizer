package com.locibot.locibot.command.game.hangman;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

public class JoinButton extends BaseCmd {
    public JoinButton() {
        super(CommandCategory.EVENT, CommandPermission.USER_GUILD, "join", "joinButton");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(InteractionFollowupCreateSpec.builder()
                .content("Button")
                .addComponent(ActionRow.of(Button.success("joinHangman", "join")))
                .build());
    }
}
