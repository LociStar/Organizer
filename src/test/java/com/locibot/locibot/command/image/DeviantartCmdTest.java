package com.locibot.locibot.command.image;

import com.locibot.locibot.command.CmdTest;
import com.locibot.locibot.api.json.TokenResponse;
import com.locibot.locibot.api.json.image.deviantart.Content;
import com.locibot.locibot.api.json.image.deviantart.Image;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DeviantartCmdTest extends CmdTest<DeviantartCmd> {

    @Test
    public void testGetPopularImage() {
        final TokenResponse token = this.invoke("requestAccessToken");
        final Image result = this.invoke("getPopularImage", token.accessToken(), "dab");
        assertFalse(result.content().map(Content::source).orElseThrow().isBlank());
        assertFalse(result.author().username().isBlank());
        assertFalse(result.categoryPath().isBlank());
        assertFalse(result.title().isBlank());
        assertFalse(result.url().isBlank());
    }

    @Test
    public void testGetPopularImageFuzzy() {
        final TokenResponse token = this.invoke("requestAccessToken");
        final Image result = this.invoke("getPopularImage", token.accessToken(), SPECIAL_CHARS);
        assertNull(result);
    }

}
