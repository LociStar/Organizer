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

public class BotRegisterSetup extends BaseCmd {
    public BotRegisterSetup() {
        super(CommandCategory.MODERATION, CommandPermission.ADMIN, "bot_server_setup", "register to weather");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getClient().getSelf().flatMap(user ->
                context.createFollowupButton(
                        EmbedCreateSpec.builder()
                                .description("""
                                        @everyone
                                        Click **register** if you allow the bot to send you private messages.
                                        This is necessary for all __event__ commands.""")
                                .thumbnail(user.getAvatarUrl())
                                .color(Color.RED)
                                .footer(EmbedCreateFields.Footer.of("Click it again, if you want to un-register to bot messages.", ""))
                                .build()
                        , Button.danger("registerButton", "register")));
    }
}
