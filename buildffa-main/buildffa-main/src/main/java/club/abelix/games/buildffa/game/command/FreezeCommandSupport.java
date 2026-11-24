package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.games.buildffa.game.BuildFFAGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

final class FreezeCommandSupport {
    static void execute(BuildFFAGame game, Gamer staff, String[] args, boolean freeze) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> execute(game, staff, args, freeze));
            return;
        }
        if (!game.hasPermission(staff.getPlayer(), "buildffa.staff")) {
            staff.sendLocalizedMessage("buildffa_freeze_no_permission");
            return;
        }
        if (args.length != 1) {
            staff.sendLocalizedMessage(freeze ? "buildffa_freeze_usage" : "buildffa_unfreeze_usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            staff.sendLocalizedMessage("buildffa_freeze_player_not_found", "%player%", args[0]);
            return;
        }
        boolean changed = freeze ? game.getFrozenPlayers().putIfAbsent(target.getUniqueId(), target.getLocation().clone()) == null
                : game.getFrozenPlayers().remove(target.getUniqueId()) != null;
        if (!changed) {
            staff.sendLocalizedMessage(freeze ? "buildffa_freeze_already_frozen" : "buildffa_freeze_not_frozen", "%player%", target.getName());
            return;
        }
        if (freeze) {
            target.setVelocity(new Vector());
            target.closeInventory();
            target.leaveVehicle();
        }
        staff.sendLocalizedMessage(freeze ? "buildffa_freeze_success" : "buildffa_unfreeze_success", "%player%", target.getName());
        Gamer gamer = Gamer.getGamer(target);
        if (gamer != null) gamer.sendLocalizedMessage(freeze ? "buildffa_freeze_target" : "buildffa_unfreeze_target");
    }
}