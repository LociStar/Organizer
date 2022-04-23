package com.locibot.locibot.core.command;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.entity.Member;

public class PrivateContext extends Context {
    public PrivateContext(DeferrableInteractionEvent event) {
        super(event, null);
    }

    public Member getAuthor() {
        return this.getEvent().getInteraction().getMember().orElseThrow();
    }

    public boolean isPrivate() {
        return true;
    }

}
