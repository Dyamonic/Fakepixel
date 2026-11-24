package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>ProxyCommand creation on 22.12.2022 at 01:08</p>
 */

@NamedCommand("proxy")
@WithAlias("bungee")
public class ProxyCommand {

    @Executor
    public static void execute(CoreExecutionSession session) {
        Gamer gamer = session.getGamer();

        session.sendLocalizedMessage("core_my_proxy", "%proxy_name%", gamer.getProxy().getName());

    }

}
