package com.locibot.locibot.utils;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.locibot.locibot.data.Config;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.EmbedData;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LociBotUtilTest {

    @Test
    public void testCleanLavaplayerErr() {
        final FriendlyException errWithMsg = new FriendlyException("<url src=\"youtube\">Watch on YouTube</url>Error",
                FriendlyException.Severity.COMMON, null);
        assertEquals("Error", ShadbotUtil.cleanLavaplayerErr(errWithMsg));

        final FriendlyException errWithoutMsg = new FriendlyException(null, FriendlyException.Severity.COMMON, null);
        assertEquals("Error not specified.", ShadbotUtil.cleanLavaplayerErr(errWithoutMsg));
    }

    @Test
    public void testGetDefaultEmbed() {
        final Consumer<EmbedCreateSpec> consumer = ShadbotUtil.getDefaultEmbed(embed -> {
        });
        final EmbedCreateSpec spec = new EmbedCreateSpec();
        consumer.accept(spec);
        final EmbedData expected = EmbedData.builder()
                .color(Config.BOT_COLOR.getRGB())
                .fields(Collections.emptyList())
                .build();
        assertEquals(expected, spec.asRequest());
    }

}
