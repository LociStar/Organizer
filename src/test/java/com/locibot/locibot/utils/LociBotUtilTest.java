package com.locibot.locibot.utils;

import com.locibot.locibot.data.Config;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.EmbedData;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LociBotUtilTest {

    @Test
    public void testGetDefaultEmbed() {
        final EmbedCreateSpec consumer = LociBotUtil.getDefaultEmbed(EmbedCreateSpec.builder().build());
        final EmbedData expected = EmbedData.builder()
                .color(Config.BOT_COLOR.getRGB())
                .fields(Collections.emptyList())
                .build();
        assertEquals(expected, consumer.asRequest());
    }

}
