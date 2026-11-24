package club.abelix.api.base.box.command;

import club.abelix.api.base.box.reward.service.RewardSelectorService;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

@NamedCommand("testboxrewardselector")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TestBoxRewardSelectorCommand {

    RewardSelectorService rewardSelectorService;

    @Executor
    @WithPermission("command.box.test.rewardselector")
    public void execute(BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer(0);
        int weight = session.getIntArgument(1, "weight");

        rewardSelectorService.giveReward(gamer, weight);
    }

    @WithPermission("command.box.test.rewardselector")
    @SubcommandExecutor("test")
    public void test(BukkitExecutionSession session) throws CommandException {
        rewardSelectorService.giveReward(
                session.getGamer(),
                session.getIntArgument(0, "weight"),
                true
        );
    }

}
