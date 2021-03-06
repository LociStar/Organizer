package com.locibot.locibot.command.setting;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.*;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.FormatUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Role;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AutoRolesSetting extends BaseCmd {

    public AutoRolesSetting() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN,
                "auto_roles", "Manage auto assigned role(s)");

        this.addOption("action", "Whether to add or remove a role from the auto ones", true,
                ApplicationCommandOption.Type.STRING, DiscordUtil.toOptions(Action.class));
        this.addOption("role1", "The first role", true,
                ApplicationCommandOption.Type.ROLE);
        this.addOption("role2", "The second role", false,
                ApplicationCommandOption.Type.ROLE);
        this.addOption("role3", "The third role", false,
                ApplicationCommandOption.Type.ROLE);
        this.addOption("role4", "The fourth role", false,
                ApplicationCommandOption.Type.ROLE);
        this.addOption("role5", "The fifth role", false,
                ApplicationCommandOption.Type.ROLE);
    }

    private static Mono<?> checkPermissions(Context context, Set<Snowflake> roleIds) {
        return context.getChannel()
                .flatMap(channel -> DiscordUtil.requirePermissions(channel, Permission.MANAGE_ROLES))
                .then(context.getClient().getSelfMember(context.getGuildId()))
                .filterWhen(self -> self.hasHigherRoles(roleIds))
                .switchIfEmpty(Mono.error(new CommandException(context.localize("autoroles.exception.higher"))));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();
        final Flux<Role> roles = Flux.fromStream(IntStream.range(1, 6).boxed())
                .flatMap(index -> context.getOptionAsRole("role%d".formatted(index)));

        return roles.collectList()
                .flatMap(mentionedRoles -> {
                    final Set<Snowflake> roleIds = mentionedRoles.stream()
                            .map(Role::getId)
                            .collect(Collectors.toSet());
                    final Set<Snowflake> autoRoleIds = context.getDbGuild().getSettings().getAutoRoleIds();
                    final String roleStr = FormatUtil.format(mentionedRoles,
                            role -> "`@%s`".formatted(role.getName()), ", ");

                    switch (action) {
                        case ADD:
                            return AutoRolesSetting.checkPermissions(context, roleIds)
                                    .doOnNext(__ -> autoRoleIds.addAll(roleIds))
                                    .then(context.getDbGuild().updateSetting(Setting.AUTO_ROLES, autoRoleIds))
                                    .then(context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("autoroles.added")
                                            .formatted(roleStr)));
                        case REMOVE:
                            autoRoleIds.removeAll(roleIds);
                            return context.getDbGuild().updateSetting(Setting.AUTO_ROLES, autoRoleIds)
                                    .then(context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("autoroles.removed")
                                            .formatted(roleStr)));
                        default:
                            return Mono.error(new IllegalStateException());
                    }
                });
    }

    private enum Action {
        ADD, REMOVE
    }

}
