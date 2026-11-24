package club.abelix.api.server.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import lombok.val;

@NamedCommand("setgamehpcommand")
public class SetGameHpCommand {

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("owner.funny.setgamehp")
    public static void execute(BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer();

        double hp = session.getIntArgument(0, "hp");

        val game = gamer.getGame();
        if (game == null) {
            session.sendMessage("Вы не в игре");
            return;
        }

        val player = gamer.getPlayer();
        game.getMetadata().set(player, "hp", hp);
        session.sendMessage("Здоровье установлено на " + hp);
        player.setMaxHealth(hp);
        player.setHealth(hp);
    }
}
