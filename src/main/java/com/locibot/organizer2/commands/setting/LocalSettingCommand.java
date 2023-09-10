package com.locibot.organizer2.commands.setting;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.command.CommandContext;
import com.locibot.organizer2.object.Emoji;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class LocalSettingCommand implements SlashCommand {
    @Override
    public String getName() {
        return "setting language";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {

        final String locale = context.getOptionAsString("value").orElseThrow();

        return context.getGuild().flatMap(guild ->
                context.getGuildRepository().findById(guild.getId().asLong()).flatMap(guildSettings -> {
                    guildSettings.setLocale(locale);
                    return context.getGuildRepository().save(guildSettings);
                }).then(context.localize_old("setting.locale.message").flatMap(message -> context.getEvent()
                        .reply("%s %s".formatted(Emoji.CHECK_MARK, message.formatted(locale))))));
    }
}
