package com.locibot.organizer2.commands.moderation;

import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.command.CommandContext;
import com.locibot.organizer2.database.repositories.IamRepository;
import com.locibot.organizer2.database.tables.Iam;
import com.locibot.organizer2.utils.DiscordUtil;
import com.locibot.organizer2.utils.FormatUtil;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.rest.util.Permission;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Component
public class IamModerationCommand implements SlashCommand {

    public static final ReactionEmoji.Unicode REACTION = ReactionEmoji.unicode("✅");
    private final IamRepository iamRepository;

    public IamModerationCommand(IamRepository iamRepository) {
        this.iamRepository = iamRepository;
    }

    @Override
    public String getName() {
        return "moderation iam";
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {

        final Flux<Role> getRoles = Flux.fromStream(IntStream.range(1, 6).boxed())
                .flatMap(index -> context.getOptionAsRole("role%d".formatted(index)));
        final Optional<String> text = context.getOptionAsString("text");

        return context.getEvent().getInteraction().getChannel()
                .flatMap(channel -> DiscordUtil.requirePermissions(channel, Permission.MANAGE_ROLES, Permission.ADD_REACTIONS)
                        .then(getRoles.collectList())
                        .flatMap(roles -> {
                            final String description = text.orElse("Click the button to get role(s): %s"
                                    .formatted(FormatUtil.format(roles,
                                            role -> "`@%s`".formatted(role.getName()), "\n")));

                            final EmbedCreateSpec embedConsumer = context.getDefaultEmbed(
                                    EmbedCreateSpec.builder()
                                            .author(EmbedCreateFields.Author.of("Iam: %s"
                                                            .formatted(FormatUtil.format(roles,
                                                                    role -> "@%s".formatted(role.getName()), ", ")),
                                                    null, context.getAuthorAvatar()))
                                            .description(description).build());

                            return createReply(context, embedConsumer)
                                    .then(createButton(context, roles));
                        }));
    }

    @NotNull
    private Mono<Message> createButton(CommandContext<?> context, List<Role> roles) {
        return context.getEvent().getReply().flatMap(message -> saveToDatabase(context, roles, message).then(
                context.getEvent().editReply(InteractionReplyEditSpec.builder()
                        .addComponent(ActionRow.of(
                                Button.success("iamButtonEvent_" + message.getId().asLong(), context.localize("event.iam.button"))))
                        .build())));
    }

    private Mono<Iam> saveToDatabase(CommandContext<?> context, List<Role> roles, Message message) {
        return context.getGuild()
                .flatMap(guild -> iamRepository.save(message.getId().asLong(), guild.getId().asLong(), roles.stream().map(role -> role.getId().asLong()).toList().toString()));
    }

    @NotNull
    private static Mono<Void> createReply(CommandContext<?> context, EmbedCreateSpec embedConsumer) {
        return context.getEvent().reply(InteractionApplicationCommandCallbackSpec.builder()
                .addEmbed(embedConsumer)
                .build());
    }
}

