package com.locibot.locibot.command.fun;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.List;

public class Hello extends BaseCmd {
    public Hello() {
        super(CommandCategory.FUN, CommandPermission.USER_GLOBAL, "hello", "You might get greeted", ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getEvent().getInteractionResponse().createFollowupMessage("Hello")
                .then(context.createFollowupMessage(EmbedCreateSpec.builder().author(EmbedCreateFields.Author.of("Test", "", ""))
                        .description("Test")
                        .fields(List.of(
                                EmbedCreateFields.Field.of("FILED1", "LOOOOOOOL", false))).build()))
                .then(context.createFollowupMessage(
                        EmbedCreateSpec.builder().author(EmbedCreateFields.Author.of("Test", "", ""))
                                .description("Test")
                                .fields(List.of(
                                        EmbedCreateFields.Field.of("FILED1", "LOOOOOOOL", false))
                                ).build()));
    }
}
