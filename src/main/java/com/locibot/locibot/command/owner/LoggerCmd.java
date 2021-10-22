package com.locibot.locibot.command.owner;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import static com.locibot.locibot.LociBot.DEFAULT_LOGGER;

public class LoggerCmd extends BaseCmd {

    public LoggerCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, "logger", "Change the level of a logger");
        this.addOption(option -> option.name("name")
                .description("Can be 'root' to change root logger")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue()));
        this.addOption(option -> option.name("level")
                .description("The new logger level")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .choices(DiscordUtil.toOptions(LogLevel.class)));
    }

    @Override
    public Mono<?> execute(Context context) {
        final String name = context.getOptionAsString("name").orElseThrow();
        final LogLevel logLevel = context.getOptionAsEnum(LogLevel.class, "level").orElseThrow();
        final Level level = Level.toLevel(logLevel.name(), null);
        if (level == null) {
            return Mono.error(new CommandException("`%s` in not a valid level.".formatted(logLevel)));
        }

        final Logger logger;
        if ("root".equalsIgnoreCase(name)) {
            logger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        } else {
            logger = (Logger) LoggerFactory.getLogger(name);
        }

        logger.setLevel(level);

        DEFAULT_LOGGER.info("Logger '{}' set to level {}", name, level);
        return context.createFollowupMessage(Emoji.INFO, "Logger `%s` set to level `%s`.".formatted(name, level));
    }

    private enum LogLevel {
        OFF,
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

}
