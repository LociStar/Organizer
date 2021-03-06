package com.locibot.locibot.database.guilds.bean.setting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;

public class IamBean implements Bean {

    @JsonProperty("message_id")
    private String messageId;
    @JsonProperty("role_id")
    private String roleId;

    public IamBean(String messageId, String roleId) {
        this.messageId = messageId;
        this.roleId = roleId;
    }

    public IamBean() {

    }

    public String getMessageId() {
        return this.messageId;
    }

    public String getRoleId() {
        return this.roleId;
    }

    @Override
    public String toString() {
        return "IamSetting{" +
                "messageId='" + this.messageId + '\'' +
                ", roleId='" + this.roleId + '\'' +
                '}';
    }
}
