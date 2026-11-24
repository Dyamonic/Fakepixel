package club.abelix.api.base.box.command;

import club.abelix.api.base.box.service.BoxesStorageService;
import club.abelix.api.base.box.type.StandardBoxType;
import club.abelix.api.base.box.util.RngUtil;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

@NamedCommand("testboxrewardchances")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TestBoxRewardChances {

    BoxesStorageService boxesStorageService;

    @Executor
    @WithPermission("command.box.testrewards")
    public void execute(BukkitExecutionSession session) throws CommandException {
        val type = session.getArgument(0);
        val box = boxesStorageService.findByTypeAndRarity(StandardBoxType.valueOf(type), Rarity.valueOf(session.getArgument(1)));
        val iterations = session.getIntArgument(2, "iteration");
        val luck = session.getIntArgument(3, "luck");

        if (box.isEmpty()) throw new CommandTranslationException("test_box_reward_chances_box_not_found");

        int commonCount = 0;
        int rareCount = 0;
        int epicCount = 0;
        int legendaryCount = 0;

        for (int i = 0; i < iterations; i++) {
            val reward = RngUtil.pickWithLuck(box.get(), luck);
            switch (reward.getRarity()) {
                case COMMON -> commonCount++;
                case RARE -> rareCount++;
                case EPIC -> epicCount++;
                case LEGENDARY -> legendaryCount++;
            }
        }

        session.sendMessage("§6§lTest Box Reward Chances Results:");
        session.sendMessage("§eCommon: §a" + commonCount + " §7(" + (commonCount * 100.0 / iterations) + "%)");
        session.sendMessage("§eRare: §a" + rareCount + " §7(" + (rareCount * 100.0 / iterations) + "%)");
        session.sendMessage("§eEpic: §a" + epicCount + " §7(" + (epicCount * 100.0 / iterations) + "%)");
        session.sendMessage("§eLegendary: §a" + legendaryCount + " §7(" + (legendaryCount * 100.0 / iterations) + "%)");

    }

}
