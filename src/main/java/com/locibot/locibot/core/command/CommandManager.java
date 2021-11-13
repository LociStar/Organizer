package com.locibot.locibot.core.command;

import com.locibot.locibot.LociBot;
import com.locibot.locibot.command.event_commands.buttons.AcceptButtonCmd;
import com.locibot.locibot.command.event_commands.buttons.DeclineButtonCmd;
import com.locibot.locibot.command.event_commands.buttons.JoinEventButtonCmd;
import com.locibot.locibot.command.event_commands.buttons.LeaveEventButtonCmd;
import com.locibot.locibot.command.game.hangman.JoinHangmanButton;
import com.locibot.locibot.command.moderation.botRegister.RegisterButtonCmd;
import com.locibot.locibot.command.currency.CoinsCmd;
import com.locibot.locibot.command.currency.LeaderboardCmd;
import com.locibot.locibot.command.currency.TransferCoinsCmd;
import com.locibot.locibot.command.donator.DonatorGroup;
import com.locibot.locibot.command.event_commands.EventGroup;
import com.locibot.locibot.command.fun.ChatCmd;
import com.locibot.locibot.command.fun.Hello;
import com.locibot.locibot.command.fun.JokeCmd;
import com.locibot.locibot.command.fun.ThisDayCmd;
import com.locibot.locibot.command.game.GameGroup;
import com.locibot.locibot.command.gamestats.GameStatsGroup;
import com.locibot.locibot.command.group.Accept;
import com.locibot.locibot.command.group.Decline;
import com.locibot.locibot.command.group.GroupGroup;
import com.locibot.locibot.command.image.ImageGroup;
import com.locibot.locibot.command.image.Rule34Cmd;
import com.locibot.locibot.command.info.InfoGroup;
import com.locibot.locibot.command.moderation.ModerationGroup;
import com.locibot.locibot.command.owner.OwnerGroup;
import com.locibot.locibot.command.register.RegisterGroup;
import com.locibot.locibot.command.setting.SettingGroup;
import com.locibot.locibot.command.standalone.*;
import com.locibot.locibot.command.util.MathCmd;
import com.locibot.locibot.command.util.UrbanCmd;
import com.locibot.locibot.command.util.WeatherCmd;
import com.locibot.locibot.command.util.WikipediaCmd;
import com.locibot.locibot.command.util.poll.PollCmd;
import com.locibot.locibot.command.util.translate.TranslateCmd;
import com.locibot.locibot.core.game.Game;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.object.ExceptionHandler;
import discord4j.rest.service.ApplicationService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

public class CommandManager {

    private static final Map<String, BaseCmd> COMMANDS_MAP;
    private static final Map<String, BaseCmdButton> BUTTONS_MAP;
    private static final List<Game> games = new ArrayList<>();

    static {
        COMMANDS_MAP = CommandManager.initialize(
                new InfoGroup(), new ImageGroup(), new ModerationGroup(), new OwnerGroup(),
                new GameStatsGroup(), new SettingGroup(),
                new DonatorGroup(), new GameGroup(games), new GroupGroup(),
                new RegisterGroup(), new EventGroup(),
                // Image
                new Rule34Cmd(), // TODO Improvement: Add to Image group when Discord autocompletion is implemented
                // Standalone
                new PingCmd(), new HelpCmd(), new AchievementsCmd(), new FeedbackCmd(), new InviteCmd(),
                // Currency
                new CoinsCmd(), new LeaderboardCmd(), new TransferCoinsCmd(),
                // Fun
                new ChatCmd(), new JokeCmd(), new ThisDayCmd(),
                // Util
                new MathCmd(), new UrbanCmd(), new WeatherCmd(), new WikipediaCmd(),
                new TranslateCmd(), new PollCmd(),
                //Global
                new Hello(), new Accept(), new Decline());
        BUTTONS_MAP = CommandManager.initializeButtons(
                new RegisterButtonCmd(), new AcceptButtonCmd(), new DeclineButtonCmd(), new JoinEventButtonCmd(), new LeaveEventButtonCmd(), new JoinHangmanButton(games)
        );
    }

    private static Map<String, BaseCmd> initialize(BaseCmd... cmds) {
        final Map<String, BaseCmd> map = new LinkedHashMap<>(cmds.length);
        for (final BaseCmd cmd : cmds) {
            if (map.putIfAbsent(cmd.getName(), cmd) != null) {
                LociBot.DEFAULT_LOGGER.error("Command name collision between {} and {}",
                        cmd.getClass().getSimpleName(), map.get(cmd.getName()).getClass().getSimpleName());
            }
        }
        LociBot.DEFAULT_LOGGER.info("{} commands initialized", map.size());
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, BaseCmdButton> initializeButtons(BaseCmdButton... cmds) {
        final Map<String, BaseCmdButton> map = new LinkedHashMap<>(cmds.length);
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
                    requests.forEach(applicationCommandRequest -> System.out.println(applicationCommandRequest.name()));
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
        final Mono<Long> registerGuildCommands = Flux.fromIterable(COMMANDS_MAP.values())
                .filter(cmd -> cmd.getCategory() != CommandCategory.OWNER || cmd.getPermission() == CommandPermission.USER_GUILD || cmd.getPermission() == CommandPermission.ADMIN)
                .map(BaseCmd::asRequest)
                .collectList()
                .flatMapMany(requests ->
                        applicationService.bulkOverwriteGuildApplicationCommand(applicationId, guildId, requests))
                .count()
                .doOnNext(cmdCount -> LociBot.DEFAULT_LOGGER.info("{} guild commands registered (ID: {})",
                        cmdCount, Config.OWNER_GUILD_ID))
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
