package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.BackManager;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BackCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final BackManager backManager;

    public BackCommand(JavaPlugin plugin, BackManager backManager) {
        this.plugin = plugin;
        this.backManager = backManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!player.hasPermission("fakevival.command.back")) return CommandUtil.noPermission(plugin, sender);
        if (plugin instanceof Game game && !game.getNetworkService().isOverworld()) {
            player.sendMessage(CC.color(game.getNetworkService().isDungeon()?"&cYou cannot use /back inside dungeons.":"&cYou can only use /back on overworld servers."));
            return true;
        }

        Location location = backManager.getBackLocation(player);
        if (location == null || location.getWorld() == null) {
            player.sendMessage(CC.color("&cYou do not have a back location."));
            return true;
        }

        player.teleport(location);
        player.sendMessage(CC.color("&aTeleported back to your last location."));
        return true;
    }
}
