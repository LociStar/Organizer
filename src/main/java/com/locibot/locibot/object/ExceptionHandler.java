package com.locibot.locibot.object;

import com.locibot.locibot.LociBot;
import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.command.MissingPermissionException;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.utils.FormatUtil;
import io.netty.channel.unix.Errors;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class ExceptionHandler {

    public static final Function<String, RetryBackoffSpec> RETRY_ON_INTERNET_FAILURES =
            message -> Retry.backoff(3, Duration.ofSeconds(1))
                    .filter(err -> err instanceof PrematureCloseException
                            || err instanceof Errors.NativeIoException
                            || err instanceof TimeoutException)
                    .onRetryExhaustedThrow((spec, signal) -> new IOException(message));

    public static Mono<?> handleCommandError(Throwable thr, Context context) {
        if (thr instanceof CommandException err) {
            return ExceptionHandler.onCommandException(err, context);
        }
        if (thr instanceof MissingPermissionException err) {
            return ExceptionHandler.onMissingPermissionException(err, context);
        }
        if (thr instanceof TimeoutException || thr instanceof IOException) {
            return ExceptionHandler.onServerAccessError(thr, context);
        }
        return ExceptionHandler.onUnknown(thr, context);
    }

    private static Mono<?> onCommandException(CommandException err, Context context) {
        return context.editFollowupMessage(Emoji.GREY_EXCLAMATION, err.getMessage());
    }

    private static Mono<?> onMissingPermissionException(MissingPermissionException err, Context context) {
        final String missingPerm = FormatUtil.capitalizeEnum(err.getPermission());
        LociBot.DEFAULT_LOGGER.info("{Guild ID: {}} Missing permission: {}", context.getGuildId().asString(), missingPerm);

        return context.createFollowupMessage(Emoji.ACCESS_DENIED, context.localize("exception.permissions")
                .formatted(FormatUtil.capitalizeEnum(err.getPermission())));
    }

    private static Mono<?> onNoMusicException(Context context) {
        return context.createFollowupMessage(Emoji.MUTE, context.localize("exception.no.music"));
    }

    private static Mono<?> onServerAccessError(Throwable err, Context context) {
        final Throwable cause = err.getCause() != null ? err.getCause() : err;
        LociBot.DEFAULT_LOGGER.warn("{Guild ID: {}} [{}] Server access error. {}: {}\n{}",
                context.getGuildId().asString(),
                context.getFullCommandName(),
                cause.getClass().getName(),
                cause.getMessage(),
                context.getEvent().getInteraction().getData().data());

        return context.createFollowupMessage(Emoji.RED_FLAG, context.localize("exception.server.access")
                .formatted(context.getFullCommandName()));
    }

    private static Mono<?> onUnknown(Throwable err, Context context) {
        LociBot.DEFAULT_LOGGER.error("{Guild ID: %s} [%s] An unknown error occurred: %s\n%s"
                        .formatted(context.getGuildId().asString(),
                                context.getFullCommandName(),
                                Objects.requireNonNullElse(err.getMessage(), ""),
                                context.getEvent().getInteraction().getData().data()),
                err);

        return context.createFollowupMessage(Emoji.RED_FLAG, context.localize("exception.unknown")
                .formatted(context.getFullCommandName()));
    }

    public static void handleUnknownError(Throwable err) {
        LociBot.DEFAULT_LOGGER.error("An unknown error occurred: %s"
                .formatted(Objects.requireNonNullElse(err.getMessage(), "")), err);
    }

}
