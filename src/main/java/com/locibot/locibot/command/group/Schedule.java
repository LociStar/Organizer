package com.locibot.locibot.command.group;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.groups.entity.DBGroup;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.locibot.locibot.command.group.GroupUtil.parseIntToGroupType;
import static com.locibot.locibot.command.group.GroupUtil.sendInviteMessage;

public class Schedule extends BaseCmd {
    protected Schedule() {
        super(CommandCategory.GROUP, "schedule_group", "schedule a group event");
        this.addOption("team_name", "Team Name", true, ApplicationCommandOption.Type.STRING);
        this.addOption("date", "dd.MM.yyyy", true, ApplicationCommandOption.Type.STRING);
        this.addOption("time", "hh:mm", true, ApplicationCommandOption.Type.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        DBGroup group = DatabaseManager.getGroups().getDBGroup(context.getOptionAsString("team_name").get()).block();
        LocalDate newDate = LocalDate.parse(context.getOptionAsString("date").get(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        LocalTime newTime = LocalTime.parse(context.getOptionAsString("time").get());

        if (!group.getOwner().getBean().getId().equals(context.getAuthor().getId().asLong())) {
            return context.createFollowupMessage("Only the group owner is allowed to create a schedule!");
        }

        //update database
        group.updateSchedules(newDate, newTime).block();
        DBGroup finalGroup1 = group;
        if (parseIntToGroupType(group.getBean().getTeamType()).isInviteOptional()) {
            group.getMembers().forEach(member -> {
                if (member.getBean().isOwner()) {
                    finalGroup1.updateInvited(member.getId(), true).then(finalGroup1.updateAccept(member.getId(), 1)).block();
                } else {
                    finalGroup1.updateInvited(member.getId(), true).then(finalGroup1.updateAccept(member.getId(), 0)).block();
                }
            });
        } else
            group.getMembers().forEach(member -> {
                if (member.getBean().isOwner()) {
                    finalGroup1.updateInvited(member.getId(), true).then(finalGroup1.updateAccept(member.getId(), 1)).block();
                } else if (!member.getBean().isOptional()) {
                    finalGroup1.updateInvited(member.getId(), true).then(finalGroup1.updateAccept(member.getId(), 0)).block();
                }
            });

        //update group object
        group = DatabaseManager.getGroups().getDBGroup(context.getOptionAsString("team_name").get()).block();
        DBGroup finalGroup = group;

        //send invitation messages
        group.getMembers().forEach(dbGroupMember -> {
            if (dbGroupMember.getBean().isInvited())
                context.getClient().getUserById(dbGroupMember.getId()).block().getPrivateChannel()
                        .flatMap(privateChannel -> privateChannel.createMessage(getMessage(finalGroup, context))).subscribe();
        });
        return context.createFollowupMessage("Group scheduled!");
    }

    public EmbedCreateSpec getMessage(DBGroup group, Context context) {
        User user = context.getEvent().getClient().getUserById(group.getOwner().getId()).block();
        return sendInviteMessage(group, user);
    }
}
