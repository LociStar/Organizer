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
        super(CommandCategory.EVENT, CommandPermission.USER_GUILD, "delete", "delete an event");
        this.addOption("title", "event title", true, ApplicationCommandOption.Type.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        return Mono.just(context.getOptionAsString("title").orElse("")).flatMap(eventName ->
                DatabaseManager.getEvents().getDBEvent(context.getAuthorId(), eventName)
                        .switchIfEmpty(context.createFollowupMessage(context.localize("event.delete.empty").formatted(eventName)).then(Mono.empty()))
                        .flatMap(dbEvent -> dbEvent.delete().then(context.createFollowupMessage(context.localize("event.delete.success").formatted(eventName)))));
    }
}
