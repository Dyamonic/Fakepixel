package me.outerspace.thepit.setup;

import me.outerspace.thepit.ThePitPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PitYamlSetupStore {
    private final ThePitPlugin plugin;
    private final File file;

    public PitYamlSetupStore(ThePitPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public PitYamlSetupStore(File file) {
        this.plugin = null;
        this.file = file;
    }

    public boolean available() {
        return file.exists() && file.isFile();
    }

    public File file() { return file; }

    public synchronized PitSetupSnapshot load() throws IOException {
        if (plugin != null) {
            plugin.getLogger().info("Loading The Pit arenas from " + file.getAbsolutePath() + " exists=" + file.isFile());
        }
        PitSetupSnapshot snapshot = read(file);
        if (plugin != null) {
            plugin.getLogger().info("Parsed arenas.yml successfully arenas=" + snapshot.arenas.size() + " npc-definitions=" + snapshot.npcs.size());
            for (PitSetupSnapshot.Arena arena : snapshot.arenas) {
                long count = snapshot.npcs.stream().filter(value -> arena.id.equalsIgnoreCase(value.arenaId)).count();
                plugin.getLogger().info("Arena NPC definitions arena=" + arena.id + " template=" + arena.worldName + " count=" + count);
            }
        }
        return snapshot;
    }

    public synchronized void migrateLegacyGlobalNpcs() throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        try { configuration.load(file); }
        catch (Exception exception) { throw new IOException("Could not parse " + file.getAbsolutePath() + ": " + exception.getMessage(), exception); }
        ConfigurationSection legacy = configuration.getConfigurationSection("npcs");
        if (legacy == null) return;
        if (legacy.getKeys(false).isEmpty()) return;
        ConfigurationSection arenas = configuration.getConfigurationSection("arenas");
        if (arenas == null) throw new IOException("Legacy NPC migration requires an arenas section");
        String original = configuration.saveToString();
        int migrated = 0;
        List<String> destinations = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (String rawId : new ArrayList<>(legacy.getKeys(false))) {
            String base = "npcs." + rawId;
            String arena = normalize(configuration.getString(base + ".arena", ""));
            if (arena.isEmpty()) arena = arenaForWorld(configuration, arenas, configuration.getString(base + ".world", configuration.getString(base + ".location.world", "")));
            if (arena.isEmpty() || !arenas.isConfigurationSection(arena)) {
                failures.add(base + " has no unique owning arena");
                continue;
            }
            String id = normalize(rawId).replaceFirst("[0-9]+$", "");
            if (id.isEmpty()) {
                failures.add(base + " has no valid NPC ID");
                continue;
            }
            String target = "arenas." + arena + ".npcs." + id;
            if (configuration.isConfigurationSection(target)) {
                configuration.set(base, null);
                destinations.add(target);
                continue;
            }
            PitSetupSnapshot.Npc value = readLegacyNpc(configuration, rawId, arena, id);
            if (value.location == null || !Double.isFinite(value.location.x) || !Double.isFinite(value.location.y)
                    || !Double.isFinite(value.location.z) || !Float.isFinite(value.location.yaw) || !Float.isFinite(value.location.pitch)) {
                failures.add(base + " has an invalid location");
                continue;
            }
            normalizeMigratedNpc(value);
            upsertNpc(configuration, value);
            configuration.set(base, null);
            destinations.add(target);
            migrated++;
        }
        if (!failures.isEmpty()) throw new IOException("Legacy NPC migration was not applied: " + String.join("; ", failures));
        configuration.set("npcs", null);
        if (original.equals(configuration.saveToString())) return;
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(Instant.now());
        File backup = new File(file.getParentFile(), "arenas.yml.legacy-npcs-" + stamp + ".bak");
        Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        try {
            save(configuration, file);
            YamlConfiguration verified = new YamlConfiguration();
            verified.load(file);
            if (verified.contains("npcs")) throw new IOException("saved YAML still contains the legacy root path npcs");
            read(file);
        } catch (Exception exception) {
            Files.copy(backup.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            throw new IOException("Legacy NPC migration failed and arenas.yml was restored from " + backup.getName() + ": " + exception.getMessage(), exception);
        }
        plugin.getLogger().info("Migrated " + migrated + " legacy NPCs from npcs to " + String.join(",", destinations) + " backup=" + backup.getName());
    }

    private String arenaForWorld(YamlConfiguration configuration, ConfigurationSection arenas, String world) {
        String normalizedWorld = normalize(world);
        if (normalizedWorld.isEmpty()) return "";
        String match = "";
        for (String rawArena : arenas.getKeys(false)) {
            String base = "arenas." + rawArena;
            String arenaWorld = normalize(configuration.getString(base + ".world", ""));
            String template = normalize(configuration.getString(base + ".world-template", arenaWorld));
            if (!normalizedWorld.equals(arenaWorld) && !normalizedWorld.equals(template)) continue;
            String candidate = normalize(rawArena);
            if (!match.isEmpty() && !match.equals(candidate)) return "";
            match = candidate;
        }
        return match;
    }

    private PitSetupSnapshot.Npc readLegacyNpc(YamlConfiguration configuration, String rawId, String arena, String id) {
        String base = "npcs." + rawId;
        PitSetupSnapshot.Npc value = new PitSetupSnapshot.Npc();
        value.stableId = id;
        value.arenaId = arena;
        value.worldName = configuration.getString(base + ".world", configuration.getString(base + ".location.world", ""));
        value.name = configuration.getString(base + ".display-name", configuration.getString(base + ".name", "&bThe Pit"));
        value.location = location(configuration, base + ".location", value.worldName);
        value.skinName = configuration.getString(base + ".skin-name", "");
        value.skinValue = configuration.getString(base + ".skin-value", "");
        value.skinSignature = configuration.getString(base + ".skin-signature", "");
        value.skinId = configuration.getString(base + ".skin-id", "pit_" + id);
        if (!value.skinValue.isEmpty() && !value.skinSignature.isEmpty()) {
            ConfigurationSection skins = configuration.getConfigurationSection("npc-skins");
            if (skins != null) {
                for (String skinId : skins.getKeys(false)) {
                    if (value.skinValue.equals(configuration.getString("npc-skins." + skinId + ".value", ""))
                            && value.skinSignature.equals(configuration.getString("npc-skins." + skinId + ".signature", ""))) {
                        value.skinId = normalize(skinId);
                        break;
                    }
                }
            }
        }
        value.command = configuration.getString(base + ".command", "");
        value.actionId = configuration.getString(base + ".action-id", "");
        value.executionMode = configuration.getString(base + ".execution-mode", "PLAYER");
        value.hologramId = configuration.getString(base + ".hologram-id", id);
        value.lookAtPlayers = configuration.getBoolean(base + ".look-at-players", false);
        value.enabled = configuration.getBoolean(base + ".enabled", true);
        return value;
    }

    public synchronized void importSnapshot(PitSetupSnapshot snapshot) throws IOException {
        importSnapshot(snapshot, true);
    }

    public synchronized void importSnapshot(PitSetupSnapshot snapshot, boolean replace) throws IOException {
        YamlConfiguration imported = new YamlConfiguration();
        writeSnapshot(imported, snapshot);
        if (replace || !file.exists()) {
            if (file.exists()) {
                YamlConfiguration current = new YamlConfiguration();
                try {
                    current.load(file);
                } catch (Exception exception) {
                    throw new IOException(exception);
                }
                preserveArenaFeatures(current, imported);
            }
            save(imported, file);
            return;
        }
        YamlConfiguration current = new YamlConfiguration();
        try {
            current.load(file);
        } catch (Exception exception) {
            throw new IOException(exception);
        }
        mergeMissing(current, imported);
        save(current, file);
    }

    public synchronized boolean upsertArenaLocation(String arena, String type, PitSetupSnapshot.Location location) {
        String path = switch (type) {
            case "SPAWN" -> "spawn";
            case "DEATH" -> "death";
            case "SPECTATOR" -> "spectator";
            case "VOID_RETURN" -> "void-return";
            default -> "";
        };
        if (path.isEmpty()) return false;
        return update(configuration -> writeLocation(configuration, "arenas." + arena + "." + path, location));
    }

    public synchronized boolean upsertRegionCorner(String arena, String region, String corner, PitSetupSnapshot.Location location) {
        String path = region.equals("SPAWN_PROTECTION") ? "spawn-protection" : "pvp-arena";
        return update(configuration -> writeLocation(configuration, "arenas." + arena + "." + path + "." + corner.toLowerCase(), location));
    }

    public synchronized boolean upsertLeaderboard(PitSetupSnapshot.Leaderboard value) {
        return update(configuration -> {
            String base = "leaderboards." + value.stableId;
            configuration.set(base + ".arena", value.arenaId);
            writeLocation(configuration, base + ".location", value.location);
            configuration.set(base + ".stat-type", value.statType);
            configuration.set(base + ".period", value.period);
        });
    }

    public synchronized boolean deleteLeaderboard(String id) { return delete("leaderboards." + id); }

    public synchronized boolean upsertNpc(PitSetupSnapshot.Npc value) {
        if (value == null || value.arenaId == null || value.arenaId.trim().isEmpty()) return false;
        return update(configuration -> {
            String base = "arenas." + normalize(value.arenaId) + ".npcs." + normalize(value.stableId);
            configuration.set(base + ".type", value.typeId);
            configuration.set(base + ".display-name", value.name);
            writeNpcLocation(configuration, base + ".location", value.location);
            configuration.set(base + ".skin-id", value.skinId);
            writeSkin(configuration,value);
            configuration.set(base + ".skin-name", null);
            configuration.set(base + ".skin-value", null);
            configuration.set(base + ".skin-signature", null);
            configuration.set(base + ".action-id", value.actionId);
            configuration.set(base + ".command", "command".equalsIgnoreCase(value.actionId) && value.command != null && !value.command.isEmpty() ? value.command : null);
            configuration.set(base + ".execution-mode", "command".equalsIgnoreCase(value.actionId) ? value.executionMode : null);
            configuration.set(base + ".hologram-id", value.hologramId);
            configuration.set(base + ".look-at-players", value.lookAtPlayers);
            configuration.set(base + ".enabled", value.enabled);
        });
    }

    public synchronized boolean deleteNpc(String id) {
        String normalized = normalize(id);
        return update(configuration -> {
            ConfigurationSection arenas = configuration.getConfigurationSection("arenas");
            if (arenas == null) return;
            for (String arena : arenas.getKeys(false)) configuration.set("arenas." + arena + ".npcs." + normalized, null);
        });
    }
    public synchronized boolean deleteNpc(String arena, String id) { return delete("arenas." + normalize(arena) + ".npcs." + normalize(id)); }

    public synchronized NpcMigrationResult migrateNpcs(List<PitSetupSnapshot.Npc> values, boolean overwrite, Map<String,String> worldMappings) {
        int migrated=0,skipped=0,duplicates=0,ambiguous=0,invalid=0;
        try {
            YamlConfiguration current = new YamlConfiguration();
            current.load(file);
            ConfigurationSection arenaRoot = current.getConfigurationSection("arenas");
            if (arenaRoot == null) return new NpcMigrationResult(0,0,0,0,values.size(),null,"Missing arenas section");
            Map<String,List<String>> worlds = new LinkedHashMap<>();
            for (String rawArena : arenaRoot.getKeys(false)) {
                String arena = normalize(rawArena);
                String world = current.getString("arenas." + rawArena + ".world", "");
                String template = current.getString("arenas." + rawArena + ".world-template", world);
                worlds.computeIfAbsent(normalize(world), ignored -> new ArrayList<>()).add(arena);
                worlds.computeIfAbsent(normalize(template), ignored -> new ArrayList<>()).add(arena);
            }
            for (PitSetupSnapshot.Npc value : values) {
                String arena = normalize(value.arenaId);
                if (!arenaRoot.isConfigurationSection(arena)) {
                    String mapped = worldMappings.get(normalize(value.worldName));
                    List<String> matches = mapped == null ? worlds.getOrDefault(normalize(value.worldName), java.util.Collections.emptyList()) : java.util.Collections.singletonList(normalize(mapped));
                    matches = new ArrayList<>(new java.util.LinkedHashSet<>(matches));
                    if (matches.size() != 1) { if (matches.isEmpty()) invalid++; else ambiguous++; continue; }
                    arena = matches.get(0);
                }
                normalizeMigratedNpc(value);
                String id = normalize(value.stableId).replaceFirst("[0-9]+$", "");
                if (id.isEmpty() || value.location == null || !Double.isFinite(value.location.x) || !Double.isFinite(value.location.y) || !Double.isFinite(value.location.z)) { invalid++; continue; }
                String base = "arenas." + arena + ".npcs." + id;
                if (current.isConfigurationSection(base) && !overwrite) { duplicates++; continue; }
                value.arenaId = arena;
                upsertNpc(current, value);
                migrated++;
            }
            if (migrated == 0) return new NpcMigrationResult(0,skipped,duplicates,ambiguous,invalid,null,"");
            String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(Instant.now());
            File backup = new File(file.getParentFile(), "arenas.yml.npc-migration-" + stamp + ".bak");
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            save(current, file);
            YamlConfiguration verified = new YamlConfiguration();
            verified.load(file);
            read(file);
            return new NpcMigrationResult(migrated,skipped,duplicates,ambiguous,invalid,backup.toPath(),"");
        } catch (Exception exception) {
            return new NpcMigrationResult(0,skipped,duplicates,ambiguous,invalid,null,exception.getMessage());
        }
    }

    public synchronized void exportSnapshot(File target, PitSetupSnapshot snapshot) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        writeSnapshot(configuration, snapshot);
        save(configuration, target);
    }

    public PitSetupSnapshot read(File source) throws IOException {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(source);
            PitSetupSnapshot snapshot = new PitSetupSnapshot();
            snapshot.schemaVersion = configuration.getInt("setup.schema-version", 1);
            snapshot.exportedAt = configuration.getString("setup.exported-at", "");
            ConfigurationSection arenas = configuration.getConfigurationSection("arenas");
            if (arenas == null) throw new IOException("Missing arenas section");
            for (String id : arenas.getKeys(false)) snapshot.arenas.add(readArena(configuration, id));
            readLeaderboards(configuration, snapshot);
            readNpcs(configuration, snapshot);
            return snapshot;
        } catch (Exception exception) {
            if (exception instanceof IOException) throw (IOException) exception;
            throw new IOException(exception);
        }
    }

    private PitSetupSnapshot.Arena readArena(YamlConfiguration configuration, String id) {
        String base = "arenas." + id;
        PitSetupSnapshot.Arena arena = new PitSetupSnapshot.Arena();
        arena.id=id;arena.enabled=configuration.getBoolean(base+".enabled",true);arena.displayName=configuration.getString(base+".display-name",id);arena.worldName=configuration.getString(base+".world",id);arena.arenaType=configuration.getString(base+".type","normal");
        arena.spawn=location(configuration,base+".spawn",arena.worldName);arena.death=location(configuration,base+".death",arena.worldName);arena.spectator=location(configuration,base+".spectator",arena.worldName);arena.voidReturn=location(configuration,base+".void-return",arena.worldName);
        arena.spawnProtection=region(configuration,base+".spawn-protection",arena.worldName);arena.pvpArena=region(configuration,base+".pvp-arena",arena.worldName);
        return arena;
    }

    private void readLeaderboards(YamlConfiguration configuration, PitSetupSnapshot snapshot) {
        ConfigurationSection root=configuration.getConfigurationSection("leaderboards");if(root==null)return;
        for(String id:root.getKeys(false)){String base="leaderboards."+id;PitSetupSnapshot.Leaderboard value=new PitSetupSnapshot.Leaderboard();value.stableId=id;value.arenaId=configuration.getString(base+".arena","");value.location=location(configuration,base+".location","");value.statType=configuration.getString(base+".stat-type","KILLS");value.period=configuration.getString(base+".period","LIFETIME");snapshot.leaderboards.add(value);}
    }

    private void readNpcs(YamlConfiguration configuration, PitSetupSnapshot snapshot) {
        ConfigurationSection arenas = configuration.getConfigurationSection("arenas");
        if (arenas == null) return;
        for (String arena : arenas.getKeys(false)) {
            ConfigurationSection root = configuration.getConfigurationSection("arenas." + arena + ".npcs");
            if (root == null) continue;
            for (String id : root.getKeys(false)) snapshot.npcs.add(readNpc(configuration, normalize(arena), id, "arenas." + arena + ".npcs." + id));
        }
    }

    private PitSetupSnapshot.Npc readNpc(YamlConfiguration configuration, String arena, String id, String base) {
        PitSetupSnapshot.Npc value = new PitSetupSnapshot.Npc();
        value.stableId=normalize(id);value.arenaId=arena;value.worldName="";value.typeId=configuration.getString(base+".type","GENERIC");value.name=configuration.getString(base+".display-name",configuration.getString(base+".name","&bThe Pit"));value.location=location(configuration,base+".location","");value.skinId=configuration.getString(base+".skin-id",value.stableId);String skin="npc-skins."+value.skinId;value.skinName=configuration.getString(skin+".name",configuration.getString(base+".skin-name",""));value.skinValue=configuration.getString(skin+".value",configuration.getString(base+".skin-value",""));value.skinSignature=configuration.getString(skin+".signature",configuration.getString(base+".skin-signature",""));value.actionId=configuration.getString(base+".action-id","command");value.command=configuration.getString(base+".command","");value.executionMode=configuration.getString(base+".execution-mode","PLAYER");value.hologramId=configuration.getString(base+".hologram-id",value.stableId);value.lookAtPlayers=configuration.getBoolean(base+".look-at-players",false);value.enabled=configuration.getBoolean(base+".enabled",true);
        return value;
    }

    private void writeSnapshot(YamlConfiguration configuration, PitSetupSnapshot snapshot) {
        configuration.set("setup.schema-version",snapshot.schemaVersion);configuration.set("setup.exported-at",snapshot.exportedAt);
        Map<String,PitSetupSnapshot.Arena> arenas=new LinkedHashMap<>();
        for(PitSetupSnapshot.Arena arena:snapshot.arenas){arenas.put(arena.id,arena);String base="arenas."+arena.id;configuration.set(base+".enabled",arena.enabled);configuration.set(base+".display-name",arena.displayName);configuration.set(base+".world",arena.worldName);configuration.set(base+".type",arena.arenaType);writeLocation(configuration,base+".spawn",arena.spawn);writeLocation(configuration,base+".death",arena.death);writeLocation(configuration,base+".spectator",arena.spectator);writeLocation(configuration,base+".void-return",arena.voidReturn);writeRegion(configuration,base+".spawn-protection",arena.spawnProtection);writeRegion(configuration,base+".pvp-arena",arena.pvpArena);}
        for(PitSetupSnapshot.Leaderboard value:snapshot.leaderboards)upsertLeaderboard(configuration,value);
        for(PitSetupSnapshot.Npc value:snapshot.npcs)upsertNpc(configuration,value);
    }

    private void upsertLeaderboard(YamlConfiguration c,PitSetupSnapshot.Leaderboard v){String b="leaderboards."+v.stableId;c.set(b+".arena",v.arenaId);writeLocation(c,b+".location",v.location);c.set(b+".stat-type",v.statType);c.set(b+".period",v.period);}
    private void upsertNpc(YamlConfiguration c,PitSetupSnapshot.Npc v){String b="arenas."+normalize(v.arenaId)+".npcs."+normalize(v.stableId);c.set(b+".type",v.typeId);c.set(b+".display-name",v.name);writeNpcLocation(c,b+".location",v.location);c.set(b+".skin-id",v.skinId);writeSkin(c,v);c.set(b+".action-id",v.actionId);c.set(b+".command","command".equalsIgnoreCase(v.actionId)&&v.command!=null&&!v.command.isEmpty()?v.command:null);c.set(b+".execution-mode","command".equalsIgnoreCase(v.actionId)?v.executionMode:null);c.set(b+".hologram-id",v.hologramId);c.set(b+".look-at-players",v.lookAtPlayers);c.set(b+".enabled",v.enabled);}
    private void writeSkin(YamlConfiguration c,PitSetupSnapshot.Npc v){if(v.skinId==null||v.skinId.isEmpty()||v.skinValue==null||v.skinValue.isEmpty()||v.skinSignature==null||v.skinSignature.isEmpty())return;String b="npc-skins."+normalize(v.skinId);c.set(b+".name",v.skinName==null?"":v.skinName);c.set(b+".value",v.skinValue);c.set(b+".signature",v.skinSignature);}
    private void normalizeMigratedNpc(PitSetupSnapshot.Npc value){String id=normalize(value.stableId).replaceFirst("[0-9]+$","");String command=normalize(value.command).replaceFirst("^bffa_","");value.stableId=id;value.hologramId=id;switch(command.isEmpty()?id:command){case "stats"->{value.typeId="STATS";value.actionId="open_stats";}case "prestige"->{value.typeId="PRESTIGE";value.actionId="open_prestige";}case "shop"->{value.typeId="SHOP";value.actionId="open_shop";}case "perks","upgrades"->{value.typeId="PERKS";value.actionId="open_perks";}case "quests","contracts"->{value.typeId="QUESTS";value.actionId="open_quests";}default->{value.typeId="GENERIC";value.actionId="command";}}if(value.skinId==null||value.skinId.isEmpty())value.skinId="pit_"+id;if(!"command".equals(value.actionId))value.command="";}
    private PitSetupSnapshot.Location location(YamlConfiguration c,String path,String world){if(!c.isConfigurationSection(path))return null;return new PitSetupSnapshot.Location(c.getString(path+".world",world),c.getDouble(path+".x"),c.getDouble(path+".y"),c.getDouble(path+".z"),(float)c.getDouble(path+".yaw"),(float)c.getDouble(path+".pitch"));}
    private PitSetupSnapshot.Region region(YamlConfiguration c,String path,String world){PitSetupSnapshot.Location one=location(c,path+".pos1",world),two=location(c,path+".pos2",world);if(one==null&&two==null)return null;PitSetupSnapshot.Region value=new PitSetupSnapshot.Region();value.pos1=one;value.pos2=two;return value;}
    private void writeRegion(YamlConfiguration c,String path,PitSetupSnapshot.Region value){if(value==null)return;writeLocation(c,path+".pos1",value.pos1);writeLocation(c,path+".pos2",value.pos2);}
    private void writeLocation(YamlConfiguration c,String path,PitSetupSnapshot.Location value){if(value==null)return;c.set(path+".world",value.worldName);c.set(path+".x",value.x);c.set(path+".y",value.y);c.set(path+".z",value.z);c.set(path+".yaw",value.yaw);c.set(path+".pitch",value.pitch);}
    private void writeNpcLocation(YamlConfiguration c,String path,PitSetupSnapshot.Location value){if(value==null)return;c.set(path+".x",value.x);c.set(path+".y",value.y);c.set(path+".z",value.z);c.set(path+".yaw",value.yaw);c.set(path+".pitch",value.pitch);}
    private String normalize(String value){return value==null?"":value.trim().toLowerCase(java.util.Locale.ENGLISH).replaceAll("[^a-z0-9_-]","_");}
    private void mergeMissing(ConfigurationSection target,ConfigurationSection source){for(String key:source.getKeys(false)){Object value=source.get(key);if(value instanceof ConfigurationSection section){ConfigurationSection child=target.getConfigurationSection(key);if(child==null)child=target.createSection(key);mergeMissing(child,section);}else if(!target.contains(key))target.set(key,value);}}

    private boolean delete(String path) { return update(configuration -> configuration.set(path,null)); }
    private void preserveArenaFeatures(YamlConfiguration source, YamlConfiguration target) {
        ConfigurationSection arenas = source.getConfigurationSection("arenas");
        if (arenas == null) return;
        for (String arena : arenas.getKeys(false)) {
            for (String feature : new String[]{"launch-pads", "coin-spawns", "care-packages", "npcs"}) {
                ConfigurationSection section = source.getConfigurationSection("arenas." + arena + "." + feature);
                if (section == null || !target.isConfigurationSection("arenas." + arena)) continue;
                ConfigurationSection destination = target.createSection("arenas." + arena + "." + feature);
                copySection(section, destination);
            }
        }
    }
    private void copySection(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            ConfigurationSection child = source.getConfigurationSection(key);
            if (child == null) target.set(key, source.get(key));
            else copySection(child, target.createSection(key));
        }
    }
    private boolean update(Change change){try{YamlConfiguration configuration=new YamlConfiguration();configuration.load(file);change.apply(configuration);save(configuration,file);return true;}catch(Exception exception){if(plugin!=null)plugin.getLogger().warning("Could not write arenas.yml setup data: "+exception.getMessage());return false;}}
    private void save(YamlConfiguration configuration,File target)throws IOException{File parent=target.getParentFile();if(parent!=null)parent.mkdirs();File temporary=new File(parent,target.getName()+".tmp");configuration.save(temporary);try{YamlConfiguration validation=new YamlConfiguration();validation.load(temporary);}catch(Exception exception){Files.deleteIfExists(temporary.toPath());throw new IOException("Temporary YAML validation failed: "+exception.getMessage(),exception);}try{Files.move(temporary.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ignored){Files.move(temporary.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);}}
    private interface Change { void apply(YamlConfiguration configuration); }
    public record NpcMigrationResult(int migrated,int skipped,int duplicates,int ambiguous,int invalid,Path backup,String error) {}
}
