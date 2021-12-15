package com.locibot.locibot.command.standalone;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import reactor.core.publisher.Mono;

public class WebLogin extends BaseCmd {
    public WebLogin() {
        super(CommandCategory.INFO, "web", "Generate a login token");
    }

    @Override
    public Mono<?> execute(Context context) {
        return DatabaseManager.getGuilds().getDBMember(context.getGuildId(), context.getAuthorId()).flatMap(dbMember ->
                DatabaseManager.getGuilds().getDBMember(context.getGuildId(), context.getAuthorId()).flatMap(member ->
                {
                    try {
                        return context.createFollowupMessageEphemeral("A Link will be created.")
                                .then(context.createFollowupMessage(InteractionFollowupCreateSpec.builder()
                                        .ephemeral(true)
                                        .content("Personal link to Organizer-Website:")
                                        .addComponent(ActionRow.of(Button.link("http://localhost:8081/login?token=" + member.generateLoginToken(), "Link"))).build()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return context.createFollowupMessageEphemeral("Error while generating a login token. Pleas contact the bot owner");
                    }
                }));
    }
}