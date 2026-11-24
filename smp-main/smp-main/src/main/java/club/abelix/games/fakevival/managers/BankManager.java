package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.config.RuntimeSettings;
import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class BankManager {
    private static final long DAILY_INTEREST_MS = 86_400_000L;

    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final Object balanceLock = new Object();
    private final Map<UUID, BankAccount> accountCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> knownPlayerNames = new ConcurrentHashMap<>();
    private SmpAlertService alertService;

    public BankManager(JavaPlugin plugin, DataManager dataManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.economyManager = economyManager;
    }

    public void setAlertService(SmpAlertService alertService) { this.alertService = alertService; }

    public boolean isEnabled() {
        return dataManager.isMySqlEnabled();
    }

    public BankAccount getCachedAccount(UUID uuid) {
        BankAccount account = accountCache.get(uuid);
        return account == null ? null : withCurrentPurse(account);
    }

    public void getAccountAsync(UUID uuid, Consumer<BankAccount> callback) {
        BankAccount cached = accountCache.get(uuid);
        if (cached != null) {
            runMain(() -> callback.accept(withCurrentPurse(cached)));
            return;
        }
        supplyAsync("load account", () -> getAccountBlocking(uuid), callback);
    }

    public void loadAccountAsync(UUID uuid, Consumer<BankAccount> callback) {
        supplyAsync("load account", () -> getAccountBlocking(uuid), callback);
    }

    public void ensureAccountAsync(UUID uuid, Consumer<Boolean> callback) {
        supplyAsync("ensure account", () -> {
            if (!isEnabled()) return false;
            try {
                synchronized (balanceLock) {
                    ensureAccountBlocking(uuid);
                    return true;
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, callback);
    }

    public void syncPurseAsync(UUID uuid, Consumer<Boolean> callback) {
        supplyAsync("sync purse", () -> {
            if (!isEnabled()) return false;
            try {
                synchronized (balanceLock) {
                    ensureAccountBlocking(uuid);
                    syncPurseBlocking(uuid);
                    accountCache.put(uuid, loadAccountBlocking(uuid));
                    return true;
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, callback);
    }

    public void depositAsync(UUID uuid, double requestedAmount, String source, Consumer<BankResult> callback) {
        supplyAsync("deposit", () -> depositBlocking(uuid, requestedAmount, source), callback);
    }

    public void withdrawAsync(UUID uuid, double requestedAmount, String source, Consumer<BankResult> callback) {
        rememberPlayerName(uuid);
        supplyAsync("withdraw", () -> withdrawBlocking(uuid, requestedAmount, source), callback);
    }

    public void withdrawHalfAsync(UUID uuid, Consumer<BankResult> callback) {
        rememberPlayerName(uuid);
        supplyAsync("withdraw half", () -> {
            BankAccount account = getAccountBlocking(uuid);
            return withdrawBlocking(uuid, account.bankBalance() / 2D, "withdraw_half");
        }, callback);
    }

    public void withdrawAmountAsync(UUID uuid, Consumer<BankResult> callback) {
        rememberPlayerName(uuid);
        supplyAsync("withdraw amount", () -> {
            BankAccount account = getAccountBlocking(uuid);
            return withdrawBlocking(uuid, Math.min(10_000D, account.bankBalance()), "withdraw_amount");
        }, callback);
    }

    public void upgradeBankAsync(UUID uuid, Consumer<BankResult> callback) {
        supplyAsync("upgrade", () -> upgradeBlocking(uuid), callback);
    }

    public void claimInterestAsync(UUID uuid, Consumer<BankResult> callback) {
        supplyAsync("claim interest", () -> claimInterestBlocking(uuid), callback);
    }

    public void recentTransactionsAsync(UUID uuid, int limit, Consumer<List<BankTransaction>> callback) {
        supplyAsync("load transactions", () -> recentTransactionsBlocking(uuid, limit), callback);
    }

    private BankAccount getAccountBlocking(UUID uuid) {
        warnIfMainThread("getAccountBlocking");
        if (!isEnabled()) {
            return new BankAccount(uuid, economyManager.getCoins(uuid), 0D, BankTier.STARTER, 0L);
        }
        synchronized (balanceLock) {
            try {
                ensureAccountBlocking(uuid);
                syncPurseBlocking(uuid);
                BankAccount account = loadAccountBlocking(uuid);
                accountCache.put(uuid, account);
                return account;
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not load bank account for " + uuid + ": " + exception.getMessage());
                return new BankAccount(uuid, economyManager.getCoins(uuid), 0D, BankTier.STARTER, 0L);
            }
        }
    }

    private BankResult depositBlocking(UUID uuid, double requestedAmount, String source) {
        warnIfMainThread("depositBlocking");
        if (!isEnabled()) return BankResult.fail("&cBanking requires MariaDB persistence.");
        synchronized (balanceLock) {
            try {
                ensureAccountBlocking(uuid);
                BankAccount account = loadAccountBlocking(uuid);
                double purse = economyManager.getCoins(uuid);
                double room = Math.max(0D, account.tier().capacity() - account.bankBalance());
                double amount = Math.clamp(requestedAmount, 0D, Math.min(purse, room));
                if (amount <= 0D) {
                    return room <= 0D
                            ? BankResult.fail("&cYour bank is at this tier's capacity.")
                            : BankResult.fail("&cYou do not have coins to deposit.");
                }
                if (!economyManager.takeCoins(uuid, amount, "bank-deposit")) {
                    return BankResult.fail("&cYou do not have enough purse coins.");
                }
                double before = account.bankBalance();
                double after = before + amount;
                try {
                    saveAccountBlocking(uuid, after, account.tier(), account.lastInterestClaim());
                    syncPurseBlocking(uuid);
                    logTransaction(uuid, "deposit", amount, before, after, source);
                    accountCache.put(uuid, loadAccountBlocking(uuid));
                } catch (SQLException exception) {
                    economyManager.giveCoins(uuid, amount, "bank-deposit-refund");
                    throw exception;
                }
                return BankResult.ok("&aDeposited &e" + economyManager.format(amount) + " coins&a.", amount);
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not deposit bank coins for " + uuid + ": " + exception.getMessage());
                return BankResult.fail("&cFailed to deposit coins. Check console.");
            }
        }
    }

    private BankResult withdrawBlocking(UUID uuid, double requestedAmount, String source) {
        warnIfMainThread("withdrawBlocking");
        if (!isEnabled()) return BankResult.fail("&cBanking requires MariaDB persistence.");
        synchronized (balanceLock) {
            try {
                ensureAccountBlocking(uuid);
                BankAccount account = loadAccountBlocking(uuid);
                double amount = Math.clamp(requestedAmount, 0D, account.bankBalance());
                if (amount <= 0D) return BankResult.fail("&cYou do not have bank coins to withdraw.");
                double before = account.bankBalance();
                double after = before - amount;
                saveAccountBlocking(uuid, after, account.tier(), account.lastInterestClaim());
                economyManager.giveCoins(uuid, amount, "bank-withdraw");
                syncPurseBlocking(uuid);
                logTransaction(uuid, "withdraw", amount, before, after, source);
                accountCache.put(uuid, loadAccountBlocking(uuid));
                if (alertService != null && amount >= alertService.threshold(SmpAlertService.Type.BANK_WITHDRAW)) {
                    String playerName = knownPlayerNames.getOrDefault(uuid, uuid.toString());
                    alertService.alert(SmpAlertService.Type.BANK_WITHDRAW, uuid,
                            "&c[SMP ALERT] &e" + playerName + " &7withdrew &6" + economyManager.format(amount) + " coins &7from the bank.",
                            "**" + playerName + "** withdrew **" + economyManager.format(amount) + " coins** from the bank. Server: `" + alertService.serverId() + "`",
                            "player=" + playerName + " amount=" + amount + " before=" + before + " after=" + after + " server=" + alertService.serverId() + " at=" + System.currentTimeMillis());
                }
                return BankResult.ok("&aWithdrew &e" + economyManager.format(amount) + " coins&a.", amount);
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not withdraw bank coins for " + uuid + ": " + exception.getMessage());
                return BankResult.fail("&cFailed to withdraw coins. Check console.");
            }
        }
    }

    private void rememberPlayerName(UUID uuid) {
        if (!Bukkit.isPrimaryThread()) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) knownPlayerNames.put(uuid, player.getName());
    }

    private BankResult upgradeBlocking(UUID uuid) {
        warnIfMainThread("upgradeBlocking");
        if (!isEnabled()) return BankResult.fail("&cBanking requires MariaDB persistence.");
        synchronized (balanceLock) {
            try {
                ensureAccountBlocking(uuid);
                BankAccount account = loadAccountBlocking(uuid);
                BankTier next = account.tier().next();
                if (next == null) return BankResult.fail("&cYour bank tier is already maxed.");
                if (!economyManager.takeCoins(uuid, next.upgradeCost(), "bank-upgrade-" + next.id())) {
                    return BankResult.fail("&cYou need &e" + economyManager.format(next.upgradeCost()) + " coins &cto upgrade.");
                }
                try {
                    saveAccountBlocking(uuid, account.bankBalance(), next, account.lastInterestClaim());
                    syncPurseBlocking(uuid);
                    logTransaction(uuid, "upgrade", next.upgradeCost(), account.bankBalance(), account.bankBalance(), "tier:" + next.id());
                    accountCache.put(uuid, loadAccountBlocking(uuid));
                } catch (SQLException exception) {
                    economyManager.giveCoins(uuid, next.upgradeCost(), "bank-upgrade-refund");
                    throw exception;
                }
                return BankResult.ok("&aUpgraded your bank to &f" + next.displayName() + "&a.", next.upgradeCost());
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not upgrade bank for " + uuid + ": " + exception.getMessage());
                return BankResult.fail("&cFailed to upgrade bank. Check console.");
            }
        }
    }

    private BankResult claimInterestBlocking(UUID uuid) {
        warnIfMainThread("claimInterestBlocking");
        if (!isEnabled()) return BankResult.fail("&cBanking requires MariaDB persistence.");
        synchronized (balanceLock) {
            try {
                ensureAccountBlocking(uuid);
                BankAccount account = loadAccountBlocking(uuid);
                long now = System.currentTimeMillis();
                long elapsed = now - account.lastInterestClaim();
                if (account.lastInterestClaim() > 0L && elapsed < DAILY_INTEREST_MS) {
                    long remaining = DAILY_INTEREST_MS - elapsed;
                    long hours = Math.max(1L, remaining / 3_600_000L);
                    return BankResult.fail("&cInterest is not ready yet. Try again in about &e" + hours + "h&c.");
                }
                if (account.bankBalance() <= 0D) return BankResult.fail("&cYou need coins in the bank to claim interest.");
                double rate = Math.max(0D, RuntimeSettings.config(plugin).getDouble("bank.interest.daily-percent", 1.0D)) / 100D;
                double room = Math.max(0D, account.tier().capacity() - account.bankBalance());
                double interest = Math.min(account.bankBalance() * rate, Math.min(account.tier().dailyInterestCap(), room));
                if (interest <= 0D) return BankResult.fail("&cYour bank is full. Upgrade it before claiming interest.");
                double before = account.bankBalance();
                double after = before + interest;
                saveAccountBlocking(uuid, after, account.tier(), now);
                syncPurseBlocking(uuid);
                logTransaction(uuid, "interest", interest, before, after, "daily_interest");
                accountCache.put(uuid, loadAccountBlocking(uuid));
                return BankResult.ok("&aClaimed &e" + economyManager.format(interest) + " coins &ain bank interest.", interest);
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not claim bank interest for " + uuid + ": " + exception.getMessage());
                return BankResult.fail("&cFailed to claim interest. Check console.");
            }
        }
    }

    public double applyDeathLoss(Player player) {
        if (!RuntimeSettings.config(plugin).getBoolean("bank.death-loss.enabled", true)) return 0D;
        double percent = Math.max(0D, RuntimeSettings.config(plugin).getDouble("bank.death-loss.percent", 10D));
        if (percent <= 0D) return 0D;
        UUID uuid = player.getUniqueId();
        double before = economyManager.getCoins(uuid);
        double amount = Math.min(before, before * (percent / 100D));
        if (amount <= 0D || !economyManager.takeCoins(uuid, amount, "bank-death-loss")) return 0D;
        if (isEnabled()) {
            UUID playerUuid = uuid;
            double balanceBefore = before;
            double balanceAfter = before - amount;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                synchronized (balanceLock) {
                    warnIfMainThread("deathLossLog");
                    try {
                        ensureAccountBlocking(playerUuid);
                        syncPurseBlocking(playerUuid);
                        logTransaction(playerUuid, "death_loss", amount, balanceBefore, balanceAfter, "player_death");
                        accountCache.put(playerUuid, loadAccountBlocking(playerUuid));
                    } catch (SQLException exception) {
                        plugin.getLogger().warning("Could not log bank death loss for " + playerUuid + ": " + exception.getMessage());
                    }
                }
            });
        }
        return amount;
    }

    private List<BankTransaction> recentTransactionsBlocking(UUID uuid, int limit) {
        warnIfMainThread("recentTransactionsBlocking");
        List<BankTransaction> transactions = new ArrayList<>();
        if (!isEnabled()) return transactions;
        synchronized (balanceLock) {
            try {
                ensureAccountBlocking(uuid);
                try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                        "SELECT `type`, `amount`, `balance_before`, `balance_after`, `source`, `created_at` FROM `smp_bank_transactions` WHERE `uuid`=? ORDER BY `id` DESC LIMIT ?")) {
                    statement.setString(1, uuid.toString());
                    statement.setInt(2, Math.max(1, limit));
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            transactions.add(new BankTransaction(
                                    uuid,
                                    result.getString("type"),
                                    result.getDouble("amount"),
                                    result.getDouble("balance_before"),
                                    result.getDouble("balance_after"),
                                    result.getString("source"),
                                    result.getLong("created_at")
                            ));
                        }
                    }
                }
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not read bank transactions for " + uuid + ": " + exception.getMessage());
            }
        }
        return transactions;
    }

    private <T> void supplyAsync(String operation, Supplier<T> supplier, Consumer<T> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            T result = null;
            try {
                result = supplier.get();
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "BankManager failed to " + operation + ".", exception);
            }
            T finalResult = result;
            runMain(() -> callback.accept(finalResult));
        });
    }

    private void runMain(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, runnable);
        }
    }

    private BankAccount withCurrentPurse(BankAccount account) {
        return new BankAccount(account.uuid(), economyManager.getCoins(account.uuid()), account.bankBalance(), account.tier(), account.lastInterestClaim());
    }

    private void warnIfMainThread(String operation) {
        if (Bukkit.isPrimaryThread()) {
            plugin.getLogger().warning("BankManager DB operation ran on main thread: " + operation);
        }
    }

    private void ensureAccountBlocking(UUID uuid) throws SQLException {
        warnIfMainThread("ensureAccountBlocking");
        long now = System.currentTimeMillis();
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT IGNORE INTO `smp_player_bank` (`uuid`, `purse`, `bank_balance`, `bank_tier`, `last_interest_claim`, `created_at`, `updated_at`) VALUES (?, ?, 0, ?, 0, ?, ?)")) {
            statement.setString(1, uuid.toString());
            statement.setDouble(2, economyManager.getCoins(uuid));
            statement.setString(3, BankTier.STARTER.id());
            statement.setLong(4, now);
            statement.setLong(5, now);
            statement.executeUpdate();
        }
    }

    private BankAccount loadAccountBlocking(UUID uuid) throws SQLException {
        warnIfMainThread("loadAccountBlocking");
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT `purse`, `bank_balance`, `bank_tier`, `last_interest_claim` FROM `smp_player_bank` WHERE `uuid`=?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new BankAccount(
                            uuid,
                            result.getDouble("purse"),
                            Math.max(0D, result.getDouble("bank_balance")),
                            BankTier.parse(result.getString("bank_tier")),
                            Math.max(0L, result.getLong("last_interest_claim"))
                    );
                }
            }
        }
        return new BankAccount(uuid, economyManager.getCoins(uuid), 0D, BankTier.STARTER, 0L);
    }

    private void saveAccountBlocking(UUID uuid, double bankBalance, BankTier tier, long lastInterestClaim) throws SQLException {
        warnIfMainThread("saveAccountBlocking");
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE `smp_player_bank` SET `purse`=?, `bank_balance`=?, `bank_tier`=?, `last_interest_claim`=?, `updated_at`=? WHERE `uuid`=?")) {
            statement.setDouble(1, economyManager.getCoins(uuid));
            statement.setDouble(2, Math.clamp(bankBalance, 0D, tier.capacity()));
            statement.setString(3, tier.id());
            statement.setLong(4, Math.max(0L, lastInterestClaim));
            statement.setLong(5, System.currentTimeMillis());
            statement.setString(6, uuid.toString());
            statement.executeUpdate();
        }
    }

    private void syncPurseBlocking(UUID uuid) throws SQLException {
        warnIfMainThread("syncPurseBlocking");
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE `smp_player_bank` SET `purse`=?, `updated_at`=? WHERE `uuid`=?")) {
            statement.setDouble(1, economyManager.getCoins(uuid));
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, uuid.toString());
            statement.executeUpdate();
        }
    }

    private void logTransaction(UUID uuid, String type, double amount, double before, double after, String source) throws SQLException {
        warnIfMainThread("logTransaction");
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO `smp_bank_transactions` (`uuid`, `type`, `amount`, `balance_before`, `balance_after`, `source`, `created_at`) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, type);
            statement.setDouble(3, Math.max(0D, amount));
            statement.setDouble(4, Math.max(0D, before));
            statement.setDouble(5, Math.max(0D, after));
            statement.setString(6, source == null ? "" : source);
            statement.setLong(7, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    public record BankAccount(UUID uuid, double purse, double bankBalance, BankTier tier, long lastInterestClaim) {
    }

    public record BankResult(boolean success, String message, double amount) {
        public static BankResult ok(String message, double amount) {
            return new BankResult(true, message, amount);
        }

        public static BankResult fail(String message) {
            return new BankResult(false, message, 0D);
        }
    }
}
