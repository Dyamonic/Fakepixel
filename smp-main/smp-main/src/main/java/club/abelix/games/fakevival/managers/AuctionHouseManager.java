package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.hooks.AbelixHook;
import club.abelix.games.fakevival.mmo.MMOItemValueService;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Locale;
import java.util.UUID;

public final class AuctionHouseManager {
    public static final String CATEGORY_ALL = "all";
    private static final List<String> CATEGORIES = List.of("all", "blocks", "resources", "gear", "farming", "combat", "tools", "misc");

    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final EconomyAuditManager auditManager;
    private final MMOItemValueService itemValueService;
    private final AbelixHook abelixHook;
    private SmpAlertService alertService;

    public AuctionHouseManager(JavaPlugin plugin, DataManager dataManager, EconomyManager economyManager, EconomyAuditManager auditManager, MMOItemValueService itemValueService, AbelixHook abelixHook) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.economyManager = economyManager;
        this.auditManager = auditManager;
        this.itemValueService = itemValueService;
        this.abelixHook = abelixHook;
    }

    public void setAlertService(SmpAlertService alertService) { this.alertService = alertService; }

    public String createListing(UUID owner, ItemStack item, double price) {
        String id = nextListingId();
        long createdAt = System.currentTimeMillis();
        dataManager.getData().set("auctionhouse.listings." + id + ".owner", owner.toString());
        Player seller = Bukkit.getPlayer(owner);
        String sellerName = seller == null ? fallbackSellerName(owner) : seller.getName();
        String sellerPrefix = resolveLivePrefix(seller);
        dataManager.getData().set("auctionhouse.listings." + id + ".seller_uuid", owner.toString());
        dataManager.getData().set("auctionhouse.listings." + id + ".seller_name", sellerName);
        dataManager.getData().set("auctionhouse.listings." + id + ".seller_prefix", sellerPrefix);
        dataManager.getData().set("auctionhouse.listings." + id + ".seller_display_name", formatSellerDisplay(sellerPrefix, sellerName));
        dataManager.getData().set("auctionhouse.listings." + id + ".price", price);
        dataManager.getData().set("auctionhouse.listings." + id + ".item", item);
        dataManager.getData().set("auctionhouse.listings." + id + ".created-at", createdAt);
        dataManager.save();
        if (auditManager != null) {
            auditManager.logAuction(owner, "listed", id, item, price, "Created listing");
            auditManager.logItem(owner, "ah-listed", item, "Listing " + id + " for " + EconomyManager.formatCompact(price) + " coins");
        }
        return id;
    }

    public List<String> getListings() {
        org.bukkit.configuration.ConfigurationSection section = dataManager.getData().getConfigurationSection("auctionhouse.listings");
        if (section == null) return new ArrayList<>();
        List<String> listings = new ArrayList<>(section.getKeys(false));
        listings.sort(Comparator.comparingLong(this::getCreatedAt).reversed());
        return listings;
    }

    public List<String> getListings(String category) {
        String normalized = normalizeCategory(category);
        if (CATEGORY_ALL.equals(normalized)) return getListings();
        List<String> listings = new ArrayList<>();
        for (String id : getListings()) {
            if (normalized.equals(getCategory(id))) listings.add(id);
        }
        return listings;
    }

    public List<String> getListings(UUID owner) {
        List<String> listings = new ArrayList<>();
        for (String id : getListings()) {
            UUID listingOwner = getOwner(id);
            if (owner.equals(listingOwner)) listings.add(id);
        }
        return listings;
    }

    public List<String> getListings(UUID owner, String category) {
        String normalized = normalizeCategory(category);
        List<String> listings = new ArrayList<>();
        for (String id : getListings(normalized)) {
            UUID listingOwner = getOwner(id);
            if (owner.equals(listingOwner)) listings.add(id);
        }
        return listings;
    }

    public List<String> getCategories() {
        return CATEGORIES;
    }

    public String normalizeCategory(String category) {
        if (category == null || category.isBlank()) return CATEGORY_ALL;
        String normalized = category.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        return CATEGORIES.contains(normalized) ? normalized : CATEGORY_ALL;
    }

    public String getCategory(String id) {
        return getCategory(getItem(id));
    }

    public String getCategory(ItemStack item) {
        if (item == null || item.getType().isAir()) return "misc";
        Material material = item.getType();
        String name = material.name().toLowerCase(Locale.ROOT);
        if (name.contains("sword") || name.contains("bow") || name.contains("crossbow") || name.contains("arrow")
                || name.contains("trident") || name.contains("mace") || name.contains("shield")) return "combat";
        if (name.contains("helmet") || name.contains("chestplate") || name.contains("leggings") || name.contains("boots")
                || name.contains("pickaxe") || name.contains("axe") || name.contains("shovel") || name.contains("hoe")
                || name.contains("elytra") || name.contains("horse_armor")) return "gear";
        if (name.endsWith("_log") || name.endsWith("_wood") || name.contains("ingot") || name.contains("nugget")
                || name.contains("gem") || name.contains("diamond") || name.contains("coal") || name.contains("ore")
                || name.contains("raw_") || name.contains("dust") || name.contains("shard")) return "resources";
        if (name.contains("seed") || name.contains("wheat") || name.contains("carrot") || name.contains("potato")
                || name.contains("beetroot") || name.contains("melon") || name.contains("pumpkin") || name.contains("sapling")
                || name.contains("cocoa") || name.contains("sugar_cane")) return "farming";
        if (material.isBlock()) return "blocks";
        if (name.contains("fishing_rod") || name.contains("shears") || name.contains("brush") || name.contains("flint_and_steel")
                || name.contains("bucket") || name.contains("compass") || name.contains("clock")) return "tools";
        return "misc";
    }

    public ItemStack getItem(String id) {
        return dataManager.getData().getItemStack("auctionhouse.listings." + id + ".item");
    }

    public double getPrice(String id) {
        return dataManager.getData().getDouble("auctionhouse.listings." + id + ".price", 0.0D);
    }

    public double getEstimatedValue(ItemStack item) {
        return itemValueService == null ? 0.0D : itemValueService.calculateValue(item);
    }

    public double getSuggestedPrice(ItemStack item) {
        return itemValueService == null ? 0.0D : itemValueService.getAuctionSuggestedPrice(item);
    }

    public boolean hasRandomizedValueData(ItemStack item) {
        return itemValueService != null && itemValueService.hasRandomizedData(item);
    }

    public long getCreatedAt(String id) {
        long createdAt = dataManager.getData().getLong("auctionhouse.listings." + id + ".created-at", 0L);
        return createdAt > 0L ? createdAt : listingTime(id);
    }

    public int getMaxListings(Player player) {
        int max = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("auctionhouse.max-listings", 8);
        if (player != null && player.hasPermission("fakevival.auctionhouse.unlimited")) return Integer.MAX_VALUE;
        int extra = player == null ? 0 : dataManager.getData().getInt("players." + player.getUniqueId() + ".store.upgrades.extra_auction_listings", 0);
        return Math.max(1, max + Math.max(0, extra));
    }

    public boolean canCreateListing(Player player) {
        if (player == null) return false;
        if (economyManager.isEcoFrozen(player)) return false;
        return getListings(player.getUniqueId()).size() < getMaxListings(player);
    }

    public UUID getOwner(String id) {
        try { return UUID.fromString(Objects.requireNonNull(dataManager.getData().getString("auctionhouse.listings." + id + ".owner"))); } catch (Exception e) { return null; }
    }

    public String getSellerDisplay(String id) {
        UUID owner = getOwner(id);
        if (owner == null) return CC.color("&7Unknown");
        Player live = Bukkit.getPlayer(owner);
        String fallbackName = dataManager.getData().getString("auctionhouse.listings." + id + ".seller_name", fallbackSellerName(owner));
        if (live != null && live.isOnline()) {
            String prefix = resolveLivePrefix(live);
            String display = formatSellerDisplay(prefix, live.getName());
            cacheSellerDisplay(id, live.getName(), prefix, display);
            return display;
        }
        String storedDisplay = dataManager.getData().getString("auctionhouse.listings." + id + ".seller_display_name", "");
        if (storedDisplay != null && !storedDisplay.isBlank()) return CC.color(resolveSellerPlaceholders(storedDisplay, fallbackName, dataManager.getData().getString("auctionhouse.listings." + id + ".seller_prefix", "")));
        String storedPrefix = dataManager.getData().getString("auctionhouse.listings." + id + ".seller_prefix", "");
        return formatSellerDisplay(storedPrefix, fallbackName);
    }

    public boolean buy(Player buyer, String id) {
        ItemStack item = getItem(id);
        UUID owner = getOwner(id);
        double price = getPrice(id);
        if (item == null || owner == null || price <= 0) return false;
        if (owner.equals(buyer.getUniqueId())) return false;
        if (economyManager.isEcoFrozen(buyer.getUniqueId()) || economyManager.isEcoFrozen(owner)) return false;
        if (hasSpaceFor(buyer.getInventory(), item)) return false;
        if (!economyManager.takeCoins(buyer.getUniqueId(), price, "auction buy " + id)) return false;
        double feePercent = Math.clamp(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getDouble("auctionhouse.saleFeePercent", 5.0D), 0.0D, 100.0D);
        double fee = price * feePercent / 100.0D;
        if (plugin instanceof club.abelix.games.fakevival.Game game) fee = game.getMayorManager().applyMayorModifier(
                Bukkit.getPlayer(owner), club.abelix.games.fakevival.mayor.MayorModifierType.AUCTION_FEE, fee);
        double sellerPayout = Math.max(0.0D, price - fee);
        economyManager.giveCoins(owner, sellerPayout, "auction sale " + id + " fee=" + fee);
        Player onlineSeller = Bukkit.getPlayer(owner);
        if (onlineSeller != null && fee > 0.0D) onlineSeller.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7Auction fee: &e" + economyManager.format(fee) + " coins&7. Net proceeds: &a" + economyManager.format(sellerPayout) + " coins&7."));
        buyer.getInventory().addItem(item.clone());
        dataManager.getData().set("auctionhouse.listings." + id, null);
        dataManager.save();
        notifySale(buyer, owner, item, price);
        if (auditManager != null) {
            auditManager.logAuction(buyer.getUniqueId(), "bought", id, item, price, "Bought from " + owner);
            auditManager.logAuction(owner, "sold", id, item, price, "Sold to " + buyer.getName());
            auditManager.logItem(buyer.getUniqueId(), "ah-bought", item, "Listing " + id);
            auditManager.logItem(owner, "ah-sold", item, "Listing " + id);
        }
        if (alertService != null && price >= alertService.threshold(SmpAlertService.Type.AUCTION_BID)) {
            String itemName = itemName(item);
            String seller = fallbackSellerName(owner);
            alertService.alert(SmpAlertService.Type.AUCTION_BID, buyer.getUniqueId(),
                    "&c[SMP ALERT] &e" + buyer.getName() + " &7bid &6" + economyManager.format(price) + " coins &7on auction &e" + itemName + "&7.",
                    "**" + buyer.getName() + "** bid **" + economyManager.format(price) + " coins** on `" + itemName + "` in Auction House. Server: `" + alertService.serverId() + "`",
                    "player=" + buyer.getName() + "/" + buyer.getUniqueId() + " amount=" + price + " auction=" + id + " seller=" + seller + " server=" + alertService.serverId() + " at=" + System.currentTimeMillis());
        }
        return true;
    }

    public void sendPendingNotifications(Player player) {
        List<String> messages = dataManager.getData().getStringList("players." + player.getUniqueId() + ".auctionhouse.notifications");
        if (messages.isEmpty()) return;
        for (String message : messages) player.sendMessage(message);
        dataManager.getData().set("players." + player.getUniqueId() + ".auctionhouse.notifications", null);
        dataManager.save();
    }

    public boolean reclaim(Player owner, String id) {
        ItemStack item = getItem(id);
        UUID listingOwner = getOwner(id);
        double price = getPrice(id);
        if (item == null || listingOwner == null || !listingOwner.equals(owner.getUniqueId())) return false;
        if (economyManager.isEcoFrozen(owner)) return false;
        if (hasSpaceFor(owner.getInventory(), item)) return false;
        owner.getInventory().addItem(item.clone());
        dataManager.getData().set("auctionhouse.listings." + id, null);
        dataManager.save();
        if (auditManager != null) {
            auditManager.logAuction(owner.getUniqueId(), "reclaimed", id, item, price, "Cancelled listing");
            auditManager.logItem(owner.getUniqueId(), "ah-reclaimed", item, "Listing " + id);
        }
        return true;
    }

    private boolean hasSpaceFor(Inventory inventory, ItemStack item) {
        int remaining = item.getAmount();
        for (ItemStack content : inventory.getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                remaining -= item.getMaxStackSize();
            } else if (content.isSimilar(item)) {
                remaining -= Math.max(0, content.getMaxStackSize() - content.getAmount());
            }
            if (remaining <= 0) return false;
        }
        return true;
    }

    private String nextListingId() {
        String id;
        do {
            id = Long.toString(System.currentTimeMillis(), 36).toLowerCase(Locale.ROOT)
                    + "-"
                    + Long.toString(System.nanoTime() & 0xfffffL, 36).toLowerCase(Locale.ROOT);
        } while (dataManager.getData().contains("auctionhouse.listings." + id));
        return id;
    }

    private long listingTime(String id) {
        try {
            return Long.parseLong(id);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private void notifySale(Player buyer, UUID sellerId, ItemStack item, double price) {
        String itemName = itemName(item);
        String formattedPrice = economyManager.format(price);
        OfflinePlayer sellerProfile = Bukkit.getOfflinePlayer(sellerId);
        String sellerName = sellerProfile.getName() == null ? sellerId.toString() : sellerProfile.getName();
        Player sellerPlayer = Bukkit.getPlayer(sellerId);
        String sellerDisplay = sellerPlayer == null ? sellerName : formatSellerDisplay(resolveLivePrefix(sellerPlayer), sellerPlayer.getName());
        buyer.sendMessage(ChatColor.translateAlternateColorCodes('&',
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("auctionhouse.notifications.buyer-bought",
                                "&6Auction House &8> &aYou bought &f%item% &afor &e%price% coins &afrom &f%seller%&a.")
                        .replace("%item%", itemName)
                        .replace("%price%", formattedPrice)
                        .replace("%seller%", sellerDisplay)
                        .replace("%buyer%", buyer.getName())));

        String sellerMessage = ChatColor.translateAlternateColorCodes('&',
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("auctionhouse.notifications.seller-sold",
                                "&6Auction House &8> &aYour &f%item% &asold to &f%buyer% &afor &e%price% coins&a.")
                        .replace("%item%", itemName)
                        .replace("%price%", formattedPrice)
                        .replace("%seller%", sellerName)
                        .replace("%buyer%", buyer.getName()));
        Player seller = Bukkit.getPlayer(sellerId);
        if (seller != null && seller.isOnline()) {
            seller.sendMessage(sellerMessage);
            return;
        }
        List<String> pending = dataManager.getData().getStringList("players." + sellerId + ".auctionhouse.notifications");
        pending.add(sellerMessage);
        dataManager.getData().set("players." + sellerId + ".auctionhouse.notifications", pending);
        dataManager.save();
    }

    private String itemName(ItemStack item) {
        if (item == null) return "Item";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String stripped = ChatColor.stripColor(meta.getDisplayName());
            if (stripped != null && !stripped.isBlank()) return stripped + " x" + item.getAmount();
        }
        return pretty(item.getType().name()) + " x" + item.getAmount();
    }

    private String resolveLivePrefix(Player player) {
        if (player == null || abelixHook == null) return "";
        try {
            String prefix = abelixHook.getPrefix(player);
            return prefix == null || prefix.equalsIgnoreCase("null") ? "" : prefix;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String formatSellerDisplay(String prefix, String name) {
        String sellerName = name == null || name.isBlank() ? "Unknown" : name;
        String sellerPrefix = prefix == null ? "" : prefix;
        return CC.color(resolveSellerPlaceholders("%seller_prefix%%seller_name%", sellerName, sellerPrefix));
    }

    private String resolveSellerPlaceholders(String text, String sellerName, String sellerPrefix) {
        String name = sellerName == null || sellerName.isBlank() ? "Unknown" : sellerName;
        String prefix = sellerPrefix == null ? "" : sellerPrefix;
        return String.valueOf(text)
                .replace("%abelix_prefix%", prefix)
                .replace("%seller_prefix%", prefix)
                .replace("%player%", name)
                .replace("%seller%", name)
                .replace("%seller_name%", name);
    }

    private void cacheSellerDisplay(String id, String sellerName, String sellerPrefix, String sellerDisplay) {
        String base = "auctionhouse.listings." + id;
        String nextName = sellerName == null ? "Unknown" : sellerName;
        String nextPrefix = sellerPrefix == null ? "" : sellerPrefix;
        String nextDisplay = sellerDisplay == null ? nextName : sellerDisplay;
        if (Objects.equals(dataManager.getData().getString(base + ".seller_name", ""), nextName)
                && Objects.equals(dataManager.getData().getString(base + ".seller_prefix", ""), nextPrefix)
                && Objects.equals(dataManager.getData().getString(base + ".seller_display_name", ""), nextDisplay)) {
            return;
        }
        dataManager.getData().set(base + ".seller_name", nextName);
        dataManager.getData().set(base + ".seller_prefix", nextPrefix);
        dataManager.getData().set(base + ".seller_display_name", nextDisplay);
        dataManager.save();
    }

    private String fallbackSellerName(UUID owner) {
        if (owner == null) return "Unknown";
        OfflinePlayer profile = Bukkit.getOfflinePlayer(owner);
        return profile.getName() == null ? owner.toString() : profile.getName();
    }

    private String pretty(String text) {
        String[] parts = text.toLowerCase(Locale.ROOT).replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? "Item" : builder.toString();
    }
}
