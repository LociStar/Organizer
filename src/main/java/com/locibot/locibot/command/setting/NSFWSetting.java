package com.locibot.locibot.command.setting;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.TextChannelEditSpec;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

public class NSFWSetting extends BaseCmd {

    public NSFWSetting() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN,
                "nsfw", "Manage current channel's NSFW state");

        this.addOption(option -> option.name("action")
                .description("Change the NSFW state of the server")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .choices(DiscordUtil.toOptions(Action.class)));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();

        return context.getChannel()
                .cast(TextChannel.class)
                .flatMap(channel -> DiscordUtil.requirePermissions(channel, Permission.MANAGE_CHANNELS)
                        .then(Mono.fromSupplier(() -> switch (action) {
                            case TOGGLE -> !channel.isNsfw();
                            case ENABLE -> true;
                            case DISABLE -> false;
                        }))
                        .flatMap(nsfw -> channel.edit(TextChannelEditSpec.builder().nsfw(nsfw).build())))
                .map(channel -> channel.isNsfw()
                        ? context.localize("setting.nsfw.nsfw").formatted(channel.getMention())
                        : context.localize("setting.nsfw.sfw").formatted(channel.getMention()))
                .flatMap(message -> context.createFollowupMessage(Emoji.CHECK_MARK, message));
    }

    private enum Action {
        TOGGLE, ENABLE, DISABLE
    }

}
