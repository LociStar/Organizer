package com.locibot.locibot.command.setting;

import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CmdAnnotation;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;

import java.util.List;

@CmdAnnotation
public class SettingGroup extends BaseCmdGroup {

    public SettingGroup() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN, "Configure Shadbot",
                List.of(new SettingShow(), new NSFWSetting(), new AutoMessagesSetting(),
                        new AutoRolesSetting(), new AllowedRolesSetting(), new AllowedChannelsSetting(),
                        new BlacklistSetting(), new RestrictedChannelsSetting(), new RestrictedRolesSetting(), new LocaleSetting()));
    }

}
