package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.data.companion.CompanionType;
import club.abelix.commons.data.companion.sql.CompanionSqlHandler;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.packet.play.server.SGiveCompanion;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@NamedCommand("givecompanion")
public class GiveCompanionCommand {

    @Dependency
    private static ServerManager manager;

    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 2")
    @WithPermission("command.give.companion")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        Gamer target = session.getOfflineGamer(0, Gamer::loadPlayerId);
        CompanionType type = CompanionType.getByName(session.getArgument(1));
        if (type == null) {
            throw new CommandTranslationException("companion_type_not_found");
        }

        CompanionSqlHandler.INSTANCE.addCompanion(target.getPlayerId(), type);

        SGiveCompanion packet = new SGiveCompanion(target.getName(), type.name());
        if (target.isOnline()) {
            AbstractServer server = target.getServer();
            server.sendPacket(packet);
        }

        session.sendLocalizedMessage("companion_give_success");
    }

}
