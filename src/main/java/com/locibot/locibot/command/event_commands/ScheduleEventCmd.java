package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.events_db.entity.DBEventMember;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
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
        LocalDate newDate;
        LocalTime newTime;
        try {
            newDate = LocalDate.parse(context.getOptionAsString("date").orElseThrow(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            newTime = LocalTime.parse(context.getOptionAsString("time").orElseThrow());
        } catch (Exception ignored) {
            return context.createFollowupMessage(context.localize("event.schedule.format.error"));
        }
        List<DBEventMember> eventMembers = new ArrayList<>();
        return DatabaseManager.getEvents().getDBEvent(context.getAuthorId(), context.getOptionAsString("event_title").orElseThrow())
                .switchIfEmpty(context.createFollowupMessage(context.localize("event.not.found").formatted(context.getOptionAsString("event_title").orElse("ERROR"))).then(Mono.empty()))
                .flatMap(dbEvent ->
                        DatabaseManager.getUsers().getDBUser(context.getAuthorId()).flatMap(dbUser -> {

                            if (dbEvent.getId() == null) {
                                return context.createFollowupMessage(context.localize("event.not.found").formatted(context.getOptionAsString("event_title").orElse("ERROR")));
                            }

                            if (!dbUser.hasZoneId())
                                return context.createFollowupMessage(context.localize("event.schedule.zone.error"));

                            if (!dbEvent.getOwner().getBean().getId().equals(context.getAuthor().getId().asLong())) {
                                return context.createFollowupMessage(context.localize("event.schedule.owner"));
                            }
                            ZoneId zoneId = dbUser.getBean().getZoneId();
                            assert zoneId != null;
                            return dbEvent.updateSchedules(ZonedDateTime.of(LocalDateTime.of(newDate, newTime), zoneId))
                                    .then(context.createFollowupMessage("Event scheduled!"))
                                    .then(Flux.fromIterable(dbEvent.getMembers()).concatMap(dbEventMember ->
                                            context.getClient().getUserById(dbEventMember.getUId()).flatMap(user -> DatabaseManager.getGuilds().getDBMember(context.getGuildId(), user.getId()).flatMap(dbMember -> {
                                                if (user.isBot()) {
                                                    return context.getChannel().flatMap(textChannel -> textChannel.getGuild().flatMap(guild -> guild.getMemberById(user.getId()).flatMap(member ->
                                                            context.createFollowupMessageEphemeral(context.localize("event.schedule.bot").formatted(member.getNickname().orElse(user.getUsername()))))));
                                                } else if (dbMember.getBotRegister())
                                                    return EventUtil.privateInvite(context, context.getOptionAsString("event_title").orElseThrow(), user);
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
                                        return DatabaseManager.getEvents().getDBEvent(context.getAuthorId(), context.getOptionAsString("event_title").orElseThrow()).flatMap(dbEvent1 ->
                                                EventUtil.publicInvite(context, dbEvent1, usersString));
                                    });
                        }
                ));
    }

    @NotNull
    private Mono<List<User>> convertToUsers(Context context, List<DBEventMember> eventMembers) {
        return Flux.fromIterable(eventMembers).concatMap(dbEventMember ->
                context.getClient().getUserById(dbEventMember.getUId())).collectList();
    }

}
