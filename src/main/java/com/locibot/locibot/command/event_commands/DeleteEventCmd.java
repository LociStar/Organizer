package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import discord4j.core.object.command.ApplicationCommandOption;
import reactor.core.publisher.Mono;

public class DeleteEventCmd extends BaseCmd {
    protected DeleteEventCmd() {
        super(CommandCategory.EVENT, CommandPermission.USER_GLOBAL, "delete", "delete an event");
        this.addOption("title", "event title", true, ApplicationCommandOption.Type.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        return Mono.just(context.getOptionAsString("title").orElse("")).flatMap(eventName ->
        {
            //check if group does exist
            if (DatabaseManager.getEvents().containsEvent(eventName)) {
                return DatabaseManager.getEvents().getDBEvent(eventName).flatMap(event ->
                {
                    //check if author is owner
                    if (event.getOwner().getId().equals(context.getAuthorId())) {
                        return event.delete().then(context.createFollowupMessage(eventName + " has been deleted!"));
                    }
                    return context.createFollowupMessage("You are not the owner of " + eventName + "!");
                });
            }
            return context.createFollowupMessage(eventName + " does not exist!");
        });
    }
}
