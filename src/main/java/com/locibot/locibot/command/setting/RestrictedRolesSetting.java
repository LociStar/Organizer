package com.locibot.locibot.command.setting;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.*;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.EnumUtil;
import com.locibot.locibot.utils.FormatUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Role;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RestrictedRolesSetting extends BaseCmd {

    public RestrictedRolesSetting() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN,
                "restricted_roles", "Restrict commands to specific roles");

        this.addOption(option -> option.name("action")
                .description("Whether to add or remove a role from the restricted ones")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .choices(DiscordUtil.toOptions(Action.class)));
        this.addOption(option -> option.name("type")
                .description("Restrict a command or a category")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .choices(DiscordUtil.toOptions(Type.class)));
        this.addOption(option -> option.name("name")
                .description("The name of the command/category")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue()));
        this.addOption(option -> option.name("role")
                .description("The role")
                .required(true)
                .type(ApplicationCommandOption.Type.ROLE.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();
        final Type type = context.getOptionAsEnum(Type.class, "type").orElseThrow();
        final String name = context.getOptionAsString("name").orElseThrow();
        final Mono<Role> getRole = context.getOptionAsRole("role");

        final Set<BaseCmd> commands = new HashSet<>();
        switch (type) {
            case COMMAND -> {
                final BaseCmd command = CommandManager.getCommand(name);
                if (command == null) {
                    return Mono.error(new CommandException(context.localize("restrictedroles.invalid.command")
                            .formatted(name)));
                }
                commands.add(command);
            }
            case CATEGORY -> {
                final CommandCategory category = EnumUtil.parseEnum(CommandCategory.class, name);
                if (category == null) {
                    return Mono.error(new CommandException(context.localize("restrictedroles.invalid.category")
                            .formatted(name, FormatUtil.format(CommandCategory.class, FormatUtil::capitalizeEnum, ", "))));
                }
                commands.addAll(CommandManager.getCommands().values().stream()
                        .filter(cmd -> cmd.getCategory() == category)
                        .collect(Collectors.toSet()));
            }
        }

        return getRole
                .flatMap(role -> {
                    final StringBuilder strBuilder = new StringBuilder();
                    final Map<Snowflake, Set<BaseCmd>> restrictedRoles = context.getDbGuild().getSettings()
                            .getRestrictedRoles();

                    switch (action) {
                        case ADD -> {
                            restrictedRoles.computeIfAbsent(role.getId(), __ -> new HashSet<>())
                                    .addAll(commands);
                            strBuilder.append(context.localize("restrictedroles.added")
                                    .formatted(FormatUtil.format(commands, cmd -> "`%s`".formatted(cmd.getName()), " "),
                                            role.getName()));
                        }
                        case REMOVE -> {
                            if (restrictedRoles.containsKey(role.getId())) {
                                restrictedRoles.get(role.getId()).removeAll(commands);
                            }
                            strBuilder.append(context.localize("restrictedroles.removed")
                                    .formatted(FormatUtil.format(commands, cmd -> "`%s`".formatted(cmd.getName()), " ")));
                        }
                    }

                    final Map<String, Set<String>> setting = restrictedRoles
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    entry -> entry.getKey().asString(),
                                    entry -> entry.getValue().stream()
                                            .map(BaseCmd::getName)
                                            .collect(Collectors.toSet())));

                    return context.getDbGuild().updateSetting(Setting.RESTRICTED_ROLES, setting)
                            .then(context.createFollowupMessage(Emoji.CHECK_MARK, strBuilder.toString()));
                });
    }

    private enum Action {
        ADD, REMOVE
    }

    private enum Type {
        COMMAND, CATEGORY
    }
}
