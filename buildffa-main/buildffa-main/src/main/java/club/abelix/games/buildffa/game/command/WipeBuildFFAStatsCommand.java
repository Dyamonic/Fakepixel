package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.manager.staff.BuildFFAWipeManager;

@NamedCommand("wipebuildffastats")
public final class WipeBuildFFAStatsCommand {
    private static BuildFFAWipeManager manager;

    public static void setManager(BuildFFAWipeManager wipeManager) {
        manager = wipeManager;
    }

    @Executor
    @WithCondition("disableConsole")
    @WithPermission(BuildFFAGame.WIPE_BUILDFFA_STATS_PERMISSION)
    public static void execute(BukkitExecutionSession session) throws CommandException {
        Gamer staff = session.getGamer();
        String[] arguments = session.getArguments();
        if (arguments.length < 1) {
            staff.sendLocalizedMessage("buildffa_wipe_usage");
            return;
        }
        Gamer target = session.getGamer(0);
        if (target == null || target.getPlayerId() <= 0) {
            staff.sendLocalizedMessage("buildffa_wipe_player_not_found", "%player%", arguments[0]);
            return;
        }
        manager.execute(staff, target, arguments.length > 1 && arguments[1].equalsIgnoreCase("confirm"));
    }
}
