package club.abelix.core.rejoiner.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.rejoiner.CoreRejoinerCorePlugin;
import lombok.RequiredArgsConstructor;

@WithAlias("return")
@NamedCommand("rejoin")
@RequiredArgsConstructor
public final class RejoinCommand {

    private final CoreRejoinerCorePlugin plugin;

    @Executor
    @WithCondition("disableConsole")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        Gamer sender = session.getGamer();

        AbstractServer server = plugin.getRejoinManager().getRejoinableServer(sender);

        if (server == null) {
            throw new CommandTranslationException("rejoin_server_not_found");
        }

        sender.sendLocalizedMessage("rejoin_server_found", "%server%", server.getName());
        sender.sendTo(server);
    }
}
