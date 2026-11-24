package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.AuctionHouseManager;
import club.abelix.games.fakevival.managers.MenuManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.StringJoiner;
import java.util.UUID;

public final class AuctionHouseCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final AuctionHouseManager auctionHouseManager;
    private final MenuManager menuManager;

    public AuctionHouseCommand(JavaPlugin plugin, AuctionHouseManager auctionHouseManager, MenuManager menuManager) {
        this.plugin = plugin;
        this.auctionHouseManager = auctionHouseManager;
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(CC.color("&cOnly players can use auctionhouse."));
            return true;
        }
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("auctionhouse.enabled", true)) {
            player.sendMessage(CC.color("&cThe Auction House is currently disabled."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            menuManager.openAuctionHouse(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("browse") || args[0].equalsIgnoreCase("category")) {
            String category = args.length >= 2 ? args[1] : AuctionHouseManager.CATEGORY_ALL;
            menuManager.openAuctionHouse(player, 0, false, category);
            return true;
        }

        if (args[0].equalsIgnoreCase("mine") || args[0].equalsIgnoreCase("my") || args[0].equalsIgnoreCase("listings")) {
            String category = args.length >= 2 ? args[1] : AuctionHouseManager.CATEGORY_ALL;
            menuManager.openAuctionHouse(player, 0, true, category);
            return true;
        }

        if (args[0].equalsIgnoreCase("categories")) {
            player.sendMessage(CC.color("&6Auction Categories&7: &f" + String.join("&7, &f", auctionHouseManager.getCategories())));
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            if (args.length < 2) {
                player.sendMessage(CC.color("&cUsage: /" + label + " sell <price>"));
                return true;
            }
            double price = parsePrice(args[1]);
            if (!Double.isFinite(price)) {
                player.sendMessage(CC.color("&cInvalid price."));
                return true;
            }
            if (!Double.isFinite(price) || price <= 0) {
                player.sendMessage(CC.color("&cPrice must be greater than 0."));
                return true;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {
                player.sendMessage(CC.color("&cHold the item you want to sell."));
                return true;
            }
            if (!auctionHouseManager.canCreateListing(player)) {
                player.sendMessage(CC.color("&cYou already have the maximum number of auction listings."));
                return true;
            }
            double suggested = auctionHouseManager.getSuggestedPrice(hand);
            String id = auctionHouseManager.createListing(player.getUniqueId(), hand.clone(), price);
            player.getInventory().setItemInMainHand(null);
            player.sendMessage(CC.color("&aListed item for &e" + formatPrice(price) + " &acoins. &7ID: &f" + id));
            if (suggested > 0.0D) player.sendMessage(CC.color("&7Estimated Value: &6" + formatPrice(suggested) + " coins"));
            player.sendMessage(CC.color("&7Category: &f" + pretty(auctionHouseManager.getCategory(hand)) + " &8- &7Use &f/ah mine &7to manage it."));
            return true;
        }

        if (args[0].equalsIgnoreCase("buy")) {
            if (args.length < 2) {
                player.sendMessage(CC.color("&cUsage: /" + label + " buy <listing id>"));
                return true;
            }
            if (!auctionHouseManager.buy(player, args[1])) {
                player.sendMessage(CC.color("&cCould not buy that listing. It may be gone, yours, too expensive, or your inventory may be full."));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reclaim") || args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("remove")) {
            if (args.length < 2) {
                player.sendMessage(CC.color("&cUsage: /" + label + " reclaim <listing id>"));
                return true;
            }
            UUID owner = auctionHouseManager.getOwner(args[1]);
            if (owner == null || !owner.equals(player.getUniqueId())) {
                player.sendMessage(CC.color("&cThat is not one of your active auction listings."));
                return true;
            }
            if (auctionHouseManager.reclaim(player, args[1])) {
                player.sendMessage(CC.color("&aReclaimed auction listing &f" + args[1] + "&a."));
            } else {
                player.sendMessage(CC.color("&cCould not reclaim that listing. Make sure your inventory has space."));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("view")) {
            if (args.length < 2) {
                player.sendMessage(CC.color("&cUsage: /" + label + " info <listing id>"));
                return true;
            }
            sendListingInfo(player, args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage(CC.color("&6Auction House"));
            player.sendMessage(CC.color("&e/" + label + " &7- Open all auctions"));
            player.sendMessage(CC.color("&e/" + label + " browse [category] &7- Browse a category"));
            player.sendMessage(CC.color("&e/" + label + " mine [category] &7- View your listings"));
            player.sendMessage(CC.color("&e/" + label + " sell <price> &7- Sell the held item &8(1k, 1m, 1b supported)"));
            player.sendMessage(CC.color("&e/" + label + " buy <id> &7- Buy a listing directly"));
            player.sendMessage(CC.color("&e/" + label + " reclaim <id> &7- Cancel your listing"));
            player.sendMessage(CC.color("&e/" + label + " info <id> &7- View listing details"));
            player.sendMessage(CC.color("&e/" + label + " categories &7- List auction categories"));
            return true;
        }

        player.sendMessage(CC.color("&cUnknown auction command. Use &e/" + label + " help&c."));
        return true;
    }

    private String formatPrice(double price) {
        if (price == Math.rint(price)) return String.valueOf((long) price);
        return String.format(java.util.Locale.US, "%.2f", price);
    }

    private double parsePrice(String input) {
        if (input == null || input.isBlank()) return Double.NaN;
        String normalized = input.trim().replace(",", "").replace("_", "").toLowerCase(java.util.Locale.ROOT);
        double multiplier = 1.0D;
        if (normalized.endsWith("k")) {
            multiplier = 1_000.0D;
            normalized = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("m")) {
            multiplier = 1_000_000.0D;
            normalized = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("b")) {
            multiplier = 1_000_000_000.0D;
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        try {
            return Double.parseDouble(normalized) * multiplier;
        } catch (Exception ignored) {
            return Double.NaN;
        }
    }

    private void sendListingInfo(Player player, String id) {
        ItemStack item = auctionHouseManager.getItem(id);
        UUID owner = auctionHouseManager.getOwner(id);
        if (item == null || owner == null) {
            player.sendMessage(CC.color("&cThat auction listing does not exist."));
            return;
        }
        player.sendMessage(CC.color("&6Auction Listing &f" + id));
        player.sendMessage(CC.color("&7Item: &f" + pretty(item.getType().name()) + " &7x&f" + item.getAmount()));
        player.sendMessage(CC.color("&7Category: &f" + pretty(auctionHouseManager.getCategory(id))));
        player.sendMessage(CC.color("&7Price: &e" + formatPrice(auctionHouseManager.getPrice(id)) + " coins"));
        double suggested = auctionHouseManager.getSuggestedPrice(item);
        if (suggested > 0.0D) player.sendMessage(CC.color("&7Estimated Value: &6" + formatPrice(suggested) + " coins"));
        player.sendMessage(CC.color(owner.equals(player.getUniqueId()) ? "&7Owner: &aYou" : "&7Owner: " + auctionHouseManager.getSellerDisplay(id)));
        player.sendMessage(CC.color(owner.equals(player.getUniqueId()) ? "&eUse /ah reclaim " + id + " to cancel it." : "&eUse /ah buy " + id + " to buy it."));
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "Item";
        StringJoiner joiner = new StringJoiner(" ");
        for (String part : text.toLowerCase(java.util.Locale.ROOT).replace('-', '_').split("_")) {
            if (part.isBlank()) continue;
            joiner.add(part.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + part.substring(1));
        }
        return ChatColor.stripColor(joiner.toString());
    }
}
