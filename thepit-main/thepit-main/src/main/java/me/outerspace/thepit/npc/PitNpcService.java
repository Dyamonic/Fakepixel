package me.outerspace.thepit.npc;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.setup.PitSetupSnapshot;
import me.outerspace.thepit.setup.PitYamlSetupStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PitNpcService implements Listener {
    public enum MutationResult { SUCCESS, NOT_FOUND, ALREADY_EXISTS, STORAGE_UNAVAILABLE }

    private static final Set<String> NPC_TYPES = Set.of("GENERIC","SHOP","PERKS","STATS","PRESTIGE","QUESTS");
    private static final Set<String> NPC_ACTIONS = Set.of("command","open_shop","open_perks","open_stats","open_prestige","open_quests");
    private final ThePitPlugin plugin;
    private final V1_8_R3NpcHologramTransport hologramTransport;
    private final List<HousingNpc> npcs = new ArrayList<>();
    private final Map<UUID, Long> clickCooldowns = new HashMap<>();
    private final Map<UUID, UUID> selectedNpcs = new HashMap<>();
    private final Map<UUID, Consumer<Player>> clickHandlers = new HashMap<>();
    private final Map<UUID,String> stableIds=new HashMap<>();
    private final Map<UUID, RuntimeAction> runtimeActions = new HashMap<>();
    private final Map<UUID, TransientHologram> transientHolograms = new HashMap<>();
    private final Map<String,String> migrationWorldMappings = new HashMap<>();
    private int lookTaskId = -1;
    private volatile String deferredSpawnReason = "not loaded";
    private volatile String lastNpcFailure = "none";
    private volatile int lastValidDefinitions;
    private volatile int lastRejectedDefinitions;

    public PitNpcService(ThePitPlugin plugin) {
        this.plugin = plugin;
        V1_8_R3NpcHologramTransport created = null;
        try {
            created = new V1_8_R3NpcHologramTransport();
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize v1_8_R3 NPC hologram packets on " + Bukkit.getVersion(), exception);
        }
        hologramTransport = created;
        plugin.getLogger().info("The Pit packet NPC engine initialized=" + (hologramTransport != null));
        startLookTask();
    }

    public void load() {
        startLookTask();
        clearNameplates();
        npcs.clear();
        clickHandlers.clear();
        stableIds.clear();
        runtimeActions.clear();
        loadConfigured();
    }

    private void loadConfigured() {
        String activeArena = plugin.getArenaManager().getCurrentArena();
        World world = plugin.getArenaManager().getCurrentWorld();
        int configured = 0;
        for (PitSetupSnapshot.Npc definition : plugin.getSetupService().snapshot().npcs) if (definition.arenaId != null && definition.arenaId.equalsIgnoreCase(activeArena)) configured++;
        if (world == null) {
            deferredSpawnReason = "runtime world is not registered or loaded";
            plugin.getLogger().warning("Deferred arena NPC spawning: arena=" + activeArena + ", template=" + arenaTemplate(activeArena) + ", npc-count=" + configured + ", reason=" + deferredSpawnReason);
            return;
        }
        deferredSpawnReason = "none";
        lastValidDefinitions = 0;
        lastRejectedDefinitions = 0;
        plugin.getLogger().info("Preparing arena packet NPCs arena=" + activeArena + " template=" + arenaTemplate(activeArena) + " runtime-world=" + world.getName() + " runtime-world-uuid=" + world.getUID() + " definitions=" + configured);
        for (PitSetupSnapshot.Npc definition : plugin.getSetupService().snapshot().npcs) {
            if (definition.arenaId == null || !definition.arenaId.equalsIgnoreCase(activeArena)) continue;
            if (!definition.enabled) {
                lastRejectedDefinitions++;
                plugin.getLogger().info("Skipping disabled NPC at arenas." + activeArena + ".npcs." + definition.stableId);
                continue;
            }
            String invalid = validateDefinition(definition);
            if (invalid != null) {
                plugin.getLogger().warning("Skipping invalid NPC at arenas." + activeArena + ".npcs." + definition.stableId + ": " + invalid);
                lastRejectedDefinitions++;
                lastNpcFailure = "arenas." + activeArena + ".npcs." + definition.stableId + ": " + invalid;
                continue;
            }
            registerNpc(new ArenaNpcDefinition(definition.arenaId, definition.stableId, definition.typeId,
                    definition.location.x, definition.location.y, definition.location.z, definition.location.yaw,
                    definition.location.pitch, definition.name, definition.skinId, definition.skinName,
                    definition.skinValue, definition.skinSignature, definition.actionId, definition.command,
                    definition.executionMode, definition.hologramId, definition.lookAtPlayers, definition.enabled), world);
            lastValidDefinitions++;
        }
        plugin.getLogger().info("Created packet NPC controllers arena=" + activeArena + " valid=" + lastValidDefinitions + " rejected=" + lastRejectedDefinitions + " controllers=" + npcs.size());
    }

    public void registerNpc(ArenaNpcDefinition definition, World world) {
            Location location = new Location(world, definition.x(), definition.y(), definition.z(), definition.yaw(), definition.pitch());
            String runtimeKey = definition.arenaId() + ":" + world.getUID() + ":" + definition.npcId();
            UUID runtimeUuid=stableUuid(runtimeKey);
            String actionId = normalizeStableId(definition.actionId());
            String command = "command".equals(actionId) ? normalizeCommand(definition.command()) : commandForAction(actionId);
            HousingNpc npc = new HousingNpc(
                    configuredEntityId(runtimeKey), runtimeUuid, npcName(definition.displayName()), location,
                    definition.skinValue(), definition.skinSignature(), command, definition.lookAtPlayers(), true
            );
            npcs.add(npc);
            stableIds.put(runtimeUuid,definition.npcId());
            runtimeActions.put(runtimeUuid, new RuntimeAction(definition.arenaId(), definition.npcId(), actionId, command, normalizeExecutionMode(definition.executionMode())));
            debug("loaded path=arenas." + definition.arenaId() + ".npcs." + definition.npcId() + " action=" + actionId + " mode=" + normalizeExecutionMode(definition.executionMode()) + " command=" + command);
            String skinName = definition.skinName();
            if ((npc.skinValue.isEmpty() || npc.skinSignature.isEmpty()) && skinName != null && !skinName.trim().isEmpty()) {
                fetchSkinLater(npc, skinName.trim());
            }
    }

    private int configuredEntityId(String key) {
        return PitPacketEntityEngine.shared().allocate().entityId();
    }

    private UUID configuredUuid(String key, String configured) {
        if (configured != null && !configured.trim().isEmpty()) {
            try { return UUID.fromString(configured.trim()); }
            catch (IllegalArgumentException ex) { plugin.getLogger().warning("Invalid UUID for The Pit NPC '" + key + "'; using a stable generated UUID."); }
        }
        return UUID.nameUUIDFromBytes(("buildffa-npc:" + key).getBytes(StandardCharsets.UTF_8));
    }

    public void spawnAll() {
        for (HousingNpc npc : npcs) ensureNameplate(npc);
        for (Player player : Bukkit.getOnlinePlayers()) {
            inject(player);
            spawnVisible(player);
        }
    }

    public boolean setMigrationWorldMapping(String databaseWorld, String arenaId) {
        String world = normalizeStableId(databaseWorld);
        String arena = normalizeStableId(arenaId);
        if (world.isEmpty() || plugin.getArenaManager().getArena(arena).isEmpty()) return false;
        migrationWorldMappings.put(world, arena);
        return true;
    }

    public void migrateFromDatabase(Player sender, boolean overwrite) {
        if (!plugin.getSetupService().repository().migrationDatabaseAvailable()) {
            sender.sendMessage(plugin.color("&cMySQL is not configured; there are no database NPC rows to migrate."));
            return;
        }
        sender.sendMessage(plugin.color("&eReading legacy The Pit NPC rows from MySQL..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<PitSetupSnapshot.Npc> rows = plugin.getSetupService().repository().loadDatabaseNpcsForMigration();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    PitYamlSetupStore.NpcMigrationResult result = plugin.getSetupService().repository().yaml().migrateNpcs(rows, overwrite, new HashMap<>(migrationWorldMappings));
                    if (result.error() != null && !result.error().isEmpty()) {
                        sender.sendMessage(plugin.color("&cNPC migration failed without replacing arenas.yml: &f" + result.error()));
                        return;
                    }
                    plugin.getSetupService().reloadFromStorage();
                    reload();
                    sender.sendMessage(plugin.color("&aNPC migration complete. &fMigrated: &a" + result.migrated() + " &7Skipped: &f" + result.skipped() + " &7Duplicates: &f" + result.duplicates() + " &7Ambiguous: &f" + result.ambiguous() + " &7Invalid: &f" + result.invalid()));
                    if (result.backup() != null) sender.sendMessage(plugin.color("&7Backup: &f" + result.backup().toAbsolutePath()));
                });
            } catch (Exception exception) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(plugin.color("&cCould not read legacy NPC rows: &f" + exception.getMessage())));
            }
        });
    }

    private String arenaTemplate(String arenaId) {
        return arenaId == null ? "unknown" : plugin.getArenaManager().yaml().worldTemplate(arenaId);
    }

    private String validateDefinition(PitSetupSnapshot.Npc definition) {
        if (definition.arenaId == null || plugin.getArenaManager().getArena(definition.arenaId).isEmpty()) return "arena is missing or unknown: " + definition.arenaId;
        if (normalizeStableId(definition.stableId).isEmpty()) return "NPC ID is empty";
        if (definition.location == null) return "location is missing";
        if (!Double.isFinite(definition.location.x) || !Double.isFinite(definition.location.y) || !Double.isFinite(definition.location.z)) return "coordinates must be finite";
        if (!Float.isFinite(definition.location.yaw) || !Float.isFinite(definition.location.pitch)) return "yaw and pitch must be finite";
        if (definition.typeId == null || !NPC_TYPES.contains(definition.typeId.trim().toUpperCase(Locale.ENGLISH))) return "type is not registered: " + definition.typeId;
        if (definition.skinId == null || definition.skinId.trim().isEmpty()) return "skin-id is missing";
        if ((definition.skinValue == null || definition.skinValue.isEmpty() || definition.skinSignature == null || definition.skinSignature.isEmpty()) && (definition.skinName == null || definition.skinName.isEmpty())) return "skin-id is not registered: " + definition.skinId;
        if (definition.actionId == null || !NPC_ACTIONS.contains(definition.actionId.trim().toLowerCase(Locale.ENGLISH))) return "action-id is not registered: " + definition.actionId;
        if ("command".equalsIgnoreCase(definition.actionId) && !isExecutionMode(definition.executionMode)) return "execution-mode must be PLAYER or CONSOLE: " + definition.executionMode;
        if (definition.hologramId == null || definition.hologramId.trim().isEmpty()) return "hologram-id is missing";
        return null;
    }

    private void spawnNpc(HousingNpc npc) {
        ensureNameplate(npc);
        for (Player player : Bukkit.getOnlinePlayers()) {
            inject(player);
            spawnVisible(player, npc);
        }
        plugin.getLogger().info("Spawned packet NPC for eligible viewers npc=" + stableIds.getOrDefault(npc.uuid, npc.uuid.toString()) + " arena=" + plugin.getArenaManager().getCurrentArena() + " viewer-links=" + npc.viewers.size());
    }

    public UUID spawnManagedNpc(String name, Location location, String skinName, boolean lookAtPlayers, Consumer<Player> clickHandler) {
        if (location == null || location.getWorld() == null) return null;
        HousingNpc npc = new HousingNpc(
                PitPacketEntityEngine.shared().allocate().entityId(),
                UUID.randomUUID(),
                npcName(name),
                location.clone(),
                "",
                "",
                "",
                lookAtPlayers,
                false
        );
        npcs.add(npc);
        if (clickHandler != null) clickHandlers.put(npc.uuid, clickHandler);
        if (skinName != null && !skinName.trim().isEmpty()) {
            fetchSkinLater(npc, skinName.trim());
        }
        spawnAll();
        return npc.uuid;
    }

    public void removeManagedNpc(UUID uuid) {
        HousingNpc npc = uuid == null ? null : findByUuid(uuid);
        if (npc == null) return;
        destroy(npc);
        npcs.remove(npc);
        clickHandlers.remove(uuid);
        selectedNpcs.values().removeIf(selected -> selected.equals(uuid));
    }



    public void create(Player creator) {
        create(creator, UUID.randomUUID().toString(), plugin.registry().npcs().getString("npcs.default-name", "The Pit"));
    }

    public void create(Player creator, String name) {
        create(creator, UUID.randomUUID().toString(), name);
    }

    public MutationResult create(Player creator, String stableId, String displayName) {
        String id = normalizeStableId(stableId);
        if (id.isEmpty() || findByStableId(id) != null || storedDefinition(id) != null) return MutationResult.ALREADY_EXISTS;
        SkinData skin = readSkin(creator);
        Location location = creator.getLocation().clone();
        HousingNpc npc = new HousingNpc(
                configuredEntityId(id),
                stableUuid(id),
                npcName(displayName),
                location,
                skin.value,
                skin.signature,
                "",
                false,
                true
        );
        PitSetupSnapshot.Npc value = snapshot(npc, location, id);
        value.skinName = creator.getName();
        if (!plugin.getSetupService().repository().upsertNpc(value)) return MutationResult.STORAGE_UNAVAILABLE;
        plugin.getSetupService().reloadFromDatabase();
        npcs.add(npc);
        stableIds.put(npc.uuid, id);
        spawnNpc(npc);
        return MutationResult.SUCCESS;
    }

    public MutationResult create(Player creator, String arenaId, String stableId, String typeId) {
        String arena = normalizeStableId(arenaId), id = normalizeStableId(stableId), type = typeId == null ? "" : typeId.trim().toUpperCase(Locale.ENGLISH);
        if (plugin.getArenaManager().getArena(arena).isEmpty() || id.isEmpty() || !NPC_TYPES.contains(type)) return MutationResult.NOT_FOUND;
        if (!isOwningArenaWorld(creator, arena)) return MutationResult.NOT_FOUND;
        if (storedDefinition(arena,id) != null) return MutationResult.ALREADY_EXISTS;
        PitSetupSnapshot.Npc value = new PitSetupSnapshot.Npc();
        value.arenaId=arena;value.stableId=id;value.typeId=type;value.name="&b&l"+id.replace('_',' ').toUpperCase(Locale.ENGLISH);value.location=relative(creator.getLocation());value.skinId=id;value.actionId="command";value.hologramId=id;value.enabled=true;
        return saveDefinition(value);
    }

    public MutationResult remove(String arenaId, String stableId) {
        String arena=normalizeStableId(arenaId),id=normalizeStableId(stableId);
        if (storedDefinition(arena,id)==null) return MutationResult.NOT_FOUND;
        if (!plugin.getSetupService().repository().deleteNpc(arena,id)) return MutationResult.STORAGE_UNAVAILABLE;
        plugin.getSetupService().reloadFromStorage();refreshDefinition(arena,id);return MutationResult.SUCCESS;
    }

    public MutationResult setLocation(Player player,String arenaId,String stableId) {
        String arena=normalizeStableId(arenaId),id=normalizeStableId(stableId);PitSetupSnapshot.Npc value=storedDefinition(arena,id);
        if(value==null||!isOwningArenaWorld(player,arena))return MutationResult.NOT_FOUND;value=copy(value);value.location=relative(player.getLocation());return saveDefinition(value);
    }

    public MutationResult setSkinId(String arenaId,String stableId,String skinId) {
        String arena=normalizeStableId(arenaId),id=normalizeStableId(stableId),skin=normalizeStableId(skinId);PitSetupSnapshot.Npc value=storedDefinition(arena,id);
        if(value==null||skin.isEmpty())return MutationResult.NOT_FOUND;value=copy(value);value.skinId=skin;
        for(PitSetupSnapshot.Npc candidate:plugin.getSetupService().snapshot().npcs)if(skin.equalsIgnoreCase(candidate.skinId)){value.skinName=candidate.skinName;value.skinValue=candidate.skinValue;value.skinSignature=candidate.skinSignature;break;}
        return saveDefinition(value);
    }

    public MutationResult setActionId(String arenaId,String stableId,String actionId) {
        String arena=normalizeStableId(arenaId),id=normalizeStableId(stableId),action=normalizeStableId(actionId);PitSetupSnapshot.Npc value=storedDefinition(arena,id);
        if(value==null||!NPC_ACTIONS.contains(action))return MutationResult.NOT_FOUND;value=copy(value);value.actionId=action;String command=commandForAction(action);if(!command.isEmpty())value.command=command;return saveDefinition(value);
    }

    public MutationResult setCommand(String arenaId, String stableId, String executionMode, String command) {
        String arena = normalizeStableId(arenaId), id = normalizeStableId(stableId), mode = normalizeExecutionMode(executionMode), normalized = normalizeCommand(command);
        PitSetupSnapshot.Npc value = storedDefinition(arena, id);
        if (value == null || normalized.isEmpty() || !isExecutionMode(mode)) return MutationResult.NOT_FOUND;
        value = copy(value);
        value.actionId = "command";
        value.command = normalized;
        value.executionMode = mode;
        MutationResult result = saveDefinition(value);
        debug("command-save path=arenas." + arena + ".npcs." + id + " mode=" + mode + " command=" + normalized + " result=" + result);
        return result;
    }

    public MutationResult setHologramId(String arenaId,String stableId,String hologramId) {
        String arena=normalizeStableId(arenaId),id=normalizeStableId(stableId),hologram=normalizeStableId(hologramId);PitSetupSnapshot.Npc value=storedDefinition(arena,id);
        if(value==null||hologram.isEmpty())return MutationResult.NOT_FOUND;value=copy(value);value.hologramId=hologram;return saveDefinition(value);
    }

    public MutationResult setEnabled(String arenaId,String stableId,boolean enabled) {
        String arena=normalizeStableId(arenaId),id=normalizeStableId(stableId);PitSetupSnapshot.Npc value=storedDefinition(arena,id);
        if(value==null)return MutationResult.NOT_FOUND;value=copy(value);value.enabled=enabled;return saveDefinition(value);
    }

    public List<String> list(String arenaId) {
        String arena=normalizeStableId(arenaId);List<String> lines=new ArrayList<>();
        for(PitSetupSnapshot.Npc value:plugin.getSetupService().snapshot().npcs)if(arena.equals(value.arenaId))lines.add(CC.color("&7- &e"+value.stableId+" &8[&f"+value.typeId+"&8] "+(value.enabled?"&aenabled":"&cdisabled")));
        if(lines.isEmpty())lines.add(CC.color("&7No NPC definitions exist for arena &f"+arena+"&7."));return lines;
    }

    public List<String> validate(String arenaId) {
        String arena=normalizeStableId(arenaId);List<String> lines=new ArrayList<>();int valid=0,invalid=0;
        if(plugin.getArenaManager().getArena(arena).isEmpty())return Collections.singletonList(CC.color("&cUnknown arena ID: &f"+arena));
        for(PitSetupSnapshot.Npc value:plugin.getSetupService().snapshot().npcs)if(arena.equals(value.arenaId)){String failure=validateDefinition(value);if(failure==null)valid++;else{invalid++;lines.add(CC.color("&carenas."+arena+".npcs."+value.stableId+": &f"+failure));}}
        lines.add(0,CC.color("&aNPC validation for &f"+arena+"&a: &f"+valid+" valid&a, &f"+invalid+" invalid&a."));return lines;
    }

    public List<String> status(String arenaId) {
        String arena = normalizeStableId(arenaId == null || arenaId.isEmpty() ? plugin.getArenaManager().getCurrentArena() : arenaId);
        int found = 0, valid = 0, rejected = 0;
        for (PitSetupSnapshot.Npc value : plugin.getSetupService().snapshot().npcs) {
            if (!arena.equals(value.arenaId)) continue;
            found++;
            if (validateDefinition(value) == null) valid++; else rejected++;
        }
        World world = arena.equals(plugin.getArenaManager().getCurrentArena()) ? plugin.getArenaManager().getCurrentWorld() : null;
        List<String> lines = new ArrayList<>();
        lines.add(CC.color("&6The Pit NPC status for &f" + arena));
        lines.add(CC.color("&7arenas.yml: &f" + plugin.getSetupService().repository().yaml().file().getAbsolutePath()));
        lines.add(CC.color("&7Arena loaded: &f" + plugin.getArenaManager().getArena(arena).isPresent()));
        lines.add(CC.color("&7Runtime world: &f" + (world == null ? "not ready" : world.getName() + " / " + world.getUID())));
        lines.add(CC.color("&7Definitions: &f" + found + " &7valid: &a" + valid + " &7rejected: &c" + rejected));
        lines.add(CC.color("&7Packet controllers: &f" + npcs.size() + " &7viewer links: &f" + viewerLinkCount()));
        lines.add(CC.color("&7Deferred reason: &f" + deferredSpawnReason));
        lines.add(CC.color("&7Last failure: &f" + lastNpcFailure));
        return lines;
    }

    public boolean reload(String arenaId) {
        String arena=normalizeStableId(arenaId);if(plugin.getArenaManager().getArena(arena).isEmpty()||!plugin.getSetupService().reloadFromStorage())return false;
        if(arena.equals(plugin.getArenaManager().getCurrentArena()))reload();return true;
    }

    private MutationResult saveDefinition(PitSetupSnapshot.Npc value){if(!plugin.getSetupService().repository().upsertNpc(value))return MutationResult.STORAGE_UNAVAILABLE;plugin.getSetupService().reloadFromStorage();refreshDefinition(value.arenaId,value.stableId);return MutationResult.SUCCESS;}
    private void refreshDefinition(String arena,String id){HousingNpc old=findByStableId(id);if(old!=null){destroy(old);npcs.remove(old);stableIds.remove(old.uuid);runtimeActions.remove(old.uuid);}if(!arena.equals(plugin.getArenaManager().getCurrentArena()))return;PitSetupSnapshot.Npc value=storedDefinition(arena,id);World world=plugin.getArenaManager().getCurrentWorld();if(value==null||!value.enabled||world==null||validateDefinition(value)!=null)return;registerNpc(toDefinition(value),world);HousingNpc added=findByStableId(id);if(added!=null)spawnNpc(added);}
    private ArenaNpcDefinition toDefinition(PitSetupSnapshot.Npc value){return new ArenaNpcDefinition(value.arenaId,value.stableId,value.typeId,value.location.x,value.location.y,value.location.z,value.location.yaw,value.location.pitch,value.name,value.skinId,value.skinName,value.skinValue,value.skinSignature,value.actionId,value.command,value.executionMode,value.hologramId,value.lookAtPlayers,value.enabled);}
    private boolean isOwningArenaWorld(Player player,String arena){return arena.equals(plugin.getArenaManager().getCurrentArena())&&plugin.getArenaManager().getCurrentWorld()==player.getWorld();}
    private PitSetupSnapshot.Location relative(Location value){return new PitSetupSnapshot.Location("",value.getX(),value.getY(),value.getZ(),value.getYaw(),value.getPitch());}
    private String commandForAction(String action){return switch(action){case "open_shop"->"shop";case "open_perks"->"upgrades";case "open_stats"->"stats";case "open_prestige"->"prestige";case "open_quests"->"quests";default->"";};}

    public boolean removeNearest(Player player) {
        HousingNpc nearest = nearest(player);
        if (nearest == null) return false;
        return removeNpc(nearest);
    }

    public boolean renameNearest(Player player, String name) {
        HousingNpc npc = nearest(player);
        if (npc == null) return false;
        String old=npc.name;
        npc.name = npcName(name);
        if(!persist(npc)){npc.name=old;return false;}
        destroy(npc);
        npc.packetEntity = null;
        spawnAll();
        return true;
    }

    public boolean setCommandNearest(Player player, String command) {
        HousingNpc npc = nearest(player);
        if (npc == null) return false;
        String old=npc.command;npc.command = command == null ? "" : command.trim();
        if(!persist(npc)){npc.command=old;return false;}return true;
    }

    public void setSkinNearest(final Player player, final String skinName) {
        final HousingNpc npc = nearest(player);
        if (npc == null) {
            player.sendMessage(plugin.color("&cNo The Pit NPC found nearby."));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final SkinData skin = fetchSkin(skinName);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (skin.value.isEmpty() || skin.signature.isEmpty()) {
                    player.sendMessage(plugin.color("&cCould not find skin for &e" + skinName + "&c."));
                    return;
                }
                String oldValue=npc.skinValue,oldSignature=npc.skinSignature;
                npc.skinValue = skin.value;
                npc.skinSignature = skin.signature;
                if(!persist(npc)){npc.skinValue=oldValue;npc.skinSignature=oldSignature;player.sendMessage(plugin.color("&cThe Pit setup storage is unavailable. Nothing was changed."));return;}
                destroy(npc);
                npc.packetEntity = null;
                spawnAll();
                player.sendMessage(plugin.color("&aUpdated nearest The Pit NPC skin to &e" + skinName + "&a."));
            });
        });
    }

    public void select(Player player, String name) {
        HousingNpc npc = findByName(player, name);
        if (npc == null) {
            player.sendMessage(plugin.color("&cNo The Pit NPC named &e" + name + "&c found."));
            return;
        }
        selectedNpcs.put(player.getUniqueId(), npc.uuid);
        player.sendMessage(plugin.color("&aSelected The Pit NPC &e" + displayName(npc) + "&a."));
    }

    public void selectLookedAtOrNearest(Player player) {
        HousingNpc npc = lookedAt(player);
        if (npc == null) npc = nearest(player);
        if (npc == null) {
            player.sendMessage(plugin.color("&cNo The Pit NPC found nearby or in sight."));
            return;
        }
        selectedNpcs.put(player.getUniqueId(), npc.uuid);
        player.sendMessage(plugin.color("&aSelected The Pit NPC &e" + displayName(npc) + "&a."));
    }

    public boolean remove(Player player, String name) {
        HousingNpc npc = findByName(player, name);
        return npc != null && removeNpc(npc);
    }

    public boolean removeSelected(Player player) {
        HousingNpc npc = selectedOrLookedAtOrNearest(player);
        return npc != null && removeNpc(npc);
    }

    public boolean renameSelected(Player player, String name) {
        HousingNpc npc = selectedOrLookedAtOrNearest(player);
        if (npc == null) return false;
        String old=npc.name;
        npc.name = npcName(name);
        if(!persist(npc)){npc.name=old;return false;}
        destroy(npc);
        npc.packetEntity = null;
        spawnAll();
        return true;
    }

    public boolean setCommandSelected(Player player, String command) {
        HousingNpc npc = selectedOnly(player);
        if (npc == null) return false;
        String id = stableIds.get(npc.uuid);
        return setCommand(plugin.getArenaManager().getCurrentArena(), id, "PLAYER", command) == MutationResult.SUCCESS;
    }

    public void setSkinSelected(final Player player, final String skinName) {
        final HousingNpc npc = selectedOrLookedAtOrNearest(player);
        if (npc == null) {
            player.sendMessage(plugin.color("&cNo The Pit NPC selected or nearby."));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final SkinData skin = fetchSkin(skinName);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (skin.value.isEmpty() || skin.signature.isEmpty()) {
                    player.sendMessage(plugin.color("&cCould not find skin for &e" + skinName + "&c."));
                    return;
                }
                String oldValue=npc.skinValue,oldSignature=npc.skinSignature;
                npc.skinValue = skin.value;
                npc.skinSignature = skin.signature;
                if(!persist(npc)){npc.skinValue=oldValue;npc.skinSignature=oldSignature;player.sendMessage(plugin.color("&cThe Pit setup storage is unavailable. Nothing was changed."));return;}
                destroy(npc);
                npc.packetEntity = null;
                spawnAll();
                player.sendMessage(plugin.color("&aUpdated selected The Pit NPC skin to &e" + skinName + "&a."));
            });
        });
    }

    public MutationResult renameById(String stableId, String displayName) {
        HousingNpc npc = findByStableId(stableId);
        if (npc == null) return MutationResult.NOT_FOUND;
        String renamed = npcName(displayName);
        PitSetupSnapshot.Npc value = snapshot(npc, npc.location, stableIds.get(npc.uuid));
        value.name = renamed;
        if (!plugin.getSetupService().repository().upsertNpc(value)) return MutationResult.STORAGE_UNAVAILABLE;
        plugin.getSetupService().reloadFromDatabase();
        destroy(npc);
        npc.name = renamed;
        npc.packetEntity = null;
        spawnNpc(npc);
        return MutationResult.SUCCESS;
    }

    public boolean select(Player player, String arenaId, String stableId) {
        PitSetupSnapshot.Npc definition = storedDefinition(normalizeStableId(arenaId), normalizeStableId(stableId));
        if (definition == null || !normalizeStableId(arenaId).equals(normalizeStableId(plugin.getArenaManager().getCurrentArena()))) return false;
        HousingNpc npc = findByStableId(definition.stableId);
        if (npc == null) return false;
        selectedNpcs.put(player.getUniqueId(), npc.uuid);
        player.sendMessage(plugin.color("&aSelected NPC &e" + definition.arenaId + "/" + definition.stableId + "&a."));
        return true;
    }

    public MutationResult setCommandSelected(Player player, String executionMode, String command) {
        HousingNpc npc = selectedOnly(player);
        if (npc == null) return MutationResult.NOT_FOUND;
        return setCommand(plugin.getArenaManager().getCurrentArena(), stableIds.get(npc.uuid), executionMode, command);
    }

    public MutationResult moveHere(String stableId, Location location) {
        if (location == null || location.getWorld() == null) return MutationResult.NOT_FOUND;
        HousingNpc npc = findByStableId(stableId);
        PitSetupSnapshot.Npc stored = storedDefinition(stableId);
        if (npc == null && stored == null) return MutationResult.NOT_FOUND;
        String id = npc == null ? stored.stableId : stableIds.get(npc.uuid);
        Location target = location.clone();
        PitSetupSnapshot.Npc value = npc == null ? copy(stored) : snapshot(npc, target, id);
        applyLocation(value, target);
        if (!plugin.getSetupService().repository().upsertNpc(value)) return MutationResult.STORAGE_UNAVAILABLE;
        plugin.getSetupService().reloadFromDatabase();
        if (npc != null) {
            destroy(npc);
            npc.location = target;
            npc.packetEntity = null;
            spawnNpc(npc);
            return MutationResult.SUCCESS;
        }
        HousingNpc loaded = new HousingNpc(configuredEntityId(id), stableUuid(id), npcName(value.name), target,
                value.skinValue, value.skinSignature, value.command, value.lookAtPlayers, true);
        npcs.add(loaded);
        stableIds.put(loaded.uuid, id);
        spawnNpc(loaded);
        return MutationResult.SUCCESS;
    }

    public void toggleLook(Player player) {
        HousingNpc npc = selectedOrLookedAtOrNearest(player);
        if (npc == null) {
            player.sendMessage(plugin.color("&cNo The Pit NPC selected or nearby."));
            return;
        }
        npc.lookAtPlayers = !npc.lookAtPlayers;
        if(!persist(npc)){npc.lookAtPlayers=!npc.lookAtPlayers;player.sendMessage(plugin.color("&cThe Pit setup storage is unavailable. Nothing was changed."));return;}
        player.sendMessage(plugin.color(npc.lookAtPlayers ? "&aNPC look mode enabled." : "&cNPC look mode disabled."));
    }

    public void list(Player player) {
        player.sendMessage(plugin.color("&6The Pit NPCs"));
        int count = 0;
        for (HousingNpc npc : npcs) {
            if (!npc.location.getWorld().equals(player.getWorld())) continue;
            count++;
            player.sendMessage(plugin.color("&7- &e" + displayName(npc) + " &8(" + Math.round(npc.location.distance(player.getLocation())) + " blocks)"));
        }
        if (count == 0) player.sendMessage(plugin.color("&7No NPCs in this world."));
    }

    public void debug(Player player, String stableId) {
        HousingNpc npc = findByStableId(stableId);
        if (npc == null) {
            player.sendMessage(plugin.color("&cNo NPC exists with the name &e" + stableId + "&c."));
            return;
        }
        ensureNameplate(npc);
        HologramPair pair = npc.holograms;
        player.sendMessage(plugin.color("&6The Pit NPC packet diagnostics"));
        player.sendMessage(plugin.color("&7Stable NPC ID: &f" + stableIds.getOrDefault(npc.uuid, stableId)));
        player.sendMessage(plugin.color("&7NPC body entity ID: &f" + npc.entityId));
        player.sendMessage(plugin.color("&7Top hologram entity ID: &f" + (pair == null ? "missing" : pair.top().entityId())));
        player.sendMessage(plugin.color("&7Bottom hologram entity ID: &f" + (pair == null ? "missing" : pair.bottom().entityId())));
        player.sendMessage(plugin.color("&7Rendered top text: &r" + (pair == null ? "missing" : pair.top().text())));
        player.sendMessage(plugin.color("&7Rendered bottom text: &r" + (pair == null ? "missing" : pair.bottom().text())));
        player.sendMessage(plugin.color("&7NPC location: &f" + formatLocation(npc.location)));
        player.sendMessage(plugin.color("&7Top hologram location: &f" + (pair == null ? "missing" : formatLocation(pair.top().location()))));
        player.sendMessage(plugin.color("&7Bottom hologram location: &f" + (pair == null ? "missing" : formatLocation(pair.bottom().location()))));
        player.sendMessage(plugin.color("&7Packet viewers: &f" + npc.viewers.size()));
    }

    public String repairHolograms(String stableId) {
        List<HousingNpc> targets = new ArrayList<>();
        if (stableId == null || stableId.trim().isEmpty()) targets.addAll(npcs);
        else {
            HousingNpc npc = findByStableId(stableId);
            if (npc == null) return plugin.color("&cNo NPC exists with the name &e" + stableId + "&c.");
            targets.add(npc);
        }
        int repaired = 0;
        for (HousingNpc npc : targets) {
            removeNameplate(npc);
            ensureNameplate(npc);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!npc.viewers.contains(viewer.getUniqueId()) || viewer.getWorld() != npc.location.getWorld()) continue;
                sendHolograms(viewer, npc);
            }
            repaired++;
        }
        return plugin.color("&aRepaired packet holograms for &f" + repaired + " &aNPC" + (repaired == 1 ? "" : "s") + ".");
    }

    public List<String> names(Player player) {
        List<String> names = new ArrayList<>();
        for (HousingNpc npc : npcs) {
            if (player == null || npc.location.getWorld().equals(player.getWorld())) names.add(displayName(npc));
        }
        return names;
    }

    public List<String> stableIds() {
        Set<String> ids = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ids.addAll(stableIds.values());
        for (PitSetupSnapshot.Npc definition : plugin.getSetupService().snapshot().npcs) {
            if (definition != null && definition.stableId != null && !definition.stableId.trim().isEmpty()) ids.add(definition.stableId);
        }
        return new ArrayList<>(ids);
    }

    private HousingNpc selectedOrLookedAtOrNearest(Player player) {
        UUID selected = selectedNpcs.get(player.getUniqueId());
        HousingNpc npc = selected == null ? null : findByUuid(selected);
        if (npc != null && npc.location.getWorld().equals(player.getWorld())) return npc;
        npc = lookedAt(player);
        return npc == null ? nearest(player) : npc;
    }

    private boolean removeNpc(HousingNpc npc) {
        if (!plugin.getSetupService().repository().deleteNpc(plugin.getArenaManager().getCurrentArena(), stableIds.getOrDefault(npc.uuid,npc.uuid.toString()))) return false;
        destroy(npc);
        npcs.remove(npc);
        clickHandlers.remove(npc.uuid);
        stableIds.remove(npc.uuid);
        runtimeActions.remove(npc.uuid);
        selectedNpcs.values().removeIf(uuid -> uuid.equals(npc.uuid));
        plugin.getSetupService().reloadFromDatabase();
        return true;
    }

    private HousingNpc nearest(Player player) {
        HousingNpc nearest = null;
        double best = 16.0D;
        for (HousingNpc npc : npcs) {
            if (!npc.location.getWorld().equals(player.getWorld())) continue;
            double distance = npc.location.distanceSquared(player.getLocation());
            if (distance < best) {
                best = distance;
                nearest = npc;
            }
        }
        return nearest;
    }

    public int size() {
        return npcs.size();
    }

    private HousingNpc selectedOnly(Player player) {
        UUID selected = selectedNpcs.get(player.getUniqueId());
        HousingNpc npc = selected == null ? null : findByUuid(selected);
        return npc != null && npc.location.getWorld().equals(player.getWorld()) ? npc : null;
    }

    public int hologramCount() { return transientHolograms.values().stream().mapToInt(value -> value.lines.size()).sum(); }
    public int viewerLinkCount() { return npcs.stream().mapToInt(value -> value.viewers.size()).sum() + transientHolograms.values().stream().mapToInt(value -> value.viewers.size()).sum(); }
    public boolean packetInitialized() { return hologramTransport != null; }
    public void respawn(Player player) {
        if (player == null || !player.isOnline()) return;
        destroyForViewer(player);
        spawnVisible(player);
    }

    public void reload() {
        for (HousingNpc npc : new ArrayList<>(npcs)) destroy(npc);
        clearNameplates();
        load();
        spawnAll();
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) plugin.getNativePacketRouter().unsubscribe(player, "npc");
        for (UUID id : new ArrayList<>(transientHolograms.keySet())) removeTransientHologram(id);
        for (HousingNpc npc : new ArrayList<>(npcs)) destroy(npc);
        clearNameplates();
        npcs.clear();
        clickHandlers.clear();
        selectedNpcs.clear();
        clickCooldowns.clear();
        if (lookTaskId != -1) {
            Bukkit.getScheduler().cancelTask(lookTaskId);
            lookTaskId = -1;
        }
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        inject(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnVisible(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        destroyForViewer(event.getPlayer());
        inject(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnVisible(event.getPlayer()), 5L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnVisible(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onRespawn(final PlayerRespawnEvent event) {
        destroyForViewer(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnVisible(event.getPlayer()), 2L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnVisible(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getNativePacketRouter().unsubscribe(event.getPlayer(), "npc");
        UUID viewerId = event.getPlayer().getUniqueId();
        for (HousingNpc npc : npcs) npc.viewers.remove(viewerId);
        for (TransientHologram hologram : transientHolograms.values()) hologram.viewers.remove(viewerId);
        selectedNpcs.remove(viewerId);
        clickCooldowns.remove(viewerId);
    }

    @EventHandler
    public void onWorldLoad(final WorldLoadEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            reload();
        }, 1L);
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        for (HousingNpc npc : new ArrayList<>(npcs)) if (npc.location != null && npc.location.getWorld() == event.getWorld()) destroy(npc);
    }

    private boolean save() {
        if (!plugin.getSetupService().repository().available()) return false;
        for (HousingNpc npc : npcs) {
            if (!npc.saveToConfig) continue;
            if (!plugin.getSetupService().repository().upsertNpc(snapshot(npc))) return false;
        }
        return true;
    }

    private boolean persist(HousingNpc npc) {
        boolean saved=plugin.getSetupService().repository().upsertNpc(snapshot(npc));
        if(saved)plugin.getSetupService().reloadFromDatabase();
        return saved;
    }

    private PitSetupSnapshot.Npc snapshot(HousingNpc npc) {
        return snapshot(npc, npc.location, stableIds.getOrDefault(npc.uuid,npc.uuid.toString()));
    }

    private PitSetupSnapshot.Npc snapshot(HousingNpc npc, Location location, String stableId) {
        PitSetupSnapshot.Npc value=new PitSetupSnapshot.Npc();
        PitSetupSnapshot.Npc stored = storedDefinition(stableId);
        value.stableId=stableId;value.worldName="";value.arenaId=stored == null ? plugin.getArenaManager().getCurrentArena() : stored.arenaId;value.typeId=stored == null ? "GENERIC" : stored.typeId;value.name=npc.name;value.location=new PitSetupSnapshot.Location("",location.getX(),location.getY(),location.getZ(),location.getYaw(),location.getPitch());value.skinId=stored == null ? stableId : stored.skinId;value.skinName=stored == null ? "" : stored.skinName;value.skinValue=npc.skinValue;value.skinSignature=npc.skinSignature;value.actionId=stored == null ? "command" : stored.actionId;value.command=npc.command;value.executionMode=stored == null ? "PLAYER" : stored.executionMode;value.hologramId=stored == null ? stableId : stored.hologramId;value.lookAtPlayers=npc.lookAtPlayers;value.enabled=true;
        return value;
    }

    private void applyLocation(PitSetupSnapshot.Npc value, Location location) {
        value.worldName = "";
        value.location = new PitSetupSnapshot.Location("", location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    private PitSetupSnapshot.Npc copy(PitSetupSnapshot.Npc source) {
        PitSetupSnapshot.Npc value = new PitSetupSnapshot.Npc();
        value.stableId=source.stableId;value.arenaId=source.arenaId;value.worldName="";value.typeId=source.typeId;value.name=source.name;value.location=source.location;value.skinId=source.skinId;value.skinName=source.skinName;value.skinValue=source.skinValue;value.skinSignature=source.skinSignature;value.actionId=source.actionId;value.command=source.command;value.executionMode=source.executionMode;value.hologramId=source.hologramId;value.lookAtPlayers=source.lookAtPlayers;value.enabled=source.enabled;
        return value;
    }

    private String encodeNpc(HousingNpc npc) {
        return text(npc.name) + "|" + text(npc.location.getWorld().getName()) + "|" + npc.location.getX() + "|" + npc.location.getY() + "|" + npc.location.getZ()
                + "|" + npc.location.getYaw() + "|" + npc.location.getPitch() + "|" + text(npc.skinValue) + "|" + text(npc.skinSignature)
                + "|" + text(npc.command) + "|" + npc.lookAtPlayers;
    }

    private HousingNpc decodeNpc(String id, String raw) {
        try {
            String[] p = raw.split("\\|", -1);
            if (p.length != 11) return null;
            World world = Bukkit.getWorld(untext(p[1]));
            if (world == null) return null;
            UUID uuid = UUID.fromString(id);
            Location location = new Location(world, Double.parseDouble(p[2]), Double.parseDouble(p[3]), Double.parseDouble(p[4]), Float.parseFloat(p[5]), Float.parseFloat(p[6]));
            return new HousingNpc(configuredEntityId(id), uuid, npcName(untext(p[0])), location, untext(p[7]), untext(p[8]), untext(p[9]), Boolean.parseBoolean(p[10]), true);
        } catch (Exception ignored) { return null; }
    }

    private String text(String value) { return Base64.getEncoder().encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)); }
    private String untext(String value) { try { return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8); } catch (Exception ignored) { return ""; } }
    private UUID stableUuid(String id){try{return UUID.fromString(id);}catch(Exception ignored){return UUID.nameUUIDFromBytes(("buildffa-npc:"+id).getBytes(StandardCharsets.UTF_8));}}

    private void fetchSkinLater(final HousingNpc npc, final String skinName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final SkinData skin = fetchSkin(skinName);
            if (skin.value.isEmpty() || skin.signature.isEmpty()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!npcs.contains(npc)) return;
                destroy(npc);
                npc.skinValue = skin.value;
                npc.skinSignature = skin.signature;
                npc.packetEntity = null;
                spawnAll();
            });
        });
    }

    private void spawnVisible(final Player viewer) {
        for (final HousingNpc npc : npcs) {
            spawnVisible(viewer, npc);
        }
        for (TransientHologram hologram : transientHolograms.values()) spawnTransient(viewer, hologram);
    }

    private void spawnVisible(final Player viewer, final HousingNpc npc) {
        if (!npc.location.getWorld().equals(viewer.getWorld())) return;
        if (npc.viewers.contains(viewer.getUniqueId())) return;
        try {
            ensureNameplate(npc);
            Object entityPlayer = packetEntity(npc);
            applyNpcTeam(viewer, npc);
            sendPacket(viewer, playerInfoPacket(entityPlayer));
            sendPacket(viewer, spawnPacket(entityPlayer));
            sendPacket(viewer, newHeadRotationPacket(entityPlayer, npc.location.getYaw()));
            if (!sendHolograms(viewer, npc)) {
                sendPacket(viewer, entityDestroyPacket(npc.entityId));
                return;
            }
            npc.viewers.add(viewer.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    if (viewer.isOnline()) sendPacket(viewer, playerInfoRemovePacket(entityPlayer, npc.uuid));
                } catch (Exception ex) {
                    plugin.getLogger().warning("Could not hide NPC from tab list: " + describe(ex));
                }
            }, 40L);
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not spawn The Pit NPC: " + describe(ex));
        }
    }

    public void applyHiddenNpcTeams(Scoreboard board) {
        if (board == null) return;
        for (HousingNpc npc : npcs) {
            applyNpcTeam(board, npc);
        }
    }

    private void destroy(HousingNpc npc) {
        removeNameplate(npc);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!npc.viewers.contains(player.getUniqueId())) continue;
            try {
                sendPacket(player, entityDestroyPacket(npc.entityId));
            } catch (Exception ex) {
                plugin.getLogger().warning("Could not remove The Pit NPC: " + describe(ex));
            }
        }
        npc.viewers.clear();
        PitPacketEntityEngine.shared().release(npc.entityId);
    }

    private void destroyForViewer(Player viewer) {
        for (HousingNpc npc : npcs) {
            if (!npc.viewers.remove(viewer.getUniqueId())) continue;
            HologramPair pair = npc.holograms;
            try {
                if (pair == null || hologramTransport == null) sendPacket(viewer, entityDestroyPacket(npc.entityId));
                else sendPacket(viewer, hologramTransport.destroyPacket(npc.entityId, pair.top().entityId(), pair.bottom().entityId()));
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not clear The Pit NPC packets for " + viewer.getName() + ": " + describe(exception));
            }
        }
        for (TransientHologram hologram : transientHolograms.values()) {
            if (!hologram.viewers.remove(viewer.getUniqueId())) continue;
            try {
                int[] ids = hologram.lines.stream().mapToInt(V1_8_R3NpcHologramTransport.Line::entityId).toArray();
                sendPacket(viewer, hologramTransport.destroyPacket(ids));
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not clear transient The Pit hologram packets for " + viewer.getName() + ": " + describe(exception));
            }
        }
    }

    private void startLookTask() {
        if (lookTaskId != -1) return;
        lookTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (HousingNpc npc : npcs) {
                if (!npc.lookAtPlayers) continue;
                updateLook(npc);
            }
        }, 20L, 10L).getTaskId();
    }

    private void updateLook(HousingNpc npc) {
        Player nearest = null;
        double best = 100.0D;
        for (Player player : npc.location.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(npc.location);
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        if (nearest == null) return;
        Location look = npc.location.clone();
        Vector direction = nearest.getEyeLocation().toVector().subtract(look.toVector());
        look.setDirection(direction);
        npc.location.setYaw(look.getYaw());
        npc.location.setPitch(look.getPitch());
        for (Player viewer : npc.location.getWorld().getPlayers()) {
            if (!npc.viewers.contains(viewer.getUniqueId())) continue;
            try {
                Object entityPlayer = packetEntity(npc);
                call(entityPlayer, "setLocation", npc.location.getX(), npc.location.getY(), npc.location.getZ(), npc.location.getYaw(), npc.location.getPitch());
                sendPacket(viewer, newHeadRotationPacket(entityPlayer, npc.location.getYaw()));
            } catch (Exception ignored) {
            }
        }
    }

    private void inject(final Player player) {
        try {
            plugin.getNativePacketRouter().subscribe(player, "npc", packet -> handlePacket(player, packet));
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not inject The Pit NPC packet listener for " + player.getName() + ": " + describe(ex));
        }
    }

    private boolean handlePacket(final Player player, Object packet) {
        if (!isInteractPacket(packet)) return false;
        final HousingNpc npc = findByEntityId(readEntityId(packet));
        if (npc == null) return false;
        if (!isRightClickInteraction(packet)) return true;
        final Consumer<Player> clickHandler = clickHandlers.get(npc.uuid);
        if (npc.command.isEmpty() && clickHandler == null) return true;
        long now = System.currentTimeMillis();
        Long last = clickCooldowns.get(player.getUniqueId());
        if (last != null && now - last < 500L) return true;
        clickCooldowns.put(player.getUniqueId(), now);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (clickHandler != null) {
                clickHandler.accept(player);
                return;
            }
            RuntimeAction action = runtimeActions.get(npc.uuid);
            String command = normalizeCommand(action == null ? npc.command : action.command())
                    .replace("%player%", player.getName())
                    .replace("%uuid%", player.getUniqueId().toString())
                    .replace("%arena%", action == null ? plugin.getArenaManager().getCurrentArena() : action.arenaId())
                    .replace("%npc_id%", action == null ? stableIds.getOrDefault(npc.uuid, "unknown") : action.npcId());
            String mode = action == null ? "PLAYER" : action.executionMode();
            debug("dispatch npc=" + stableIds.getOrDefault(npc.uuid, npc.uuid.toString()) + " arena=" + (action == null ? plugin.getArenaManager().getCurrentArena() : action.arenaId()) + " mode=" + mode + " command=" + command + " clicker=" + player.getUniqueId());
            boolean dispatched;
            try {
                dispatched = "CONSOLE".equals(mode) ? Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command) : Bukkit.dispatchCommand(player, command);
            } catch (Throwable failure) {
                plugin.getLogger().log(Level.SEVERE, "NPC dispatch failed npc=" + stableIds.getOrDefault(npc.uuid, npc.uuid.toString()) + " mode=" + mode + " command=" + command, failure);
                return;
            }
            if (!dispatched) plugin.getLogger().warning("NPC dispatch returned false npc=" + stableIds.getOrDefault(npc.uuid, npc.uuid.toString()) + " mode=" + mode + " command=" + command + " reason=command was not accepted by Bukkit");
        });
        return true;
    }

    private boolean isInteractPacket(Object packet) {
        String packetName = packet.getClass().getSimpleName();
        String packetClass = packet.getClass().getName();
        return packetName.equals("PacketPlayInUseEntity")
                || packetName.equals("ServerboundInteractPacket")
                || packetClass.endsWith(".PacketPlayInUseEntity")
                || packetClass.endsWith(".ServerboundInteractPacket");
    }

    private boolean isRightClickInteraction(Object packet) {
        for (Method method : packet.getClass().getDeclaredMethods()) {
            if (method.getParameterTypes().length != 0 || !method.getReturnType().isEnum()) continue;
            try {
                method.setAccessible(true);
                Object action = method.invoke(packet);
                if (action instanceof Enum<?>) return !((Enum<?>) action).name().equalsIgnoreCase("ATTACK");
            } catch (Throwable ignored) { }
        }
        for (Field field : packet.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object action = field.get(packet);
                if (action instanceof Enum<?>) return !((Enum<?>) action).name().equalsIgnoreCase("ATTACK");
                if (action != null && action.getClass().getSimpleName().toLowerCase(Locale.ENGLISH).contains("attack")) return false;
            } catch (Throwable ignored) { }
        }
        return true;
    }

    private HousingNpc findByEntityId(int entityId) {
        for (HousingNpc npc : npcs) {
            if (npc.entityId == entityId) return npc;
        }
        return null;
    }

    private HousingNpc findByUuid(UUID uuid) {
        for (HousingNpc npc : npcs) {
            if (npc.uuid.equals(uuid)) return npc;
        }
        return null;
    }

    private HousingNpc findByStableId(String stableId) {
        String wanted = stableId == null ? "" : stableId.trim();
        for (HousingNpc npc : npcs) {
            String id = stableIds.get(npc.uuid);
            if (id != null && id.equalsIgnoreCase(wanted)) return npc;
        }
        return null;
    }

    private PitSetupSnapshot.Npc storedDefinition(String stableId) {
        return storedDefinition(plugin.getArenaManager().getCurrentArena(), stableId);
    }

    private PitSetupSnapshot.Npc storedDefinition(String arenaId, String stableId) {
        if (stableId == null) return null;
        for (PitSetupSnapshot.Npc definition : plugin.getSetupService().snapshot().npcs) {
            if (definition != null && definition.stableId != null && definition.stableId.equalsIgnoreCase(stableId.trim()) && Objects.equals(normalizeStableId(definition.arenaId),normalizeStableId(arenaId))) return definition;
        }
        return null;
    }

    private String normalizeStableId(String stableId) {
        if (stableId == null) return "";
        return stableId.trim().toLowerCase(Locale.ENGLISH).replaceAll("\\s+", "_");
    }

    private HousingNpc findByName(Player player, String name) {
        String wanted = cleanName(name);
        for (HousingNpc npc : npcs) {
            if (!npc.location.getWorld().equals(player.getWorld())) continue;
            if (cleanName(displayName(npc)).equalsIgnoreCase(wanted) || cleanName(npc.name).equalsIgnoreCase(wanted)) return npc;
        }
        return null;
    }

    private HousingNpc lookedAt(Player player) {
        Vector eye = player.getEyeLocation().toVector();
        Vector direction = player.getEyeLocation().getDirection().normalize();
        HousingNpc bestNpc = null;
        double bestDistance = 32.0D;
        for (HousingNpc npc : npcs) {
            if (!npc.location.getWorld().equals(player.getWorld())) continue;
            Vector toNpc = npc.location.clone().add(0.0D, 1.6D, 0.0D).toVector().subtract(eye);
            double distance = toNpc.length();
            if (distance > bestDistance || distance <= 0.0D) continue;
            double angle = direction.dot(toNpc.normalize());
            if (angle < 0.98D) continue;
            bestDistance = distance;
            bestNpc = npc;
        }
        return bestNpc;
    }

    private String displayName(HousingNpc npc) {
        return profileBaseName(npc == null ? null : npc.name);
    }

    private String cleanName(String name) {
        String clean = ChatColor.stripColor(plugin.color(name == null ? "" : name));
        if (clean == null) return "";
        return clean.replaceAll("(?i)\\s*(?:RIGHT\\s+)?CLICK\\s*$", "").trim();
    }

    private int readEntityId(Object packet) {
        try {
            Object value = call(packet, "getEntityId");
            if (value instanceof Number) return ((Number) value).intValue();
        } catch (Exception ignored) {
        }
        try {
            Field field = findField(packet.getClass(), "a");
            field.setAccessible(true);
            Object value = field.get(packet);
            return value instanceof Number ? ((Number) value).intValue() : -1;
        } catch (Exception ignored) {
            for (Field field : packet.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(packet);
                    if (value instanceof Number) return ((Number) value).intValue();
                } catch (Exception ignoredAgain) {
                }
            }
        }
        return -1;
    }


    private void ensureNameplate(HousingNpc npc) {
        if (npc == null || npc.location == null || npc.location.getWorld() == null || hologramTransport == null) return;
        HologramPair existing = npc.holograms;
        String[] lines = npcNameLines(npc.name);
        Location topLocation = npc.location.clone().add(0.0D, topOffset(), 0.0D);
        Location bottomLocation = npc.location.clone().add(0.0D, bottomOffset(), 0.0D);
        if (existing != null
                && isHealthyNameLine(existing.top(), topLocation, lines[0])
                && isHealthyNameLine(existing.bottom(), bottomLocation, lines[1])) return;
        removeNameplate(npc);
        try {
            V1_8_R3NpcHologramTransport.Line top = hologramTransport.create(topLocation, lines[0]);
            V1_8_R3NpcHologramTransport.Line bottom = hologramTransport.create(bottomLocation, lines[1]);
            if (top.entityId() == bottom.entityId() || top.entityId() == npc.entityId || bottom.entityId() == npc.entityId
                    || top.uuid().equals(bottom.uuid()) || top.uuid().equals(npc.uuid) || bottom.uuid().equals(npc.uuid)) {
                throw new IllegalStateException("NPC and hologram identities collided");
            }
            npc.holograms = new HologramPair(top, bottom);
        } catch (Throwable exception) {
            npc.holograms = null;
            logNameplateFailure(npc, exception);
        }
    }

    public UUID createTransientHologram(Location location, List<String> text) {
        if (location == null || location.getWorld() == null || hologramTransport == null || text == null || text.isEmpty()) return null;
        UUID id = UUID.randomUUID();
        List<V1_8_R3NpcHologramTransport.Line> lines = new ArrayList<>();
        try {
            double offset = Math.max(0, text.size() - 1) * 0.25D;
            for (String value : text) {
                lines.add(hologramTransport.create(location.clone().add(0.0D, offset, 0.0D), CC.color(value)));
                offset -= 0.25D;
            }
            TransientHologram hologram = new TransientHologram(id, location.clone(), lines);
            transientHolograms.put(id, hologram);
            for (Player viewer : location.getWorld().getPlayers()) spawnTransient(viewer, hologram);
            return id;
        } catch (Throwable exception) {
            plugin.getLogger().warning("Could not create transient The Pit hologram: " + describe(exception));
            return null;
        }
    }

    public UUID replaceTransientHologram(UUID id, Location location, List<String> text) {
        removeTransientHologram(id);
        return createTransientHologram(location, text);
    }

    public void removeTransientHologram(UUID id) {
        TransientHologram hologram = id == null ? null : transientHolograms.remove(id);
        if (hologram == null || hologramTransport == null) return;
        int[] ids = hologram.lines.stream().mapToInt(V1_8_R3NpcHologramTransport.Line::entityId).toArray();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!hologram.viewers.contains(viewer.getUniqueId())) continue;
            try {
                sendPacket(viewer, hologramTransport.destroyPacket(ids));
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not remove transient The Pit hologram for " + viewer.getName() + ": " + describe(exception));
            }
        }
        hologram.viewers.clear();
        for (int entityId : ids) PitPacketEntityEngine.shared().release(entityId);
    }

    private void spawnTransient(Player viewer, TransientHologram hologram) {
        if (viewer == null || hologram == null || hologram.location.getWorld() != viewer.getWorld()
                || hologram.viewers.contains(viewer.getUniqueId()) || hologramTransport == null) return;
        try {
            for (V1_8_R3NpcHologramTransport.Line line : hologram.lines) {
                for (Object packet : hologramTransport.spawnPackets(line)) sendPacket(viewer, packet);
            }
            hologram.viewers.add(viewer.getUniqueId());
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not send transient The Pit hologram to " + viewer.getName() + ": " + describe(exception));
        }
    }

    private boolean sendHolograms(Player viewer, HousingNpc npc) {
        if (hologramTransport == null || viewer.getWorld() != npc.location.getWorld()) return false;
        HologramPair pair = npc.holograms;
        if (pair == null) return false;
        try {
            for (Object packet : hologramTransport.spawnPackets(pair.top())) sendPacket(viewer, packet);
            for (Object packet : hologramTransport.spawnPackets(pair.bottom())) sendPacket(viewer, packet);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not send The Pit NPC holograms to " + viewer.getName() + ": " + describe(exception));
            return false;
        }
    }

    private void removeNameplate(HousingNpc npc) {
        if (npc == null) return;
        HologramPair pair = npc.holograms;
        npc.holograms = null;
        if (pair == null || hologramTransport == null) return;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!npc.viewers.contains(viewer.getUniqueId())) continue;
            try {
                sendPacket(viewer, hologramTransport.destroyPacket(pair.top().entityId(), pair.bottom().entityId()));
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not destroy The Pit NPC holograms for " + viewer.getName() + ": " + describe(exception));
            }
        }
        PitPacketEntityEngine.shared().release(pair.top().entityId());
        PitPacketEntityEngine.shared().release(pair.bottom().entityId());
    }

    private void clearNameplates() {
        for (HousingNpc npc : new ArrayList<>(npcs)) removeNameplate(npc);
    }

    private boolean isHealthyNameLine(V1_8_R3NpcHologramTransport.Line line, Location expectedLocation, String expectedText) {
        return line != null
                && line.location().getWorld() == expectedLocation.getWorld()
                && line.location().distanceSquared(expectedLocation) <= 0.0001D
                && Objects.equals(line.text(), expectedText);
    }

    private String worldName(Location location) {
        return location == null || location.getWorld() == null ? "unloaded" : location.getWorld().getName();
    }

    private String formatLocation(Location location) {
        if (location == null) return "n/a";
        return worldName(location) + "," + round(location.getX()) + "," + round(location.getY()) + "," + round(location.getZ());
    }

    private double round(double value) { return Math.round(value * 100.0D) / 100.0D; }

    private void logNameplateFailure(HousingNpc npc, Throwable exception) {
        String message = "Could not create The Pit NPC hologram: npc=" + npc.uuid
                + ", world=" + worldName(npc.location) + ", location=" + formatLocation(npc.location)
                + ", server=" + Bukkit.getVersion();
        plugin.getLogger().log(Level.WARNING, message, exception);
    }

    private String[] npcNameLines(String configured) {
        return new String[]{plugin.color(npcName(configured)), plugin.color("&e&lCLICK")};
    }

    private double topOffset() {
        return plugin.registry().npcs().getDouble("npcs.hologram.top-offset", 2.55D);
    }

    private double bottomOffset() {
        return plugin.registry().npcs().getDouble("npcs.hologram.bottom-offset", 2.25D);
    }

    private String profileBaseName(String configured) {
        String text = configured == null ? "The Pit" : configured.replace("\\n", "\n");
        String firstLine = text.split("\\n")[0];
        String clean = ChatColor.stripColor(plugin.color(firstLine));
        if (clean == null || clean.trim().isEmpty()) clean = ChatColor.stripColor(plugin.color(text));
        if (clean == null || clean.trim().isEmpty()) clean = "The Pit";
        clean = clean.replaceAll("(?i)\\s*(?:RIGHT\\s+)?CLICK\\s*$", "").trim();
        return clean.isEmpty() ? "The Pit" : clean;
    }

    private Object packetEntity(HousingNpc npc) throws Exception {
        if (npc.packetEntity == null) npc.packetEntity = createEntityPlayer(npc);
        return npc.packetEntity;
    }

    private Object createEntityPlayer(HousingNpc npc) throws Exception {
        if (classExists("net.minecraft.server.level.ServerPlayer")) return createModernServerPlayer(npc);

        Object minecraftServer = call(Bukkit.getServer(), "getServer");
        Object worldServer = call(npc.location.getWorld(), "getHandle");
        Object profile = newGameProfile(npc);
        Constructor<?> interactManagerConstructor = nmsClass("PlayerInteractManager").getConstructor(nmsClass("World"));
        Object interactManager = interactManagerConstructor.newInstance(worldServer);
        Object entityPlayer = nmsClass("EntityPlayer")
                .getConstructor(nmsClass("MinecraftServer"), nmsClass("WorldServer"), classForName("com.mojang.authlib.GameProfile"), nmsClass("PlayerInteractManager"))
                .newInstance(minecraftServer, worldServer, profile, interactManager);

        call(entityPlayer, "setLocation", npc.location.getX(), npc.location.getY(), npc.location.getZ(), npc.location.getYaw(), npc.location.getPitch());
        setEntityId(entityPlayer, npc.entityId);
        return entityPlayer;
    }

    private Object createModernServerPlayer(HousingNpc npc) throws Exception {
        Object minecraftServer = call(Bukkit.getServer(), "getServer");
        Object serverLevel = call(npc.location.getWorld(), "getHandle");
        Object profile = newGameProfile(npc);
        Object clientInformation = call(classForName("net.minecraft.server.level.ClientInformation"), "createDefault");
        Object entityPlayer = classForName("net.minecraft.server.level.ServerPlayer")
                .getConstructor(classForName("net.minecraft.server.MinecraftServer"), classForName("net.minecraft.server.level.ServerLevel"), classForName("com.mojang.authlib.GameProfile"), classForName("net.minecraft.server.level.ClientInformation"))
                .newInstance(minecraftServer, serverLevel, profile, clientInformation);
        moveEntity(entityPlayer, npc.location);
        setEntityId(entityPlayer, npc.entityId);
        return entityPlayer;
    }

    private void moveEntity(Object entity, Location location) {
        Object moved = tryCall(entity, "moveTo", location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        if (moved != null) return;
        moved = tryCall(entity, "absMoveTo", location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        if (moved != null) return;
        tryCall(entity, "setPos", location.getX(), location.getY(), location.getZ());
        tryCall(entity, "setYRot", location.getYaw());
        tryCall(entity, "setXRot", location.getPitch());
    }

    private Object newGameProfile(HousingNpc npc) throws Exception {
        String profileName = profileName(profileBaseName(npc.name));
        if (!npc.skinValue.isEmpty() && !npc.skinSignature.isEmpty()) {
            Object property = newTextureProperty(npc);
            Object profile = newGameProfileWithProperties(npc.uuid, profileName, property);
            if (profile != null) {
                return profile;
            }

            profile = classForName("com.mojang.authlib.GameProfile")
                    .getConstructor(UUID.class, String.class)
                    .newInstance(npc.uuid, profileName);
            if (!addProfileProperty(profile, property)) {
                plugin.getLogger().warning("Could not apply skin to The Pit NPC '" + profileBaseName(npc.name) + "'. Spawning it without a skin.");
            }
            return profile;
        }
        return classForName("com.mojang.authlib.GameProfile")
                .getConstructor(UUID.class, String.class)
                .newInstance(npc.uuid, profileName);
    }

    private Object newTextureProperty(HousingNpc npc) throws Exception {
        return classForName("com.mojang.authlib.properties.Property")
                .getConstructor(String.class, String.class, String.class)
                .newInstance("textures", npc.skinValue, npc.skinSignature);
    }

    private Object newGameProfileWithProperties(UUID uuid, String name, Object property) {
        try {
            Object multimap = call(classForName("com.google.common.collect.HashMultimap"), "create");
            Method put = multimap.getClass().getMethod("put", Object.class, Object.class);
            put.invoke(multimap, "textures", property);
            Object propertyMap = classForName("com.mojang.authlib.properties.PropertyMap")
                    .getConstructor(classForName("com.google.common.collect.Multimap"))
                    .newInstance(multimap);
            return classForName("com.mojang.authlib.GameProfile")
                    .getConstructor(UUID.class, String.class, classForName("com.mojang.authlib.properties.PropertyMap"))
                    .newInstance(uuid, name, propertyMap);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean addProfileProperty(Object profile, Object property) {
        try {
            Object properties = profileProperties(profile);
            Method put = properties.getClass().getMethod("put", Object.class, Object.class);
            put.invoke(properties, "textures", property);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private SkinData readSkin(Player player) {
        try {
            Object profile = call(player, "getProfile");
            Object properties = profileProperties(profile);
            Object textures = call(properties, "get", "textures");
            Object iterator = call(textures, "iterator");
            if (!(Boolean) call(iterator, "hasNext")) return new SkinData("", "");
            Object texture = call(iterator, "next");
            return new SkinData(propertyValue(texture), propertySignature(texture));
        } catch (Exception ex) {
            return new SkinData("", "");
        }
    }

    private Object profileProperties(Object profile) throws Exception {
        try {
            return call(profile, "getProperties");
        } catch (NoSuchMethodException ignored) {
            return call(profile, "properties");
        }
    }

    private String propertyValue(Object property) throws Exception {
        try {
            return (String) call(property, "getValue");
        } catch (NoSuchMethodException ignored) {
            return (String) call(property, "value");
        }
    }

    private String propertySignature(Object property) throws Exception {
        try {
            return (String) call(property, "getSignature");
        } catch (NoSuchMethodException ignored) {
            return (String) call(property, "signature");
        }
    }

    private SkinData fetchSkin(String name) {
        try {
            String profile = readUrl("https://api.mojang.com/users/profiles/minecraft/" + name);
            String id = match(profile, "\"id\"\\s*:\\s*\"([^\"]+)\"");
            if (id.isEmpty()) return new SkinData("", "");
            String session = readUrl("https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=false");
            String value = match(session, "\"value\"\\s*:\\s*\"([^\"]+)\"");
            String signature = match(session, "\"signature\"\\s*:\\s*\"([^\"]+)\"");
            return new SkinData(value, signature);
        } catch (Exception ex) {
            return new SkinData("", "");
        }
    }

    private String readUrl(String url) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(URI.create(url).toURL().openStream(), StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) builder.append(line);
        reader.close();
        return builder.toString();
    }

    private String match(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private Object playerInfoPacket(Object entityPlayer) throws Exception {
        if (classExists("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket")) {
            Class<?> packetClass = classForName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            Class<?> actionClass = classForName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
            Object actionValue = enumConstant(actionClass, "ADD_PLAYER");
            @SuppressWarnings({"rawtypes", "unchecked"})
            EnumSet<?> actions = EnumSet.of((Enum) actionValue);

            Object entry = playerInfoEntry(entityPlayer);
            try {
                return packetClass.getConstructor(EnumSet.class, Collection.class).newInstance(actions, new ArrayList<>(Collections.singletonList(entry)));
            } catch (Throwable ignored) {
            }

            try {
                return packetClass.getConstructor(EnumSet.class, List.class).newInstance(actions, new ArrayList<>(Collections.singletonList(entry)));
            } catch (Throwable ignored) {
            }

            return packetClass.getConstructor(EnumSet.class, List.class).newInstance(actions, new ArrayList<>(Collections.singletonList(entityPlayer)));
        }
        return oldPlayerInfoPacket("ADD_PLAYER", entityPlayer);
    }

    private Object playerInfoEntry(Object entityPlayer) throws Exception {
        Class<?> entryClass = classForName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry");
        Class<?> gameTypeClass = classForName("net.minecraft.world.level.GameType");
        Class<?> componentClass = classForName("net.minecraft.network.chat.Component");
        Class<?> chatSessionDataClass = classForName("net.minecraft.network.chat.RemoteChatSession$Data");
        Object gameMode = enumConstant(gameTypeClass, "SURVIVAL");
        Object profile = call(entityPlayer, "getGameProfile");
        UUID uuid = (UUID) call(entityPlayer, "getUUID");
        return entryClass.getConstructor(UUID.class, classForName("com.mojang.authlib.GameProfile"), boolean.class, int.class, gameTypeClass, componentClass, boolean.class, int.class, chatSessionDataClass)
                .newInstance(uuid, profile, true, 0, gameMode, null, true, 0, null);
    }

    private Object oldPlayerInfoPacket(String action, Object entityPlayer) throws Exception {
        Class<?> packetClass = nmsClass("PacketPlayOutPlayerInfo");
        Class<?> actionClass = nmsClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
        Object actionValue = enumConstant(actionClass, action);
        Object players = newEntityPlayerArray();
        Array.set(players, 0, entityPlayer);
        return packetClass.getConstructor(actionClass, players.getClass()).newInstance(actionValue, players);
    }

    private Object playerInfoRemovePacket(Object entityPlayer, UUID uuid) throws Exception {
        if (classExists("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket")) {
            return classForName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket")
                    .getConstructor(List.class)
                    .newInstance(new ArrayList<>(Collections.singletonList(uuid)));
        }
        return oldPlayerInfoPacket("REMOVE_PLAYER", entityPlayer);
    }

    private Object spawnPacket(Object entityPlayer) throws Exception {
        if (classExists("net.minecraft.network.protocol.game.ClientboundAddEntityPacket")) {
            Class<?> packetClass = classForName("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
            Class<?> entityClass = classForName("net.minecraft.world.entity.Entity");

            try {
                return packetClass.getConstructor(entityClass, int.class).newInstance(entityPlayer, 0);
            } catch (Throwable ignored) {
            }
            try {
                return packetClass.getConstructor(entityClass).newInstance(entityPlayer);
            } catch (Throwable ignored) {
            }

            Class<?> entityTypeClass = classForName("net.minecraft.world.entity.EntityType");
            Class<?> vec3Class = classForName("net.minecraft.world.phys.Vec3");
            Object movement = zeroVec3(vec3Class);
            Object entityType = call(entityPlayer, "getType");
            int entityId = ((Number) call(entityPlayer, "getId")).intValue();
            UUID uuid = (UUID) call(entityPlayer, "getUUID");
            double x = ((Number) call(entityPlayer, "getX")).doubleValue();
            double y = ((Number) call(entityPlayer, "getY")).doubleValue();
            double z = ((Number) call(entityPlayer, "getZ")).doubleValue();
            float xRot = ((Number) call(entityPlayer, "getXRot")).floatValue();
            float yRot = ((Number) call(entityPlayer, "getYRot")).floatValue();
            return packetClass.getConstructor(int.class, UUID.class, double.class, double.class, double.class, float.class, float.class, entityTypeClass, int.class, vec3Class, double.class)
                    .newInstance(entityId, uuid, x, y, z, xRot, yRot, entityType, 0, movement, (double) yRot);
        }
        return newPacket("PacketPlayOutNamedEntitySpawn", nmsClass("EntityHuman")).newInstance(entityPlayer);
    }

    private Object zeroVec3(Class<?> vec3Class) throws Exception {
        Field zero = vec3Class.getField("ZERO");
        return zero.get(null);
    }

    private Object entityDestroyPacket(int entityId) throws Exception {
        if (classExists("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket")) {
            return classForName("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket")
                    .getConstructor(int[].class)
                    .newInstance((Object) new int[]{entityId});
        }
        return newPacket("PacketPlayOutEntityDestroy", int[].class).newInstance((Object) new int[]{entityId});
    }

    private Object newHeadRotationPacket(Object entity, float yaw) throws Exception {
        if (classExists("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket")) {
            return classForName("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket")
                    .getConstructor(classForName("net.minecraft.world.entity.Entity"), byte.class)
                    .newInstance(entity, angle(yaw));
        }
        return nmsClass("PacketPlayOutEntityHeadRotation").getConstructor(nmsClass("Entity"), byte.class).newInstance(entity, angle(yaw));
    }

    private byte angle(float value) {
        return (byte) ((int) (value * 256.0F / 360.0F));
    }

    private String describe(Throwable throwable) {
        Throwable current = throwable;
        if (current instanceof InvocationTargetException && ((InvocationTargetException) current).getTargetException() != null) {
            current = ((InvocationTargetException) current).getTargetException();
        }
        StringBuilder builder = new StringBuilder(current.getClass().getSimpleName());
        if (current.getMessage() != null && !current.getMessage().isEmpty()) {
            builder.append(": ").append(current.getMessage());
        }
        Throwable cause = current.getCause();
        if (cause != null && cause != current) {
            builder.append(" (caused by ").append(cause.getClass().getSimpleName());
            if (cause.getMessage() != null && !cause.getMessage().isEmpty()) {
                builder.append(": ").append(cause.getMessage());
            }
            builder.append(")");
        }
        StackTraceElement location = firstUsefulFrame(current);
        if (location != null) {
            builder.append(" at ").append(location.getClassName()).append(".").append(location.getMethodName()).append(":").append(location.getLineNumber());
        }
        return builder.toString();
    }

    private StackTraceElement firstUsefulFrame(Throwable throwable) {
        if (throwable == null) return null;
        for (StackTraceElement element : throwable.getStackTrace()) {
            if (element.getClassName().startsWith("me.outerspace.thepit.")) {
                return element;
            }
        }
        StackTraceElement[] stack = throwable.getStackTrace();
        return stack.length == 0 ? null : stack[0];
    }

    private String npcName(String configured) {
        String clean = configured == null ? "" : configured.replace("\\n", "\n").trim();
        if (clean.contains("\n")) clean = clean.substring(0, clean.indexOf('\n')).trim();
        clean = clean.replaceAll("(?i)(?:\\s|(?:&|§)[0-9A-FK-OR])*(?:RIGHT\\s+)?CLICK\\s*$", "").trim();
        String stripped = ChatColor.stripColor(plugin.color(clean));
        return stripped == null || stripped.trim().isEmpty() ? "&bThe Pit" : clean;
    }

    private String profileName(String displayName) {
        String clean = ChatColor.stripColor(plugin.color(displayName)).replaceAll("[^A-Za-z0-9_]", "");
        if (clean.isEmpty()) clean = "NPC";
        return clean.length() > 16 ? clean.substring(0, 16) : clean;
    }

    private void applyNpcTeam(Player viewer, HousingNpc npc) {
        applyNpcTeam(viewer.getScoreboard(), npc);
    }

    private void applyNpcTeam(Scoreboard board, HousingNpc npc) {
        if (board == null || npc == null) return;
        String teamName = ("hnpc" + npc.entityId);
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);
        String profileName = profileName(profileBaseName(npc.name));
        team.setPrefix("");
        team.setSuffix("");
        hideTeamNameTag(team);
        for (String entry : new HashSet<>(team.getEntries())) if (!entry.equals(profileName)) team.removeEntry(entry);
        if (!team.hasEntry(profileName)) team.addEntry(profileName);
    }


    private void hideTeamNameTag(Team team) {
        try {
            Class<?> optionClass = Class.forName("org.bukkit.scoreboard.Team$Option");
            Class<?> statusClass = Class.forName("org.bukkit.scoreboard.Team$OptionStatus");
            Object option = enumConstant(optionClass, "NAME_TAG_VISIBILITY");
            Object status = enumConstant(statusClass, "NEVER");
            Team.class.getMethod("setOption", optionClass, statusClass).invoke(team, option, status);
            return;
        } catch (Throwable ignored) {
        }
        try {
            Class<?> visibilityClass = Class.forName("org.bukkit.scoreboard.NameTagVisibility");
            Object never = enumConstant(visibilityClass, "NEVER");
            Team.class.getMethod("setNameTagVisibility", visibilityClass).invoke(team, never);
        } catch (Throwable ignored) {
        }
    }

    private String trim(String text) {
        if (text == null || text.length() <= 16) return text == null ? "" : text;
        return text.substring(0, 16);
    }

    private Constructor<?> newPacket(String name, Class<?>... parameterTypes) throws Exception {
        return nmsClass(name).getConstructor(parameterTypes);
    }

    private Object enumConstant(Class<?> enumClass, String name) {
        for (Object constant : enumClass.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(name)) return constant;
        }
        throw new IllegalArgumentException(name);
    }

    private Object newEntityPlayerArray() throws ClassNotFoundException {
        return Array.newInstance(nmsClass("EntityPlayer"), 1);
    }

    private void sendPacket(Player player, Object packet) throws Exception {
        Object connection = playerConnection(call(player, "getHandle"));
        try {
            call(connection, "send", packet);
        } catch (Exception ex) {
            call(connection, "sendPacket", packet);
        }
    }

    private Object call(Object target, String method, Object... args) throws Exception {
        if (target instanceof Class<?>) {
            return callStatic((Class<?>) target, method, args);
        }
        Method found = null;
        for (Method candidate : target.getClass().getMethods()) {
            if (!candidate.getName().equals(method) || candidate.getParameterTypes().length != args.length) continue;
            if (!accepts(candidate.getParameterTypes(), args)) continue;
            found = candidate;
            break;
        }
        if (found == null) throw new NoSuchMethodException(method);
        found.setAccessible(true);
        return found.invoke(target, args);
    }

    private Object tryCall(Object target, String method, Object... args) {
        try {
            return call(target, method, args);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object callStatic(Class<?> target, String method, Object... args) throws Exception {
        Method found = null;
        for (Method candidate : target.getMethods()) {
            if (!candidate.getName().equals(method) || candidate.getParameterTypes().length != args.length) continue;
            if (!accepts(candidate.getParameterTypes(), args)) continue;
            found = candidate;
            break;
        }
        if (found == null) throw new NoSuchMethodException(method);
        found.setAccessible(true);
        return found.invoke(null, args);
    }

    private boolean accepts(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            if (args[i] == null) continue;
            if (!wrap(types[i]).isAssignableFrom(args[i].getClass())) return false;
        }
        return true;
    }

    private Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private Object playerConnection(Object entityPlayer) throws Exception {
        Field field = findFieldOrNull(entityPlayer.getClass(), "playerConnection");
        if (field == null) field = findFieldOrNull(entityPlayer.getClass(), "connection");
        if (field == null) field = findFieldByTypeName(entityPlayer.getClass(), "ServerGamePacketListenerImpl");
        if (field == null) throw new NoSuchFieldException("playerConnection/connection");
        field.setAccessible(true);
        return field.get(entityPlayer);
    }

    private void setEntityId(Object entityPlayer, int id) throws Exception {
        Field field = findField(entityPlayer.getClass(), "id");
        field.setAccessible(true);
        field.set(entityPlayer, id);
    }

    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Field field = findFieldOrNull(type, name);
        if (field != null) return field;
        throw new NoSuchFieldException(name);
    }

    private Field findFieldOrNull(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Field findFieldByTypeName(Class<?> type, String typeName) {
        Class<?> current = type;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType().getName().equals(typeName) || field.getType().getSimpleName().equals(typeName)) return field;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Class<?> nmsClass(String name) throws ClassNotFoundException {
        try {
            return classForName("net.minecraft.server." + version() + "." + name);
        } catch (ClassNotFoundException ignored) {
            String modern = modernClassName(name);
            if (modern != null) return classForName(modern);
            throw ignored;
        }
    }

    private Class<?> classForName(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    private String version() {
        String[] parts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        return parts.length > 3 ? parts[3] : "";
    }

    private String modernClassName(String oldName) {
        if (oldName.equals("MinecraftServer")) return "net.minecraft.server.MinecraftServer";
        if (oldName.equals("WorldServer")) return "net.minecraft.server.level.ServerLevel";
        if (oldName.equals("World")) return "net.minecraft.world.level.Level";
        if (oldName.equals("EntityPlayer")) return "net.minecraft.server.level.ServerPlayer";
        if (oldName.equals("EntityHuman")) return "net.minecraft.world.entity.player.Player";
        if (oldName.equals("Entity")) return "net.minecraft.world.entity.Entity";
        if (oldName.equals("PlayerInteractManager")) return "net.minecraft.server.level.ServerPlayerGameMode";
        return null;
    }

    private boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static class HousingNpc {
        private final int entityId;
        private final UUID uuid;
        private String name;
        private Location location;
        private String skinValue;
        private String skinSignature;
        private String command;
        private boolean lookAtPlayers;
        private final boolean saveToConfig;
        private Object packetEntity;
        private HologramPair holograms;
        private final Set<UUID> viewers = new HashSet<>();

        private HousingNpc(int entityId, UUID uuid, String name, Location location, String skinValue, String skinSignature, String command, boolean lookAtPlayers, boolean saveToConfig) {
            this.entityId = entityId;
            this.uuid = uuid;
            this.name = name;
            this.location = location;
            this.skinValue = skinValue == null ? "" : skinValue;
            this.skinSignature = skinSignature == null ? "" : skinSignature;
            this.command = command == null ? "" : command;
            this.lookAtPlayers = lookAtPlayers;
            this.saveToConfig = saveToConfig;
        }
    }

    private String normalizeCommand(String command) {
        String normalized = command == null ? "" : command.trim();
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }

    private String normalizeExecutionMode(String mode) {
        return mode == null ? "PLAYER" : mode.trim().toUpperCase(Locale.ENGLISH);
    }

    private boolean isExecutionMode(String mode) {
        return "PLAYER".equalsIgnoreCase(mode) || "CONSOLE".equalsIgnoreCase(mode);
    }

    private void debug(String message) {
        if (plugin.registry().settings().getBoolean("npc.debug", false)) plugin.getLogger().info("NPC debug: " + message);
    }

    private record HologramPair(V1_8_R3NpcHologramTransport.Line top,
                                V1_8_R3NpcHologramTransport.Line bottom) {}

    private record RuntimeAction(String arenaId, String npcId, String actionId, String command, String executionMode) {}

    private static final class TransientHologram {
        private final UUID id;
        private final Location location;
        private final List<V1_8_R3NpcHologramTransport.Line> lines;
        private final Set<UUID> viewers = new HashSet<>();

        private TransientHologram(UUID id, Location location, List<V1_8_R3NpcHologramTransport.Line> lines) {
            this.id = id;
            this.location = location;
            this.lines = lines;
        }
    }

    private static class SkinData {
        private final String value;
        private final String signature;

        private SkinData(String value, String signature) {
            this.value = value == null ? "" : value;
            this.signature = signature == null ? "" : signature;
        }
    }
}
