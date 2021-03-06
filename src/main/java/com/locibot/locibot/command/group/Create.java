package com.locibot.locibot.command.group;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.groups.entity.DBGroup;
import com.locibot.locibot.database.groups.entity.DBGroupMember;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public abstract class Create extends BaseCmd {

    private final int min;
    private final int opt;
    private final int groupType;

    /**
     * @param min         count of minimum required members
     * @param opt         count of optional members
     * @param name        command name
     * @param description command description
     */
    protected Create(int min, int opt, String name, String description, int groupType) {
        super(CommandCategory.GROUP, name, description);
        this.min = min;
        this.opt = opt;
        this.groupType = groupType;
        this.addOption("team_name", "Give us a Name", true, ApplicationCommandOption.Type.STRING);
        for (int i = 0; i < min; i++) {
            this.addOption("member_" + i, "User", true, ApplicationCommandOption.Type.USER);
        }
        for (int i = 0; i < opt; i++) {
            this.addOption("optional_member_" + i, "User", false, ApplicationCommandOption.Type.USER);
        }
    }

    @Override
    public Mono<?> execute(Context context) {
        String groupName = context.getOptionAsString("team_name").get();
        if (DatabaseManager.getGroups().containsGroup(groupName))
            return context.createFollowupMessage("This group name is already taken. Pls try another one.");
        DBGroup group = new DBGroup(groupName, groupType);
        group.insert().block();
        List<DBGroupMember> dbGroupMembers = new ArrayList<>();

        dbGroupMembers.add(new DBGroupMember(context.getEvent().getInteraction().getUser().getId(), groupName, false, false, 2, true));

        for (int i = 0; i < min; i++) {
            Snowflake member = context.getOptionAsUser("member_" + i).block().getId();
            dbGroupMembers.add(new DBGroupMember(member, groupName, false, false, 0, false));
        }
        for (int i = 0; i < opt; i++) {
            User user = context.getOptionAsUser("optional_member_" + i).block();
            if (user != null) {
                dbGroupMembers.add(new DBGroupMember(user.getId(), groupName, true, false, 0, false));
            }
        }
        dbGroupMembers.forEach(dbGroupMember -> dbGroupMember.insert().block());

        return context.createFollowupMessage("Created");
    }
}
