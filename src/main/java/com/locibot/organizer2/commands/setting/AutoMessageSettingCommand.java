package com.locibot.organizer2.commands.setting;

import com.locibot.organizer2.commands.CommandException;
import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.CommandContext;
import com.locibot.organizer2.database.tables.Guild;
import com.locibot.organizer2.object.Emoji;
import discord4j.core.object.entity.channel.TextChannel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.Optional;

@Component
public class AutoMessageSettingCommand implements SlashCommand {
    @Override
    public String getName() {
        return "setting welcome_message";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        final Boolean action = Boolean.parseBoolean(context.getOptionAsString("action").orElseThrow());

        return updateMessage(context, action);
    }

    private Mono<Void> updateMessage(CommandContext<?> context, Boolean action) {
        if (action) {
            final Optional<String> messageOpt = context.getOptionAsString("message");
            if (messageOpt.isEmpty()) {
                return context.localize_old("automessage.missing.message").flatMap(message -> Mono.error(new CommandException(message)));
            }
            final String message = messageOpt.orElseThrow();
            return Mono.zip(context.localize_old("automessage.missing.channel"), context.localize_old("automessage.exception.channel.type"), context.localize_old("automessage.join.enabled"))
                    .flatMap(TupleUtils.function((channelText, channelTypeText, enabledText) ->
                            context.getGuild().flatMap(guild -> context.getOptionAsChannel("channel")
                                    .switchIfEmpty(Mono.error(new CommandException(channelText)))
                                    .ofType(TextChannel.class)
                                    .switchIfEmpty(Mono.error(new CommandException(channelTypeText)))
                                    .flatMap(channel_ -> saveToDatabase(context, message, guild, channel_)
                                            .then(context.getEvent().reply("%s %s".formatted(Emoji.CHECK_MARK, enabledText
                                                    .formatted(message, channel_.getMention()))))))));
//                    context.getGuild().flatMap(guild -> context.getOptionAsChannel("channel")
//                    .switchIfEmpty(Mono.error(new CommandException(context.localize("automessage.missing.channel"))))
//                    .ofType(TextChannel.class)
//                    .switchIfEmpty(Mono.error(new CommandException(context.localize("automessage.exception.channel.type"))))
//                    .flatMap(channel_ -> saveToDatabase(context, message, guild, channel_)
//                            .then(context.getEvent().reply("%s %s".formatted(Emoji.CHECK_MARK, context.localize("automessage.join.enabled")
//                                    .formatted(message, channel_.getMention()))))));
        } else {
            return disable(context)
                    .then(context.getEvent().reply("%s %s".formatted(Emoji.CHECK_MARK, context.localize_old("automessage.join.disabled"))));
        }
    }

    private static Mono<Guild> disable(CommandContext<?> context) {
        return context.getGuild().flatMap(guild -> context.getGuildRepository()
                .findById(guild.getId().asLong()).flatMap(guildSettings -> {
                    guildSettings.setLeave_message("");
                    return context.getGuildRepository().save(guildSettings);
                }));
    }

    private static Mono<Guild> saveToDatabase(CommandContext<?> context, String message, discord4j.core.object.entity.Guild guild, TextChannel channel) {
        return context.getGuildRepository()
                .findById(guild.getId().asLong()).flatMap(guildSettings -> {
                    guildSettings.setJoin_message(message);
                    guildSettings.setMessage_channel_id(channel.getId().asLong());
                    return context.getGuildRepository().save(guildSettings);
                });
    }
}

