package club.abelix.api.server.lobby.gamehistory;


import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.language.Lang;
import lombok.val;

@WithAlias("history")
@NamedCommand("gamehistory")
public class GameHistoryCommand {

    @WithCondition("disableConsole")
    @Executor
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        if (session.getArgumentCount() == 0) {
            GameHistoryLobbyGui.open(session.getGamer());
            return;
        }

        if (session.getSender().hasPermission("gamehistory.view.other")) {
            val target = session.getOfflineGamer(0, gamer -> {
                gamer.loadRanks();
                gamer.loadPrefixAndUpdateName();
            });
            if (target == null) {
                session.sendMessage(Lang.of("player_not_found").compile());
                return;
            }
            GameHistoryLobbyGui.openOther(session.getGamer(), target);
        } else {
            session.sendMessage(Lang.of("no_permission").compile());
        }
    }

}
