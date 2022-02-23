package com.locibot.locibot.command.standalone;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
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
                        return context.editFollowupMessage(context.localize("web.link")).flatMap(Message::delete)
                                .then(context.createFollowupMessage(InteractionFollowupCreateSpec.builder()
                                        .ephemeral(true)
                                        .content(context.localize("web.use"))
                                        .addComponent(ActionRow.of(Button.link(context.localize("web.url").formatted(member.generateLoginToken()), "Link")))
                                        //.addComponent(ActionRow.of(Button.link("https://organizer-bot-website.herokuapp.com/", "Link")))
                                        .build()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return context.createFollowupMessageEphemeral(context.localize("web.error"));
                    }
                }));
    }
}
