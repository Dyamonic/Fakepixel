package club.abelix.api.server.lobby.snow;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.util.exclusive.ExclusivePeriod;
import lombok.val;

@NamedCommand("snow")
@IgnoreLogs
public class SnowCommand {

    @Executor
    public static void snow(BukkitExecutionSession session) {
        if (!ExclusivePeriod.NEW_YEAR.isAvailable()) {
            session.sendLocalizedMessage("snow_unavailable");
            return;
        }

        val gamer = session.getGamer();
        int snowStatus = session.getGamer().getProperty("Lobby/Snow");

        if (snowStatus == 0) {
            gamer.setProperty("Lobby/Snow", 1);
            BiomeChangePacketListener.toggleSnowForPlayer(gamer.getPlayer());
            gamer.sendLocalizedMessage("snow_enabled");
        } else {
            gamer.setProperty("Lobby/Snow", 0);
            BiomeChangePacketListener.toggleSnowForPlayer(gamer.getPlayer());
            gamer.sendMessage("Snow disabled");
        }
    }
}


