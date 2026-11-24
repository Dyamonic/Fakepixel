package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.dto.dao.impl.KitDAOImpl;
import club.abelix.games.buildffa.game.kit.registry.KitRegistry;
import lombok.val;

@NamedCommand("resetkit")
public class ResetKitCommand {

    @Executor
    @WithPermission("buildffa.resetkit")
    public static void execute(BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer(0);
        val kit = KitRegistry.REGISTRY.getKitById(
                session.getIntArgument(1, "kit_id")
        );

        KitDAOImpl.INSTANCE.resetPlayerKit(gamer.getPlayerId(), String.valueOf(kit.getId()));
        gamer.sendMessage("Вы успешно сбросили кит!");
    }
}
