package club.abelix.core.app.command;

import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.data.gamer.Gamer;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>WhereCommand creation on 22.12.2022 at 00:58</p>
 */

@WithAlias("whereami")
@WithAlias("whereiam")
@WithAlias("гдея")
@IgnoreLogs
@NamedCommand("wherei")
public class WhereCommand {

    @Executor
    public static void execute(CoreExecutionSession session) {
        Gamer gamer = session.getGamer();

        AbstractServer server = gamer.getServer();
        String serverName = server == null ? gamer.getLanguage().getMessage("server_unknown") : server.getName();
        String proxyName = gamer.getProxy().getName();

        session.sendLocalizedMessage("core_where_i_am", "%server_name%", serverName, "%proxy_name%", proxyName);

    }
}
