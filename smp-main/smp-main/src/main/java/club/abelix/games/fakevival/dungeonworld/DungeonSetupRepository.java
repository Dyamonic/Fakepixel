package club.abelix.games.fakevival.dungeonworld;

import club.abelix.games.fakevival.data.DataManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Relational persistence for permanent dungeon templates. Active run state never enters this repository. */
public final class DungeonSetupRepository {
    private final DataManager dataManager;
    private final Gson gson = new Gson();

    public DungeonSetupRepository(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public boolean available() { return dataManager != null && dataManager.isMySqlEnabled(); }

    public void save(Snapshot snapshot) throws SQLException {
        try (Connection connection = dataManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO smp_dungeon_templates
                        (dungeon_id,server_type,template_world_id,template_world_name,lobby_world,spawn_world,spawn_x,spawn_y,spawn_z,
                         spawn_yaw,spawn_pitch,enabled,metadata_json,updated_at)
                        VALUES (?,'DUNGEON',?,?,?,?,?,?,?,?,?,?,?,?)
                        ON DUPLICATE KEY UPDATE template_world_id=VALUES(template_world_id),
                         template_world_name=VALUES(template_world_name),lobby_world=VALUES(lobby_world),
                         spawn_world=VALUES(spawn_world),spawn_x=VALUES(spawn_x),spawn_y=VALUES(spawn_y),
                         spawn_z=VALUES(spawn_z),spawn_yaw=VALUES(spawn_yaw),spawn_pitch=VALUES(spawn_pitch),
                         enabled=VALUES(enabled),metadata_json=VALUES(metadata_json),updated_at=VALUES(updated_at)
                        """)) {
                    statement.setString(1, snapshot.dungeonId());
                    statement.setString(2, snapshot.templateWorldId());
                    statement.setString(3, snapshot.templateWorldName());
                    statement.setString(4, snapshot.lobbyWorld());
                    bindLocation(statement, 5, snapshot.playerSpawn());
                    statement.setBoolean(11, snapshot.enabled());
                    statement.setString(12, gson.toJson(snapshot.metadata()));
                    statement.setLong(13, System.currentTimeMillis());
                    statement.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM smp_dungeon_spawn_points WHERE dungeon_id=?")) {
                    delete.setString(1, snapshot.dungeonId());
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO smp_dungeon_spawn_points
                        (dungeon_id,spawn_type,spawn_id,mob_or_boss_id,level,world,x,y,z,yaw,pitch,metadata_json)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                        """)) {
                    for (SpawnPoint point : snapshot.spawnPoints()) {
                        insert.setString(1, snapshot.dungeonId());
                        insert.setString(2, point.type());
                        insert.setString(3, point.id());
                        insert.setString(4, point.registryId());
                        insert.setInt(5, level(point.metadataJson())); insert.setString(6, point.location().world());
                        insert.setDouble(7, point.location().x()); insert.setDouble(8, point.location().y());
                        insert.setDouble(9, point.location().z()); insert.setFloat(10, point.location().yaw());
                        insert.setFloat(11, point.location().pitch()); insert.setString(12, point.metadataJson());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<Snapshot> loadAll() throws SQLException {
        Map<String, SnapshotBuilder> builders = new LinkedHashMap<>();
        try (Connection connection = dataManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM smp_dungeon_templates");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                String id = result.getString("dungeon_id");
                LocationData spawn = result.getString("spawn_world") == null ? null : new LocationData(
                        result.getString("spawn_world"), result.getDouble("spawn_x"), result.getDouble("spawn_y"),
                        result.getDouble("spawn_z"), result.getFloat("spawn_yaw"), result.getFloat("spawn_pitch"));
                builders.put(id, new SnapshotBuilder(id, result.getString("template_world_id"),
                        result.getString("template_world_name"), result.getString("lobby_world"),
                        result.getBoolean("enabled"), spawn, parseMetadata(result.getString("metadata_json"))));
            }
        }
        if (builders.isEmpty()) return List.of();
        try (Connection connection = dataManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM smp_dungeon_spawn_points");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                SnapshotBuilder builder = builders.get(result.getString("dungeon_id"));
                if (builder == null) continue;
                String metadata=result.getString("metadata_json");
                if((metadata==null||metadata.isBlank())&&result.getInt("level")>0)metadata="{\"level\":"+result.getInt("level")+"}";
                builder.points.add(new SpawnPoint(result.getString("spawn_type"), result.getString("spawn_id"),
                        result.getString("mob_or_boss_id"), new LocationData(result.getString("world"),
                        result.getDouble("x"), result.getDouble("y"), result.getDouble("z"),
                        result.getFloat("yaw"), result.getFloat("pitch")), metadata));
            }
        }
        return builders.values().stream().map(SnapshotBuilder::build).toList();
    }

    private void bindLocation(PreparedStatement statement, int start, LocationData location) throws SQLException {
        if (location == null) {
            statement.setNull(start, Types.VARCHAR);
            for (int index = start + 1; index <= start + 5; index++) statement.setNull(index, Types.DOUBLE);
            return;
        }
        statement.setString(start, location.world()); statement.setDouble(start + 1, location.x());
        statement.setDouble(start + 2, location.y()); statement.setDouble(start + 3, location.z());
        statement.setFloat(start + 4, location.yaw()); statement.setFloat(start + 5, location.pitch());
    }

    private Map<String, Object> parseMetadata(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            Map<String, Object> parsed = gson.fromJson(json, new TypeToken<Map<String, Object>>() { }.getType());
            return parsed == null ? new LinkedHashMap<>() : parsed;
        } catch (RuntimeException ignored) { }
        return new LinkedHashMap<>();
    }

    private int level(String json) {
        if (json == null || json.isBlank()) return 1;
        try { JsonObject object=gson.fromJson(json,JsonObject.class);return object!=null&&object.has("level")?Math.max(1,object.get("level").getAsInt()):1; }
        catch (RuntimeException ignored) { return 1; }
    }

    public record LocationData(String world, double x, double y, double z, float yaw, float pitch) { }
    public record SpawnPoint(String type, String id, String registryId, LocationData location, String metadataJson) { }
    public record Snapshot(String dungeonId, String templateWorldId, String templateWorldName, String lobbyWorld,
                           boolean enabled, LocationData playerSpawn, List<SpawnPoint> spawnPoints,
                           Map<String, Object> metadata) { }

    private static final class SnapshotBuilder {
        private final String id, worldId, worldName, lobby;
        private final boolean enabled;
        private final LocationData spawn;
        private final Map<String, Object> metadata;
        private final List<SpawnPoint> points = new ArrayList<>();
        private SnapshotBuilder(String id, String worldId, String worldName, String lobby, boolean enabled,
                                LocationData spawn, Map<String, Object> metadata) {
            this.id=id; this.worldId=worldId; this.worldName=worldName; this.lobby=lobby;
            this.enabled=enabled; this.spawn=spawn; this.metadata=metadata;
        }
        private Snapshot build() { return new Snapshot(id, worldId, worldName, lobby, enabled, spawn, points, metadata); }
    }
}
