package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.event.gamer.PlayerVanishEvent;

@NamedCommand("vanish")
@WithAlias("v")
public final class VanishCommand {

    @Dependency
    private static ServerManager manager;

    @Executor
    @WithPermission("command.vanish")
    public static void execute(CoreExecutionSession session) {
        Gamer sender = session.getGamer();

        boolean vanished = !sender.isVanished();

        if (sender.setVanished(vanished)) {
            session.sendLocalizedMessage(vanished ? "core_vanish_enabled" : "core_vanish_disabled");

            manager.broadcastStaff("command.staffchat", vanished ? "core_vanish_enabled_broadcast" : "core_vanish_disabled_broadcast",
                    "%user%", sender.getDisplayName());
        }

        CoreApplication.getInstance().getPluginManager().callEvent(new PlayerVanishEvent(sender, vanished));
    }
}
