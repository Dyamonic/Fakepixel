package club.abelix.core.app.data.tunnel.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.tunnel.TunnelManager;

@NamedCommand("tunnel")
public class TunnelCommand {

    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.tunnel")
    @WithCondition("disableConsole")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {

        if (session.getArgument(0).equalsIgnoreCase("list")) {
            if (TunnelManager.INSTANCE.getObservableGamers(session.getGamer()).size() == 0) {
                session.getGamer().sendLocalizedMessage("core_tunnel_emptylist");
                return;
            }

            StringBuffer buf = new StringBuffer();
            session.getGamer().sendLocalizedMessage("core_tunnel_list",
                    "%size%", TunnelManager.INSTANCE.getObservableGamers(session.getGamer()).size());

            TunnelManager.INSTANCE.getObservableGamers(session.getGamer()).forEach((i) -> {
                Gamer gamer = Gamer.getOfflineGamer(i, offline -> {
                    offline.loadPlayerId();
                    offline.loadRanks();
                    offline.loadPrefix();
                });
                if (gamer != null)
                    buf.append("\n").append(gamer.isOnline() ? "§a[+]" : "§c[-]").append(" ")
                            .append(gamer.getDisplayName());
                return true;
            });

            session.getGamer().sendMessage(buf.toString().replaceFirst("\n", ""));

            return;
        }

        String target = session.getArgument(0);
        Gamer targetGamer = Gamer.getGamer(target);
        Gamer executor = session.getGamer();

        if (targetGamer == null || !targetGamer.isOnline()) {
            session.getGamer().sendLocalizedMessage("core_tunnel_invaliduser");
            return;
        }

        if (!TunnelManager.INSTANCE.isObserving(executor, targetGamer)) { // если челик еще не наблюдает за другим челом

            TunnelManager.INSTANCE.addObservableGamer(session.getGamer(), targetGamer);

            session.getGamer().sendLocalizedMessage("core_tunnel_observed",
                    "%target%", targetGamer.getDisplayName());
        } else {

            TunnelManager.INSTANCE.removeObservableGamer(session.getGamer(), targetGamer);

            session.getGamer().sendLocalizedMessage("core_tunnel_removed",
                    "%target%", targetGamer.getDisplayName());
        }

    }
}
