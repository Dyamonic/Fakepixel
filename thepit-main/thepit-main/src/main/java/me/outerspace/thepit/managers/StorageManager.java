package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.registry.DatabaseSettings;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;

public class StorageManager {
    public enum ReadStatus { FOUND, MISSING, FAILED }
    public record StoredValue(ReadStatus status, String value, String failure) {}
    public enum PurchaseWriteResult {
        SUCCESS,
        BALANCE_CONFLICT,
        REFUNDED,
        STORAGE_FAILURE,
        ROLLBACK_FAILURE
    }

    private final ThePitPlugin plugin;
    private Connection connection;
    private boolean mysql;
    private boolean mongo;
    private Object mongoClient;
    private Object mongoCollection;
    private Class<?> documentClass;
    private boolean shadedMongo;
    private volatile String lastMigrationFailure = "";

    public StorageManager(ThePitPlugin plugin) { this.plugin = plugin; }

    public void connect() {
        close();
        connectMongo();
        if (mongo) return;

        DatabaseSettings settings = plugin.registry().deployment().database();
        mysql = settings.enabled() && (settings.type().equals("MARIADB") || settings.type().equals("MYSQL"));
        if (!mysql) return;
        try {
            plugin.getLogger().info("MySQL connection target source=" + settings.source() + " host=" + settings.host()
                    + " port=" + settings.port() + " database=" + settings.database());
            connection = DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password());
            update("CREATE TABLE IF NOT EXISTS buildffa_stats (uuid VARCHAR(36) NOT NULL, name VARCHAR(16), period VARCHAR(16) NOT NULL, period_key VARCHAR(32) NOT NULL, kills INT NOT NULL DEFAULT 0, deaths INT NOT NULL DEFAULT 0, rating INT NOT NULL DEFAULT 0, coins INT NOT NULL DEFAULT 0, killstreak INT NOT NULL DEFAULT 0, rating_gained INT NOT NULL DEFAULT 0, rating_lost INT NOT NULL DEFAULT 0, prestige INT NOT NULL DEFAULT 0, PRIMARY KEY(uuid, period, period_key))");
            update("CREATE TABLE IF NOT EXISTS buildffa_overrides (scope VARCHAR(64) NOT NULL, entry_key VARCHAR(255) NOT NULL, entry_value MEDIUMTEXT NOT NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY(scope, entry_key))");
            update("CREATE TABLE IF NOT EXISTS buildffa_player_statistics (uuid VARCHAR(36) NOT NULL, statistic_id VARCHAR(64) NOT NULL, long_value BIGINT NOT NULL DEFAULT 0, double_value DOUBLE NOT NULL DEFAULT 0, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY(uuid, statistic_id))");
            update("CREATE TABLE IF NOT EXISTS buildffa_stash_entries (sequence_id BIGINT NOT NULL AUTO_INCREMENT, entry_id VARCHAR(36) NOT NULL, uuid VARCHAR(36) NOT NULL, item_data MEDIUMTEXT NOT NULL, entry_state VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE', claim_id VARCHAR(36), created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL, PRIMARY KEY(sequence_id), UNIQUE KEY uq_buildffa_stash_entry(entry_id), KEY idx_buildffa_stash_player(uuid,entry_state,sequence_id))");
            ensureColumn("deaths", "INT NOT NULL DEFAULT 0");
            ensureColumn("killstreak", "INT NOT NULL DEFAULT 0");
            ensureColumn("rating_gained", "INT NOT NULL DEFAULT 0");
            ensureColumn("rating_lost", "INT NOT NULL DEFAULT 0");
            ensureColumn("prestige", "INT NOT NULL DEFAULT 0");
            ensureColumn("level", "INT NOT NULL DEFAULT 1");
            ensureColumn("current_xp", "INT NOT NULL DEFAULT 0");
            ensureColumn("lifetime_xp", "BIGINT NOT NULL DEFAULT 0");
            ensureColumn("current_prestige_gold", "BIGINT NOT NULL DEFAULT 0");
            ensureColumn("renown", "BIGINT NOT NULL DEFAULT 0");
            ensureColumn("last_prestige", "BIGINT NOT NULL DEFAULT 0");
            ensureColumn("last_buildffa_login", "BIGINT NOT NULL DEFAULT 0");
            ensureColumn("pit_migrated", "INT NOT NULL DEFAULT 0");
            ensureColumn("prestige_migrated", "INT NOT NULL DEFAULT 0");
            ensureColumn("prefix", "VARCHAR(128) NOT NULL DEFAULT ''");
            ensureColumn("renown_upgrades", "MEDIUMTEXT");
            ensureColumn("temporary_passive_upgrades", "MEDIUMTEXT");
            ensureIndex("idx_buildffa_active_players",
                    "CREATE INDEX idx_buildffa_active_players ON buildffa_stats(period,period_key,last_buildffa_login,lifetime_xp)");
            lastMigrationFailure = "";
        } catch (Exception e) {
            mysql = false;
            lastMigrationFailure = e.getClass().getName() + ": " + e.getMessage();
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "MySQL startup migration failed; persistent The Pit mutations and purchase menus are unavailable.", e);
            try { if (connection != null && !connection.isClosed()) connection.close(); } catch (Exception closeFailure) {
                e.addSuppressed(closeFailure);
            }
            connection = null;
        }
    }

    private void connectMongo() {
        DatabaseSettings settings = plugin.registry().deployment().database();
        mongo = settings.enabled() && settings.type().equals("MONGODB");
        if (!mongo) return;
        try {
            String credentials = settings.username().isEmpty() ? "" : settings.username() + ":" + settings.password() + "@";
            String uri = "mongodb://" + credentials + settings.host() + ":" + settings.port();
            String database = settings.database();
            String collection = "stats";

            Class<?> clientsClass;
            try {
                clientsClass = Class.forName("com.mongodb.client.MongoClients");
                documentClass = Class.forName("org.bson.Document");
                shadedMongo = false;
            } catch (Throwable normalMissing) {
                clientsClass = Class.forName("club.abelix.api.shaded.mongodb.client.MongoClients");
                documentClass = Class.forName("club.abelix.api.shaded.bson.Document");
                shadedMongo = true;
            }

            mongoClient = clientsClass.getMethod("create", String.class).invoke(null, uri);
            Object mongoDatabase = invokeByName(mongoClient, "getDatabase", database);
            mongoCollection = invokeByName(mongoDatabase, "getCollection", collection);
            try { invokeByName(mongoCollection, "createIndex", doc("period", 1, "period_key", 1, "last_buildffa_login", -1, "lifetime_xp", -1)); } catch (Throwable ignored) { }
            plugin.getLogger().info("MongoDB storage enabled for The Pit.");
        } catch (Throwable t) {
            mongo = false;
            plugin.getLogger().warning("MongoDB connection failed; persistent storage is unavailable: " + t.getMessage());
        }
    }

    private Object doc(Object... values) throws Exception {
        Object d = documentClass.getConstructor().newInstance();
        Method append = documentClass.getMethod("append", String.class, Object.class);
        for (int i = 0; i + 1 < values.length; i += 2) append.invoke(d, String.valueOf(values[i]), values[i + 1]);
        return d;
    }

    private Object combine(String op, Object inner) throws Exception {
        return doc(op, inner);
    }

    private Object invokeByName(Object target, String name, Object... args) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) continue;
            return method.invoke(target, args);
        }
        throw new NoSuchMethodException(name);
    }

    public boolean isMysql() { return mysql && connection != null && !mongo; }
    public boolean isMongo() { return mongo && mongoCollection != null; }
    public boolean isPersistentStorageAvailable() { return isMysql() || isMongo(); }
    public boolean isPersistentStorageConfigured() {
        DatabaseSettings settings = plugin.registry().deployment().database();
        return settings.enabled() && (settings.type().equals("MARIADB") || settings.type().equals("MYSQL") || settings.type().equals("MONGODB"));
    }

    public record StashEntry(long sequenceId, UUID entryId, String itemData) {}

    public synchronized boolean enqueueStash(UUID uuid, UUID entryId, String itemData, int capacity) {
        if (!isMysql() || uuid == null || entryId == null || itemData == null || itemData.isEmpty()) return false;
        try {
            int count;
            try (PreparedStatement statement = prepare("SELECT COUNT(*) FROM buildffa_stash_entries WHERE uuid=?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) { count = result.next() ? result.getInt(1) : 0; }
            }
            String state = count < Math.max(1, capacity) ? "AVAILABLE" : "WAITING";
            long now = System.currentTimeMillis();
            try (PreparedStatement statement = prepare("INSERT IGNORE INTO buildffa_stash_entries(entry_id,uuid,item_data,entry_state,claim_id,created_at,updated_at) VALUES(?,?,?,?,NULL,?,?)")) {
                statement.setString(1, entryId.toString());
                statement.setString(2, uuid.toString());
                statement.setString(3, itemData);
                statement.setString(4, state);
                statement.setLong(5, now);
                statement.setLong(6, now);
                return statement.executeUpdate() == 1 || stashEntryExists(entryId);
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Stash enqueue failed for tx=" + entryId + " player=" + uuid + " code=" + exception.getErrorCode() + " message=" + exception.getMessage());
            return false;
        }
    }

    public synchronized List<StashEntry> claimStash(UUID uuid, UUID claimId, int limit) {
        List<StashEntry> entries = new ArrayList<>();
        if (!isMysql() || uuid == null || claimId == null) return entries;
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = prepare("SELECT sequence_id,entry_id,item_data FROM buildffa_stash_entries WHERE uuid=? AND entry_state IN ('AVAILABLE','WAITING') ORDER BY sequence_id LIMIT ? FOR UPDATE")) {
                statement.setString(1, uuid.toString());
                statement.setInt(2, Math.max(1, limit));
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) entries.add(new StashEntry(result.getLong("sequence_id"), UUID.fromString(result.getString("entry_id")), result.getString("item_data")));
                }
            }
            if (!entries.isEmpty()) {
                try (PreparedStatement statement = prepare("UPDATE buildffa_stash_entries SET entry_state='CLAIMING',claim_id=?,updated_at=? WHERE sequence_id=? AND entry_state IN ('AVAILABLE','WAITING')")) {
                    for (StashEntry entry : entries) {
                        statement.setString(1, claimId.toString());
                        statement.setLong(2, System.currentTimeMillis());
                        statement.setLong(3, entry.sequenceId());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
            }
            connection.commit();
        } catch (Exception exception) {
            try { connection.rollback(); } catch (Exception ignored) { }
            entries.clear();
            plugin.getLogger().warning("Stash claim failed for tx=" + claimId + " player=" + uuid + " message=" + exception.getMessage());
        } finally {
            try { connection.setAutoCommit(true); } catch (Exception ignored) { }
        }
        return entries;
    }

    public synchronized boolean finishStashClaim(UUID uuid, UUID claimId, Map<Long, String> remaining) {
        if (!isMysql() || uuid == null || claimId == null) return false;
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = prepare("DELETE FROM buildffa_stash_entries WHERE uuid=? AND claim_id=? AND sequence_id=? AND entry_state='CLAIMING'");
                 PreparedStatement update = prepare("UPDATE buildffa_stash_entries SET item_data=?,entry_state='AVAILABLE',claim_id=NULL,updated_at=? WHERE uuid=? AND claim_id=? AND sequence_id=? AND entry_state='CLAIMING'")) {
                for (Map.Entry<Long, String> entry : remaining.entrySet()) {
                    update.setString(1, entry.getValue());
                    update.setLong(2, System.currentTimeMillis());
                    update.setString(3, uuid.toString());
                    update.setString(4, claimId.toString());
                    update.setLong(5, entry.getKey());
                    update.addBatch();
                }
                update.executeBatch();
                try (PreparedStatement claimed = prepare("SELECT sequence_id FROM buildffa_stash_entries WHERE uuid=? AND claim_id=? AND entry_state='CLAIMING'")) {
                    claimed.setString(1, uuid.toString());
                    claimed.setString(2, claimId.toString());
                    try (ResultSet result = claimed.executeQuery()) {
                        while (result.next()) {
                            long sequenceId = result.getLong(1);
                            if (remaining.containsKey(sequenceId)) continue;
                            delete.setString(1, uuid.toString());
                            delete.setString(2, claimId.toString());
                            delete.setLong(3, sequenceId);
                            delete.addBatch();
                        }
                    }
                }
                delete.executeBatch();
            }
            connection.commit();
            promoteWaitingStash(uuid);
            return true;
        } catch (Exception exception) {
            try { connection.rollback(); } catch (Exception ignored) { }
            plugin.getLogger().warning("Stash finalize failed for tx=" + claimId + " player=" + uuid + " message=" + exception.getMessage());
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (Exception ignored) { }
        }
    }

    public synchronized int stashCount(UUID uuid) {
        if (!isMysql() || uuid == null) return 0;
        try (PreparedStatement statement = prepare("SELECT COUNT(*) FROM buildffa_stash_entries WHERE uuid=?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) { return result.next() ? result.getInt(1) : 0; }
        } catch (SQLException ignored) { return 0; }
    }

    private boolean stashEntryExists(UUID entryId) throws SQLException {
        try (PreparedStatement statement = prepare("SELECT 1 FROM buildffa_stash_entries WHERE entry_id=?")) {
            statement.setString(1, entryId.toString());
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    private void promoteWaitingStash(UUID uuid) throws SQLException {
        try (PreparedStatement statement = prepare("UPDATE buildffa_stash_entries SET entry_state='AVAILABLE',updated_at=? WHERE uuid=? AND entry_state='WAITING'")) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        }
    }
    public String lastMigrationFailure() { return lastMigrationFailure; }

    public Connection openTransactionConnection() throws SQLException {
        if (!isMysql()) throw new SQLException("MySQL storage is unavailable");
        DatabaseSettings settings = plugin.registry().deployment().database();
        return DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password());
    }

    public Optional<String> getOverride(String scope, String key) {
        if (isMongo()) {
            try {
                Object filter = doc("uuid", key, "period", "OVERRIDE", "period_key", scope);
                Object first = invokeByName(invokeByName(mongoCollection, "find", filter), "first");
                if (first == null) return Optional.empty();
                Object value = first.getClass().getMethod("get", Object.class).invoke(first, "entry_value");
                return value == null ? Optional.empty() : Optional.of(String.valueOf(value));
            } catch (Throwable e) {
                plugin.getLogger().warning("Could not read MongoDB runtime override " + scope + "/" + key + ": " + e.getMessage());
                return Optional.empty();
            }
        }
        if (!isMysql()) return Optional.empty();
        try (PreparedStatement ps = prepare("SELECT entry_value FROM buildffa_overrides WHERE scope=? AND entry_key=?")) {
            ps.setString(1, scope); ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty(); }
        } catch (Exception e) { plugin.getLogger().warning("Could not read runtime override " + scope + "/" + key + ": " + e.getMessage()); return Optional.empty(); }
    }

    public Map<String, String> getOverrides(String scope) {
        Map<String, String> values = new LinkedHashMap<>();
        if (isMongo()) {
            try {
                Object iterable = invokeByName(mongoCollection, "find", doc("period", "OVERRIDE", "period_key", scope));
                Iterator<?> iterator = (Iterator<?>) invokeByName(iterable, "iterator");
                while (iterator.hasNext()) {
                    Object item = iterator.next();
                    Object key = item.getClass().getMethod("get", Object.class).invoke(item, "uuid");
                    Object value = item.getClass().getMethod("get", Object.class).invoke(item, "entry_value");
                    if (key != null && value != null) values.put(String.valueOf(key), String.valueOf(value));
                }
            } catch (Throwable e) {
                plugin.getLogger().warning("Could not load MongoDB runtime overrides for " + scope + ": " + e.getMessage());
            }
            return values;
        }
        if (!isMysql()) return values;
        try (PreparedStatement ps = prepare("SELECT entry_key,entry_value FROM buildffa_overrides WHERE scope=? ORDER BY entry_key")) {
            ps.setString(1, scope);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) values.put(rs.getString(1), rs.getString(2)); }
        } catch (Exception e) { plugin.getLogger().warning("Could not load runtime overrides for " + scope + ": " + e.getMessage()); }
        return values;
    }

    public boolean putOverride(String scope, String key, String value) {
        if (isMongo()) {
            try {
                Object filter = doc("uuid", key, "period", "OVERRIDE", "period_key", scope);
                Object update = doc("$set", doc("uuid", key, "period", "OVERRIDE", "period_key", scope, "entry_value", value == null ? "" : value));
                Class<?> optionsClass = Class.forName(shadedMongo ? "club.abelix.api.shaded.mongodb.client.model.UpdateOptions" : "com.mongodb.client.model.UpdateOptions");
                Object options = optionsClass.getConstructor().newInstance();
                optionsClass.getMethod("upsert", boolean.class).invoke(options, true);
                invokeByName(mongoCollection, "updateOne", filter, update, options);
                return true;
            } catch (Throwable e) {
                plugin.getLogger().warning("Could not persist MongoDB runtime override " + scope + "/" + key + ": " + e.getMessage());
                return false;
            }
        }
        if (!isMysql()) return false;
        try (PreparedStatement ps = prepare("INSERT INTO buildffa_overrides(scope,entry_key,entry_value) VALUES(?,?,?) ON DUPLICATE KEY UPDATE entry_value=VALUES(entry_value)")) {
            ps.setString(1, scope); ps.setString(2, key); ps.setString(3, value == null ? "" : value);
            ps.executeUpdate(); return true;
        } catch (Exception e) { plugin.getLogger().warning("Could not persist runtime override " + scope + "/" + key + ": " + e.getMessage()); return false; }
    }

    public boolean deleteOverride(String scope, String key) {
        if (isMongo()) {
            try {
                invokeByName(mongoCollection, "deleteOne", doc("uuid", key, "period", "OVERRIDE", "period_key", scope));
                return true;
            } catch (Throwable e) {
                plugin.getLogger().warning("Could not delete MongoDB runtime override " + scope + "/" + key + ": " + e.getMessage());
                return false;
            }
        }
        if (!isMysql()) return false;
        try (PreparedStatement ps = prepare("DELETE FROM buildffa_overrides WHERE scope=? AND entry_key=?")) {
            ps.setString(1, scope); ps.setString(2, key); ps.executeUpdate(); return true;
        } catch (Exception e) { plugin.getLogger().warning("Could not delete runtime override " + scope + "/" + key + ": " + e.getMessage()); return false; }
    }

    public boolean deleteOverrides(String scope) {
        if (isMongo()) {
            try {
                invokeByName(mongoCollection, "deleteMany", doc("period", "OVERRIDE", "period_key", scope));
                return true;
            } catch (Throwable e) {
                plugin.getLogger().warning("Could not clear MongoDB runtime overrides for " + scope + ": " + e.getMessage());
                return false;
            }
        }
        if (!isMysql()) return false;
        try (PreparedStatement ps = prepare("DELETE FROM buildffa_overrides WHERE scope=?")) {
            ps.setString(1, scope); ps.executeUpdate(); return true;
        } catch (Exception e) { plugin.getLogger().warning("Could not clear runtime overrides for " + scope + ": " + e.getMessage()); return false; }
    }

    public int getMongoInt(String uuid, String period, String periodKey, String field, int def) {
        if (!isMongo()) return def;
        try {
            Object filter = doc("uuid", uuid, "period", period, "period_key", periodKey);
            Object result = invokeByName(mongoCollection, "find", filter);
            Object first = invokeByName(result, "first");
            if (first == null) return def;
            Object value = first.getClass().getMethod("get", Object.class).invoke(first, field);
            return value instanceof Number ? ((Number) value).intValue() : def;
        } catch (Throwable ignored) { return def; }
    }

    public StoredValue readOverride(String scope, String key) {
        if (!isPersistentStorageAvailable()) return new StoredValue(ReadStatus.FAILED, "", "persistent storage unavailable");
        if (isMongo()) {
            try {
                Object filter = doc("uuid", key, "period", "OVERRIDE", "period_key", scope);
                Object first = invokeByName(invokeByName(mongoCollection, "find", filter), "first");
                if (first == null) return new StoredValue(ReadStatus.MISSING, "", "");
                Object value = first.getClass().getMethod("get", Object.class).invoke(first, "entry_value");
                return new StoredValue(ReadStatus.FOUND, value == null ? "" : String.valueOf(value), "");
            } catch (Throwable failure) {
                return new StoredValue(ReadStatus.FAILED, "", failure.getClass().getSimpleName() + ": " + failure.getMessage());
            }
        }
        try (PreparedStatement statement = prepare("SELECT entry_value FROM buildffa_overrides WHERE scope=? AND entry_key=?")) {
            statement.setString(1, scope);
            statement.setString(2, key);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? new StoredValue(ReadStatus.FOUND, result.getString(1), "")
                        : new StoredValue(ReadStatus.MISSING, "", "");
            }
        } catch (Exception failure) {
            return new StoredValue(ReadStatus.FAILED, "", failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    public PurchaseWriteResult persistPitGoldPurchase(String scope, UUID uuid, String playerName, String value,
                                                      int expectedBalance, int updatedBalance) {
        if (scope == null || uuid == null || expectedBalance < 0 || updatedBalance < 0
                || updatedBalance > expectedBalance) return PurchaseWriteResult.STORAGE_FAILURE;
        if (isMongo()) return persistMongoPitGoldPurchase(scope, uuid, playerName, value, expectedBalance, updatedBalance);
        if (!isMysql()) return PurchaseWriteResult.STORAGE_FAILURE;
        DatabaseSettings settings = plugin.registry().deployment().database();
        try (Connection purchaseConnection = DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password())) {
            purchaseConnection.setAutoCommit(false);
            try {
                int currencyRows;
                try (PreparedStatement currency = purchaseConnection.prepareStatement(
                        "UPDATE buildffa_stats SET coins=? WHERE uuid=? AND period='LIFETIME' AND period_key='all' AND coins=?")) {
                    currency.setInt(1, updatedBalance);
                    currency.setString(2, uuid.toString());
                    currency.setInt(3, expectedBalance);
                    currencyRows = currency.executeUpdate();
                }
                if (currencyRows != 1) {
                    purchaseConnection.rollback();
                    return PurchaseWriteResult.BALANCE_CONFLICT;
                }
                try (PreparedStatement ownership = purchaseConnection.prepareStatement(
                        "INSERT INTO buildffa_overrides(scope,entry_key,entry_value) VALUES(?,?,?) "
                                + "ON DUPLICATE KEY UPDATE entry_value=VALUES(entry_value)")) {
                    ownership.setString(1, scope);
                    ownership.setString(2, uuid.toString());
                    ownership.setString(3, value == null ? "" : value);
                    ownership.executeUpdate();
                }
                purchaseConnection.commit();
                return PurchaseWriteResult.SUCCESS;
            } catch (Exception exception) {
                try {
                    purchaseConnection.rollback();
                } catch (Exception rollbackException) {
                    plugin.getLogger().severe("Pit Gold purchase rollback failed for " + scope + "/" + uuid + ": "
                            + rollbackException.getMessage());
                    return PurchaseWriteResult.ROLLBACK_FAILURE;
                }
                plugin.getLogger().warning("Pit Gold purchase storage failed for " + scope + "/" + uuid + ": "
                        + exception.getMessage());
                return PurchaseWriteResult.STORAGE_FAILURE;
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Pit Gold purchase connection failed for " + scope + "/" + uuid + ": "
                    + exception.getMessage());
            return PurchaseWriteResult.STORAGE_FAILURE;
        }
    }

    private PurchaseWriteResult persistMongoPitGoldPurchase(String scope, UUID uuid, String playerName, String value,
                                                            int expectedBalance, int updatedBalance) {
        String key = uuid.toString();
        Optional<String> originalOwnership = getOverride(scope, key);
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("coins", expectedBalance);
        Map<String, Object> changed = new LinkedHashMap<>();
        changed.put("coins", updatedBalance);
        if (!setMongoFieldsConditional(key, playerName == null ? "" : playerName,
                "LIFETIME", "all", expected, changed)) {
            return PurchaseWriteResult.BALANCE_CONFLICT;
        }
        if (putOverride(scope, key, value)) return PurchaseWriteResult.SUCCESS;
        Map<String, Object> rollbackExpected = new LinkedHashMap<>();
        rollbackExpected.put("coins", updatedBalance);
        Map<String, Object> restored = new LinkedHashMap<>();
        restored.put("coins", expectedBalance);
        boolean currencyRestored = setMongoFieldsConditional(key, playerName == null ? "" : playerName,
                "LIFETIME", "all", rollbackExpected, restored);
        boolean ownershipRestored = originalOwnership.isPresent()
                ? putOverride(scope, key, originalOwnership.get()) : deleteOverride(scope, key);
        if (currencyRestored && ownershipRestored) {
            return PurchaseWriteResult.REFUNDED;
        }
        plugin.getLogger().severe("Pit Gold purchase rollback failed for " + scope + "/" + uuid
                + ": expected balance " + expectedBalance);
        return PurchaseWriteResult.ROLLBACK_FAILURE;
    }

    public long getMongoLong(String uuid, String period, String periodKey, String field, long def) {
        if (!isMongo()) return def;
        try {
            Object filter = doc("uuid", uuid, "period", period, "period_key", periodKey);
            Object result = invokeByName(mongoCollection, "find", filter);
            Object first = invokeByName(result, "first");
            if (first == null) return def;
            Object value = first.getClass().getMethod("get", Object.class).invoke(first, field);
            return value instanceof Number ? ((Number) value).longValue() : def;
        } catch (Throwable ignored) { return def; }
    }

    public String getMongoString(String uuid, String period, String periodKey, String field, String def) {
        if (!isMongo()) return def;
        try {
            Object filter = doc("uuid", uuid, "period", period, "period_key", periodKey);
            Object first = invokeByName(invokeByName(mongoCollection, "find", filter), "first");
            if (first == null) return def;
            Object value = first.getClass().getMethod("get", Object.class).invoke(first, field);
            return value == null ? def : String.valueOf(value);
        } catch (Throwable ignored) { return def; }
    }

    public boolean setMongoFields(String uuid, String name, String period, String periodKey, Map<String, Object> fields) {
        if (!isMongo()) return false;
        try {
            Object filter = doc("uuid", uuid, "period", period, "period_key", periodKey);
            List<Object> values = new ArrayList<>();
            values.add("uuid"); values.add(uuid);
            values.add("name"); values.add(name);
            values.add("period"); values.add(period);
            values.add("period_key"); values.add(periodKey);
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                values.add(entry.getKey());
                values.add(entry.getValue());
            }
            Object update = doc("$set", doc(values.toArray()));
            Class<?> optionsClass = Class.forName(shadedMongo ? "club.abelix.api.shaded.mongodb.client.model.UpdateOptions" : "com.mongodb.client.model.UpdateOptions");
            Object options = optionsClass.getConstructor().newInstance();
            optionsClass.getMethod("upsert", boolean.class).invoke(options, true);
            invokeByName(mongoCollection, "updateOne", filter, update, options);
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("MongoDB field update failed: " + throwable.getMessage());
            return false;
        }
    }

    public boolean setMongoFieldsConditional(String uuid, String name, String period, String periodKey,
                                             Map<String, Object> expected, Map<String, Object> fields) {
        if (!isMongo()) return false;
        try {
            List<Object> filters = new ArrayList<>();
            filters.add("uuid"); filters.add(uuid);
            filters.add("period"); filters.add(period);
            filters.add("period_key"); filters.add(periodKey);
            for (Map.Entry<String, Object> entry : expected.entrySet()) {
                filters.add(entry.getKey());
                filters.add(entry.getValue());
            }
            List<Object> values = new ArrayList<>();
            values.add("uuid"); values.add(uuid);
            values.add("name"); values.add(name);
            values.add("period"); values.add(period);
            values.add("period_key"); values.add(periodKey);
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                values.add(entry.getKey());
                values.add(entry.getValue());
            }
            Object result = invokeByName(mongoCollection, "updateOne", doc(filters.toArray()), doc("$set", doc(values.toArray())));
            Object matched = invokeByName(result, "getMatchedCount");
            return matched instanceof Number && ((Number) matched).longValue() == 1L;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("MongoDB conditional field update failed: " + throwable.getMessage());
            return false;
        }
    }

    public void addMongoStats(String uuid, String name, String prefix, String period, String periodKey,
                              int kills, int deaths, int coins, int streakAdd) {
        if (!isMongo()) return;
        try {
            int currentCoins = getMongoInt(uuid, period, periodKey, "coins", 0);
            int currentStreak = getMongoInt(uuid, period, periodKey, "killstreak", 0);
            int newCoins = Math.max(0, currentCoins + coins);
            int newStreak = streakAdd < 0 ? 0 : Math.max(0, currentStreak + streakAdd);

            Object filter = doc("uuid", uuid, "period", period, "period_key", periodKey);
            Object set = doc("uuid", uuid, "name", name, "prefix", prefix, "period", period, "period_key", periodKey,
                    "coins", newCoins, "killstreak", newStreak);
            Object inc = doc("kills", kills, "deaths", deaths);
            Object update = doc("$set", set, "$inc", inc);
            Class<?> optionsClass = Class.forName(shadedMongo ?
                    "club.abelix.api.shaded.mongodb.client.model.UpdateOptions" : "com.mongodb.client.model.UpdateOptions");
            Object options = optionsClass.getConstructor().newInstance();
            optionsClass.getMethod("upsert", boolean.class).invoke(options, true);
            invokeByName(mongoCollection, "updateOne", filter, update, options);
        } catch (Throwable t) {
            plugin.getLogger().warning("MongoDB stat update failed: " + t.getMessage());
        }
    }

    public void setMongoStat(String uuid, String name, String period, String periodKey, String field, int value) {
        if (!isMongo()) return;
        try {
            Object filter = doc("uuid", uuid, "period", period, "period_key", periodKey);
            Object set = doc("uuid", uuid, "name", name, "period", period, "period_key", periodKey, field, value);
            Object update = doc("$set", set);
            Class<?> optionsClass = Class.forName(shadedMongo ?
                    "club.abelix.api.shaded.mongodb.client.model.UpdateOptions" : "com.mongodb.client.model.UpdateOptions");
            Object options = optionsClass.getConstructor().newInstance();
            optionsClass.getMethod("upsert", boolean.class).invoke(options, true);
            invokeByName(mongoCollection, "updateOne", filter, update, options);
        } catch (Throwable t) { plugin.getLogger().warning("MongoDB stat set failed: " + t.getMessage()); }
    }

    public void setMongoLong(String uuid, String name, String period, String periodKey, String field, long value) {
        if (!isMongo()) return;
        try {
            Object filter = doc("uuid", uuid, "period", period, "period_key", periodKey);
            Object set = doc("uuid", uuid, "name", name, "period", period, "period_key", periodKey, field, value);
            Object update = doc("$set", set);
            Class<?> optionsClass = Class.forName(shadedMongo ?
                    "club.abelix.api.shaded.mongodb.client.model.UpdateOptions" : "com.mongodb.client.model.UpdateOptions");
            Object options = optionsClass.getConstructor().newInstance();
            optionsClass.getMethod("upsert", boolean.class).invoke(options, true);
            invokeByName(mongoCollection, "updateOne", filter, update, options);
        } catch (Throwable t) { plugin.getLogger().warning("MongoDB stat set failed: " + t.getMessage()); }
    }

    public void maxMongoLong(String uuid, String name, String period, String periodKey, String field, long value) {
        if (!isMongo()) return;
        try {
            Object filter = doc("uuid", uuid, "period", period, "period_key", periodKey);
            Object set = doc("uuid", uuid, "name", name, "period", period, "period_key", periodKey);
            Object update = doc("$set", set, "$max", doc(field, value));
            Class<?> optionsClass = Class.forName(shadedMongo ?
                    "club.abelix.api.shaded.mongodb.client.model.UpdateOptions" : "com.mongodb.client.model.UpdateOptions");
            Object options = optionsClass.getConstructor().newInstance();
            optionsClass.getMethod("upsert", boolean.class).invoke(options, true);
            invokeByName(mongoCollection, "updateOne", filter, update, options);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("MongoDB maximum field update failed: " + throwable.getMessage());
        }
    }

    public void deleteMongoPlayer(String uuid) {
        if (!isMongo()) return;
        try {
            Object filter = doc("uuid", uuid);
            invokeByName(mongoCollection, "deleteMany", filter);
        } catch (Throwable t) { plugin.getLogger().warning("MongoDB stat wipe failed: " + t.getMessage()); }
    }

    public java.util.List<Object> topMongo(String period, String periodKey, String field, int limit) {
        java.util.List<Object> out = new java.util.ArrayList<>();
        if (!isMongo()) return out;
        try {
            Object filter = doc("period", period, "period_key", periodKey);
            Object iterable = invokeByName(mongoCollection, "find", filter);
            Object sorted = invokeByName(iterable, "sort", doc(field, -1));
            Object limited = invokeByName(sorted, "limit", limit);
            Iterator<?> it = (Iterator<?>) invokeByName(limited, "iterator");
            while (it.hasNext()) out.add(it.next());
        } catch (Throwable ignored) { }
        return out;
    }

    public java.util.List<Object> activePlayersMongo(long cutoff, int limit) {
        java.util.List<Object> out = new java.util.ArrayList<>();
        if (!isMongo()) return out;
        try {
            Object filter = doc("period", "LIFETIME", "period_key", "all", "last_buildffa_login", doc("$gte", cutoff));
            Object iterable = invokeByName(mongoCollection, "find", filter);
            Object sorted = invokeByName(iterable, "sort", doc("lifetime_xp", -1, "uuid", 1));
            Object limited = invokeByName(sorted, "limit", limit);
            Iterator<?> iterator = (Iterator<?>) invokeByName(limited, "iterator");
            while (iterator.hasNext()) out.add(iterator.next());
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable.getMessage(), throwable);
        }
        return out;
    }

    public String getDocString(Object doc, String field, String def) {
        try {
            Object val = doc.getClass().getMethod("get", Object.class).invoke(doc, field);
            return val == null ? def : String.valueOf(val);
        } catch (Throwable ignored) { return def; }
    }

    public int getDocInt(Object doc, String field, int def) {
        try {
            Object val = doc.getClass().getMethod("get", Object.class).invoke(doc, field);
            return val instanceof Number ? ((Number) val).intValue() : def;
        } catch (Throwable ignored) { return def; }
    }

    public long getDocLong(Object doc, String field, long def) {
        try {
            Object val = doc.getClass().getMethod("get", Object.class).invoke(doc, field);
            return val instanceof Number ? ((Number) val).longValue() : def;
        } catch (Throwable ignored) { return def; }
    }

    private void ensureColumn(String name, String definition) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, "buildffa_stats", name)) {
            if (columns.next()) return;
        }
        plugin.getLogger().info("Applying The Pit database migration: buildffa_stats." + name);
        update("ALTER TABLE buildffa_stats ADD COLUMN " + name + " " + definition);
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, "buildffa_stats", name)) {
            if (!columns.next()) throw new SQLException("Migration did not create buildffa_stats." + name);
        }
    }

    private void ensureIndex(String name, String sql) throws SQLException {
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null,
                "buildffa_stats", false, false)) {
            while (indexes.next()) if (name.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return;
        }
        plugin.getLogger().info("Applying The Pit database migration: " + name);
        update(sql);
    }

    public PreparedStatement prepare(String sql) throws SQLException {
        if (connection == null || connection.isClosed()) connect();
        return connection.prepareStatement(sql);
    }

    public Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) connect();
        if (!isMysql()) throw new SQLException("MySQL setup storage is unavailable");
        return connection;
    }

    public void markMysqlUnavailable() {
        try { if (connection != null && !connection.isClosed()) connection.close(); } catch (Exception ignored) {}
        connection=null;
        mysql=false;
    }

    public void update(String sql) throws SQLException {
        Statement st = connection.createStatement();
        st.executeUpdate(sql);
        st.close();
    }

    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); } catch (Exception ignored) {}
        try { if (mongoClient != null) mongoClient.getClass().getMethod("close").invoke(mongoClient); } catch (Exception ignored) {}
        connection = null; mongoClient = null; mongoCollection = null; mysql = false; mongo = false;
    }
}
