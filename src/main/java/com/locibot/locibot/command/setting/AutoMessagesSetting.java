package com.locibot.locibot.command.setting;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.*;
import com.locibot.locibot.database.guilds.bean.setting.AutoMessageBean;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class AutoMessagesSetting extends BaseCmd {

    public AutoMessagesSetting() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN,
                "auto_messages", "Manage auto-message(s) on user join/leave");

        this.addOption("action", "Whether to enable or disable automatic messages", true,
                ApplicationCommandOption.Type.STRING, DiscordUtil.toOptions(Action.class));
        this.addOption("type", "The type of automatic message to configure", true,
                ApplicationCommandOption.Type.STRING, DiscordUtil.toOptions(Type.class));
        this.addOption("message", "The message to automatically send", false, ApplicationCommandOption.Type.STRING);
        this.addOption("channel", "The channel in which send the automatic message", false, ApplicationCommandOption.Type.CHANNEL);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();
        final Type type = context.getOptionAsEnum(Type.class, "type").orElseThrow();

        return this.updateMessage(context, action, type);
    }

    private Mono<Message> updateMessage(Context context, Action action, Type type) {
        final String typeStr = type == Type.JOIN_MESSAGE ? "join" : "leave";
        final Setting setting = type == Type.JOIN_MESSAGE ? Setting.AUTO_JOIN_MESSAGE : Setting.AUTO_LEAVE_MESSAGE;
        switch (action) {
            case ENABLE:
                final Optional<String> messageOpt = context.getOptionAsString("message");
                if (messageOpt.isEmpty()) {
                    return Mono.error(new CommandException(context.localize("automessage.missing.message")));
                }
                final String message = messageOpt.orElseThrow();
                return context.getOptionAsChannel("channel")
                        .switchIfEmpty(Mono.error(new CommandException(context.localize("automessage.missing.channel"))))
                        .ofType(TextChannel.class)
                        .switchIfEmpty(Mono.error(new CommandException(context.localize("automessage.exception.channel.type"))))
                        .flatMap(channel -> context.getDbGuild()
                                .updateSetting(setting, new AutoMessageBean(message, channel.getId().asString()))
                                .then(context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("automessage." + typeStr + ".enabled")
                                        .formatted(message, channel.getMention()))));

            case DISABLE:
                return context.getDbGuild().removeSetting(setting)
                        .then(context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("automessage." + typeStr + ".disabled")));

            default:
                return Mono.error(new IllegalStateException());
        }
    }

    private enum Action {
        ENABLE, DISABLE
    }

    private enum Type {
        JOIN_MESSAGE, LEAVE_MESSAGE
    }

}
