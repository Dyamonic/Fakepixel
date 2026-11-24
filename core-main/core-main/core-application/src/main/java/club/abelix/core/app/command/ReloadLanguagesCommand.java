package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.language.Language;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.Proxy;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.network.packet.play.server.SReloadLanguages;

import java.util.Arrays;

@NamedCommand("reloadlanguages")
@WithAlias("reloadlanguage")
public final class ReloadLanguagesCommand {

    @Dependency
    private static ServerManager manager;

    @Executor
    @WithCondition("permission: command.reloadlanguages")
    public static void executor(CoreExecutionSession session) {
        boolean updateAll = Arrays.asList(session.getArguments()).contains("@all");

        SReloadLanguages packet = new SReloadLanguages();

        if (updateAll) {
            Language.reloadLanguages();

            for (AbstractServer server : manager.getServers()) {
                server.sendPacket(packet);
            }

            for (Proxy proxy : manager.getProxies()) {
                proxy.sendPacket(packet);
            }

            session.sendPrefixedMessage("All translations on all servers have been updated.");
        } else {
            if (session.isConsole()) {
                Language.reloadLanguages();

                session.sendPrefixedMessage("Translation successfully updated");
            } else {
                session.getGamer().sendToServer(packet);
                session.sendPrefixedMessage("Translation on " + session.getGamer().getServer() + " successfully updated");
            }
        }
    }

}
