package com.locibot.locibot.command.fun;

import com.locibot.locibot.core.command.*;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import java.time.Instant;

@CmdAnnotation
public class Hello extends BaseCmd {
    public Hello() {
        super(CommandCategory.FUN, CommandPermission.USER_GLOBAL, "hello", "You might get greeted", ApplicationCommandOption.Type.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        context.getClient();
        return context.getEvent().getInteractionResponse().createFollowupMessage("Hello").then(
                context.createFollowupMessage(EmbedCreateSpec.builder()
                        .color(Color.BLUE)
                        .title("Event invitation")
                        .description("Invitation to **" + "test" + "**")
                        .footer(EmbedCreateFields.Footer.of(context.localize("event.util.footer").formatted(context.getAuthor().getUsername()), context.getAuthor().getAvatarUrl()))
                        .addField(EmbedCreateFields.Field.of(context.localize("event.description"), "empty" + "\n", false))
                        .addField(EmbedCreateFields.Field.of(context.localize("event.util.date"),
                                "<t:" + Instant.now().getEpochSecond() + ">",
                                false))
                        .addField(EmbedCreateFields.Field.of(context.localize("event.util.users"), "Test" + "\n", false))
                        .build())
        );
    }
}
