package com.locibot.organizer2.listeners;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.command.CommandContext;
import com.locibot.organizer2.data.Telemetry;
import com.locibot.organizer2.database.repositories.EventRepository;
import com.locibot.organizer2.database.repositories.GuildRepository;
import com.locibot.organizer2.database.repositories.UserRepository;
import com.locibot.organizer2.object.Emoji;
import com.locibot.organizer2.utils.DiscordUtil;
import com.locibot.organizer2.object.ExceptionHandler;
import com.locibot.organizer2.utils.ReactorUtil;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Component
public class SlashCommandListener {

    private final GuildRepository guildRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    private final Collection<SlashCommand> commands;

    public SlashCommandListener(List<SlashCommand> slashCommands, GatewayDiscordClient client, GuildRepository guildRepository, UserRepository userRepository, EventRepository eventRepository) {
        commands = slashCommands;
        this.guildRepository = guildRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;

        client.on(ChatInputInteractionEvent.class, this::handle).subscribe();
    }


    public Mono<?> handle(ChatInputInteractionEvent event) {

        Mono<?> updateLastUsedTimestamp = userRepository.updateLastUsedTimestamp(event.getInteraction().getUser().getId().asLong());

        //Convert our list to a flux that we can iterate through
        return Flux.fromIterable(commands)
                //Filter out all commands that don't match the name this event is for
                .filter(command -> command.getName().equals(DiscordUtil.getFullCommandName(event)))
                //Get the first (and only) item in the flux that matches our filter
                .next()
                //Have our command class handle all logic related to its specific command.
                .flatMap(command -> {
                    CommandContext<ChatInputInteractionEvent> commandContext = new CommandContext<>(event, guildRepository, userRepository, eventRepository);

                    // add Locale to context
                    return commandContext.getUncachedLocale()
                            .flatMap(locale -> {
                                commandContext.setLocale(locale);

                                //Check if the user has the required permissions to run the command
                                return commandContext.getPermissions()
                                        .collectList()
                                        // The author has the permission to execute this command
                                        .filterWhen(ReactorUtil.filterOrExecute(
                                                userPerms -> userPerms.contains(command.getPermission()),
                                                commandContext.getEvent()
                                                        .reply(Emoji.ACCESS_DENIED + " " + commandContext.localize("command.missing.permission"))
                                                        .withEphemeral(true)))
                                        .flatMap(__ -> command.handle(commandContext));
                            });
                })
                .onErrorResume(err -> {
                    CommandContext<ChatInputInteractionEvent> commandContextInit = new CommandContext<>(event, guildRepository, userRepository, eventRepository);
                    // add Locale to context
                    Mono<CommandContext<ChatInputInteractionEvent>> commandContextMono = commandContextInit.getUncachedLocale()
                            .map(locale -> {
                                commandContextInit.setLocale(locale);
                                return commandContextInit;
                            });
                    return commandContextMono.flatMap(commandContext -> ExceptionHandler.handleCommandError(err, commandContext).then(Mono.empty()));
                })
                .then(updateLastUsedTimestamp)
                .doOnSuccess(__ -> Telemetry.COMMAND_USAGE_COUNTER.labels(event.getCommandName()).inc()); //TODO: Add error handling
    }
}
