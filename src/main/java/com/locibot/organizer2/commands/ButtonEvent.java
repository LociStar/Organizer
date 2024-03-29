package com.locibot.organizer2.commands;

import com.locibot.organizer2.core.ButtonEventContext;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

/**
 * A simple interface defining our slash command class contract.
 *  a getName() method to provide the case-sensitive name of the command.
 *  and a handle() method which will house all the logic for processing each command.
 */
public interface ButtonEvent {
    String getName();

    Mono<?> handle(ButtonEventContext<ButtonInteractionEvent> context);
}