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
import discord4j.core.object.entity.Message;
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
                            .delayElement(Duration.ofSeconds(1)).then(Flux.fromIterable(dbEvent.getMembers()).concatMap(dbEventMember ->
                                    context.getClient().getUserById(dbEventMember.getId()).flatMap(user -> DatabaseManager.getGuilds().getDBMember(context.getGuildId(), user.getId()).flatMap(dbMember -> {
                                        if (user.isBot()) {
                                            return context.getChannel().flatMap(textChannel -> textChannel.getGuild().flatMap(guild -> guild.getMemberById(user.getId()).flatMap(member ->
                                                    context.createFollowupMessageEphemeral(member.getNickname().orElse(user.getUsername()) + " is a bot and can not be messaged."))));
                                        } else if (dbMember.getBotRegister())
                                            return EventUtil.privateInvite(context, dbEvent, user);
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
                                return DatabaseManager.getEvents().getDBEvent(context.getOptionAsString("event_title").orElseThrow()).flatMap(dbEvent1 ->
                                        EventUtil.publicInvite(context, dbEvent1, usersString));
                            });
                })
        );
    }

    @NotNull
    private Mono<List<User>> convertToUsers(Context context, List<DBEventMember> eventMembers) {
        return Flux.fromIterable(eventMembers).concatMap(dbEventMember ->
                context.getClient().getUserById(dbEventMember.getId())).collectList();
    }

}
