package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.manager.event.BuildFFAEventManager;

@NamedCommand("eventstats")
public final class EventStatsCommand {
    private static BuildFFAEventManager eventManager;

    public static void setEventManager(BuildFFAEventManager manager) {
        eventManager = manager;
    }

    @Executor
    @WithCondition("disableConsole")
    @WithPermission(BuildFFAGame.EVENT_STATS_PERMISSION)
    public static void execute(BukkitExecutionSession session) {
        if (eventManager != null) eventManager.openStats(session.getGamer().getPlayer());
    }
}
