package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.manager.progression.CosmeticManager;

@NamedCommand("cosmetics")
public class CosmeticsCommand {
    private static CosmeticManager cosmeticManager;

    public static void setCosmeticManager(CosmeticManager manager) {
        cosmeticManager = manager;
    }

    @Executor
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) {
        if (cosmeticManager != null) cosmeticManager.open(session.getGamer().getPlayer());
    }
}
