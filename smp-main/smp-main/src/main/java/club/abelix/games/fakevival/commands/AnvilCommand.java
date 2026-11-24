package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;

public final class AnvilCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public AnvilCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!player.hasPermission("fakevival.command.anvil")) return CommandUtil.noPermission(plugin, sender);
        player.openInventory(Bukkit.createInventory(player, InventoryType.ANVIL, CC.color("&8Anvil")));
        return true;
    }
}
