package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.events_db.entity.DBEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public abstract class EventUtil {
    @NotNull
    static Mono<Message> publicInvite(Context context, DBEvent dbEvent, List<String> usersString) {
        return context.getClient().getUserById(dbEvent.getOwner().getId()).flatMap(owner ->
                context.createFollowupMessage(InteractionFollowupCreateSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .color(Color.BLUE)
                                .title("Event invitation")
                                .thumbnail(dbEvent.getIcon() == null ? owner.getAvatarUrl() : dbEvent.getIcon())
                                .description("Invitation to **" + dbEvent.getEventName() + "**")
                                .footer(EmbedCreateFields.Footer.of("Only users who do not want to receive direct messages from the bot are listed above.\nAuthor: " + owner.getUsername(), owner.getAvatarUrl()))
                                .addField(EmbedCreateFields.Field.of("Description", dbEvent.getEventDescription() + "\n", false))
                                .addField(EmbedCreateFields.Field.of("Users", usersString.stream().map(String::toString).collect(Collectors.joining(",")) + "\n", false))
                                .build())
                        .content(usersString.stream().map(String::toString).collect(Collectors.joining(",")))
                        .addComponent(createButtons(dbEvent))
                        .build()));
    }

    @NotNull
    static Mono<Message> privateInvite(Context context, DBEvent dbEvent, User user) {
        return user.getPrivateChannel().flatMap(privateChannel ->
                context.getClient().getUserById(dbEvent.getOwner().getId()).flatMap(owner ->
                        privateChannel.createMessage(MessageCreateSpec.builder()
                                .addEmbed(EmbedCreateSpec.builder()
                                        .color(Color.BLUE)
                                        .title("Event invitation")
                                        .thumbnail(dbEvent.getIcon() == null ? owner.getAvatarUrl() : dbEvent.getIcon())
                                        .description("You got invited to **" + dbEvent.getEventName() + "**")
                                        .footer(EmbedCreateFields.Footer.of("Author: " + owner.getUsername(), owner.getAvatarUrl()))
                                        .addField(EmbedCreateFields.Field.of("Description", dbEvent.getEventDescription() + "\n", false))
                                        .build())
                                .addComponent(createButtons(dbEvent)).build())));
    }

    @NotNull
    private static ActionRow createButtons(DBEvent dbEvent) {
        return ActionRow.of(
                Button.success("acceptInviteButton_" + dbEvent.getEventName(), "Accept"),
                Button.danger("declineInviteButton_" + dbEvent.getEventName(), "Decline"));
    }
}