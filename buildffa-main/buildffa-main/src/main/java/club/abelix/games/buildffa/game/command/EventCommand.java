package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.manager.event.BuildFFAEventManager;

@NamedCommand("event")
public final class EventCommand {
    private static BuildFFAEventManager eventManager;

    public static void setEventManager(BuildFFAEventManager manager) {
        eventManager = manager;
    }

    @Executor
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) {
        if (eventManager != null) eventManager.openPlayerMenu(session.getGamer().getPlayer());
    }
}
