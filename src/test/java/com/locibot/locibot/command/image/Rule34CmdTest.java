package com.locibot.locibot.command.image;

import com.locibot.locibot.command.CmdTest;
import com.locibot.locibot.api.json.image.r34.R34Post;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Rule34CmdTest extends CmdTest<Rule34Cmd> {

    @Test
    public void testGetR34Post() {
        final R34Post result = this.invoke("getR34Post", "dab");
        assertFalse(result.fileUrl().isBlank());
        assertFalse(result.getTags().isEmpty());
        assertTrue(result.height() > 0);
        assertTrue(result.width() > 0);
    }

    @Test
    public void testGetR34PostFuzzy() {
        final R34Post result = this.invoke("getR34Post", SPECIAL_CHARS);
        assertNull(result);
    }

}
