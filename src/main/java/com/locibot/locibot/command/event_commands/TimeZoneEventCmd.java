package com.locibot.locibot.command.event_commands;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import discord4j.core.object.command.ApplicationCommandOption;
import reactor.core.publisher.Mono;

import java.time.ZoneId;

public class TimeZoneEventCmd extends BaseCmd {
    protected TimeZoneEventCmd() {
        super(CommandCategory.EVENT, CommandPermission.USER_GUILD, "time_zone", "Set your default time zone, to schedule events");
        this.addOption(option -> option.name("zone_id")
                .description("default: \"Europe/Berlin\", you can also use UTC+/-xx:xx")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true));
    }

    @Override
    public Mono<?> execute(Context context) {
        String zoneId = context.getOptionAsString("zone_id").orElseThrow();
        return DatabaseManager.getUsers().getDBUser(context.getAuthorId()).flatMap(dbUser -> {
            try {
                return dbUser.setZoneId(ZoneId.of(zoneId)).then(context.createFollowupMessageEphemeral(context.localize("time.zone.success").formatted(ZoneId.of(zoneId))));
            } catch (Exception e) {
                return context.createFollowupMessageEphemeral(context.localize("time.zone.error"));
            }
        });
    }
}
