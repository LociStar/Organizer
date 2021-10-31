package com.locibot.locibot.command.moderation;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

public class BotRegisterSetup extends BaseCmd {
    protected BotRegisterSetup() {
        super(CommandCategory.MODERATION, CommandPermission.ADMIN, "bot_server_setup", "register to weather");
    }

    @Override
    public Mono<?> execute(Context context) {
        //return context.reply(Emoji.CHECK_MARK, "test");
        return context.createFollowupButton("test", "hello");
    }
}
