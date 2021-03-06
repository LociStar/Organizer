package com.locibot.locibot.command.moderation.member;

import discord4j.core.object.entity.Member;
import discord4j.core.spec.BanQuerySpec;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

public class SoftBanCmd extends RemoveMembersCmd {

    public SoftBanCmd() {
        super(Permission.BAN_MEMBERS,
                "softban", "Kick a user and delete his messages from the last 7 days");
    }

    @Override
    public Mono<?> action(Member memberToRemove, String reason) {
        return memberToRemove.ban(BanQuerySpec.builder().deleteMessageDays(7).reason(reason).build())
                .then(memberToRemove.unban());
    }
}
