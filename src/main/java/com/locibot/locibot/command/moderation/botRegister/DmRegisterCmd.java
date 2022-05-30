package com.locibot.locibot.command.moderation.botRegister;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

public class DmRegisterCmd extends BaseCmd {
    public DmRegisterCmd() {
        super(CommandCategory.MODERATION, CommandPermission.ADMIN, "dm_register_button", "Create a button to register to DMs from the bot");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getClient().getSelf().flatMap(user ->
                context.createFollowupButton(
                        EmbedCreateSpec.builder()
                                .title("DM subscription")
                                .description("""
                                        Click **register** if you allow the bot to send you direct messages.
                                        This is necessary for all __event__ commands.""")
                                .thumbnail(user.getAvatarUrl())
                                .color(Color.RED)
                                .footer(EmbedCreateFields.Footer.of("Click it again, if you want to un-register DMs from the bot.", ""))
                                .build()
                        , Button.danger("registerButton", "register")));
    }
}
