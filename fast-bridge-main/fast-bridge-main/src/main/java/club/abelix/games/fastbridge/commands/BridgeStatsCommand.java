package club.abelix.games.fastbridge.commands;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.fastbridge.game.gui.DetailedBridgingStatsGui;

@WithAlias("bstats")
@NamedCommand("bridgestats")
public class BridgeStatsCommand {

    @Executor
    public static void handle(final BukkitExecutionSession session) {
        new DetailedBridgingStatsGui(session.getGamer());
    }

}
