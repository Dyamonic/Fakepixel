package club.abelix.api.server.base.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.slime.SlimeWorldUtil;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: api</p>
 * <p>SlimeMapCommand creation on 15.01.2024 at 00:17</p>
 */

@NamedCommand("slimemap")
public class SlimeMapCommand {

    private static void safeGamerTeleport(@NonNull String worldName, @NonNull Gamer gamer) throws CommandTranslationException {
        World bukkitWorld = Bukkit.getWorld(worldName);

        if (bukkitWorld == null) {
            throw new CommandTranslationException("internal_error");
        }

        gamer.getPlayer().teleport(bukkitWorld.getSpawnLocation());
    }


    @Executor
    @WithCondition("minArgs: 1")
    @WithPermission("command.slime.map")
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        String mapName = session.getArgumentString(0);

        if (SlimeWorldUtil.getLoadedLocalWorlds().contains(mapName)) {
            safeGamerTeleport(mapName, session.getGamer());
            return;
        }

        World slimeLoadedWorld = SlimeWorldUtil.loadSLimeWorld(mapName);
        if (slimeLoadedWorld == null) {
            throw new CommandTranslationException("internal_error");
        }
        safeGamerTeleport(slimeLoadedWorld.getName(), session.getGamer());
    }
}
