package com.locibot.locibot.command.game.hangman;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.core.game.GameCmd;
import com.locibot.locibot.core.game.player.Player;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.locibot.locibot.LociBot.DEFAULT_LOGGER;

public class HangmanCmd extends GameCmd<HangmanGame> {

    protected static final String JOIN_SUB_COMMAND = "join";
    protected static final String CREATE_SUB_COMMAND = "create";
    private final WordsList easyWords;
    private final WordsList hardWords;
    public HangmanCmd() {
        super("hangman", "Start or join a Hangman game", ApplicationCommandOption.Type.SUB_COMMAND_GROUP);
        this.addOption(option -> option.name(JOIN_SUB_COMMAND)
                .description("Join a Hangman game")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue()));
        this.addOption(option -> option.name(CREATE_SUB_COMMAND)
                .description("Start a Hangman game")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder().name("difficulty")
                        .description("The difficulty of the word to find, easy by default")
                        .required(false)
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .choices(DiscordUtil.toOptions(Difficulty.class))
                        .build()));

        this.easyWords = new WordsList(
                "https://gist.githubusercontent.com/deekayen/4148741/raw/01c6252ccc5b5fb307c1bb899c95989a8a284616/1-1000.txt");
        this.hardWords = new WordsList(
                "https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt");
    }

    @Override
    public Mono<?> execute(Context context) {
        final String subCmd = context.getSubCommandName().orElseThrow();
        if (subCmd.equals(JOIN_SUB_COMMAND)) {
            return this.join(context);
        } else if (subCmd.equals(CREATE_SUB_COMMAND)) {
            return this.create(context);
        }
        return Mono.error(new IllegalStateException());
    }

    private Mono<?> join(Context context) {
        final HangmanGame game = this.getGame(context.getChannelId());
        if (game == null) {
            return Mono.error(new CommandException(context.localize("hangman.cannot.join")
                    .formatted(context.getCommandName(), context.getSubCommandGroupName().orElseThrow(), CREATE_SUB_COMMAND)));
        }
        if (game.addPlayerIfAbsent(new Player(context.getGuildId(), context.getAuthorId()))) {
            return context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("hangman.joined"));
        }
        return Mono.error(new CommandException(context.localize("hangman.already.participating")));
    }

    private Mono<?> create(Context context) {
        final Difficulty difficulty = context.getOptionAsEnum(Difficulty.class, "difficulty").orElse(Difficulty.EASY);
        return this.loadWords(difficulty)
                .then(Mono.defer(() -> {
                    if (this.isGameStarted(context.getChannelId())) {
                        return Mono.error(new CommandException(context.localize("hangman.already.started")
                                .formatted(context.getCommandName(), context.getSubCommandGroupName().orElseThrow(), CREATE_SUB_COMMAND)));
                    }

                    final String word = this.getWord(difficulty);
                    final HangmanGame game = new HangmanGame(context, difficulty, word);
                    game.addPlayerIfAbsent(new Player(context.getGuildId(), context.getAuthorId()));
                    this.addGame(context.getChannelId(), game);
                    return game.start()
                            .then(game.show())
                            .doOnError(err -> game.destroy());
                }));
    }

    private Mono<List<String>> loadWords(Difficulty difficulty) {
        if (difficulty == Difficulty.EASY && !this.easyWords.isLoaded()) {
            return this.easyWords.load()
                    .doOnSuccess(__ -> DEFAULT_LOGGER.info("Hangman word list (difficulty: easy) loaded"));
        } else if (difficulty == Difficulty.HARD && !this.hardWords.isLoaded()) {
            return this.hardWords.load()
                    .doOnSuccess(__ -> DEFAULT_LOGGER.info("Hangman word list (difficulty: hard) loaded"));
        }
        return Mono.empty();
    }

    private String getWord(HangmanCmd.Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> this.easyWords.getRandomWord();
            case HARD -> this.hardWords.getRandomWord();
        };
    }

    protected enum Difficulty {
        EASY, HARD
    }

}