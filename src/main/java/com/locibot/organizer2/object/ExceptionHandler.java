package com.locibot.organizer2.object;


import com.locibot.organizer2.commands.CommandException;
import com.locibot.organizer2.core.ButtonEventContext;
import com.locibot.organizer2.core.CommandContext;
import io.netty.channel.unix.Errors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class ExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);

    public static final Function<String, RetryBackoffSpec> RETRY_ON_INTERNET_FAILURES =
            message -> Retry.backoff(3, Duration.ofSeconds(1))
                    .filter(err -> err instanceof PrematureCloseException
                            || err instanceof Errors.NativeIoException
                            || err instanceof TimeoutException)
                    .onRetryExhaustedThrow((spec, signal) -> new IOException(message));

    public static Mono<?> handleCommandError(Throwable thr, CommandContext<?> context) {
        if (thr instanceof CommandException err) {
            return ExceptionHandler.onCommandException(err, context);
        }
        if (thr instanceof TimeoutException || thr instanceof IOException) {
            return ExceptionHandler.onServerAccessError(thr, context);
        }
        return ExceptionHandler.onUnknown(thr, context);
    }

    public static Mono<?> handleCommandError(Throwable thr, ButtonEventContext<?> context) {
        if (thr instanceof CommandException err) {
            return ExceptionHandler.onCommandException(err, context);
        }
        if (thr instanceof TimeoutException || thr instanceof IOException) {
            return ExceptionHandler.onServerAccessError(thr, context);
        }
        return ExceptionHandler.onUnknown(thr, context);
    }

    private static Mono<?> onCommandException(CommandException err, CommandContext<?> context) {
        return context.getEvent().reply(Emoji.GREY_EXCLAMATION + err.getMessage());
    }

    private static Mono<?> onCommandException(CommandException err, ButtonEventContext<?> context) {
        return context.getEvent().reply(Emoji.GREY_EXCLAMATION + err.getMessage());
    }

    private static Mono<?> onServerAccessError(Throwable err, CommandContext<?> context) {
        final Throwable cause = err.getCause() != null ? err.getCause() : err;

        Mono<?> log = context.getGuild().flatMap(guild -> {
            LOGGER.warn("{Guild ID: {}} [{}] Server access error. {}: {}\n{}",
                    guild.getId().asString(),
                    context.getFullCommandName(),
                    cause.getClass().getName(),
                    cause.getMessage(),
                    context.getEvent().getInteraction().getData().data());
            return Mono.empty();
        });

        return log.then(context.getEvent()
                .reply(Emoji.RED_FLAG + context.localize("exception.server.access").formatted(context.getFullCommandName())));
    }

    private static Mono<?> onServerAccessError(Throwable err, ButtonEventContext<?> context) {
        final Throwable cause = err.getCause() != null ? err.getCause() : err;

        Mono<?> log = context.getGuild().flatMap(guild -> {
            LOGGER.warn("{Guild ID: {}} [{}] Server access error. {}: {}\n{}",
                    guild.getId().asString(),
                    context.getEvent().getCustomId(),
                    cause.getClass().getName(),
                    cause.getMessage(),
                    context.getEvent().getInteraction().getData().data());
            return Mono.empty();
        });
        return log.then(context.getEvent()
                .reply(Emoji.RED_FLAG + context.localize("exception.server.access").formatted(context.getEvent().getCustomId())));
    }

    private static Mono<?> onUnknown(Throwable err, CommandContext<?> context) {

        Mono<?> log = context.getGuild().flatMap(guild -> {
            LOGGER.error("{Guild ID: %s} [%s] An unknown error occurred: %s\n%s"
                            .formatted(guild.getId().asString(),
                                    context.getFullCommandName(),
                                    Objects.requireNonNullElse(err.getMessage(), ""),
                                    context.getEvent().getInteraction().getData().data()),
                    err);
            return Mono.empty();
        });

        return log.then(context.getEvent().reply(Emoji.RED_FLAG + context.localize("exception.unknown")
                .formatted(context.getFullCommandName())));
    }

    private static Mono<?> onUnknown(Throwable err, ButtonEventContext<?> context) {

        System.out.println("ERROR " + Arrays.toString(err.getStackTrace()));
        Mono<?> log = context.getGuild().flatMap(guild -> {
            LOGGER.error("{Guild ID: %s} [%s] An unknown error occurred: %s\n%s"
                            .formatted(guild.getId().asString(),
                                    context.getEvent().getCustomId(),
                                    Objects.requireNonNullElse(err.getMessage(), ""),
                                    context.getEvent().getInteraction().getData().data()),
                    err);
            return Mono.empty();
        });

        return log.then(context.getEvent().reply(Emoji.RED_FLAG + context.localize("exception.unknown")
                .formatted(context.getEvent().getCustomId())));
    }

    public static void handleUnknownError(Throwable err) {
        LOGGER.error("An unknown error occurred: %s"
                .formatted(Objects.requireNonNullElse(err.getMessage(), "")), err);
    }

}

