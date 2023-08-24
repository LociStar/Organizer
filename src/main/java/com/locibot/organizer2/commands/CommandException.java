package com.locibot.organizer2.commands;

import java.io.Serial;

public class CommandException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CommandException(String message) {
        super(message, null, false, false);
    }
}
