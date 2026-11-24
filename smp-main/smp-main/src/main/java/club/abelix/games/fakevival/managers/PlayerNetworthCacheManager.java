package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.mmo.MMOItemValueService;
import club.abelix.games.fakevival.registries.DatabaseRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerNetworthCacheManager implements Listener {
    private static final long INITIAL_DELAY_TICKS = 200L;
    private static final long REFRESH_PERIOD_TICKS = 1_200L;

    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final AuctionHouseManager auctionHouseManager;
    private final StorageManager storageManager;
    private final MMOItemValueService itemValueService;
    private final Set<CompletableFuture<Void>> inFlight = ConcurrentHashMap.newKeySet();
    private BukkitTask refreshTask;

    public PlayerNetworthCacheManager(JavaPlugin plugin, DataManager dataManager, EconomyManager economyManager,
                                      AuctionHouseManager auctionHouseManager, StorageManager storageManager,
                                      MMOItemValueService itemValueService) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.economyManager = economyManager;
        this.auctionHouseManager = auctionHouseManager;
        this.storageManager = storageManager;
        this.itemValueService = itemValueService;
    }

    public void start() {
        if (!dataManager.isMySqlEnabled() || refreshTask != null) return;
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> Bukkit.getOnlinePlayers().forEach(this::refresh),
                INITIAL_DELAY_TICKS, REFRESH_PERIOD_TICKS);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) refresh(event.getPlayer());
        }, INITIAL_DELAY_TICKS);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        refresh(event.getPlayer());
    }

    public CompletableFuture<Void> refresh(Player player) {
        if (!dataManager.isMySqlEnabled()) return CompletableFuture.completedFuture(null);

        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        long purseCoins = coins(economyManager.getCoins(uuid));
        long inventoryValue = value(player.getInventory().getContents());
        long auctionValue = auctionHouseManager.getListings(uuid).stream()
                .mapToLong(listing -> coins(auctionHouseManager.getPrice(listing)))
                .reduce(0L, PlayerNetworthCacheManager::add);

        CompletableFuture<Void> future = storageManager.snapshotContents(uuid)
                .exceptionally(error -> {
                    plugin.getLogger().warning("Could not read storage for networth cache " + uuid + ": " + error.getMessage());
                    return List.of();
                })
                .thenAcceptAsync(storage -> persist(uuid, playerName, purseCoins, inventoryValue,
                        value(storage.toArray(ItemStack[]::new)), auctionValue));
        inFlight.add(future);
        future.whenComplete((ignored, error) -> {
            inFlight.remove(future);
            if (error != null) plugin.getLogger().warning("Could not update networth cache for " + uuid + ": " + error.getMessage());
        });
        return future;
    }

    public CompletableFuture<Void> shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        List<CompletableFuture<Void>> pending = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) pending.add(refresh(player));
        pending.addAll(inFlight);
        return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new));
    }

    private void persist(UUID uuid, String playerName, long purseCoins, long inventoryValue,
                         long storageValue, long auctionValue) {
        try (Connection connection = dataManager.getConnection()) {
            long bankCoins = 0L;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT bank_balance FROM smp_player_bank WHERE uuid=?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) bankCoins = coins(result.getDouble("bank_balance"));
                }
            }
            long total = add(add(purseCoins, bankCoins), add(add(inventoryValue, storageValue), auctionValue));
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO `" + DatabaseRegistry.PLAYER_NETWORTH_CACHE_TABLE + "` " +
                            "(player_uuid,player_name,total_networth,purse_coins,bank_coins,inventory_value,storage_value,auction_value,updated_at) " +
                            "VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                            "player_name=VALUES(player_name),total_networth=VALUES(total_networth),purse_coins=VALUES(purse_coins)," +
                            "bank_coins=VALUES(bank_coins),inventory_value=VALUES(inventory_value),storage_value=VALUES(storage_value)," +
                            "auction_value=VALUES(auction_value),updated_at=VALUES(updated_at)")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, playerName);
                statement.setLong(3, total);
                statement.setLong(4, purseCoins);
                statement.setLong(5, bankCoins);
                statement.setLong(6, inventoryValue);
                statement.setLong(7, storageValue);
                statement.setLong(8, auctionValue);
                statement.setLong(9, System.currentTimeMillis());
                statement.executeUpdate();
            }
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    private long value(ItemStack[] items) {
        long total = 0L;
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;
            total = add(total, coins(itemValueService.calculateValue(item) * item.getAmount()));
        }
        return total;
    }

    private static long coins(double value) {
        if (!Double.isFinite(value) || value <= 0D) return 0L;
        if (value >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.round(value);
    }

    private static long add(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }
}
