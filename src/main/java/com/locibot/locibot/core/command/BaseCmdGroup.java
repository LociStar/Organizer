package com.locibot.locibot.core.command;

import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseCmdGroup extends BaseCmd {

    private final Map<String, BaseCmd> cmds;

    public BaseCmdGroup(CommandCategory category, CommandPermission permission, String description, List<BaseCmd> cmds) {
        super(category, permission, category.getName().toLowerCase(), description);
        this.cmds = BaseCmdGroup.buildCmdsMap(cmds);
    }

    public BaseCmdGroup(CommandCategory category, String description, List<BaseCmd> cmds) {
        super(category, category.getName().toLowerCase(), description);
        this.cmds = BaseCmdGroup.buildCmdsMap(cmds);
    }

    private static Map<String, BaseCmd> buildCmdsMap(List<BaseCmd> cmds) {
        return cmds.stream().collect(Collectors.toUnmodifiableMap(BaseCmd::getName, Function.identity()));
    }

    public Collection<BaseCmd> getCommands() {
        return this.cmds.values();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String cmdName = context.getLastCommandName();
        return this.cmds.get(cmdName).execute(context);
    }

    @Override
    public List<ApplicationCommandOptionData> getOptions() {
        final List<ApplicationCommandOptionData> options = new ArrayList<>();
        for (final BaseCmd cmd : this.cmds.values()) {
            options.add(ApplicationCommandOptionData.builder()
                    .name(cmd.getName())
                    .description(cmd.getDescription())
                    .type(cmd.getType().orElse(ApplicationCommandOption.Type.SUB_COMMAND).getValue())
                    .options(cmd.getOptions())
                    .build());
        }
        return Collections.unmodifiableList(options);
    }

}
