package club.abelix.games.fakevival.setup;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.config.StaticRegistryData;
import club.abelix.games.fakevival.network.SMPServerGroups;
import club.abelix.games.fakevival.managers.DungeonManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SMPSetupSnapshotImporter {
    private static final List<String> SETUP_TABLES = List.of(
            "smp_server_spawns", "smp_rtp_locations", "smp_worlds"
    );

    private final Game game;
    private final Gson gson = new Gson();

    public SMPSetupSnapshotImporter(Game game) {
        this.game = game;
    }

    public void seedIfEmptyFromStaticRegistryData() {
        String json = StaticRegistryData.smpNetworkSetupJson();
        if (json == null || json.isBlank()) {
            game.getLogger().info("[SMP Setup] StaticRegistryData network seed is not configured.");
            return;
        }

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            normalizeSnapshot(root);
            ensureNpcTable();
            normalizePersistedLobbyWorlds();
            boolean empty = setupDatabaseEmpty();
            seedNpcRows(root);
            if (empty) {
                seedServerGroups(root);
                seedRows(root, "spawns", "smp_server_spawns", List.of(
                        "spawn_key", "server_type", "server_id", "world_name", "x", "y", "z",
                        "yaw", "pitch", "updated_by", "updated_at"
                ));
                seedRows(root, "rtpLocations", "smp_rtp_locations", List.of(
                        "server_id", "world_name", "x", "y", "z", "yaw", "pitch",
                        "min_radius", "max_radius", "updated_by", "updated_at"
                ));
                seedRows(root, "smpWorlds", "smp_worlds", List.of(
                        "world_id", "display_name", "template", "parent_template", "state", "world_type",
                        "environment", "seed", "spawn_x", "spawn_y", "spawn_z", "spawn_yaw", "spawn_pitch",
                        "created_at", "updated_at", "persistent", "temporary", "save_on_hubsave",
                        "source_format", "requires_bridge"
                ));
                game.getLogger().info("[SMP Setup] Seeded empty setup tables from StaticRegistryData.");
            } else {
                game.getLogger().info("[SMP Setup] Core setup tables already contain data; imported missing NPC setup only.");
            }

            Bukkit.getScheduler().runTask(game, () -> seedLocalSetup(root));
        } catch (Exception e) {
            game.getLogger().warning("[SMP Setup] StaticRegistryData seed failed: " + e.getMessage());
        }
    }

    private void ensureNpcTable() throws Exception {
        try (Connection connection = game.getDataManager().getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS smp_setup_npcs (" +
                    "npc_id VARCHAR(64) PRIMARY KEY, name VARCHAR(128) NOT NULL DEFAULT '', skin VARCHAR(128) NULL, " +
                    "npc_type VARCHAR(32) NOT NULL DEFAULT 'PLAYER', look_at_players BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "server_type VARCHAR(32) NULL, server_id VARCHAR(64) NULL, world_name VARCHAR(128) NOT NULL, " +
                    "x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, yaw FLOAT NOT NULL, pitch FLOAT NOT NULL, action TEXT NULL)");
            statement.executeUpdate("ALTER TABLE smp_setup_npcs ADD COLUMN IF NOT EXISTS server_type VARCHAR(32) NULL");
            statement.executeUpdate("ALTER TABLE smp_setup_npcs ADD COLUMN IF NOT EXISTS server_id VARCHAR(64) NULL");
        }
    }

    private void normalizePersistedLobbyWorlds() throws Exception {
        try (Connection connection = game.getDataManager().getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE smp_server_spawns SET world_name='lobby' WHERE LOWER(world_name)='lobby' AND server_type='DUNGEON'");
            statement.executeUpdate("UPDATE smp_setup_npcs SET world_name='lobby',server_type=COALESCE(server_type,'DUNGEON') WHERE LOWER(world_name)='lobby'");
        }
    }

    void syncNpcRows(JsonObject npcs) throws Exception {
        JsonObject root = new JsonObject();
        root.add("npcs", npcs == null ? new JsonObject() : npcs.deepCopy());
        normalizeNpcs(root);
        ensureNpcTable();
        seedNpcRows(root);
    }

    private void seedNpcRows(JsonObject root) throws Exception {
        if (!root.has("npcs") || !root.get("npcs").isJsonObject()) return;
        String sql = "INSERT INTO smp_setup_npcs " +
                "(npc_id,name,skin,npc_type,look_at_players,server_type,server_id,world_name,x,y,z,yaw,pitch,action) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                "server_type=COALESCE(NULLIF(server_type,''),VALUES(server_type)),server_id=COALESCE(NULLIF(server_id,''),VALUES(server_id))," +
                "world_name=IF(LOWER(world_name)='lobby','lobby',world_name)";
        try (Connection connection = game.getDataManager().getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("npcs").entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject npc = entry.getValue().getAsJsonObject();
                String world = string(npc, "world", "");
                String serverType = string(npc, "server_type", inferNpcServerType(world));
                statement.setString(1, entry.getKey());
                statement.setString(2, string(npc, "name", entry.getKey()));
                statement.setString(3, nullableString(npc, "skin"));
                statement.setString(4, string(npc, "type", "PLAYER"));
                statement.setBoolean(5, npc.has("look") && npc.get("look").getAsBoolean());
                statement.setString(6, serverType);
                statement.setString(7, nullableString(npc, "server_id"));
                statement.setString(8, canonicalNpcWorld(world));
                statement.setDouble(9, number(npc, "x"));
                statement.setDouble(10, number(npc, "y"));
                statement.setDouble(11, number(npc, "z"));
                statement.setFloat(12, (float) number(npc, "yaw"));
                statement.setFloat(13, (float) number(npc, "pitch"));
                statement.setString(14, nullableString(npc, "action"));
                statement.addBatch();
                game.getLogger().info("[SMP] Imported NPC " + entry.getKey() + " serverType=" + serverType + " world=" + canonicalNpcWorld(world));
            }
            statement.executeBatch();
        }
    }

    private boolean setupDatabaseEmpty() throws Exception {
        try (Connection connection = game.getDataManager().getConnection()) {
            for (String table : SETUP_TABLES) {
                try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + " LIMIT 1");
                     ResultSet results = statement.executeQuery()) {
                    if (results.next()) return false;
                }
            }
            return true;
        }
    }

    private void seedServerGroups(JsonObject root) throws Exception {
        if (!root.has("serverGroups") || !root.get("serverGroups").isJsonObject()) return;
        JsonObject groups = root.getAsJsonObject("serverGroups");
        Set<ServerSeed> servers = new LinkedHashSet<>();
        addGroup(servers, groups, "HUB");
        addGroup(servers, groups, "OVERWORLD");
        addGroup(servers, groups, "DUNGEON");
        if (groups.has("registered") && groups.get("registered").isJsonArray()) {
            for (JsonElement element : groups.getAsJsonArray("registered")) {
                JsonObject row = element.getAsJsonObject();
                servers.add(new ServerSeed(normalizeServerId(row.get("server_id").getAsString()), row.get("server_type").getAsString()));
            }
        }
        if (servers.isEmpty()) return;

        String sql = "INSERT IGNORE INTO smp_servers " +
                "(server_id,server_type,online_players,max_players,status,tps,last_heartbeat,metadata_json) " +
                "VALUES (?,?,0,0,'OFFLINE',20,0,?)";
        try (Connection connection = game.getDataManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (ServerSeed server : servers) {
                statement.setString(1, server.id());
                statement.setString(2, server.type());
                statement.setString(3, "{\"seededFrom\":\"StaticRegistryData\"}");
                statement.addBatch();
            }
            statement.executeBatch();
        }
        game.getLogger().info("[SMP Setup] Seeded server groups.");
    }

    private void addGroup(Set<ServerSeed> output, JsonObject groups, String type) {
        if (!groups.has(type) || !groups.get(type).isJsonArray()) return;
        for (JsonElement element : groups.getAsJsonArray(type)) {
            output.add(new ServerSeed(normalizeServerId(element.getAsString()), type));
        }
    }

    private void seedRows(JsonObject root, String section, String table, List<String> columns) throws Exception {
        if (!root.has(section) || !root.get(section).isJsonArray()) return;
        String marks = String.join(",", Collections.nCopies(columns.size(), "?"));
        String sql = "INSERT IGNORE INTO " + table + " (" + String.join(",", columns) + ") VALUES (" + marks + ")";
        try (Connection connection = game.getDataManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (JsonElement element : root.getAsJsonArray(section)) {
                JsonObject row = element.getAsJsonObject();
                if ("smpWorlds".equals(section) && isExcludedWorld(row)) continue;
                for (int i = 0; i < columns.size(); i++) {
                    JsonElement value = row.get(columns.get(i));
                    if (value == null || value.isJsonNull()) statement.setObject(i + 1, null);
                    else if (value.getAsJsonPrimitive().isNumber()) statement.setObject(i + 1, value.getAsNumber());
                    else if (value.getAsJsonPrimitive().isBoolean()) statement.setBoolean(i + 1, value.getAsBoolean());
                    else statement.setString(i + 1, value.getAsString());
                }
                statement.addBatch();
            }
            statement.executeBatch();
        }
        game.getLogger().info("[SMP Setup] Seeded section " + section + ".");
    }

    private boolean isExcludedWorld(JsonObject world) {
        return world.has("temporary") && world.get("temporary").getAsBoolean()
                || world.has("world_type") && "DUNGEON_INSTANCE".equalsIgnoreCase(world.get("world_type").getAsString());
    }

    private void normalizeSnapshot(JsonObject root) {
        normalizeRows(root, "spawns", true);
        normalizeRows(root, "rtpLocations", false);
        normalizeRows(root, "dungeonLobby", true);
        normalizeNpcs(root);
        normalizeDungeons(root);
    }

    private void normalizeRows(JsonObject root, String section, boolean normalizeLobbyWorld) {
        if (!root.has(section) || !root.get(section).isJsonArray()) return;
        for (JsonElement element : root.getAsJsonArray(section)) {
            JsonObject row = element.getAsJsonObject();
            if (row.has("server_id")) row.addProperty("server_id", normalizeServerId(row.get("server_id").getAsString()));
            if (normalizeLobbyWorld && row.has("world_name") && "lobby".equalsIgnoreCase(row.get("world_name").getAsString())) {
                row.addProperty("world_name", "lobby");
            }
        }
    }

    private void normalizeNpcs(JsonObject root) {
        if (!root.has("npcs") || !root.get("npcs").isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("npcs").entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject npc = entry.getValue().getAsJsonObject();
            String world = canonicalNpcWorld(string(npc, "world", ""));
            npc.addProperty("world", world);
            if (!npc.has("server_type") || npc.get("server_type").isJsonNull() || npc.get("server_type").getAsString().isBlank()) {
                npc.addProperty("server_type", inferNpcServerType(world));
            } else {
                npc.addProperty("server_type", npc.get("server_type").getAsString().toUpperCase(java.util.Locale.ROOT));
            }
        }
    }

    private void normalizeDungeons(JsonObject root) {
        if (!root.has("dungeons") || !root.get("dungeons").isJsonObject()) return;
        JsonObject normalized = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("dungeons").entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            String canonicalId = DungeonManager.canonicalDungeonId(entry.getKey());
            if (!normalized.has(canonicalId)) {
                JsonObject dungeon = entry.getValue().getAsJsonObject().deepCopy();
                if (!dungeon.has("enabled")) dungeon.addProperty("enabled", true);
                normalized.add(canonicalId, dungeon);
            }
        }
        root.add("dungeons", normalized);
    }

    private String normalizeServerId(String serverId) {
        return SMPServerGroups.canonicalId(serverId);
    }

    private void seedLocalSetup(JsonObject root) {
        for (String name : List.of("zones", "portalSetup")) {
            if (!root.has(name) || !root.get(name).isJsonObject()) continue;
            String path = switch (name) {
                case "zones" -> "map-zones";
                case "portalSetup" -> "dungeon-spawn-portal";
                default -> name;
            };
            if (game.getDataManager().getData().contains(path)) continue;
            apply(game.getDataManager().getData().createSection(path), root.getAsJsonObject(name));
            game.getLogger().info("[SMP Setup] Seeded setup section " + name + ".");
        }
        mergeDungeonSetup(root);
        mergeNpcSetup(root);
        seedReservedWarps(root);
        game.getDataManager().save();
        game.getDungeonManager().refreshDungeonCache();
        if (game.getNetworkService().isDungeon()) game.getNetworkService().ensureDungeonLobbyLoaded();
        game.getNpcManager().loadNpcs();
    }

    private void mergeDungeonSetup(JsonObject root) {
        if (!root.has("dungeons") || !root.get("dungeons").isJsonObject()) return;
        ConfigurationSection dungeons = game.getDataManager().getData().getConfigurationSection("dungeons");
        if (dungeons == null) dungeons = game.getDataManager().getData().createSection("dungeons");
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("dungeons").entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            String canonicalId = DungeonManager.canonicalDungeonId(entry.getKey());
            String existingKey = null;
            for (String key : dungeons.getKeys(false)) {
                if (DungeonManager.canonicalDungeonId(key).equals(canonicalId)) {
                    existingKey = key;
                    if (key.equals(canonicalId)) break;
                }
            }
            JsonObject snapshot = entry.getValue().getAsJsonObject();
            if (existingKey == null) {
                ConfigurationSection created = dungeons.createSection(canonicalId);
                apply(created, snapshot);
                boolean enabled = snapshot.has("enabled") && snapshot.get("enabled").getAsBoolean();
                game.getLogger().info("[SMP] Loaded dungeon " + canonicalId + " enabled=" + enabled + " source=StaticRegistryData");
                continue;
            }
            ConfigurationSection existing = dungeons.getConfigurationSection(existingKey);
            if (existing != null && !existing.contains("enabled") && snapshot.has("enabled")) {
                existing.set("enabled", snapshot.get("enabled").getAsBoolean());
            }
            boolean enabled = existing != null && existing.getBoolean("enabled", snapshot.has("enabled") && snapshot.get("enabled").getAsBoolean());
            game.getLogger().info("[SMP] Loaded dungeon " + canonicalId + " enabled=" + enabled + " source=database");
        }
    }

    private void mergeNpcSetup(JsonObject root) {
        if (!root.has("npcs") || !root.get("npcs").isJsonObject()) return;
        ConfigurationSection npcs = game.getDataManager().getData().getConfigurationSection("npcs");
        if (npcs == null) npcs = game.getDataManager().getData().createSection("npcs");
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("npcs").entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            ConfigurationSection existing = npcs.getConfigurationSection(entry.getKey());
            if (existing == null) {
                apply(npcs.createSection(entry.getKey()), entry.getValue().getAsJsonObject());
            } else {
                JsonObject npc = entry.getValue().getAsJsonObject();
                if (!existing.contains("server_type")) existing.set("server_type", string(npc, "server_type", inferNpcServerType(string(npc, "world", ""))));
                if (!existing.contains("server_id") && npc.has("server_id")) existing.set("server_id", nullableString(npc, "server_id"));
                String world = existing.getString("world", string(npc, "world", ""));
                if ("lobby".equalsIgnoreCase(world)) existing.set("world", "lobby");
            }
        }
        game.getLogger().info("[SMP Setup] Seeded/updated setup section npcs.");
    }

    private String inferNpcServerType(String world) {
        if (world == null) return "UNKNOWN";
        if (world.equalsIgnoreCase("hub")) return "HUB";
        if (world.equalsIgnoreCase("lobby")) return "DUNGEON";
        if (world.equalsIgnoreCase("world") || world.equalsIgnoreCase("world_nether") || world.equalsIgnoreCase("world_the_end")) return "OVERWORLD";
        return "UNKNOWN";
    }

    private String canonicalNpcWorld(String world) {
        return world != null && world.equalsIgnoreCase("lobby") ? "lobby" : world == null ? "" : world;
    }

    private String string(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    private String nullableString(JsonObject object, String key) {
        String value = string(object, key, null);
        return value == null || value.isBlank() ? null : value;
    }

    private double number(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsDouble() : 0.0D;
    }

    private void seedReservedWarps(JsonObject root) {
        if (!root.has("warps") || !root.get("warps").isJsonArray()) return;
        String path = "network-routing.reserved-warps";
        if (game.getDataManager().getData().contains(path)) return;
        ConfigurationSection section = game.getDataManager().getData().createSection(path);
        for (JsonElement element : root.getAsJsonArray("warps")) {
            JsonObject warp = element.getAsJsonObject();
            if (!warp.has("id")) continue;
            String id = warp.get("id").getAsString();
            ConfigurationSection row = section.createSection(id);
            row.set("routing", !warp.has("routing") || warp.get("routing").getAsBoolean());
            if (warp.has("targetType")) row.set("target-type", warp.get("targetType").getAsString());
        }
        game.getLogger().info("[SMP Setup] Seeded reserved routing warps.");
    }

    private void apply(ConfigurationSection section, JsonObject object) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonObject()) apply(section.createSection(entry.getKey()), entry.getValue().getAsJsonObject());
            else section.set(entry.getKey(), gson.fromJson(entry.getValue(), Object.class));
        }
    }

    private record ServerSeed(String id, String type) {}
}
