package club.abelix.api.server.base.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.util.nms.locale.LocaleUtil;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import org.bukkit.entity.Player;

@NamedCommand("testitem")
public class TestItemTranslationCommand {

    @Executor
    @WithPermission("command.test.item.translation")
    public static void execute(BukkitExecutionSession session) {
        Player player = session.getPlayer();

        player.sendMessage("§eItem in hand: §b" + LocaleUtil.translateWithAmount(
                session.getLanguage(), player.getItemInHand()
        ));
    }
}
