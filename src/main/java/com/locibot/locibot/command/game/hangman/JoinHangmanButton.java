package com.locibot.locibot.command.game.hangman;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.BaseCmdButton;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.core.game.Game;
import com.locibot.locibot.core.game.player.Player;
import com.locibot.locibot.object.Emoji;
import reactor.core.publisher.Mono;

import java.util.List;

public class JoinHangmanButton extends BaseCmdButton {

    List<Game> games;

    public JoinHangmanButton(List<Game> games) {
        super(CommandPermission.USER_GUILD, "joinHangman");
        this.games = games;
    }

    @Override
    public Mono<?> execute(Context context) {
        if (games.isEmpty()) {
            return context.createFollowupMessageEphemeral(context.localize("hangman.cannot.join")
                    .formatted("hangman", "game", HangmanCmd.CREATE_SUB_COMMAND));
        }
        final HangmanGame game = (HangmanGame) games.get(0);
        if (game.addPlayerIfAbsent(new Player(context.getGuildId(), context.getAuthorId()))) {
            return context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("hangman.joined"));
        }
        return context.createFollowupMessageEphemeral(context.localize("hangman.already.participating"));
    }
}
