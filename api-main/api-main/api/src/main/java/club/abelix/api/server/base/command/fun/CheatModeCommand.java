package club.abelix.api.server.base.command.fun;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import lombok.val;

@NamedCommand("cheatmode")
@IgnoreLogs
public class CheatModeCommand {
    private final static String CHEAT_PROPERTY = "Cheat/Mode";

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("command.cheat.mode")
    @WithCondition("minArgs: 1")
    @SubcommandExecutor("personal")
    public static void personalExecute(BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer();
        val game = gamer.getGame();

        if (game == null) {
            gamer.sendMessage("Only game command!");
            return;
        }


        int newStatus = session.getIntArgument(0, "power");
        game.getMetadata().set(gamer.getPlayer(), CHEAT_PROPERTY, newStatus);
        gamer.sendMessage("Chet mode value: " + newStatus);
    }

    @Executor
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 2")
    @WithPermission("command.cheat.mode")
    @SubcommandExecutor("target")
    public static void targetExecute(BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer(1);
        val game = gamer.getGame();

        if (game == null) {
            gamer.sendMessage("Only game command!");
            return;
        }


        int newStatus = session.getIntArgument(0, "power");
        game.getMetadata().set(gamer.getPlayer(), CHEAT_PROPERTY, newStatus);
        session.getGamer().sendMessage("Chet mode value: " + newStatus + " " + gamer.getDisplayName());
    }


    @Executor
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    @WithPermission("command.cheat.mode")
    @SubcommandExecutor("all")
    public static void allExecute(BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer();
        val game = gamer.getGame();

        if (game == null) {
            gamer.sendMessage("Only game command!");
            return;
        }


        int newStatus = session.getIntArgument(0, "power");
        for (Gamer player : game.getGamers()) {
            game.getMetadata().set(player.getPlayer(), CHEAT_PROPERTY, newStatus);
        }

        gamer.sendMessage("Chet mode value: " + newStatus + " (ALL GAME)");
    }
}
