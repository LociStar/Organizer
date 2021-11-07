package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.events_db.entity.DBEvent;
import com.locibot.locibot.database.events_db.entity.DBEventMember;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScheduleEventCmd extends BaseCmd {

    public ScheduleEventCmd() {
        super(CommandCategory.EVENT, CommandPermission.USER_GUILD, "schedule", "schedule a event");
        this.addOption("event_title", "Event name", true, ApplicationCommandOption.Type.STRING);
        this.addOption("date", "dd.MM.yyyy", true, ApplicationCommandOption.Type.STRING);
        this.addOption("time", "hh:mm", true, ApplicationCommandOption.Type.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        LocalDate newDate = LocalDate.parse(context.getOptionAsString("date").orElseThrow(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        LocalTime newTime = LocalTime.parse(context.getOptionAsString("time").orElseThrow());
        List<DBEventMember> eventMembers = new ArrayList<>();


        return context.createFollowupMessage("Event scheduled!").then(
                DatabaseManager.getEvents().getDBEvent(context.getOptionAsString("event_title").orElseThrow()).flatMap(dbEvent -> {
                    if (!dbEvent.getOwner().getBean().getId().equals(context.getAuthor().getId().asLong())) {
                        return context.createFollowupMessage("Only the event owner is allowed to create a schedule!");
                    }
                    return dbEvent.updateSchedules(ZonedDateTime.of(LocalDateTime.of(newDate, newTime), ZoneId.of("Europe/Berlin")))
                            .then(Flux.fromIterable(dbEvent.getMembers()).concatMap(dbEventMember ->
                                    context.getClient().getUserById(dbEventMember.getId()).flatMap(user -> DatabaseManager.getGuilds().getDBMember(context.getGuildId(), user.getId()).flatMap(dbMember -> {
                                        if (user.isBot()) {
                                            return context.getChannel().flatMap(textChannel -> textChannel.getGuild().flatMap(guild -> guild.getMemberById(user.getId()).flatMap(member ->
                                                    context.createFollowupMessageEphemeral(member.getNickname().orElse(user.getUsername()) + " is a bot and can not be messaged."))));
                                        } else if (dbMember.getBotRegister())
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
                                        else {
                                            eventMembers.add(dbEventMember);
                                            return Mono.empty();
                                        }
                                    }))
                            ).collectList())
                            //Channel (public) invitation
                            .then(convertToUsers(context, eventMembers))
                            .flatMap(users -> {
                                if (users.isEmpty())
                                    return Mono.empty();
                                List<String> usersString = users.stream().map(User::getMention).collect(Collectors.toList());
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
                            });
                })
        );
    }

    @NotNull
    private ActionRow createButtons(DBEvent dbEvent) {
        return ActionRow.of(
                Button.success("acceptInviteButton_" + dbEvent.getEventName(), "Accept"),
                Button.danger("declineInviteButton_", "Decline"));
    }

    @NotNull
    private Mono<List<User>> convertToUsers(Context context, List<DBEventMember> eventMembers) {
        return Flux.fromIterable(eventMembers).concatMap(dbEventMember ->
                context.getClient().getUserById(dbEventMember.getId())).collectList();
    }

}
