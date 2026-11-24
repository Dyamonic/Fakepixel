package club.abelix.games.fakevival.util;

import club.abelix.games.fakevival.Game;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CommandUtil {
    private CommandUtil() {
    }

    public static boolean noPermission(JavaPlugin plugin, CommandSender sender) {
        sender.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.no-permission", "&cYou do not have permission.")));
        return true;
    }

    public static boolean requireAdmin(JavaPlugin plugin, CommandSender sender) {
        if (!(sender instanceof Player player)) return true;
        boolean allowed = player.hasPermission("fakevival.admin") || player.hasPermission("fakevival.staff.admin")
                || player.hasPermission("smp.admin");
        if (!allowed && plugin instanceof Game game && game.getAbelixHook() != null) {
            allowed = game.getAbelixHook().isAdmin(player);
        }
        if (allowed) return true;
        sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command."));
        return false;
    }

    public static boolean requireExactAdmin(JavaPlugin plugin, CommandSender sender, String permission) {
        if (!(sender instanceof Player player)) return true;
        boolean allowed = player.hasPermission(permission) || player.hasPermission("fakevival.admin")
                || player.hasPermission("fakevival.staff.admin") || player.hasPermission("smp.admin");
        if (!allowed && plugin instanceof Game game && game.getAbelixHook() != null) {
            allowed = game.getAbelixHook().isAdmin(player);
        }
        if (allowed) return true;
        sender.sendMessage(CC.color("&cYou need ADMIN to use this command."));
        return false;
    }

    public static boolean requireDev(JavaPlugin plugin, CommandSender sender) {
        return requireDev(plugin, sender, "fakevival.dev.edititem");
    }

    public static boolean requireDev(JavaPlugin plugin, CommandSender sender, String permission) {
        if (!(sender instanceof Player player)) return true;
        boolean allowed = player.hasPermission(permission) || player.hasPermission("fakevival.developer");
        if (!allowed && plugin instanceof Game game && game.getAbelixHook() != null && game.getAbelixHook().isHooked()) {
            allowed = game.getAbelixHook().hasRankOrHigher(player, "JR_DEVELOPER");
        }
        if (allowed) return true;
        sender.sendMessage(CC.color("&cYou need DEV to use this command."));
        return false;
    }

    public static boolean adminDenied(CommandSender sender) {
        sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command."));
        return true;
    }

    public static boolean playersOnly(CommandSender sender) {
        sender.sendMessage(CC.color("&cOnly players can use this command."));
        return true;
    }

    public static Player getTarget(CommandSender sender, String name) {
        if (name == null) {
            if (!(sender instanceof Player)) {
                playersOnly(sender);
                return null;
            }
            return (Player) sender;
        }
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) sender.sendMessage(CC.color("&cThat player is not online."));
        return target;
    }
}
