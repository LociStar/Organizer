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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AddUserEventCmd extends BaseCmd {
    protected AddUserEventCmd() {
        super(CommandCategory.EVENT, CommandPermission.USER_GLOBAL, "add_users", "add additional users to an event");
        this.addOption("title", "event name", true, ApplicationCommandOption.Type.STRING);
        this.addOption("member_0", "Add a member", false, ApplicationCommandOption.Type.USER);
        for (int i = 0; i < 10; i++) {
            this.addOption("member_" + (i + 1), "Add a member", false, ApplicationCommandOption.Type.USER);
        }
    }

    @Override
    public Mono<?> execute(Context context) {
        String eventTitle = context.getOptionAsString("title").orElseThrow();
        if (!DatabaseManager.getEvents().containsEvent(eventTitle)) {
            return context.createFollowupMessage("Only the event-owner is allowed to add users!");
        }
        List<Mono<User>> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (context.getOptionAsUser("member_" + i) != null)
                users.add(context.getOptionAsUser("member_" + i));
        }

        return context.createFollowupMessage("Users added")
                .then(Flux.fromIterable(users).flatMap(userMono -> userMono.map(user -> user)).collectList().flatMap(usersNoMono ->
                        Flux.fromIterable(usersNoMono).flatMap(user -> DatabaseManager.getEvents().getDBEvent(eventTitle).flatMap(dbEvent ->
                                DatabaseManager.getGuilds().getDBMember(context.getGuildId(), user.getId()).flatMap(dbMember -> {
                                    if (user.isBot()) {
                                        return context.getChannel().flatMap(textChannel -> textChannel.getGuild().flatMap(guild -> guild.getMemberById(user.getId()).flatMap(member ->
                                                context.createFollowupMessageEphemeral(member.getNickname().orElse(user.getUsername()) + " is a bot and can not be messaged."))));
                                    } else if (dbMember.getBotRegister())
                                        return EventUtil.privateInvite(context, dbEvent, user);
                                    else {
                                        return EventUtil.publicInvite(context, dbEvent, new ArrayList<>(Collections.singleton(user.getUsername())));
                                    }
                                }).then(dbEvent.addMember(user, 0))
                        )).collectList()));
    }
}
