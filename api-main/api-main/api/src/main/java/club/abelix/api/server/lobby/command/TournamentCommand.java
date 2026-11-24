package club.abelix.api.server.lobby.command;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;

@WithAlias("турнир")
@NamedCommand("tournament")
public class TournamentCommand {

    @WithCondition("disableConsole")
    @Executor
    public static void execute(BukkitExecutionSession session) {
        FalconAPI.getInstance()
                .getTournamentService()
                .openTournamentGui(session.getGamer());
    }
}
