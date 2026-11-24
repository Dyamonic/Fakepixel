package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.EconomyManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoinsCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final EconomyManager economyManager;

    public CoinsCommand(JavaPlugin plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("balance") || args[0].equalsIgnoreCase("bal")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(CC.color("&cUsage: /coins <player>"));
                return true;
            }
            sender.sendMessage(CC.color("&eCoins: &f" + economyManager.format(economyManager.getCoins(player))));
            return true;
        }
        if (args.length == 1 && Bukkit.getPlayer(args[0]) != null) {
            Player target = Bukkit.getPlayer(args[0]);
            assert target != null;
            sender.sendMessage(CC.color("&e" + target.getName() + "'s Coins: &f" + economyManager.format(economyManager.getCoins(target))));
            return true;
        }
        if (args[0].equalsIgnoreCase("baltop")) {
            sender.sendMessage(CC.color("&6&lCoin BalTop"));
            for (String row : economyManager.baltop(10)) sender.sendMessage(row);
            return true;
        }
        if (!sender.hasPermission("fakevival.admin.coins") && !sender.hasPermission("fakevival.devcontrols.admin") && !sender.hasPermission("smp.dev")) {
            sender.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.no-permission", "&cNo permission.")));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /coins give|take|set|reset <player> [amount]"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        target.getUniqueId();
        if (args[0].equalsIgnoreCase("reset")) {
            economyManager.setCoins(target.getUniqueId(), 0, "admin reset by " + sender.getName());
            sender.sendMessage(CC.color("&aReset &f" + args[1] + "&a's coins."));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(CC.color("&cEnter an amount."));
            return true;
        }
        double amount;
        try { amount = Double.parseDouble(args[2]); } catch (Exception e) { sender.sendMessage(CC.color("&cInvalid amount.")); return true; }
        if (args[0].equalsIgnoreCase("give")) economyManager.giveCoins(target.getUniqueId(), amount, "admin give by " + sender.getName());
        else if (args[0].equalsIgnoreCase("take")) economyManager.takeCoins(target.getUniqueId(), amount, "admin take by " + sender.getName());
        else if (args[0].equalsIgnoreCase("set")) economyManager.setCoins(target.getUniqueId(), amount, "admin set by " + sender.getName());
        else {
            sender.sendMessage(CC.color("&cUsage: /coins give|take|set|reset <player> [amount]"));
            return true;
        }
        sender.sendMessage(CC.color("&aUpdated &f" + args[1] + "&a's coins to &e" + economyManager.format(economyManager.getCoins(target.getUniqueId())) + "&a."));
        return true;
    }
}
