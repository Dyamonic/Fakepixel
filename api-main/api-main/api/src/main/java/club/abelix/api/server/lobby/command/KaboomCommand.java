package club.abelix.api.server.lobby.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.cooldown.WithCooldown;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@NamedCommand("kaboom")
public class KaboomCommand {

    @Executor
    @WithCooldown(value = 600_000, currentServerOnly = false) // 10 минут
    @WithPermission("command.kaboom")
    @WithCondition("disableStaff")
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();

        for (Gamer target : Gamer.getNonVanishedGamers()) {
            gamer.sendLocalizedMessage("lobby_kaboomed_player",
                    "%player%", target.getName());

            target.sendTitle("§c§lKABOOM!", "§eОт "+gamer.getColoredName(), 0, 30, 0);

            Player player = target.getPlayer();

            World world = player.getWorld();
            world.strikeLightning(player.getLocation()); // ебашим молнией

            player.setVelocity(player.getVelocity().add(new Vector(0, 3.5, 0))); // подкидываем
        }
    }

}
