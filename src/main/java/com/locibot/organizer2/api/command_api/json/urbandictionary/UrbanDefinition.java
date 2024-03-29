package com.locibot.organizer2.api.command_api.json.urbandictionary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.organizer2.utils.StringUtil;

public record UrbanDefinition(String definition,
                              String example,
                              String word,
                              String permalink,
                              @JsonProperty("thumbs_up") int thumbsUp,
                              @JsonProperty("thumbs_down") int thumbsDown) {

    public String getDefinition() {
        return StringUtil.remove(this.definition, "[", "]");
    }

    public String getExample() {
        return StringUtil.remove(this.example, "[", "]");
    }

    public int getRatio() {
        return this.thumbsUp - this.thumbsDown;
    }

}
