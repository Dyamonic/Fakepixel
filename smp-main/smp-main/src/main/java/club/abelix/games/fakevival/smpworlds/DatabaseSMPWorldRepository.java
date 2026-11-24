package club.abelix.games.fakevival.smpworlds;

import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DatabaseSMPWorldRepository implements SMPWorldRepository {
    private final DataManager dataManager;

    public DatabaseSMPWorldRepository(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void initialize() throws SQLException {
        try (Connection connection = dataManager.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS smp_worlds (
                      id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                      world_id VARCHAR(64) NOT NULL UNIQUE,
                      display_name VARCHAR(128),
                      template BOOLEAN NOT NULL DEFAULT FALSE,
                      parent_template VARCHAR(64) NULL,
                      state VARCHAR(32) NOT NULL DEFAULT 'UNLOADED',
                      world_type VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
                      environment VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
                      seed BIGINT NULL,
                      spawn_x DOUBLE DEFAULT 0,
                      spawn_y DOUBLE DEFAULT 64,
                      spawn_z DOUBLE DEFAULT 0,
                      spawn_yaw FLOAT DEFAULT 0,
                      spawn_pitch FLOAT DEFAULT 0,
                      created_at BIGINT NOT NULL,
                      updated_at BIGINT NOT NULL,
                      persistent BOOLEAN NOT NULL DEFAULT TRUE,
                      temporary BOOLEAN NOT NULL DEFAULT FALSE,
                      save_on_hubsave BOOLEAN NOT NULL DEFAULT TRUE
                      ,source_format VARCHAR(32) NOT NULL DEFAULT 'SMP'
                      ,requires_bridge BOOLEAN NOT NULL DEFAULT FALSE
                    )
                    """);
            statement.executeUpdate("ALTER TABLE smp_worlds ADD COLUMN IF NOT EXISTS persistent BOOLEAN NOT NULL DEFAULT TRUE");
            statement.executeUpdate("ALTER TABLE smp_worlds ADD COLUMN IF NOT EXISTS temporary BOOLEAN NOT NULL DEFAULT FALSE");
            statement.executeUpdate("ALTER TABLE smp_worlds ADD COLUMN IF NOT EXISTS save_on_hubsave BOOLEAN NOT NULL DEFAULT TRUE");
            statement.executeUpdate("ALTER TABLE smp_worlds ADD COLUMN IF NOT EXISTS source_format VARCHAR(32) NOT NULL DEFAULT 'SMP'");
            statement.executeUpdate("ALTER TABLE smp_worlds ADD COLUMN IF NOT EXISTS requires_bridge BOOLEAN NOT NULL DEFAULT FALSE");
            statement.executeUpdate("ALTER TABLE smp_worlds ADD COLUMN IF NOT EXISTS saved_by_uuid VARCHAR(36) NULL");
            statement.executeUpdate("ALTER TABLE smp_worlds ADD COLUMN IF NOT EXISTS saved_by_name VARCHAR(32) NULL");
            statement.executeUpdate("ALTER TABLE smp_worlds ADD COLUMN IF NOT EXISTS linked_dungeon_id VARCHAR(128) NULL");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS smp_world_data (
                      world_id VARCHAR(64) PRIMARY KEY,
                      format_version INT NOT NULL,
                      compression VARCHAR(32) NOT NULL DEFAULT 'ZIP',
                      data LONGBLOB NOT NULL,
                      checksum VARCHAR(128) NULL,
                      size_bytes BIGINT NOT NULL,
                      updated_at BIGINT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS smp_world_instances (
                      instance_id VARCHAR(96) PRIMARY KEY,
                      template_id VARCHAR(64) NOT NULL,
                      world_id VARCHAR(96) NOT NULL,
                      state VARCHAR(32) NOT NULL DEFAULT 'CREATED',
                      owner_uuid VARCHAR(36) NULL,
                      party_id VARCHAR(64) NULL,
                      server_id VARCHAR(64) NULL,
                      created_at BIGINT NOT NULL,
                      updated_at BIGINT NOT NULL,
                      expires_at BIGINT NULL
                    )
                    """);
        }
    }

    @Override
    public boolean isAvailable() {
        return dataManager != null && dataManager.isMySqlEnabled();
    }

    @Override
    public int countWorlds() throws SQLException {
        try (Connection connection = dataManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM smp_worlds");
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    @Override
    public List<SMPWorldMetadata> listWorlds() throws SQLException {
        try (Connection connection = dataManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM smp_worlds ORDER BY world_id ASC");
             ResultSet result = statement.executeQuery()) {
            List<SMPWorldMetadata> worlds = new ArrayList<>();
            while (result.next()) worlds.add(readMetadata(result));
            return worlds;
        }
    }

    @Override
    public Optional<SMPWorldMetadata> findMetadata(String worldId) throws SQLException {
        try (Connection connection = dataManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM smp_worlds WHERE world_id=?")) {
            statement.setString(1, worldId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readMetadata(result)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<SMPWorldData> findData(String worldId) throws SQLException {
        try (Connection connection = dataManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM smp_world_data WHERE world_id=?")) {
            statement.setString(1, worldId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(new SMPWorldData(
                        result.getString("world_id"),
                        result.getInt("format_version"),
                        result.getString("compression"),
                        result.getBytes("data"),
                        result.getString("checksum"),
                        result.getLong("size_bytes"),
                        result.getLong("updated_at")
                ));
            }
        }
    }

    @Override
    public void insertWorld(SMPWorldMetadata metadata, SMPWorldData data) throws SQLException {
        try (Connection connection = dataManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (exists(connection, metadata.worldId())) throw new SQLException("A SMPWorld with id " + metadata.worldId() + " already exists.");
                insertMetadata(connection, metadata);
                insertData(connection, data);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public void saveWorld(SMPWorldMetadata metadata, SMPWorldData data) throws SQLException {
        try (Connection connection = dataManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                saveMetadata(connection, metadata);
                saveData(connection, data);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public void saveMetadata(SMPWorldMetadata metadata) throws SQLException {
        try (Connection connection = dataManager.getConnection()) {
            saveMetadata(connection, metadata);
        }
    }

    @Override
    public void saveData(SMPWorldData data) throws SQLException {
        try (Connection connection = dataManager.getConnection()) {
            saveData(connection, data);
        }
    }

    @Override
    public void updateState(String worldId, SMPWorldState state) throws SQLException {
        try (Connection connection = dataManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE smp_worlds SET state=?, updated_at=? WHERE world_id=?")) {
            statement.setString(1, state.name());
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, worldId);
            statement.executeUpdate();
        }
    }

    @Override
    public void updateSaveMetadata(String worldId, String savedByUuid, String savedByName, String linkedDungeonId) throws SQLException {
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE smp_worlds SET saved_by_uuid=?,saved_by_name=?,linked_dungeon_id=COALESCE(?,linked_dungeon_id),updated_at=? WHERE world_id=?")) {
            statement.setString(1, savedByUuid); statement.setString(2, savedByName);
            statement.setString(3, linkedDungeonId); statement.setLong(4, System.currentTimeMillis());
            statement.setString(5, worldId); statement.executeUpdate();
        }
    }

    @Override
    public void cloneWorld(String templateId, String newWorldId) throws SQLException {
        try (Connection connection = dataManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                SMPWorldMetadata template = findMetadata(templateId).orElseThrow(() -> new SQLException("Template not found: " + templateId));
                SMPWorldData data = findData(templateId).orElseThrow(() -> new SQLException("Template data not found: " + templateId));
                long now = System.currentTimeMillis();
                saveMetadata(connection, template.asClone(newWorldId, templateId));
                saveData(connection, new SMPWorldData(newWorldId, data.formatVersion(), data.compression(),
                        data.data(), data.checksum(), data.sizeBytes(), now));
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public void deleteWorld(String worldId) throws SQLException {
        try (Connection connection = dataManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteData = connection.prepareStatement("DELETE FROM smp_world_data WHERE world_id=?");
                 PreparedStatement deleteMeta = connection.prepareStatement("DELETE FROM smp_worlds WHERE world_id=?")) {
                deleteData.setString(1, worldId);
                deleteData.executeUpdate();
                deleteMeta.setString(1, worldId);
                deleteMeta.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public void insertInstance(String instanceId, String templateId, String worldId, String ownerUuid,
                               String partyId, String serverId, Long expiresAt) throws SQLException {
        long now = System.currentTimeMillis();
        try (Connection connection = dataManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     REPLACE INTO smp_world_instances
                     (instance_id, template_id, world_id, state, owner_uuid, party_id, server_id, created_at, updated_at, expires_at)
                     VALUES (?, ?, ?, 'CREATED', ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, instanceId);
            statement.setString(2, templateId);
            statement.setString(3, worldId);
            statement.setString(4, ownerUuid);
            statement.setString(5, partyId);
            statement.setString(6, serverId);
            statement.setLong(7, now);
            statement.setLong(8, now);
            if (expiresAt == null) statement.setNull(9, java.sql.Types.BIGINT);
            else statement.setLong(9, expiresAt);
            statement.executeUpdate();
        }
    }

    private void saveMetadata(Connection connection, SMPWorldMetadata metadata) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO smp_worlds
                (world_id, display_name, template, parent_template, state, world_type, environment, seed,
                 spawn_x, spawn_y, spawn_z, spawn_yaw, spawn_pitch, created_at, updated_at,
                 persistent, temporary, save_on_hubsave, source_format, requires_bridge)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  display_name=VALUES(display_name), template=VALUES(template), parent_template=VALUES(parent_template),
                  state=VALUES(state), world_type=VALUES(world_type), environment=VALUES(environment), seed=VALUES(seed),
                  spawn_x=VALUES(spawn_x), spawn_y=VALUES(spawn_y), spawn_z=VALUES(spawn_z),
                  spawn_yaw=VALUES(spawn_yaw), spawn_pitch=VALUES(spawn_pitch), updated_at=VALUES(updated_at),
                  persistent=VALUES(persistent), temporary=VALUES(temporary), save_on_hubsave=VALUES(save_on_hubsave),
                  source_format=VALUES(source_format), requires_bridge=VALUES(requires_bridge)
                """)) {
            statement.setString(1, metadata.worldId());
            statement.setString(2, metadata.displayName());
            statement.setBoolean(3, metadata.template());
            statement.setString(4, metadata.parentTemplate());
            statement.setString(5, metadata.state().name());
            statement.setString(6, metadata.worldType());
            statement.setString(7, metadata.environment().name());
            if (metadata.seed() == null) statement.setNull(8, java.sql.Types.BIGINT);
            else statement.setLong(8, metadata.seed());
            statement.setDouble(9, metadata.spawnX());
            statement.setDouble(10, metadata.spawnY());
            statement.setDouble(11, metadata.spawnZ());
            statement.setFloat(12, metadata.spawnYaw());
            statement.setFloat(13, metadata.spawnPitch());
            statement.setLong(14, metadata.createdAt());
            statement.setLong(15, metadata.updatedAt());
            statement.setBoolean(16, metadata.persistent());
            statement.setBoolean(17, metadata.temporary());
            statement.setBoolean(18, metadata.saveOnHubsave());
            statement.setString(19, metadata.sourceFormat());
            statement.setBoolean(20, metadata.requiresBridge());
            statement.executeUpdate();
        }
    }

    private boolean exists(Connection connection, String worldId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM smp_worlds WHERE world_id=? LIMIT 1")) {
            statement.setString(1, worldId);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    private void insertMetadata(Connection connection, SMPWorldMetadata metadata) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO smp_worlds (world_id,display_name,template,parent_template,state,world_type,environment,seed,spawn_x,spawn_y,spawn_z,spawn_yaw,spawn_pitch,created_at,updated_at,persistent,temporary,save_on_hubsave,source_format,requires_bridge) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            bindMetadata(statement, metadata);
            statement.executeUpdate();
        }
    }

    private void insertData(Connection connection, SMPWorldData data) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO smp_world_data (world_id,format_version,compression,data,checksum,size_bytes,updated_at) VALUES (?,?,?,?,?,?,?)")) {
            statement.setString(1, data.worldId()); statement.setInt(2, data.formatVersion()); statement.setString(3, data.compression());
            statement.setBytes(4, data.data()); statement.setString(5, data.checksum()); statement.setLong(6, data.sizeBytes()); statement.setLong(7, data.updatedAt());
            statement.executeUpdate();
        }
    }

    private void bindMetadata(PreparedStatement statement, SMPWorldMetadata metadata) throws SQLException {
        statement.setString(1, metadata.worldId()); statement.setString(2, metadata.displayName()); statement.setBoolean(3, metadata.template());
        statement.setString(4, metadata.parentTemplate()); statement.setString(5, metadata.state().name()); statement.setString(6, metadata.worldType());
        statement.setString(7, metadata.environment().name()); if (metadata.seed() == null) statement.setNull(8, java.sql.Types.BIGINT); else statement.setLong(8, metadata.seed());
        statement.setDouble(9, metadata.spawnX()); statement.setDouble(10, metadata.spawnY()); statement.setDouble(11, metadata.spawnZ());
        statement.setFloat(12, metadata.spawnYaw()); statement.setFloat(13, metadata.spawnPitch()); statement.setLong(14, metadata.createdAt()); statement.setLong(15, metadata.updatedAt());
        statement.setBoolean(16, metadata.persistent()); statement.setBoolean(17, metadata.temporary()); statement.setBoolean(18, metadata.saveOnHubsave());
        statement.setString(19, metadata.sourceFormat()); statement.setBoolean(20, metadata.requiresBridge());
    }

    private void saveData(Connection connection, SMPWorldData data) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                REPLACE INTO smp_world_data
                (world_id, format_version, compression, data, checksum, size_bytes, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, data.worldId());
            statement.setInt(2, data.formatVersion());
            statement.setString(3, data.compression());
            statement.setBytes(4, data.data());
            statement.setString(5, data.checksum());
            statement.setLong(6, data.sizeBytes());
            statement.setLong(7, data.updatedAt());
            statement.executeUpdate();
        }
    }

    private SMPWorldMetadata readMetadata(ResultSet result) throws SQLException {
        Long seed = result.getObject("seed") == null ? null : result.getLong("seed");
        return new SMPWorldMetadata(
                result.getString("world_id"),
                result.getString("display_name"),
                result.getBoolean("template"),
                result.getString("parent_template"),
                parseState(result.getString("state")),
                result.getString("world_type"),
                parseEnvironment(result.getString("environment")),
                seed,
                result.getDouble("spawn_x"),
                result.getDouble("spawn_y"),
                result.getDouble("spawn_z"),
                result.getFloat("spawn_yaw"),
                result.getFloat("spawn_pitch"),
                result.getLong("created_at"),
                result.getLong("updated_at"),
                result.getBoolean("persistent"),
                result.getBoolean("temporary"),
                result.getBoolean("save_on_hubsave"),
                result.getString("source_format"),
                result.getBoolean("requires_bridge")
        );
    }

    private SMPWorldState parseState(String value) {
        try {
            return SMPWorldState.valueOf(value == null ? "UNLOADED" : value);
        } catch (IllegalArgumentException ignored) {
            return SMPWorldState.UNLOADED;
        }
    }

    private World.Environment parseEnvironment(String value) {
        try {
            return World.Environment.valueOf(value == null ? "NORMAL" : value);
        } catch (IllegalArgumentException ignored) {
            return World.Environment.NORMAL;
        }
    }
}
