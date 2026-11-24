package club.abelix.duels.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.FallbackExecutor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.tabcomplete.StaticTabcompleter;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.command.tabcomplete.TabcompleteManager;
import club.abelix.commons.util.request.RequestData;
import club.abelix.commons.util.request.RequestManager;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.duels.DuelsCorePlugin;
import club.abelix.duels.mode.DuelMode;
import club.abelix.duels.request.DuelRequestManager;
import club.abelix.duels.request.DuelRequestType;
import club.abelix.network.packet.play.server.SDuelOpenGui;

/**
 * @author artem
 */
@WithAlias("duel")
@WithAlias("fight")
@WithAlias("fights")
@NamedCommand("duels")
public class DuelsCommand {

    static {
        TabcompleteManager.IMP.addCompleter("duelMode", new StaticTabcompleter(
                DuelsCorePlugin.AVAILABLE_DUEL_MODES.keySet().toArray(new String[0])
        ));
    }

    @Tabcomplete("[playerName] [duelMode]")
    @WithCondition("minArgs: 1")
    @WithCondition("disableConsole")
    @FallbackExecutor
    public void sendRequest(CoreExecutionSession session) throws CommandTranslationException {
        Gamer sender = session.getGamer();
        Gamer receiver = session.getGamer(0);

        if (session.getArgumentCount() == 1) {
            sender.getServer().sendPacket(new SDuelOpenGui(
                    sender.getPlayerId(), receiver.getPlayerId()
            ));
            return;
        }

        DuelMode duelMode = DuelsCorePlugin.AVAILABLE_DUEL_MODES.get(session.getArgument(1));
        if (duelMode == null) {
            throw new CommandTranslationException("duel_command_invalid_mode");
        }

        DuelRequestManager.send(session.getGamer(), session.getGamer(0), duelMode);
    }

    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @SubcommandExecutor("accept")
    public void acceptRequest(CoreExecutionSession session) throws CommandTranslationException {
        Gamer receiver = session.getGamer();
        Gamer sender = session.getGamer(0);

        RequestManager.IMP.acceptRequest(
                sender, new RequestData(DuelRequestType.INSTANCE, sender.getPlayerId(), receiver.getPlayerId())
        );
    }

    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @SubcommandExecutor("reject")
    public void rejectRequest(CoreExecutionSession session) throws CommandTranslationException {
        Gamer receiver = session.getGamer();
        Gamer sender = session.getGamer(0);

        RequestManager.IMP.rejectRequest(
                sender, new RequestData(DuelRequestType.INSTANCE, sender.getPlayerId(), receiver.getPlayerId())
        );
    }

    @Executor
    public void fallback(CoreExecutionSession session) {
        session.sendLocalizedMessage("duel_command_help", "%modes%", String.join(", ", DuelsCorePlugin.AVAILABLE_DUEL_MODES.keySet()));
    }

}
