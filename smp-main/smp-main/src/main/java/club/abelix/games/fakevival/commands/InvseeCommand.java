package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class InvseeCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public InvseeCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return CommandUtil.playersOnly(sender);
        if (!sender.hasPermission("fakevival.command.invsee")) return CommandUtil.noPermission(plugin, sender);
        if (args.length == 0) {
            sender.sendMessage(CC.color("&cUsage: /invsee <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(CC.color("&cThat player is not online."));
            return true;
        }

        ((Player) sender).openInventory(target.getInventory());
        return true;
    }
}
