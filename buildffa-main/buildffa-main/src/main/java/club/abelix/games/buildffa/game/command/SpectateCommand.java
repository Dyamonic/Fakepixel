package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.manager.staff.SpectateManager;
import club.abelix.games.buildffa.game.BuildFFAGame;

@NamedCommand("spectate")
public final class SpectateCommand {
    private static SpectateManager manager;

    public static void setManager(SpectateManager spectateManager) {
        manager = spectateManager;
    }

    @Executor
    @WithCondition("disableConsole")
    @WithPermission(BuildFFAGame.SPECTATE_PERMISSION)
    public static void execute(BukkitExecutionSession session) {
        if (manager != null) manager.execute(session.getGamer(), session.getArguments());
    }
}
