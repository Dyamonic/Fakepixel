package club.abelix.api.server.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.network.common.enums.GameState;
import lombok.val;

@NamedCommand("start")
public class StartCommand {

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("game.force.start")
    public static void execute(BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer();
        val game = gamer.getGame();

        if (game == null) {
            session.sendMessage("You are not in the game!");
            return;
        }

        if (game.getGameState() != GameState.STAND_BY) {
            session.sendMessage("The game has already started!");
            return;
        }

        int time = 3;

        if (session.getArgumentCount() > 0) {
            time = session.getIntArgument(0, "time");

            if (time < 3)
                time = 3;
        }

        game.sendLocalizedMessage(gamer, "game_force_started");

        game.getWaitingManager().setStartTime(time);
        game.getWaitingManager().startTimer();
    }
}
