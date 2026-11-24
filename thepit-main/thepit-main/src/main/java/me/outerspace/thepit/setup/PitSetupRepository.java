package me.outerspace.thepit.setup;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.managers.StorageManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PitSetupRepository {
    private final StorageManager storage;
    private final PitYamlSetupStore yaml;

    public PitSetupRepository(ThePitPlugin plugin) {
        storage = plugin.getStorageManager();
        yaml = new PitYamlSetupStore(plugin);
    }

    public boolean available() { return yaml.available(); }
    public boolean databaseAvailable() { return false; }
    public boolean migrationDatabaseAvailable() { return storage.isMysql(); }
    public PitYamlSetupStore yaml() { return yaml; }
    public void migrateLegacyNpcsAtStartup() throws SQLException {
        try { yaml.migrateLegacyGlobalNpcs(); } catch (Exception exception) { throw new SQLException(exception); }
    }
    public Map<String,Integer> counts() throws SQLException {
        try {
            PitSetupSnapshot snapshot = yaml.load();
            Map<String,Integer> values = new LinkedHashMap<>();
            values.put("arenas",snapshot.arenas.size());values.put("leaderboards",snapshot.leaderboards.size());values.put("npcs",snapshot.npcs.size());
            return values;
        } catch (Exception exception) { throw new SQLException(exception); }
    }

    public PitSetupSnapshot load() throws SQLException {
        try { return yaml.load(); } catch (Exception exception) { throw new SQLException(exception); }
    }

    public List<PitSetupSnapshot.Npc> loadDatabaseNpcsForMigration() throws SQLException {
        if (!migrationDatabaseAvailable()) throw new SQLException("MySQL is not configured");
        List<PitSetupSnapshot.Npc> values = new ArrayList<>();
        try (PreparedStatement statement = storage.prepare("SELECT * FROM buildffa_npcs ORDER BY stable_id"); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                PitSetupSnapshot.Npc value = new PitSetupSnapshot.Npc();
                value.stableId=result.getString("stable_id");value.arenaId=result.getString("arena_id");value.worldName=result.getString("world_name");value.name=result.getString("npc_name");value.location=new PitSetupSnapshot.Location(value.worldName,result.getDouble("x"),result.getDouble("y"),result.getDouble("z"),result.getFloat("yaw"),result.getFloat("pitch"));value.skinName=result.getString("skin_name");value.skinValue=result.getString("skin_value");value.skinSignature=result.getString("skin_signature");value.command=result.getString("command_text");value.lookAtPlayers=result.getBoolean("look_at_players");value.enabled=result.getBoolean("enabled");
                values.add(value);
            }
        }
        return values;
    }

    public void importSnapshot(PitSetupSnapshot snapshot, boolean replace) throws SQLException { try { yaml.importSnapshot(snapshot,replace); } catch (Exception exception) { throw new SQLException(exception); } }
    public boolean upsertArenaLocation(String arena,String type,PitSetupSnapshot.Location location){return yaml.upsertArenaLocation(arena,type,location);}
    public boolean upsertRegionCorner(String arena,String region,String corner,PitSetupSnapshot.Location location){return yaml.upsertRegionCorner(arena,region,corner,location);}
    public boolean upsertLeaderboard(PitSetupSnapshot.Leaderboard value){return yaml.upsertLeaderboard(value);}
    public boolean deleteLeaderboard(String id){return yaml.deleteLeaderboard(id);}
    public boolean upsertNpc(PitSetupSnapshot.Npc value){return yaml.upsertNpc(value);}
    public boolean deleteNpc(String id){return yaml.deleteNpc(id);}
    public boolean deleteNpc(String arena,String id){return yaml.deleteNpc(arena,id);}
    public String metadata(String key){return "";}
    public void recordExport(Instant instant){ }
    public void setMetadata(String key,String value){ }
}
