package me.outerspace.thepit.setup;

import me.outerspace.thepit.ThePitPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PitSetupService {
    public enum Source { MARIADB, YAML }

    private final ThePitPlugin plugin;
    private final PitSetupRepository repository;
    private final PitSetupValidator validator = new PitSetupValidator();
    private volatile PitSetupSnapshot snapshot = new PitSetupSnapshot();
    private volatile Source source = Source.YAML;
    private volatile Map<String, PitSetupSnapshot.Arena> arenas = Collections.emptyMap();

    public PitSetupService(ThePitPlugin plugin) {
        this.plugin = plugin;
        this.repository = new PitSetupRepository(plugin);
    }

    public boolean initialize() {
        try {
            try { repository.migrateLegacyNpcsAtStartup(); }
            catch (Exception exception) { plugin.getLogger().severe("Legacy NPC startup migration failed: " + exception.getMessage()); }
            if (!reloadFromStorage()) throw new IOException("No valid setup data was loaded");
            return true;
        } catch (Exception exception) {
            plugin.getLogger().severe("Could not initialize The Pit arena setup: " + exception.getMessage());
            return false;
        }
    }

    public boolean reloadFromDatabase() {
        return reloadFromStorage();
    }

    public boolean reloadFromStorage() {
        try {
            PitSetupSnapshot loaded = repository.load();
            SetupValidationResult result = validator.validate(loaded);
            if (!result.valid()) throw new IllegalArgumentException(String.join("; ", result.errors()));
            replaceCache(loaded);
            source = repository.databaseAvailable() ? Source.MARIADB : Source.YAML;
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not reload The Pit setup storage; previous values remain active: " + exception.getMessage());
            return false;
        }
    }

    public ExportResult exportSnapshot() throws IOException {
        if (!reloadFromStorage()) throw new IOException("Could not read setup storage");
        Instant exportedAt = Instant.now();
        PitSetupSnapshot output = sorted(snapshot);
        output.exportedAt = exportedAt.toString();
        File file = new File(plugin.getDataFolder(), "thepit-arena-setup.yml");
        repository.yaml().exportSnapshot(file, output);
        repository.recordExport(exportedAt);
        return new ExportResult(file.toPath().toAbsolutePath(), output.arenas.size(), output.leaderboards.size(), output.npcs.size());
    }

    public SetupValidationResult validateCurrent() { return validator.validate(snapshot); }

    public ImportResult importFile(boolean replace) {
        File file = new File(plugin.getDataFolder(), "thepit-arena-setup.yml");
        if (!file.isFile()) file = new File(plugin.getDataFolder(), "buildffa-arena-setup.yml");
        try {
            PitSetupSnapshot imported = repository.yaml().read(file);
            SetupValidationResult validation = validator.validate(imported);
            if (!validation.valid()) return ImportResult.failure(String.join("; ", validation.errors()));
            repository.importSnapshot(sorted(imported), replace);
            if (!reloadFromStorage()) return ImportResult.failure("Imported setup could not be reloaded");
            return ImportResult.success(file.toPath().toAbsolutePath());
        } catch (Exception exception) {
            return ImportResult.failure(exception.getMessage());
        }
    }

    public boolean setLocation(String arena, String type, org.bukkit.Location location) {
        if (!repository.available() || !arenas.containsKey(arena)) return false;
        PitSetupSnapshot.Location value = location(location);
        if (!repository.upsertArenaLocation(arena, type, value)) return false;
        PitSetupSnapshot.Arena target = arenas.get(arena);
        switch (type) { case "SPAWN" -> target.spawn=value; case "DEATH" -> target.death=value; case "SPECTATOR" -> target.spectator=value; case "VOID_RETURN" -> target.voidReturn=value; }
        return true;
    }

    public boolean setRegionCorner(String arena, String region, String corner, org.bukkit.Location location) {
        if (!repository.available() || !arenas.containsKey(arena)) return false;
        PitSetupSnapshot.Location value = location(location);
        if (!repository.upsertRegionCorner(arena, region, corner, value)) return false;
        PitSetupSnapshot.Arena target = arenas.get(arena);
        PitSetupSnapshot.Region targetRegion = region.equals("SPAWN_PROTECTION") ? target.spawnProtection : target.pvpArena;
        if (targetRegion == null) targetRegion = new PitSetupSnapshot.Region();
        if (corner.equals("POS1")) targetRegion.pos1=value; else targetRegion.pos2=value;
        if (region.equals("SPAWN_PROTECTION")) target.spawnProtection=targetRegion; else target.pvpArena=targetRegion;
        return true;
    }

    public PitSetupSnapshot snapshot() { return snapshot; }
    public Optional<PitSetupSnapshot.Arena> arena(String id) { return Optional.ofNullable(arenas.get(normalize(id))); }
    public Map<String, PitSetupSnapshot.Arena> arenas() { return arenas; }
    public PitSetupRepository repository() { return repository; }
    public Source source() { return source; }
    public String schemaVersion() { return String.valueOf(snapshot.schemaVersion); }
    public String lastExportAt() { return repository.databaseAvailable() ? repository.metadata("last_export_at") : snapshot.exportedAt; }

    private void replaceCache(PitSetupSnapshot value) { snapshot = sorted(value); rebuildIndexes(); }
    private void rebuildIndexes() { Map<String,PitSetupSnapshot.Arena> map=new LinkedHashMap<>();for(PitSetupSnapshot.Arena arena:snapshot.arenas)map.put(normalize(arena.id),arena);arenas=Collections.unmodifiableMap(map); }
    private String normalize(String id) { return id == null ? "" : id.trim().toLowerCase(java.util.Locale.ENGLISH).replaceAll("\\s+", "_"); }
    private PitSetupSnapshot.Location location(org.bukkit.Location value) { return new PitSetupSnapshot.Location(value.getWorld().getName(),value.getX(),value.getY(),value.getZ(),value.getYaw(),value.getPitch()); }

    private PitSetupSnapshot sorted(PitSetupSnapshot input) {
        input.arenas.sort(Comparator.comparing(v -> v.id));input.leaderboards.sort(Comparator.comparing(v -> v.stableId));input.npcs.sort(Comparator.comparing(v -> v.stableId));
        return input;
    }

    public record ExportResult(Path path, int arenas, int leaderboards, int npcs) {}
    public record ImportResult(boolean success, Path path, String error) { public static ImportResult success(Path path){return new ImportResult(true,path,"");}public static ImportResult failure(String error){return new ImportResult(false,null,error==null?"Unknown import error":error);} }
}
