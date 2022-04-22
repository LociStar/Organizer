package com.locibot.locibot.command.standalone;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CmdAnnotation;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.utils.LociBotUtil;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;

@CmdAnnotation
public class InviteCmd extends BaseCmd {

    public InviteCmd() {
        super(CommandCategory.INFO, "invite", "Get an invitation for the bot or for the support server");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(LociBotUtil.getDefaultEmbed(
                EmbedCreateSpec.builder().author(EmbedCreateFields.Author.of(context.localize("invite.title"), Config.INVITE_URL, context.getAuthorAvatar()))
                        .fields(List.of(
                                EmbedCreateFields.Field.of(context.localize("invite.bot"), context.localize("invite.link")
                                        .formatted(Config.INVITE_URL), true),
                                EmbedCreateFields.Field.of(context.localize("invite.support"), context.localize("invite.link")
                                        .formatted(Config.SUPPORT_SERVER_URL), true),
                                /*EmbedCreateFields.Field.of(context.localize("invite.donation"), context.localize("invite.link")
                                        .formatted(Config.PATREON_URL), true),*/
                                EmbedCreateFields.Field.of(context.localize("invite.vote"), context.localize("invite.link")
                                        .formatted(Config.TOP_GG_URL), true))).build()));
    }

}
