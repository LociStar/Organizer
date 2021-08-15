package com.locibot.locibot.command.moderation.member;

import discord4j.core.object.entity.Member;
import discord4j.core.spec.BanQuerySpec;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

public class BanCmd extends RemoveMembersCmd {

    public BanCmd() {
        super(Permission.BAN_MEMBERS,
                "ban", "Ban a user and delete his messages from the last 7 days");
    }

    @Override
    public Mono<?> action(Member memberToRemove, String reason) {
        return memberToRemove.ban(BanQuerySpec.builder().deleteMessageDays(7).reason(reason).build());
    }
}
