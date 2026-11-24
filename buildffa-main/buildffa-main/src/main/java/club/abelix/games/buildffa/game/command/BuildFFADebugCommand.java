package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.manager.event.BuildFFAEventManager;

@NamedCommand("buildffadebug")
public final class BuildFFADebugCommand {
    private static BuildFFAEventManager eventManager;

    public static void setEventManager(BuildFFAEventManager manager) {
        eventManager = manager;
    }

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("buildffa.debug")
    public static void execute(BukkitExecutionSession session) {
        if (eventManager != null) eventManager.showDebug(session.getGamer().getPlayer());
    }
}
