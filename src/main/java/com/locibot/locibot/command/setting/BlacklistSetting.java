package com.locibot.locibot.command.setting;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.*;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.EnumUtil;
import com.locibot.locibot.utils.FormatUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BlacklistSetting extends BaseCmd {

    public BlacklistSetting() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN,
                "blacklist", "Manage blacklisted command(s)");

        this.addOption(option -> option.name("action")
                .description("Whether to add or remove a command/category from the blacklisted ones")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .choices(DiscordUtil.toOptions(Action.class)));
        this.addOption(option -> option.name("type")
                .description("Blacklist a command or a category")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .choices(DiscordUtil.toOptions(Type.class)));
        this.addOption(option -> option.name("cmd1")
                .description("The first command/category")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue()));
        this.addOption(option -> option.name("cmd2")
                .description("The second command/category")
                .required(false)
                .type(ApplicationCommandOption.Type.STRING.getValue()));
        this.addOption(option -> option.name("cmd3")
                .description("The third command/category")
                .required(false)
                .type(ApplicationCommandOption.Type.STRING.getValue()));
        this.addOption(option -> option.name("cmd4")
                .description("The fourth command/category")
                .required(false)
                .type(ApplicationCommandOption.Type.STRING.getValue()));
        this.addOption(option -> option.name("cmd5")
                .description("The fifth command/category")
                .required(false)
                .type(ApplicationCommandOption.Type.STRING.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();
        final Type type = context.getOptionAsEnum(Type.class, "type").orElseThrow();
        final List<String> names = IntStream.range(1, 6).boxed()
                .map(index -> context.getOptionAsString("cmd%d".formatted(index)))
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .collect(Collectors.toList());

        return switch (type) {
            case COMMAND -> this.blacklistCommands(context, action, names);
            case CATEGORY -> this.blacklistCategories(context, action, names);
        };
    }

    private Mono<?> blacklistCategories(Context context, Action action, List<String> categoryNames) {
        final Set<String> unknownCategories = categoryNames.stream()
                .filter(category -> EnumUtil.parseEnum(CommandCategory.class, category) == null)
                .collect(Collectors.toSet());

        if (!unknownCategories.isEmpty()) {
            return Mono.error(new CommandException(context.localize("blacklist.unknown.category")
                    .formatted(FormatUtil.format(unknownCategories, "`%s`"::formatted, ", "))));
        }

        final Set<CommandCategory> categories = categoryNames.stream()
                .map(category -> EnumUtil.parseEnum(CommandCategory.class, category))
                .collect(Collectors.toSet());

        // Do not allow to blacklist admin category
        if (categories.contains(CommandCategory.ADMIN)) {
            return Mono.error(new CommandException(context.localize("blacklist.exception.remove.admin")));
        }

        final Set<String> blacklist = context.getDbGuild().getSettings().getBlacklistedCmds();

        final Set<String> cmdNames = new HashSet<>();
        for (final CommandCategory category : categories) {
            for (final BaseCmd cmd : CommandManager.getCommands().values()) {
                if (cmd.getCategory() == category) {
                    cmdNames.add(cmd.getName());
                }
            }
        }

        final String message;
        switch (action) {
            case ADD -> {
                blacklist.addAll(cmdNames);
                message = context.localize("blacklist.category.added");
            }
            case REMOVE -> {
                cmdNames.forEach(blacklist::remove);
                message = context.localize("blacklist.category.removed");
            }
            default -> {
                return Mono.error(new IllegalStateException("Unexpected value: %s".formatted(action)));
            }
        }

        return context.getDbGuild().updateSetting(Setting.BLACKLIST, blacklist)
                .then(context.createFollowupMessage(Emoji.CHECK_MARK, message
                        .formatted(FormatUtil.format(categoryNames, "`%s`"::formatted, ", "))));
    }

    private Mono<?> blacklistCommands(Context context, Action action, List<String> cmdNames) {
        final Set<String> unknownCmds = cmdNames.stream()
                .filter(cmd -> CommandManager.getCommand(cmd) == null)
                .collect(Collectors.toUnmodifiableSet());

        if (!unknownCmds.isEmpty()) {
            return Mono.error(new CommandException(context.localize("blacklist.unknown.command")
                    .formatted(FormatUtil.format(unknownCmds, "`%s`"::formatted, ", "))));
        }

        // Do not allow to blacklist setting command
        if (cmdNames.contains(context.getCommandName())) {
            return Mono.error(new CommandException(context.localize("blacklist.exception.remove.setting")
                    .formatted(context.getCommandName())));
        }

        final Set<String> blacklist = context.getDbGuild().getSettings().getBlacklistedCmds();

        final String message;
        switch (action) {
            case ADD -> {
                blacklist.addAll(cmdNames);
                message = context.localize("blacklist.command.removed");
            }
            case REMOVE -> {
                cmdNames.forEach(blacklist::remove);
                message = context.localize("blacklist.command.added");
            }
            default -> {
                return Mono.error(new IllegalStateException("Unexpected value: %s".formatted(action)));
            }
        }

        return context.getDbGuild().updateSetting(Setting.BLACKLIST, blacklist)
                .then(context.createFollowupMessage(Emoji.CHECK_MARK, message
                        .formatted(FormatUtil.format(cmdNames, "`%s`"::formatted, ", "))));
    }

    private enum Action {
        ADD, REMOVE
    }

    private enum Type {
        COMMAND, CATEGORY
    }

}
