package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.manager.progression.PrestigeManager;

@NamedCommand("prestige")
public class PrestigeCommand {
    private static PrestigeManager prestigeManager;

    public static void setPrestigeManager(PrestigeManager manager) {
        prestigeManager = manager;
    }

    @Executor
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) {
        if (prestigeManager != null) prestigeManager.prestige(session.getGamer().getPlayer());
    }
}
