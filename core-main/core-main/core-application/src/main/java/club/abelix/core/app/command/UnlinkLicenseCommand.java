package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.auth.AuthSqlHandler;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.packet.play.both.BLicenseStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

@NamedCommand("unlinklicense")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UnlinkLicenseCommand {

    @Executor
    @WithPermission("command.unlinklicense")
    public void execute(final CoreExecutionSession session) {
        val gamer = Gamer.getOfflineGamer(session.getArgument(0), Gamer::load);

        if (gamer == null) {
            session.sendLocalizedMessage("gamer_not_found", "%name%", session.getArgument(0));
            return;
        }

        AuthSqlHandler.INSTANCE.setPremiumStatus(gamer.getPlayerId(), false);

        ServerManager.IMP.getProxies().forEach(p -> p.sendPacket(new BLicenseStatus(gamer.getLowerName(), false)));

        session.sendMessage("Unlinked license for gamer " + gamer.getName());

        ServerManager.IMP.broadcastStaff("command.staffchat", "unlinklicense_broadcast",
                "%target%", gamer.getName(),
                "%sender%", session.getGamer().getDisplayName()
        );
    }

}
