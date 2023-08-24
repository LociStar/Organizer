package com.locibot.organizer2.commands.util;

import com.locibot.organizer2.api.command_api.ServerAccessException;
import com.locibot.organizer2.api.command_api.json.urbandictionary.UrbanDefinition;
import com.locibot.organizer2.api.command_api.json.urbandictionary.UrbanDictionaryResponse;
import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.CommandContext;
import com.locibot.organizer2.object.Emoji;
import com.locibot.organizer2.object.RequestHelper;
import com.locibot.organizer2.utils.NetUtil;
import com.locibot.organizer2.utils.StringUtil;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Component
public class UrbanUtilCommand implements SlashCommand {

    private static final String HOME_URL = "http://api.urbandictionary.com/v0/define";

    @Override
    public String getName() {
        return "util urban";
    }

    private static Mono<InteractionReplyEditSpec> formatEmbed(CommandContext<?> context, UrbanDefinition urbanDef) {
        final String definition = StringUtil.abbreviate(urbanDef.getDefinition(), Embed.MAX_DESCRIPTION_LENGTH);
        final String example = StringUtil.abbreviate(urbanDef.getExample(), Embed.Field.MAX_VALUE_LENGTH);
        Mono<EmbedCreateSpec.Builder> embedMono = context.localize_old("urban.title")
                .map(title -> EmbedCreateSpec.builder()
                        .author(EmbedCreateFields.Author.of(title.formatted(urbanDef.word()),
                                urbanDef.permalink(), context.getAuthorAvatar()))
                        .thumbnail("https://i.imgur.com/7KJtwWp.png")
                        .description(definition));

        if (!example.isBlank()) {
            return Mono.zip(context.localize_old("urban.example"), embedMono).map(TupleUtils.function((exampleText, embed) -> {
                embed.fields(List.of(EmbedCreateFields.Field.of(exampleText, example, false)));
                return InteractionReplyEditSpec.builder().addEmbed(context.getDefaultEmbed(embed.build())).build();
            }));
        }
        return embedMono.map(builder -> InteractionReplyEditSpec.builder().addEmbed(context.getDefaultEmbed(builder.build())).build());
    }

    private static Mono<UrbanDefinition> getUrbanDefinition(String query) {
        final String url = "%s?".formatted(HOME_URL)
                + "term=%s".formatted(NetUtil.encode(query));
        return RequestHelper.fromUrl(url)
                .to(UrbanDictionaryResponse.class)
                .flatMapIterable(UrbanDictionaryResponse::definitions)
                .sort(Comparator.comparingInt(UrbanDefinition::getRatio).reversed())
                .next()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(ServerAccessException.isStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR))
                        .onRetryExhaustedThrow((spec, signal) -> new IOException("Retries exhausted on error %d"
                                .formatted(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()))));
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        final String query = context.getOptionAsString("word").orElseThrow();
        return Mono.zip(context.localize_old("urban.loading"), getUrbanDefinition(query), context.localize_old("urban.not.found")).flatMap(TupleUtils.function(
                (loading, urbanDef, notFound) -> context.getEvent().reply(Emoji.HOURGLASS + loading)
                        .then(formatEmbed(context, urbanDef)
                                .flatMap(spec -> context.getEvent().editReply(spec))
                                .switchIfEmpty(context.getEvent().editReply(notFound.formatted(query))))));
//        return context.getEvent().reply(Emoji.HOURGLASS + context.localize("urban.loading"))
//                .then(getUrbanDefinition(query))
//                .flatMap(urbanDef -> context.getEvent().editReply(formatEmbed(context, urbanDef)))
//                .switchIfEmpty(context.getEvent().editReply(Emoji.MAGNIFYING_GLASS +
//                        context.localize("urban.not.found").formatted(query)));

    }
}
