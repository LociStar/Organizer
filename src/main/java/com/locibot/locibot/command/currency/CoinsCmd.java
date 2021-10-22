package com.locibot.locibot.command.currency;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.guilds.entity.DBMember;
import com.locibot.locibot.object.Emoji;
import discord4j.core.object.command.ApplicationCommand;
import reactor.core.publisher.Mono;

public class CoinsCmd extends BaseCmd {

    public CoinsCmd() {
        super(CommandCategory.CURRENCY, "coins", "Show user's coins");
        this.addOption(option -> option.name("user")
                .description("If not specified, it will show your coins")
                .required(false)
                .type(ApplicationCommand.Type.USER.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getOptionAsMember("user")
                .defaultIfEmpty(context.getAuthor())
                .flatMap(user -> DatabaseManager.getGuilds()
                        .getDBMember(context.getGuildId(), user.getId())
                        .map(DBMember::getCoins)
                        .flatMap(coins -> {
                            if (user.getId().equals(context.getAuthorId())) {
                                return context.createFollowupMessage(Emoji.PURSE, context.localize("coins.yours")
                                        .formatted(context.localize(coins)));
                            } else {
                                return context.createFollowupMessage(Emoji.PURSE, context.localize("coins.user")
                                        .formatted(user.getUsername(), context.localize(coins)));
                            }
                        }));
    }

}