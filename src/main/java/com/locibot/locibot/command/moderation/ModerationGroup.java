package com.locibot.locibot.command.moderation;

import com.locibot.locibot.command.moderation.botRegister.DmRegisterCmd;
import com.locibot.locibot.command.moderation.member.BanCmd;
import com.locibot.locibot.command.moderation.member.KickCmd;
import com.locibot.locibot.command.moderation.member.SoftBanCmd;
import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CmdAnnotation;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;

import java.util.List;

@CmdAnnotation
public class ModerationGroup extends BaseCmdGroup {

    public ModerationGroup() {
        super(CommandCategory.MODERATION, CommandPermission.ADMIN, "Manages your server",
                List.of(new RolelistCmd(), new PruneCmd(), new KickCmd(), new BanCmd(), new SoftBanCmd(),
                        new ManageCoinsCmd(), new IamCmd(), new DmRegisterCmd()));
    }

}
