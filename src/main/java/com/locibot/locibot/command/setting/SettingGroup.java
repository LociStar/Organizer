package com.locibot.locibot.command.setting;

import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;

import java.util.List;

public class SettingGroup extends BaseCmdGroup {

    public SettingGroup() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN, "setting", "Configure Shadbot",
                List.of(new SettingShow(), new NSFWSetting(), new VolumeSetting(), new AutoMessagesSetting(),
                        new AutoRolesSetting(), new AllowedRolesSetting(), new AllowedChannelsSetting(),
                        new BlacklistSetting(), new RestrictedChannelsSetting(), new RestrictedRolesSetting(), new LocaleSetting()));
    }

}
