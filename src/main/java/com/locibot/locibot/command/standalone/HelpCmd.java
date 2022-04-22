package com.locibot.locibot.command.standalone;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.*;
import com.locibot.locibot.core.i18n.I18nContext;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.guilds.entity.Settings;
import com.locibot.locibot.utils.LociBotUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;

@CmdAnnotation
public class HelpCmd extends BaseCmd {

    public HelpCmd() {
        super(CommandCategory.INFO, "help", "Show the list of available commands");
        this.addOption("command", "Show help about a specific command", false, ApplicationCommandOption.Type.STRING);
    }

    private static EmbedCreateSpec formatEmbed(I18nContext context, Map<CommandCategory, Collection<String>> map, String avatarUrl) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();
        for (final CommandCategory category : CommandCategory.values()) {
            final Collection<String> cmds = map.get(category);
            if (cmds != null && !cmds.isEmpty()) {
                fields.add(EmbedCreateFields.Field.of(context.localize("help.field.title").formatted(category.getName()),
                        String.join(" ", cmds), false));
            }
        }
        return LociBotUtil.getDefaultEmbed(
                EmbedCreateSpec.builder()
                        .author(context.localize("help.title"), "https://github.com/LociStar/Organizer/wiki/Commands", avatarUrl)
                        .description(context.localize("help.description")
                                .formatted(Config.SUPPORT_SERVER_URL)) //Config.PATREON_URL
                        .footer(context.localize("help.footer"), "https://i.imgur.com/eaWQxvS.png")
                        .addAllFields(fields).build());
    }

    private static Mono<Map<CommandCategory, Collection<String>>> getMultiMap(Context context, List<CommandPermission> authorPermissions) {
        final Mono<Boolean> getIsDm = context.getChannel()
                .map(Channel::getType)
                .map(Channel.Type.DM::equals)
                .cache();

        final Mono<Settings> getSettings = DatabaseManager.getGuilds()
                .getSettings(context.getGuildId())
                .cache();

        // Iterates over all commands...
        return Flux.fromIterable(CommandManager.getCommands().values())
                // ... and removes duplicate ...
                .distinct()
                // ... and removes commands that the author cannot use ...
                .filter(cmd -> authorPermissions.contains(cmd.getPermission()))
                // ... and removes commands that are not allowed by the guild
                .filterWhen(cmd -> BooleanUtils.or(getIsDm,
                        getSettings.map(settings ->
                                        settings.isCommandAllowed(cmd)
                                                && settings.isCommandAllowedInChannel(cmd, context.getChannelId()))
                                .defaultIfEmpty(true)))
                .flatMapIterable(cmd -> {
                    if (cmd instanceof BaseCmdGroup group) {
                        return group.getCommands().stream()
                                .map(it -> Tuples.of(it.getCategory(), "%s %s".formatted(cmd.getName(), it.getName())))
                                .toList();
                    }
                    return List.of(Tuples.of(cmd.getCategory(), cmd.getName()));
                })
                .collectMultimap(Tuple2::getT1, tuples -> "`/%s`".formatted(tuples.getT2()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Optional<String> cmdNameOpt = context.getOptionAsString("command");
        if (cmdNameOpt.isPresent()) {
            final String cmdName = cmdNameOpt.orElseThrow();
            final BaseCmd baseCmd = CommandManager.getCommand(cmdName);
            if (baseCmd == null) {
                return Mono.error(new CommandException(context.localize("help.cmd.not.found")
                        .formatted(cmdName)));
            }
            return context.createFollowupMessage(baseCmd.getHelp(context));
        }

        return context.getPermissions()
                .collectList()
                .flatMap(authorPermissions -> HelpCmd.getMultiMap(context, authorPermissions))
                .map(map -> HelpCmd.formatEmbed(context, map, context.getAuthorAvatar()))
                .flatMap(context::createFollowupMessage);
    }

    // Essential part of Shadbot (Thanks to @Bluerin)
    private String howToDoAChocolateCake() {
        final String meal = "50g farine";
        final String chocolate = "200g chocolat";
        final String eggs = "3 oeufs";
        final String sugar = "100g sucre";
        final String butter = "100g beurre";
        return "Mélanger " + meal + " " + sugar + " " + eggs + " dans un saladier." +
                "\nFaire fondre au bain-marie " + chocolate + " " + butter +
                "\nRajouter le chocolat et le beurre dans le saladier." +
                "\nVerser le mélange dans un moule et enfourner pendant 25min à 180°C.";
    }

}
