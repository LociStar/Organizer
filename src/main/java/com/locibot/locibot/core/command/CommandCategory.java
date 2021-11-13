package com.locibot.locibot.core.command;

public enum CommandCategory {

    HIDDEN("Hidden"),
    DONATOR("Donator"),
    UTILS("Utility"),
    FUN("Fun"),
    IMAGE("Image"),
    GAME("Game"),
    CURRENCY("Currency"),
    MUSIC("Music"),
    GAMESTATS("GameStats"),
    INFO("Info"),
    ADMIN("Admin"),
    MODERATION("Moderation"),
    OWNER("Owner"),
    SETTING("Setting"),
    GROUP("Group"),
    REGISTER("Register"),
    EVENT("EVENT"),
    BUTTON("BUTTON");

    private final String name;

    CommandCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
