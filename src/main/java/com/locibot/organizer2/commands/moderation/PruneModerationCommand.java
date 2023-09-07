package com.locibot.organizer2.commands.moderation;

import com.locibot.organizer2.commands.CommandException;
import com.locibot.organizer2.commands.SlashCommand;
import com.locibot.organizer2.core.CommandContext;
import com.locibot.organizer2.object.Emoji;
import com.locibot.organizer2.utils.DiscordUtil;
import com.locibot.organizer2.utils.NumberUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.rest.util.Permission;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class PruneModerationCommand implements SlashCommand {

    private static final long MIN_MESSAGES = 1;
    private static final long MAX_MESSAGES = 100;
    private static final int MESSAGES_OFFSET = 1;

    @Override
    public String getName() {
        return "moderation prune";
    }

    private static Mono<Long> getLimit(CommandContext<?> context) {
        final Optional<Long> limitOpt = context.getOptionAsLong("limit");
        final long limit = limitOpt.orElse(MAX_MESSAGES);
        if (!NumberUtil.isBetween(limit, MIN_MESSAGES, MAX_MESSAGES)) {
            return Mono.error(new CommandException(context.localize("prune.limit.out.of.range")
                    .formatted(MIN_MESSAGES, MAX_MESSAGES)));
        }

        // The count is incremented by MESSAGES_OFFSET to take into account the command
        return Mono.just(Math.min(MAX_MESSAGES, limit + MESSAGES_OFFSET));
    }

    private static Predicate<Message> filterMessage(@Nullable Snowflake authorId) {
        return message -> (authorId == null
                || message.getAuthor().map(User::getId).map(authorId::equals).orElse(false));
    }

    @Override
    public Mono<?> handle(CommandContext<?> context) {
        return context.getEvent().reply(Emoji.HOURGLASS + context.localize("prune.loading"))
                .then(context.getEvent().getInteraction().getChannel())
                .cast(GuildMessageChannel.class)
                .flatMap(channel -> DiscordUtil.requirePermissions(channel,
                                Permission.MANAGE_MESSAGES, Permission.READ_MESSAGE_HISTORY)
                        .then(getLimit(context))
                        .flatMapMany(limit -> Flux.defer(() -> {

                            final Mono<User> getAuthor = context.getOptionAsUser("authors");
                            final Mono<Optional<Snowflake>> getAuthorId = getAuthor
                                    .map(User::getId)
                                    .map(Optional::of)
                                    .defaultIfEmpty(Optional.empty());

                            return getAuthorId.flatMapMany(authorOpt ->
                                    channel.getMessagesBefore(Snowflake.of(Instant.now()))
                                            .take(limit)
                                            .filter(filterMessage(authorOpt.orElse(null))));
                        }))
                        .map(Message::getId)
                        .collectList()
                        .flatMap(messageIds -> channel.bulkDelete(Flux.fromIterable(messageIds))
                                .count()
                                .map(messagesNotDeleted -> Math.max(0, messageIds.size() - messagesNotDeleted - MESSAGES_OFFSET))))
                .flatMap(messagesDeleted -> context.getEvent().createFollowup(Emoji.CHECK_MARK + context.localize("prune.messages.deleted")
                        .formatted(messagesDeleted)));
    }
}
