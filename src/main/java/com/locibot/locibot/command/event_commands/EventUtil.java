package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
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
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public abstract class EventUtil {
    @NotNull
    static Mono<Message> publicInvite(Context context, DBEvent dbEvent, List<String> usersString) {
        return context.getClient().getUserById(dbEvent.getOwner().getUId()).flatMap(owner ->
                context.createFollowupMessage(InteractionFollowupCreateSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .color(Color.BLUE)
                                .title("Event invitation")
                                .thumbnail(dbEvent.getIcon() == null ? owner.getAvatarUrl() : dbEvent.getIcon())
                                .description("Invitation to **" + dbEvent.getEventName() + "**")
                                .footer(EmbedCreateFields.Footer.of(context.localize("event.util.footer").formatted(owner.getUsername()), owner.getAvatarUrl()))
                                .addField(EmbedCreateFields.Field.of(context.localize("event.description"), dbEvent.getEventDescription() + "\n", false))
                                .addField(EmbedCreateFields.Field.of(context.localize("event.util.date"),
                                        "<t:" + dbEvent.getBean().getScheduledDate() + ">",
                                        false))
                                .addField(EmbedCreateFields.Field.of(context.localize("event.util.users"), usersString.stream().map(String::toString).collect(Collectors.joining(",")) + "\n", false))
                                .build())
                        .content(usersString.stream().map(String::toString).collect(Collectors.joining(",")))
                        .addComponent(createButtons(dbEvent, context))
                        .build()));
    }

    @NotNull
    static Mono<Message> privateInvite(Context context, String dbEvent_title, User user) {
        return DatabaseManager.getEvents().getDBEvent(context.getAuthorId(), dbEvent_title).flatMap(dbEvent ->
                user.getPrivateChannel().flatMap(privateChannel ->
                        context.getClient().getUserById(dbEvent.getOwner().getUId()).flatMap(owner ->
                                privateChannel.createMessage(MessageCreateSpec.builder()
                                        .addEmbed(EmbedCreateSpec.builder()
                                                .color(Color.BLUE)
                                                .title(context.localize("event.util.invitation"))
                                                .thumbnail(dbEvent.getIcon() == null ? owner.getAvatarUrl() : dbEvent.getIcon())
                                                .description("You got invited to **" + dbEvent.getEventName() + "**")
                                                .addField(EmbedCreateFields.Field.of(context.localize("event.util.date"),
                                                        "<t:" + dbEvent.getBean().getScheduledDate() + ">",
                                                        false))
                                                .footer(EmbedCreateFields.Footer.of(context.localize("event.author").formatted(owner.getUsername()), owner.getAvatarUrl()))
                                                .addField(EmbedCreateFields.Field.of(context.localize("event.description"), dbEvent.getEventDescription() + "\n", false))
                                                .build())
                                        .addComponent(createButtons(dbEvent, context)).build()))));
    }

    @NotNull
    private static ActionRow createButtons(DBEvent dbEvent, Context context) {
        return ActionRow.of(
                Button.success("acceptInviteButton_" + dbEvent.getId(), context.localize("event.util.button.accept")),
                Button.danger("declineInviteButton_" + dbEvent.getId(), context.localize("event.util.button.decline")));
    }

    @Nullable
    public static Mono<Message> disableIfNoEvent(Context context, ObjectId objectId) {
        if (!DatabaseManager.getEvents().containsEvent(objectId)) {
            ActionRow a = (ActionRow) ActionRow.fromData(context.getEvent().getInteraction().getMessage().orElseThrow().getComponents().get(0).getData());
            Button accept = (Button) Button.fromData(a.getChildren().get(0).getData());
            Button decline = (Button) Button.fromData(a.getChildren().get(1).getData());
            return context.getEvent().deferReply().onErrorResume(throwable -> Mono.empty()).then(context.createFollowupMessageEphemeral(context.localize("event.button.accept.deleted"))
                    .then(context.getEvent().getInteraction().getMessage().get().edit().withComponents(
                            ActionRow.of(
                                    accept.disabled(), decline.disabled()
                            ))));
        }
        return null;
    }
}
