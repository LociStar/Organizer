package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        List<Mono<User>> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (context.getOptionAsUser("member_" + i) != null)
                users.add(context.getOptionAsUser("member_" + i));
        }

        return DatabaseManager.getEvents().getDBEvent(context.getAuthorId(), eventTitle)
                .switchIfEmpty(context.createFollowupMessageEphemeral(context.localize("event.not.found").formatted(eventTitle)).then(Mono.empty()))
                .flatMap(dbEvent -> {
                    if (dbEvent.getId() == null) {
                        return context.createFollowupMessageEphemeral(context.localize("event.not.found").formatted(eventTitle));
                    }

                    return Flux.fromIterable(users).flatMap(userMono -> userMono.map(user -> user)).collectList().flatMap(usersNoMono ->
                            Flux.fromIterable(usersNoMono).flatMap(user ->
                                    DatabaseManager.getGuilds().getDBMember(context.getGuildId(), user.getId()).flatMap(dbMember -> {
                                        if (dbEvent.containsUser(user.getId())) {
                                            return context.createFollowupMessageEphemeral(context.localize("event.add.warning.user").formatted(user.getId().asLong()));
                                        }
                                        Mono<?> result = Mono.empty();
                                        if (user.isBot()) {
                                            return context.getChannel().flatMap(textChannel -> textChannel.getGuild().flatMap(guild -> guild.getMemberById(user.getId()).flatMap(member ->
                                                    context.createFollowupMessageEphemeral(member.getNickname().orElse(user.getUsername()) + " " + context.localize("event.add.warning")))));
                                        } else if (dbMember.getBotRegister() && dbEvent.isScheduled())
                                            result = EventUtil.privateInvite(context, eventTitle, user);
                                        else if (dbEvent.isScheduled()) {
                                            result = EventUtil.publicInvite(context, dbEvent, new ArrayList<>(Collections.singleton(user.getUsername())));
                                        }
                                        return result.then(dbEvent.addMember(user, 0)).then(context.createFollowupMessageEphemeral(context.localize("event.add.success").formatted(user.getId().asLong())));
                                    })
                            ).collectList());
                });
    }
}
