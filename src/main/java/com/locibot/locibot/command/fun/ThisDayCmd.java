package com.locibot.locibot.command.fun;

import com.locibot.locibot.api.html.thisday.ThisDay;
import com.locibot.locibot.core.cache.SingleValueCache;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.ShadbotUtil;
import com.locibot.locibot.utils.StringUtil;
import com.locibot.locibot.utils.TimeUtil;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.ZonedDateTime;

public class ThisDayCmd extends BaseCmd {

    private static final String HOME_URL = "https://www.onthisday.com/";

    private final SingleValueCache<ThisDay> getThisDay;

    public ThisDayCmd() {
        super(CommandCategory.FUN, "this_day", "Significant events on this day");
        this.getThisDay = SingleValueCache.Builder
                .create(ThisDayCmd.getThisDay())
                .withTtlForValue(__ -> ThisDayCmd.getNextUpdate())
                .build();

    }

    private static Mono<ThisDay> getThisDay() {
        return RequestHelper.request(HOME_URL)
                .map(Jsoup::parse)
                .map(ThisDay::new);
    }

    private static EmbedCreateSpec formatEmbed(Context context, ThisDay thisDay) {
        return ShadbotUtil.getDefaultEmbed(
                EmbedCreateSpec.builder().author(EmbedCreateFields.Author.of(context.localize("thisday.title").formatted(thisDay.getDate()),
                        HOME_URL, context.getAuthorAvatar()))
                        .thumbnail("https://i.imgur.com/FdfyJDD.png")
                        .description(StringUtil.abbreviate(thisDay.getEvents(), Embed.MAX_DESCRIPTION_LENGTH)).build());
    }

    private static Duration getNextUpdate() {
        ZonedDateTime nextDate = ZonedDateTime.now()
                .withHour(0)
                .withMinute(10) // Waits for the website to update, just in case
                .withSecond(0);
        if (nextDate.isBefore(ZonedDateTime.now())) {
            nextDate = nextDate.plusDays(1);
        }

        return TimeUtil.elapsed(nextDate.toInstant());
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("thisday.loading"))
                .then(this.getThisDay)
                .flatMap(thisDay -> context.editFollowupMessage(ThisDayCmd.formatEmbed(context, thisDay)));
    }

}
