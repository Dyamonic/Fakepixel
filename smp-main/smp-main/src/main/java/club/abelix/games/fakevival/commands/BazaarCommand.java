package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.MenuManager;
import club.abelix.games.fakevival.managers.BazaarManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BazaarCommand implements CommandExecutor {
    private final MenuManager menuManager;
    private final BazaarManager bazaarManager;

    public BazaarCommand(MenuManager menuManager, BazaarManager bazaarManager) {
        this.menuManager = menuManager;
        this.bazaarManager = bazaarManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(CC.color("&cOnly players can use the market."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            menuManager.openBazaar(player);
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("buy")) {
            int amount = parseInt(args, 1);
            if (bazaarManager.instantBuy(player, args[1], amount)) {
                player.sendMessage(CC.color("&aBought &f" + amount + "x " + bazaarManager.getDisplayName(args[1]) + " &afrom the Market."));
            } else {
                player.sendMessage(CC.color("&cCould not buy that item. Check your coins and inventory space."));
            }
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("sell")) {
            int amount = parseInt(args, 1);
            if (bazaarManager.instantSell(player, args[1], amount)) {
                player.sendMessage(CC.color("&aSold &f" + amount + "x " + bazaarManager.getDisplayName(args[1]) + " &ato the Market."));
            } else {
                player.sendMessage(CC.color("&cCould not sell that item. Check your inventory."));
            }
            return true;
        }

        if (args.length >= 4 && args[0].equalsIgnoreCase("buyorder")) {
            int amount = parseInt(args, 0);
            double price = parseDouble(args[3]);
            if (bazaarManager.createBuyOrder(player, args[1], amount, price)) {
                player.sendMessage(CC.color("&aCreated a buy order for &f" + amount + "x " + bazaarManager.getDisplayName(args[1]) + "&a."));
            } else {
                player.sendMessage(CC.color("&cCould not create that buy order."));
            }
            return true;
        }

        if (args.length >= 4 && args[0].equalsIgnoreCase("sellorder")) {
            int amount = parseInt(args, 0);
            double price = parseDouble(args[3]);
            if (bazaarManager.createSellOrder(player, args[1], amount, price)) {
                player.sendMessage(CC.color("&aCreated a sell order for &f" + amount + "x " + bazaarManager.getDisplayName(args[1]) + "&a."));
            } else {
                player.sendMessage(CC.color("&cCould not create that sell order."));
            }
            return true;
        }

        player.sendMessage(CC.color("&6Market"));
        player.sendMessage(CC.color("&e/" + label + " &7- Open the Market"));
        player.sendMessage(CC.color("&e/" + label + " buy <item> [amount] &7- Buy instantly"));
        player.sendMessage(CC.color("&e/" + label + " sell <item> [amount] &7- Sell instantly"));
        player.sendMessage(CC.color("&e/" + label + " buyorder <item> <amount> <price> &7- Create a buy order"));
        player.sendMessage(CC.color("&e/" + label + " sellorder <item> <amount> <price> &7- Create a sell order"));
        return true;
    }

    private int parseInt(String[] args, int fallback) {
        if (args.length <= 2) return fallback;
        try {
            return Math.max(1, Integer.parseInt(args[2]));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private double parseDouble(String value) {
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) ? Math.max(0.0D, parsed) : 0.0D;
        } catch (Exception ignored) {
            return 0.0D;
        }
    }
}
