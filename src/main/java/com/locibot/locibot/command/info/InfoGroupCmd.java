package com.locibot.locibot.command.info;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.groups.entity.DBGroup;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class InfoGroupCmd extends BaseCmd {
    protected InfoGroupCmd() {
        super(CommandCategory.INFO, "groups", "get a list of all groups");
        this.addOption("group_name", "group name", false, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        Optional<String> groupName = context.getOptionAsString("group_name");
        if (groupName.isPresent()) {
            return getSpecificGroupInfo(context, groupName.get());
        } else {
            return getAllGroupInfo(context);
        }
    }

    public Mono<?> getSpecificGroupInfo(Context context, String groupName) {
        if (!DatabaseManager.getGroups().containsGroup(groupName)) {
            return context.createFollowupMessage("Huh? The group you are looking  for does not exist!");
        }

        DBGroup group = DatabaseManager.getGroups().getDBGroup(groupName).block();
        EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder();
        User owner = context.getClient().getUserById(group.getOwner().getId()).block();
        String date = group.getBean().getScheduledDate();
        String dateTime = date == null ? "---" : LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd. MMMM yyyy")) + "\n" +
                group.getBean().getScheduledTime() + " o'clock";
        embedCreateSpec.title("Team: " + group.getGroupName())
                //.footer("If you need more information about a group, use /group info <groupName>", "https://img.icons8.com/cotton/344/info--v3.png")
                .color(Color.GREEN)
                .thumbnail("https://img.icons8.com/cotton/344/info--v3.png")
                .author(EmbedCreateFields.Author.of(owner.getUsername(), "", owner.getAvatarUrl()))
                .addFields(
                        EmbedCreateFields.Field.of("Members", group.getMembers().size() + "", true),
                        EmbedCreateFields.Field.of("Scheduled", dateTime, true),
                        EmbedCreateFields.Field.of("Team Created", LocalDate.parse(group.getBean().getCreationDate()).format(DateTimeFormatter.ofPattern("dd. MMMM yyyy")), true));
        StringBuilder users = new StringBuilder();
        StringBuilder invited = new StringBuilder();
        StringBuilder inviteStatus = new StringBuilder();
        group.getMembers().forEach(member -> {
            User user = context.getEvent().getClient().getUserById(member.getId()).block();
            users.append(user.getUsername()).append(System.getProperty("line.separator"));
            invited.append(member.getBean().isInvited()).append(System.getProperty("line.separator"));
            inviteStatus.append(member.getBean().getAccepted() == 1 ? "accepted" : member.getBean().getAccepted() == 0 ? "pending" : "declined").append(System.getProperty("line.separator"));
        });
        embedCreateSpec.addFields(
                EmbedCreateFields.Field.of("Users", users.toString(), true),
                EmbedCreateFields.Field.of("Invited", invited.toString(), true),
                EmbedCreateFields.Field.of("Invite Status", inviteStatus.toString(), true));
        return context.createFollowupMessage(embedCreateSpec.build());
    }

    public Mono<?> getAllGroupInfo(Context context) {
        List<DBGroup> groups = DatabaseManager.getGroups().getAllGroups();
        groups.sort((o1, o2) -> Integer.compare(o2.getMembers().size(), o1.getMembers().size()));
        EmbedCreateSpec.Builder embedCreateSpec = EmbedCreateSpec.builder();
        embedCreateSpec.title("Group List")
                .footer(EmbedCreateFields.Footer.of("If you need more information about a group, use \"/info groups <groupName>\"", "https://img.icons8.com/cotton/344/info--v3.png"))
                .color(Color.GREEN)
                .thumbnail("https://img.icons8.com/cotton/344/info--v3.png");
        StringBuilder name = new StringBuilder();
        StringBuilder count = new StringBuilder();
        StringBuilder scheduled = new StringBuilder();
        groups.forEach(group -> {
            name.append(group.getGroupName()).append(System.getProperty("line.separator"));
            count.append(group.getMembers().size()).append(System.getProperty("line.separator"));
            String date = group.getBean().getScheduledDate();
            scheduled.append(date == null ? "---" : LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd. MMMM yyyy")) + " - " + group.getBean().getScheduledTime()).append(System.getProperty("line.separator"));
        });
        if (name.isEmpty() || count.isEmpty()) {
            name.append("---");
            count.append("---");
            scheduled.append("---");
        }
        embedCreateSpec.addFields(
                EmbedCreateFields.Field.of("Name", name.toString(), true),
                EmbedCreateFields.Field.of("Member Count", count.toString(), true),
                EmbedCreateFields.Field.of("Scheduled", scheduled.toString(), true));

        return context.createFollowupMessage(embedCreateSpec.build());
    }
}
