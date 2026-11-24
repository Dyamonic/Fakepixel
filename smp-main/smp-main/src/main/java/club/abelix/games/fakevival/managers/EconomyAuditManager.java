package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class EconomyAuditManager {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final DataManager dataManager;

    public EconomyAuditManager(JavaPlugin plugin, DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public boolean isFrozen(UUID uuid) {
        return dataManager.getData().getBoolean("players." + uuid + ".eco-frozen", false);
    }

    public void setFrozen(UUID uuid, boolean frozen) {
        dataManager.getData().set("players." + uuid + ".eco-frozen", frozen);
        dataManager.save();
    }

    public void logEconomy(UUID player, String action, double amount, double balance, String detail) {
        String id = nextId();
        String path = "audit.economy." + player + "." + id;
        dataManager.getData().set(path + ".time", System.currentTimeMillis());
        dataManager.getData().set(path + ".action", action);
        dataManager.getData().set(path + ".amount", amount);
        dataManager.getData().set(path + ".balance", balance);
        dataManager.getData().set(path + ".detail", detail);
        dataManager.save();
    }

    public void logAuction(UUID player, String action, String listingId, ItemStack item, double price, String detail) {
        String id = nextId();
        String path = "audit.auction." + player + "." + id;
        dataManager.getData().set(path + ".time", System.currentTimeMillis());
        dataManager.getData().set(path + ".action", action);
        dataManager.getData().set(path + ".listing-id", listingId);
        dataManager.getData().set(path + ".item", itemSummary(item));
        dataManager.getData().set(path + ".price", price);
        dataManager.getData().set(path + ".detail", detail);
        dataManager.save();
    }

    public void logItem(UUID player, String action, ItemStack item, String detail) {
        String id = nextId();
        String path = "audit.items." + player + "." + id;
        dataManager.getData().set(path + ".time", System.currentTimeMillis());
        dataManager.getData().set(path + ".action", action);
        dataManager.getData().set(path + ".item", itemSummary(item));
        dataManager.getData().set(path + ".detail", detail);
        dataManager.save();
    }

    public void logTrade(String tradeId, UUID left, String leftName, UUID right, String rightName, List<ItemStack> leftItems, List<ItemStack> rightItems, double leftCoins, double rightCoins) {
        String path = "audit.trades." + tradeId;
        dataManager.getData().set(path + ".time", System.currentTimeMillis());
        dataManager.getData().set(path + ".left.uuid", left.toString());
        dataManager.getData().set(path + ".left.name", leftName);
        dataManager.getData().set(path + ".left.coins", leftCoins);
        dataManager.getData().set(path + ".left.items", leftItems);
        dataManager.getData().set(path + ".left.summary", summarize(leftItems, leftCoins));
        dataManager.getData().set(path + ".right.uuid", right.toString());
        dataManager.getData().set(path + ".right.name", rightName);
        dataManager.getData().set(path + ".right.coins", rightCoins);
        dataManager.getData().set(path + ".right.items", rightItems);
        dataManager.getData().set(path + ".right.summary", summarize(rightItems, rightCoins));
        dataManager.getData().set(path + ".rolled-back", false);
        dataManager.save();

        for (ItemStack item : leftItems) logItem(left, "trade-gave", item, "Trade " + tradeId + " to " + rightName);
        for (ItemStack item : rightItems) logItem(right, "trade-gave", item, "Trade " + tradeId + " to " + leftName);
        for (ItemStack item : rightItems) logItem(left, "trade-received", item, "Trade " + tradeId + " from " + rightName);
        for (ItemStack item : leftItems) logItem(right, "trade-received", item, "Trade " + tradeId + " from " + leftName);
    }

    public List<String> economyHistory(UUID uuid, int limit) {
        return sectionHistory("audit.economy." + uuid, limit, entry -> {
            double amount = dataManager.getData().getDouble(entry.path() + ".amount", 0.0D);
            double balance = dataManager.getData().getDouble(entry.path() + ".balance", 0.0D);
            return "&8" + time(entry.time()) + " &e" + dataManager.getData().getString(entry.path() + ".action", "?")
                    + " &f" + EconomyManager.formatCompact(amount)
                    + " &7bal=&f" + EconomyManager.formatCompact(balance)
                    + " &7" + dataManager.getData().getString(entry.path() + ".detail", "");
        });
    }

    public List<String> auctionHistory(UUID uuid, int limit) {
        return sectionHistory("audit.auction." + uuid, limit, entry -> "&8" + time(entry.time()) + " &e"
                + dataManager.getData().getString(entry.path() + ".action", "?")
                + " &7id=&f" + dataManager.getData().getString(entry.path() + ".listing-id", "?")
                + " &7price=&f" + EconomyManager.formatCompact(dataManager.getData().getDouble(entry.path() + ".price", 0.0D))
                + " &7" + dataManager.getData().getString(entry.path() + ".item", ""));
    }

    public List<String> itemHistory(UUID uuid, int limit) {
        return sectionHistory("audit.items." + uuid, limit, entry -> "&8" + time(entry.time()) + " &e"
                + dataManager.getData().getString(entry.path() + ".action", "?")
                + " &f" + dataManager.getData().getString(entry.path() + ".item", "")
                + " &7" + dataManager.getData().getString(entry.path() + ".detail", ""));
    }

    public List<String> tradeHistory(UUID uuid, int limit) {
        org.bukkit.configuration.ConfigurationSection section = dataManager.getData().getConfigurationSection("audit.trades");
        if (section == null) return List.of();
        List<HistoryEntry> entries = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            String path = "audit.trades." + id;
            if (!uuid.toString().equals(dataManager.getData().getString(path + ".left.uuid"))
                    && !uuid.toString().equals(dataManager.getData().getString(path + ".right.uuid"))) continue;
            entries.add(new HistoryEntry(id, path, dataManager.getData().getLong(path + ".time", 0L)));
        }
        entries.sort(Comparator.comparingLong(HistoryEntry::time).reversed());
        List<String> rows = new ArrayList<>();
        for (HistoryEntry entry : entries.subList(0, Math.min(limit, entries.size()))) {
            rows.add("&8" + time(entry.time()) + " &e" + entry.id()
                    + " &7L=&f" + dataManager.getData().getString(entry.path() + ".left.summary", "nothing")
                    + " &7R=&f" + dataManager.getData().getString(entry.path() + ".right.summary", "nothing")
                    + (dataManager.getData().getBoolean(entry.path() + ".rolled-back", false) ? " &cROLLED BACK" : ""));
        }
        return rows;
    }

    public RollbackResult rollbackTrade(String tradeId, EconomyManager economyManager) {
        String path = "audit.trades." + tradeId;
        if (!dataManager.getData().isConfigurationSection(path)) return RollbackResult.NOT_FOUND;
        if (dataManager.getData().getBoolean(path + ".rolled-back", false)) return RollbackResult.ALREADY_ROLLED_BACK;

        UUID left = uuid(dataManager.getData().getString(path + ".left.uuid"));
        UUID right = uuid(dataManager.getData().getString(path + ".right.uuid"));
        Player leftPlayer = left == null ? null : Bukkit.getPlayer(left);
        Player rightPlayer = right == null ? null : Bukkit.getPlayer(right);
        if (leftPlayer == null || rightPlayer == null) return RollbackResult.PLAYERS_OFFLINE;

        double leftCoins = dataManager.getData().getDouble(path + ".left.coins", 0.0D);
        double rightCoins = dataManager.getData().getDouble(path + ".right.coins", 0.0D);
        if (leftCoins > 0 && economyManager.getCoins(right) < leftCoins) return RollbackResult.INSUFFICIENT_COINS;
        if (rightCoins > 0 && economyManager.getCoins(left) < rightCoins) return RollbackResult.INSUFFICIENT_COINS;

        List<ItemStack> leftItems = itemList(path + ".left.items");
        List<ItemStack> rightItems = itemList(path + ".right.items");
        if (removeItems(rightPlayer, leftItems) || removeItems(leftPlayer, rightItems)) return RollbackResult.MISSING_ITEMS;

        if (leftCoins > 0) {
            economyManager.takeCoins(right, leftCoins, "rollback trade " + tradeId);
            economyManager.giveCoins(left, leftCoins, "rollback trade " + tradeId);
        }
        if (rightCoins > 0) {
            economyManager.takeCoins(left, rightCoins, "rollback trade " + tradeId);
            economyManager.giveCoins(right, rightCoins, "rollback trade " + tradeId);
        }
        giveItems(leftPlayer, leftItems);
        giveItems(rightPlayer, rightItems);
        dataManager.getData().set(path + ".rolled-back", true);
        dataManager.getData().set(path + ".rolled-back-at", System.currentTimeMillis());
        dataManager.save();
        return RollbackResult.SUCCESS;
    }

    private List<String> sectionHistory(String basePath, int limit, RowFormatter formatter) {
        org.bukkit.configuration.ConfigurationSection section = dataManager.getData().getConfigurationSection(basePath);
        if (section == null) return List.of();
        List<HistoryEntry> entries = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            String path = basePath + "." + id;
            entries.add(new HistoryEntry(id, path, dataManager.getData().getLong(path + ".time", 0L)));
        }
        entries.sort(Comparator.comparingLong(HistoryEntry::time).reversed());
        List<String> rows = new ArrayList<>();
        for (HistoryEntry entry : entries.subList(0, Math.min(limit, entries.size()))) rows.add(formatter.format(entry));
        return rows;
    }

    private boolean removeItems(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            if (!player.getInventory().containsAtLeast(item, item.getAmount())) return true;
        }
        for (ItemStack item : items) player.getInventory().removeItem(item.clone());
        return false;
    }

    private void giveItems(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            player.getInventory().addItem(item.clone()).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }

    private List<ItemStack> itemList(String path) {
        List<?> raw = dataManager.getData().getList(path, List.of());
        List<ItemStack> items = new ArrayList<>();
        for (Object value : raw) if (value instanceof ItemStack item) items.add(item.clone());
        return items;
    }

    private UUID uuid(String text) {
        try {
            return UUID.fromString(String.valueOf(text));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String summarize(List<ItemStack> items, double coins) {
        List<String> parts = new ArrayList<>();
        if (coins > 0) parts.add(EconomyManager.formatCompact(coins) + " coins");
        for (ItemStack item : items) parts.add(itemSummary(item));
        return parts.isEmpty() ? "nothing" : String.join(", ", parts);
    }

    public String itemSummary(ItemStack item) {
        if (item == null || item.getType().isAir()) return "nothing";
        String name = pretty(item.getType().name());
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String display = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (!display.isBlank()) name = display + " (" + name + ")";
        }
        return item.getAmount() + "x " + name;
    }

    public String playerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() == null ? uuid.toString() : player.getName();
    }

    private String nextId() {
        return Long.toString(System.currentTimeMillis(), 36).toLowerCase(Locale.ROOT)
                + "-"
                + Long.toString(System.nanoTime() & 0xfffffL, 36).toLowerCase(Locale.ROOT);
    }

    private String time(long millis) {
        return TIME_FORMAT.format(Instant.ofEpochMilli(Math.max(0L, millis)));
    }

    private String pretty(String value) {
        String text = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    public enum RollbackResult {
        SUCCESS,
        NOT_FOUND,
        ALREADY_ROLLED_BACK,
        PLAYERS_OFFLINE,
        INSUFFICIENT_COINS,
        MISSING_ITEMS
    }

    private record HistoryEntry(String id, String path, long time) {
    }

    private interface RowFormatter {
        String format(HistoryEntry entry);
    }
}
