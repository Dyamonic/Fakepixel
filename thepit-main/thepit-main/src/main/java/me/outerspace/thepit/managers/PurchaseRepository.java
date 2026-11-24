package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Owns the MariaDB side of Contract and Killstreak purchases. Currency and the
 * purchased object are always written on the same connection and transaction.
 */
public final class PurchaseRepository {
    public enum Type { CONTRACT, KILLSTREAK }
    public enum Status {
        SUCCESS, REPOSITORY_UNAVAILABLE, PLAYER_ROW_MISSING, INSUFFICIENT_COINS,
        ALREADY_PURCHASED, PENDING_SELECTION_EXISTS, PERSISTENCE_FAILURE, COMMIT_FAILURE,
        ROLLBACK_FAILURE
    }

    public record Offer(int index, String contractId, String objective, long target, int reward,
                        boolean enhanced, boolean vile) { }

    public record Request(
            UUID transactionId,
            UUID playerUuid,
            String playerName,
            boolean profileLoaded,
            int cachedCoins,
            Type type,
            String purchaseId,
            int originalPrice,
            int discountedPrice,
            String requirementResult,
            String encodedState,
            long createdAt,
            long expiresAt,
            List<Offer> offers
    ) { }

    public record Report(
            UUID transactionId,
            Status status,
            int databaseCoins,
            int updatedCoins,
            int deductionRows,
            int persistenceRows,
            boolean committed,
            boolean rolledBack,
            String failureReason,
            Throwable exception
    ) {
        public boolean success() { return status == Status.SUCCESS; }
    }

    private static final Set<String> REQUIRED_TABLES = Set.of(
            "buildffa_stats",
            "buildffa_contract_pending",
            "buildffa_contract_offers",
            "buildffa_contract_active",
            "buildffa_contract_daily",
            "buildffa_killstreak_purchases",
            "buildffa_killstreak_loadout"
    );

    private final ThePitPlugin plugin;
    private final Map<UUID, UUID> pending = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastFailure = new ConcurrentHashMap<>();
    private volatile boolean contractAvailable;
    private volatile boolean killstreakAvailable;
    private volatile boolean requiredTablesPresent;

    public PurchaseRepository(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        contractAvailable = false;
        killstreakAvailable = false;
        requiredTablesPresent = false;
        if (!plugin.getStorageManager().isMysql()) {
            plugin.getLogger().severe("Purchase repository startup failed: MySQL storage is unavailable. "
                    + plugin.getStorageManager().lastMigrationFailure());
            return false;
        }
        try (Connection connection = plugin.getStorageManager().openTransactionConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS buildffa_contract_pending ("
                    + "uuid VARCHAR(36) NOT NULL, transaction_id VARCHAR(36) NOT NULL, difficulty VARCHAR(16) NOT NULL,"
                    + "original_price INT NOT NULL, paid_price INT NOT NULL, created_at BIGINT NOT NULL,"
                    + "expires_at BIGINT NOT NULL, PRIMARY KEY(uuid), UNIQUE KEY(transaction_id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS buildffa_contract_offers ("
                    + "uuid VARCHAR(36) NOT NULL, offer_index TINYINT NOT NULL, contract_id VARCHAR(64) NOT NULL,"
                    + "objective VARCHAR(64) NOT NULL, target BIGINT NOT NULL, reward INT NOT NULL,"
                    + "enhanced TINYINT(1) NOT NULL DEFAULT 0, vile TINYINT(1) NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY(uuid,offer_index), UNIQUE KEY(uuid,contract_id),"
                    + "CONSTRAINT fk_buildffa_contract_offer_pending FOREIGN KEY(uuid)"
                    + " REFERENCES buildffa_contract_pending(uuid) ON DELETE CASCADE)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS buildffa_contract_active ("
                    + "uuid VARCHAR(36) NOT NULL, contract_id VARCHAR(64) NOT NULL, difficulty VARCHAR(16) NOT NULL,"
                    + "objective VARCHAR(64) NOT NULL, target BIGINT NOT NULL, progress BIGINT NOT NULL DEFAULT 0,"
                    + "reward INT NOT NULL, started_at BIGINT NOT NULL, expires_at BIGINT NOT NULL, PRIMARY KEY(uuid))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS buildffa_contract_daily ("
                    + "uuid VARCHAR(36) NOT NULL, day_key VARCHAR(16) NOT NULL, completions INT NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY(uuid,day_key))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS buildffa_killstreak_purchases ("
                    + "uuid VARCHAR(36) NOT NULL, killstreak_id VARCHAR(64) NOT NULL, purchased_at BIGINT NOT NULL,"
                    + "paid_price INT NOT NULL, transaction_id VARCHAR(36) NOT NULL,"
                    + "PRIMARY KEY(uuid,killstreak_id), UNIQUE KEY(transaction_id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS buildffa_killstreak_loadout ("
                    + "uuid VARCHAR(36) NOT NULL, slot_index TINYINT NOT NULL, killstreak_id VARCHAR(64) NULL,"
                    + "PRIMARY KEY(uuid,slot_index))");
            requiredTablesPresent = verifyTables(connection);
            if (!requiredTablesPresent) throw new SQLException("one or more required purchase tables are missing");
            contractAvailable = true;
            killstreakAvailable = true;
            plugin.getLogger().info("Contract and Killstreak purchase repositories are ready.");
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Purchase repository startup migration failed; Contract and Killstreak purchase menus are unavailable.",
                    exception);
            return false;
        }
    }

    public Report purchase(Request request) {
        if (request == null || request.transactionId() == null || request.playerUuid() == null
                || request.type() == null || request.purchaseId() == null || request.purchaseId().isBlank()
                || request.discountedPrice() < 0) {
            return failure(request, Status.PERSISTENCE_FAILURE, -1, 0, 0,
                    false, false, "invalid purchase request", null);
        }
        pending.put(request.playerUuid(), request.transactionId());
        logAttempt(request, -1);
        if (!available(request.type())) {
            return finish(request, failure(request, Status.REPOSITORY_UNAVAILABLE, -1, 0, 0,
                    false, false, "purchase repository is unavailable", null));
        }

        Connection connection = null;
        int databaseCoins = -1;
        int deductionRows = 0;
        int persistenceRows = 0;
        boolean rollback = false;
        try {
            connection = plugin.getStorageManager().openTransactionConnection();
            connection.setAutoCommit(false);
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT coins FROM buildffa_stats WHERE uuid=? AND period='LIFETIME' AND period_key='all' FOR UPDATE")) {
                select.setString(1, request.playerUuid().toString());
                try (ResultSet result = select.executeQuery()) {
                    if (!result.next()) {
                        rollback = rollback(connection, request, "missing player row");
                        return finish(request, failure(request, Status.PLAYER_ROW_MISSING, -1, 0, 0,
                                false, rollback, "no LIFETIME/all player row", null));
                    }
                    databaseCoins = result.getInt("coins");
                }
            }

            try (PreparedStatement deduction = connection.prepareStatement(
                    "UPDATE buildffa_stats SET coins=coins-? "
                            + "WHERE uuid=? AND period='LIFETIME' AND period_key='all' AND coins>=?")) {
                deduction.setInt(1, request.discountedPrice());
                deduction.setString(2, request.playerUuid().toString());
                deduction.setInt(3, request.discountedPrice());
                deductionRows = deduction.executeUpdate();
            }
            if (deductionRows != 1) {
                rollback = rollback(connection, request, "conditional deduction affected " + deductionRows + " rows");
                return finish(request, failure(request, Status.INSUFFICIENT_COINS, databaseCoins,
                        deductionRows, 0, false, rollback,
                        "database coins are below the discounted price", null));
            }

            if (request.type() == Type.CONTRACT) {
                if (request.offers() == null || request.offers().size() != 3
                        || request.offers().stream().map(Offer::contractId).distinct().count() != 3L) {
                    throw new SQLException("contract transaction did not contain three unique offers");
                }
                try (PreparedStatement pendingInsert = connection.prepareStatement(
                        "INSERT INTO buildffa_contract_pending"
                                + "(uuid,transaction_id,difficulty,original_price,paid_price,created_at,expires_at)"
                                + " VALUES(?,?,?,?,?,?,?)")) {
                    pendingInsert.setString(1, request.playerUuid().toString());
                    pendingInsert.setString(2, request.transactionId().toString());
                    pendingInsert.setString(3, request.purchaseId());
                    pendingInsert.setInt(4, request.originalPrice());
                    pendingInsert.setInt(5, request.discountedPrice());
                    pendingInsert.setLong(6, request.createdAt());
                    pendingInsert.setLong(7, request.expiresAt());
                    persistenceRows += pendingInsert.executeUpdate();
                }
                try (PreparedStatement offerInsert = connection.prepareStatement(
                        "INSERT INTO buildffa_contract_offers"
                                + "(uuid,offer_index,contract_id,objective,target,reward,enhanced,vile)"
                                + " VALUES(?,?,?,?,?,?,?,?)")) {
                    for (Offer offer : request.offers()) {
                        offerInsert.setString(1, request.playerUuid().toString());
                        offerInsert.setInt(2, offer.index());
                        offerInsert.setString(3, offer.contractId());
                        offerInsert.setString(4, offer.objective());
                        offerInsert.setLong(5, offer.target());
                        offerInsert.setInt(6, offer.reward());
                        offerInsert.setBoolean(7, offer.enhanced());
                        offerInsert.setBoolean(8, offer.vile());
                        offerInsert.addBatch();
                    }
                    for (int rows : offerInsert.executeBatch()) persistenceRows += Math.max(0, rows);
                }
            } else {
                try (PreparedStatement purchaseInsert = connection.prepareStatement(
                        "INSERT INTO buildffa_killstreak_purchases"
                                + "(uuid,killstreak_id,purchased_at,paid_price,transaction_id) VALUES(?,?,?,?,?)")) {
                    purchaseInsert.setString(1, request.playerUuid().toString());
                    purchaseInsert.setString(2, request.purchaseId());
                    purchaseInsert.setLong(3, request.createdAt());
                    purchaseInsert.setInt(4, request.discountedPrice());
                    purchaseInsert.setString(5, request.transactionId().toString());
                    persistenceRows += purchaseInsert.executeUpdate();
                }
            }

            try (PreparedStatement state = connection.prepareStatement(
                    "INSERT INTO buildffa_overrides(scope,entry_key,entry_value) VALUES(?,?,?) "
                            + "ON DUPLICATE KEY UPDATE entry_value=VALUES(entry_value)")) {
                state.setString(1, request.type() == Type.CONTRACT ? "contracts" : "killstreaks");
                state.setString(2, request.playerUuid().toString());
                state.setString(3, request.encodedState() == null ? "" : request.encodedState());
                persistenceRows += state.executeUpdate();
            }

            try {
                connection.commit();
            } catch (Exception commitException) {
                rollback = rollback(connection, request, "commit failure");
                return finish(request, failure(request, Status.COMMIT_FAILURE, databaseCoins, deductionRows,
                        persistenceRows, false, rollback, "commit failed", commitException));
            }
            return finish(request, new Report(request.transactionId(), Status.SUCCESS, databaseCoins,
                    databaseCoins - request.discountedPrice(), deductionRows, persistenceRows,
                    true, false, "", null));
        } catch (java.sql.SQLIntegrityConstraintViolationException duplicate) {
            rollback = rollback(connection, request, "duplicate key");
            Status status = request.type() == Type.CONTRACT
                    ? Status.PENDING_SELECTION_EXISTS : Status.ALREADY_PURCHASED;
            return finish(request, failure(request, status, databaseCoins, deductionRows, persistenceRows,
                    false, rollback, "duplicate persistent purchase", duplicate));
        } catch (Throwable exception) {
            rollback = rollback(connection, request, "persistence exception");
            Status status = rollback ? Status.PERSISTENCE_FAILURE : Status.ROLLBACK_FAILURE;
            return finish(request, failure(request, status, databaseCoins, deductionRows, persistenceRows,
                    false, rollback, exception.getClass().getName() + ": " + exception.getMessage(), exception));
        } finally {
            if (connection != null) try { connection.close(); } catch (Exception closeException) {
                plugin.getLogger().log(Level.WARNING, "Could not close purchase transaction connection", closeException);
            }
        }
    }

    public int databaseCoins(UUID uuid) {
        if (uuid == null || !plugin.getStorageManager().isMysql()) return -1;
        try (Connection connection = plugin.getStorageManager().openTransactionConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT coins FROM buildffa_stats WHERE uuid=? AND period='LIFETIME' AND period_key='all'")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt("coins") : -1;
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Purchase diagnostic database balance query failed for " + uuid,
                    exception);
            return -1;
        }
    }

    public boolean selectContract(UUID uuid, String contractId, String difficulty, String objective,
                                  long target, long progress, int reward, long startedAt, long expiresAt,
                                  String encodedState) {
        if (uuid == null || !contractAvailable) return false;
        try (Connection connection = plugin.getStorageManager().openTransactionConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement active = connection.prepareStatement(
                        "INSERT INTO buildffa_contract_active"
                                + "(uuid,contract_id,difficulty,objective,target,progress,reward,started_at,expires_at)"
                                + " VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                                + "contract_id=VALUES(contract_id),difficulty=VALUES(difficulty),"
                                + "objective=VALUES(objective),target=VALUES(target),progress=VALUES(progress),"
                                + "reward=VALUES(reward),started_at=VALUES(started_at),expires_at=VALUES(expires_at)")) {
                    active.setString(1, uuid.toString());
                    active.setString(2, contractId);
                    active.setString(3, difficulty);
                    active.setString(4, objective);
                    active.setLong(5, target);
                    active.setLong(6, progress);
                    active.setInt(7, reward);
                    active.setLong(8, startedAt);
                    active.setLong(9, expiresAt);
                    active.executeUpdate();
                }
                writeOverride(connection, "contracts", uuid, encodedState);
                try (PreparedStatement clear = connection.prepareStatement(
                        "DELETE FROM buildffa_contract_pending WHERE uuid=?")) {
                    clear.setString(1, uuid.toString());
                    if (clear.executeUpdate() != 1) throw new SQLException("pending Contract row was not present");
                }
                connection.commit();
                return true;
            } catch (Exception exception) {
                try { connection.rollback(); } catch (Exception rollback) { exception.addSuppressed(rollback); }
                plugin.getLogger().log(Level.SEVERE, "Contract selection persistence failed for " + uuid, exception);
                return false;
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Contract selection connection failed for " + uuid, exception);
            return false;
        }
    }

    public int refundExpiredContract(UUID uuid, int paidPrice, String encodedState) {
        if (uuid == null || paidPrice < 0 || !contractAvailable) return -1;
        try (Connection connection = plugin.getStorageManager().openTransactionConnection()) {
            connection.setAutoCommit(false);
            try {
                int storedPrice;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT paid_price FROM buildffa_contract_pending WHERE uuid=? FOR UPDATE")) {
                    select.setString(1, uuid.toString());
                    try (ResultSet result = select.executeQuery()) {
                        if (!result.next()) throw new SQLException("pending Contract row was not present");
                        storedPrice = result.getInt("paid_price");
                    }
                }
                if (storedPrice != paidPrice) throw new SQLException("pending Contract paid price mismatch");
                try (PreparedStatement refund = connection.prepareStatement(
                        "UPDATE buildffa_stats SET coins=coins+? "
                                + "WHERE uuid=? AND period='LIFETIME' AND period_key='all'")) {
                    refund.setInt(1, paidPrice);
                    refund.setString(2, uuid.toString());
                    if (refund.executeUpdate() != 1) throw new SQLException("refund affected no player row");
                }
                writeOverride(connection, "contracts", uuid, encodedState);
                try (PreparedStatement clear = connection.prepareStatement(
                        "DELETE FROM buildffa_contract_pending WHERE uuid=?")) {
                    clear.setString(1, uuid.toString());
                    if (clear.executeUpdate() != 1) throw new SQLException("pending Contract delete affected no row");
                }
                int balance;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT coins FROM buildffa_stats WHERE uuid=? AND period='LIFETIME' AND period_key='all'")) {
                    select.setString(1, uuid.toString());
                    try (ResultSet result = select.executeQuery()) {
                        if (!result.next()) throw new SQLException("refunded player row disappeared");
                        balance = result.getInt("coins");
                    }
                }
                connection.commit();
                return balance;
            } catch (Exception exception) {
                try { connection.rollback(); } catch (Exception rollback) { exception.addSuppressed(rollback); }
                plugin.getLogger().log(Level.SEVERE, "Expired Contract refund transaction failed for " + uuid,
                        exception);
                return -1;
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Expired Contract refund connection failed for " + uuid, exception);
            return -1;
        }
    }

    public boolean discardPendingContract(UUID uuid, String encodedState) {
        if (uuid == null || !contractAvailable) return false;
        try (Connection connection = plugin.getStorageManager().openTransactionConnection()) {
            connection.setAutoCommit(false);
            try {
                writeOverride(connection, "contracts", uuid, encodedState);
                try (PreparedStatement clear = connection.prepareStatement(
                        "DELETE FROM buildffa_contract_pending WHERE uuid=?")) {
                    clear.setString(1, uuid.toString());
                    clear.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (Exception exception) {
                try { connection.rollback(); } catch (Exception rollback) { exception.addSuppressed(rollback); }
                plugin.getLogger().log(Level.SEVERE, "Pending Contract discard failed for " + uuid, exception);
                return false;
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Pending Contract discard connection failed for " + uuid,
                    exception);
            return false;
        }
    }

    public boolean saveKillstreakLoadout(UUID uuid, List<String> slots, String encodedState) {
        if (uuid == null || slots == null || !killstreakAvailable) return false;
        try (Connection connection = plugin.getStorageManager().openTransactionConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM buildffa_killstreak_loadout WHERE uuid=?")) {
                    delete.setString(1, uuid.toString());
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO buildffa_killstreak_loadout(uuid,slot_index,killstreak_id) VALUES(?,?,?)")) {
                    for (int index = 0; index < slots.size(); index++) {
                        insert.setString(1, uuid.toString());
                        insert.setInt(2, index);
                        String id = slots.get(index);
                        if (id == null || id.isBlank()) insert.setNull(3, java.sql.Types.VARCHAR);
                        else insert.setString(3, id);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                writeOverride(connection, "killstreaks", uuid, encodedState);
                connection.commit();
                return true;
            } catch (Exception exception) {
                try { connection.rollback(); } catch (Exception rollback) { exception.addSuppressed(rollback); }
                plugin.getLogger().log(Level.SEVERE, "Killstreak loadout persistence failed for " + uuid,
                        exception);
                return false;
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Killstreak loadout connection failed for " + uuid, exception);
            return false;
        }
    }

    public boolean saveContractState(UUID uuid, String contractId, String difficulty, String objective,
                                     long target, long progress, int reward, long startedAt, long expiresAt,
                                     String dayKey, int dailyCompletions, String encodedState) {
        if (uuid == null || !contractAvailable) return false;
        try (Connection connection = plugin.getStorageManager().openTransactionConnection()) {
            connection.setAutoCommit(false);
            try {
                writeOverride(connection, "contracts", uuid, encodedState);
                try (PreparedStatement daily = connection.prepareStatement(
                        "INSERT INTO buildffa_contract_daily(uuid,day_key,completions) VALUES(?,?,?) "
                                + "ON DUPLICATE KEY UPDATE completions=VALUES(completions)")) {
                    daily.setString(1, uuid.toString());
                    daily.setString(2, dayKey == null ? "" : dayKey);
                    daily.setInt(3, Math.max(0, dailyCompletions));
                    daily.executeUpdate();
                }
                if (objective == null || objective.isBlank()) {
                    try (PreparedStatement clear = connection.prepareStatement(
                            "DELETE FROM buildffa_contract_active WHERE uuid=?")) {
                        clear.setString(1, uuid.toString());
                        clear.executeUpdate();
                    }
                } else {
                    try (PreparedStatement active = connection.prepareStatement(
                            "INSERT INTO buildffa_contract_active"
                                    + "(uuid,contract_id,difficulty,objective,target,progress,reward,started_at,expires_at)"
                                    + " VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                                    + "contract_id=VALUES(contract_id),difficulty=VALUES(difficulty),"
                                    + "objective=VALUES(objective),target=VALUES(target),progress=VALUES(progress),"
                                    + "reward=VALUES(reward),started_at=VALUES(started_at),expires_at=VALUES(expires_at)")) {
                        active.setString(1, uuid.toString());
                        active.setString(2, contractId);
                        active.setString(3, difficulty);
                        active.setString(4, objective);
                        active.setLong(5, target);
                        active.setLong(6, progress);
                        active.setInt(7, reward);
                        active.setLong(8, startedAt);
                        active.setLong(9, expiresAt);
                        active.executeUpdate();
                    }
                }
                connection.commit();
                return true;
            } catch (Exception exception) {
                try { connection.rollback(); } catch (Exception rollback) { exception.addSuppressed(rollback); }
                plugin.getLogger().log(Level.SEVERE, "Contract state persistence failed for " + uuid, exception);
                return false;
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Contract state connection failed for " + uuid, exception);
            return false;
        }
    }

    public void cacheUpdated(Request request, boolean success, String reason) {
        plugin.getLogger().info(prefix(request) + " cacheUpdate=" + success
                + (reason == null || reason.isBlank() ? "" : " reason=" + reason));
        if (!success) lastFailure.put(request.playerUuid(), "CACHE_UPDATE_FAILURE");
    }

    public void rejected(Request request, String failureCode, String reason, Throwable exception) {
        int databaseCoins = databaseCoins(request.playerUuid());
        lastFailure.put(request.playerUuid(), failureCode == null ? "PREFLIGHT_FAILURE" : failureCode);
        String message = prefix(request)
                + " profileLoaded=" + request.profileLoaded()
                + " cachedCoins=" + request.cachedCoins()
                + " databaseCoins=" + databaseCoins
                + " originalPrice=" + request.originalPrice()
                + " discountedPrice=" + request.discountedPrice()
                + " requirementCheck=" + request.requirementResult()
                + " deductionResult=not_attempted sqlRowsAffected=0 persistenceResult=not_attempted"
                + " commit=not_attempted rollback=not_required cacheUpdate=not_attempted"
                + " failureReason=" + reason;
        if (exception == null) plugin.getLogger().warning(message);
        else plugin.getLogger().log(Level.SEVERE, message, exception);
    }

    public boolean contractAvailable() { return contractAvailable; }
    public boolean killstreakAvailable() { return killstreakAvailable; }
    public boolean requiredTablesPresent() { return requiredTablesPresent; }
    public String pending(UUID uuid) {
        UUID transaction = pending.get(uuid);
        return transaction == null ? "none" : transaction.toString();
    }
    public String lastFailure(UUID uuid) { return lastFailure.getOrDefault(uuid, "NONE"); }

    private boolean available(Type type) {
        return type == Type.CONTRACT ? contractAvailable : killstreakAvailable;
    }

    private boolean verifyTables(Connection connection) throws SQLException {
        for (String table : REQUIRED_TABLES) {
            try (ResultSet result = connection.getMetaData().getTables(connection.getCatalog(), null, table,
                    new String[]{"TABLE"})) {
                if (!result.next()) return false;
            }
        }
        return true;
    }

    private void writeOverride(Connection connection, String scope, UUID uuid, String encodedState)
            throws SQLException {
        try (PreparedStatement state = connection.prepareStatement(
                "INSERT INTO buildffa_overrides(scope,entry_key,entry_value) VALUES(?,?,?) "
                        + "ON DUPLICATE KEY UPDATE entry_value=VALUES(entry_value)")) {
            state.setString(1, scope);
            state.setString(2, uuid.toString());
            state.setString(3, encodedState == null ? "" : encodedState);
            state.executeUpdate();
        }
    }

    private boolean rollback(Connection connection, Request request, String reason) {
        if (connection == null) return false;
        try {
            connection.rollback();
            plugin.getLogger().warning(prefix(request) + " rollback=true reason=" + reason);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, prefix(request) + " rollback=false reason=" + reason, exception);
            return false;
        }
    }

    private Report failure(Request request, Status status, int databaseCoins, int deductionRows,
                           int persistenceRows, boolean committed, boolean rolledBack, String reason,
                           Throwable exception) {
        UUID transactionId = request == null ? null : request.transactionId();
        return new Report(transactionId, status, databaseCoins, databaseCoins, deductionRows,
                persistenceRows, committed, rolledBack, reason, exception);
    }

    private Report finish(Request request, Report report) {
        pending.remove(request.playerUuid(), request.transactionId());
        if (!report.success()) lastFailure.put(request.playerUuid(), report.status().name());
        else lastFailure.put(request.playerUuid(), "NONE");
        String message = prefix(request)
                + " databaseCoins=" + report.databaseCoins()
                + " deductionResult=" + (report.deductionRows() == 1)
                + " sqlRowsAffected=" + (report.deductionRows() + report.persistenceRows())
                + " persistenceResult=" + report.status()
                + " commit=" + report.committed()
                + " rollback=" + report.rolledBack()
                + " failureReason=" + (report.failureReason().isBlank() ? "none" : report.failureReason());
        if (report.exception() == null) plugin.getLogger().info(message);
        else plugin.getLogger().log(Level.SEVERE, message, report.exception());
        return report;
    }

    private void logAttempt(Request request, int databaseCoins) {
        plugin.getLogger().info(prefix(request)
                + " profileLoaded=" + request.profileLoaded()
                + " cachedCoins=" + request.cachedCoins()
                + " databaseCoins=" + databaseCoins
                + " originalPrice=" + request.originalPrice()
                + " discountedPrice=" + request.discountedPrice()
                + " requirementCheck=" + request.requirementResult()
                + " deductionResult=pending sqlRowsAffected=0 persistenceResult=pending"
                + " commit=pending rollback=pending cacheUpdate=pending failureReason=none");
    }

    private String prefix(Request request) {
        return "[Purchase tx=" + request.transactionId()
                + " player=" + request.playerUuid()
                + " type=" + request.type()
                + " id=" + request.purchaseId() + "]";
    }
}
