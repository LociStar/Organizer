package com.locibot.locibot.service.tasks;

import com.locibot.locibot.command.group.GroupUtil;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.object.ExceptionHandler;
import com.locibot.locibot.utils.LogUtil;
import discord4j.core.GatewayDiscordClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.Logger;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class ScheduleGroupsTask implements Task {

    private static final Logger LOGGER = LogUtil.getLogger(ScheduleGroupsTask.class, LogUtil.Category.TASK);
    private final GatewayDiscordClient gateway;

    public ScheduleGroupsTask(GatewayDiscordClient gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Disposable schedule(Scheduler scheduler) {
        LOGGER.info("Scheduling group reminder");
        return Flux.interval(Duration.ofMinutes(5), Duration.ofMinutes(5), scheduler)
                .flatMap(__ -> {
                    LOGGER.info("Starting group reminder");
                    DatabaseManager.getGroups().getAllGroups().forEach(group -> {
                        String date = group.getBean().getScheduledDate();
                        //check if group is scheduled
                        if (date != null) {
                            //check if date_now == group scheduled date
                            if (LocalDate.parse(date).isEqual(LocalDate.now())
                                    //send invite to all members of group, if (time_now + 35min) > scheduled_time > (time_now + 25min)
                                    && LocalTime.parse(group.getBean().getScheduledTime()).isAfter(LocalTime.now().plus(1950, ChronoUnit.SECONDS))
                                    && LocalTime.parse(group.getBean().getScheduledTime()).isBefore(LocalTime.now().plus(1650, ChronoUnit.SECONDS))) {
                                LOGGER.info("30min reminder for group " + group.getGroupName());
                                group.getMembers().forEach(member -> {
                                    if (member.getBean().getAccepted() == 0)
                                        gateway.getUserById(member.getId()).flatMap(user ->
                                                user.getPrivateChannel().flatMap(privateChannel ->
                                                        privateChannel.createMessage(GroupUtil.sendInviteMessage(group, gateway.getUserById(member.getId()).block()))
                                                                .then(group.updateInvited(member.getId(), true).then(group.updateAccept(member.getId(), 0)))
                                                )).subscribe();
                                });
                            } else if (LocalDate.parse(date).isEqual(LocalDate.now())
                                    && LocalTime.parse(group.getBean().getScheduledTime()).isBefore(LocalTime.now().plus(3750, ChronoUnit.SECONDS))
                                    && LocalTime.parse(group.getBean().getScheduledTime()).isAfter(LocalTime.now().plus(3450, ChronoUnit.SECONDS))) {
                                LOGGER.info("1h reminder for group " + group.getGroupName());
                                group.getMembers().forEach(member -> {
                                    if (member.getBean().isInvited() && member.getBean().getAccepted() == 0)
                                        gateway.getUserById(member.getId()).flatMap(user ->
                                                user.getPrivateChannel().flatMap(privateChannel ->
                                                        privateChannel.createMessage(GroupUtil.sendInviteMessage(group, gateway.getUserById(member.getId()).block()))
                                                )).subscribe();
                                });
                            }

                        }
                    });
                    LOGGER.info("Closing group reminder");
                    return Mono.empty();
                })
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }
}
