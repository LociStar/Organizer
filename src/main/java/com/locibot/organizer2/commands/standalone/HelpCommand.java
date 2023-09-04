package com.locibot.organizer2.commands.standalone;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.CommandContext;
import com.locibot.organizer2.data.Config;
import discord4j.common.JacksonResources;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class HelpCommand implements SlashCommand {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {

        PathMatchingResourcePatternResolver matcher = new PathMatchingResourcePatternResolver();
        final JacksonResources d4jMapper = JacksonResources.create();
        List<EmbedCreateFields.Field> fields = new ArrayList<>();
        List<ApplicationCommandRequest> commands = new ArrayList<>();

        try {
            for (Resource resource : matcher.getResources("commands/*.json")) {
                ApplicationCommandRequest request = d4jMapper.getObjectMapper()
                        .readValue(resource.getInputStream(), ApplicationCommandRequest.class);
                commands.add(request);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (ApplicationCommandRequest command : commands) {
            StringBuilder options = new StringBuilder();
            if (!command.options().isAbsent())
                for (ApplicationCommandOptionData option : command.options().get()) {
                    options.append("`/").append(command.name()).append(" ").append(option.name()).append("`").append(" - ");
                    options.append(option.description()).append("\n");
                }
            fields.add(EmbedCreateFields.Field.of("`/" + command.name() + "` " + command.description().get(), options.toString(), false));
        }

        return context.getEvent().reply(InteractionApplicationCommandCallbackSpec.builder()
                .ephemeral(true)
                .addEmbed(EmbedCreateSpec.builder()
                        .author(context.localize("help.title"), "https://github.com/LociStar/Organizer/wiki/Commands", context.getAuthorAvatar())
                        .description(context.localize("help.description").formatted(Config.SUPPORT_SERVER_URL))
                        .addAllFields(fields)
                        .footer(context.localize("help.footer"), "https://i.imgur.com/eaWQxvS.png")
                        .build())
                .build());
    }
}
