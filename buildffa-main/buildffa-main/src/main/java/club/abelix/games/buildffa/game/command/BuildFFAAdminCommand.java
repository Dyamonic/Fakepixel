package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.manager.staff.CombatIntegrityManager;

@NamedCommand("bffa")
public final class BuildFFAAdminCommand {
    private static CombatIntegrityManager manager;

    public static void setManager(CombatIntegrityManager value) {
        manager = value;
    }

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("buildffa.staff")
    public static void execute(BukkitExecutionSession session) {
        if (manager != null) manager.executeAlerts(session.getGamer().getPlayer(), session.getArguments());
    }
}
