package club.abelix.api.server.lobby.warmode.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.lobby.warmode.WarModeManager;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;

/**
 * @author pro0xy
 * <p>Created on 03.09.2023 at 15:10</p>
 */

@NamedCommand("warmode")
public class WarModeCommand {

    @Executor
    @WithPermission("command.warmode")
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();

        if (WarModeManager.INSTANCE.getWarMode()){
            WarModeManager.INSTANCE.endWar();
            return;
        }

        WarModeManager.INSTANCE.startWar(gamer);
    }
}
