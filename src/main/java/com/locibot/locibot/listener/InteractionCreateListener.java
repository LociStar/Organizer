package com.locibot.locibot.listener;

import com.locibot.locibot.core.command.*;
import com.locibot.locibot.core.i18n.I18nManager;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.ReactorUtil;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.component.MessageComponent;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

public class InteractionCreateListener implements EventListener<DeferrableInteractionEvent> {

    @Override
    public Class<DeferrableInteractionEvent> getEventType() {
        return DeferrableInteractionEvent.class;
    }

    @Override
    public Mono<?> execute(DeferrableInteractionEvent event) {
        Telemetry.INTERACTING_USERS.add(event.getInteraction().getUser().getId().asLong());

        if (event.getInteraction().getCommandInteraction().flatMap(ApplicationCommandInteraction::getComponentType).orElse(MessageComponent.Type.UNKNOWN).equals(MessageComponent.Type.BUTTON)) {
            String commandName = event.getInteraction().getCommandInteraction().map(applicationCommandInteraction -> applicationCommandInteraction.getCustomId().orElse("").split("_")[0]).orElseThrow();
            final BaseCmdButton command = CommandManager.getButtonCommand(commandName);
            if (command == null)
                return Mono.empty();
            return event.acknowledge().then(command.execute(new PrivateContext(event)));
        }

        // TODO Feature: Interactions from DM
        if (event.getInteraction().getGuildId().isEmpty()) {
            //return event.getInteraction().getChannel().ofType(PrivateChannel.class).flatMap(privateChannel -> CommandProcessor.processCommand(new PrivateContext(event))); //TODO: Parse with CommandProcessor
            final BaseCmd command = CommandManager.getCommand(event.getInteraction().getCommandInteraction().flatMap(ApplicationCommandInteraction::getName).orElseThrow());
            return event.acknowledge().then(command.execute(new PrivateContext(event)));
        }

        return event.getInteraction().getChannel()
                .ofType(TextChannel.class)
                .flatMap(channel -> channel.getEffectivePermissions(event.getClient().getSelfId()))
                .filterWhen(ReactorUtil.filterOrExecute(
                        permissions -> permissions.contains(Permission.SEND_MESSAGES)
                                && permissions.contains(Permission.VIEW_CHANNEL),
                        event.reply(InteractionApplicationCommandCallbackSpec.builder()
                                .content(Emoji.RED_CROSS
                                        + I18nManager.localize(Config.DEFAULT_LOCALE, "interaction.missing.permissions"))
                                .ephemeral(true).build())))
                .flatMap(__ -> Mono.justOrEmpty(event.getInteraction().getGuildId()))
                .flatMap(guildId -> DatabaseManager.getGuilds().getDBGuild(guildId))
                .map(dbGuild -> new Context(event, dbGuild))
                .flatMap(CommandProcessor::processCommand);
    }

}
