package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.dungeonworld.DungeonWorldInstance;
import club.abelix.games.fakevival.dungeonworld.DungeonWorldProvider;
import club.abelix.games.fakevival.dungeonworld.SMPWorldDungeonProvider;
import club.abelix.games.fakevival.dungeonworld.DungeonSetupRepository;
import club.abelix.games.fakevival.dungeonworld.SlimeWorldDungeonProvider;
import club.abelix.games.fakevival.mmo.MMODamageSourceTracker;
import club.abelix.games.fakevival.mmo.MMODamageSourceType;
import club.abelix.games.fakevival.mmo.MobHealthScaler;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class DungeonManager {
    private static final int ABSOLUTE_MAX_MOBS_PER_CHUNK = 16;
    private static final double ACTIVE_MOB_RANGE = 48.0D;
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final Random random = new Random();
    private final NamespacedKey dungeonSpawnedKey;
    private final NamespacedKey dungeonBossKey;
    private final NamespacedKey dungeonLevelKey;
    private final NamespacedKey dungeonPortalKey;
    private final NamespacedKey dungeonPortalIdKey;
    private final NamespacedKey dungeonSpawnPortalKey;
    private final NamespacedKey dungeonIdKey;
    private final NamespacedKey spawnIdKey;
    private final NamespacedKey mobIdKey;
    private final NamespacedKey spawnIndexKey;
    private final NamespacedKey currentHealthKey;
    private final NamespacedKey maxHealthKey;
    private final NamespacedKey smpMobCurrentHealthKey;
    private final NamespacedKey smpMobMaxHealthKey;
    private final NamespacedKey smpMobLevelKey;
    private final NamespacedKey smpMobDisplayNameKey;
    private final NamespacedKey smpMobIdKey;
    private final NamespacedKey defenseKey;
    private final NamespacedKey lastDamagedKey;
    private final NamespacedKey temporaryMinionKey;
    private final NamespacedKey smpEntityTypeKey;
    private final NamespacedKey smpBossKey;
    private final NamespacedKey smpMobTypeKey;
    private final NamespacedKey smpDungeonIdKey;
    private final NamespacedKey smpDungeonRunIdKey;
    private final NamespacedKey smpBossIdKey;
    private final NamespacedKey smpMobDefenseKey;
    private final NamespacedKey smpMobDamageKey;
    private final NamespacedKey bossShieldUntilKey;
    private final NamespacedKey bossEnragedKey;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<String, CachedDungeonSnapshot> tablistSnapshots = new HashMap<>();
    private final Set<UUID> trackedDungeonMobs = new HashSet<>();
    private final Map<String, DungeonRun> activeRuns = new HashMap<>();
    private final Map<UUID, Map<String, Long>> abilityCooldowns = new HashMap<>();
    private final Map<UUID, Set<String>> triggeredBossPhases = new HashMap<>();
    private final Set<String> restoringDungeons = new HashSet<>();
    private final Set<String> duplicateBossSpawnWarnings = new HashSet<>();
    private final Set<String> missingTemplateWarnings = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<UUID, Location> pendingDungeonRespawns = new HashMap<>();
    private final Set<UUID> pendingTemplateEntries = new HashSet<>();
    private final AtomicBoolean dungeonSpawnTickRunning = new AtomicBoolean(false);
    private final DungeonWorldProvider worldProvider;
    private final DungeonSetupRepository setupRepository;
    private final MobHealthScaler healthScaler;
    private DungeonLootChestManager lootChestManager;
    private DungeonTrapManager trapManager;
    private volatile CompletableFuture<Void> setupReady = CompletableFuture.completedFuture(null);
    private long spawnRateWindow;
    private int spawnedInRateWindow;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();
    private final java.util.concurrent.ExecutorService runPersistence = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {Thread thread=new Thread(r,"SMP-Dungeon-Run-Save");thread.setDaemon(true);return thread;});

    public DungeonManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.dungeonSpawnedKey = new NamespacedKey(plugin, "dungeon_spawned");
        this.dungeonBossKey = new NamespacedKey(plugin, "dungeon_boss");
        this.dungeonLevelKey = new NamespacedKey(plugin, "dungeon_level");
        this.dungeonPortalKey = new NamespacedKey(plugin, "dungeon_portal");
        this.dungeonPortalIdKey = new NamespacedKey(plugin, "dungeon_portal_id");
        this.dungeonSpawnPortalKey = new NamespacedKey(plugin, "dungeon_spawn_portal");
        this.dungeonIdKey = new NamespacedKey(plugin, "dungeon_id");
        this.spawnIdKey = new NamespacedKey(plugin, "spawn_id");
        this.mobIdKey = new NamespacedKey(plugin, "mob_id");
        this.spawnIndexKey = new NamespacedKey(plugin, "spawn_index");
        this.currentHealthKey = new NamespacedKey(plugin, "dungeon_current_health");
        this.maxHealthKey = new NamespacedKey(plugin, "dungeon_max_health");
        this.smpMobCurrentHealthKey = new NamespacedKey(plugin, "smp_mob_current_health");
        this.smpMobMaxHealthKey = new NamespacedKey(plugin, "smp_mob_max_health");
        this.smpMobLevelKey = new NamespacedKey(plugin, "smp_mob_level");
        this.smpMobDisplayNameKey = new NamespacedKey(plugin, "smp_mob_display_name");
        this.smpMobIdKey = new NamespacedKey(plugin, "smp_mob_id");
        this.defenseKey = new NamespacedKey(plugin, "dungeon_defense");
        this.lastDamagedKey = new NamespacedKey(plugin, "dungeon_last_damaged");
        this.temporaryMinionKey = new NamespacedKey(plugin, "dungeon_temp_minion");
        this.smpEntityTypeKey = new NamespacedKey(plugin, "smp_entity_type");
        this.smpBossKey = new NamespacedKey(plugin, "smp_boss");
        this.smpMobTypeKey = new NamespacedKey(plugin, "smp_mob_type");
        this.smpDungeonIdKey = new NamespacedKey(plugin, "smp_dungeon_id");
        this.smpDungeonRunIdKey = new NamespacedKey(plugin, "smp_dungeon_run_id");
        this.smpBossIdKey = new NamespacedKey(plugin, "smp_boss_id");
        this.smpMobDefenseKey = new NamespacedKey(plugin, "smp_mob_defense");
        this.smpMobDamageKey = new NamespacedKey(plugin, "smp_mob_damage");
        this.bossShieldUntilKey = new NamespacedKey(plugin, "dungeon_boss_shield_until");
        this.bossEnragedKey = new NamespacedKey(plugin, "dungeon_boss_enraged");
        this.healthScaler = new MobHealthScaler(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin));
        registerDefaultDungeonBossTemplates();
        this.setupRepository = new DungeonSetupRepository(dataManager);
        this.worldProvider = createWorldProvider();
        initializePersistentDungeonSetup();
    }

    private void registerDefaultDungeonBossTemplates() {
        ConfigurationSection runtime = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        registerDefaultDungeonBossTemplates(runtime);
        if (plugin instanceof club.abelix.games.fakevival.Game game) {
            ConfigurationSection zones = game.getConfigFileManager().getZones();
            if (zones != runtime) registerDefaultDungeonBossTemplates(zones);
        }
    }

    private void registerDefaultDungeonBossTemplates(ConfigurationSection config) {
        if (!config.isConfigurationSection("zones.boss-templates.pirate_captain_blackwake")) {
            registerBossTemplate(config, "pirate_captain_blackwake", "piratedungeon", "Captain Blackwake", "HUSK", 75,
                350000.0D, 450.0D, 650.0D,
                "&8[&7Lv.75&8] &cCaptain Blackwake &a%current_health%/%max_health%&c\u2764",
                "&cCaptain Blackwake &7- &a%current_health%/%max_health%\u2764");
            registerBossAbility(config, "pirate_captain_blackwake", "blackwake_cleave", 8.0D,
                "&cCaptain Blackwake swings his cursed cutlass!");
            registerBossAbility(config, "pirate_captain_blackwake", "cannon_barrage", 18.0D,
                "&6Captain Blackwake calls a cannon barrage!");
            registerBossAbility(config, "pirate_captain_blackwake", "summon_crew", 25.0D,
                "&cCaptain Blackwake summons his crew!");
            registerBossAbility(config, "pirate_captain_blackwake", "tide_pull", 22.0D,
                "&bThe tide drags you toward Captain Blackwake!");
            registerBossAbility(config, "pirate_captain_blackwake", "enrage", 0.0D,
                "&4Captain Blackwake becomes enraged!");
            config.set("zones.boss-templates.pirate_captain_blackwake.abilities.summon_crew.aliases", List.of("pirate", "pirate_archer", "pirate_brute"));
            config.set("zones.boss-templates.pirate_captain_blackwake.abilities.summon_crew.fallback-types", List.of("HUSK", "SKELETON", "ZOMBIE"));
            config.set("zones.boss-templates.pirate_captain_blackwake.abilities.summon_crew.max-alive", 10);
            config.set("zones.boss-templates.pirate_captain_blackwake.abilities.enrage.threshold-percent", 30.0D);
            config.set("zones.boss-templates.pirate_captain_blackwake.abilities.enrage.damage-multiplier", 1.25D);
            config.set("zones.boss-templates.pirate_captain_blackwake.abilities.enrage.cooldown-multiplier", 0.8D);
            config.set("zones.boss-templates.pirate_captain_blackwake.drops.guaranteed", List.of("coins:5000-15000", "dungeon_token:1-3:if_exists", "pirate_doubloon:2-8:if_exists"));
            config.set("zones.boss-templates.pirate_captain_blackwake.drops.common", List.of("rotten_monster_flesh:4-12", "dungeon_fang:2-6", "imperial_cloth:1-4"));
            config.set("zones.boss-templates.pirate_captain_blackwake.drops.rare", List.of("cursed_fabric:1-3", "shadow_dust:1-3", "cracked_monster_core:1-2"));
            config.set("zones.boss-templates.pirate_captain_blackwake.drops.epic", List.of("pirate_captain_hat:1:if_exists", "blackwake_cutlass_fragment:1", "monster_soul_shard:1"));
            config.set("zones.boss-templates.pirate_captain_blackwake.drops.legendary", List.of("blackwake_cutlass:1:if_registered", "captain_blackwake_trophy:1:if_registered"));
        }
        if (!config.isConfigurationSection("zones.boss-templates.sacred_guardian_aurelius")) {
            registerBossTemplate(config, "sacred_guardian_aurelius", "sacreddungeon", "Sacred Guardian Aurelius", "ZOMBIE", 90,
                500000.0D, 700.0D, 800.0D,
                "&8[&7Lv.90&8] &cSacred Guardian Aurelius &a%current_health%/%max_health%&c\u2764",
                "&eSacred Guardian Aurelius &7- &a%current_health%/%max_health%\u2764");
            registerBossAbility(config, "sacred_guardian_aurelius", "radiant_slam", 10.0D,
                "&eAurelius slams the sacred ground!");
            registerBossAbility(config, "sacred_guardian_aurelius", "holy_beam", 16.0D,
                "&eA sacred beam locks onto %player%!");
            registerBossAbility(config, "sacred_guardian_aurelius", "sacred_circle", 22.0D,
                "&6Sacred circles begin to glow!");
            registerBossAbility(config, "sacred_guardian_aurelius", "summon_temple_spirits", 28.0D,
                "&eAurelius calls sacred spirits!");
            registerBossAbility(config, "sacred_guardian_aurelius", "divine_shield", 0.0D,
                "&eAurelius is protected by a divine shield!");
            registerBossAbility(config, "sacred_guardian_aurelius", "enrage", 0.0D,
                "&4Aurelius releases sacred fury!");
            config.set("zones.boss-templates.sacred_guardian_aurelius.abilities.summon_temple_spirits.aliases", List.of("temple_spirit", "sacred_minion", "guardian"));
            config.set("zones.boss-templates.sacred_guardian_aurelius.abilities.summon_temple_spirits.fallback-types", List.of("ZOMBIE", "SKELETON"));
            config.set("zones.boss-templates.sacred_guardian_aurelius.abilities.summon_temple_spirits.max-alive", 8);
            config.set("zones.boss-templates.sacred_guardian_aurelius.abilities.divine_shield.thresholds", List.of(70, 40, 15));
            config.set("zones.boss-templates.sacred_guardian_aurelius.abilities.divine_shield.duration-seconds", 8);
            config.set("zones.boss-templates.sacred_guardian_aurelius.abilities.divine_shield.damage-reduction", 0.5D);
            config.set("zones.boss-templates.sacred_guardian_aurelius.abilities.enrage.threshold-percent", 20.0D);
            config.set("zones.boss-templates.sacred_guardian_aurelius.abilities.enrage.damage-multiplier", 1.3D);
            config.set("zones.boss-templates.sacred_guardian_aurelius.abilities.enrage.holy-beam-cooldown-multiplier", 0.7D);
            config.set("zones.boss-templates.sacred_guardian_aurelius.drops.guaranteed", List.of("coins:10000-25000", "dungeon_token:2-5:if_exists", "sacred_relic_shard:2-6:if_exists"));
            config.set("zones.boss-templates.sacred_guardian_aurelius.drops.common", List.of("enchanted_monster_bone:1-3", "shadow_dust:1-3", "dungeon_hide:2-5"));
            config.set("zones.boss-templates.sacred_guardian_aurelius.drops.rare", List.of("sacred_cloth:1-3:if_exists", "holy_fragment:1-3:if_exists", "monster_soul_shard:1"));
            config.set("zones.boss-templates.sacred_guardian_aurelius.drops.epic", List.of("aurelius_core:1:if_exists", "sacred_guardian_trophy:1:if_exists", "divine_rune:1:if_exists"));
            config.set("zones.boss-templates.sacred_guardian_aurelius.drops.legendary", List.of("aurelius_sacred_blade:1:if_registered", "sacred_guardian_helmet:1:if_registered"));
        }
    }

    private void registerBossTemplate(ConfigurationSection config, String bossId, String dungeonId, String name, String type,
                                      int level, double health, double defense, double damage, String nametag, String bossbar) {
        String base = "zones.boss-templates." + bossId;
        config.set(base + ".name", name);
        config.set(base + ".display-name", name);
        config.set(base + ".dungeon-id", dungeonId);
        config.set(base + ".type", type);
        config.set(base + ".entity-type", type);
        config.set(base + ".level", level);
        config.set(base + ".health", health);
        config.set(base + ".max-health", health);
        config.set(base + ".defense", defense);
        config.set(base + ".damage", damage);
        config.set(base + ".boss-type", "DUNGEON_BOSS");
        config.set(base + ".nametag", nametag);
        config.set(base + ".bossbar", bossbar);
    }

    private void registerBossAbility(ConfigurationSection config, String bossId, String abilityId, double cooldownSeconds, String message) {
        String base = "zones.boss-templates." + bossId + ".abilities." + abilityId;
        config.set(base + ".registered", true);
        config.set(base + ".cooldown-seconds", cooldownSeconds);
        config.set(base + ".message", message);
    }

    public void startTasks() {
        setupReady.whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error != null) plugin.getLogger().warning("[SMP] Dungeon setup database load failed: " + rootMessage(error));
            migrateDungeonMobDefinitions();
            recoverDungeonRunStates();
            applyDungeonWorldRulesToAll();
        }));
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickActivePortals, 20L, 20L * 30L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickSpawnPortalRotation, 20L * 10L, 20L * 60L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::applyDungeonWorldRulesToAll, 40L, 20L * 300L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickBossBars, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::clearDungeonMobFire, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickDungeonAbilities, 40L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickDungeonRunPersistence, 200L, 200L);
    }

    private DungeonWorldProvider createWorldProvider() {
        String backend = dataManager.getData().getString("dungeons.world-backend", "smpworlds");
        if ("smpworlds".equalsIgnoreCase(backend) && plugin instanceof club.abelix.games.fakevival.Game game
                && game.getSMPWorldManager() != null) {
            plugin.getLogger().info("[Dungeon] Using SMPWorlds dungeon world provider.");
            return new SMPWorldDungeonProvider(game.getSMPWorldManager());
        }
        plugin.getLogger().info("[Dungeon] Using SlimeWorldManager-compatible dungeon world provider.");
        return new SlimeWorldDungeonProvider(plugin, dataManager);
    }

    public String dungeonWorldBackendName() {
        return worldProvider.getBackendName();
    }

    public void setLootChestManager(DungeonLootChestManager lootChestManager) {
        this.lootChestManager = lootChestManager;
    }

    public void setTrapManager(DungeonTrapManager trapManager) { this.trapManager = trapManager; }

    public String createDungeon(String name) {
        String worldName = getCreateWorldName(name);
        dataManager.getData().set(path(name) + ".enabled", true);
        dataManager.getData().set(path(name) + ".world", worldName);
        dataManager.getData().set(path(name) + ".boss.name", "Dungeon Boss");
        dataManager.getData().set(path(name) + ".boss.type", "ZOMBIE");
        dataManager.save();
        persistDungeonSetup(name);
        applyDungeonWorldRules(name);
        return worldName;
    }

    public boolean exists(String name) {
        return name == null || !dataManager.getData().isConfigurationSection(path(name));
    }

    public void setEnabled(String name, boolean enabled) {
        if (exists(name)) createDungeon(name);
        dataManager.getData().set(path(name) + ".enabled", enabled);
        dataManager.save();
        persistDungeonSetup(name);
        logDungeonLoaded(name);
    }

    public boolean isEnabled(String name) {
        String enabledPath = path(name) + ".enabled";
        if (dataManager.getData().contains(enabledPath)) return dataManager.getData().getBoolean(enabledPath);
        return dungeonDefinition(name) != null;
    }

    public String getWorldName(String name) {
        return dataManager.getData().getString(path(name) + ".world", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.default-world", "dungeon"));
    }

    public void setPortal(String name, Location location) {
        if (exists(name)) createDungeon(name);
        saveLocation(path(name) + ".portal", location);
        persistDungeonSetup(name);
    }

    public void setSpawnPortal(Location location) {
        saveLocation("dungeon-spawn-portal.location", location);
        saveLocation("dungeon-spawn-portal.pos1", location.clone().add(-1, -1, -1));
        saveLocation("dungeon-spawn-portal.pos2", location.clone().add(1, 3, 1));
        dataManager.getData().set("dungeon-spawn-portal.enabled", true);
        dataManager.save();
        refreshSpawnPortalStructure();
        if (getActiveSpawnPortalDungeon() == null) rotateSpawnPortal(false);
    }

    public void setSpawnPortalPos(String pos, Location location) {
        saveLocation("dungeon-spawn-portal." + pos.toLowerCase(), location);
        dataManager.getData().set("dungeon-spawn-portal.enabled", true);
        if (loadLocation("dungeon-spawn-portal.location") == null) saveLocation("dungeon-spawn-portal.location", location);
        dataManager.save();
        if (getActiveSpawnPortalDungeon() == null) rotateSpawnPortal(false);
    }

    public boolean isInsideSpawnPortal(Location location) {
        Location pos1 = loadLocation("dungeon-spawn-portal.pos1");
        Location pos2 = loadLocation("dungeon-spawn-portal.pos2");
        if (pos1 == null || pos2 == null || location == null || location.getWorld() == null) return false;
        if (!location.getWorld().equals(pos1.getWorld()) || !pos1.getWorld().equals(pos2.getWorld())) return false;
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return location.getX() >= minX && location.getX() <= maxX && location.getY() >= minY && location.getY() <= maxY && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    public String getActiveSpawnPortalDungeon() {
        return dataManager.getData().getString("dungeon-spawn-portal.active-dungeon");
    }

    public void rotateSpawnPortal(boolean forceBroadcast) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dungeons.spawn-portal.enabled", true)) return;
        Location portal = loadLocation("dungeon-spawn-portal.location");
        if (portal == null || portal.getWorld() == null) return;
        List<String> enabled = getEnabledDungeons();
        if (enabled.isEmpty()) return;
        String old = getActiveSpawnPortalDungeon();
        String next = enabled.get(random.nextInt(enabled.size()));
        if (enabled.size() > 1) {
            int tries = 0;
            while (next.equalsIgnoreCase(String.valueOf(old)) && tries++ < 10) next = enabled.get(random.nextInt(enabled.size()));
        }
        dataManager.getData().set("dungeon-spawn-portal.active-dungeon", next);
        dataManager.getData().set("dungeon-spawn-portal.next-rotate", System.currentTimeMillis() + getSpawnPortalIntervalMillis());
        dataManager.save();
        refreshSpawnPortalStructure();
        if (forceBroadcast || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dungeons.spawn-portal.broadcast-on-rotate", true)) {
            Bukkit.broadcastMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.spawn-portal.broadcast", "&5The spawn dungeon portal now leads to &f%dungeon%&5!")
                    .replace("%dungeon%", next)));
        }
    }

    public String getSpawnPortalInfo() {
        Location location = loadLocation("dungeon-spawn-portal.location");
        String active = getActiveSpawnPortalDungeon();
        long next = dataManager.getData().getLong("dungeon-spawn-portal.next-rotate", 0L);
        String loc = location == null ? "Not set" : location.getWorld().getName() + ", " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
        long seconds = Math.max(0L, (next - System.currentTimeMillis()) / 1000L);
        return "Portal: " + loc + " | Active: " + (active == null ? "None" : active) + " | Rotates in: " + seconds + "s";
    }

    public int clearAllPortals() {
        int cleared = 0;
        if (dataManager.getData().isConfigurationSection("dungeon-spawn-portal")) {
            clearSavedPortalStructure("dungeon-spawn-portal.location");
            cleared++;
            dataManager.getData().set("dungeon-spawn-portal", null);
        }

        ConfigurationSection section = dataManager.getData().getConfigurationSection("dungeons");
        if (section != null) {
            for (String dungeon : section.getKeys(false)) {
                String base = path(dungeon);
                if (dataManager.getData().isConfigurationSection(base + ".portal")) {
                    clearSavedPortalStructure(base + ".portal");
                    cleared++;
                    dataManager.getData().set(base + ".portal", null);
                }
                if (dataManager.getData().isConfigurationSection(base + ".active-portal")) {
                    clearSavedPortalStructure(base + ".active-portal.location");
                    cleared++;
                    dataManager.getData().set(base + ".active-portal", null);
                }
            }
        }

        cleared += clearTaggedPortalEntities();
        dataManager.save();
        return cleared;
    }

    public void setSpawn(String name, Location location) {
        if (exists(name)) createDungeon(name);
        dataManager.getData().set(path(name) + ".world", location.getWorld().getName());
        saveLocation(path(name) + ".spawn", location);
        applyDungeonWorldRules(name);
        persistDungeonSetup(name);
    }

    public void setAreaPos(String name, String pos, Location location) {
        if (exists(name)) createDungeon(name);
        saveLocation(path(name) + ".area." + pos.toLowerCase(), location);
        applyDungeonWorldRules(name);
        persistDungeonSetup(name);
    }

    public void setMobSpawn(String name, String type, Location location) {
        if (exists(name)) createDungeon(name);
        DungeonMobTemplateRegistry.DungeonMobTemplate template = DungeonMobTemplateRegistry.resolve(type);
        if (template == null) throw new IllegalArgumentException("Unknown dungeon mob alias " + type);
        String mobId=template.mobId();
        String base=path(name)+".mobs."+mobId;
        storeDungeonMobDefinition(base, mobId, template);
        saveLocation(base, location);
        applyDungeonWorldRules(name);
        persistDungeonSetup(name);
    }

    public String addMobSpawn(String name, String type, int level, String displayName, Location location) {
        if (exists(name)) createDungeon(name);
        DungeonMobTemplateRegistry.DungeonMobTemplate template = DungeonMobTemplateRegistry.resolve(type);
        if (template == null) throw new IllegalArgumentException("Unknown dungeon mob alias " + type);
        String key = normalizeMobId(type) + "_" + System.currentTimeMillis();
        String base = path(name) + ".mobs." + key;
        String cleanName = DungeonMobTemplateRegistry.cleanDisplayName(displayName, template);
        storeDungeonMobDefinition(base, key, template);
        dataManager.getData().set(base + ".mob_display_name", cleanName);
        dataManager.getData().set(base + ".name", cleanName);
        dataManager.getData().set(base + ".level", Math.max(1, level));
        saveLocation(base, location);
        applyDungeonWorldRules(name);
        persistDungeonSetup(name);
        return key;
    }

    public DungeonMobTemplateRegistry.DungeonMobTemplate dungeonMobTemplate(String alias) {
        return DungeonMobTemplateRegistry.resolve(alias);
    }

    public List<String> dungeonMobAliases() {
        return DungeonMobTemplateRegistry.aliases();
    }

    public void setBossLocation(String name, String bossName, Location location) { setBossLocation(name,bossName,1,location); }

    public void setBossLocation(String name, String bossName, int level, Location location) {
        if (exists(name)) createDungeon(name);
        String bossKey = normalizeMobId(bossName);
        ConfigurationSection template=bossTemplateSection()==null?null:bossTemplateSection().getConfigurationSection(bossKey);
        String display = template==null?bossName.replace('_', ' '):template.getString("name",bossName.replace('_',' '));
        dataManager.getData().set(path(name) + ".boss.name", display);
        dataManager.getData().set(path(name) + ".boss.id", bossKey);
        dataManager.getData().set(path(name) + ".boss.level", Math.max(1,level));
        saveLocation(path(name) + ".boss.location", location);
        dataManager.getData().set(path(name) + ".bosses." + bossKey + ".name", display);
        dataManager.getData().set(path(name) + ".bosses." + bossKey + ".mob-id", bossKey);
        dataManager.getData().set(path(name) + ".bosses." + bossKey + ".level", Math.max(1,level));
        dataManager.getData().set(path(name) + ".bosses." + bossKey + ".type", template==null?"ZOMBIE":template.getString("type","ZOMBIE"));
        if(template!=null){dataManager.getData().set(path(name)+".bosses."+bossKey+".health",Math.max(1,template.getDouble("health",100)));dataManager.getData().set(path(name)+".bosses."+bossKey+".defense",Math.max(0,template.getDouble("defense",0)));dataManager.getData().set(path(name)+".bosses."+bossKey+".damage",Math.max(0,template.getDouble("damage",10)));dataManager.getData().set(path(name)+".bosses."+bossKey+".boss-type",template.getString("boss-type","DUNGEON_BOSS"));}
        saveLocation(path(name) + ".bosses." + bossKey + ".location", location);
        applyDungeonWorldRules(name);
        persistDungeonSetup(name);
    }

    private ConfigurationSection bossTemplateSection(){if(plugin instanceof club.abelix.games.fakevival.Game game){ConfigurationSection section=game.getConfigFileManager().getZones().getConfigurationSection("zones.boss-templates");if(section!=null)return section;}return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("zones.boss-templates");}
    public boolean bossTypeExists(String id){String key=canonicalMobId(id);if(key.isBlank())return false;ConfigurationSection templates=bossTemplateSection();if(templates!=null&&templates.isConfigurationSection(key))return true;for(DungeonDefinition dungeon:dungeonDefinitions()){ConfigurationSection bosses=dataManager.getData().getConfigurationSection(path(dungeon.id())+".bosses");if(bosses!=null&&bosses.getKeys(false).stream().anyMatch(value->value.equalsIgnoreCase(key)))return true;}return false;}
    public boolean bossBelongsToDungeon(String bossId,String dungeonId){ConfigurationSection templates=bossTemplateSection();if(templates==null)return false;String registered=templates.getString(canonicalMobId(bossId)+".dungeon-id","");return registered.isBlank()||canonicalDungeonId(registered).equals(canonicalDungeonId(dungeonId));}
    public int defaultBossLevel(String bossId){ConfigurationSection templates=bossTemplateSection();return templates==null?1:Math.max(1,templates.getInt(canonicalMobId(bossId)+".level",1));}
    public List<MobTypeView> bossTypes(){Map<String,MobTypeView> result=new java.util.TreeMap<>();ConfigurationSection templates=bossTemplateSection();if(templates!=null)for(String id:templates.getKeys(false)){ConfigurationSection s=templates.getConfigurationSection(id);if(s!=null)result.put(canonicalMobId(id),new MobTypeView(canonicalMobId(id),s.getString("name",pretty(id)),"BOSS",Math.max(1,s.getInt("level",1)),Math.max(1,s.getDouble("health",100)),Math.max(0,s.getDouble("damage",10)),s.getConfigurationSection("abilities")==null?0: Objects.requireNonNull(s.getConfigurationSection("abilities")).getKeys(false).size()));}for(DungeonDefinition dungeon:dungeonDefinitions()){ConfigurationSection bosses=dataManager.getData().getConfigurationSection(path(dungeon.id())+".bosses");if(bosses!=null)for(String id:bosses.getKeys(false)){String base=path(dungeon.id())+".bosses."+id;result.putIfAbsent(canonicalMobId(id),new MobTypeView(canonicalMobId(id),dataManager.getData().getString(base+".name",pretty(id)),"BOSS",Math.max(1,dataManager.getData().getInt(base+".level",1)),Math.max(1,dataManager.getData().getDouble(base+".health",100)),Math.max(0,dataManager.getData().getDouble(base+".damage",10)),0));}}result.put("tyrant",new MobTypeView("tyrant","&4Tyrant","BOSS",75,250000,450,5));return new ArrayList<>(result.values());}
    public List<MobTypeView> mobTypes(){Map<String,MobTypeView> result=new java.util.TreeMap<>();for(String id:List.of("imperial_guards","imperial_archers","imperial_scouts","imperial_officer","imperial_minions"))result.put(id,new MobTypeView(id,pretty(id),"DUNGEON_MOB",1,40,5,0));for(DungeonDefinition dungeon:dungeonDefinitions()){ConfigurationSection mobs=dataManager.getData().getConfigurationSection(path(dungeon.id())+".mobs");if(mobs==null)continue;for(String spawn:mobs.getKeys(false)){String base=path(dungeon.id())+".mobs."+spawn;String id=canonicalMobId(dataManager.getData().getString(base+".mob_id",DungeonMobTemplateRegistry.withoutInternalSuffix(spawn)));if(id.contains("boss"))continue;result.putIfAbsent(id,new MobTypeView(id,dataManager.getData().getString(base+".mob_display_name",dataManager.getData().getString(base+".name",pretty(id))),"DUNGEON_MOB",Math.max(1,dataManager.getData().getInt(base+".level",1)),Math.max(1,dataManager.getData().getDouble(base+".health",40)),Math.max(0,dataManager.getData().getDouble(base+".damage",5)),0));}}return new ArrayList<>(result.values());}
    public int spawnTrapAmbush(String dungeonId, Location center, Player target, String requestedMobId, int amount) {
        String dungeon = canonicalDungeonId(dungeonId);
        DungeonRun run = activeRun(dungeon);
        if (run == null || run.state != DungeonState.ACTIVE || center == null || center.getWorld() == null) return 0;
        ConfigurationSection mobs = dataManager.getData().getConfigurationSection(path(dungeon) + ".mobs");
        if (mobs == null || mobs.getKeys(false).isEmpty()) return 0;
        String requested = canonicalMobId(requestedMobId);
        String key = mobs.getKeys(false).stream().filter(candidate -> requested.isBlank() || canonicalMobId(candidate).equals(requested)
                || canonicalMobId(dataManager.getData().getString(path(dungeon) + ".mobs." + candidate + ".mob_id",
                dataManager.getData().getString(path(dungeon) + ".mobs." + candidate + ".type", candidate))).equals(requested)).findFirst().orElse(mobs.getKeys(false).iterator().next());
        String mobPath = path(dungeon) + ".mobs." + key;
        String configuredType = dataManager.getData().getString(mobPath + ".entity_type",
                dataManager.getData().getString(mobPath + ".type", fallbackMobType(key)));
        DungeonMobTemplateRegistry.DungeonMobTemplate template = DungeonMobTemplateRegistry.resolveDefinition(key,
                dataManager.getData().getString(mobPath + ".mob_id"), configuredType);
        if (template == null) return 0;
        String mobName = DungeonMobTemplateRegistry.cleanDisplayName(dataManager.getData().getString(mobPath + ".mob_display_name",
                dataManager.getData().getString(mobPath + ".name", "")), template);
        int level = Math.max(1, dataManager.getData().getInt(mobPath + ".level", 1));
        double health = Math.max(1.0D, dataManager.getData().getDouble(mobPath + ".health", 500.0D));
        int spawned = 0;
        for (int index = 0; index < Math.clamp(amount, 1, 8); index++) {
            Location location = center.clone().add(random.nextDouble(-2.5D, 2.5D), 0, random.nextDouble(-2.5D, 2.5D));
            if (!canSpawnDungeonAt(dungeon, "trap_ambush", false, location)) continue;
            LivingEntity living = spawnOptimizedDungeonMob(template.mobId(), location,
                    resolveDungeonEntityType(dungeon, mobPath, configuredType), mobName, health, Math.max(1.0D, 4.0D + level), level);
            if (living == null) continue;
            tagDungeonEntity(living, dungeon, "trap_ambush_" + key, template.mobId(), index, level, mobName, false);
            living.getPersistentDataContainer().set(temporaryMinionKey, PersistentDataType.BYTE, (byte) 1);
            living.setRemoveWhenFarAway(false);
            makeDungeonMobSafe(living); writeDungeonHealth(living, health, health); syncDungeonBukkitHealth(living, health, health); updateDisplay(living);
            if (living instanceof Mob mob && target != null) mob.setTarget(target);
            trackedDungeonMobs.add(living.getUniqueId()); spawned++;
        }
        return spawned;
    }
    public static String normalizeMobId(String input){return input==null?"":input.toLowerCase(Locale.ROOT).trim().replace(' ','_').replace('-','_').replaceAll("[^a-z0-9_]","");}
    private String canonicalMobId(String id){return normalizeMobId(id);}
    public record MobTypeView(String id,String displayName,String role,int defaultLevel,double health,double damage,int abilities){}

    public void teleportToDungeon(Player player, String name) {
        if (!setupReady.isDone()) {
            if (!pendingTemplateEntries.add(player.getUniqueId())) return;
            player.sendMessage(CC.color("&eLoading dungeon template data..."));
            setupReady.whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                pendingTemplateEntries.remove(player.getUniqueId());
                if (!player.isOnline()) return;
                if (error != null) player.sendMessage(CC.color("&cDungeon setup could not be loaded. Try again shortly."));
                else teleportToDungeon(player, name);
            }));
            return;
        }
        final String canonicalName = canonicalDungeonId(name);
        if (!isEnabled(canonicalName)) {
            player.sendMessage(CC.color("&cThat dungeon is currently disabled."));
            return;
        }
        DungeonDefinition definition = dungeonDefinition(canonicalName);
        EntryCheck entry = canJoinDungeon(player, definition == null ? defaultDefinition(canonicalName) : definition);
        if (!entry.allowed()) {
            player.sendMessage(CC.color(entry.message()));
            return;
        }
        DungeonRun existingRun = activeRun(canonicalName);
        boolean rejoining=existingRun!=null&&existingRun.participants.contains(player.getUniqueId())&&existingRun.rejoinable();
        if(rejoining){existingRun.state=DungeonState.ACTIVE;existingRun.expiresAt=0;existingRun.lastPlayerLeftAt=0;existingRun.activePlayers.add(player.getUniqueId());player.sendMessage(CC.color("&aRejoining your existing dungeon instance..."));persistRunAsync(existingRun);}
        boolean needsNewRun = existingRun == null || existingRun.state == DungeonState.AVAILABLE || existingRun.state==DungeonState.EXPIRED || existingRun.state==DungeonState.COMPLETED || (!rejoining&&!hasAliveDungeonEntities(canonicalName));
        if (needsNewRun && worldProvider instanceof SMPWorldDungeonProvider smpProvider) {
            if (!hasSavedLocation(path(canonicalName) + ".spawn")) {
                player.sendMessage(CC.color("&cDungeon &e" + canonicalName + " &chas no saved spawn. Admins must run &e/setdungeonspawn " + canonicalName + "&c once and then save the dungeon."));
                return;
            }
            if (!pendingTemplateEntries.add(player.getUniqueId())) return;
            player.sendMessage(CC.color("&7Creating dungeon instance..."));
            String templateWorld = getWorldName(canonicalName);
            smpProvider.createInstance(canonicalName, templateWorld).whenComplete((instance, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
                pendingTemplateEntries.remove(player.getUniqueId());
                if (throwable != null) {
                    String reason = rootMessage(throwable);
                    if (reason.contains("does not exist") || reason.contains("not found") || reason.contains("No world data")) {
                        player.sendMessage(CC.color("&cDungeon world template &e" + templateWorld + " &cis missing from the SMPWorlds database."));
                    } else player.sendMessage(CC.color("&cCould not create dungeon instance: " + reason));
                    plugin.getLogger().warning("[Dungeon] SMPWorlds instance creation failed for " + canonicalName + ": " + rootMessage(throwable));
                    return;
                }
                enterDungeonWithInstance(player, canonicalName, instance, true);
            }));
            return;
        }
        Location spawn = rejoining?existingRun.lastLocations.get(player.getUniqueId()):null;
        if(spawn==null)spawn = loadLocation(path(canonicalName) + ".spawn");
        World world = getDungeonWorld(canonicalName);
        if (spawn == null && world != null) spawn = world.getSpawnLocation();
        if (spawn == null || spawn.getWorld() == null) {
            player.sendMessage(CC.color("&cDungeon spawn/world is not loaded. Load the world first, then set /setdungeonspawn " + canonicalName + "."));
            return;
        }
        applyDungeonWorldRules(canonicalName);
        saveLocation("players." + player.getUniqueId() + ".dungeon.return", player.getLocation());
        dataManager.getData().set("players." + player.getUniqueId() + ".dungeon.current", canonicalName);
        dataManager.save();
        player.teleport(spawn);
        player.sendMessage(CC.color("&aEntered dungeon &f" + canonicalName + "&a."));
        DungeonRun run = activeRun(canonicalName);
        if (run == null || run.state == DungeonState.AVAILABLE || !hasAliveDungeonEntities(canonicalName)) {
            run = startDungeonRun(canonicalName, player);
            if (lootChestManager != null) lootChestManager.spawnForRun(canonicalName, run.id, world, false);
            if (trapManager != null) trapManager.registerRun(canonicalName, run.id, world, false);
            Bukkit.getScheduler().runTaskLater(plugin, () -> spawnDungeonMobs(canonicalName), 20L);
        } else {
            run.activePlayers.add(player.getUniqueId());
            run.participants.add(player.getUniqueId());
            run.removedPlayers.remove(player.getUniqueId());
            run.state=DungeonState.ACTIVE;run.expiresAt=0;persistRunAsync(run);
        }
        run.lastLocations.put(player.getUniqueId(),player.getLocation().clone());persistPlayerLocationAsync(run,player.getUniqueId(),player.getLocation(),true);
    }

    private void enterDungeonWithInstance(Player player, String name, DungeonWorldInstance instance, boolean newRun) {
        if (!player.isOnline()) return;
        DungeonRun run = newRun ? startDungeonRun(name, player) : activeRun(name);
        if (run == null) run = startDungeonRun(name, player);
        run.instanceId = instance.instanceId();
        run.worldId = instance.worldId();
        run.backend = worldProvider.getBackendName();
        run.participants.add(player.getUniqueId());
        run.activePlayers.add(player.getUniqueId());
        if (lootChestManager != null) lootChestManager.spawnForRun(name, run.id, instance.world(), false);
        if (trapManager != null) trapManager.registerRun(name, run.id, instance.world(), false);
        Location spawn = loadLocationInWorld(path(name) + ".spawn", instance.world());
        if (spawn == null) spawn = instance.world().getSpawnLocation();
        applyDungeonWorldRules(name);
        saveLocation("players." + player.getUniqueId() + ".dungeon.return", player.getLocation());
        dataManager.getData().set("players." + player.getUniqueId() + ".dungeon.current", name.toLowerCase(Locale.ROOT));
        dataManager.save();
        player.teleport(spawn);
        player.sendMessage(CC.color("&aEntered dungeon &f" + name + "&a."));
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnDungeonMobs(name), 20L);
        run.lastLocations.put(player.getUniqueId(),player.getLocation().clone());persistPlayerLocationAsync(run,player.getUniqueId(),player.getLocation(),true);
        persistRunAsync(run);
    }

    public void completeDungeon(String name) {
        completeDungeon(name, null, "Dungeon Boss");
    }

    private void completeDungeon(String name, Player killer, String bossName) {
        DungeonRun run = activeRun(name);
        if (run == null) run = startEmptyRun(name);
        run.state = DungeonState.COMPLETED;
        run.completed=true;persistRunAsync(run);
        Set<UUID> players = new HashSet<>(run.activePlayers);
        if (killer != null) players.add(killer.getUniqueId());
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            if(plugin instanceof club.abelix.games.fakevival.Game game&&game.getAdventureGuildManager()!=null)
                game.getAdventureGuildManager().dungeonCompleted(player,normalizeMobId(bossName).contains("tyrant"));
            if (plugin instanceof club.abelix.games.fakevival.Game game)
                game.getEventManager().recordDungeonClear(player, 100L);
            String victor = killer == null ? "Your party" : killer.getName();
            player.sendMessage(CC.color("&6&lDUNGEON COMPLETE! &e" + victor + " defeated &c" + DungeonMobTemplateRegistry.cleanDisplayName(bossName, null) + "&e!"));
            player.sendMessage(CC.color("&aYou have been returned to spawn."));
            returnDungeonPlayer(player, name);
        }
        beginRestore(name);
    }

    private void returnDungeonPlayer(Player player, String dungeon) {
        if (player == null || !player.isOnline()) return;
        String current = dataManager.getData().getString("players." + player.getUniqueId() + ".dungeon.current");
        if (current == null || !current.equalsIgnoreCase(dungeon)) return;
        Location target = loadLocation("spawn");
        if (target == null && plugin instanceof club.abelix.games.fakevival.Game game) target = game.getSpawnManager().getSpawn();
        if (target == null) target = loadLocation("players." + player.getUniqueId() + ".dungeon.return");
        if (target == null && !Bukkit.getWorlds().isEmpty()) target = Bukkit.getWorlds().getFirst().getSpawnLocation();
        if (target != null) player.teleport(target);
        player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.returned-message", "&aYou were returned from the dungeon.")));
        dataManager.getData().set("players." + player.getUniqueId() + ".dungeon.current", null);
        dataManager.getData().set("players." + player.getUniqueId() + ".dungeon.return", null);
        dataManager.save();
    }

    public boolean isDungeonDeath(Player player) {
        if (player == null) return false;
        String current = dataManager.getData().getString("players." + player.getUniqueId() + ".dungeon.current");
        return current != null && !current.isBlank();
    }

    public void handleDungeonDeath(Player player) {
        if (player == null) return;
        String dungeon = dataManager.getData().getString("players." + player.getUniqueId() + ".dungeon.current");
        if (dungeon == null || dungeon.isBlank()) return;
        DungeonRun run = activeRun(dungeon);
        Location spawn = dungeonReturnLocation(player, dungeon);
        if (spawn != null) pendingDungeonRespawns.put(player.getUniqueId(), spawn);
        player.sendMessage(CC.color("&cYou died in the dungeon and were sent back to spawn. Your items were kept."));
        dataManager.getData().set("players." + player.getUniqueId() + ".dungeon.current", null);
        dataManager.getData().set("players." + player.getUniqueId() + ".dungeon.return", null);
        if (run == null) {
            failDungeonRun(dungeon, "&cYou died and your dungeon run failed.");
            dataManager.save();
            return;
        }
        run.activePlayers.remove(player.getUniqueId());
        run.removedPlayers.add(player.getUniqueId());
        dataManager.save();
        int remaining = activePlayerCount(dungeon);
        if (remaining <= 0) {
            player.sendMessage(CC.color("&cYou died and your dungeon run failed."));
            beginRestore(dungeon);
            return;
        }
    }

    public Location consumePendingDungeonRespawn(Player player) {
        return player == null ? null : pendingDungeonRespawns.remove(player.getUniqueId());
    }

    private Location dungeonReturnLocation(Player player, String dungeon) {
        Location target = loadLocation("spawn");
        if (target == null && plugin instanceof club.abelix.games.fakevival.Game game) target = game.getSpawnManager().getSpawn();
        if (target == null) target = loadLocation("players." + player.getUniqueId() + ".dungeon.return");
        if (target == null && !Bukkit.getWorlds().isEmpty()) target = Bukkit.getWorlds().getFirst().getSpawnLocation();
        return target;
    }

    public void isDungeonBoss(Entity entity) {
        handleDungeonBossDeath(entity, entity instanceof LivingEntity living ? living.getKiller() : null);
    }

    public void handleDungeonBossDeath(Entity entity, Player killer) {
        if (entity != null) removeBossBar(entity.getUniqueId());
        if (!(entity instanceof LivingEntity living)) return;
        String dungeon = living.getPersistentDataContainer().get(dungeonBossKey, PersistentDataType.STRING);
        if (dungeon != null) {
            DungeonRun run = activeRun(dungeon);
            if (run == null || run.state != DungeonState.ACTIVE || !entity.getUniqueId().equals(run.bossEntityUuid)) {
                plugin.getLogger().warning("[Dungeon] Ignored stale dungeon boss death for dungeon " + dungeon + " uuid=" + entity.getUniqueId());
                return;
            }
            run.bossAlive = false;
            run.bossKilled = true;
            run.bossEntityUuid = null;
            plugin.getLogger().info("[Dungeon] Boss killed for dungeon " + dungeon + " run " + run.id + "; completing run");
            String bossId = living.getPersistentDataContainer().getOrDefault(smpBossIdKey, PersistentDataType.STRING,
                    living.getPersistentDataContainer().getOrDefault(mobIdKey, PersistentDataType.STRING, ""));
            giveDefaultBossDrops(killer, bossId);
            rollDungeonBossLootBox(killer, dungeon);
            clearSummonedMinions(dungeon);
            triggeredBossPhases.remove(living.getUniqueId());
            completeDungeon(dungeon, killer, living.getPersistentDataContainer().getOrDefault(dungeonSpawnedKey, PersistentDataType.STRING, "Dungeon Boss"));
            return;
        }
        if (living.getCustomName() == null) return;
        ConfigurationSection section = dataManager.getData().getConfigurationSection("dungeons");
        if (section == null) return;
        for (String dungeonName : section.getKeys(false)) {
            String bossName = dataManager.getData().getString(path(dungeonName) + ".boss.name", "Dungeon Boss");
            if (CC.color("&c&l" + bossName).equals(living.getCustomName()) || living.getCustomName().contains(bossName)) {
                rollDungeonBossLootBox(killer, dungeonName);
                completeDungeon(dungeonName, killer, bossName);
                return;
            }
        }
    }

    private void rollDungeonBossLootBox(Player killer, String dungeon) {
        if (killer == null) return;
        double chance = Math.clamp(dataManager.getData().getDouble(path(dungeon) + ".boss-lootbox-drop-chance",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("dungeons.boss-lootbox-drop-chance", 20.0D)), 0.0D, 100.0D);
        if (random.nextDouble() * 100.0D > chance) return;
        List<String> commands = pickDungeonBossRewardCommands(dungeon);
        for (String rawCommand : commands) {
            String command = rawCommand
                .replace("%player%", killer.getName())
                .replace("%dungeon%", dungeon);
            if (!command.isBlank()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    private void giveDefaultBossDrops(Player killer, String bossId) {
        if (killer == null || !(plugin instanceof club.abelix.games.fakevival.Game game)) return;
        String id = canonicalMobId(bossId);
        if (id.equals("pirate_captain_blackwake")) {
            game.getEconomyManager().giveCoins(killer.getUniqueId(), random.nextLong(5000L, 15001L), "dungeon-boss:" + id);
            giveBossDrop(killer, "dungeon_token", 1, 3, 1.0D);
            giveBossDrop(killer, "pirate_doubloon", 2, 8, 1.0D);
            giveBossDrop(killer, "rotten_monster_flesh", 4, 12, 0.55D);
            giveBossDrop(killer, "dungeon_fang", 2, 6, 0.55D);
            giveBossDrop(killer, "imperial_cloth", 1, 4, 0.55D);
            giveBossDrop(killer, "cursed_fabric", 1, 3, 0.20D);
            giveBossDrop(killer, "shadow_dust", 1, 3, 0.20D);
            giveBossDrop(killer, "cracked_monster_core", 1, 2, 0.20D);
            giveBossDrop(killer, "pirate_captain_hat", 1, 1, 0.06D);
            giveBossDrop(killer, "blackwake_cutlass_fragment", 1, 1, 0.06D);
            giveBossDrop(killer, "monster_soul_shard", 1, 1, 0.06D);
            giveBossDrop(killer, "blackwake_cutlass", 1, 1, 0.015D);
            giveBossDrop(killer, "captain_blackwake_trophy", 1, 1, 0.015D);
        }
        if (id.equals("sacred_guardian_aurelius")) {
            game.getEconomyManager().giveCoins(killer.getUniqueId(), random.nextLong(10000L, 25001L), "dungeon-boss:" + id);
            giveBossDrop(killer, "dungeon_token", 2, 5, 1.0D);
            giveBossDrop(killer, "sacred_relic_shard", 2, 6, 1.0D);
            giveBossDrop(killer, "enchanted_monster_bone", 1, 3, 0.55D);
            giveBossDrop(killer, "shadow_dust", 1, 3, 0.55D);
            giveBossDrop(killer, "dungeon_hide", 2, 5, 0.55D);
            giveBossDrop(killer, "sacred_cloth", 1, 3, 0.20D);
            giveBossDrop(killer, "holy_fragment", 1, 3, 0.20D);
            giveBossDrop(killer, "monster_soul_shard", 1, 1, 0.20D);
            giveBossDrop(killer, "aurelius_core", 1, 1, 0.06D);
            giveBossDrop(killer, "sacred_guardian_trophy", 1, 1, 0.06D);
            giveBossDrop(killer, "divine_rune", 1, 1, 0.06D);
            giveBossDrop(killer, "aurelius_sacred_blade", 1, 1, 0.015D);
            giveBossDrop(killer, "sacred_guardian_helmet", 1, 1, 0.015D);
        }
    }

    private void giveBossDrop(Player player, String itemId, int minimum, int maximum, double chance) {
        if (!(plugin instanceof club.abelix.games.fakevival.Game game) || random.nextDouble() > chance) return;
        if (!game.getCustomItemManager().itemExists(itemId)) return;
        int amount = random.nextInt(Math.max(1, minimum), Math.max(minimum + 1, maximum + 1));
        ItemStack item = game.getCustomItemManager().createItem(itemId, amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack remaining : overflow.values()) player.getWorld().dropItemNaturally(player.getLocation(), remaining);
    }

    private void clearSummonedMinions(String dungeon) {
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity living : world.getLivingEntities()) {
                if (!living.getPersistentDataContainer().has(temporaryMinionKey, PersistentDataType.BYTE)) continue;
                if (!canonicalDungeonId(dungeon).equals(canonicalDungeonId(living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING)))) continue;
                trackedDungeonMobs.remove(living.getUniqueId());
                abilityCooldowns.remove(living.getUniqueId());
                living.remove();
            }
        }
    }

    private List<String> pickDungeonBossRewardCommands(String dungeon) {
        List<Map<?, ?>> rewards = dataManager.getData().getMapList(path(dungeon) + ".boss-rewards");
        if (rewards.isEmpty()) rewards = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getMapList("dungeons.boss-rewards");
        if (!rewards.isEmpty()) {
            double totalWeight = 0.0D;
            for (Map<?, ?> reward : rewards) totalWeight += Math.max(0.0D, asDouble(reward.get("weight"), 1.0D));
            double roll = random.nextDouble() * Math.max(0.0001D, totalWeight);
            double cursor = 0.0D;
            for (Map<?, ?> reward : rewards) {
                cursor += Math.max(0.0D, asDouble(reward.get("weight"), 1.0D));
                if (roll > cursor) continue;
                Object commands = reward.get("commands");
                if (commands instanceof List<?> list) return list.stream().map(String::valueOf).toList();
                Object command = reward.get("command");
                if (command != null) return List.of(String.valueOf(command));
            }
        }

        List<String> commands = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("dungeons.boss-lootbox-drop-commands");
        if (commands.isEmpty()) commands = List.of("lootbox give %player% weapon 1", "lootbox give %player% gear 1", "lootbox give %player% item 1");
        return List.of(commands.get(random.nextInt(commands.size())));
    }

    public boolean isInsideDungeonArea(Location location) {
        ConfigurationSection section = dataManager.getData().getConfigurationSection("dungeons");
        if (section == null) return false;
        for (String dungeon : section.getKeys(false)) {
            if (isInsideArea(dungeon, location)) return true;
        }
        return false;
    }

    public DungeonSnapshot getSnapshot(Player player) {
        if (player == null || !player.isOnline()) return DungeonSnapshot.NONE;
        ConfigurationSection section = dataManager.getData().getConfigurationSection("dungeons");
        if (section == null) return DungeonSnapshot.NONE;
        String dungeon = section.getKeys(false).stream()
                .filter(name -> isInsideArea(name, player.getLocation()))
                .findFirst().orElse(null);
        if (dungeon == null) return DungeonSnapshot.NONE;

        long now = System.currentTimeMillis();
        CachedDungeonSnapshot cached = tablistSnapshots.get(dungeon.toLowerCase(Locale.ROOT));
        if (cached != null && now - cached.loadedAtMillis() < 1500L) return cached.snapshot();

        int alive = 0;
        boolean bossAlive = false;
        for (Entity entity : player.getWorld().getEntities()) {
            if (!(entity instanceof LivingEntity living) || living.isDead() || !isDungeonEntity(living)) continue;
            if (!isInsideArea(dungeon, living.getLocation())) continue;
            alive++;
            if (living.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING)) bossAlive = true;
        }
        String base = path(dungeon);
        ConfigurationSection mobs = dataManager.getData().getConfigurationSection(base + ".mobs");
        ConfigurationSection bosses = dataManager.getData().getConfigurationSection(base + ".bosses");
        int bossCount = bosses == null ? (dataManager.getData().contains(base + ".boss.location") ? 1 : 0) : bosses.getKeys(false).size();
        int total = (mobs == null ? 0 : mobs.getKeys(false).size()) + bossCount;
        int progress = total <= 0 ? 0 : Math.clamp((int) Math.round((total - Math.min(total, alive)) * 100.0D / total), 0, 100);
        boolean active = dataManager.getData().getBoolean(base + ".run.active", false);
        String display = dataManager.getData().getString(base + ".display", pretty(dungeon));
        DungeonSnapshot snapshot = new DungeonSnapshot(true, display, alive,
                bossAlive ? "Alive" : active ? "Defeated" : "Inactive", progress);
        tablistSnapshots.put(dungeon.toLowerCase(Locale.ROOT), new CachedDungeonSnapshot(snapshot, now));
        return snapshot;
    }

    public record DungeonSnapshot(boolean active, String name, int mobsAlive, String bossStatus, int progress) {
        private static final DungeonSnapshot NONE = new DungeonSnapshot(false, "None", 0, "None", 0);
    }

    public BossSnapshot getBossSnapshot(Player player) {
        if (player == null || !player.isOnline()) return BossSnapshot.NONE;
        DungeonSnapshot dungeon = getSnapshot(player);
        if (!dungeon.active()) return BossSnapshot.NONE;
        for (Entity entity : player.getWorld().getEntities()) {
            if (!(entity instanceof LivingEntity living) || living.isDead() || !isDungeonEntity(living)) continue;
            if (!living.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING)) continue;
            if (!player.canSee(living)) continue;
            return new BossSnapshot(true, dungeon.name() + " Boss", "Alive",
                    (int) Math.ceil(living.getHealth()), (int) Math.ceil(living.getMaxHealth()));
        }
        return BossSnapshot.NONE;
    }

    public record BossSnapshot(boolean active, String name, String status, int health, int maxHealth) {
        private static final BossSnapshot NONE = new BossSnapshot(false, "None", "None", 0, 0);
    }

    private record CachedDungeonSnapshot(DungeonSnapshot snapshot, long loadedAtMillis) {
    }

    private static final class DungeonRun {
        private final String id;
        private final String dungeon;
        private final Set<String> clearedMobSpawns = new HashSet<>();
        private final Set<UUID> activePlayers = new HashSet<>();
        private final Set<UUID> participants = new HashSet<>();
        private final Set<UUID> spectators = new HashSet<>();
        private final Map<UUID,Location> lastLocations = new HashMap<>();
        private final Set<UUID> removedPlayers = new HashSet<>();
        private UUID bossEntityUuid;
        private boolean bossSpawned;
        private boolean bossAlive;
        private boolean bossKilled;
        private String instanceId;
        private String worldId;
        private String backend = "SlimeWorldManager";
        private DungeonState state = DungeonState.AVAILABLE;
        private final long createdAt=System.currentTimeMillis();
        private long lastPlayerLeftAt;
        private long expiresAt;
        private boolean completed;

        private DungeonRun(String dungeon) {
            this(dungeon,UUID.randomUUID().toString());
        }
        private DungeonRun(String dungeon,String id) {
            this.dungeon = dungeon.toLowerCase(Locale.ROOT);
            this.id=id;
        }

        private void markCleared(String spawnId, int spawnIndex) {
            clearedMobSpawns.add(id + ":" + dungeon + ":" + spawnId.toLowerCase(Locale.ROOT) + ":" + Math.max(0, spawnIndex));
        }

        private boolean isClearedSpawn(String spawnId, int spawnIndex) {
            return clearedMobSpawns.contains(id + ":" + dungeon + ":" + spawnId.toLowerCase(Locale.ROOT) + ":" + Math.max(0, spawnIndex));
        }
        private boolean rejoinable(){return !completed&&(state==DungeonState.ACTIVE||state==DungeonState.EMPTY_WAITING_FOR_REJOIN)&& (expiresAt==0||expiresAt>System.currentTimeMillis());}
    }

    public enum DungeonState {
        AVAILABLE,
        ACTIVE,
        EMPTY_WAITING_FOR_REJOIN,
        COMPLETED,
        RESETTING,
        EXPIRED,
        RESTORING
    }

    public boolean isDungeonEntity(Entity entity) {
        return entity instanceof LivingEntity living
                && living.getPersistentDataContainer().has(dungeonSpawnedKey, PersistentDataType.STRING);
    }

    public boolean isActiveDungeonInstanceWorld(World world) {
        if (world == null || world.getName().equalsIgnoreCase("lobby")) return false;
        for (DungeonRun run : activeRuns.values()) {
            if ((run.state != DungeonState.ACTIVE && run.state != DungeonState.EMPTY_WAITING_FOR_REJOIN) || run.instanceId == null) continue;
            World instanceWorld = worldProvider.getWorld(run.instanceId);
            if (instanceWorld != null && instanceWorld.getUID().equals(world.getUID())) return true;
            if (run.worldId != null && (run.worldId.equalsIgnoreCase(world.getName())
                    || world.getName().replace('\\', '/').endsWith("/" + run.worldId))) return true;
        }
        return false;
    }

    public boolean isActiveDungeonRun(String runId) {
        DungeonRun run = runById(runId);
        return run != null && run.state == DungeonState.ACTIVE;
    }

    public DungeonWorldContext dungeonWorldContext(World world) {
        if (world == null) return null;
        for (DungeonRun run : activeRuns.values()) {
            World instance = run.instanceId == null ? getDungeonWorld(run.dungeon) : worldProvider.getWorld(run.instanceId);
            if ((instance != null && instance.getUID().equals(world.getUID()))
                    || (run.worldId != null && run.worldId.equalsIgnoreCase(world.getName()))) {
                return new DungeonWorldContext(run.dungeon, run.id, getWorldName(run.dungeon), world);
            }
        }
        for (DungeonDefinition definition : dungeonDefinitions()) {
            String template = getWorldName(definition.id());
            if (template.equalsIgnoreCase(world.getName()) || world.getName().replace('\\', '/').endsWith("/" + template))
                return new DungeonWorldContext(definition.id(), null, template, world);
        }
        return null;
    }

    public List<DungeonWorldContext> activeDungeonWorldContexts() {
        List<DungeonWorldContext> contexts = new ArrayList<>();
        for (DungeonRun run : activeRuns.values()) {
            World world = run.instanceId == null ? getDungeonWorld(run.dungeon) : worldProvider.getWorld(run.instanceId);
            if (world != null && run.rejoinable()) contexts.add(new DungeonWorldContext(run.dungeon, run.id, getWorldName(run.dungeon), world));
        }
        return contexts;
    }

    public record DungeonWorldContext(String dungeonId, String runId, String templateWorldId, World world) { }

    public boolean isDungeonBossEntity(Entity entity) {
        return entity instanceof LivingEntity living
                && living.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING);
    }

    public DungeonCombatContext combatContext(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return DungeonCombatContext.NONE;
        String dungeon = living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING);
        if (dungeon == null) return DungeonCombatContext.NONE;
        DungeonRun run = activeRun(dungeon);
        return new DungeonCombatContext(dungeon, run == null ? "none" : run.id,
                living.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING));
    }

    public record DungeonCombatContext(String dungeonId, String runId, boolean boss) {
        private static final DungeonCombatContext NONE = new DungeonCombatContext("none", "none", false);
    }

    public boolean isAllowedDungeonCustomSpawn(Entity entity) {
        if (isDungeonEntity(entity)) return true;
        return entity instanceof LivingEntity living
                && living.getPersistentDataContainer().has(new NamespacedKey(plugin, "companion_id"), PersistentDataType.STRING);
    }

    public void removeUnauthorizedDungeonCustomSpawn(Entity entity) {
        if (!(entity instanceof LivingEntity)) return;
        if (!isInsideDungeonArea(entity.getLocation())) return;
        if (isAllowedDungeonCustomSpawn(entity)) return;
        entity.remove();
    }

    public void updateDungeonEntityDisplay(LivingEntity living) {
        if (!isDungeonEntity(living)) return;
        if (isEngineMob(living)) return;
        updateDisplay(living);
        if (living.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING)) updateBossBar(living);
    }

    public void handleDungeonEntityDamage(EntityDamageEvent event, Player attacker) {
        if (!(event.getEntity() instanceof LivingEntity living) || !isDungeonEntity(living)) return;
        String dungeon = living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING);
        if (dungeon != null && dungeonState(dungeon) != DungeonState.ACTIVE) {
            event.setCancelled(true);
            plugin.getLogger().info("[Combat] Cancelled monster damage for dungeon not active player="
                    + (attacker == null ? "unknown" : attacker.getName()) + " mob=" + living.getType());
            return;
        }
        if (!living.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING)) return;
        if (living.getPersistentDataContainer().getOrDefault(bossShieldUntilKey, PersistentDataType.LONG, 0L) > System.currentTimeMillis()) {
            event.setDamage(event.getDamage() * 0.5D);
        }
        if (isEngineMob(living)) return;
        double maxHealth = dungeonMaxHealth(living);
        double currentHealth = dungeonCurrentHealth(living);
        double incoming = Math.max(0.0D, event.getDamage());
        double defense = Math.min(600.0D, Math.max(0.0D, living.getPersistentDataContainer().getOrDefault(defenseKey, PersistentDataType.DOUBLE, 0.0D)));
        double damageTaken = incoming * (100.0D / (100.0D + defense));
        damageTaken = Math.max(damageTaken, incoming * 0.10D);
        currentHealth = Math.max(0.0D, currentHealth - damageTaken);
        writeDungeonHealth(living, currentHealth, maxHealth);
        living.getPersistentDataContainer().set(lastDamagedKey, PersistentDataType.LONG, System.currentTimeMillis());
        if (attacker != null && living instanceof Mob mob) mob.setTarget(attacker);
        if (currentHealth <= 0.0D) {
            event.setDamage(Math.max(0.01D, living.getHealth()));
            return;
        }
        event.setDamage(Math.min(Math.max(0.01D, damageTaken), Math.max(0.01D, living.getHealth() - 0.01D)));
        double syncedCurrentHealth = currentHealth;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!living.isDead() && living.isValid()) {
                syncDungeonBukkitHealth(living, syncedCurrentHealth, maxHealth);
                updateDungeonEntityDisplay(living);
            }
        });
    }

    public void applyDungeonBossOutgoingDamage(EntityDamageEvent event) {
        if (!(event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent byEntity)) return;
        Entity source = byEntity.getDamager();
        if (!(source instanceof LivingEntity living) || !isDungeonBossEntity(living)) return;
        if (!living.getPersistentDataContainer().has(bossEnragedKey, PersistentDataType.BYTE)) return;
        String bossId = living.getPersistentDataContainer().getOrDefault(smpBossIdKey, PersistentDataType.STRING, "");
        double multiplier = bossId.equals("sacred_guardian_aurelius") ? 1.30D : bossId.equals("pirate_captain_blackwake") ? 1.25D : 1.0D;
        event.setDamage(event.getDamage() * multiplier);
    }

    public boolean canBypassDungeonRules(Player player) {
        return player != null && player.hasPermission("fakevival.admin.dungeon.bypass");
    }

    public boolean isRuleEnabled(String key, boolean fallback) {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dungeons.arenas." + key, fallback);
    }

    public String getPortalDungeon(Location location) {
        if (location == null || location.getWorld() == null) return null;
        Location spawnPortal = loadLocation("dungeon-spawn-portal.location");
        String activeSpawnDungeon = getActiveSpawnPortalDungeon();
        if (activeSpawnDungeon != null && isEnabled(activeSpawnDungeon) && (near(location, spawnPortal) || isInsideSpawnPortal(location))) return activeSpawnDungeon;

        ConfigurationSection section = dataManager.getData().getConfigurationSection("dungeons");
        if (section == null) return null;
        for (String dungeon : section.getKeys(false)) {
            Location staticPortal = loadLocation(path(dungeon) + ".portal");
            Location activePortal = loadLocation(path(dungeon) + ".active-portal.location");
            if (near(location, staticPortal) || near(location, activePortal)) return dungeon;
        }
        return null;
    }

    public void spawnRandomPortal(String name) {
        if (exists(name)) createDungeon(name);
        String worldName = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.random-portal.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        Location location = findSafe(world);
        if (location == null) return;
        buildPortalStructure(location);
        saveLocation(path(name) + ".active-portal.location", location);
        long closesAt = System.currentTimeMillis() + club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("dungeons.random-portal.open-millis", 10800000L);
        dataManager.getData().set(path(name) + ".active-portal.closes-at", closesAt);
        dataManager.save();
        Bukkit.broadcastMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.random-portal.broadcast", "&5A dungeon portal opened at &f%x%, %y%, %z% &5in &f%world%&5 for 3 hours!")
                .replace("%dungeon%", name)
                .replace("%world%", world.getName())
                .replace("%x%", String.valueOf(location.getBlockX()))
                .replace("%y%", String.valueOf(location.getBlockY()))
                .replace("%z%", String.valueOf(location.getBlockZ()))));
    }

    public String listDungeons() {
        ConfigurationSection section = dataManager.getData().getConfigurationSection("dungeons");
        if (section == null || section.getKeys(false).isEmpty()) return "None";
        return String.join(", ", section.getKeys(false));
    }

    public List<DungeonDefinition> dungeonDefinitions() {
        Map<String, DungeonDefinition> definitions = new java.util.LinkedHashMap<>();
        registerDefaultDungeonDefinitions(definitions);
        ConfigurationSection section = dataManager.getData().getConfigurationSection("dungeons");
        if (section != null) for (String dungeon : section.getKeys(false)) {
            if (!section.isConfigurationSection(dungeon)) continue;
            String canonicalId = canonicalDungeonId(dungeon);
            if (!dungeon.equals(canonicalId) && section.isConfigurationSection(canonicalId)) continue;
            DungeonDefinition fallback = definitions.get(canonicalId);
            String base = path(dungeon);
            definitions.put(canonicalId, new DungeonDefinition(
                    canonicalId,
                    dataManager.getData().getString(base + ".display", fallback == null ? pretty(dungeon) : fallback.displayName()),
                    Material.matchMaterial(dataManager.getData().getString(base + ".menu-material", fallback == null ? "ENDER_EYE" : fallback.material().name())),
                    dataManager.getData().getString(base + ".description", fallback == null ? "A dangerous dungeon." : fallback.description()),
                    dataManager.getData().getInt(base + ".recommended-level", fallback == null ? 1 : fallback.recommendedLevel()),
                    dataManager.getData().getString(base + ".boss.name", fallback == null ? "Dungeon Boss" : fallback.bossName()),
                    fallback == null ? Map.of("combat", 1) : fallback.requiredSkills(),
                    dataManager.getData().getString(base + ".required-key", fallback == null ? "" : fallback.requiredKeyId()),
                    dataManager.getData().getBoolean(base + ".consume-key", fallback != null && fallback.consumeKey()),
                    Math.max(1, dataManager.getData().getInt(base + ".max-players", fallback == null ? 5 : fallback.maxPlayers())),
                    dataManager.getData().getBoolean(base + ".allow-late-join", fallback != null && fallback.allowLateJoin())));
        }
        return new ArrayList<>(definitions.values());
    }

    public EntryCheck checkEntry(Player player, String dungeonId) {
        DungeonAccessResult access = getAccessResult(player, dungeonId);
        if (!access.exists()) return new EntryCheck(false, false, "&cDungeon not found.");
        if (!access.enabled()) return new EntryCheck(false, false, "&cThat dungeon is currently disabled.");
        if (!access.requirementsMet()) return new EntryCheck(false, false, "&cYou do not meet the requirements for this dungeon.");
        DungeonDefinition definition = dungeonDefinition(dungeonId);
        EntryCheck joinCheck = canJoinDungeon(player, definition);
        if (!joinCheck.allowed()) return joinCheck;
        return new EntryCheck(true, definition.consumeKey() && !requiredKey(definition.id()).isBlank(), "&aUnlocked");
    }

    public DungeonAccessResult getAccessResult(Player player, String requestedId) {
        DungeonDefinition definition = dungeonDefinition(requestedId);
        if (definition == null) return new DungeonAccessResult(false, false, false, false, false, false, false,
                List.of("Dungeon not found"));
        String base = path(definition.id());
        int requiredPlayer = Math.max(0, dataManager.getData().getInt(base + ".required-player-level", 0));
        int requiredCombat = Math.max(0, dataManager.getData().getInt(base + ".required-combat-level",
                definition.requiredSkills().getOrDefault("combat", 0)));
        String key = requiredKey(definition.id());
        String permission = dataManager.getData().getString(base + ".required-permission", "");
        int playerLevel = plugin instanceof club.abelix.games.fakevival.Game game ? game.getPlayerLevelManager().getLevel(player) : 0;
        int combatLevel = plugin instanceof club.abelix.games.fakevival.Game game ? game.getSkillManager().getLevel(player, "combat") : 0;
        boolean playerLevelMet = playerLevel >= requiredPlayer;
        boolean combatLevelMet = combatLevel >= requiredCombat;
        boolean keyMet = key.isBlank() || hasDungeonKey(player, key);
        boolean permissionMet = permission == null || permission.isBlank() || player.hasPermission(permission);
        List<String> reasons = new ArrayList<>();
        if (!playerLevelMet) reasons.add("PlayerLevel " + requiredPlayer);
        if (!combatLevelMet) reasons.add("Combat Level " + requiredCombat);
        for (Map.Entry<String, Integer> requirement : definition.requiredSkills().entrySet()) {
            if (requirement.getKey().equalsIgnoreCase("combat")) continue;
            int level = plugin instanceof club.abelix.games.fakevival.Game game
                    ? game.getSkillManager().getLevel(player, requirement.getKey()) : 0;
            if (level < requirement.getValue()) reasons.add(pretty(requirement.getKey()) + " Level " + requirement.getValue());
        }
        if (!keyMet) reasons.add("Key " + key);
        if (!permissionMet) reasons.add("Permission " + permission);
        boolean requirementsMet = reasons.isEmpty();
        return new DungeonAccessResult(true, isEnabled(definition.id()), requirementsMet, playerLevelMet,
                combatLevelMet, keyMet, permissionMet, reasons);
    }

    public void enterFromMenu(Player player, String dungeonId) {
        EntryCheck check = checkEntry(player, dungeonId);
        if (!check.allowed()) {
            player.sendMessage(CC.color(check.message()));
            return;
        }
        DungeonDefinition definition = dungeonDefinition(dungeonId);
        if (definition != null && check.consumeKey()) removeDungeonKey(player, requiredKey(definition.id()));
        teleportToDungeon(player, dungeonId);
    }

    public EntryCheck canJoinDungeon(Player player, DungeonDefinition definition) {
        if (definition == null) return new EntryCheck(false, false, "&cUnknown dungeon.");
        DungeonState state = dungeonState(definition.id());
        DungeonRun run = activeRun(definition.id());
        if (state == DungeonState.RESTORING || state==DungeonState.RESETTING) return new EntryCheck(false, false, "&cThat dungeon is currently restoring. Please wait until it reopens.");
        if(state==DungeonState.COMPLETED)return new EntryCheck(false,false,"&cThat dungeon run is complete and is resetting.");
        if(state==DungeonState.EXPIRED)return new EntryCheck(false,false,"&cYour previous dungeon instance expired.");
        int activeCount = activePlayerCount(definition.id());
        if (state == DungeonState.ACTIVE || state==DungeonState.EMPTY_WAITING_FOR_REJOIN) {
            if (run != null && run.participants.contains(player.getUniqueId())&&run.rejoinable()) return new EntryCheck(true, false, "&aRejoining active run");
            if (!definition.allowLateJoin()) return new EntryCheck(false, false, "&cThat dungeon is already in progress.");
        }
        if (activeCount >= definition.maxPlayers() && (run == null || !run.activePlayers.contains(player.getUniqueId()))) {
            return new EntryCheck(false, false, "&cThat dungeon is full.");
        }
        return new EntryCheck(true, false, "&aOpen");
    }

    public DungeonState dungeonState(String dungeon) {
        DungeonRun run = activeRun(dungeon);
        if (run != null) return run.state;
        String stored = dataManager.getData().getString(path(dungeon) + ".run.state", DungeonState.AVAILABLE.name());
        try {
            return DungeonState.valueOf(stored.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return DungeonState.AVAILABLE;
        }
    }

    public int activeRunCount() {
        return activeRuns.size();
    }

    public int activePlayerCount(String dungeon) {
        DungeonRun run = activeRun(dungeon);
        if (run == null) return 0;
        run.activePlayers.removeIf(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) return true;
            String current = dataManager.getData().getString("players." + uuid + ".dungeon.current");
            return current == null || !current.equalsIgnoreCase(dungeon);
        });
        return run.activePlayers.size();
    }

    public int maxPlayers(String dungeon) {
        DungeonDefinition definition = dungeonDefinition(dungeon);
        return definition == null ? 5 : definition.maxPlayers();
    }

    private boolean meetsSkillRequirements(Player player, DungeonDefinition definition) {
        if (!(plugin instanceof club.abelix.games.fakevival.Game game)) return definition.requiredSkills().isEmpty();
        for (Map.Entry<String, Integer> requirement : definition.requiredSkills().entrySet()) {
            if (game.getSkillManager().getLevel(player, requirement.getKey()) < requirement.getValue()) return false;
        }
        return true;
    }

    private boolean hasDungeonKey(Player player, String keyId) {
        if (!(plugin instanceof club.abelix.games.fakevival.Game game)) return false;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && keyId.equalsIgnoreCase(String.valueOf(game.getCustomItemManager().getItemId(item)))) return true;
        }
        return false;
    }

    private boolean removeDungeonKey(Player player, String keyId) {
        if (!(plugin instanceof club.abelix.games.fakevival.Game game)) return false;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || !keyId.equalsIgnoreCase(String.valueOf(game.getCustomItemManager().getItemId(item)))) continue;
            item.setAmount(item.getAmount() - 1);
            contents[slot] = item.getAmount() <= 0 ? null : item;
            player.getInventory().setStorageContents(contents);
            return true;
        }
        return false;
    }

    private void registerDefaultDungeonDefinitions(Map<String, DungeonDefinition> definitions) {
        addDungeonDefinition(definitions, "imperial-dungeon", "Imperial Dungeon", Material.GOLDEN_SWORD, "Lost halls guarded by imperial soldiers.", 15, "Royal Guardian", Map.of("combat", 15), "dungeon_key_imperial");
        addDungeonDefinition(definitions, "frozen_catacombs", "Frozen Catacombs", Material.BLUE_ICE, "Frozen crypts crawling with revenants.", 20, "Frost Revenant", Map.of("combat", 20, "fishing", 5), "dungeon_key_frozen");
        addDungeonDefinition(definitions, "shadow_crypt", "Shadow Crypt", Material.ECHO_SHARD, "A sealed crypt full of assassins and void magic.", 30, "Shadow Assassin", Map.of("combat", 30), "dungeon_key_shadow");
        addDungeonDefinition(definitions, "toxic_depths", "Toxic Depths", Material.SLIME_BALL, "Poisoned depths ruled by toxic casters.", 25, "Plague Doctor", Map.of("combat", 22, "alchemy", 8), "dungeon_key_toxic");
        addDungeonDefinition(definitions, "crystal_mines", "Crystal Mines", Material.AMETHYST_SHARD, "Crystal caverns defended by golems.", 18, "Crystal Golem", Map.of("combat", 16, "mining", 15), "dungeon_key_crystal");
        addDungeonDefinition(definitions, "ancient_ruins", "Ancient Ruins", Material.CHISELED_STONE_BRICKS, "Ancient ruins where old guardians awaken.", 28, "Ancient Guardian", Map.of("combat", 25, "foraging", 10), "dungeon_key_ancient");
        addDungeonDefinition(definitions, "piratedungeon", "Pirate Dungeon", Material.CROSSBOW, "A cursed pirate stronghold commanded by Captain Blackwake.", 75, "Captain Blackwake", Map.of("combat", 75), "dungeon_key_pirate");
        addDungeonDefinition(definitions, "sacreddungeon", "Sacred Dungeon", Material.GOLDEN_HELMET, "A sacred temple defended by Guardian Aurelius.", 90, "Sacred Guardian Aurelius", Map.of("combat", 90), "dungeon_key_sacred");
    }

    private void addDungeonDefinition(Map<String, DungeonDefinition> definitions, String id, String name, Material material, String description,
                                      int recommendedLevel, String bossName, Map<String, Integer> skills, String key) {
        String canonicalId = canonicalDungeonId(id);
        definitions.put(canonicalId, new DungeonDefinition(canonicalId, name, material, description, recommendedLevel, bossName, skills, key, true, 5, false));
    }

    public record DungeonDefinition(String id, String displayName, Material material, String description, int recommendedLevel,
                                    String bossName, Map<String, Integer> requiredSkills, String requiredKeyId, boolean consumeKey,
                                    int maxPlayers, boolean allowLateJoin) {
        public DungeonDefinition {
            if (material == null || !material.isItem()) material = Material.ENDER_EYE;
            requiredSkills = requiredSkills == null ? Map.of() : requiredSkills;
            requiredKeyId = requiredKeyId == null ? "" : requiredKeyId;
            maxPlayers = Math.max(1, maxPlayers);
        }
    }

    public record EntryCheck(boolean allowed, boolean consumeKey, String message) {
    }

    public DungeonDefinition getDungeon(String dungeonId) {
        return dungeonDefinition(dungeonId);
    }

    private DungeonDefinition dungeonDefinition(String dungeonId) {
        String canonicalId = canonicalDungeonId(dungeonId);
        return dungeonDefinitions().stream()
                .filter(dungeon -> dungeon.id().equalsIgnoreCase(canonicalId))
                .findFirst().orElse(null);
    }

    public String dungeonSource(String dungeonId) {
        return storedDungeonKey(dungeonId) == null ? "StaticRegistryData" : "database";
    }

    public int requiredPlayerLevel(String dungeonId) {
        return Math.max(0, dataManager.getData().getInt(path(dungeonId) + ".required-player-level", 0));
    }

    public int requiredCombatLevel(String dungeonId) {
        DungeonDefinition definition = dungeonDefinition(dungeonId);
        return Math.max(0, dataManager.getData().getInt(path(dungeonId) + ".required-combat-level",
                definition == null ? 0 : definition.requiredSkills().getOrDefault("combat", 0)));
    }

    public String requiredKey(String dungeonId) {
        DungeonDefinition definition = dungeonDefinition(dungeonId);
        String value = dataManager.getData().getString(path(dungeonId) + ".required-key",
                definition == null ? "" : definition.requiredKeyId());
        return value == null ? "" : value;
    }

    public String requiredPermission(String dungeonId) {
        String value = dataManager.getData().getString(path(dungeonId) + ".required-permission", "");
        return value == null ? "" : value;
    }

    public int configuredMobCount(String dungeonId) {
        return configuredEntryCount(path(dungeonId) + ".mobs", path(dungeonId) + ".spawns");
    }

    public int configuredBossCount(String dungeonId) {
        int count = configuredEntryCount(path(dungeonId) + ".bosses", path(dungeonId) + ".boss-spawns");
        if (count == 0 && dataManager.getData().isConfigurationSection(path(dungeonId) + ".boss.location")) return 1;
        return count;
    }

    private int configuredEntryCount(String primary, String legacy) {
        ConfigurationSection section = dataManager.getData().getConfigurationSection(primary);
        if (section == null) section = dataManager.getData().getConfigurationSection(legacy);
        return section == null ? 0 : section.getKeys(false).size();
    }

    public void refreshDungeonCache() {
        for (DungeonDefinition definition : dungeonDefinitions()) logDungeonLoaded(definition.id());
    }

    private void initializePersistentDungeonSetup() {
        if (!setupRepository.available()) return;
        CompletableFuture<Void> future = new CompletableFuture<>();
        setupReady = future;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<DungeonSetupRepository.Snapshot> stored = setupRepository.loadAll();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        if (stored.isEmpty()) {
                            List<DungeonSetupRepository.Snapshot> seed = captureAllDungeonSnapshots();
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                try {
                                    for (DungeonSetupRepository.Snapshot snapshot : seed) setupRepository.save(snapshot);
                                    logDungeonSetupSummary(seed);
                                    future.complete(null);
                                } catch (SQLException exception) { future.completeExceptionally(exception); }
                            });
                        } else {
                            for (DungeonSetupRepository.Snapshot snapshot : stored) applyPersistentSnapshot(snapshot);
                            dataManager.save();
                            Set<String> storedIds = new HashSet<>();
                            for (DungeonSetupRepository.Snapshot snapshot : stored) storedIds.add(snapshot.dungeonId());
                            List<DungeonSetupRepository.Snapshot> merged = captureAllDungeonSnapshots();
                            List<DungeonSetupRepository.Snapshot> missing = merged.stream()
                                    .filter(snapshot -> !storedIds.contains(snapshot.dungeonId())).toList();
                            if (missing.isEmpty()) {
                                logDungeonSetupSummary(merged);
                                future.complete(null);
                            } else Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                try {
                                    for (DungeonSetupRepository.Snapshot snapshot : missing) setupRepository.save(snapshot);
                                    logDungeonSetupSummary(merged);
                                    future.complete(null);
                                } catch (SQLException exception) { future.completeExceptionally(exception); }
                            });
                        }
                    } catch (RuntimeException exception) { future.completeExceptionally(exception); }
                });
            } catch (SQLException exception) { future.completeExceptionally(exception); }
        });
    }

    private void logDungeonSetupSummary(List<DungeonSetupRepository.Snapshot> snapshots) {
        int playerSpawns = 0, mobs = 0, bosses = 0;
        for (DungeonSetupRepository.Snapshot snapshot : snapshots) {
            if (snapshot.playerSpawn() != null) playerSpawns++;
            for (DungeonSetupRepository.SpawnPoint point : snapshot.spawnPoints()) {
                if (point.type().equals("MOB_SPAWN")) mobs++;
                else if (point.type().equals("BOSS_SPAWN")) bosses++;
            }
        }
        plugin.getLogger().info("[SMP] Loaded " + snapshots.size() + " dungeon templates.");
        plugin.getLogger().info("[SMP] Loaded " + playerSpawns + " dungeon spawns.");
        plugin.getLogger().info("[SMP] Loaded " + mobs + " dungeon mob spawn points.");
        plugin.getLogger().info("[SMP] Loaded " + bosses + " dungeon boss spawn points.");
        if (plugin instanceof club.abelix.games.fakevival.Game game && game.getSMPWorldManager() != null) {
            for (DungeonSetupRepository.Snapshot snapshot : snapshots) {
                game.getSMPWorldManager().savedWorldExists(snapshot.templateWorldName()).thenAccept(exists -> {
                    if (!exists && missingTemplateWarnings.add(snapshot.dungeonId())) {
                        plugin.getLogger().warning("[SMP] Dungeon " + snapshot.dungeonId() + " template world "
                                + snapshot.templateWorldName() + " is missing from SMPWorlds database.");
                    }
                });
            }
        }
    }

    private List<DungeonSetupRepository.Snapshot> captureAllDungeonSnapshots() {
        List<DungeonSetupRepository.Snapshot> snapshots = new ArrayList<>();
        ConfigurationSection dungeons = dataManager.getData().getConfigurationSection("dungeons");
        if (dungeons == null) return snapshots;
        for (String id : dungeons.getKeys(false)) {
            DungeonSetupRepository.Snapshot snapshot = captureDungeonSnapshot(id);
            if (snapshot != null) snapshots.add(snapshot);
        }
        return snapshots;
    }

    private DungeonSetupRepository.Snapshot captureDungeonSnapshot(String dungeonName) {
        String id = canonicalDungeonId(dungeonName);
        ConfigurationSection section = dataManager.getData().getConfigurationSection(path(id));
        if (section == null) return null;
        String worldName = getWorldName(id);
        DungeonSetupRepository.LocationData spawn = rawLocation(path(id) + ".spawn");
        List<DungeonSetupRepository.SpawnPoint> points = new ArrayList<>();
        if (spawn != null) points.add(new DungeonSetupRepository.SpawnPoint("PLAYER_SPAWN", "entry", null, spawn, null));
        DungeonSetupRepository.LocationData portal = rawLocation(path(id) + ".portal");
        if (portal != null) points.add(new DungeonSetupRepository.SpawnPoint("PORTAL", "entry", null, portal, null));
        ConfigurationSection mobs = section.getConfigurationSection("mobs");
        if (mobs != null) for (String key : mobs.getKeys(false)) {
            DungeonSetupRepository.LocationData location = rawLocation(path(id) + ".mobs." + key);
            if (location != null) points.add(new DungeonSetupRepository.SpawnPoint("MOB_SPAWN", key,
                    section.getString("mobs." + key + ".mob_id", DungeonMobTemplateRegistry.withoutInternalSuffix(key)),
                    location, "{\"level\":"+Math.max(1,section.getInt("mobs."+key+".level",1))+"}"));
        }
        ConfigurationSection bosses = section.getConfigurationSection("bosses");
        if (bosses != null) for (String key : bosses.getKeys(false)) {
            DungeonSetupRepository.LocationData location = rawLocation(path(id) + ".bosses." + key + ".location");
            if (location != null) points.add(new DungeonSetupRepository.SpawnPoint("BOSS_SPAWN", key,
                    section.getString("bosses."+key+".mob-id",key), location, "{\"level\":"+Math.max(1,section.getInt("bosses."+key+".level",1))+"}"));
        }
        if (bosses == null) {
            DungeonSetupRepository.LocationData legacyBoss = rawLocation(path(id) + ".boss.location");
            if (legacyBoss != null) points.add(new DungeonSetupRepository.SpawnPoint("BOSS_SPAWN", "boss",
                    section.getString("boss.id", "boss"), legacyBoss, "{\"level\":"+Math.max(1,section.getInt("boss.level",1))+"}"));
        }
        Map<String, Object> metadata = new HashMap<>();
        for (Map.Entry<String, Object> entry : section.getValues(true).entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.startsWith("run.") || key.startsWith("active-portal.") || value instanceof ConfigurationSection) continue;
            if (value instanceof String || value instanceof Number || value instanceof Boolean
                    || value instanceof List<?> || value instanceof Map<?, ?>) metadata.put(key, value);
        }
        return new DungeonSetupRepository.Snapshot(id, worldName, worldName, "lobby", isEnabled(id),
                spawn, points, metadata);
    }

    private DungeonSetupRepository.LocationData rawLocation(String base) {
        String world = dataManager.getData().getString(base + ".world");
        if (world == null || !dataManager.getData().contains(base + ".x")) return null;
        return new DungeonSetupRepository.LocationData(world, dataManager.getData().getDouble(base + ".x"),
                dataManager.getData().getDouble(base + ".y"), dataManager.getData().getDouble(base + ".z"),
                (float) dataManager.getData().getDouble(base + ".yaw"),
                (float) dataManager.getData().getDouble(base + ".pitch"));
    }

    private void applyPersistentSnapshot(DungeonSetupRepository.Snapshot snapshot) {
        String base = "dungeons." + canonicalDungeonId(snapshot.dungeonId());
        for (Map.Entry<String, Object> entry : snapshot.metadata().entrySet())
            dataManager.getData().set(base + "." + entry.getKey(), entry.getValue());
        dataManager.getData().set(base + ".world", snapshot.templateWorldName());
        dataManager.getData().set(base + ".enabled", snapshot.enabled());
        applyLocationData(base + ".spawn", snapshot.playerSpawn());
        for (DungeonSetupRepository.SpawnPoint point : snapshot.spawnPoints()) {
            String target = switch (point.type()) {
                case "MOB_SPAWN" -> base + ".mobs." + point.id();
                case "BOSS_SPAWN" -> base + ".bosses." + point.id() + ".location";
                case "PORTAL" -> base + ".portal";
                default -> null;
            };
            if (target != null) applyLocationData(target, point.location());
            if(target!=null&&point.registryId()!=null&&!point.registryId().isBlank()){
                String normalized=normalizeMobId(point.registryId());
                if(point.type().equals("MOB_SPAWN"))dataManager.getData().set(base+".mobs."+point.id()+".mob_id",normalized);
                else if(point.type().equals("BOSS_SPAWN"))dataManager.getData().set(base+".bosses."+point.id()+".mob-id",normalized);
            }
            if(target!=null&&point.metadataJson()!=null)try{com.google.gson.JsonObject metadata=gson.fromJson(point.metadataJson(),com.google.gson.JsonObject.class);if(metadata!=null&&metadata.has("level")){String levelPath=point.type().equals("BOSS_SPAWN")?base+".bosses."+point.id()+".level":base+".mobs."+point.id()+".level";dataManager.getData().set(levelPath,Math.max(1,metadata.get("level").getAsInt()));}}catch(RuntimeException ignored){}
        }
    }

    private void applyLocationData(String base, DungeonSetupRepository.LocationData location) {
        if (location == null) return;
        dataManager.getData().set(base + ".world", location.world()); dataManager.getData().set(base + ".x", location.x());
        dataManager.getData().set(base + ".y", location.y()); dataManager.getData().set(base + ".z", location.z());
        dataManager.getData().set(base + ".yaw", location.yaw()); dataManager.getData().set(base + ".pitch", location.pitch());
    }

    private void persistDungeonSetup(String dungeonName) {
        if (!setupRepository.available()) return;
        DungeonSetupRepository.Snapshot snapshot = captureDungeonSnapshot(dungeonName);
        if (snapshot == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { setupRepository.save(snapshot); }
            catch (SQLException exception) { plugin.getLogger().warning("[SMP] Could not save dungeon setup "
                    + snapshot.dungeonId() + ": " + exception.getMessage()); }
        });
    }

    public CompletableFuture<String> saveDungeonTemplate(String dungeonName, boolean force) {
        return saveDungeonTemplate(dungeonName, force, null, null);
    }

    public CompletableFuture<String> saveDungeonTemplate(String dungeonName, boolean force, String savedByUuid, String savedByName) {
        String id = canonicalDungeonId(dungeonName);
        DungeonRun active = activeRun(id);
        if (active != null && active.state == DungeonState.ACTIVE)
            return CompletableFuture.failedFuture(new IllegalStateException("Finish or reset the active dungeon run before saving its template."));
        dataManager.save();
        clearDungeonEntities(id);
        DungeonSetupRepository.Snapshot snapshot = captureDungeonSnapshot(id);
        if (snapshot == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown dungeon " + id));
        CompletableFuture<Void> setupSave = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { setupRepository.save(snapshot); setupSave.complete(null); }
            catch (SQLException exception) { setupSave.completeExceptionally(exception); }
        });
        if (!(plugin instanceof club.abelix.games.fakevival.Game game) || game.getSMPWorldManager() == null)
            return CompletableFuture.failedFuture(new IllegalStateException("SMPWorlds is unavailable"));
        return setupSave.thenCompose(ignored -> game.getSMPWorldManager()
                .saveWorldToDatabase(snapshot.templateWorldName(), force, true, savedByUuid, savedByName, id))
                .thenApply(ignored -> "Saved dungeon " + id + " world and setup to the database.");
    }

    public CompletableFuture<DungeonValidation> validateDungeonTemplate(String dungeonName) {
        String id = canonicalDungeonId(dungeonName);
        DungeonSetupRepository.Snapshot snapshot = captureDungeonSnapshot(id);
        if (snapshot == null) return CompletableFuture.completedFuture(new DungeonValidation(id, false, false, false, 0, 0, false));
        if (!(plugin instanceof club.abelix.games.fakevival.Game game) || game.getSMPWorldManager() == null)
            return CompletableFuture.completedFuture(new DungeonValidation(id, false, false, snapshot.playerSpawn() != null,
                    configuredMobCount(id), configuredBossCount(id), false));
        return game.getSMPWorldManager().savedWorldExists(snapshot.templateWorldName()).thenCompose(exists -> {
            int mobs = (int) snapshot.spawnPoints().stream().filter(point -> point.type().equals("MOB_SPAWN")).count();
            int bosses = (int) snapshot.spawnPoints().stream().filter(point -> point.type().equals("BOSS_SPAWN")).count();
            boolean spawn = snapshot.playerSpawn() != null;
            if (!exists) return CompletableFuture.completedFuture(new DungeonValidation(id, false, false, spawn, mobs, bosses, false));
            return game.getSMPWorldManager().loadWorld(snapshot.templateWorldName())
                    .handle((world, error) -> new DungeonValidation(id, true, error == null, spawn, mobs, bosses,
                            error == null && spawn && bosses > 0));
        });
    }

    public record DungeonValidation(String dungeonId, boolean templateExists, boolean worldLoadable, boolean spawnExists,
                                    int mobSpawns, int bossSpawns, boolean ready) { }

    public record DungeonMobValidation(int checked, int repaired, int invalid, int dirtyNames, List<String> unknownAliases,
                                       boolean entityTypesValid, boolean displayNamesClean, boolean aliasesValid,
                                       boolean pdcHealthSource, boolean exactNametagFormat) { }

    public DungeonBossValidation validateDefaultDungeonBosses() {
        ConfigurationSection templates = bossTemplateSection();
        boolean pirateRegistered = bossTypeExists("pirate_captain_blackwake");
        boolean sacredRegistered = bossTypeExists("sacred_guardian_aurelius");
        boolean pirateDungeon = bossBelongsToDungeon("pirate_captain_blackwake", "piratedungeon");
        boolean sacredDungeon = bossBelongsToDungeon("sacred_guardian_aurelius", "sacreddungeon");
        List<String> missingSpawns = new ArrayList<>();
        if (loadLocation(path("piratedungeon") + ".bosses.pirate_captain_blackwake.location") == null) missingSpawns.add("piratedungeon:pirate_captain_blackwake");
        if (loadLocation(path("sacreddungeon") + ".bosses.sacred_guardian_aurelius.location") == null) missingSpawns.add("sacreddungeon:sacred_guardian_aurelius");
        boolean entityTypes = validBossEntityType(templates, "pirate_captain_blackwake", EntityType.HUSK)
                && validBossEntityType(templates, "sacred_guardian_aurelius", EntityType.ZOMBIE);
        boolean nametags = templates != null
                && templates.getString("pirate_captain_blackwake.nametag", "").equals("&8[&7Lv.75&8] &cCaptain Blackwake &a%current_health%/%max_health%&c\u2764")
                && templates.getString("sacred_guardian_aurelius.nametag", "").equals("&8[&7Lv.90&8] &cSacred Guardian Aurelius &a%current_health%/%max_health%&c\u2764");
        boolean bossbars = templates != null
                && templates.getString("pirate_captain_blackwake.bossbar", "").equals("&cCaptain Blackwake &7- &a%current_health%/%max_health%\u2764")
                && templates.getString("sacred_guardian_aurelius.bossbar", "").equals("&eSacred Guardian Aurelius &7- &a%current_health%/%max_health%\u2764");
        boolean abilities = validBossAbilities(templates, "pirate_captain_blackwake",
                List.of("blackwake_cleave", "cannon_barrage", "summon_crew", "tide_pull", "enrage"))
                && validBossAbilities(templates, "sacred_guardian_aurelius",
                List.of("radiant_slam", "holy_beam", "sacred_circle", "summon_temple_spirits", "divine_shield", "enrage"));
        boolean cooldowns = templates != null
                && templates.getDouble("pirate_captain_blackwake.abilities.blackwake_cleave.cooldown-seconds", 0) == 8.0D
                && templates.getDouble("pirate_captain_blackwake.abilities.cannon_barrage.cooldown-seconds", 0) == 18.0D
                && templates.getDouble("pirate_captain_blackwake.abilities.summon_crew.cooldown-seconds", 0) == 25.0D
                && templates.getDouble("pirate_captain_blackwake.abilities.tide_pull.cooldown-seconds", 0) == 22.0D
                && templates.getDouble("sacred_guardian_aurelius.abilities.radiant_slam.cooldown-seconds", 0) == 10.0D
                && templates.getDouble("sacred_guardian_aurelius.abilities.holy_beam.cooldown-seconds", 0) == 16.0D
                && templates.getDouble("sacred_guardian_aurelius.abilities.sacred_circle.cooldown-seconds", 0) == 22.0D
                && templates.getDouble("sacred_guardian_aurelius.abilities.summon_temple_spirits.cooldown-seconds", 0) == 28.0D;
        boolean minions = validMinionAliases(List.of("pirate", "pirate_archer", "pirate_brute"))
                && validMinionAliases(List.of("temple_spirit", "sacred_minion", "guardian"));
        boolean pdcHealth = smpMobCurrentHealthKey != null && smpMobMaxHealthKey != null && smpMobTypeKey != null
                && smpDungeonIdKey != null && smpDungeonRunIdKey != null && smpBossIdKey != null
                && smpMobDisplayNameKey != null && smpMobLevelKey != null && smpMobDefenseKey != null && smpMobDamageKey != null;
        return new DungeonBossValidation(pirateRegistered, pirateDungeon, sacredRegistered, sacredDungeon,
                pirateRegistered && pirateDungeon && sacredRegistered && sacredDungeon, List.copyOf(missingSpawns),
                entityTypes, nametags, pdcHealth, bossbars, abilities, cooldowns, minions,
                true, true, true, true, true, true);
    }

    public BossHealthValidation validateBossHealthScaling() {
        double normalLevel15 = healthScaler.normalHealth(15);
        double minimumMultiplier = Math.max(1.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getDouble("bosses.dungeon.minimumOverNormalMultiplier", 8.0D));
        boolean allBossesMeetMinimum = true;
        ConfigurationSection dungeons = dataManager.getData().getConfigurationSection("dungeons");
        if (dungeons != null) {
            for (String dungeon : dungeons.getKeys(false)) {
                ConfigurationSection bosses = dataManager.getData().getConfigurationSection(path(dungeon) + ".bosses");
                if (bosses == null) continue;
                for (String key : bosses.getKeys(false)) {
                    int level = Math.max(1, bosses.getInt(key + ".level", 1));
                    double configured = Math.max(0.0D, bosses.getDouble(key + ".health", 0.0D));
                    double normal = healthScaler.normalHealth(level);
                    if (healthScaler.dungeonBossHealth(level, configured) + 0.01D < normal * minimumMultiplier) {
                        allBossesMeetMinimum = false;
                    }
                }
            }
        }
        double sampleBoss = healthScaler.dungeonBossHealth(15, 0.0D);
        return new BossHealthValidation(normalLevel15, sampleBoss, allBossesMeetMinimum,
                smpMobCurrentHealthKey != null && smpMobMaxHealthKey != null && smpMobTypeKey != null
                        && smpBossKey != null,
                true, true);
    }

    public record BossHealthValidation(double normalLevel15Health, double dungeonBossLevel15Health,
                                       boolean allBossesMeetMinimum, boolean pdcHealthSource,
                                       boolean safeBukkitHealth, boolean pdcNametagAndBossbar) {
    }

    private boolean validBossEntityType(ConfigurationSection templates, String id, EntityType expected) {
        if (templates == null) return false;
        try {
            return EntityType.valueOf(templates.getString(id + ".type", "").toUpperCase(Locale.ROOT)) == expected;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean validBossAbilities(ConfigurationSection templates, String id, List<String> abilities) {
        if (templates == null) return false;
        for (String ability : abilities) if (!templates.getBoolean(id + ".abilities." + ability + ".registered", false)) return false;
        return true;
    }

    private boolean validMinionAliases(List<String> aliases) {
        for (String alias : aliases) {
            DungeonMobTemplateRegistry.DungeonMobTemplate template = DungeonMobTemplateRegistry.resolve(alias);
            if (template == null || !template.entityType().isAlive()) return false;
        }
        return true;
    }

    public record DungeonBossValidation(boolean pirateRegistered, boolean pirateDungeon, boolean sacredRegistered,
                                        boolean sacredDungeon, boolean commandUsable, List<String> missingSpawns,
                                        boolean entityTypesValid, boolean nametagsValid, boolean pdcHealthSource,
                                        boolean bossbarsValid, boolean abilitiesRegistered, boolean cooldownsValid,
                                        boolean minionsValid, boolean missingDropsSkipped, boolean duplicateSpawnBlocked,
                                        boolean resetValid, boolean minionsClear, boolean noYamlCreated,
                                        boolean noAddedCodeComments) { }

    private void logDungeonLoaded(String dungeonId) {
        String canonicalId = canonicalDungeonId(dungeonId);
        plugin.getLogger().info("[SMP] Loaded dungeon " + canonicalId + " enabled=" + isEnabled(canonicalId)
                + " source=" + dungeonSource(canonicalId));
    }

    private DungeonDefinition defaultDefinition(String dungeonId) {
        String canonicalId = canonicalDungeonId(dungeonId);
        return new DungeonDefinition(canonicalId, pretty(canonicalId), Material.ENDER_EYE,
                "A dangerous dungeon.", 1, "Dungeon Boss", Map.of("combat", 1), "", false, 5, false);
    }

    private DungeonRun startDungeonRun(String dungeon, Player firstPlayer) {
        DungeonRun run = new DungeonRun(dungeon);
        run.state = DungeonState.ACTIVE;
        if (firstPlayer != null) {run.activePlayers.add(firstPlayer.getUniqueId());run.participants.add(firstPlayer.getUniqueId());run.lastLocations.put(firstPlayer.getUniqueId(),firstPlayer.getLocation().clone());}
        activeRuns.put(canonicalDungeonId(dungeon), run);
        duplicateBossSpawnWarnings.removeIf(value -> value.startsWith(canonicalDungeonId(dungeon) + ":"));
        dataManager.getData().set(path(dungeon) + ".run.active", true);
        dataManager.getData().set(path(dungeon) + ".run.state", DungeonState.ACTIVE.name());
        dataManager.save();
        persistRunAsync(run);
        return run;
    }

    private DungeonRun startEmptyRun(String dungeon) {
        DungeonRun run = activeRun(dungeon);
        if (run == null) {
            run = new DungeonRun(dungeon);
            activeRuns.put(canonicalDungeonId(dungeon), run);
        }
        return run;
    }

    private void failDungeonRun(String dungeon, String message) {
        DungeonRun run = activeRun(dungeon);
        if (run != null) {
            for (UUID uuid : new HashSet<>(run.activePlayers)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    if (message != null && !message.isBlank()) player.sendMessage(CC.color(message));
                    returnDungeonPlayer(player, dungeon);
                }
            }
        }
        beginRestore(dungeon);
    }

    private void beginRestore(String dungeon) {
        String key = dungeon.toLowerCase(Locale.ROOT);
        DungeonRun run = startEmptyRun(dungeon);
        run.state = DungeonState.RESTORING;
        if(plugin instanceof club.abelix.games.fakevival.Game game)for(UUID uuid:new HashSet<>(run.spectators)){Player staff=Bukkit.getPlayer(uuid);if(staff!=null)game.getDungeonSpectateService().leave(staff);}run.spectators.clear();
        run.expiresAt=0;persistRunAsync(run);
        dataManager.getData().set(path(dungeon) + ".run.active", false);
        dataManager.getData().set(path(dungeon) + ".run.state", DungeonState.RESTORING.name());
        dataManager.save();
        for (UUID uuid : new HashSet<>(run.activePlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) player.sendMessage(CC.color("&eThe dungeon is restoring..."));
        }
        if (!restoringDungeons.add(key)) return;
        clearDungeonEntities(dungeon);
        Bukkit.getScheduler().runTaskLater(plugin, () -> finishRestore(dungeon), 20L * 10L);
    }

    private void finishRestore(String dungeon) {
        String key = dungeon.toLowerCase(Locale.ROOT);
        restoringDungeons.remove(key);
        resetDungeon(dungeon);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(CC.color("&aThe dungeon has reopened."));
        }
    }

    private void recoverDungeonRunStates() {
        if(!dataManager.isMySqlEnabled())return;String server=serverId();CompletableFuture.supplyAsync(()->{List<RecoveredRun> rows=new ArrayList<>();try(Connection c=dataManager.getConnection();PreparedStatement s=c.prepareStatement("SELECT * FROM smp_dungeon_runs WHERE server_id=? AND state IN ('ACTIVE','EMPTY_WAITING_FOR_REJOIN')")){s.setString(1,server);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new RecoveredRun(r.getString("run_id"),r.getString("dungeon_id"),r.getString("instance_world"),r.getString("state"),r.getString("participants_json"),r.getLong("created_at"),r.getLong("last_player_left_at"),r.getLong("expires_at"),r.getBoolean("boss_alive")));}}catch(SQLException e){throw new java.util.concurrent.CompletionException(e);}return rows;}).whenComplete((rows,error)->Bukkit.getScheduler().runTask(plugin,()->{if(error!=null){plugin.getLogger().warning("[SMP] Could not recover dungeon runs: "+rootMessage(error));return;}long now=System.currentTimeMillis();for(RecoveredRun row:rows){World world=row.instanceId==null?null:worldProvider.getWorld(row.instanceId);if(row.expiresAt>0&&row.expiresAt<=now||world==null){expireRecoveredRun(row.runId);continue;}DungeonRun run=new DungeonRun(row.dungeonId,row.runId);run.instanceId=row.instanceId;run.worldId=world.getName();run.state=parseRunState(row.state);run.lastPlayerLeftAt=row.lastLeft;run.expiresAt=row.expiresAt;run.bossAlive=row.bossAlive;try{String[] ids=gson.fromJson(row.participantsJson,String[].class);if(ids!=null)for(String id:ids)run.participants.add(UUID.fromString(id));}catch(Exception ignored){}activeRuns.put(canonicalDungeonId(run.dungeon),run);if(lootChestManager!=null)lootChestManager.spawnForRun(run.dungeon,run.id,world,false);if(trapManager!=null)trapManager.registerRun(run.dungeon,run.id,world,false);plugin.getLogger().info("[SMP] Recovered dungeon run "+run.id+" state="+run.state+" world="+world.getName());}}));
    }
    private DungeonState parseRunState(String value){try{return DungeonState.valueOf(value);}catch(Exception ignored){return DungeonState.EXPIRED;}}
    private void expireRecoveredRun(String runId){CompletableFuture.runAsync(()->{try(Connection c=dataManager.getConnection();PreparedStatement s=c.prepareStatement("UPDATE smp_dungeon_runs SET state='EXPIRED' WHERE run_id=?")){s.setString(1,runId);s.executeUpdate();}catch(SQLException ignored){}});}
    private record RecoveredRun(String runId,String dungeonId,String instanceId,String state,String participantsJson,long createdAt,long lastLeft,long expiresAt,boolean bossAlive){}

    public void handleParticipantQuit(Player player){if(player==null)return;for(DungeonRun run:activeRuns.values()){if(!run.activePlayers.remove(player.getUniqueId()))continue;Location safe=isActiveDungeonInstanceWorld(player.getWorld())?player.getLocation().clone():run.lastLocations.get(player.getUniqueId());if(safe!=null){run.lastLocations.put(player.getUniqueId(),safe);persistPlayerLocationAsync(run,player.getUniqueId(),safe,false);}if(run.activePlayers.isEmpty()&&run.rejoinable()){long timeout=Math.max(30,club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("dungeons.empty-timeout-seconds",300));run.state=DungeonState.EMPTY_WAITING_FOR_REJOIN;run.lastPlayerLeftAt=System.currentTimeMillis();run.expiresAt=run.lastPlayerLeftAt+timeout*1000L;}persistRunAsync(run);break;}}

    public boolean isSpectating(Player player){if(player==null)return false;return activeRuns.values().stream().anyMatch(run->run.spectators.contains(player.getUniqueId()));}
    public void addSpectator(String runId,Player staff){DungeonRun run=runById(runId);if(run!=null){run.spectators.add(staff.getUniqueId());persistRunAsync(run);}}
    public void removeSpectator(Player staff){for(DungeonRun run:activeRuns.values())if(run.spectators.remove(staff.getUniqueId()))persistRunAsync(run);}
    private DungeonRun runById(String id){return activeRuns.values().stream().filter(run->run.id.equalsIgnoreCase(id)).findFirst().orElse(null);}

    private void tickDungeonRunPersistence(){long now=System.currentTimeMillis();for(DungeonRun run:new ArrayList<>(activeRuns.values())){for(UUID uuid:new HashSet<>(run.activePlayers)){Player player=Bukkit.getPlayer(uuid);if(player==null||!player.isOnline()){run.activePlayers.remove(uuid);continue;}Location location=player.getLocation().clone();run.lastLocations.put(uuid,location);persistPlayerLocationAsync(run,uuid,location,true);}if(run.activePlayers.isEmpty()&&run.state==DungeonState.ACTIVE){long timeout=Math.max(30,club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("dungeons.empty-timeout-seconds",300));run.state=DungeonState.EMPTY_WAITING_FOR_REJOIN;run.lastPlayerLeftAt=now;run.expiresAt=now+timeout*1000L;}if(run.state==DungeonState.EMPTY_WAITING_FOR_REJOIN&&run.expiresAt>0&&now>=run.expiresAt){run.state=DungeonState.EXPIRED;persistRunAsync(run);plugin.getLogger().info("[SMP] Dungeon run "+run.id+" expired after being empty for "+Math.max(1,(now-run.lastPlayerLeftAt)/1000)+"s.");beginRestore(run.dungeon);continue;}persistRunAsync(run);}}

    private void persistRunAsync(DungeonRun run){if(run==null||!dataManager.isMySqlEnabled()||runPersistence.isShutdown())return;String participants=gson.toJson(run.participants.stream().map(UUID::toString).toList()),spectators=gson.toJson(run.spectators.stream().map(UUID::toString).toList()),server=serverId(),boss=dataManager.getData().getString(path(run.dungeon)+".boss.name","Dungeon Boss"),instance=run.instanceId,state=run.state.name();boolean bossAlive=run.bossAlive,completed=run.completed;long created=run.createdAt,lastLeft=run.lastPlayerLeftAt,expires=run.expiresAt;CompletableFuture.runAsync(()->{try(Connection c=dataManager.getConnection();PreparedStatement s=c.prepareStatement("INSERT INTO smp_dungeon_runs(run_id,dungeon_id,server_id,instance_world,state,participants_json,spectators_json,boss_name,boss_alive,completed,created_at,last_player_left_at,expires_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE instance_world=VALUES(instance_world),state=VALUES(state),participants_json=VALUES(participants_json),spectators_json=VALUES(spectators_json),boss_name=VALUES(boss_name),boss_alive=VALUES(boss_alive),completed=VALUES(completed),last_player_left_at=VALUES(last_player_left_at),expires_at=VALUES(expires_at)")){s.setString(1,run.id);s.setString(2,run.dungeon);s.setString(3,server);s.setString(4,instance);s.setString(5,state);s.setString(6,participants);s.setString(7,spectators);s.setString(8,boss);s.setBoolean(9,bossAlive);s.setBoolean(10,completed);s.setLong(11,created);s.setLong(12,lastLeft);s.setLong(13,expires);s.executeUpdate();}catch(SQLException e){plugin.getLogger().warning("[SMP] Could not persist dungeon run: "+e.getMessage());}},runPersistence);}
    public CompletableFuture<Void> shutdown(){for(DungeonRun run:activeRuns.values())persistRunAsync(run);CompletableFuture<Void> flushed=CompletableFuture.runAsync(()->{},runPersistence);flushed.whenComplete((ignored,error)->runPersistence.shutdown());return flushed;}
    private void persistPlayerLocationAsync(DungeonRun run,UUID uuid,Location location,boolean online){if(!dataManager.isMySqlEnabled()||location==null||location.getWorld()==null)return;String server=serverId(),world=location.getWorld().getName();double x=location.getX(),y=location.getY(),z=location.getZ(),health=0,maxHealth=0;Player tracked=Bukkit.getPlayer(uuid);if(tracked!=null&&plugin instanceof club.abelix.games.fakevival.Game game&&game.getMmoStatService()!=null){var snapshot=game.getMmoStatService().snapshot(tracked);health=snapshot.health();maxHealth=snapshot.maxHealth();}float yaw=location.getYaw(),pitch=location.getPitch();double savedHealth=health,savedMax=maxHealth;CompletableFuture.runAsync(()->{try(Connection c=dataManager.getConnection();PreparedStatement s=c.prepareStatement("INSERT INTO smp_dungeon_run_players(uuid,run_id,server_id,world,x,y,z,yaw,pitch,online,updated_at,current_health,max_health) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE server_id=VALUES(server_id),world=VALUES(world),x=VALUES(x),y=VALUES(y),z=VALUES(z),yaw=VALUES(yaw),pitch=VALUES(pitch),online=VALUES(online),updated_at=VALUES(updated_at),current_health=VALUES(current_health),max_health=VALUES(max_health)")){s.setString(1,uuid.toString());s.setString(2,run.id);s.setString(3,server);s.setString(4,world);s.setDouble(5,x);s.setDouble(6,y);s.setDouble(7,z);s.setFloat(8,yaw);s.setFloat(9,pitch);s.setBoolean(10,online);s.setLong(11,System.currentTimeMillis());s.setDouble(12,savedHealth);s.setDouble(13,savedMax);s.executeUpdate();}catch(SQLException e){plugin.getLogger().warning("[SMP] Could not save dungeon participant location: "+e.getMessage());}});}
    private String serverId(){return plugin instanceof club.abelix.games.fakevival.Game game&&game.getNetworkService()!=null?game.getNetworkService().getCurrentServerId():"unknown";}

    public List<RunView> runViews(){return activeRuns.values().stream().filter(run->run.state!=DungeonState.AVAILABLE&&run.state!=DungeonState.EXPIRED).map(run->new RunView(run.id,run.dungeon,serverId(),run.worldId,run.state.name(),run.activePlayers.size(),dataManager.getData().getString(path(run.dungeon)+".boss.name","Dungeon Boss"),run.createdAt,run.expiresAt,List.copyOf(run.participants))).toList();}
    public RunView runView(String id){return runViews().stream().filter(run->run.runId().equalsIgnoreCase(id)).findFirst().orElse(null);}
    public boolean forceCloseRun(String id){DungeonRun run=runById(id);if(run==null)return false;run.state=DungeonState.EXPIRED;persistRunAsync(run);beginRestore(run.dungeon);return true;}
    public record RunView(String runId,String dungeonId,String serverId,String instanceWorld,String state,int onlinePlayers,String bossName,long createdAt,long expiresAt,List<UUID> participants){}

    public List<String> debugMobLines() {
        List<String> lines = new ArrayList<>();
        ConfigurationSection dungeons = dataManager.getData().getConfigurationSection("dungeons");
        if (dungeons != null) for (String dungeon : dungeons.getKeys(false)) cleanupTrackedDungeonMobs(dungeon);
        lines.add("&eTotal dungeon mobs alive: &f" + countAllAliveDungeonMobs());
        if (dungeons == null) return lines;
        for (String dungeon : dungeons.getKeys(false)) {
            lines.add("&6Dungeon " + dungeon + ": &f" + countAliveDungeonMobs(dungeon) + " alive");
            ConfigurationSection mobs = dataManager.getData().getConfigurationSection(path(dungeon) + ".mobs");
            if (mobs == null) continue;
            for (String spawn : mobs.getKeys(false)) {
                int configured = configuredMobCount(dungeon, spawn);
                int alive = countAliveForSpawn(dungeon, spawn, false);
                int missing = Math.max(0, configured - alive);
                int duplicates = Math.max(0, alive - configured);
                lines.add("&eSpawn " + spawn + ": &a" + alive + "/" + configured
                        + " alive &7(missing: " + missing + ", duplicates: " + duplicates + ")");
            }
        }
        return lines;
    }

    private void spawnDungeonMobs(String name) {
        if (dungeonState(name) != DungeonState.ACTIVE) return;
        if (!dungeonSpawnTickRunning.compareAndSet(false, true)) return;
        boolean stillMissing = false;
        boolean capReached = false;
        try {
            cleanupTrackedDungeonMobs(name);
            int batch = availableSpawnBatch();
            int totalCap = setting("dungeons.max-mobs-total", 180);
            int dungeonCap = cappedSetting("dungeons.max-mobs-per-dungeon", 150, 150);
            int totalAlive = countAllAliveDungeonMobs();
            int dungeonAlive = countAliveDungeonMobs(name);
            ConfigurationSection mobs = dataManager.getData().getConfigurationSection(path(name) + ".mobs");
            if (mobs != null) {
                outer:
                for (String key : mobs.getKeys(false)) {
                    int configured = configuredMobCount(name, key);
                    int alive = countAliveForSpawn(name, key, false);
                    for (int index = 0; index < configured; index++) {
                        if (hasAliveSpawnIndex(name, key, false, index)) continue;
                        if (isClearedSpawn(name, key, index)) continue;
                        if (batch <= 0 || totalAlive >= totalCap || dungeonAlive >= dungeonCap
                                || !canSpawnDungeonAt(name, key, false)) {
                            stillMissing = true;
                            capReached = totalAlive >= totalCap || dungeonAlive >= dungeonCap;
                            break outer;
                        }
                        if (spawnMob(name, key, index)) {
                            batch--;
                            totalAlive++;
                            dungeonAlive++;
                            recordSpawnInRateWindow();
                        }
                    }
                    if (alive < configured && countAliveForSpawn(name, key, false) < configured) stillMissing = true;
                }
            }
            ConfigurationSection bosses = dataManager.getData().getConfigurationSection(path(name) + ".bosses");
            if (bosses != null) {
                for (String key : bosses.getKeys(false)) {
                    if (bossSlotClosed(name)) continue;
                    if (hasAliveSpawnIndex(name, key, true, 0)) continue;
                    if (batch <= 0 || totalAlive >= totalCap || dungeonAlive >= dungeonCap
                            || !canSpawnDungeonAt(name, key, true)) {
                        stillMissing = true;
                        capReached |= totalAlive >= totalCap || dungeonAlive >= dungeonCap;
                        continue;
                    }
                    if (spawnBoss(name, key, 0)) { batch--; totalAlive++; dungeonAlive++; recordSpawnInRateWindow(); }
                    else stillMissing = true;
                }
            } else if (!bossSlotClosed(name) && !hasAliveSpawnIndex(name, "legacy_boss", true, 0)) {
                if (batch > 0 && totalAlive < totalCap && dungeonAlive < dungeonCap && spawnLegacyBoss(name, 0)) recordSpawnInRateWindow();
                else if (batch <= 0) stillMissing = true;
            }
        } finally {
            dungeonSpawnTickRunning.set(false);
        }
        if (stillMissing && !capReached && isEnabled(name) && dataManager.getData().getBoolean(path(name) + ".run.active", false)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> spawnDungeonMobs(name), 5L);
        }
    }


    private LivingEntity spawnOptimizedDungeonMob(String mobId, Location location, EntityType type, String displayName,
                                                 double maxMmoHealth, double attackDamage, int level) {
        if (plugin instanceof club.abelix.games.fakevival.Game game && game.getSmpMobEngine() != null) {
            return game.getSmpMobEngine().spawnMob(mobId, location, type, displayName, maxMmoHealth, attackDamage, level, java.util.List.of(), true);
        }
        Entity entity = location.getWorld().spawnEntity(location, type);
        return entity instanceof LivingEntity living ? living : null;
    }

    private boolean spawnLegacyBoss(String name, int index) {
        DungeonRun run = bossSpawnRun(name);
        if (run == null || !canSpawnDungeonBoss(name, run)) return false;
        Location bossLocation = loadLocation(path(name) + ".boss.location");
        if (bossLocation == null) bossLocation = loadLocation(path(name) + ".spawn");
        if (bossLocation == null || bossLocation.getWorld() == null) {
            plugin.getLogger().warning("Dungeon boss spawn skipped for " + name + ": boss location/world is not loaded.");
            return false;
        }
        try {
            String bossType = dataManager.getData().getString(path(name) + ".boss.type", "ZOMBIE");
            if (!bossLocation.getWorld().isChunkLoaded(bossLocation.getBlockX() >> 4, bossLocation.getBlockZ() >> 4)) return false;
            String bossName = dataManager.getData().getString(path(name) + ".boss.name", "Dungeon Boss");
            int level = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("dungeons.default-boss-level", 10);
            LivingEntity living = spawnOptimizedDungeonMob("dungeon_" + name + "_legacy_boss", bossLocation, EntityType.valueOf(bossType.toUpperCase()), bossName, 10000.0D + Math.max(0, level) * 350.0D, 12.0D + Math.max(0, level) * 1.5D, level);
            if (living != null) {
                tagDungeonEntity(living, name, "legacy_boss", "legacy_boss", index, level, bossName, true);
                living.setCustomNameVisible(true);
                living.setRemoveWhenFarAway(false);
                makeDungeonMobSafe(living);
                applyDungeonBossStats(living, level, "normal");
                updateDisplay(living);
                updateBossBar(living);
                trackedDungeonMobs.add(living.getUniqueId());
                markBossAlive(name, living.getUniqueId());
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not spawn legacy dungeon boss for " + name + ": " + e.getMessage());
        }
        return false;
    }

    private boolean bossSlotClosed(String dungeon) {
        DungeonRun run = activeRun(dungeon);
        boolean closed = run != null && (run.bossKilled || run.bossAlive || validBossEntity(run.bossEntityUuid));
        if (closed && duplicateBossSpawnWarnings.add(dungeon.toLowerCase(Locale.ROOT) + ":" + run.id)) {
            plugin.getLogger().warning("[Dungeon] Blocked duplicate boss spawn for dungeon " + dungeon + " run " + run.id);
        }
        return closed;
    }

    private boolean spawnBoss(String name, String key, int index) {
        DungeonRun run = bossSpawnRun(name);
        if (run == null || !canSpawnDungeonBoss(name, run)) return false;
        Location bossLocation = loadLocation(path(name) + ".bosses." + key + ".location");
        if (bossLocation == null || bossLocation.getWorld() == null) {
            plugin.getLogger().warning("Dungeon boss spawn skipped for " + name + "/" + key + ": location/world is not loaded.");
            return false;
        }
        try {
            String bossType = dataManager.getData().getString(path(name) + ".bosses." + key + ".type", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.bosses." + key + ".type", "ZOMBIE"));
            if (!bossLocation.getWorld().isChunkLoaded(bossLocation.getBlockX() >> 4, bossLocation.getBlockZ() >> 4)) return false;
            ConfigurationSection templates=bossTemplateSection();
            ConfigurationSection template=templates==null?null:templates.getConfigurationSection(canonicalMobId(key));
            String bossName = DungeonMobTemplateRegistry.cleanDisplayName(dataManager.getData().getString(path(name) + ".bosses." + key + ".name", template==null?pretty(key):template.getString("name",pretty(key))),null);
            boolean tyrant=normalizeMobId(key).equals("tyrant");
            int level = tyrant?75:dataManager.getData().getInt(path(name) + ".bosses." + key + ".level", template==null?club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("dungeons.bosses." + key + ".level", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("dungeons.default-boss-level", 10)):template.getInt("level",10));
            if(tyrant)bossName="&4Tyrant";
            double configuredHealth=tyrant?250000.0D:dataManager.getData().getDouble(path(name)+".bosses."+key+".health",template==null?10000.0D+Math.max(0,level)*350.0D:template.getDouble("health",10000.0D));
            double configuredDefense=tyrant?350.0D:dataManager.getData().getDouble(path(name)+".bosses."+key+".defense",template==null?25.0D+Math.max(0,level)*3.0D:template.getDouble("defense",0.0D));
            double configuredDamage=tyrant?450.0D:dataManager.getData().getDouble(path(name)+".bosses."+key+".damage",template==null?12.0D+Math.max(0,level)*1.5D:template.getDouble("damage",10.0D));
            double finalHealth = healthScaler.dungeonBossHealth(level, configuredHealth);
            LivingEntity living = spawnOptimizedDungeonMob("dungeon_" + name + "_boss_" + key, bossLocation, EntityType.valueOf(bossType.toUpperCase(Locale.ROOT)), bossName, finalHealth, configuredDamage, level);
            if (living != null) {
                tagDungeonEntity(living, name, key, key, index, level, bossName, true);
                living.setCustomNameVisible(true);
                living.setRemoveWhenFarAway(false);
                makeDungeonMobSafe(living);
                String tier = dataManager.getData().getString(path(name) + ".bosses." + key + ".tier", "normal");
                applyDungeonBossStats(living, level, tier);
                applyBossStats(living,finalHealth,configuredDefense,configuredDamage);
                updateDisplay(living);
                updateBossBar(living);
                trackedDungeonMobs.add(living.getUniqueId());
                markBossAlive(name, living.getUniqueId());
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not spawn dungeon boss " + name + "/" + key + ": " + e.getMessage());
        }
        return false;
    }

    private boolean spawnMob(String name, String key, int index) {
        Location location = loadLocation(path(name) + ".mobs." + key);
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Dungeon mob spawn skipped for " + name + "/" + key + ": location/world is not loaded.");
            return false;
        }
        try {
            String fallbackType = fallbackMobType(key);
            String mobPath = path(name) + ".mobs." + key;
            String configuredType = dataManager.getData().getString(mobPath + ".entity_type",
                    dataManager.getData().getString(mobPath + ".type", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.mobs." + key + ".type", fallbackType)));
            if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return false;
            DungeonMobTemplateRegistry.DungeonMobTemplate template = DungeonMobTemplateRegistry.resolveDefinition(key,
                    dataManager.getData().getString(mobPath + ".mob_id"), configuredType);
            if (template == null) {
                plugin.getLogger().warning("[SMP] Unknown dungeon mob alias '" + DungeonMobTemplateRegistry.withoutInternalSuffix(key)
                        + "' for dungeon '" + name + "'. Use /dungeon mob aliases.");
                return false;
            }
            String mobName = DungeonMobTemplateRegistry.cleanDisplayName(dataManager.getData().getString(mobPath + ".mob_display_name",
                    dataManager.getData().getString(mobPath + ".name", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.mobs." + key + ".name", ""))), template);
            int level = dataManager.getData().getInt(path(name) + ".mobs." + key + ".level", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("dungeons.mobs." + key + ".level", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("dungeons.default-mob-level", 1)));
            double maxHealth = healthScaler.normalHealth(level);
            LivingEntity living = spawnOptimizedDungeonMob(template.mobId(), location, resolveDungeonEntityType(name, mobPath, configuredType), mobName, maxHealth, Math.max(1.0D, 4.0D + level), level);
            if (living != null) {
                tagDungeonEntity(living, name, key, template.mobId(), index, level, mobName, false);
                living.setCustomNameVisible(true);
                living.setRemoveWhenFarAway(false);
                makeDungeonMobSafe(living);
                writeDungeonHealth(living, maxHealth, maxHealth);
                syncDungeonBukkitHealth(living, maxHealth, maxHealth);
                updateDisplay(living);
                trackedDungeonMobs.add(living.getUniqueId());
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not spawn dungeon mob " + name + "/" + key + ": " + e.getMessage());
        }
        return false;
    }

    private boolean canSpawnDungeonAt(String dungeon, String spawnId, boolean boss) {
        String base = path(dungeon) + (boss ? ".bosses." : ".mobs.") + spawnId;
        Location location = loadLocation(base + (boss ? ".location" : ""));
        return canSpawnDungeonAt(dungeon, spawnId, boss, location);
    }

    private boolean canSpawnDungeonAt(String dungeon, String spawnId, boolean boss, Location location) {
        if (location == null || location.getWorld() == null) return false;
        int chunkCap = Math.min(ABSOLUTE_MAX_MOBS_PER_CHUNK, setting("dungeons.max-mobs-per-chunk", 12));
        if (countDungeonMobsInChunk(location) >= chunkCap) return false;
        String roomId = roomId(dungeon, spawnId, boss);
        int roomCap = boss ? cappedSetting("dungeons.max-boss-room-mobs", 50, 50)
                : cappedSetting("dungeons.max-mobs-per-room", 40, 40);
        if (countDungeonMobsInRoom(dungeon, roomId) >= roomCap) return false;
        return activeMobLimitAllows(dungeon, location);
    }

    private int countDungeonMobsInChunk(Location location) {
        int count = 0;
        for (Entity entity : location.getChunk().getEntities()) {
            if (entity instanceof LivingEntity living && !living.isDead() && living.isValid() && isDungeonEntity(living)) count++;
        }
        return count;
    }

    private int countDungeonMobsInRoom(String dungeon, String room) {
        int count = 0;
        for (UUID uuid : trackedDungeonMobs) {
            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) continue;
            if (!dungeon.equalsIgnoreCase(living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING))) continue;
            if (room.equalsIgnoreCase(roomIdForEntity(dungeon, living))) count++;
        }
        return count;
    }

    private String roomId(String dungeon, String spawnId, boolean boss) {
        if ("boss_minion".equalsIgnoreCase(spawnId)) return "boss";
        String base = path(dungeon) + (boss ? ".bosses." : ".mobs.") + spawnId;
        return dataManager.getData().getString(base + ".room-id",
                dataManager.getData().getString(base + ".room", boss ? "boss" : spawnId));
    }

    private String roomIdForEntity(String dungeon, LivingEntity living) {
        if (living.getPersistentDataContainer().has(temporaryMinionKey, PersistentDataType.BYTE)) return "boss";
        String spawn = living.getPersistentDataContainer().getOrDefault(spawnIdKey, PersistentDataType.STRING, "unknown");
        return roomId(dungeon, spawn, living.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING));
    }

    private boolean activeMobLimitAllows(String dungeon, Location location) {
        int cap = setting("dungeons.max-active-mobs-near-player", 80);
        for (Player player : location.getWorld().getPlayers()) {
            if (player.isDead() || player.getGameMode().name().contains("SPECTATOR")
                    || player.getLocation().distanceSquared(location) > ACTIVE_MOB_RANGE * ACTIVE_MOB_RANGE) continue;
            int active = 0;
            for (UUID uuid : trackedDungeonMobs) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity instanceof LivingEntity living && !living.isDead() && living.isValid()
                        && dungeon.equalsIgnoreCase(living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING))
                        && living.getWorld().equals(player.getWorld())
                        && living.getLocation().distanceSquared(player.getLocation()) <= ACTIVE_MOB_RANGE * ACTIVE_MOB_RANGE) active++;
            }
            if (active >= cap) return false;
        }
        return true;
    }

    public void handleDungeonMobDeath(LivingEntity entity, Player killer) {
        if (entity == null || !isDungeonEntity(entity)) return;
        String dungeon = entity.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING);
        String spawn = entity.getPersistentDataContainer().get(spawnIdKey, PersistentDataType.STRING);
        int index = entity.getPersistentDataContainer().getOrDefault(spawnIndexKey, PersistentDataType.INTEGER, 0);
        trackedDungeonMobs.remove(entity.getUniqueId());
        removeBossBar(entity.getUniqueId());
        abilityCooldowns.remove(entity.getUniqueId());
        if (dungeon == null || spawn == null || entity.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING)) return;
        DungeonRun run = activeRun(dungeon);
        if (run != null && run.state == DungeonState.ACTIVE) run.markCleared(spawn, index);
    }

    private void applyDungeonBossStats(LivingEntity living, int level, String tier) {
        double health = Math.max(1.0D, 10000.0D + Math.max(0, level) * 350.0D);
        double defense = Math.max(0.0D, 25.0D + Math.max(0, level) * 3.0D);
        double damage = Math.max(1.0D, 12.0D + Math.max(0, level) * 1.5D);
        if ("elite".equalsIgnoreCase(tier)) {
            health *= 1.5D;
            defense *= 1.15D;
            damage *= 1.2D;
        } else if ("final".equalsIgnoreCase(tier)) {
            health *= 2.25D;
            defense *= 1.35D;
            damage *= 1.4D;
        }
        defense = Math.min("final".equalsIgnoreCase(tier) ? 600.0D : "elite".equalsIgnoreCase(tier) ? 400.0D : 250.0D, defense);
        writeDungeonHealth(living, health, health);
        living.getPersistentDataContainer().set(defenseKey, PersistentDataType.DOUBLE, defense);
        AttributeInstance attack = living.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(damage);
        syncDungeonBukkitHealth(living, health, health);
    }

    private void applyBossStats(LivingEntity living,double health,double defense,double damage){
        writeDungeonHealth(living,health,health);
        living.getPersistentDataContainer().set(defenseKey,PersistentDataType.DOUBLE,Math.max(0.0D,defense));
        living.getPersistentDataContainer().set(smpMobDefenseKey,PersistentDataType.DOUBLE,Math.max(0.0D,defense));
        living.getPersistentDataContainer().set(smpMobDamageKey,PersistentDataType.DOUBLE,Math.max(1.0D,damage));
        AttributeInstance attack=living.getAttribute(Attribute.ATTACK_DAMAGE);
        if(attack!=null)attack.setBaseValue(Math.max(1.0D,damage));
        syncDungeonBukkitHealth(living,health,health);
    }

    private double dungeonCurrentHealth(LivingEntity living) {
        if (isEngineMob(living)) return engineCurrentHp(living);
        return living.getPersistentDataContainer().getOrDefault(currentHealthKey, PersistentDataType.DOUBLE, living.getHealth());
    }

    private double dungeonMaxHealth(LivingEntity living) {
        if (isEngineMob(living)) return engineMaxHp(living);
        return living.getPersistentDataContainer().getOrDefault(maxHealthKey, PersistentDataType.DOUBLE, living.getMaxHealth());
    }

    private void writeDungeonHealth(LivingEntity living, double current, double max) {
        if (isEngineMob(living)) return;
        double safeMax = Math.max(1.0D, max);
        double safeCurrent = Math.clamp(current, 0.0D, safeMax);
        living.getPersistentDataContainer().set(currentHealthKey, PersistentDataType.DOUBLE, safeCurrent);
        living.getPersistentDataContainer().set(maxHealthKey, PersistentDataType.DOUBLE, safeMax);
        living.getPersistentDataContainer().set(smpMobCurrentHealthKey, PersistentDataType.DOUBLE, safeCurrent);
        living.getPersistentDataContainer().set(smpMobMaxHealthKey, PersistentDataType.DOUBLE, safeMax);
    }

    private void syncDungeonBukkitHealth(LivingEntity living, double current, double max) {
        if (isEngineMob(living)) return;
        double safeMax = Math.min(Math.max(1.0D, max), 1024.0D);
        AttributeInstance attribute = living.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null) attribute.setBaseValue(safeMax);
        else living.setMaxHealth(safeMax);
        double visual = Math.clamp((current / Math.max(1.0D, max)) * living.getMaxHealth(), 0.01D, living.getMaxHealth());
        if (!living.isDead() && current > 0.0D) living.setHealth(visual);
    }

    private DungeonRun bossSpawnRun(String dungeon) {
        DungeonRun run = activeRun(dungeon);
        if (run == null || run.state != DungeonState.ACTIVE) {
            plugin.getLogger().warning("[Dungeon] Cancelled invalid dungeon boss respawn task for " + dungeon);
            return null;
        }
        return run;
    }

    private boolean canSpawnDungeonBoss(String dungeon, DungeonRun run) {
        if (run.bossKilled || run.bossAlive || validBossEntity(run.bossEntityUuid)) {
            plugin.getLogger().warning("[Dungeon] Blocked duplicate boss spawn for dungeon " + dungeon + " run " + run.id);
            if (plugin instanceof club.abelix.games.fakevival.Game game) {
                for (UUID playerId : run.activePlayers) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) game.getCombatDebugManager().sendCombatDebug(player,
                            "&cBlocked duplicate dungeon boss spawn &7Dungeon=&e" + dungeon + " &7Run=&e" + run.id);
                }
            }
            return false;
        }
        return true;
    }

    private boolean validBossEntity(UUID uuid) {
        if (uuid == null) return false;
        Entity entity = Bukkit.getEntity(uuid);
        return entity instanceof LivingEntity living && living.isValid() && !living.isDead()
                && living.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING);
    }

    private void markBossAlive(String dungeon, UUID bossUuid) {
        DungeonRun run = activeRun(dungeon);
        if (run != null) {
            run.state = DungeonState.ACTIVE;
            run.bossEntityUuid = bossUuid;
            run.bossSpawned = true;
            run.bossAlive = true;
            run.bossKilled = false;
            persistRunAsync(run);
            plugin.getLogger().info("[Dungeon] Spawned boss for dungeon " + dungeon + " run " + run.id + " uuid=" + bossUuid);
        }
        dataManager.getData().set(path(dungeon) + ".run.state", DungeonState.ACTIVE.name());
        dataManager.save();
    }

    private boolean isClearedSpawn(String dungeon, String spawn, int index) {
        DungeonRun run = activeRun(dungeon);
        return run != null && run.isClearedSpawn(spawn, index);
    }

    private DungeonRun activeRun(String dungeon) {
        return activeRuns.get(canonicalDungeonId(dungeon));
    }

    private void resetDungeon(String dungeon) {
        clearDungeonEntities(dungeon);
        DungeonRun run = activeRun(dungeon);
        if (run != null && lootChestManager != null) lootChestManager.clearRun(run.id);
        if (run != null && trapManager != null) trapManager.unregisterRun(run.id, true);
        if(run!=null){run.state=run.completed?DungeonState.COMPLETED:DungeonState.EXPIRED;persistRunAsync(run);if(dataManager.isMySqlEnabled()){String runId=run.id;CompletableFuture.runAsync(()->{try(Connection c=dataManager.getConnection();PreparedStatement s=c.prepareStatement("DELETE FROM smp_dungeon_run_players WHERE run_id=?")){s.setString(1,runId);s.executeUpdate();}catch(SQLException e){plugin.getLogger().warning("[SMP] Could not clear dungeon rejoin records: "+e.getMessage());}});}}
        cleanupDungeonWorldInstance(run);
        activeRuns.remove(canonicalDungeonId(dungeon));
        duplicateBossSpawnWarnings.removeIf(value -> value.startsWith(canonicalDungeonId(dungeon) + ":"));
        dataManager.getData().set(path(dungeon) + ".run.active", false);
        dataManager.getData().set(path(dungeon) + ".run.state", DungeonState.AVAILABLE.name());
        dataManager.save();
    }

    private void cleanupDungeonWorldInstance(DungeonRun run) {
        if (run == null || run.instanceId == null) return;
        String instanceId = run.instanceId;
        worldProvider.unloadInstance(instanceId, false)
                .thenCompose(ignored -> worldProvider.deleteInstance(instanceId))
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        plugin.getLogger().warning("[Dungeon] Could not clean dungeon world instance " + instanceId + ": " + rootMessage(throwable));
                    } else {
                        plugin.getLogger().info("[Dungeon] Cleaned dungeon world instance " + instanceId + " using " + worldProvider.getBackendName());
                    }
                });
    }

    private void clearDungeonEntities(String dungeon) {
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity living : world.getLivingEntities()) {
                if (!isDungeonEntity(living)) continue;
                if (!dungeon.equalsIgnoreCase(living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING))) continue;
                living.remove();
                trackedDungeonMobs.remove(living.getUniqueId());
                removeBossBar(living.getUniqueId());
                abilityCooldowns.remove(living.getUniqueId());
                triggeredBossPhases.remove(living.getUniqueId());
            }
        }
    }

    private void tickDungeonAbilities() {
        int checked = 0;
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity living : world.getLivingEntities()) {
                if (++checked > 80) return;
                if (living.isDead() || !isDungeonEntity(living)) continue;
                Player target = nearestPlayer(living, 18.0D);
                if (target == null) continue;
                if (living.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING)) tickBossAbility(living, target);
                else tickMobAbility(living, target);
            }
        }
    }

    private Player nearestPlayer(LivingEntity living, double radius) {
        Player best = null;
        double bestSq = radius * radius;
        for (Player player : living.getWorld().getPlayers()) {
            if (player.isDead() || player.getGameMode().name().contains("SPECTATOR")) continue;
            if(plugin instanceof club.abelix.games.fakevival.Game game&&game.getDungeonSpectateService().isSpectating(player))continue;
            double distance = player.getLocation().distanceSquared(living.getLocation());
            if (distance > bestSq) continue;
            bestSq = distance;
            best = player;
        }
        return best;
    }

    private void tickBossAbility(LivingEntity boss, Player target) {
        String bossId=boss.getPersistentDataContainer().getOrDefault(smpBossIdKey,PersistentDataType.STRING,
                boss.getPersistentDataContainer().getOrDefault(mobIdKey,PersistentDataType.STRING,""));
        if (bossId.equals("pirate_captain_blackwake")) {
            tickCaptainBlackwake(boss);
            return;
        }
        if (bossId.equals("sacred_guardian_aurelius")) {
            tickGuardianAurelius(boss);
            return;
        }
        if (ready(boss, "regen", 4000L) && System.currentTimeMillis() - boss.getPersistentDataContainer().getOrDefault(lastDamagedKey, PersistentDataType.LONG, 0L) >= 8000L) {
            double max = dungeonMaxHealth(boss);
            double current = Math.min(max, dungeonCurrentHealth(boss) + max * 0.0015D);
            writeDungeonHealth(boss, current, max);
            syncDungeonBukkitHealth(boss, current, max);
        }
        if (dungeonCurrentHealth(boss) / dungeonMaxHealth(boss) <= 0.25D && ready(boss, "enrage", 120000L)) {
            boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60, 0, true, true, true));
            boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 0, true, true, true));
        }
        if (ready(boss, "ground_slam", 12000L + random.nextInt(6000))) {
            boss.getWorld().spawnParticle(Particle.CRIT, boss.getLocation().add(0, 0.2, 0), 25, 2.4, 0.1, 2.4, 0.02);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player player : boss.getWorld().getPlayers()) if (player.getLocation().distanceSquared(boss.getLocation()) <= 25.0D) {
                    player.setVelocity(player.getLocation().toVector().subtract(boss.getLocation().toVector()).normalize().multiply(0.8D).setY(0.45D));
                    player.damage(8.0D, boss);
                }
                boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6F, 0.9F);
            }, 20L);
        }
        if (ready(boss, "pull", 22000L + random.nextInt(8000))) {
            for (Player player : boss.getWorld().getPlayers()) if (player.getLocation().distanceSquared(boss.getLocation()) <= 100.0D) {
                player.setVelocity(boss.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.55D).setY(0.18D));
            }
            boss.getWorld().spawnParticle(Particle.PORTAL, boss.getLocation().add(0, 1, 0), 25, 1.2, 0.7, 1.2, 0.03);
        }
        if (ready(boss, "teleport", 24000L)) {
            boss.teleport(target.getLocation().clone().add(random.nextDouble(-2.5D, 2.5D), 0, random.nextDouble(-2.5D, 2.5D)));
            target.damage(6.0D, boss);
        }
        if (ready(boss, "poison", 26000L)) {
            boss.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, target.getLocation(), 28, 1.8, 0.5, 1.8, 0.01);
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 0, true, true, true));
        }
        if(normalizeMobId(bossId).equals("tyrant")&&ready(boss,"dark_cleave",16000L)){
            Vector facing=boss.getLocation().getDirection().setY(0).normalize();
            for(Player player:boss.getWorld().getPlayers())if(player.getLocation().distanceSquared(boss.getLocation())<=36.0D){Vector toward=player.getLocation().toVector().subtract(boss.getLocation().toVector()).setY(0).normalize();if(facing.dot(toward)>0.25D)player.damage(12.0D,boss);}
            boss.getWorld().spawnParticle(Particle.SQUID_INK,boss.getLocation().add(0,1,0),18,1.5,0.4,1.5,0.01);
        }
        if (ready(boss, "minions", 20000L)) summonTemporaryMinions(boss, target);
    }

    private void tickCaptainBlackwake(LivingEntity boss) {
        double healthRatio = dungeonCurrentHealth(boss) / Math.max(1.0D, dungeonMaxHealth(boss));
        if (healthRatio <= 0.30D && triggerBossPhase(boss, "enrage")) {
            boss.getPersistentDataContainer().set(bossEnragedKey, PersistentDataType.BYTE, (byte) 1);
            broadcastBossMessage(boss, "&4Captain Blackwake becomes enraged!");
        }
        if (ready(boss, "blackwake_cleave", 8000L)) {
            broadcastBossMessage(boss, "&cCaptain Blackwake swings his cursed cutlass!");
            Vector facing = boss.getLocation().getDirection().setY(0);
            if (facing.lengthSquared() > 0) facing.normalize();
            for (Player player : bossTargets(boss, 4.0D)) {
                Vector toward = player.getLocation().toVector().subtract(boss.getLocation().toVector()).setY(0);
                if (toward.lengthSquared() > 0 && facing.dot(toward.normalize()) >= 0.35D) {
                    dealDungeonAbilityDamage(player, boss, bossDamage(boss) * 0.75D);
                }
            }
            boss.getWorld().spawnParticle(Particle.SWEEP_ATTACK, boss.getLocation().add(0, 1, 0), 12, 1.5D, 0.4D, 1.5D, 0.0D);
        }
        if (ready(boss, "cannon_barrage", 18000L)) {
            broadcastBossMessage(boss, "&6Captain Blackwake calls a cannon barrage!");
            List<Player> targets = bossTargets(boss, 24.0D);
            if (!targets.isEmpty()) {
                int spots = 3 + random.nextInt(4);
                List<Location> marks = new ArrayList<>();
                for (int index = 0; index < spots; index++) {
                    Player marked = targets.get(random.nextInt(targets.size()));
                    Location location = marked.getLocation().clone().add(random.nextDouble(-2.0D, 2.0D), 0.1D, random.nextDouble(-2.0D, 2.0D));
                    marks.add(location);
                    boss.getWorld().spawnParticle(Particle.FLAME, location, 24, 1.2D, 0.05D, 1.2D, 0.01D);
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!bossActive(boss)) return;
                    for (Location mark : marks) {
                        boss.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, mark, 1);
                        boss.getWorld().playSound(mark, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.8F);
                        for (Player player : bossTargets(boss, 32.0D)) {
                            if (player.getLocation().distanceSquared(mark) > 9.0D) continue;
                            dealDungeonAbilityDamage(player, boss, bossDamage(boss) * 0.90D);
                            Vector knockback = player.getLocation().toVector().subtract(mark.toVector()).setY(0.35D);
                            if (knockback.lengthSquared() > 0) player.setVelocity(knockback.normalize().multiply(1.05D).setY(0.45D));
                        }
                    }
                }, 40L);
            }
        }
        if (ready(boss, "summon_crew", 25000L)) {
            broadcastBossMessage(boss, "&cCaptain Blackwake summons his crew!");
            summonBossMinions(boss, 3 + random.nextInt(3), 10,
                    List.of("pirate", "pirate_archer", "pirate_brute"), List.of(EntityType.HUSK, EntityType.SKELETON, EntityType.ZOMBIE));
        }
        if (ready(boss, "tide_pull", 22000L)) {
            broadcastBossMessage(boss, "&bThe tide drags you toward Captain Blackwake!");
            for (Player player : bossTargets(boss, 12.0D)) {
                Vector pull = boss.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0.15D);
                if (pull.lengthSquared() > 0) player.setVelocity(pull.normalize().multiply(0.45D).setY(0.18D));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, true, true));
            }
            boss.getWorld().spawnParticle(Particle.SPLASH, boss.getLocation().add(0, 0.5D, 0), 40, 4.0D, 0.4D, 4.0D, 0.05D);
        }
    }

    private void tickGuardianAurelius(LivingEntity boss) {
        double healthPercent = dungeonCurrentHealth(boss) / Math.max(1.0D, dungeonMaxHealth(boss)) * 100.0D;
        for (int threshold : List.of(70, 40, 15)) {
            if (healthPercent <= threshold && triggerBossPhase(boss, "shield_" + threshold)) {
                boss.getPersistentDataContainer().set(bossShieldUntilKey, PersistentDataType.LONG, System.currentTimeMillis() + 8000L);
                broadcastBossMessage(boss, "&eAurelius is protected by a divine shield!");
                summonBossMinions(boss, 2, 8, List.of("temple_spirit", "sacred_minion", "guardian"), List.of(EntityType.ZOMBIE, EntityType.SKELETON));
                boss.getWorld().spawnParticle(Particle.END_ROD, boss.getLocation().add(0, 1, 0), 45, 1.4D, 1.0D, 1.4D, 0.02D);
            }
        }
        if (healthPercent <= 20.0D && triggerBossPhase(boss, "enrage")) {
            boss.getPersistentDataContainer().set(bossEnragedKey, PersistentDataType.BYTE, (byte) 1);
            broadcastBossMessage(boss, "&4Aurelius releases sacred fury!");
        }
        if (ready(boss, "radiant_slam", 10000L)) {
            broadcastBossMessage(boss, "&eAurelius slams the sacred ground!");
            boss.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, boss.getLocation().add(0, 0.3D, 0), 45, 2.5D, 0.2D, 2.5D, 0.08D);
            boss.getWorld().spawnParticle(Particle.END_ROD, boss.getLocation().add(0, 0.3D, 0), 35, 2.5D, 0.2D, 2.5D, 0.04D);
            for (Player player : bossTargets(boss, 5.0D)) {
                dealDungeonAbilityDamage(player, boss, bossDamage(boss) * 0.80D);
                Vector knockback = player.getLocation().toVector().subtract(boss.getLocation().toVector()).setY(0.4D);
                if (knockback.lengthSquared() > 0) player.setVelocity(knockback.normalize().multiply(0.9D).setY(0.45D));
            }
        }
        if (ready(boss, "holy_beam", 16000L)) {
            List<Player> targets = bossTargets(boss, 24.0D);
            if (!targets.isEmpty()) {
                Player marked = targets.get(random.nextInt(targets.size()));
                broadcastBossMessage(boss, "&eA sacred beam locks onto " + marked.getName() + "!");
                Location endpoint = marked.getLocation().clone().add(0, 1, 0);
                boss.getWorld().spawnParticle(Particle.END_ROD, endpoint, 18, 0.4D, 0.8D, 0.4D, 0.01D);
                Bukkit.getScheduler().runTaskLater(plugin, () -> fireHolyBeam(boss, endpoint), 30L);
            }
        }
        if (ready(boss, "sacred_circle", 22000L)) {
            broadcastBossMessage(boss, "&6Sacred circles begin to glow!");
            Location center = boss.getLocation().clone();
            spawnRing(center, 5.0D, Particle.END_ROD);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!bossActive(boss)) return;
                spawnRing(center, 5.0D, Particle.TOTEM_OF_UNDYING);
                for (Player player : bossTargets(boss, 32.0D)) {
                    if (player.getLocation().distanceSquared(center) <= 25.0D) dealDungeonAbilityDamage(player, boss, bossDamage(boss));
                }
            }, 60L);
        }
        if (ready(boss, "summon_temple_spirits", 28000L)) {
            broadcastBossMessage(boss, "&eAurelius calls sacred spirits!");
            summonBossMinions(boss, 2 + random.nextInt(3), 8,
                    List.of("temple_spirit", "sacred_minion", "guardian"), List.of(EntityType.ZOMBIE, EntityType.SKELETON));
        }
    }

    private void fireHolyBeam(LivingEntity boss, Location endpoint) {
        if (!bossActive(boss) || endpoint == null || !boss.getWorld().equals(endpoint.getWorld())) return;
        Location start = boss.getLocation().clone().add(0, 1.2D, 0);
        Vector line = endpoint.toVector().subtract(start.toVector());
        double length = line.length();
        if (length <= 0.01D) return;
        Vector step = line.clone().normalize().multiply(0.4D);
        Location particle = start.clone();
        for (double distance = 0; distance <= length; distance += 0.4D) {
            boss.getWorld().spawnParticle(Particle.END_ROD, particle, 2, 0.05D, 0.05D, 0.05D, 0.0D);
            particle.add(step);
        }
        for (Player player : bossTargets(boss, Math.max(24.0D, length + 2.0D))) {
            if (distanceToLine(player.getLocation().add(0, 1, 0), start, endpoint) <= 1.1D) {
                dealDungeonAbilityDamage(player, boss, bossDamage(boss) * 1.10D);
            }
        }
        boss.getWorld().playSound(start, Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.4F);
    }

    private double distanceToLine(Location point, Location start, Location end) {
        Vector segment = end.toVector().subtract(start.toVector());
        double lengthSquared = segment.lengthSquared();
        if (lengthSquared <= 0.0D) return point.distance(start);
        double position = Math.clamp(point.toVector().subtract(start.toVector()).dot(segment) / lengthSquared, 0.0D, 1.0D);
        return point.toVector().distance(start.toVector().add(segment.multiply(position)));
    }

    private void spawnRing(Location center, double radius, Particle particle) {
        if (center == null || center.getWorld() == null) return;
        for (int index = 0; index < 48; index++) {
            double angle = Math.PI * 2.0D * index / 48.0D;
            center.getWorld().spawnParticle(particle, center.clone().add(Math.cos(angle) * radius, 0.15D, Math.sin(angle) * radius), 1);
        }
    }

    private List<Player> bossTargets(LivingEntity boss, double radius) {
        List<Player> players = new ArrayList<>();
        String runId = boss.getPersistentDataContainer().get(smpDungeonRunIdKey, PersistentDataType.STRING);
        for (Player player : boss.getWorld().getPlayers()) {
            if (player.isDead() || player.getGameMode().name().contains("SPECTATOR") || player.getLocation().distanceSquared(boss.getLocation()) > radius * radius) continue;
            if (plugin instanceof club.abelix.games.fakevival.Game game && game.getDungeonSpectateService().isSpectating(player)) continue;
            DungeonWorldContext context = dungeonWorldContext(player.getWorld());
            if (runId != null && context != null && !runId.equals(context.runId())) continue;
            players.add(player);
        }
        return players;
    }

    private void broadcastBossMessage(LivingEntity boss, String message) {
        for (Player player : bossTargets(boss, 32.0D)) player.sendMessage(CC.color(message));
    }

    private void dealDungeonAbilityDamage(Player target, LivingEntity source, double damage) {
        MMODamageSourceTracker.runAs(MMODamageSourceType.ABILITY, () -> target.damage(Math.max(1.0D, damage), source));
    }

    private double bossDamage(LivingEntity boss) {
        return boss.getPersistentDataContainer().getOrDefault(smpMobDamageKey, PersistentDataType.DOUBLE,
                boss.getAttribute(Attribute.ATTACK_DAMAGE) == null ? 10.0D : Objects.requireNonNull(boss.getAttribute(Attribute.ATTACK_DAMAGE)).getValue());
    }

    private boolean triggerBossPhase(LivingEntity boss, String phase) {
        return triggeredBossPhases.computeIfAbsent(boss.getUniqueId(), ignored -> new HashSet<>()).add(phase);
    }

    private boolean bossActive(LivingEntity boss) {
        if (boss == null || boss.isDead() || !boss.isValid()) return false;
        String dungeon = boss.getPersistentDataContainer().get(dungeonBossKey, PersistentDataType.STRING);
        DungeonRun run = dungeon == null ? null : activeRun(dungeon);
        return run != null && run.state == DungeonState.ACTIVE && boss.getUniqueId().equals(run.bossEntityUuid);
    }

    private void summonBossMinions(LivingEntity boss, int requested, int maxAlive, List<String> aliases, List<EntityType> fallbacks) {
        String dungeon = boss.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING);
        if (dungeon == null || aliases.isEmpty() || fallbacks.isEmpty()) return;
        List<LivingEntity> existing = trackedDungeonMobs.stream().map(Bukkit::getEntity)
                .filter(entity -> entity instanceof LivingEntity living
                        && dungeon.equalsIgnoreCase(living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING))
                        && living.getPersistentDataContainer().has(temporaryMinionKey, PersistentDataType.BYTE))
                .map(entity -> (LivingEntity) entity).toList();
        int amount = Math.min(Math.max(0, maxAlive - existing.size()), Math.max(0, requested));
        Player target = nearestPlayer(boss, 24.0D);
        for (int index = 0; index < amount; index++) {
            String alias = aliases.get(random.nextInt(aliases.size()));
            DungeonMobTemplateRegistry.DungeonMobTemplate template = DungeonMobTemplateRegistry.resolve(alias);
            EntityType fallback = fallbacks.get(index % fallbacks.size());
            EntityType type = template == null ? fallback : template.entityType();
            String mobId = template == null ? alias : template.mobId();
            String display = template == null ? pretty(alias) : template.displayName();
            int level = Math.max(1, boss.getPersistentDataContainer().getOrDefault(dungeonLevelKey, PersistentDataType.INTEGER, 1) - 2);
            Location location = boss.getLocation().clone().add(random.nextDouble(-2.5D, 2.5D), 0, random.nextDouble(-2.5D, 2.5D));
            if (!canSpawnDungeonAt(dungeon, "boss_minion", false, location)) continue;
            LivingEntity minion = spawnOptimizedDungeonMob(mobId, location, type, display,
                    1000.0D + level * 120.0D, 8.0D + level * 2.0D, level);
            if (minion == null) continue;
            tagDungeonEntity(minion, dungeon, "boss_minion", mobId, index, level, display, false);
            minion.getPersistentDataContainer().set(temporaryMinionKey, PersistentDataType.BYTE, (byte) 1);
            minion.setRemoveWhenFarAway(false);
            makeDungeonMobSafe(minion);
            if (minion instanceof Mob mob && target != null) mob.setTarget(target);
            trackedDungeonMobs.add(minion.getUniqueId());
        }
    }

    private void tickMobAbility(LivingEntity mob, Player target) {
        if (!ready(mob, "mob_ability", 10000L + random.nextInt(10000))) return;
        String role = mob.getPersistentDataContainer().getOrDefault(mobIdKey, PersistentDataType.STRING, mob.getType().name()).toLowerCase(Locale.ROOT);
        if (role.contains("archer") || mob.getType() == EntityType.SKELETON) {
            target.damage(4.0D, mob);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3);
        } else if (role.contains("brute") || role.contains("guard")) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, true, true));
            Vector dash = target.getLocation().toVector().subtract(mob.getLocation().toVector()).normalize().multiply(0.65D).setY(0.1D);
            mob.setVelocity(dash);
        } else if (role.contains("mage") || mob.getType() == EntityType.WITCH) {
            target.damage(5.0D, mob);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, true, true, true));
        } else if (random.nextDouble() < 0.35D) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, true, true, true));
        }
    }

    private boolean ready(LivingEntity living, String ability, long cooldownMillis) {
        long now = System.currentTimeMillis();
        if (living.getPersistentDataContainer().has(bossEnragedKey, PersistentDataType.BYTE)) {
            String bossId = living.getPersistentDataContainer().getOrDefault(smpBossIdKey, PersistentDataType.STRING, "");
            if (bossId.equals("pirate_captain_blackwake")) cooldownMillis = Math.max(1L, Math.round(cooldownMillis * 0.8D));
            if (bossId.equals("sacred_guardian_aurelius") && ability.equals("holy_beam")) cooldownMillis = Math.max(1L, Math.round(cooldownMillis * 0.7D));
        }
        Map<String, Long> cooldowns = abilityCooldowns.computeIfAbsent(living.getUniqueId(), ignored -> new HashMap<>());
        if (cooldowns.getOrDefault(ability, 0L) > now) return false;
        cooldowns.put(ability, now + cooldownMillis);
        return true;
    }

    private void summonTemporaryMinions(LivingEntity boss, Player target) {
        String dungeon = boss.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING);
        if (dungeon == null) return;
        long existing = trackedDungeonMobs.stream().map(Bukkit::getEntity)
                .filter(entity -> entity instanceof LivingEntity living
                        && dungeon.equalsIgnoreCase(living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING))
                        && living.getPersistentDataContainer().has(temporaryMinionKey, PersistentDataType.BYTE))
                .count();
        int amount = Math.min(4, Math.min(8 - (int) existing, 2 + random.nextInt(3)));
        for (int i = 0; i < amount; i++) {
            int level = Math.max(1, boss.getPersistentDataContainer().getOrDefault(dungeonLevelKey, PersistentDataType.INTEGER, 1) - 1);
            Location location = boss.getLocation().clone().add(random.nextDouble(-2, 2), 0, random.nextDouble(-2, 2));
            if (!canSpawnDungeonAt(dungeon, "boss_minion", false, location)) break;
            LivingEntity minion = spawnOptimizedDungeonMob("dungeon_" + dungeon + "_boss_minion", location, EntityType.ZOMBIE, "&cBoss Minion", 500.0D + level * 100.0D, 4.0D + level, level);
            if (minion != null) {
                tagDungeonEntity(minion, dungeon, "boss_minion", "minion", i, level, "&cBoss Minion", false);
                minion.getPersistentDataContainer().set(temporaryMinionKey, PersistentDataType.BYTE, (byte) 1);
                minion.setRemoveWhenFarAway(false);
                if (minion instanceof Mob mob) mob.setTarget(target);
                trackedDungeonMobs.add(minion.getUniqueId());
            }
        }
    }

    private void makeDungeonMobSafe(LivingEntity living) {
        living.setFireTicks(0);
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dungeons.prevent-sunburn-with-helmets", true)) return;
        EntityEquipment equipment = living.getEquipment();
        if (equipment == null || equipment.getHelmet() != null) return;
        equipment.setHelmet(new ItemStack(Material.LEATHER_HELMET));
        equipment.setHelmetDropChance(0.0F);
    }

    public boolean isProtectedDungeonEntity(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (isDungeonEntity(living)) return true;
        String[] tags = {"smp_dungeon_mob", "dungeon_mob", "zone_mob", "zone_mob_zone",
                "zone_mob_spawn", "zone_boss", "is_zone_boss", "dungeon_boss"};
        for (String tag : tags) if (hasTag(living, new NamespacedKey(plugin, tag))) return true;
        return false;
    }

    private void clearDungeonMobFire() {
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity living : world.getLivingEntities()) {
                if (living.getFireTicks() > 0 && isProtectedDungeonEntity(living)) living.setFireTicks(0);
            }
        }
    }

    private int setting(String path, int fallback) {
        return Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path, fallback));
    }

    private int cappedSetting(String path, int fallback, int maximum) {
        return Math.min(maximum, setting(path, fallback));
    }

    public MobLimitSnapshot mobLimits(Player player) {
        if (player == null) return new MobLimitSnapshot(0, 0, setting("dungeons.max-mobs-per-chunk", 12), 0,
                setting("dungeons.max-mobs-per-room", 40), 0, setting("dungeons.max-active-mobs-near-player", 80), 0, 150);
        String dungeon = activeRuns.keySet().stream().filter(id -> isInsideArea(id, player.getLocation())).findFirst().orElse(null);
        int nearby = 0;
        int active = 0;
        for (LivingEntity living : player.getWorld().getLivingEntities()) {
            if (!isDungeonEntity(living) || living.isDead() || !living.isValid()) continue;
            if (living.getLocation().distanceSquared(player.getLocation()) <= ACTIVE_MOB_RANGE * ACTIVE_MOB_RANGE) { nearby++; active++; }
        }
        int chunk = countDungeonMobsInChunk(player.getLocation());
        String room = dungeon == null ? "none" : roomAt(dungeon, player.getLocation());
        int roomCount = dungeon == null ? 0 : countDungeonMobsInRoom(dungeon, room);
        int alive = dungeon == null ? 0 : countAliveDungeonMobs(dungeon);
        return new MobLimitSnapshot(nearby, chunk, Math.min(ABSOLUTE_MAX_MOBS_PER_CHUNK, setting("dungeons.max-mobs-per-chunk", 12)),
                roomCount, "boss".equalsIgnoreCase(room) ? cappedSetting("dungeons.max-boss-room-mobs", 50, 50) : cappedSetting("dungeons.max-mobs-per-room", 40, 40),
                active, setting("dungeons.max-active-mobs-near-player", 80), alive, cappedSetting("dungeons.max-mobs-per-dungeon", 150, 150));
    }

    private String roomAt(String dungeon, Location location) {
        ConfigurationSection mobs = dataManager.getData().getConfigurationSection(path(dungeon) + ".mobs");
        if (mobs == null) return "none";
        for (String key : mobs.getKeys(false)) {
            Location spawn = loadLocation(path(dungeon) + ".mobs." + key);
            if (spawn != null && spawn.getWorld().equals(location.getWorld()) && spawn.distanceSquared(location) < 32.0D * 32.0D) return roomId(dungeon, key, false);
        }
        return "none";
    }

    public record MobLimitSnapshot(int nearby, int chunkCount, int maxChunk, int roomCount, int maxRoom,
                                   int active, int maxActive, int alive, int maxInstance) {
    }

    private int configuredMobCount(String dungeon, String spawn) {
        String base = path(dungeon) + ".mobs." + spawn;
        int count = dataManager.getData().contains(base + ".count")
                ? dataManager.getData().getInt(base + ".count", 1)
                : dataManager.getData().getInt(base + ".amount", 1);
        return Math.max(1, count);
    }

    private int availableSpawnBatch() {
        if (isLowTps()) return 0;
        long window = System.currentTimeMillis() / 1000L;
        if (window != spawnRateWindow) {
            spawnRateWindow = window;
            spawnedInRateWindow = 0;
        }
        int perTick = setting("dungeons.spawn-batch-per-tick", 5);
        int perSecond = setting("dungeons.spawn-batch-per-second", 25);
        return Math.max(0, Math.min(perTick, perSecond - spawnedInRateWindow));
    }

    private boolean isLowTps() {
        try {
            Object value = Bukkit.getServer().getClass().getMethod("getTPS").invoke(Bukkit.getServer());
            return value instanceof double[] tps && tps.length > 0 && tps[0] < 18.0D;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private void recordSpawnInRateWindow() {
        spawnedInRateWindow++;
    }

    private void cleanupTrackedDungeonMobs(String dungeon) {
        trackedDungeonMobs.removeIf(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) return true;
            if (!living.getWorld().isChunkLoaded(living.getLocation().getBlockX() >> 4, living.getLocation().getBlockZ() >> 4)) return true;
            return !isDungeonEntity(living);
        });
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity living : world.getLivingEntities()) {
                if (living.isDead() || !living.isValid() || !isDungeonEntity(living)) continue;
                String storedDungeon = living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING);
                if (storedDungeon == null && isInsideArea(dungeon, living.getLocation())) repairLegacyIdentity(living, dungeon);
                storedDungeon = living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING);
                if (dungeon.equalsIgnoreCase(storedDungeon)) trackedDungeonMobs.add(living.getUniqueId());
            }
        }
        removeExcessDungeonMobs(dungeon);
    }

    private void removeExcessDungeonMobs(String dungeon) {
        for (UUID uuid : new HashSet<>(trackedDungeonMobs)) {
            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) continue;
            if (!dungeon.equalsIgnoreCase(living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING))) continue;
            String spawn = living.getPersistentDataContainer().get(spawnIdKey, PersistentDataType.STRING);
            if (spawn == null) continue;
            boolean boss = living.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING);
            int configured = boss ? 1 : configuredMobCount(dungeon, spawn);
            int index = living.getPersistentDataContainer().getOrDefault(spawnIndexKey, PersistentDataType.INTEGER, 0);
            if (index < configured) continue;
            living.remove();
            trackedDungeonMobs.remove(uuid);
            removeBossBar(uuid);
        }
    }

    private void repairLegacyIdentity(LivingEntity living, String dungeon) {
        ConfigurationSection mobs = dataManager.getData().getConfigurationSection(path(dungeon) + ".mobs");
        if (mobs != null) for (String key : mobs.getKeys(false)) {
            Location location = loadLocation(path(dungeon) + ".mobs." + key);
            if (nearSpawn(living, location)) {
                int index = firstFreeIndex(dungeon, key, false, configuredMobCount(dungeon, key));
                String name = living.getPersistentDataContainer().getOrDefault(dungeonSpawnedKey, PersistentDataType.STRING, pretty(key));
                int level = living.getPersistentDataContainer().getOrDefault(dungeonLevelKey, PersistentDataType.INTEGER, 1);
                tagDungeonEntity(living, dungeon, key, living.getType().name(), index, level, name, false);
                return;
            }
        }
        ConfigurationSection bosses = dataManager.getData().getConfigurationSection(path(dungeon) + ".bosses");
        if (bosses != null) for (String key : bosses.getKeys(false)) {
            if (!nearSpawn(living, loadLocation(path(dungeon) + ".bosses." + key + ".location"))) continue;
            String name = living.getPersistentDataContainer().getOrDefault(dungeonSpawnedKey, PersistentDataType.STRING, pretty(key));
            int level = living.getPersistentDataContainer().getOrDefault(dungeonLevelKey, PersistentDataType.INTEGER, 1);
            tagDungeonEntity(living, dungeon, key, key, 0, level, name, true);
            return;
        }
        if (nearSpawn(living, loadLocation(path(dungeon) + ".boss.location"))) {
            String name = living.getPersistentDataContainer().getOrDefault(dungeonSpawnedKey, PersistentDataType.STRING, "Dungeon Boss");
            int level = living.getPersistentDataContainer().getOrDefault(dungeonLevelKey, PersistentDataType.INTEGER, 1);
            tagDungeonEntity(living, dungeon, "legacy_boss", "legacy_boss", 0, level, name, true);
        }
    }

    private boolean nearSpawn(LivingEntity living, Location location) {
        return location != null && location.getWorld() != null && living.getWorld().equals(location.getWorld())
                && living.getLocation().distanceSquared(location) <= 16.0D;
    }

    private int firstFreeIndex(String dungeon, String spawn, boolean boss, int configured) {
        for (int i = 0; i < configured; i++) if (!hasAliveSpawnIndex(dungeon, spawn, boss, i)) return i;
        return configured;
    }

    private boolean hasAliveSpawnIndex(String dungeon, String spawn, boolean boss, int index) {
        for (UUID uuid : trackedDungeonMobs) {
            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) continue;
            if (!dungeon.equalsIgnoreCase(living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING))) continue;
            if (!spawn.equalsIgnoreCase(living.getPersistentDataContainer().get(spawnIdKey, PersistentDataType.STRING))) continue;
            if (boss != living.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING)) continue;
            if (index == living.getPersistentDataContainer().getOrDefault(spawnIndexKey, PersistentDataType.INTEGER, 0)) return true;
        }
        return false;
    }

    private int countAliveForSpawn(String dungeon, String spawn, boolean boss) {
        int count = 0;
        for (UUID uuid : trackedDungeonMobs) {
            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) continue;
            if (dungeon.equalsIgnoreCase(living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING))
                    && spawn.equalsIgnoreCase(living.getPersistentDataContainer().get(spawnIdKey, PersistentDataType.STRING))
                    && boss == living.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING)) count++;
        }
        return count;
    }

    private int countAliveDungeonMobs(String dungeon) {
        int count = 0;
        for (UUID uuid : trackedDungeonMobs) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity instanceof LivingEntity living && !living.isDead() && living.isValid()
                    && dungeon.equalsIgnoreCase(living.getPersistentDataContainer().get(dungeonIdKey, PersistentDataType.STRING))) count++;
        }
        return count;
    }

    private int countAllAliveDungeonMobs() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) for (LivingEntity living : world.getLivingEntities())
            if (!living.isDead() && living.isValid() && isProtectedDungeonEntity(living)) count++;
        return count;
    }

    private boolean hasAliveDungeonEntities(String name) {
        ConfigurationSection section = dataManager.getData().getConfigurationSection(path(name));
        if (section == null) return false;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity living) || living.isDead() || !isDungeonEntity(living)) continue;
                if (isInsideArea(name, living.getLocation())) return true;
            }
        }
        return false;
    }

    private void storeDungeonMobDefinition(String base, String spawnId, DungeonMobTemplateRegistry.DungeonMobTemplate template) {
        dataManager.getData().set(base + ".spawn_id", spawnId);
        dataManager.getData().set(base + ".mob_id", template.mobId());
        dataManager.getData().set(base + ".mob_display_name", template.displayName());
        dataManager.getData().set(base + ".entity_type", template.entityType().name());
        dataManager.getData().set(base + ".type", template.entityType().name());
        dataManager.getData().set(base + ".name", template.displayName());
        dataManager.getData().set(base + ".role", template.role());
        if (!dataManager.getData().contains(base + ".health")) dataManager.getData().set(base + ".health", template.defaultHealth());
        if (!dataManager.getData().contains(base + ".damage")) dataManager.getData().set(base + ".damage", template.defaultDamage());
    }

    private void migrateDungeonMobDefinitions() {
        DungeonMobValidation validation = validateDungeonMobs(null, true);
        if (validation.repaired() > 0) {
            plugin.getLogger().info("[SMP] Repaired " + validation.repaired() + " dungeon mob definitions without changing spawn locations.");
        }
        if (validation.invalid() > 0) {
            plugin.getLogger().warning("[SMP] Found " + validation.invalid() + " unknown dungeon mob aliases: " + String.join(", ", validation.unknownAliases()));
        }
    }

    public DungeonMobValidation validateDungeonMobs(String dungeonName, boolean repair) {
        List<String> dungeonIds = new ArrayList<>();
        if (dungeonName != null && !dungeonName.isBlank()) {
            dungeonIds.add(canonicalDungeonId(dungeonName));
        } else {
            ConfigurationSection all = dataManager.getData().getConfigurationSection("dungeons");
            if (all != null) dungeonIds.addAll(all.getKeys(false));
        }
        int checked = 0;
        int repaired = 0;
        int invalid = 0;
        int dirtyNames = 0;
        Set<String> unknown = new java.util.TreeSet<>();
        Set<String> changedDungeons = new HashSet<>();
        for (String dungeon : dungeonIds) {
            ConfigurationSection mobs = dataManager.getData().getConfigurationSection(path(dungeon) + ".mobs");
            if (mobs == null) continue;
            for (String spawnId : mobs.getKeys(false)) {
                checked++;
                String base = path(dungeon) + ".mobs." + spawnId;
                String mobId = dataManager.getData().getString(base + ".mob_id");
                String rawType = dataManager.getData().getString(base + ".entity_type",
                        dataManager.getData().getString(base + ".type", ""));
                DungeonMobTemplateRegistry.DungeonMobTemplate template =
                        DungeonMobTemplateRegistry.resolveDefinition(spawnId, mobId, rawType);
                if (template == null) {
                    invalid++;
                    unknown.add((mobId == null || mobId.isBlank() ? DungeonMobTemplateRegistry.withoutInternalSuffix(spawnId) : mobId).toLowerCase(Locale.ROOT));
                    continue;
                }
                String storedName = dataManager.getData().getString(base + ".mob_display_name",
                        dataManager.getData().getString(base + ".name", ""));
                String cleanName = DungeonMobTemplateRegistry.cleanDisplayName(storedName, template);
                boolean dirty = !cleanName.equals(storedName)
                        || !template.entityType().name().equalsIgnoreCase(rawType)
                        || !template.mobId().equalsIgnoreCase(String.valueOf(mobId))
                        || !dataManager.getData().contains(base + ".mob_display_name")
                        || !template.role().equalsIgnoreCase(dataManager.getData().getString(base + ".role", ""))
                        || !spawnId.equals(dataManager.getData().getString(base + ".spawn_id", ""));
                if (!cleanName.equals(storedName)) dirtyNames++;
                if (!repair || !dirty) continue;
                storeDungeonMobDefinition(base, spawnId, template);
                dataManager.getData().set(base + ".mob_display_name", cleanName);
                dataManager.getData().set(base + ".name", cleanName);
                repaired++;
                changedDungeons.add(canonicalDungeonId(dungeon));
            }
        }
        if (repair && repaired > 0) {
            dataManager.save();
            for (String dungeon : changedDungeons) persistDungeonSetup(dungeon);
        }
        boolean aliasesValid = aliasMatches("guards", EntityType.HUSK, "Guard")
                && aliasMatches("knights", EntityType.HUSK, "Knight")
                && aliasMatches("spiders", EntityType.SPIDER, "Spider")
                && aliasMatches("minions", EntityType.ZOMBIE, "Minion")
                && aliasMatches("archers", EntityType.SKELETON, "Archer");
        boolean cleanupValid = DungeonMobTemplateRegistry.cleanDisplayName("guards_1784980100299",
                DungeonMobTemplateRegistry.resolve("guards")).equals("Guard")
                && DungeonMobTemplateRegistry.cleanDisplayName("knights_1784978670946",
                DungeonMobTemplateRegistry.resolve("knights")).equals("Knight");
        boolean nametagValid = formatCleanDungeonName("guards_1784980100299", 41, 500, 500)
                .equals("&8[&7Lv.41&8] &cGuard &a500/500&c\u2764");
        boolean pdcHealthValid = smpMobCurrentHealthKey != null && smpMobMaxHealthKey != null
                && smpMobLevelKey != null && smpMobDisplayNameKey != null && smpMobIdKey != null;
        return new DungeonMobValidation(checked, repaired, invalid, dirtyNames, List.copyOf(unknown),
                invalid == 0, (dirtyNames == 0 || repair) && cleanupValid,
                invalid == 0 && aliasesValid, pdcHealthValid, nametagValid);
    }

    private boolean aliasMatches(String alias, EntityType type, String displayName) {
        DungeonMobTemplateRegistry.DungeonMobTemplate template = DungeonMobTemplateRegistry.resolve(alias);
        return template != null && template.entityType() == type && template.displayName().equals(displayName);
    }

    private String fallbackMobType(String key) {
        DungeonMobTemplateRegistry.DungeonMobTemplate template = DungeonMobTemplateRegistry.resolve(key);
        return template == null ? "" : template.entityType().name();
    }

    private EntityType resolveDungeonEntityType(String dungeon, String mobPath, String rawType) {
        DungeonMobTemplateRegistry.DungeonMobTemplate template = DungeonMobTemplateRegistry.resolve(rawType);
        if (template != null) return template.entityType();
        plugin.getLogger().warning("[SMP] Unknown dungeon mob alias '" + rawType + "' for dungeon '" + dungeon + "'. Use /dungeon mob aliases.");
        throw new IllegalArgumentException("Unknown dungeon mob alias " + rawType + " at " + mobPath);
    }

    private void tagDungeonEntity(LivingEntity living, String dungeon, String spawnId, String mobId, int spawnIndex,
                                  int level, String name, boolean boss) {
        DungeonMobTemplateRegistry.DungeonMobTemplate template = DungeonMobTemplateRegistry.resolve(mobId);
        String displayName = boss ? name : DungeonMobTemplateRegistry.cleanDisplayName(name, template);
        living.getPersistentDataContainer().set(dungeonSpawnedKey, PersistentDataType.STRING, displayName);
        living.getPersistentDataContainer().set(dungeonIdKey, PersistentDataType.STRING, dungeon.toLowerCase(Locale.ROOT));
        living.getPersistentDataContainer().set(spawnIdKey, PersistentDataType.STRING, spawnId.toLowerCase(Locale.ROOT));
        living.getPersistentDataContainer().set(mobIdKey, PersistentDataType.STRING, mobId.toLowerCase(Locale.ROOT));
        living.getPersistentDataContainer().set(spawnIndexKey, PersistentDataType.INTEGER, Math.max(0, spawnIndex));
        living.getPersistentDataContainer().set(dungeonLevelKey, PersistentDataType.INTEGER, Math.max(1, level));
        living.getPersistentDataContainer().set(smpMobLevelKey, PersistentDataType.INTEGER, Math.max(1, level));
        living.getPersistentDataContainer().set(smpMobDisplayNameKey, PersistentDataType.STRING, displayName);
        living.getPersistentDataContainer().set(smpMobIdKey, PersistentDataType.STRING, mobId.toLowerCase(Locale.ROOT));
        living.getPersistentDataContainer().set(smpMobCurrentHealthKey, PersistentDataType.DOUBLE, dungeonCurrentHealth(living));
        living.getPersistentDataContainer().set(smpMobMaxHealthKey, PersistentDataType.DOUBLE, dungeonMaxHealth(living));
        boolean minion = living.getPersistentDataContainer().has(temporaryMinionKey, PersistentDataType.BYTE)
                || spawnId.toLowerCase(Locale.ROOT).contains("minion");
        living.getPersistentDataContainer().set(smpEntityTypeKey, PersistentDataType.STRING,
                boss ? "DUNGEON_BOSS" : (minion ? "DUNGEON_MINION" : "DUNGEON_MOB"));
        living.getPersistentDataContainer().set(smpMobTypeKey, PersistentDataType.STRING,
                boss ? "DUNGEON_BOSS" : (minion ? "DUNGEON_MINION" : "DUNGEON_MOB"));
        living.getPersistentDataContainer().set(smpDungeonIdKey, PersistentDataType.STRING, canonicalDungeonId(dungeon));
        DungeonRun run = activeRun(dungeon);
        if (run != null) living.getPersistentDataContainer().set(smpDungeonRunIdKey, PersistentDataType.STRING, run.id);
        if (boss) {
            living.getPersistentDataContainer().set(dungeonBossKey, PersistentDataType.STRING, dungeon.toLowerCase(Locale.ROOT));
            living.getPersistentDataContainer().set(smpBossKey, PersistentDataType.BYTE, (byte) 1);
            living.getPersistentDataContainer().set(smpBossIdKey, PersistentDataType.STRING, canonicalMobId(mobId));
        } else living.getPersistentDataContainer().remove(smpBossKey);
    }

    private void updateDisplay(LivingEntity living) {
        if (isEngineMob(living)) return;
        String rawName = living.getPersistentDataContainer().get(dungeonSpawnedKey, PersistentDataType.STRING);
        int level = living.getPersistentDataContainer().getOrDefault(dungeonLevelKey, PersistentDataType.INTEGER, 1);
        double current = dungeonCurrentHealth(living);
        double max = dungeonMaxHealth(living);
        living.getPersistentDataContainer().set(smpMobCurrentHealthKey, PersistentDataType.DOUBLE, current);
        living.getPersistentDataContainer().set(smpMobMaxHealthKey, PersistentDataType.DOUBLE, max);
        living.setCustomName(CC.color(formatCleanDungeonName(rawName == null ? pretty(living.getType().name()) : rawName, level, current, max)));
        living.setCustomNameVisible(true);
    }

    private String formatCleanDungeonName(String name, int level, double health, double maxHealth) {
        return "&8[&7Lv.%level%&8] &c%name% &a%health%/%max_health%&c\u2764"
                .replace("%level%", String.valueOf(level))
                .replace("%name%", DungeonMobTemplateRegistry.cleanDisplayName(name, null))
                .replace("%health%", String.valueOf(Math.max(0, (int) Math.ceil(health))))
                .replace("%max_health%", String.valueOf((int) Math.ceil(maxHealth)));
    }

    private String formatDungeonName(String name, int level, double health, double maxHealth) {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.mob-name-format", "&8[&7Lv.%level%&8] &c%name% &a%health%&7/&a%max_health%&c❤")
                .replace("%level%", String.valueOf(level))
                .replace("%name%", name)
                .replace("%health%", String.valueOf(Math.max(0, (int) Math.ceil(health))))
                .replace("%max_health%", String.valueOf((int) Math.ceil(maxHealth)));
    }

    private void updateBossBar(LivingEntity living) {
        BossBar bar = bossBars.get(living.getUniqueId());
        if (bar == null) {
            bar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID);
            bossBars.put(living.getUniqueId(), bar);
        }
        String rawName = living.getPersistentDataContainer().get(dungeonSpawnedKey, PersistentDataType.STRING);
        int level = living.getPersistentDataContainer().getOrDefault(dungeonLevelKey, PersistentDataType.INTEGER, 1);
        double current = dungeonCurrentHealth(living);
        double max = dungeonMaxHealth(living);
        String bossId = living.getPersistentDataContainer().getOrDefault(smpBossIdKey, PersistentDataType.STRING, "");
        String title = bossId.equals("pirate_captain_blackwake")
                ? "&cCaptain Blackwake &7- &a" + (int) Math.ceil(current) + "/" + (int) Math.ceil(max) + "\u2764"
                : bossId.equals("sacred_guardian_aurelius")
                ? "&eSacred Guardian Aurelius &7- &a" + (int) Math.ceil(current) + "/" + (int) Math.ceil(max) + "\u2764"
                : formatCleanDungeonName(rawName == null ? "Dungeon Boss" : rawName, level, current, max);
        bar.setTitle(CC.color(title));
        bar.setProgress(Math.clamp(current / Math.max(1.0D, max), 0.0D, 1.0D));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(living.getWorld()) && player.getLocation().distanceSquared(living.getLocation()) <= 64.0D * 64.0D) {
                bar.addPlayer(player);
            } else {
                bar.removePlayer(player);
            }
        }
    }

    private void tickBossBars() {
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity living : world.getLivingEntities()) {
                if (living.getPersistentDataContainer().has(dungeonBossKey, PersistentDataType.STRING)) {
                    updateDisplay(living);
                    updateBossBar(living);
                }
            }
        }
        bossBars.entrySet().removeIf(entry -> {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity instanceof LivingEntity living && !living.isDead()) return false;
            entry.getValue().removeAll();
            return true;
        });
    }

    private void removeBossBar(UUID uuid) {
        BossBar bar = bossBars.remove(uuid);
        if (bar != null) bar.removeAll();
    }

    private boolean isInsideArea(String name, Location location) {
        Location pos1 = loadLocation(path(name) + ".area.pos1");
        Location pos2 = loadLocation(path(name) + ".area.pos2");
        if (pos1 == null || pos2 == null || location == null || location.getWorld() == null) return false;
        if (!location.getWorld().equals(pos1.getWorld()) || !pos1.getWorld().equals(pos2.getWorld())) return false;
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return location.getX() >= minX && location.getX() <= maxX && location.getY() >= minY && location.getY() <= maxY && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    private boolean near(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null || !a.getWorld().equals(b.getWorld())) return false;
        return a.distanceSquared(b) <= 6.25D;
    }

    private void tickSpawnPortalRotation() {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dungeons.spawn-portal.enabled", true)) return;
        Location portal = loadLocation("dungeon-spawn-portal.location");
        if (portal == null || portal.getWorld() == null) return;
        String active = getActiveSpawnPortalDungeon();
        if (active == null || !isEnabled(active)) {
            rotateSpawnPortal(false);
            return;
        }
        long next = dataManager.getData().getLong("dungeon-spawn-portal.next-rotate", 0L);
        if (next <= 0L) {
            dataManager.getData().set("dungeon-spawn-portal.next-rotate", System.currentTimeMillis() + getSpawnPortalIntervalMillis());
            dataManager.save();
            return;
        }
        if (System.currentTimeMillis() >= next) rotateSpawnPortal(true);
    }

    private List<String> getEnabledDungeons() {
        List<String> enabled = new ArrayList<>();
        ConfigurationSection section = dataManager.getData().getConfigurationSection("dungeons");
        if (section == null) return enabled;
        for (String dungeon : section.getKeys(false)) if (isEnabled(dungeon)) enabled.add(dungeon);
        return enabled;
    }

    private long getSpawnPortalIntervalMillis() {
        String mode = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.spawn-portal.rotate", "DAILY").toUpperCase();
        if (mode.equals("WEEKLY")) return 7L * 24L * 60L * 60L * 1000L;
        if (mode.equals("HOURLY")) return 60L * 60L * 1000L;
        long minutes = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("dungeons.spawn-portal.interval-minutes", 1440L);
        return Math.max(1L, minutes) * 60L * 1000L;
    }

    private void refreshSpawnPortalStructure() {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dungeons.spawn-portal.build-structure", true)) return;
        Location portal = loadLocation("dungeon-spawn-portal.location");
        if (portal == null || portal.getWorld() == null) return;
        buildPortalStructure(portal);
    }

    private void tickActivePortals() {
        ConfigurationSection section = dataManager.getData().getConfigurationSection("dungeons");
        if (section == null) return;
        long now = System.currentTimeMillis();
        for (String dungeon : section.getKeys(false)) {
            long closesAt = dataManager.getData().getLong(path(dungeon) + ".active-portal.closes-at", 0L);
            Location location = loadLocation(path(dungeon) + ".active-portal.location");
            if (location != null && closesAt > 0L && now >= closesAt) {
                clearPortalStructure(location);
                dataManager.getData().set(path(dungeon) + ".active-portal", null);
                dataManager.save();
                Bukkit.broadcastMessage(CC.color("&cThe &f" + dungeon + " &cdungeon portal has closed."));
            }
        }
    }

    private void clearSavedPortalStructure(String locationPath) {
        Location location = loadLocation(locationPath);
        if (location == null || location.getWorld() == null) return;
        clearPortalStructure(location);
    }

    private int clearTaggedPortalEntities() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!isDungeonPortalEntity(entity)) continue;
                entity.remove();
                removed++;
            }
        }
        return removed;
    }

    private boolean isDungeonPortalEntity(Entity entity) {
        return hasTag(entity, dungeonPortalKey)
                || hasTag(entity, dungeonPortalIdKey)
                || hasTag(entity, dungeonSpawnPortalKey);
    }

    private boolean hasTag(Entity entity, NamespacedKey key) {
        return entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE)
                || entity.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)
                || entity.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    private Location findSafe(World world) {
        int min = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("dungeons.random-portal.min-radius", 1000);
        int max = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("dungeons.random-portal.max-radius", 8000);
        for (int i = 0; i < 100; i++) {
            int x = random.nextInt(max * 2 + 1) - max;
            int z = random.nextInt(max * 2 + 1) - max;
            if (Math.abs(x) < min && Math.abs(z) < min) continue;
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location location = new Location(world, x + 0.5D, y, z + 0.5D);
            Material ground = location.clone().subtract(0, 1, 0).getBlock().getType();
            if (!ground.isSolid() || ground == Material.WATER || ground == Material.LAVA) continue;
            return location;
        }
        return null;
    }

    private void buildPortalStructure(Location center) {
        List<Block> blocks = portalBlocks(center);
        for (Block block : blocks) block.setType(Material.OBSIDIAN, false);
        center.getBlock().setType(Material.PURPLE_STAINED_GLASS, false);
        center.clone().add(0, 1, 0).getBlock().setType(Material.PURPLE_STAINED_GLASS, false);
        center.clone().add(0, 2, 0).getBlock().setType(Material.PURPLE_STAINED_GLASS, false);
    }

    private void clearPortalStructure(Location center) {
        for (Block block : portalBlocks(center)) block.setType(Material.AIR, false);
        center.getBlock().setType(Material.AIR, false);
        center.clone().add(0, 1, 0).getBlock().setType(Material.AIR, false);
        center.clone().add(0, 2, 0).getBlock().setType(Material.AIR, false);
    }

    private List<Block> portalBlocks(Location center) {
        List<Block> blocks = new ArrayList<>();
        World world = center.getWorld();
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();
        for (int dy = 0; dy <= 4; dy++) {
            blocks.add(world.getBlockAt(x - 2, y + dy, z));
            blocks.add(world.getBlockAt(x + 2, y + dy, z));
        }
        for (int dx = -2; dx <= 2; dx++) {
            blocks.add(world.getBlockAt(x + dx, y, z));
            blocks.add(world.getBlockAt(x + dx, y + 4, z));
        }
        return blocks;
    }

    private World getDungeonWorld(String name) {
        DungeonRun run = activeRun(name);
        if (run != null && run.instanceId != null) {
            World world = worldProvider.getWorld(run.instanceId);
            if (world != null) return world;
        }
        return Bukkit.getWorld(getWorldName(name));
    }

    private void applyDungeonWorldRulesToAll() {
        ConfigurationSection section = dataManager.getData().getConfigurationSection("dungeons");
        if (section == null) return;
        for (String dungeon : section.getKeys(false)) applyDungeonWorldRules(dungeon);
    }

    private void applyDungeonWorldRules(String name) {
        World world = getDungeonWorld(name);
        if (world == null) return;
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).contains("dungeons.arenas.random-tick-speed")) {
            world.setGameRule(org.bukkit.GameRules.RANDOM_TICK_SPEED, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("dungeons.arenas.random-tick-speed", 0));
        }
        if (isRuleEnabled("disable-fire-tick", true)) world.setGameRule(org.bukkit.GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        if (isRuleEnabled("disable-mob-griefing", true)) world.setGameRule(org.bukkit.GameRules.MOB_GRIEFING, false);
        if (isRuleEnabled("enable-keepinventory", true)) world.setGameRule(org.bukkit.GameRules.KEEP_INVENTORY, true);
        try {
            world.setDifficulty(Difficulty.valueOf(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.arenas.difficulty", "EASY").toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            world.setDifficulty(Difficulty.EASY);
        }
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.arenas.advance_weather.type", "clear").equalsIgnoreCase("clear")) {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("dungeons.arenas.advance_weather.duration", 12000));
            world.setThunderDuration(0);
        }
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.arenas.time", "day").equalsIgnoreCase("day")) world.setTime(1000L);
    }

    private String pretty(String key) {
        String[] parts = key.toLowerCase(Locale.ROOT).replace('_', ' ').split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? key : builder.toString();
    }

    private String getCreateWorldName(String name) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dungeons.swm.create-on-dungeon-create", true)) {
            return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.default-world", "dungeon");
        }
        String prefix = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.swm.world-prefix", "");
        return prefix + normalizeWorldName(name);
    }

    private String normalizeWorldName(String name) {
        String normalized = name == null ? "dungeon" : name.toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
        return normalized.isBlank() ? "dungeon" : normalized;
    }

    private double asDouble(Object value, double fallback) {
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    public static String canonicalDungeonId(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_]+", "-")
                .replaceAll("-+", "-");
    }

    private String storedDungeonKey(String name) {
        String canonicalId = canonicalDungeonId(name);
        ConfigurationSection section = dataManager.getData().getConfigurationSection("dungeons");
        if (section == null) return null;
        if (section.isConfigurationSection(canonicalId)) return canonicalId;
        for (String key : section.getKeys(false)) {
            if (canonicalDungeonId(key).equals(canonicalId)) return key;
        }
        return null;
    }

    private String path(String name) {
        String stored = storedDungeonKey(name);
        return "dungeons." + (stored == null ? canonicalDungeonId(name) : stored);
    }

    private void saveLocation(String path, Location location) {
        dataManager.getData().set(path + ".world", location.getWorld().getName());
        dataManager.getData().set(path + ".x", location.getX());
        dataManager.getData().set(path + ".y", location.getY());
        dataManager.getData().set(path + ".z", location.getZ());
        dataManager.getData().set(path + ".yaw", location.getYaw());
        dataManager.getData().set(path + ".pitch", location.getPitch());
        dataManager.save();
    }

    private Location loadLocation(String path) {
        String worldName = dataManager.getData().getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        World instanceWorld = dungeonInstanceWorldForPath(path);
        if (instanceWorld != null) world = instanceWorld;
        if (world == null) return null;
        return new Location(world,
                dataManager.getData().getDouble(path + ".x"),
                dataManager.getData().getDouble(path + ".y"),
                dataManager.getData().getDouble(path + ".z"),
                (float) dataManager.getData().getDouble(path + ".yaw"),
                (float) dataManager.getData().getDouble(path + ".pitch"));
    }

    private boolean hasSavedLocation(String path) {
        return dataManager.getData().getString(path + ".world") != null
                && dataManager.getData().contains(path + ".x") && dataManager.getData().contains(path + ".y")
                && dataManager.getData().contains(path + ".z");
    }

    private Location loadLocationInWorld(String path, World world) {
        if (world == null || !hasSavedLocation(path)) return null;
        return new Location(world, dataManager.getData().getDouble(path + ".x"),
                dataManager.getData().getDouble(path + ".y"), dataManager.getData().getDouble(path + ".z"),
                (float) dataManager.getData().getDouble(path + ".yaw"),
                (float) dataManager.getData().getDouble(path + ".pitch"));
    }

    private World dungeonInstanceWorldForPath(String path) {
        if (path == null || !path.startsWith("dungeons.")) return null;
        String[] parts = path.split("\\.");
        if (parts.length < 2) return null;
        DungeonRun run = activeRun(parts[1]);
        if (run == null || run.instanceId == null) return null;
        return worldProvider.getWorld(run.instanceId);
    }

    private boolean isEngineMob(Entity entity) {
        return plugin instanceof club.abelix.games.fakevival.Game game
                && game.getSmpMobEngine() != null
                && game.getSmpMobEngine().isEngineMob(entity);
    }

    private double engineCurrentHp(LivingEntity living) {
        if (plugin instanceof club.abelix.games.fakevival.Game game && game.getSmpMobEngine() != null) {
            return game.getSmpMobEngine().getCurrentHp(living);
        }
        return Math.max(0.0D, living.getHealth());
    }

    private double engineMaxHp(LivingEntity living) {
        if (plugin instanceof club.abelix.games.fakevival.Game game && game.getSmpMobEngine() != null) {
            return game.getSmpMobEngine().getMaxHp(living);
        }
        return Math.max(1.0D, living.getMaxHealth());
    }
}
