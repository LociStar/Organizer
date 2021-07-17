package com.locibot.locibot.command.register;

import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CommandCategory;

import java.util.List;

public class RegisterGroup extends BaseCmdGroup {
    public RegisterGroup() {
        super(CommandCategory.REGISTER, "register", "Register commands",
                List.of(new RegisterWeather()));
    }
}
