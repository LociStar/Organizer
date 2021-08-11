package com.locibot.locibot.command.group;

import com.locibot.locibot.database.groups.entity.DBGroup;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public abstract class GroupUtil {

    public static GroupType parseIntToGroupType(int type) {
        for (GroupType groupType : GroupType.values()) {
            if (type == groupType.ordinal()) {
                return groupType;
            }
        }
        return GroupType.DEFAULT;
    }

    public static EmbedCreateSpec sendInviteMessage(DBGroup group, User user) {
        String dateString = group.getBean().getScheduledDate();
        String timeString = group.getBean().getScheduledTime();
        if (dateString == null || timeString == null) {
            dateString = "---";
            timeString = "---";
        } else {
            dateString = LocalDate.parse(dateString).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        String finalDateString = dateString;
        String finalTimeString = timeString;

        GroupType groupType = parseIntToGroupType(group.getBean().getTeamType());
        String url = groupType.getUrl();

        return EmbedCreateSpec.builder().title(groupType.getName() + "-Invite")
                .color(Color.RED)
                .thumbnail(url)
                .author(EmbedCreateFields.Author.of(user.getUsername(), "", user.getAvatarUrl()))
                .fields(List.of(EmbedCreateFields.Field.of("Invitation to group: " + group.getBean().getGroupName(), "You got invited from " + user.getUsername() + "!", false),
                        EmbedCreateFields.Field.of("Date", finalDateString, true),
                        EmbedCreateFields.Field.of("Time", finalTimeString, true),
                        EmbedCreateFields.Field.of("Accept/ Decline", "If you are interested to join the " + groupType.getName() + " group, answer with \"/accept " + group.getBean().getGroupName() + "\"\n" +
                                "You can decline the invitation with \"/decline " + group.getBean().getGroupName() + "\".", false))).build();
    }
}

