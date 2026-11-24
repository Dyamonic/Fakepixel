package me.outerspace.thepit.setup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PitSetupValidator {
    public SetupValidationResult validate(PitSetupSnapshot snapshot) {
        SetupValidationResult result = new SetupValidationResult();
        if (snapshot == null) { result.add("Snapshot is empty"); return result; }
        if (snapshot.schemaVersion != 1) result.add("Unsupported schema version: " + snapshot.schemaVersion);
        if (snapshot.arenas == null || snapshot.leaderboards == null || snapshot.npcs == null) {
            result.add("Required snapshot arrays are missing");
            return result;
        }
        Set<String> arenas = new HashSet<>();
        for (PitSetupSnapshot.Arena arena : snapshot.arenas) {
            if (arena == null || blank(arena.id)) { result.add("Arena ID is missing"); continue; }
            if (!arenas.add(arena.id)) result.add("Duplicate arena ID: " + arena.id);
            if (blank(arena.worldName)) result.add("Arena " + arena.id + " has no world name");
            if (arena.spawn == null) result.add("Arena " + arena.id + " has no spawn");
            location(result, "arena " + arena.id + " spawn", arena.spawn, arena.id, arena.worldName);
            location(result, "arena " + arena.id + " death", arena.death, arena.id, arena.worldName);
            location(result, "arena " + arena.id + " spectator", arena.spectator, arena.id, arena.worldName);
            location(result, "arena " + arena.id + " void return", arena.voidReturn, arena.id, arena.worldName);
            region(result, arena.id, arena.worldName, "spawn protection", arena.spawnProtection);
            region(result, arena.id, arena.worldName, "PvP arena", arena.pvpArena);
        }
        ids(result, snapshot.leaderboards, arenas, "leaderboard");
        for (PitSetupSnapshot.Leaderboard value : snapshot.leaderboards) if (value != null) { location(result, "leaderboard " + value.stableId, value.location, value.arenaId, null); if(!oneOf(value.statType,"KILLS","LEVEL","XP","PRESTIGE"))result.add("Leaderboard "+value.stableId+" has invalid stat type");if(!oneOf(value.period,"LIFETIME","MONTHLY","WEEKLY","DAILY"))result.add("Leaderboard "+value.stableId+" has invalid period"); }
        return result;
    }

    private void ids(SetupValidationResult result, List<?> values, Set<String> arenas, String type) {
        Set<String> ids = new HashSet<>();
        for (Object value : values) {
            String id;
            String arena;
            if (value instanceof PitSetupSnapshot.Leaderboard v) { id = v.stableId; arena = v.arenaId; }
            else if (value instanceof PitSetupSnapshot.Npc v) { id = v.stableId; arena = v.arenaId; }
            else { result.add("Null " + type + " record"); continue; }
            if (blank(id)) result.add(type + " stable ID is missing");
            else if (!ids.add(arena + ":" + id)) result.add("Duplicate " + type + " stable ID inside arena " + arena + ": " + id);
            if (blank(arena) || !arenas.contains(arena)) result.add(type + " " + id + " references unknown arena: " + arena);
        }
    }

    private void region(SetupValidationResult result, String arena, String world, String name, PitSetupSnapshot.Region region) {
        if (region == null) return;
        if (region.pos1 == null || region.pos2 == null) result.add("Arena " + arena + " has incomplete " + name + " region");
        location(result, "arena " + arena + " " + name + " pos1", region.pos1, arena, world);
        location(result, "arena " + arena + " " + name + " pos2", region.pos2, arena, world);
    }

    private void location(SetupValidationResult result, String label, PitSetupSnapshot.Location location, String arena, String expectedWorld) {
        if (location == null) return;
        if (blank(location.worldName)) result.add(label + " has no world name");
        if (!blank(expectedWorld) && !expectedWorld.equals(location.worldName)) result.add(label + " world does not match " + expectedWorld);
        if (!finite(location.x) || !finite(location.y) || !finite(location.z) || !finite(location.yaw) || !finite(location.pitch)) result.add(label + " contains a non-finite number");
    }

    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private boolean finite(double value) { return Double.isFinite(value); }
    private boolean oneOf(String value,String... allowed){if(value==null)return false;for(String candidate:allowed)if(candidate.equalsIgnoreCase(value))return true;return false;}
}
