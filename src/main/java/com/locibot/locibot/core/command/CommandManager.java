package com.locibot.locibot.core.command;

import com.locibot.locibot.LociBot;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.object.ExceptionHandler;
import discord4j.rest.service.ApplicationService;
import org.reflections.Reflections;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CommandManager {

    private static final Map<String, BaseCmd> COMMANDS_MAP;
    private static final Map<String, BaseCmdButton> BUTTONS_MAP;


    static {
        COMMANDS_MAP = CommandManager.initialize();
        //new Rule34Cmd(), TODO Improvement: Add to Image group when Discord autocompletion is implemented
        BUTTONS_MAP = CommandManager.initializeButtons();
    }

    private static Map<String, BaseCmd> initialize() {
        Set<Class<?>> list = new Reflections("com.locibot.locibot.command").getTypesAnnotatedWith(CmdAnnotation.class);
        List<BaseCmd> cmds = new ArrayList<>();
        list.forEach(aClass -> {
            try {
                cmds.add((BaseCmd) aClass.getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                LociBot.DEFAULT_LOGGER.error(e.getMessage());
            }
        });
        final Map<String, BaseCmd> map = new LinkedHashMap<>(cmds.size());
        for (final BaseCmd cmd : cmds) {
            if (map.putIfAbsent(cmd.getName(), cmd) != null) {
                LociBot.DEFAULT_LOGGER.error("Command name collision between {} and {}",
                        cmd.getClass().getSimpleName(), map.get(cmd.getName()).getClass().getSimpleName());
            }
        }
        LociBot.DEFAULT_LOGGER.info("{} commands initialized", map.size());
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, BaseCmdButton> initializeButtons() {
        Set<Class<?>> list = new Reflections("com.locibot.locibot.command").getTypesAnnotatedWith(ButtonAnnotation.class);
        List<BaseCmdButton> cmds = new ArrayList<>();
        list.forEach(aClass -> {
            try {
                cmds.add((BaseCmdButton) aClass.getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                LociBot.DEFAULT_LOGGER.error(e.getMessage());
            }
        });
        final Map<String, BaseCmdButton> map = new LinkedHashMap<>(cmds.size());
        for (final BaseCmdButton cmd : cmds) {
            if (map.putIfAbsent(cmd.getName(), cmd) != null) {
                LociBot.DEFAULT_LOGGER.error("Button command name collision between {} and {}",
                        cmd.getClass().getSimpleName(), map.get(cmd.getName()).getClass().getSimpleName());
            }
        }
        LociBot.DEFAULT_LOGGER.info("{} button commands initialized", map.size());
        return Collections.unmodifiableMap(map);
    }

    public static Mono<Void> register(ApplicationService applicationService, long applicationId) {
        final Mono<Long> registerGuildCommands = Flux.fromIterable(COMMANDS_MAP.values())
                .filter(cmd -> cmd.getPermission() != CommandPermission.USER_GLOBAL)
                .map(BaseCmd::asRequest)
                .collectList()
                .flatMapMany(requests -> {
                    return applicationService
                            .bulkOverwriteGuildApplicationCommand(applicationId, Config.OWNER_GUILD_ID, requests);
                })
                .count()
                .doOnNext(cmdCount -> LociBot.DEFAULT_LOGGER.info("{} guild commands registered (ID: {})",
                        cmdCount, Config.OWNER_GUILD_ID))
                .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err)));

//        final Mono<Long> registerGuildCommands2 = Flux.fromIterable(COMMANDS_MAP.values())
//                .filter(cmd -> cmd.getCategory() == CommandCategory.OWNER || cmd.getPermission() == CommandPermission.USER_GUILD || cmd.getPermission() == CommandPermission.ADMIN)
//                .map(BaseCmd::asRequest)
//                .collectList()
//                .flatMapMany(requests -> applicationService
//                        .bulkOverwriteGuildApplicationCommand(applicationId, 317219629611089921L, requests))
//                .count()
//                .doOnNext(cmdCount -> LociBot.DEFAULT_LOGGER.info("{} guild commands registered (ID: {})",
//                        cmdCount, 317219629611089921L))
//                .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err)));

        final Mono<Long> registerGlobalCommands = Flux.fromIterable(COMMANDS_MAP.values())
                .filter(cmd -> cmd.getCategory() != CommandCategory.OWNER)
                .filter(baseCmd -> baseCmd.getPermission() == CommandPermission.USER_GLOBAL)
                .map(BaseCmd::asRequest)
                .collectList()
                .flatMapMany(requests -> applicationService
                        .bulkOverwriteGlobalApplicationCommand(applicationId, requests))
                .count()
                .doOnNext(cmdCount -> LociBot.DEFAULT_LOGGER.info("{} global commands registered", cmdCount))
                .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err)));

        return registerGuildCommands.and(registerGlobalCommands);
    }

    public static Mono<Void> registerGuildCommands(ApplicationService applicationService, long applicationId, long guildId) {
        if (guildId == Config.OWNER_GUILD_ID)
            return Mono.empty();
        final Mono<Long> registerGuildCommands = Flux.fromIterable(COMMANDS_MAP.values())
                .filter(cmd -> cmd.getCategory() != CommandCategory.OWNER || cmd.getPermission() == CommandPermission.USER_GUILD || cmd.getPermission() == CommandPermission.ADMIN)
                .map(BaseCmd::asRequest)
                .collectList()
                .flatMapMany(requests ->
                        applicationService.bulkOverwriteGuildApplicationCommand(applicationId, guildId, requests))
                .count()
                .doOnNext(cmdCount -> LociBot.DEFAULT_LOGGER.info("{} guild commands registered (ID: {})",
                        cmdCount, guildId))
                .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err)));

        return registerGuildCommands.and(Mono.empty());
    }

    public static Map<String, BaseCmd> getCommands() {
        return COMMANDS_MAP;
    }

    public static Map<String, BaseCmdButton> getButtonCommands() {
        return BUTTONS_MAP;
    }

    public static BaseCmdButton getButtonCommand(String name) {
        final BaseCmdButton cmd = BUTTONS_MAP.get(name);
        if (cmd != null) {
            return cmd;
        }
        return BUTTONS_MAP.values().stream()
                .filter(it -> it.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static BaseCmd getCommand(String name) {
        final BaseCmd cmd = COMMANDS_MAP.get(name);
        if (cmd != null) {
            return cmd;
        }
        return COMMANDS_MAP.values().stream()
                .filter(it -> it instanceof BaseCmdGroup)
                .flatMap(it -> ((BaseCmdGroup) it).getCommands().stream())
                .filter(it -> it.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
