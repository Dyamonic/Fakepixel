package club.abelix.api.server.lobby.reward;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;

@WithAlias("deliveryman")
@NamedCommand("rewards")
public class RewardCommand {

    @WithCondition("disableConsole")
    @Executor
    public static void execute(BukkitExecutionSession session) {
        DailyRewardsGui.open(session.getGamer());
    }

}
