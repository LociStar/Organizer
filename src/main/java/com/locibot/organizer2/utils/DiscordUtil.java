package com.locibot.organizer2.utils;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.*;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.rest.util.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

public class DiscordUtil {

    public static Logger LOGGER = LoggerFactory.getLogger(DiscordUtil.class);

    /**
     * @param channel    The channel in which the permission has to be checked.
     * @param userId     The ID of the user to check permissions for.
     * @param permission The permission to check.
     * @return {@code true} if the user has the permission in the provided channel, {@code false} otherwise.
     */
    public static Mono<Boolean> hasPermission(Channel channel, Snowflake userId, Permission permission) {
        // A user has all the permissions in a private channel
        if (channel instanceof PrivateChannel) {
            return Mono.just(true);
        }
        return ((GuildChannel) channel).getEffectivePermissions(userId)
                .map(permissions -> permissions.contains(permission));
    }

    /**
     * @param enumClass The enumeration to convert as a list of choices.
     * @param <T>       The type of enumeration.
     * @return An ordered list of {@link ApplicationCommandOptionChoiceData} converted from {@code enumClass}.
     */
    public static <T extends Enum<T>> List<ApplicationCommandOptionChoiceData> toOptions(Class<T> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .map(String::toLowerCase)
                .map(it -> ApplicationCommandOptionChoiceData.builder().name(it).value(it).build())
                .collect(Collectors.toList());
    }

    public static <T extends Enum<T>> List<ApplicationCommandOptionChoiceData> toOptions(Collection<String> list) {
        return list.stream()
                .map(it -> ApplicationCommandOptionChoiceData.builder().name(it).value(it).build())
                .collect(Collectors.toList());
    }

    public static List<ApplicationCommandInteractionOption> flattenOptions(ApplicationCommandInteraction interaction) {
        final ArrayList<ApplicationCommandInteractionOption> options = new ArrayList<>();
        DiscordUtil.flattenOptionRecursive(options, interaction.getOptions());
        return options;
    }

    private static void flattenOptionRecursive(List<ApplicationCommandInteractionOption> list,
                                               List<ApplicationCommandInteractionOption> options) {
        for (final ApplicationCommandInteractionOption option : options) {
            list.add(option);
            DiscordUtil.flattenOptionRecursive(list, option.getOptions());
        }
    }

    public static Optional<String> getSubCommandGroupName(ApplicationCommandInteractionEvent event) {
        return DiscordUtil.flattenOptions(event.getInteraction().getCommandInteraction().orElseThrow())
                .stream()
                .filter(option -> option.getType() == ApplicationCommandOption.Type.SUB_COMMAND_GROUP)
                .map(ApplicationCommandInteractionOption::getName)
                .findFirst();
    }

    public static Optional<String> getSubCommandName(ApplicationCommandInteractionEvent event) {
        return DiscordUtil.flattenOptions(event.getInteraction().getCommandInteraction().orElseThrow())
                .stream()
                .filter(option -> option.getType() == ApplicationCommandOption.Type.SUB_COMMAND)
                .map(ApplicationCommandInteractionOption::getName)
                .findFirst();
    }

    public static String getCommandName(ApplicationCommandInteractionEvent event) {
        return event.getInteraction().getCommandInteraction().flatMap(ApplicationCommandInteraction::getName).orElseThrow();
    }

    public static String getFullCommandName(ApplicationCommandInteractionEvent event) {
        final List<String> cmds = new ArrayList<>();
        cmds.add(getCommandName(event));
        getSubCommandGroupName(event).ifPresent(cmds::add);
        getSubCommandName(event).ifPresent(cmds::add);
        return String.join(" ", cmds);
    }

}
