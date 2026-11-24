package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.config.RuntimeSettings;
import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.UUID;

/** Persistent, network-wide purchase counters for the existing Gold Store. */
public final class GoldStoreLimitManager {
    public enum Window { DAILY, WEEKLY, MONTHLY, LIFETIME, SEASON }
    public record Usage(int daily, int weekly, int season, int lifetime, double dailyCoins, double weeklyCoins) {}
    public record LimitResult(boolean allowed, String message, Usage usage) {
        public static LimitResult allowed(Usage usage) { return new LimitResult(true, "", usage); }
        public static LimitResult denied(String message, Usage usage) { return new LimitResult(false, message, usage); }
    }
    public record ReservationResult(boolean reserved, int used, int limit, String message) {}

    private final JavaPlugin plugin;
    private final DataManager data;
    private final ZoneId zone = ZoneId.systemDefault();

    public GoldStoreLimitManager(JavaPlugin plugin, DataManager data) {
        this.plugin = plugin;
        this.data = data;
    }

    public synchronized LimitResult canPurchase(UUID uuid, String productId, String type, int quantity, double coins) {
        quantity = Math.max(1, quantity);
        Usage usage = usage(uuid, productId, type);
        FileConfiguration c = RuntimeSettings.config(plugin);
        if (type.equals("coin-exchange") && lifetimeLimit(productId) > 0) {
            int limit = lifetimeLimit(productId);
            if (usage.lifetime() + quantity > limit) {
                return LimitResult.denied("&cYou have reached the purchase limit for this Coin Purse.\n&7Limit: &e"
                        + usage.lifetime() + "/" + limit, usage);
            }
        } else if (type.equals("coin-exchange")) {
            int daily = c.getInt("goldstore.coinExchange.dailyLimit", 3);
            int weekly = c.getInt("goldstore.coinExchange.weeklyLimit", 15);
            double dailyCoins = c.getDouble("goldstore.coinExchange.maxCoinsPerDay", 250000D);
            double weeklyCoins = c.getDouble("goldstore.coinExchange.maxCoinsPerWeek", 1000000D);
            if (usage.daily() + 1 > daily || usage.weekly() + 1 > weekly
                    || usage.dailyCoins() + coins > dailyCoins || usage.weeklyCoins() + coins > weeklyCoins) {
                return LimitResult.denied("&cYou reached your Gold to Coins exchange limit.\n&7Daily: &e" + usage.daily() + "/" + daily
                        + "\n&7Weekly: &e" + usage.weekly() + "/" + weekly, usage);
            }
        } else if (type.equals("lootbox")) {
            int daily = lootboxLimit(productId, true), weekly = lootboxLimit(productId, false);
            if (usage.daily() + quantity > daily || usage.weekly() + quantity > weekly)
                return LimitResult.denied("&cYou reached the purchase limit for this lootbox.\n&7Daily: &e" + usage.daily() + "/" + daily
                        + "\n&7Weekly: &e" + usage.weekly() + "/" + weekly + "\n&7Resets in: &e" + timeUntil(Window.DAILY), usage);
        } else if (type.equals("battlepass-levels")) {
            int daily = c.getInt("goldstore.battlepassLevels.dailyLimit", 10);
            int weekly = c.getInt("goldstore.battlepassLevels.weeklyLimit", 30);
            int season = c.getInt("goldstore.battlepassLevels.seasonLimit", 100);
            if (usage.daily() + quantity > daily || usage.weekly() + quantity > weekly || usage.season() + quantity > season)
                return LimitResult.denied("&cYou cannot buy that many Battlepass levels.\n&7Daily: &e" + usage.daily() + "/" + daily
                        + "\n&7Weekly: &e" + usage.weekly() + "/" + weekly + "\n&7Season: &e" + usage.season() + "/" + season, usage);
        } else if (type.equals("booster")) {
            int daily = c.getInt("goldstore.boosters.dailyLimit", 3);
            int weekly = c.getInt("goldstore.boosters.weeklyLimit", 10);
            if (usage.daily() + quantity > daily || usage.weekly() + quantity > weekly)
                return LimitResult.denied("&cYou cannot buy more of this booster right now.\n&7Daily: &e" + usage.daily() + "/" + daily
                        + "\n&7Weekly: &e" + usage.weekly() + "/" + weekly, usage);
        }
        return LimitResult.allowed(usage);
    }

    public synchronized void record(UUID uuid, String productId, String type, int quantity, double coins) {
        quantity = Math.max(1, quantity);
        if (type.equals("coin-exchange") && lifetimeLimit(productId) > 0) {
            increment(uuid, normalize(productId), Window.LIFETIME, quantity);
            return;
        }
        if (type.equals("coin-exchange")) {
            incrementCoins(uuid, Window.DAILY, coins);
            incrementCoins(uuid, Window.WEEKLY, coins);
            return;
        }
        String key = type.equals("battlepass-levels") ? "battlepass_levels" : normalize(productId);
        increment(uuid, key, Window.DAILY, quantity);
        increment(uuid, key, Window.WEEKLY, quantity);
        if (type.equals("battlepass-levels")) increment(uuid, key, Window.SEASON, quantity);
    }

    public Usage usage(UUID uuid, String productId, String type) {
        if (type.equals("coin-exchange") && lifetimeLimit(productId) > 0) {
            return new Usage(0, 0, 0, read(uuid, normalize(productId), Window.LIFETIME), 0, 0);
        }
        if (type.equals("coin-exchange")) {
            CoinUsage d = coinUsage(uuid, Window.DAILY), w = coinUsage(uuid, Window.WEEKLY);
            return new Usage(d.count, w.count, 0, 0, d.coins, w.coins);
        }
        String key = type.equals("battlepass-levels") ? "battlepass_levels" : normalize(productId);
        return new Usage(read(uuid, key, Window.DAILY), read(uuid, key, Window.WEEKLY),
                type.equals("battlepass-levels") ? read(uuid, key, Window.SEASON) : 0, 0, 0, 0);
    }

    /** Returns the account-wide purchase cap for fixed coin purses, or zero for ordinary exchanges. */
    public int lifetimeLimit(String productId) {
        String id = normalize(productId);
        int fallback = switch (id) {
            case "small_coin_purse" -> 1;
            case "medium_coin_purse" -> 3;
            case "large_coin_purse" -> 6;
            default -> 0;
        };
        if (fallback == 0) return 0;
        return Math.max(0, RuntimeSettings.config(plugin).getInt("store.coins." + id + ".lifetime-limit", fallback));
    }

    /**
     * Atomically reserves one lifetime purchase in MariaDB. The guarded UPDATE is the final authority,
     * so two backend servers cannot both pass a stale read at the product limit.
     */
    public ReservationResult reserveLifetimePurchase(UUID uuid, String productId) {
        String id = normalize(productId);
        int limit = lifetimeLimit(id);
        if (limit <= 0) return new ReservationResult(false, 0, 0, "&cThis Coin Purse has no valid lifetime limit.");
        if (!data.isMySqlEnabled()) {
            plugin.getLogger().warning("[SMP GoldStore] Denied lifetime Coin Purse purchase because MariaDB is unavailable: " + id);
            return new ReservationResult(false, read(uuid, id, Window.LIFETIME), limit,
                    "&cCoin Purse purchases are temporarily unavailable. Please try again shortly.");
        }

        long now = System.currentTimeMillis();
        try (Connection connection = data.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO smp_goldstore_purchase_limits(player_uuid,product_id,limit_window,window_start,purchases,last_purchase_at) " +
                                "VALUES(?,?,'LIFETIME',0,0,?) ON DUPLICATE KEY UPDATE purchases=purchases")) {
                    insert.setString(1, uuid.toString());
                    insert.setString(2, id);
                    insert.setLong(3, now);
                    insert.executeUpdate();
                }

                int changed;
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE smp_goldstore_purchase_limits SET purchases=purchases+1,last_purchase_at=? " +
                                "WHERE player_uuid=? AND product_id=? AND limit_window='LIFETIME' AND window_start=0 AND purchases<?")) {
                    update.setLong(1, now);
                    update.setString(2, uuid.toString());
                    update.setString(3, id);
                    update.setInt(4, limit);
                    changed = update.executeUpdate();
                }

                int used = readLifetime(connection, uuid, id);
                connection.commit();
                if (changed == 1) return new ReservationResult(true, used, limit, "");
                return new ReservationResult(false, used, limit,
                        "&cYou have reached the purchase limit for this Coin Purse.\n&7Limit: &e" + used + "/" + limit + " lifetime");
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            warn(exception);
            return new ReservationResult(false, read(uuid, id, Window.LIFETIME), limit,
                    "&cCoin Purse purchases are temporarily unavailable. Please try again shortly.");
        }
    }

    /** Releases a reservation when Gold could not be charged. It never drops the counter below zero. */
    public void releaseLifetimeReservation(UUID uuid, String productId) {
        String id = normalize(productId);
        if (lifetimeLimit(id) <= 0 || !data.isMySqlEnabled()) return;
        try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE smp_goldstore_purchase_limits SET purchases=purchases-1,last_purchase_at=? " +
                        "WHERE player_uuid=? AND product_id=? AND limit_window='LIFETIME' AND window_start=0 AND purchases>0")) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setString(2, uuid.toString());
            statement.setString(3, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().severe("[SMP GoldStore] Could not release failed Coin Purse reservation player=" + uuid
                    + " product=" + id + ": " + exception.getMessage());
        }
    }

    private int readLifetime(Connection connection, UUID uuid, String productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT purchases FROM smp_goldstore_purchase_limits WHERE player_uuid=? AND product_id=? " +
                        "AND limit_window='LIFETIME' AND window_start=0")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, productId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    public synchronized int reset(UUID uuid, String productId) {
        if (!data.isMySqlEnabled()) return 0;
        int changed = 0;
        try (Connection c = data.getConnection()) {
            String sql = productId.equalsIgnoreCase("all")
                    ? "DELETE FROM smp_goldstore_purchase_limits WHERE player_uuid=?"
                    : "DELETE FROM smp_goldstore_purchase_limits WHERE player_uuid=? AND product_id=?";
            try (PreparedStatement s = c.prepareStatement(sql)) {
                s.setString(1, uuid.toString());
                if (!productId.equalsIgnoreCase("all")) s.setString(2, normalize(productId));
                changed += s.executeUpdate();
            }
            if (productId.equalsIgnoreCase("coin_exchange") || productId.equalsIgnoreCase("all")) {
                try (PreparedStatement s = c.prepareStatement("DELETE FROM smp_goldstore_coin_exchange_usage WHERE player_uuid=?")) {
                    s.setString(1, uuid.toString()); changed += s.executeUpdate();
                }
            }
        } catch (SQLException e) { plugin.getLogger().warning("[SMP GoldStore] Could not reset limits: " + e.getMessage()); }
        return changed;
    }

    public String timeUntil(Window window) {
        long now = System.currentTimeMillis();
        long next = switch (window) {
            case DAILY -> start(Window.DAILY, now) + 86_400_000L;
            case WEEKLY -> start(Window.WEEKLY, now) + 604_800_000L;
            default -> now;
        };
        long seconds = Math.max(0, (next - now) / 1000);
        long hours = seconds / 3600, minutes = (seconds % 3600) / 60;
        return hours > 0 ? hours + "h " + minutes + "m" : Math.max(1, minutes) + "m";
    }

    private int lootboxLimit(String id, boolean daily) {
        FileConfiguration c = RuntimeSettings.config(plugin);
        String suffix = daily ? "dailyLimit" : "weeklyLimit";
        String path = "goldstore.lootboxes.products." + normalize(id) + "." + suffix;
        if (c.contains(path)) return Math.max(0, c.getInt(path));
        String n = normalize(id);
        if (n.contains("legendary")) return daily ? 1 : 3;
        if (n.contains("rare")) return daily ? 3 : 10;
        return c.getInt("goldstore.lootboxes." + suffix, daily ? 5 : 20);
    }

    private int read(UUID uuid, String product, Window window) {
        if (!data.isMySqlEnabled()) return fallbackInt(uuid, product, window);
        try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                "SELECT purchases FROM smp_goldstore_purchase_limits WHERE player_uuid=? AND product_id=? AND limit_window=? AND window_start=?")) {
            s.setString(1, uuid.toString()); s.setString(2, product); s.setString(3, window.name()); s.setLong(4, start(window, System.currentTimeMillis()));
            try (ResultSet r = s.executeQuery()) { return r.next() ? r.getInt(1) : 0; }
        } catch (SQLException e) { warn(e); return fallbackInt(uuid, product, window); }
    }

    private void increment(UUID uuid, String product, Window window, int amount) {
        long start = start(window, System.currentTimeMillis());
        if (!data.isMySqlEnabled()) { setFallbackInt(uuid, product, window, fallbackInt(uuid, product, window) + amount); return; }
        try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO smp_goldstore_purchase_limits(player_uuid,product_id,limit_window,window_start,purchases,last_purchase_at) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE purchases=purchases+VALUES(purchases),last_purchase_at=VALUES(last_purchase_at)")) {
            s.setString(1, uuid.toString()); s.setString(2, product); s.setString(3, window.name()); s.setLong(4, start); s.setInt(5, amount); s.setLong(6, System.currentTimeMillis()); s.executeUpdate();
        } catch (SQLException e) {
            warn(e);
            setFallbackInt(uuid, product, window, fallbackInt(uuid, product, window) + amount);
        }
    }

    private CoinUsage coinUsage(UUID uuid, Window window) {
        if (!data.isMySqlEnabled()) return new CoinUsage(fallbackInt(uuid, "coin_exchange", window), fallbackDouble(uuid, window));
        try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                "SELECT exchange_count,coins_received FROM smp_goldstore_coin_exchange_usage WHERE player_uuid=? AND window_type=? AND window_start=?")) {
            s.setString(1, uuid.toString()); s.setString(2, window.name()); s.setLong(3, start(window, System.currentTimeMillis()));
            try (ResultSet r = s.executeQuery()) { return r.next() ? new CoinUsage(r.getInt(1), r.getDouble(2)) : new CoinUsage(0, 0); }
        } catch (SQLException e) { warn(e); return new CoinUsage(fallbackInt(uuid, "coin_exchange", window), fallbackDouble(uuid, window)); }
    }

    private void incrementCoins(UUID uuid, Window window, double coins) {
        long start = start(window, System.currentTimeMillis());
        if (!data.isMySqlEnabled()) {
            setFallbackInt(uuid, "coin_exchange", window, fallbackInt(uuid, "coin_exchange", window) + 1);
            data.getData().set(fallbackPath(uuid, "coin_exchange", window) + ".coins", fallbackDouble(uuid, window) + coins); data.save(); return;
        }
        try (Connection c = data.getConnection(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO smp_goldstore_coin_exchange_usage(player_uuid,window_type,window_start,exchange_count,coins_received,last_exchange_at) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE exchange_count=exchange_count+1,coins_received=coins_received+VALUES(coins_received),last_exchange_at=VALUES(last_exchange_at)")) {
            s.setString(1, uuid.toString()); s.setString(2, window.name()); s.setLong(3, start); s.setInt(4, 1); s.setDouble(5, coins); s.setLong(6, System.currentTimeMillis()); s.executeUpdate();
        } catch (SQLException e) {
            warn(e);
            setFallbackInt(uuid, "coin_exchange", window, fallbackInt(uuid, "coin_exchange", window) + 1);
            data.getData().set(fallbackPath(uuid, "coin_exchange", window) + ".coins", fallbackDouble(uuid, window) + coins);
            data.save();
        }
    }

    private long start(Window window, long millis) {
        if (window == Window.LIFETIME) return 0;
        if (window == Window.SEASON) return RuntimeSettings.config(plugin).getLong("goldstore.battlepassLevels.seasonId", 1L);
        LocalDate date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate();
        if (window == Window.WEEKLY) date = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (window == Window.MONTHLY) date = date.withDayOfMonth(1);
        return date.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    private String fallbackPath(UUID uuid, String product, Window window) { return "goldstore-limit-fallback." + uuid + "." + product + "." + window.name() + "." + start(window, System.currentTimeMillis()); }
    private int fallbackInt(UUID uuid, String product, Window window) { return data.getData().getInt(fallbackPath(uuid, product, window) + ".purchases", 0); }
    private void setFallbackInt(UUID uuid, String product, Window window, int value) { data.getData().set(fallbackPath(uuid, product, window) + ".purchases", value); data.save(); }
    private double fallbackDouble(UUID uuid, Window window) { return data.getData().getDouble(fallbackPath(uuid, "coin_exchange", window) + ".coins", 0); }
    private String normalize(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_'); }
    private void warn(SQLException e) { plugin.getLogger().warning("[SMP GoldStore] Limit database operation failed; safe runtime fallback used: " + e.getMessage()); }
    private record CoinUsage(int count, double coins) {}
}
