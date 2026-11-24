package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.mmo.MMODamageSourceTracker;
import club.abelix.games.fakevival.mmo.MMODamageSourceType;
import club.abelix.games.fakevival.mmo.MMOItemType;
import club.abelix.games.fakevival.mmo.MobHealthScaler;
import club.abelix.games.fakevival.util.CC;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ZoneManager {
    private static final double MAX_BUKKIT_HEALTH = 1024.0D;
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final CustomItemManager customItemManager;
    private final Map<String, Set<UUID>> spawned = new HashMap<>();
    private final Set<String> regeneratingBlocks = new HashSet<>();
    private final Map<UUID, Set<String>> inside = new HashMap<>();
    private final Map<UUID, UUID> bossTargets = new HashMap<>();
    private final Map<UUID, Set<UUID>> bossDeadTargets = new HashMap<>();
    private final Map<UUID, Map<String, Long>> bossAbilityCooldowns = new HashMap<>();
    private final Map<UUID, Long> bossLastDamagedAt = new HashMap<>();
    private final Set<UUID> enragedBosses = new HashSet<>();
    private final Set<UUID> processingDamage = new HashSet<>();
    private final Map<UUID, DungeonMobData> dungeonMobData = new HashMap<>();
    private final Map<String, DungeonRunState> dungeonRunStates = new HashMap<>();
    private final Map<String, Set<String>> clearedDungeonSpawns = new HashMap<>();
    private final Map<String, Integer> spawnCursor = new HashMap<>();
    private final Map<String, Long> lastSpawnDebugLog = new HashMap<>();
    private final Map<ZoneChunkKey, List<String>> zoneChunkCache = new HashMap<>();
    private final NamespacedKey zoneKey;
    private final NamespacedKey spawnKey;
    private final NamespacedKey bossKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey phaseKey;
    private final NamespacedKey bossDefenseKey;
    private final NamespacedKey bossMaxHealthKey;
    private final NamespacedKey bossShieldUntilKey;
    private final NamespacedKey bossCurrentHealthKey;
    private final NamespacedKey bossRealMaxHealthKey;
    private final NamespacedKey bossConfiguredHealthKey;
    private final NamespacedKey bossLevelKey;
    private final NamespacedKey bossNameKey;
    private final NamespacedKey isZoneBossKey;
    private final NamespacedKey dungeonMobKey;
    private final NamespacedKey mobHealthKey;
    private final NamespacedKey mobDeadKey;
    private final NamespacedKey mobRewardsProcessedKey;
    private final NamespacedKey mobLastKillerKey;
    private final NamespacedKey mobRoleKey;
    private final NamespacedKey mobEliteKey;
    private final NamespacedKey spawnIndexKey;
    private final NamespacedKey smpEntityTypeKey;
    private final NamespacedKey smpMobTypeKey;
    private final NamespacedKey smpMobIdKey;
    private final NamespacedKey smpMobLevelKey;
    private final NamespacedKey smpMobCurrentHealthKey;
    private final NamespacedKey smpMobMaxHealthKey;
    private final NamespacedKey smpMobDisplayNameKey;
    private final NamespacedKey smpZoneIdKey;
    private final NamespacedKey smpBossKey;
    private final MobHealthScaler healthScaler;
    private final Set<String> warnedDefenseClamps = new HashSet<>();
    private final AtomicBoolean dungeonSpawnTickRunning = new AtomicBoolean(false);
    private volatile boolean combatDebug;
    private int taskId = -1;
    private static final String DEFAULT_DUNGEON_BOSSES = """
            ember_warden|&cEmber Warden|ZOMBIE|15|12500|40|10|15|0|0|&c%boss% &8> &7Another fool enters my flames.~&c%boss% &8> &7Burn with the caverns!|30|&c%boss% &8> &7My fire will not fade!|false|false|false|0|2|BLAZE|Ember Shard~Burning Core~Ember Blade
            crystal_golem|&bCrystal Golem|IRON_GOLEM|20|25000|35|25|10|0|0|&c%boss% &8> &7The mines reject your presence.~&c%boss% &8> &7Stone breaks. So will you.|35|&c%boss% &8> &7My core is cracking...|false|false|false|5|0|ZOMBIE|Crystal Fragment~Golem Plate~Crystal Hammer
            drowned_captain|&3Drowned Captain|DROWNED|18|18000|40|45|20|0|0|&c%boss% &8> &7The sea keeps what it claims.~&c%boss% &8> &7Come closer, landwalker.|30|&c%boss% &8> &7The tide turns against me...|false|false|false|0|2|DROWNED|Captain's Trident~Sunken Pearl~Ancient Scale
            venom_broodmother|&2Venom Broodmother|SPIDER|22|30000|45|20|45|0|0|&c%boss% &8> &7My children hunger.~&c%boss% &8> &7Swarm them!|35|&c%boss% &8> &7You will choke on venom!|false|false|false|0|4|CAVE_SPIDER|Venom Fang~Brood Silk~Spider Dagger
            frost_revenant|&bFrost Revenant|STRAY|25|38000|40|15|20|0|15|&c%boss% &8> &7Your warmth does not belong here.~&c%boss% &8> &7Freeze where you stand.|30|&c%boss% &8> &7The cold will remember me...|false|true|false|0|0|ZOMBIE|Frozen Bone~Revenant Ice~Frost Bow
            ancient_treant|&aAncient Treant|IRON_GOLEM|24|42000|35|35|25|0|0|&c%boss% &8> &7The roots awaken.~&c%boss% &8> &7You cut too deep.|40|&c%boss% &8> &7The forest lends me strength.|false|false|false|10|2|ZOMBIE|Ancient Bark~Living Root~Treant Axe
            magma_colossus|&6Magma Colossus|MAGMA_CUBE|30|60000|40|25|35|0|0|&c%boss% &8> &7The depths are alive.~&c%boss% &8> &7Melt beneath me!|25|&c%boss% &8> &7The core erupts!|false|false|false|0|4|MAGMA_CUBE|Molten Core~Magma Plate~Colossus Chestplate
            shadow_assassin|&5Shadow Assassin|WITHER_SKELETON|28|45000|45|5|0|0|45|&c%boss% &8> &7You heard the blade too late.~&c%boss% &8> &7Behind you.|30|&c%boss% &8> &7You are harder to kill than expected.|false|true|false|0|0|ZOMBIE|Shadow Cloth~Assassin Fang~Shadow Katana
            royal_guardian|&6Royal Guardian|ZOMBIFIED_PIGLIN|32|70000|35|30|20|0|0|&c%boss% &8> &7You stand before the lost crown.~&c%boss% &8> &7Kneel.|30|&c%boss% &8> &7The kingdom still stands!|false|false|false|8|0|ZOMBIE|Royal Crest~Guardian Plate~Royal Spear
            withered_alchemist|&8Withered Alchemist|WITCH|27|40000|45|0|20|0|15|&c%boss% &8> &7A perfect brew requires suffering.~&c%boss% &8> &7Drink deeply.|35|&c%boss% &8> &7The final mixture is unstable!|false|false|false|6|0|ZOMBIE|Alchemist Dust~Withered Flask~Alchemist Staff
            void_watcher|&5Void Watcher|ENDERMAN|35|85000|40|35|0|0|50|&c%boss% &8> &7The rift has already seen your end.~&c%boss% &8> &7Reality bends.|25|&c%boss% &8> &7The void is pulling me back...|false|true|false|0|0|ZOMBIE|Void Pearl~Rift Essence
            sand_pharaoh|&eSand Pharaoh|HUSK|26|50000|40|20|35|0|0|&c%boss% &8> &7You disturb a buried throne.~&c%boss% &8> &7The tomb guards answer.|35|&c%boss% &8> &7My dynasty is eternal!|false|false|false|0|3|HUSK|Pharaoh's Jewel~Sand Relic~Pharaoh Blade
            deepstone_behemoth|&7Deepstone Behemoth|RAVAGER|38|110000|30|40|10|0|0|&c%boss% &8> &7The quarry trembles.~&c%boss% &8> &7I will bury you in stone.|25|&c%boss% &8> &7The mountain will fall with me!|true|false|false|0|0|ZOMBIE|Deepstone Chunk~Behemoth Horn
            soul_harvester|&5Soul Harvester|SKELETON|31|65000|45|0|25|0|15|&c%boss% &8> &7Your soul is ripe.~&c%boss% &8> &7Run, and I will harvest fear instead.|30|&c%boss% &8> &7One more soul...|false|false|false|8|0|ZOMBIE|Soul Fragment~Haunted Bone~Harvester Bow
            jungle_tyrant|&2Jungle Tyrant|RAVAGER|34|90000|35|35|20|0|0|&c%boss% &8> &7The jungle bows to me.~&c%boss% &8> &7You are prey here.|30|&c%boss% &8> &7The vines will drag you under!|false|false|false|0|2|ZOMBIE|Tyrant Hide~Jungle Fang
            stormcaller_shaman|&bStormcaller Shaman|ZOMBIE|36|80000|45|25|0|45|0|&c%boss% &8> &7The storm has chosen its target.~&c%boss% &8> &7Lightning, answer me!|30|&c%boss% &8> &7The storm will not end with me!|false|false|false|0|0|ZOMBIE|Storm Crystal~Shaman Totem~Storm Staff
            bloodfang_alpha|&cBloodfang Alpha|WOLF|29|55000|40|20|40|0|0|&c%boss% &8> &7The pack smells weakness.~&c%boss% &8> &7Tear them apart!|30|&c%boss% &8> &7The alpha never falls alone!|false|false|false|0|3|WOLF|Bloodfang~Alpha Pelt~Bloodfang Dagger
            obsidian_sentinel|&8Obsidian Sentinel|IRON_GOLEM|40|130000|30|45|10|0|0|&c%boss% &8> &7Fortress defense awakened.~&c%boss% &8> &7Crushing force deployed.|25|&c%boss% &8> &7Armor integrity failing.|false|false|false|5|0|ZOMBIE|Obsidian Core~Sentinel Plate~Obsidian Hammer
            plague_doctor|&aPlague Doctor|WITCH|33|72000|45|0|25|0|15|&c%boss% &8> &7This cure may hurt.~&c%boss% &8> &7Breathe in.|35|&c%boss% &8> &7The infection spreads faster now.|false|false|false|0|2|ZOMBIE|Plague Mask~Toxic Vial~Plague Wand
            celestial_knight|&fCelestial Knight|ZOMBIE|45|175000|40|35|2|25|15|&c%boss% &8> &7Only the worthy may enter the keep.~&c%boss% &8> &7Light shall cut through you!|25|&c%boss% &8> &7I will not fall while the stars remain!|false|false|false|10|2|ZOMBIE|Celestial Fragment~Knight's Emblem~Celestial Blade
            """;

    public ZoneManager(JavaPlugin plugin, DataManager dataManager, EconomyManager economyManager, CustomItemManager customItemManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.economyManager = economyManager;
        this.customItemManager = customItemManager;
        this.zoneKey = new NamespacedKey(plugin, "zone_mob_zone");
        this.spawnKey = new NamespacedKey(plugin, "zone_mob_spawn");
        this.bossKey = new NamespacedKey(plugin, "zone_boss");
        this.levelKey = new NamespacedKey(plugin, "zone_mob_level");
        this.phaseKey = new NamespacedKey(plugin, "zone_boss_phase");
        this.bossDefenseKey = new NamespacedKey(plugin, "mmo_defense");
        this.bossMaxHealthKey = new NamespacedKey(plugin, "mmo_max_health");
        this.bossShieldUntilKey = new NamespacedKey(plugin, "zone_boss_shield_until");
        this.bossCurrentHealthKey = new NamespacedKey(plugin, "boss_current_health");
        this.bossRealMaxHealthKey = new NamespacedKey(plugin, "boss_max_health");
        this.bossConfiguredHealthKey = new NamespacedKey(plugin, "boss_configured_health");
        this.bossLevelKey = new NamespacedKey(plugin, "boss_level");
        this.bossNameKey = new NamespacedKey(plugin, "boss_name");
        this.isZoneBossKey = new NamespacedKey(plugin, "is_zone_boss");
        this.dungeonMobKey = new NamespacedKey(plugin, "dungeon_mob");
        this.mobHealthKey = new NamespacedKey(plugin, "mmo_health");
        this.mobDeadKey = new NamespacedKey(plugin, "dungeon_mob_dead");
        this.mobRewardsProcessedKey = new NamespacedKey(plugin, "dungeon_mob_death_processed");
        this.mobLastKillerKey = new NamespacedKey(plugin, "dungeon_mob_last_killer");
        this.mobRoleKey = new NamespacedKey(plugin, "dungeon_mob_role");
        this.mobEliteKey = new NamespacedKey(plugin, "dungeon_mob_elite");
        this.spawnIndexKey = new NamespacedKey(plugin, "spawn_index");
        this.smpEntityTypeKey = new NamespacedKey(plugin, "smp_entity_type");
        this.smpMobTypeKey = new NamespacedKey(plugin, "smp_mob_type");
        this.smpMobIdKey = new NamespacedKey(plugin, "smp_mob_id");
        this.smpMobLevelKey = new NamespacedKey(plugin, "smp_mob_level");
        this.smpMobCurrentHealthKey = new NamespacedKey(plugin, "smp_mob_current_health");
        this.smpMobMaxHealthKey = new NamespacedKey(plugin, "smp_mob_max_health");
        this.smpMobDisplayNameKey = new NamespacedKey(plugin, "smp_mob_display_name");
        this.smpZoneIdKey = new NamespacedKey(plugin, "smp_zone_id");
        this.smpBossKey = new NamespacedKey(plugin, "smp_boss");
        this.healthScaler = new MobHealthScaler(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin));
    }

    public void start() {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("zones.enabled", true)) return;
        registerDefaultDungeonBossTemplates();
        repairPharaohTemplateStats();
        long interval = Math.max(20L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("zones.spawn-scan-interval-ticks", 100L));
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tickMobZones, interval, interval).getTaskId();
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        clearAllSpawned();
    }

    public void create(String name, String type) {
        zoneChunkCache.clear();
        String path = path(name);
        dataManager.getData().set(path + ".name", name.toLowerCase());
        dataManager.getData().set(path + ".display", pretty(name));
        applyType(name, type);
        dataManager.save();
    }

    public void remove(String name) {
        zoneChunkCache.clear();
        despawnZone(name);
        dataManager.getData().set(path(name), null);
        dataManager.save();
    }

    public boolean exists(String name) {
        return name != null && dataManager.getData().isConfigurationSection(path(name));
    }

    public void setPos(String name, String pos, Location location) {
        zoneChunkCache.clear();
        if (!exists(name)) create(name, "safe");
        dataManager.getData().set(path(name) + ".zone_shape", "CUBOID");
        dataManager.getData().set(path(name) + ".points_json", null);
        saveLocation(path(name) + "." + pos.toLowerCase(), location);
    }

    public boolean setAreaFromSelection(String name, AdminWorldEditManager.SelectionSnapshot selection, boolean create) {
        if (selection == null || selection.world() == null) return false;
        if (!exists(name)) { if (!create) return false; create(name, "safe"); }
        World world = Bukkit.getWorld(selection.world());
        if (world == null) return false;
        String base = path(name);
        zoneChunkCache.clear();
        dataManager.getData().set(base + ".zone_shape", selection.type().name());
        dataManager.getData().set(base + ".min_y", selection.minY());
        dataManager.getData().set(base + ".max_y", selection.maxY());
        if (selection.type() == AdminWorldEditManager.SelectionType.CUBOID) {
            AdminWorldEditManager.SelectionPoint a=selection.pos1(),b=selection.pos2();
            if(a==null||b==null)return false;
            saveLocation(base+".pos1",new Location(world,a.x(),a.y(),a.z()));
            saveLocation(base+".pos2",new Location(world,b.x(),b.y(),b.z()));
            dataManager.getData().set(base + ".points_json", null);
        } else {
            JsonArray points = new JsonArray();
            int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
            for(AdminWorldEditManager.SelectionPoint point:selection.points()){JsonObject json=new JsonObject();json.addProperty("x",point.x());json.addProperty("z",point.z());points.add(json);minX=Math.min(minX,point.x());maxX=Math.max(maxX,point.x());minZ=Math.min(minZ,point.z());maxZ=Math.max(maxZ,point.z());}
            if(points.size()<3)return false;
            dataManager.getData().set(base+".points_json",points.toString());
            saveLocation(base+".pos1",new Location(world,minX,selection.minY(),minZ));
            saveLocation(base+".pos2",new Location(world,maxX,selection.maxY(),maxZ));
        }
        dataManager.save();
        return true;
    }

    public AdminWorldEditManager.SelectionSnapshot getAreaSelection(String name) {
        if (!exists(name)) return null;
        String base=path(name);Location pos1=loadLocation(base+".pos1"),pos2=loadLocation(base+".pos2");
        if(pos1==null||pos2==null||pos1.getWorld()==null||!pos1.getWorld().equals(pos2.getWorld()))return null;
        int minY=dataManager.getData().getInt(base+".min_y",Math.min(pos1.getBlockY(),pos2.getBlockY()));
        int maxY=dataManager.getData().getInt(base+".max_y",Math.max(pos1.getBlockY(),pos2.getBlockY()));
        if(dataManager.getData().getString(base+".zone_shape","CUBOID").equalsIgnoreCase("POLY")){
            List<AdminWorldEditManager.SelectionPoint>points=polyPoints(name).stream().map(point->new AdminWorldEditManager.SelectionPoint((int)Math.round(point.x),minY,(int)Math.round(point.z))).toList();
            if(points.size()<3)return null;
            return new AdminWorldEditManager.SelectionSnapshot(AdminWorldEditManager.SelectionType.POLY,pos1.getWorld().getName(),minY,maxY,null,null,points);
        }
        return new AdminWorldEditManager.SelectionSnapshot(AdminWorldEditManager.SelectionType.CUBOID,pos1.getWorld().getName(),minY,maxY,new AdminWorldEditManager.SelectionPoint(pos1.getBlockX(),pos1.getBlockY(),pos1.getBlockZ()),new AdminWorldEditManager.SelectionPoint(pos2.getBlockX(),pos2.getBlockY(),pos2.getBlockZ()),List.of());
    }

    public void setType(String name, String type) {
        if (!exists(name)) create(name, type);
        applyType(name, type);
        dataManager.save();
    }

    public boolean setWarp(String name, Location location) {
        if (!exists(name) || location == null || location.getWorld() == null) return false;
        String base = path(name) + ".warp";
        dataManager.getData().set(base + ".enabled", true);
        saveLocation(base, location);
        return true;
    }

    public void addSpawn(String name, EntityType type, int level, Location location) {
        addSpawn(name, type, level, location, null);
    }

    public boolean addRoleSpawn(String name, String roleName, int level, Location location) {
        DungeonMobRole role;
        try {
            role = DungeonMobRole.valueOf(normalizeType(roleName));
        } catch (IllegalArgumentException exception) {
            return false;
        }
        EntityType type = switch (role) {
            case GRUNT -> EntityType.ZOMBIE;
            case GUARD -> EntityType.HUSK;
            case ARCHER -> EntityType.SKELETON;
            case ASSASSIN -> EntityType.CAVE_SPIDER;
            case MAGE -> EntityType.WITCH;
            case BRUTE -> EntityType.VINDICATOR;
            case HEALER -> EntityType.EVOKER;
            case ELITE -> EntityType.PIGLIN_BRUTE;
        };
        addSpawn(name, type, level, location, role);
        return true;
    }

    private void addSpawn(String name, EntityType type, int level, Location location, DungeonMobRole role) {
        if (!exists(name)) create(name, "combat");
        String base = path(name) + ".spawns." + System.currentTimeMillis();
        dataManager.getData().set(base + ".type", type.name());
        dataManager.getData().set(base + ".level", Math.max(1, level));
        if (role != null) {
            dataManager.getData().set(base + ".role", role.name());
            dataManager.getData().set(base + ".name", "&cDungeon " + pretty(role.name()));
            if (role == DungeonMobRole.ELITE) dataManager.getData().set(base + ".elite", true);
        }
        saveLocation(base + ".location", location);
        dataManager.getData().set(path(name) + ".allow-mobs", true);
        dataManager.save();
        spawnZone(name);
    }

    public void addBossSpawn(String name, EntityType type, int level, String bossName, Location location) {
        if (!exists(name)) create(name, "dungeon");
        String base = path(name) + ".boss-spawns." + System.currentTimeMillis();
        dataManager.getData().set(base + ".type", type.name());
        dataManager.getData().set(base + ".level", Math.max(1, level));
        dataManager.getData().set(base + ".name", bossName == null || bossName.isBlank() ? "&c" + pretty(name) + " Boss" : bossName);
        dataManager.getData().set(base + ".health", defaultBossHealth());
        dataManager.getData().set(base + ".drop-commands", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("zones.default-boss-drop-commands"));
        dataManager.getData().set(base + ".lootbox-chance", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.boss-lootbox-drop-chance", 20.0D));
        saveLocation(base + ".location", location);
        dataManager.getData().set(path(name) + ".allow-mobs", true);
        dataManager.save();
        spawnZone(name);
    }

    public boolean addBossSpawnTemplate(String name, String templateId, int levelOverride, String bossNameOverride, Location location) {
        ConfigurationSection template = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("zones.boss-templates." + templateId.toLowerCase(Locale.ROOT));
        if (template == null) return false;
        if (!exists(name)) create(name, "dungeon");

        String base = path(name) + ".boss-spawns." + System.currentTimeMillis();
        int level = levelOverride > 0 ? levelOverride : Math.max(1, template.getInt("level", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("zones.default-boss-level", 10)));
        String display = bossNameOverride == null || bossNameOverride.isBlank()
                ? template.getString("name", "&c" + pretty(templateId) + " Boss")
                : bossNameOverride;

        dataManager.getData().set(base + ".template", templateId.toLowerCase(Locale.ROOT));
        dataManager.getData().set(base + ".boss_id", template.getString("boss_id", templateId.toLowerCase(Locale.ROOT)));
        dataManager.getData().set(base + ".boss_type", "ZONE_BOSS");
        dataManager.getData().set(base + ".type", template.getString("type", "ZOMBIE"));
        dataManager.getData().set(base + ".level", level);
        dataManager.getData().set(base + ".name", display);
        dataManager.getData().set(base + ".health", Math.max(1.0D, template.getDouble("health", defaultBossHealth())));
        if (template.contains("defense")) dataManager.getData().set(base + ".defense", Math.max(0.0D, template.getDouble("defense")));
        if (template.contains("damage")) dataManager.getData().set(base + ".damage", Math.max(0.0D, template.getDouble("damage")));
        if (template.contains("tier")) dataManager.getData().set(base + ".tier", template.getString("tier", "normal"));
        dataManager.getData().set(base + ".drop-commands", template.getStringList("drop-commands").isEmpty()
                ? club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("zones.default-boss-drop-commands")
                : template.getStringList("drop-commands"));
        dataManager.getData().set(base + ".lootbox-chance", template.getDouble("lootbox-chance", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.boss-lootbox-drop-chance", 20.0D)));
        copySection(template.getConfigurationSection("abilities"), ensureDataSection(base + ".abilities"));
        copySection(template.getConfigurationSection("model"), ensureDataSection(base + ".model"));
        dataManager.getData().set(base + ".rare-drops", template.getMapList("rare-drops"));
        saveLocation(base + ".location", location);
        dataManager.getData().set(path(name) + ".allow-mobs", true);
        dataManager.save();
        spawnZone(name);
        return true;
    }

    public List<String> getBossTemplateIds() {
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("zones.boss-templates");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    public void checkMove(Player player) {
        List<String> currentZones = zonesAt(player.getLocation());
        Set<String> previous = inside.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>());
        for (String zone : currentZones) {
            String key = zone.toLowerCase();
            if (previous.add(key)) discover(player, zone);
        }
        previous.removeIf(zone -> currentZones.stream().noneMatch(current -> current.equalsIgnoreCase(zone)));
    }

    public boolean isZoneMob(Entity entity) {
        return entity instanceof LivingEntity living && living.getPersistentDataContainer().has(zoneKey, PersistentDataType.STRING);
    }

    public boolean isZoneBoss(Entity entity) {
        return entity instanceof LivingEntity living && living.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING);
    }

    public boolean isZoneDungeonBoss(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !isZoneBoss(living)) return false;
        String zone = living.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING);
        return isDungeonZone(zone);
    }

    public boolean isZoneDungeonMob(Entity entity) {
        return entity instanceof LivingEntity living
                && living.getPersistentDataContainer().has(dungeonMobKey, PersistentDataType.BYTE);
    }

    public BossHealthValidation validateBossHealthScaling() {
        double normalLevel15 = healthScaler.normalHealth(15);
        double pharaohHealth = healthScaler.zoneBossHealth(15, 12000.0D);
        double minimumMultiplier = Math.max(1.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getDouble("bosses.zone.minimumOverNormalMultiplier", 5.0D));
        boolean allBossesMeetMinimum = true;
        ConfigurationSection zones = dataManager.getData().getConfigurationSection("map-zones");
        if (zones != null) {
            for (String zone : zones.getKeys(false)) {
                ConfigurationSection bosses = dataManager.getData().getConfigurationSection(path(zone) + ".boss-spawns");
                if (bosses == null) continue;
                for (String key : bosses.getKeys(false)) {
                    int level = Math.max(1, bosses.getInt(key + ".level", 1));
                    double configured = Math.max(0.0D, bosses.getDouble(key + ".health", 0.0D));
                    double normal = healthScaler.normalHealth(level);
                    if (healthScaler.zoneBossHealth(level, configured) + 0.01D < normal * minimumMultiplier) {
                        allBossesMeetMinimum = false;
                    }
                    String template = bosses.getString(key + ".template", "");
                    String bossId = bosses.getString(key + ".boss_id", "");
                    if (template.equalsIgnoreCase("tomb_pharaoh") || bossId.equalsIgnoreCase("pharaoh")) {
                        pharaohHealth = healthScaler.zoneBossHealth(level, configured);
                    }
                }
            }
        }
        return new BossHealthValidation(normalLevel15, pharaohHealth, pharaohHealth > normalLevel15,
                allBossesMeetMinimum, smpMobCurrentHealthKey != null && smpMobMaxHealthKey != null
                && smpMobTypeKey != null && smpBossKey != null, MAX_BUKKIT_HEALTH <= 1024.0D,
                pharaohHealth >= 10000.0D && pharaohHealth <= 15000.0D);
    }

    public record BossHealthValidation(double normalLevel15Health, double pharaohLevel15Health,
                                       boolean pharaohHigherThanNormal, boolean allBossesMeetMinimum,
                                       boolean pdcHealthSource, boolean safeBukkitHealth,
                                       boolean pharaohTargetRange) {
    }

    public boolean hasEventBossZone() {
        ConfigurationSection zones = dataManager.getData().getConfigurationSection("map-zones");
        if (zones == null) return false;
        for (String zone : zones.getKeys(false)) {
            if (!isDungeonZone(zone)) continue;
            ConfigurationSection bosses = dataManager.getData().getConfigurationSection(path(zone) + ".boss-spawns");
            if (bosses != null && !bosses.getKeys(false).isEmpty()) return true;
        }
        return false;
    }

    public boolean hasConfiguredZones() {
        ConfigurationSection section = dataManager.getData().getConfigurationSection("map-zones");
        return section != null && !section.getKeys(false).isEmpty();
    }

    public List<String> debugDungeonMobLines() {
        List<String> lines = new ArrayList<>();
        ConfigurationSection zones = dataManager.getData().getConfigurationSection("map-zones");
        if (zones == null) return lines;
        for (String zone : zones.getKeys(false)) {
            if (!isDungeonZone(zone)) continue;
            Set<UUID> current = spawned.computeIfAbsent(zone.toLowerCase(Locale.ROOT), ignored -> new HashSet<>());
            reconcileLoadedZoneMobs(zone, current);
            cleanTrackedMobs(zone, current);
            int zoneAlive = 0;
            Map<String, Integer> aliveBySpawn = new HashMap<>();
            for (UUID uuid : current) {
                Entity entity = Bukkit.getEntity(uuid);
                if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) continue;
                zoneAlive++;
                String spawn = living.getPersistentDataContainer().get(spawnKey, PersistentDataType.STRING);
                if (spawn != null) aliveBySpawn.merge(spawn, 1, Integer::sum);
            }
            lines.add("&6Zone " + zone + ": &f" + zoneAlive + " alive");
            ConfigurationSection spawns = dataManager.getData().getConfigurationSection(path(zone) + ".spawns");
            if (spawns == null) continue;
            for (String spawn : spawns.getKeys(false)) {
                String base = path(zone) + ".spawns." + spawn;
                int configured = Math.max(1, dataManager.getData().contains(base + ".count")
                        ? dataManager.getData().getInt(base + ".count", 1) : dataManager.getData().getInt(base + ".amount", 1));
                int alive = aliveBySpawn.getOrDefault(spawn, 0);
                lines.add("&eSpawn " + spawn + ": &a" + alive + "/" + configured + " alive &7(missing: "
                        + Math.max(0, configured - alive) + ", duplicates: " + Math.max(0, alive - configured) + ")");
            }
        }
        return lines;
    }

    public String startEventBossInvasion() {
        ConfigurationSection zones = dataManager.getData().getConfigurationSection("map-zones");
        if (zones == null) return null;
        List<String> candidates = new ArrayList<>();
        for (String zone : zones.getKeys(false)) {
            if (!isDungeonZone(zone) || !hasNearbyPlayer(zone)) continue;
            ConfigurationSection bosses = dataManager.getData().getConfigurationSection(path(zone) + ".boss-spawns");
            if (bosses != null && !bosses.getKeys(false).isEmpty()) candidates.add(zone);
        }
        if (candidates.isEmpty()) return null;
        String zone = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        ConfigurationSection bosses = dataManager.getData().getConfigurationSection(path(zone) + ".boss-spawns");
        if (bosses != null) {
            for (String key : bosses.getKeys(false)) {
                dataManager.getData().set(path(zone) + ".boss-spawns." + key + ".next-respawn", 0L);
            }
        }
        spawnZone(zone);
        dataManager.save();
        return dataManager.getData().getString(path(zone) + ".display", pretty(zone));
    }

    public double getBossDefense(LivingEntity entity) {
        if (!isZoneDungeonBoss(entity)) return 0.0D;
        return Math.max(0.0D, entity.getPersistentDataContainer().getOrDefault(bossDefenseKey, PersistentDataType.DOUBLE, 0.0D));
    }

    public double minimumBossDamageMultiplier() {
        double percent = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getDouble("zones.boss-minimum-damage-percent", 10.0D);
        return Math.clamp(percent / 100.0D, 0.0D, 1.0D);
    }

    public double bossShieldDamageMultiplier(LivingEntity entity) {
        if (!isZoneDungeonBoss(entity)) return 1.0D;
        long until = entity.getPersistentDataContainer().getOrDefault(bossShieldUntilKey, PersistentDataType.LONG, 0L);
        return System.currentTimeMillis() < until ? 0.5D : 1.0D;
    }

    public ZoneSnapshot getSnapshot(Player player) {
        if (player == null || !player.isOnline()) return ZoneSnapshot.NONE;
        List<String> zones = zonesAt(player.getLocation());
        if (zones.isEmpty()) return new ZoneSnapshot("Wilderness", 0, false, 0, "None", 0);
        String zone = zones.stream().filter(this::isDungeonZone).findFirst().orElse(zones.getFirst());
        String base = path(zone);
        String display = dataManager.getData().getString(base + ".display", pretty(zone));
        int level = Math.max(0, dataManager.getData().getInt(base + ".level",
                dataManager.getData().getInt(base + ".recommended-level", 0)));
        boolean dungeon = isDungeonZone(zone);
        if (!dungeon) return new ZoneSnapshot(display, level, false, 0, "None", 0);

        Set<UUID> tracked = spawned.getOrDefault(zone.toLowerCase(Locale.ROOT), Set.of());
        int alive = 0;
        boolean bossAlive = false;
        for (UUID uuid : tracked) {
            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || living.isDead()) continue;
            alive++;
            if (isZoneBoss(living)) bossAlive = true;
        }
        ConfigurationSection mobs = dataManager.getData().getConfigurationSection(base + ".spawns");
        ConfigurationSection bosses = dataManager.getData().getConfigurationSection(base + ".boss-spawns");
        int total = (mobs == null ? 0 : mobs.getKeys(false).size()) + (bosses == null ? 0 : bosses.getKeys(false).size());
        int progress = total <= 0 ? 0 : Math.clamp((int) Math.round((total - Math.min(total, alive)) * 100.0D / total), 0, 100);
        return new ZoneSnapshot(display, level, true, alive, bossAlive ? "Alive" : "Defeated", progress);
    }

    public record ZoneSnapshot(String name, int level, boolean dungeon, int mobsAlive, String bossStatus, int progress) {
        private static final ZoneSnapshot NONE = new ZoneSnapshot("Wilderness", 0, false, 0, "None", 0);
    }

    public BossSnapshot getBossSnapshot(Player player) {
        if (player == null || !player.isOnline()) return BossSnapshot.NONE;
        for (String zone : zonesAt(player.getLocation())) {
            ConfigurationSection bosses = dataManager.getData().getConfigurationSection(path(zone) + ".boss-spawns");
            if (bosses == null) continue;
            for (String key : bosses.getKeys(false)) {
                String active = dataManager.getData().getString(path(zone) + ".boss-spawns." + key + ".active", "");
                if (active.isBlank()) continue;
                try {
                    Entity entity = Bukkit.getEntity(UUID.fromString(active));
                    if (!(entity instanceof LivingEntity living) || living.isDead() || !player.canSee(living)) continue;
                    String name = dataManager.getData().getString(path(zone) + ".boss-spawns." + key + ".name", pretty(key));
                    DungeonMobData health = dungeonMobData.computeIfAbsent(living.getUniqueId(), ignored -> readDungeonMobData(living));
                    return new BossSnapshot(true, ChatColor.stripColor(CC.color(name)), "Alive",
                            (int) Math.ceil(health == null ? bossDisplayCurrentHealth(living) : health.currentHealth),
                            (int) Math.ceil(health == null ? bossDisplayMaxHealth(living) : health.maxHealth));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return BossSnapshot.NONE;
    }

    public record BossSnapshot(boolean active, String name, String status, int health, int maxHealth) {
        private static final BossSnapshot NONE = new BossSnapshot(false, "None", "None", 0, 0);
    }

    public void handleZoneMobDamage(EntityDamageEvent event, Player attacker) {
        long started = combatDebug ? System.nanoTime() : 0L;
        if (!(event.getEntity() instanceof LivingEntity entity) || !isZoneDungeonMob(entity)) return;
        if (isEngineMob(entity)) return;
        UUID uuid = entity.getUniqueId();
        if (!processingDamage.add(uuid)) {
            return;
        }
        try {
            if (entity.isDead() || entity.getPersistentDataContainer().has(mobDeadKey, PersistentDataType.BYTE)) {
                event.setCancelled(true);
                return;
            }
            DungeonMobData data = dungeonMobData.computeIfAbsent(uuid, ignored -> readDungeonMobData(entity));
            if (data == null) return;
            if (!data.boss && entity.getHealth() > data.currentHealth) {
                data.currentHealth = Math.min(data.maxHealth, entity.getHealth());
            }

            double damage = Math.max(0.0D, event.getDamage());
            double multiplier = 100.0D / (100.0D + Math.max(0.0D, data.defense));
            if (data.boss) {
                multiplier = Math.max(minimumBossDamageMultiplier(), multiplier) * bossShieldDamageMultiplier(entity);
            } else {
                multiplier = Math.max(data.elite ? 0.10D : 0.15D, multiplier);
                if (System.currentTimeMillis() < data.guardUntil) multiplier *= 0.70D;
            }
            double applied = damage <= 0.0D ? 0.0D : Math.max(data.boss ? damage * 0.10D : 1.0D, damage * multiplier);
            if (attacker != null) {
                entity.getPersistentDataContainer().set(mobLastKillerKey, PersistentDataType.STRING, attacker.getUniqueId().toString());
                data.lastDamagedAt = System.currentTimeMillis();
                if (entity instanceof Mob mob && validDungeonTarget(attacker, entity.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING), entity.getLocation(), 30.0D)) {
                    mob.setTarget(attacker);
                }
            }
            if (data.boss) {
                data.currentHealth = Math.max(0.0D, data.currentHealth - applied);
                writeBossHealth(entity, data.currentHealth, data.maxHealth);
                if (data.currentHealth <= 0.0D) {
                    entity.getPersistentDataContainer().set(mobDeadKey, PersistentDataType.BYTE, (byte) 1);
                    event.setDamage(Math.max(0.01D, entity.getHealth()));
                    return;
                }
                event.setDamage(Math.min(Math.max(0.01D, applied), Math.max(0.01D, entity.getHealth() - 0.01D)));
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (entity.isDead() || !entity.isValid()) return;
                    syncBossBukkitHealth(entity, data.currentHealth, data.maxHealth);
                    updateMobName(entity);
                });
                return;
            }
            event.setDamage(applied);
            Bukkit.getScheduler().runTask(plugin, () -> syncDungeonMobHealthAfterHit(entity, uuid));
        } finally {
            processingDamage.remove(uuid);
            if (combatDebug) {
                double elapsedMs = (System.nanoTime() - started) / 1_000_000.0D;
                if (elapsedMs > 10.0D) {
                    String zone = entity.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING);
                    String weapon = attacker == null ? "environment" : String.valueOf(customItemManager.getItemId(attacker.getInventory().getItemInMainHand()));
                    plugin.getLogger().warning(String.format(Locale.US,
                            "[SMP] Slow mob damage event: %.2fms mob=%s zone=%s weapon=%s",
                            elapsedMs, entity.getType(), zone == null ? "none" : zone, weapon));
                }
            }
        }
    }

    public boolean toggleCombatDebug() {
        combatDebug = !combatDebug;
        return combatDebug;
    }

    private void syncDungeonMobHealthAfterHit(LivingEntity entity, UUID uuid) {
        if (entity == null || entity.isDead() || !entity.isValid() || !isZoneDungeonMob(entity)) return;
        DungeonMobData data = dungeonMobData.get(uuid);
        if (data == null) return;
        data.currentHealth = Math.clamp(entity.getHealth(), 0.0D, data.maxHealth);
        entity.getPersistentDataContainer().set(mobHealthKey, PersistentDataType.DOUBLE, data.currentHealth);
        updateMobName(entity);
    }

    public Player getZoneMobKiller(LivingEntity entity) {
        if (entity == null) return null;
        String value = entity.getPersistentDataContainer().get(mobLastKillerKey, PersistentDataType.STRING);
        if (value == null) return null;
        try {
            return Bukkit.getPlayer(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public boolean allowDungeonMobAttack(LivingEntity entity) {
        if (!isZoneDungeonMob(entity) || isZoneBoss(entity)) return true;
        DungeonMobData data = dungeonMobData.computeIfAbsent(entity.getUniqueId(), ignored -> readDungeonMobData(entity));
        if (data == null) return true;
        long now = System.currentTimeMillis();
        long delay = switch (data.role) {
            case ASSASSIN -> 800L;
            case BRUTE, ELITE -> 1_800L;
            case GUARD -> 1_400L;
            case HEALER, MAGE, ARCHER -> 1_250L;
            default -> 1_050L;
        };
        if (now - data.lastBasicAttack < delay) return false;
        data.lastBasicAttack = now;
        return true;
    }

    public void handleBossDeath(LivingEntity entity, Player killer) {
        if (!claimZoneMobDeath(entity)) return;
        String zone = entity.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING);
        String key = entity.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
        if (zone == null || key == null) return;
        bossTargets.remove(entity.getUniqueId());
        bossDeadTargets.remove(entity.getUniqueId());
        bossAbilityCooldowns.remove(entity.getUniqueId());
        bossLastDamagedAt.remove(entity.getUniqueId());
        enragedBosses.remove(entity.getUniqueId());
        long now = System.currentTimeMillis();
        String base = path(zone) + ".boss-spawns." + key;
        dataManager.getData().set(base + ".active", null);
        dataManager.getData().set(base + ".last-death", now);
        long respawnMillis = isDungeonZone(zone) ? 10_000L : bossRespawnMillis();
        if (!isDungeonZone(zone) && plugin instanceof club.abelix.games.fakevival.Game game) {
            respawnMillis = Math.max(1_000L, Math.round(respawnMillis * game.getEventManager().bossRespawnMultiplier()));
        }
        dataManager.getData().set(base + ".next-respawn", isDungeonZone(zone) ? 0L : now + respawnMillis);
        dataManager.save();
        long respawnTicks = Math.max(1L, (respawnMillis + 49L) / 50L);
        if (isDungeonZone(zone)) {
            completeDungeonZoneRun(zone, killer, entity);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (exists(zone) && hasNearbyPlayer(zone)) spawnZone(zone);
            }, respawnTicks);
        }
        if (killer != null) rewardKill(entity, killer, zone, true);
        runBossDropCommands(zone, key, killer);
    }

    public void handleBossDamagedByPlayer(LivingEntity boss, Player attacker) {
        if (!isZoneBoss(boss) || attacker == null || attacker.isDead()) return;
        bossLastDamagedAt.put(boss.getUniqueId(), System.currentTimeMillis());
        UUID bossId = boss.getUniqueId();
        UUID playerId = attacker.getUniqueId();
        if (bossDeadTargets.getOrDefault(bossId, Set.of()).contains(playerId)) return;
        UUID current = bossTargets.get(bossId);
        Player currentPlayer = current == null ? null : Bukkit.getPlayer(current);
        if (current != null && currentPlayer != null && currentPlayer.isOnline() && !currentPlayer.isDead()) {
            if (boss instanceof Mob mob) mob.setTarget(currentPlayer);
            return;
        }
        bossTargets.put(bossId, playerId);
        if (boss instanceof Mob mob) mob.setTarget(attacker);
    }

    public boolean canBossTarget(LivingEntity boss, LivingEntity target) {
        if (!isZoneBoss(boss) || !(target instanceof Player player) || player.isDead()) return true;
        UUID bossId = boss.getUniqueId();
        UUID playerId = player.getUniqueId();
        if (bossDeadTargets.getOrDefault(bossId, Set.of()).contains(playerId)) return true;
        UUID current = bossTargets.get(bossId);
        return current == null || !current.equals(playerId);
    }

    public void handleBossTargetPlayerDeath(Player player) {
        UUID playerId = player.getUniqueId();
        for (Map.Entry<UUID, UUID> entry : new ArrayList<>(bossTargets.entrySet())) {
            if (!entry.getValue().equals(playerId)) continue;
            Entity boss = Bukkit.getEntity(entry.getKey());
            if (boss instanceof Mob mob) mob.setTarget(null);
            bossTargets.remove(entry.getKey());
            bossDeadTargets.computeIfAbsent(entry.getKey(), ignored -> new HashSet<>()).add(playerId);
        }
    }

    public void handleZoneMobDeath(LivingEntity entity, Player killer) {
        String zone = entity.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING);
        if (zone == null || isZoneBoss(entity) || !claimZoneMobDeath(entity)) return;
        String spawnId = entity.getPersistentDataContainer().get(spawnKey, PersistentDataType.STRING);
        if (spawnId != null) {
            if (isDungeonZone(zone)) {
                int spawnIndex = entity.getPersistentDataContainer().getOrDefault(spawnIndexKey, PersistentDataType.INTEGER, 0);
                clearedDungeonSpawns.computeIfAbsent(zone.toLowerCase(Locale.ROOT), ignored -> new HashSet<>())
                        .add(clearedSpawnKey(spawnId, spawnIndex));
            } else {
            long min = Math.max(1L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("zones.dungeon-mob-respawn-min-seconds", 20L));
            long max = Math.max(min, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("zones.dungeon-mob-respawn-max-seconds", 45L));
            if (plugin instanceof club.abelix.games.fakevival.Game game) {
                double multiplier = game.getEventManager().dungeonRespawnMultiplier();
                min = Math.max(1L, Math.round(min * multiplier));
                max = Math.max(min, Math.round(max * multiplier));
            }
            long delay = ThreadLocalRandom.current().nextLong(min, max + 1L) * 1000L;
            dataManager.getData().set(path(zone) + ".spawns." + spawnId + ".next-respawn", System.currentTimeMillis() + delay);
            }
        }
        if (killer != null) rewardKill(entity, killer, zone, false);
    }

    private void completeDungeonZoneRun(String zone, Player killer, LivingEntity boss) {
        setDungeonRunState(zone, DungeonRunState.COMPLETED);
        clearDungeonZoneMobs(zone, boss == null ? null : boss.getUniqueId());
        if (killer != null && killer.isOnline()) {
            killer.sendMessage(CC.color("&6&lDUNGEON COMPLETE! &7You defeated &c" + ChatColor.stripColor(boss == null || boss.getCustomName() == null ? "Dungeon Boss" : boss.getCustomName()) + "&7."));
            killer.sendMessage(CC.color("&aYou have been returned to spawn."));
            Location spawn = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getSpawnLocation();
            if (plugin instanceof club.abelix.games.fakevival.Game game && game.getSpawnManager().getSpawn() != null) spawn = game.getSpawnManager().getSpawn();
            if (spawn != null) killer.teleport(spawn);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> resetDungeonZoneRun(zone), 20L * 10L);
    }

    private void resetDungeonZoneRun(String zone) {
        clearedDungeonSpawns.remove(zone.toLowerCase(Locale.ROOT));
        setDungeonRunState(zone, DungeonRunState.WAITING);
        if (exists(zone) && hasNearbyPlayer(zone)) spawnZone(zone);
    }

    private void clearDungeonZoneMobs(String zone, UUID except) {
        Set<UUID> current = spawned.get(zone.toLowerCase(Locale.ROOT));
        if (current == null) return;
        for (UUID uuid : new HashSet<>(current)) {
            if (except != null && except.equals(uuid)) continue;
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) entity.remove();
            current.remove(uuid);
            dungeonMobData.remove(uuid);
            bossAbilityCooldowns.remove(uuid);
        }
    }

    private boolean claimZoneMobDeath(LivingEntity entity) {
        if (entity.getPersistentDataContainer().has(mobRewardsProcessedKey, PersistentDataType.BYTE)) return false;
        entity.getPersistentDataContainer().set(mobRewardsProcessedKey, PersistentDataType.BYTE, (byte) 1);
        entity.getPersistentDataContainer().set(mobDeadKey, PersistentDataType.BYTE, (byte) 1);
        UUID uuid = entity.getUniqueId();
        String zone = entity.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING);
        if (zone != null) {
            Set<UUID> active = spawned.get(zone.toLowerCase(Locale.ROOT));
            if (active != null) active.remove(uuid);
        }
        dungeonMobData.remove(uuid);
        processingDamage.remove(uuid);
        return true;
    }

    public boolean isPvpBlocked(Location location) {
        for (String zone : zonesAt(location)) {
            if (!dataManager.getData().getBoolean(path(zone) + ".allow-pvp", false)) return true;
        }
        return false;
    }

    public boolean allowsMobSpawn(Location location) {
        for (String zone : zonesAt(location)) {
            if (dataManager.getData().getBoolean(path(zone) + ".allow-mobs", false)) return true;
        }
        return false;
    }

    public boolean allowsMobSpawn(Location location, EntityType type) {
        for (String zone : zonesAt(location)) {
            if (!dataManager.getData().getBoolean(path(zone) + ".allow-mobs", false)) continue;
            if (zoneAllowsMob(zone, type)) return true;
        }
        return false;
    }

    public boolean blocksDungeonCreatureSpawn(Location location, EntityType type, boolean customSpawn) {
        for (String zone : zonesAt(location)) {
            if (!dataManager.getData().getString(path(zone) + ".type", "safe").equalsIgnoreCase("dungeon")) continue;
            if (customSpawn) return false;
            return true;
        }
        return false;
    }

    public boolean blocksBuild(Location location) {
        for (String zone : zonesAt(location)) {
            if (!dataManager.getData().getBoolean(path(zone) + ".allow-build", false)) return true;
        }
        return false;
    }

    public boolean isProtectedSafeZone(Location location) {
        for (String zone : zonesAt(location)) {
            if (dataManager.getData().getString(path(zone) + ".type", "safe").equalsIgnoreCase("safe")) return true;
        }
        return false;
    }

    public boolean allowsBuildOverride(Location location) {
        List<String> zones = zonesAt(location);
        if (zones.isEmpty()) return false;
        for (String zone : zones) {
            if (!dataManager.getData().getBoolean(path(zone) + ".allow-build", false)) return false;
        }
        return true;
    }

    private void registerDefaultDungeonBossTemplates() {
        ConfigurationSection config = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        for (String line : DEFAULT_DUNGEON_BOSSES.strip().split("\\R")) {
            String[] parts = line.trim().split("\\|", -1);
            if (parts.length < 20) continue;
            String base = "zones.boss-templates." + parts[0];
            if (config.isConfigurationSection(base)) continue;
            int level = parseInt(parts[3], 1);
            config.set(base + ".name", parts[1]);
            config.set(base + ".type", parts[2]);
            config.set(base + ".level", level);
            config.set(base + ".health", parseDouble(parts[4], defaultBossHealth()));
            config.set(base + ".lootbox-chance", Math.min(55.0D, 20.0D + level * 0.75D));
            config.set(base + ".drop-commands", config.getStringList("zones.default-boss-drop-commands"));
            config.set(base + ".abilities.message-chance", parseDouble(parts[5], 45.0D));
            config.set(base + ".abilities.pull-chance", parseDouble(parts[6], 0.0D));
            config.set(base + ".abilities.summon-chance", parseDouble(parts[7], 0.0D));
            config.set(base + ".abilities.lightning-chance", parseDouble(parts[8], 0.0D));
            config.set(base + ".abilities.teleport-chance", parseDouble(parts[9], 0.0D));
            config.set(base + ".abilities.messages", splitList(parts[10]));
            config.set(base + ".abilities.low-health.enabled", true);
            config.set(base + ".abilities.low-health.threshold-percent", parseDouble(parts[11], 35.0D));
            config.set(base + ".abilities.low-health.messages", splitList(parts[12]));
            config.set(base + ".abilities.low-health.pull", Boolean.parseBoolean(parts[13]));
            config.set(base + ".abilities.low-health.teleport-near-target", Boolean.parseBoolean(parts[14]));
            config.set(base + ".abilities.low-health.teleport-to-spawn", Boolean.parseBoolean(parts[15]));
            config.set(base + ".abilities.low-health.heal-percent", parseDouble(parts[16], 0.0D));
            config.set(base + ".abilities.low-health.summon-amount", parseInt(parts[17], 0));
            config.set(base + ".abilities.low-health.summon-type", parts[18]);
            config.set(base + ".rare-drops", rareDropMaps(splitList(parts[19]), level));
        }
    }

    private List<Map<String, Object>> rareDropMaps(List<String> drops, int level) {
        List<Map<String, Object>> values = new ArrayList<>();
        for (int index = 0; index < drops.size(); index++) {
            String display = drops.get(index);
            Map<String, Object> drop = new LinkedHashMap<>();
            drop.put("display", "&d" + display);
            drop.put("custom-item", normalizeItemId(display));
            drop.put("rarity", index == 0 ? "rare" : index == 1 ? "epic" : "legendary");
            drop.put("chance", index == 0 ? 14.0D : index == 1 ? 6.0D : 2.0D);
            drop.put("min-level", Math.max(1, level - 2));
            drop.put("min-amount", 1);
            drop.put("max-amount", 1);
            values.add(drop);
        }
        return values;
    }

    private void repairPharaohTemplateStats() {
        ConfigurationSection zones = dataManager.getData().getConfigurationSection("map-zones");
        if (zones == null) return;
        boolean changed = false;
        for (String zone : zones.getKeys(false)) {
            ConfigurationSection bosses = dataManager.getData().getConfigurationSection(path(zone) + ".boss-spawns");
            if (bosses == null) continue;
            for (String key : bosses.getKeys(false)) {
                String base = path(zone) + ".boss-spawns." + key;
                String template = dataManager.getData().getString(base + ".template", "");
                String bossId = dataManager.getData().getString(base + ".boss_id", "");
                if (!template.equalsIgnoreCase("tomb_pharaoh") && !bossId.equalsIgnoreCase("pharaoh")) continue;
                dataManager.getData().set(base + ".boss_id", "pharaoh");
                dataManager.getData().set(base + ".boss_type", "ZONE_BOSS");
                dataManager.getData().set(base + ".level", Math.max(15, dataManager.getData().getInt(base + ".level", 15)));
                dataManager.getData().set(base + ".health", Math.max(12000.0D,
                        dataManager.getData().getDouble(base + ".health", 0.0D)));
                dataManager.getData().set(base + ".defense", Math.max(120.0D,
                        dataManager.getData().getDouble(base + ".defense", 0.0D)));
                dataManager.getData().set(base + ".damage", Math.max(160.0D,
                        dataManager.getData().getDouble(base + ".damage", 0.0D)));
                changed = true;
            }
        }
        if (changed) dataManager.save();
    }

    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("~")).map(String::trim).filter(entry -> !entry.isBlank()).toList();
    }

    private String normalizeItemId(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace("'", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public boolean canBreak(Player player, Block block) {
        List<String> zones = zonesAt(block.getLocation());
        if (zones.isEmpty()) return true;
        if (hasResourceBreakZone(zones, block)) return true;
        for (String zone : zones) {
            if (canBreakInZone(zone, block)) return false;
        }
        return true;
    }

    public boolean canPlace(Player player, Block block) {
        List<String> zones = zonesAt(block.getLocation());
        if (zones.isEmpty()) return true;
        for (String zone : zones) {
            if (!dataManager.getData().getBoolean(path(zone) + ".allow-place", false)) return false;
        }
        return true;
    }

    public boolean shouldOverrideSpawnPlace(Block block) {
        List<String> zones = zonesAt(block.getLocation());
        if (zones.isEmpty()) return false;
        for (String zone : zones) {
            if (!dataManager.getData().getBoolean(path(zone) + ".allow-place", false)) return false;
        }
        return true;
    }

    public boolean shouldOverrideSpawnBreak(Block block) {
        List<String> zones = zonesAt(block.getLocation());
        if (zones.isEmpty()) return false;
        if (hasResourceBreakZone(zones, block)) return true;
        for (String zone : zones) {
            if (canBreakInZone(zone, block)) return false;
        }
        return true;
    }

    public boolean bypassesAdventureMode(Location location) {
        for (String zone : zonesAt(location)) {
            String type = dataManager.getData().getString(path(zone) + ".type", "safe").toLowerCase();
            if (type.equals("mining") || type.equals("farming") || dataManager.getData().getBoolean(path(zone) + ".allow-place", false)) return true;
        }
        return false;
    }

    public void scheduleRegeneration(Block block) {
        List<String> zones = zonesAt(block.getLocation());
        if (zones.isEmpty()) return;
        String zone = firstResourceBreakZone(zones, block);
        if (zone == null) zone = zones.getFirst();
        String type = dataManager.getData().getString(path(zone) + ".type", "safe").toLowerCase();
        if (!type.equals("mining") && !type.equals("farming")) return;

        String key = blockKey(block);
        if (!regeneratingBlocks.add(key)) return;
        BlockData original = block.getBlockData().clone();
        Material placeholder = placeholder(type);
        long delay = Math.max(20L, dataManager.getData().getLong(path(zone) + ".regen-delay-ticks",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("zones.regeneration." + type + "-delay-ticks", type.equals("mining") ? 200L : 100L)));

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType() == Material.AIR && placeholder != Material.AIR) block.setType(placeholder, false);
        });
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (block.getType() == Material.AIR || block.getType() == placeholder) block.setBlockData(original, false);
            regeneratingBlocks.remove(key);
        }, delay);
    }

    private boolean hasResourceBreakZone(List<String> zones, Block block) {
        return firstResourceBreakZone(zones, block) != null;
    }

    private String firstResourceBreakZone(List<String> zones, Block block) {
        for (String zone : zones) {
            String type = dataManager.getData().getString(path(zone) + ".type", "safe").toLowerCase();
            if (type.equals("mining") && isRegeneratingMiningBlock(zone, block.getType())) return zone;
            if (type.equals("farming") && isFarmingRegeneratingBlock(block)) return zone;
        }
        return null;
    }

    public boolean allowsSpawnProtectionPvpOutsideZones() {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("zones.spawnprotection.allow-pvp-outside-zones", true);
    }

    public String listZones() {
        ConfigurationSection section = dataManager.getData().getConfigurationSection("map-zones");
        if (section == null || section.getKeys(false).isEmpty()) return "None";
        return String.join(", ", section.getKeys(false));
    }

    public String info(String name) {
        if (!exists(name)) return "Missing";
        String base = path(name);
        Location pos1 = loadLocation(base + ".pos1");
        Location pos2 = loadLocation(base + ".pos2");
        int spawns = 0;
        ConfigurationSection section = dataManager.getData().getConfigurationSection(base + ".spawns");
        if (section != null) spawns = section.getKeys(false).size();
        int bosses = 0;
        ConfigurationSection bossSection = dataManager.getData().getConfigurationSection(base + ".boss-spawns");
        if (bossSection != null) bosses = bossSection.getKeys(false).size();
        return "Type: " + dataManager.getData().getString(base + ".type", "safe")
                + " | Shape: " + dataManager.getData().getString(base + ".zone_shape", "CUBOID")
                + " | PvP: " + dataManager.getData().getBoolean(base + ".allow-pvp", false)
                + " | Mobs: " + dataManager.getData().getBoolean(base + ".allow-mobs", false)
                + " | Break: " + dataManager.getData().getBoolean(base + ".allow-break", false)
                + " | Place: " + dataManager.getData().getBoolean(base + ".allow-place", false)
                + " | Pos1: " + format(pos1)
                + " | Pos2: " + format(pos2)
                + " | Spawns: " + spawns
                + " | Bosses: " + bosses;
    }

    public List<ZoneTravelInfo> discoveredZones(Player player) {
        if (player == null) return List.of();
        ConfigurationSection zones = dataManager.getData().getConfigurationSection("map-zones");
        if (zones == null) return List.of();
        return zones.getKeys(false).stream()
                .filter(zone -> isDiscovered(player, zone))
                .sorted(Comparator.comparing(this::zoneDisplayName, String.CASE_INSENSITIVE_ORDER))
                .map(zone -> zoneTravelInfo(player, zone))
                .filter(Objects::nonNull)
                .toList();
    }

    public ZoneTravelInfo zoneTravelInfo(Player player, String zone) {
        if (player == null || !exists(zone) || !isDiscovered(player, zone)) return null;
        String id = zone.toLowerCase(Locale.ROOT);
        String base = path(id);
        Location warp = getZoneWarp(id);
        boolean unlocked = isTravelUnlocked(player, id);
        String travelStatus = warp == null ? "&7Unavailable" : unlocked ? "&aUnlocked" : "&cLocked";
        return new ZoneTravelInfo(id, zoneDisplayName(id), zoneIcon(id),
                dataManager.getData().getString(base + ".type", "safe"),
                Math.max(1, dataManager.getData().getInt(base + ".recommended-level",
                        dataManager.getData().getInt(base + ".level", 1))),
                discoveredAt(player, id), warp != null, unlocked, travelStatus,
                travelRequirementLines(player, id), zoneMobSummary(id), zoneBossSummary(id));
    }

    public boolean isDiscovered(Player player, String zone) {
        if (player == null || zone == null) return false;
        String base = discoveryPath(player, zone);
        Object stored = dataManager.getData().get(base);
        if (stored instanceof Boolean value) return value;
        return dataManager.getData().isConfigurationSection(base)
                && dataManager.getData().getBoolean(base + ".discovered", true);
    }

    public boolean setDiscovered(Player player, String zone, boolean discovered) {
        if (player == null || !exists(zone)) return false;
        String base = discoveryPath(player, zone);
        if (!discovered) dataManager.getData().set(base, null);
        else {
            if (dataManager.getData().get(base) instanceof Boolean) dataManager.getData().set(base, null);
            dataManager.getData().set(base + ".discovered", true);
            if (dataManager.getData().getLong(base + ".discovered-at", 0L) <= 0L) {
                dataManager.getData().set(base + ".discovered-at", System.currentTimeMillis());
            }
        }
        dataManager.save();
        return true;
    }

    public boolean unlockTravel(Player player, String zone) {
        if (!setDiscovered(player, zone, true)) return false;
        dataManager.getData().set(discoveryPath(player, zone) + ".travel-unlocked", true);
        dataManager.getData().set(discoveryPath(player, zone) + ".travel-unlocked-at", System.currentTimeMillis());
        dataManager.save();
        return true;
    }

    public TravelResult travelToZone(Player player, String zone) {
        if (player == null || !exists(zone)) return new TravelResult(false, "&cThat zone does not exist.");
        if (!isDiscovered(player, zone)) return new TravelResult(false, "&cYou have not discovered this zone yet.");
        Location warp = getZoneWarp(zone);
        if (warp == null) return new TravelResult(false, "&cThis zone does not have a travel location set yet.");
        if (!isTravelUnlocked(player, zone) && !unlockTravelIfEligible(player, zone)) {
            return new TravelResult(false, "&cYou have not unlocked travel to this zone yet.");
        }
        if (plugin instanceof club.abelix.games.fakevival.Game game && game.getPveManager().isCombatTagged(player)) {
            return new TravelResult(false, "&cYou cannot travel while combat tagged.");
        }
        if (isPlayerInBossFight(player)) return new TravelResult(false, "&cYou cannot travel during a boss fight.");
        Location safe = findSafeWarp(warp);
        if (safe == null) return new TravelResult(false, "&cThat zone warp is currently unsafe.");
        player.teleport(safe);
        return new TravelResult(true, "&aTraveling to &e" + zoneDisplayName(zone) + "&a...");
    }

    public record ZoneTravelInfo(String id, String name, Material icon, String type, int level,
                                 long discoveredAt, boolean hasWarp, boolean travelUnlocked,
                                 String travelStatus, List<String> requirementLines,
                                 String mobs, String bosses) {
    }

    public record TravelResult(boolean success, String message) {
    }

    private void tickMobZones() {
        ConfigurationSection section = dataManager.getData().getConfigurationSection("map-zones");
        if (section == null) return;
        for (String zone : section.getKeys(false)) {
            if (!dataManager.getData().getBoolean(path(zone) + ".allow-mobs", false)) {
                despawnZone(zone);
                continue;
            }
            if (hasNearbyPlayer(zone)) spawnZone(zone);
            else despawnZone(zone);
            containZoneMobs(zone);
            tickDungeonMobAi(zone);
            tickBossAbilities(zone);
        }
    }

    private void tickDungeonMobAi(String zone) {
        if (!isDungeonZone(zone)) return;
        Set<UUID> active = spawned.get(zone.toLowerCase(Locale.ROOT));
        if (active == null || active.isEmpty()) return;
        for (UUID uuid : new HashSet<>(active)) {
            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof Mob mob) || mob.isDead() || !isZoneDungeonMob(mob) || isZoneBoss(mob)) continue;
            DungeonMobData data = dungeonMobData.computeIfAbsent(uuid, ignored -> readDungeonMobData(mob));
            if (data == null) continue;

            Location spawn = getTrackedSpawn(mob, zone);
            double leash = Math.max(12.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.dungeon-mob-leash-radius", 32.0D));
            if (spawn != null && spawn.getWorld() != null && spawn.getWorld().equals(mob.getWorld())
                    && mob.getLocation().distanceSquared(spawn) > leash * leash) {
                resetDungeonMob(mob, data, spawn);
                continue;
            }

            Player target = mob.getTarget() instanceof Player player ? player : null;
            if (!validDungeonTarget(target, zone, mob.getLocation(), 30.0D)) {
                if (System.currentTimeMillis() - data.lastDamagedAt <= 10_000L) {
                    Player recentAttacker = getZoneMobKiller(mob);
                    if (validDungeonTarget(recentAttacker, zone, mob.getLocation(), 30.0D)) target = recentAttacker;
                }
            }
            if (!validDungeonTarget(target, zone, mob.getLocation(), 30.0D)) {
                target = nearestDungeonTarget(mob, zone, 20.0D);
            }
            mob.setTarget(target);
            if (target == null) continue;

            double distanceSquared = mob.getLocation().distanceSquared(target.getLocation());
            if (data.role == DungeonMobRole.ARCHER && distanceSquared < 25.0D) keepDistance(mob, target);
            tickDungeonRoleAbility(zone, mob, target, data, distanceSquared);
        }
    }

    private Player nearestDungeonTarget(LivingEntity mob, String zone, double radius) {
        Player nearest = null;
        double best = radius * radius;
        for (Player player : mob.getWorld().getPlayers()) {
            if (!validDungeonTarget(player, zone, mob.getLocation(), radius)) continue;
            double distance = player.getLocation().distanceSquared(mob.getLocation());
            if (distance >= best || !mob.hasLineOfSight(player)) continue;
            nearest = player;
            best = distance;
        }
        return nearest;
    }

    private boolean validDungeonTarget(Player player, String zone, Location origin, double radius) {
        if (zone == null || player == null || !player.isOnline() || player.isDead() || player.getGameMode() == GameMode.SPECTATOR) return false;
        if (player.isInvisible() || player.hasMetadata("vanished") || player.hasMetadata("vanish")) return false;
        if (origin == null || origin.getWorld() == null || !origin.getWorld().equals(player.getWorld())) return false;
        return origin.distanceSquared(player.getLocation()) <= radius * radius && isInside(zone, player.getLocation());
    }

    private void resetDungeonMob(Mob mob, DungeonMobData data, Location spawn) {
        mob.setTarget(null);
        mob.teleport(spawn);
        data.currentHealth = data.maxHealth;
        data.guardUntil = 0L;
        data.cooldowns.clear();
        mob.getPersistentDataContainer().set(mobHealthKey, PersistentDataType.DOUBLE, data.currentHealth);
        mob.setHealth(Math.min(mob.getMaxHealth(), data.currentHealth));
        updateMobName(mob);
    }

    private void keepDistance(Mob mob, Player target) {
        Vector away = mob.getLocation().toVector().subtract(target.getLocation().toVector());
        if (away.lengthSquared() < 0.001D) return;
        mob.setVelocity(away.normalize().multiply(0.28D).setY(0.12D));
    }

    private void tickDungeonRoleAbility(String zone, Mob mob, Player target, DungeonMobData data, double distanceSquared) {
        switch (data.role) {
            case GUARD -> {
                if (cooldownReady(data, "guard_stance", 15_000L, 15_000L)) guardStance(mob, data);
                if (distanceSquared <= 16.0D && cooldownReady(data, "ground_slam", 9_000L, 12_000L)) groundSlam(mob, data);
            }
            case BRUTE -> {
                if (distanceSquared <= 20.25D && cooldownReady(data, "ground_slam", 8_000L, 12_000L)) groundSlam(mob, data);
            }
            case ASSASSIN -> {
                if (distanceSquared <= 144.0D && cooldownReady(data, "dash", 6_000L, 10_000L)) dashStrike(mob, target, data);
            }
            case ARCHER -> {
                if (distanceSquared <= 400.0D && cooldownReady(data, "volley", 8_000L, 12_000L)) arrowVolley(mob, target, data);
            }
            case MAGE -> {
                if (distanceSquared <= 324.0D && cooldownReady(data, "fire_bolt", 5_000L, 8_000L)) fireBolt(mob, target, data);
                if (distanceSquared <= 225.0D && cooldownReady(data, "weakness", 12_000L, 18_000L)) weaknessCurse(mob, target);
            }
            case HEALER -> {
                if (cooldownReady(data, "healing_pulse", 10_000L, 15_000L)) healingPulse(zone, mob, data);
            }
            case ELITE -> {
                if (distanceSquared <= 20.25D && cooldownReady(data, "elite_slam", 7_000L, 10_000L)) groundSlam(mob, data);
                else if (distanceSquared <= 144.0D && cooldownReady(data, "elite_dash", 7_000L, 11_000L)) dashStrike(mob, target, data);
            }
            default -> {
                if (data.elite && distanceSquared <= 16.0D && cooldownReady(data, "elite_extra", 9_000L, 13_000L)) groundSlam(mob, data);
            }
        }
    }

    private boolean cooldownReady(DungeonMobData data, String ability, long minMillis, long maxMillis) {
        long now = System.currentTimeMillis();
        Long readyAt = data.cooldowns.get(ability);
        double eliteScale = data.elite ? 0.80D : 1.0D;
        long delay = (long) (ThreadLocalRandom.current().nextLong(minMillis, maxMillis + 1L) * eliteScale);
        if (readyAt == null) {
            data.cooldowns.put(ability, now + delay);
            return false;
        }
        if (now < readyAt) return false;
        data.cooldowns.put(ability, now + delay);
        return true;
    }

    private void groundSlam(Mob mob, DungeonMobData data) {
        Location warning = mob.getLocation().clone();
        mob.getWorld().spawnParticle(Particle.CRIT, warning.clone().add(0.0D, 0.15D, 0.0D), 8, 1.2D, 0.1D, 1.2D, 0.01D);
        mob.getWorld().playSound(warning, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8F, 0.65F);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!mob.isValid() || mob.isDead()) return;
            Location center = mob.getLocation();
            mob.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(0.0D, 0.15D, 0.0D), 10, 1.1D, 0.1D, 1.1D, 0.02D);
            mob.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7F, 0.8F);
            for (Player player : mob.getWorld().getNearbyPlayers(center, 3.0D)) {
                String zone = mob.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING);
                if (!validDungeonTarget(player, zone, center, 3.0D)) continue;
                dealDungeonAbilityDamage(player, mob, abilityDamage(mob, data, 0.65D));
                Vector push = player.getLocation().toVector().subtract(center.toVector());
                if (push.lengthSquared() > 0.001D) player.setVelocity(push.normalize().multiply(0.55D).setY(0.28D));
            }
        }, 15L);
    }

    private void dashStrike(Mob mob, Player target, DungeonMobData data) {
        Vector direction = target.getLocation().toVector().subtract(mob.getLocation().toVector());
        if (direction.lengthSquared() < 0.01D) return;
        mob.setVelocity(direction.normalize().multiply(1.05D).setY(0.18D));
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.45F, 1.6F);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!mob.isValid() || mob.isDead() || !validDungeonTarget(target,
                    mob.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING), mob.getLocation(), 3.0D)) return;
            dealDungeonAbilityDamage(target, mob, abilityDamage(mob, data, 0.80D));
        }, 5L);
    }

    private void arrowVolley(Mob mob, Player target, DungeonMobData data) {
        for (int i = 0; i < 3; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> launchDungeonArrow(mob, target, data), i * 3L);
        }
    }

    private void launchDungeonArrow(Mob mob, Player target, DungeonMobData data) {
        if (!mob.isValid() || mob.isDead() || target == null || target.isDead() || !mob.hasLineOfSight(target)) return;
        Arrow arrow = mob.launchProjectile(Arrow.class);
        Vector direction = target.getEyeLocation().toVector().subtract(mob.getEyeLocation().toVector()).normalize();
        arrow.setVelocity(direction.multiply(1.55D));
        arrow.setDamage(Math.max(2.0D, abilityDamage(mob, data, 0.45D)));
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
    }

    private void fireBolt(Mob mob, Player target, DungeonMobData data) {
        if (!mob.hasLineOfSight(target)) return;
        mob.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0.0D, 1.0D, 0.0D), 6, 0.2D, 0.35D, 0.2D, 0.01D);
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.55F, 1.25F);
        dealDungeonAbilityDamage(target, mob, abilityDamage(mob, data, 0.55D));
        target.setFireTicks(Math.max(target.getFireTicks(), 40));
    }

    private void weaknessCurse(Mob mob, Player target) {
        int duration = ThreadLocalRandom.current().nextInt(60, 101);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 0, true, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 0, true, true, true));
        mob.getWorld().playSound(target.getLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, 0.5F, 0.8F);
    }

    private void healingPulse(String zone, Mob healer, DungeonMobData healerData) {
        Set<UUID> active = spawned.getOrDefault(zone.toLowerCase(Locale.ROOT), Set.of());
        boolean healed = false;
        for (UUID uuid : active) {
            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof LivingEntity ally) || ally.isDead() || ally.equals(healer) || isZoneBoss(ally)) continue;
            if (!ally.getWorld().equals(healer.getWorld()) || ally.getLocation().distanceSquared(healer.getLocation()) > 36.0D) continue;
            DungeonMobData data = dungeonMobData.get(uuid);
            if (data == null || data.currentHealth >= data.maxHealth) continue;
            double percent = ThreadLocalRandom.current().nextDouble(0.05D, 0.0801D);
            data.currentHealth = Math.min(data.maxHealth, data.currentHealth + data.maxHealth * percent);
            ally.getPersistentDataContainer().set(mobHealthKey, PersistentDataType.DOUBLE, data.currentHealth);
            ally.setHealth(Math.min(ally.getMaxHealth(), data.currentHealth));
            updateMobName(ally);
            healed = true;
        }
        if (healed) {
            healer.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, healer.getLocation().add(0.0D, 1.0D, 0.0D), 8, 0.8D, 0.4D, 0.8D, 0.01D);
            healer.getWorld().playSound(healer.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.55F, 1.4F);
        }
    }

    private void guardStance(Mob mob, DungeonMobData data) {
        data.guardUntil = System.currentTimeMillis() + 4_000L;
        mob.getWorld().spawnParticle(Particle.ENCHANTED_HIT, mob.getLocation().add(0.0D, 1.0D, 0.0D), 6, 0.45D, 0.5D, 0.45D, 0.01D);
        mob.getWorld().playSound(mob.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.65F, 0.8F);
    }

    private double abilityDamage(LivingEntity mob, DungeonMobData data, double multiplier) {
        AttributeInstance attack = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        double base = attack == null ? 10.0D : attack.getValue();
        return Math.max(1.0D, base * multiplier * (data.elite ? 1.10D : 1.0D));
    }

    private void dealDungeonAbilityDamage(Player target, LivingEntity source, double damage) {
        MMODamageSourceTracker.runAs(MMODamageSourceType.ABILITY, () -> target.damage(Math.max(1.0D, damage), source));
    }

    private void spawnZone(String zone) {
        boolean dungeon = isDungeonZone(zone);
        if (dungeon && !dungeonSpawnTickRunning.compareAndSet(false, true)) return;
        try {
            spawnZoneInternal(zone);
        } finally {
            if (dungeon) dungeonSpawnTickRunning.set(false);
        }
    }


    private LivingEntity spawnOptimizedZoneMob(String mobId, Location location, EntityType type, String displayName,
                                             double maxMmoHealth, double attackDamage, int level) {
        if (plugin instanceof club.abelix.games.fakevival.Game game && game.getSmpMobEngine() != null) {
            return game.getSmpMobEngine().spawnMob(mobId, location, type, displayName, maxMmoHealth, attackDamage, level, java.util.List.of(), true);
        }
        Entity entity = location.getWorld().spawnEntity(location, type);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void spawnZoneInternal(String zone) {
        String zoneId = zone.toLowerCase(Locale.ROOT);
        Set<UUID> current = spawned.computeIfAbsent(zoneId, ignored -> new HashSet<>());
        reconcileLoadedZoneMobs(zone, current);
        int cleaned = cleanTrackedMobs(zone, current);
        spawnZoneBosses(zone, current);

        boolean dungeon = isDungeonZone(zone);
        if (dungeon && dungeonRunState(zone) == DungeonRunState.WAITING) setDungeonRunState(zone, DungeonRunState.ACTIVE);
        int configuredMax = dataManager.getData().getInt(path(zone) + ".max-mobs",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("zones.default-max-mobs", 12));
        int max = dungeon ? Math.max(configuredMax, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("zones.dungeon-max-mobs", 120)) : Math.max(1, configuredMax);
        int softCap = dungeon ? Math.min(max, Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("zones.dungeon-soft-cap", 90))) : max;

        Map<String, Set<Integer>> occupiedSlots = new HashMap<>();
        int normalAlive = 0;
        for (UUID uuid : current) {
            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || living.isDead() || isZoneBoss(living)) continue;
            normalAlive++;
            String spawnId = living.getPersistentDataContainer().get(spawnKey, PersistentDataType.STRING);
            if (spawnId != null) occupiedSlots.computeIfAbsent(spawnId, ignored -> new HashSet<>())
                    .add(living.getPersistentDataContainer().getOrDefault(spawnIndexKey, PersistentDataType.INTEGER, 0));
        }
        if (normalAlive >= max) {
            debugSpawn(zone, normalAlive, max, cleaned, 0, 0, "cap reached");
            return;
        }
        ConfigurationSection spawns = dataManager.getData().getConfigurationSection(path(zone) + ".spawns");
        if (spawns == null || spawns.getKeys(false).isEmpty()) return;

        List<String> keys = new ArrayList<>(spawns.getKeys(false));
        int start = Math.floorMod(spawnCursor.getOrDefault(zoneId, 0), keys.size());
        int batchLimit = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getInt("zones.dungeon-spawn-batch-size", 5));
        if (normalAlive >= softCap) batchLimit = Math.min(batchLimit, 5);
        int attempts = 0;
        int successful = 0;
        for (int offset = 0; offset < keys.size() && successful < batchLimit && normalAlive < max; offset++) {
            String key = keys.get((start + offset) % keys.size());
            String spawnPath = path(zone) + ".spawns." + key;
            if (System.currentTimeMillis() < dataManager.getData().getLong(spawnPath + ".next-respawn", 0L)) continue;
            attempts++;
            Location location = loadLocation(path(zone) + ".spawns." + key + ".location");
            if (location == null || location.getWorld() == null) continue;
            if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) continue;
            int configuredCount = Math.max(1, dataManager.getData().contains(spawnPath + ".count")
                    ? dataManager.getData().getInt(spawnPath + ".count", 1) : dataManager.getData().getInt(spawnPath + ".amount", 1));
            Set<Integer> slots = occupiedSlots.computeIfAbsent(key, ignored -> new HashSet<>());
            for (int index = 0; index < configuredCount && successful < batchLimit && normalAlive < max; index++) {
                if (slots.contains(index)) continue;
                if (dungeon && isClearedDungeonSpawn(zone, key, index)) continue;
                EntityType type = resolveDungeonEntityType(zone, spawnPath, dataManager.getData().getString(spawnPath + ".type", "ZOMBIE"));
                int level = Math.max(1, dataManager.getData().getInt(path(zone) + ".spawns." + key + ".level", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("zones.default-mob-level", 1)));
                String displayName = dataManager.getData().getString(path(zone) + ".spawns." + key + ".name", dataManager.getData().getString(path(zone) + ".mob-name", pretty(zone) + " Monster"));
                double engineHealth = healthScaler.normalHealth(level);
                LivingEntity living = spawnOptimizedZoneMob("zone_" + zone + "_" + key, location, type, displayName, engineHealth, 2.0D + level * 0.25D, level);
                if (living != null) {
                    current.add(living.getUniqueId());
                    living.setRemoveWhenFarAway(false);
                    tagMob(living, zone, key, false, level);
                    living.getPersistentDataContainer().set(spawnIndexKey, PersistentDataType.INTEGER, index);
                    living.setFireTicks(0);
                    applyLevelStats(living, zone, level);
                    living.setCustomName(baseMobName(zone, key, level));
                    living.setCustomNameVisible(true);
                    updateMobName(living);
                    syncSmpMobData(living, zone, key, displayName, false, level, engineHealth);
                    slots.add(index);
                    normalAlive++;
                    successful++;
                }
            }
        }
        spawnCursor.put(zoneId, (start + Math.max(1, attempts)) % keys.size());
        debugSpawn(zone, normalAlive, max, cleaned, attempts, successful, successful == 0 ? "no eligible spawn points" : "batch complete");
    }

    private void reconcileLoadedZoneMobs(String zone, Set<UUID> current) {
        Map<String, Set<Integer>> used = new HashMap<>();
        for (World world : Bukkit.getWorlds()) for (LivingEntity living : world.getLivingEntities()) {
            if (living.isDead() || !living.isValid()) continue;
            String storedZone = living.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING);
            String spawn = living.getPersistentDataContainer().get(spawnKey, PersistentDataType.STRING);
            if (!zone.equalsIgnoreCase(storedZone) || spawn == null) continue;
            Set<Integer> slots = used.computeIfAbsent(spawn, ignored -> new HashSet<>());
            Integer index = living.getPersistentDataContainer().get(spawnIndexKey, PersistentDataType.INTEGER);
            if (index == null || slots.contains(index)) {
                index = 0;
                while (slots.contains(index)) index++;
                living.getPersistentDataContainer().set(spawnIndexKey, PersistentDataType.INTEGER, index);
            }
            String configuredPath = path(zone) + (isZoneBoss(living) ? ".boss-spawns." : ".spawns.") + spawn;
            int configured = isZoneBoss(living) ? 1 : Math.max(1, dataManager.getData().contains(configuredPath + ".count")
                    ? dataManager.getData().getInt(configuredPath + ".count", 1)
                    : dataManager.getData().getInt(configuredPath + ".amount", 1));
            if (index >= configured) {
                living.remove();
                current.remove(living.getUniqueId());
                continue;
            }
            slots.add(index);
            current.add(living.getUniqueId());
            if (isDungeonZone(zone)) {
                living.getPersistentDataContainer().set(dungeonMobKey, PersistentDataType.BYTE, (byte) 1);
                living.setFireTicks(0);
            }
        }
    }

    private int cleanTrackedMobs(String zone, Set<UUID> current) {
        int cleaned = 0;
        for (UUID uuid : new HashSet<>(current)) {
            Entity entity = Bukkit.getEntity(uuid);
            boolean valid = entity instanceof LivingEntity living
                    && !living.isDead()
                    && living.isValid()
                    && living.getWorld().isChunkLoaded(living.getLocation().getBlockX() >> 4, living.getLocation().getBlockZ() >> 4)
                    && zone.equalsIgnoreCase(living.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING))
                    && living.getPersistentDataContainer().has(spawnKey, PersistentDataType.STRING)
                    && (!isDungeonZone(zone) || living.getPersistentDataContainer().has(dungeonMobKey, PersistentDataType.BYTE));
            if (valid) continue;
            current.remove(uuid);
            dungeonMobData.remove(uuid);
            processingDamage.remove(uuid);
            cleaned++;
        }
        return cleaned;
    }

    private void debugSpawn(String zone, int active, int cap, int cleaned, int attempts, int successful, String reason) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("zones.debug", false)) return;
        long now = System.currentTimeMillis();
        if (now - lastSpawnDebugLog.getOrDefault(zone.toLowerCase(Locale.ROOT), 0L) < 10_000L) return;
        lastSpawnDebugLog.put(zone.toLowerCase(Locale.ROOT), now);
        plugin.getLogger().info("[ZoneManager] zone=" + zone + " active=" + active + "/" + cap
                + " cleaned=" + cleaned + " attempts=" + attempts + " spawned=" + successful + " result=" + reason);
    }

    private void spawnZoneBosses(String zone, Set<UUID> current) {
        ConfigurationSection bosses = dataManager.getData().getConfigurationSection(path(zone) + ".boss-spawns");
        if (bosses == null) return;
        for (String key : bosses.getKeys(false)) {
            if (isBossAlive(current, zone, key)) continue;
            String bossPath = path(zone) + ".boss-spawns." + key;
            if (System.currentTimeMillis() < dataManager.getData().getLong(bossPath + ".next-respawn", 0L)) continue;
            if (isDungeonZone(zone) && dungeonRunState(zone) == DungeonRunState.COMPLETED) continue;
            Location location = loadLocation(path(zone) + ".boss-spawns." + key + ".location");
            if (location == null || location.getWorld() == null) {
                plugin.getLogger().warning("Zone boss spawn skipped for " + zone + "/" + key + ": world or location is not loaded.");
                continue;
            }
            if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) continue;
            String spawnPath = path(zone) + ".boss-spawns." + key;
            EntityType type = resolveDungeonEntityType(zone, spawnPath, dataManager.getData().getString(spawnPath + ".type", "ZOMBIE"));
            int level = Math.max(1, dataManager.getData().getInt(path(zone) + ".boss-spawns." + key + ".level", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("zones.default-boss-level", 10)));
            double health = bossConfiguredHealth(zone, key);
            String displayName = dataManager.getData().getString(path(zone) + ".boss-spawns." + key + ".name", pretty(key));
            LivingEntity living = spawnOptimizedZoneMob("zone_" + zone + "_boss_" + key, location, type, displayName, bossScaledHealth(zone, key, level, health), 25.0D + level * 3.5D, level);
            if (living != null) {
                current.add(living.getUniqueId());
                dataManager.getData().set(path(zone) + ".boss-spawns." + key + ".active", living.getUniqueId().toString());
                living.setRemoveWhenFarAway(false);
                tagMob(living, zone, key, true, level);
                living.setCustomNameVisible(true);
                applyBossStats(living, zone, key, level, health);
                updateMobName(living);
                syncSmpMobData(living, zone, key, displayName, true, level,
                        bossScaledHealth(zone, key, level, health));
                logBossSpawn(zone, key, living, level, health, living);
                if (isDungeonZone(zone)) setDungeonRunState(zone, DungeonRunState.BOSS_ALIVE);
            }
            dataManager.save();
        }
    }

    private DungeonRunState dungeonRunState(String zone) {
        return dungeonRunStates.getOrDefault(zone.toLowerCase(Locale.ROOT), DungeonRunState.WAITING);
    }

    private void setDungeonRunState(String zone, DungeonRunState state) {
        dungeonRunStates.put(zone.toLowerCase(Locale.ROOT), state);
        dataManager.getData().set(path(zone) + ".run.state", state.name());
        dataManager.save();
    }

    private boolean isClearedDungeonSpawn(String zone, String spawnId, int index) {
        return clearedDungeonSpawns.getOrDefault(zone.toLowerCase(Locale.ROOT), Set.of()).contains(clearedSpawnKey(spawnId, index));
    }

    private String clearedSpawnKey(String spawnId, int index) {
        return spawnId.toLowerCase(Locale.ROOT) + ":" + Math.max(0, index);
    }

    private void tickBossAbilities(String zone) {
        if (!dataManager.getData().getString(path(zone) + ".type", "safe").equalsIgnoreCase("dungeon")) return;
        Set<UUID> entities = spawned.get(zone.toLowerCase());
        if (entities == null || entities.isEmpty()) return;
        for (UUID uuid : new HashSet<>(entities)) {
            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof LivingEntity boss) || boss.isDead() || !isZoneBoss(boss)) continue;
            String spawnId = boss.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
            if (spawnId == null) continue;
            Player target = bossTarget(boss);
            if (target == null || !isInside(zone, target.getLocation())) continue;
            List<Player> nearby = nearbyBossPlayers(boss, zone, 18.0D);
            if (nearby.isEmpty()) continue;
            String abilitiesPath = path(zone) + ".boss-spawns." + spawnId + ".abilities.";
            tickLowHealthPhase(zone, spawnId, boss, target, nearby, abilitiesPath);
            tickEnrageBelowHealth(boss, nearby, abilitiesPath);
            if (shouldUseBossAbility(boss, abilitiesPath, "ground-slam", "ground-slam-chance", "ground-slam-cooldown", 18.0D, 12.0D)) groundSlam(boss, nearby);
            if (shouldUseBossAbility(boss, abilitiesPath, "cone-slash", "cone-slash-chance", "cone-slash-cooldown", 16.0D, 10.0D)) coneSlash(boss, nearby);
            if (shouldUseBossAbility(boss, abilitiesPath, "poison-cloud", "poison-cloud-chance", "poison-cloud-cooldown", 10.0D, 18.0D)) poisonCloud(boss, nearby);
            if (shouldUseBossAbility(boss, abilitiesPath, "fire-ring", "fire-ring-chance", "fire-ring-cooldown", 10.0D, 18.0D)) fireRing(boss, nearby);
            if (shouldUseBossAbility(boss, abilitiesPath, "frost-nova", "frost-nova-chance", "frost-nova-cooldown", 12.0D, 16.0D)) frostNova(boss, nearby);
            if (shouldUseBossAbility(boss, abilitiesPath, "lightning-strike", "lightning-strike-chance", "lightning-strike-cooldown", 12.0D, 16.0D)) warningLightningStrike(boss, target);
            if (shouldUseBossAbility(boss, abilitiesPath, "meteor-target", "meteor-chance", "meteor-cooldown", 9.0D, 22.0D)) meteorTarget(boss, target);
            if (shouldUseBossAbility(boss, abilitiesPath, "life-drain", "life-drain-chance", "life-drain-cooldown", 10.0D, 18.0D)) lifeDrain(boss, nearby);
            if (shouldUseBossAbility(boss, abilitiesPath, "shield-phase", "shield-phase-chance", "shield-phase-cooldown", 8.0D, 45.0D)) shieldPhase(boss, nearby);
            if (shouldUseBossAbility(boss, abilitiesPath, "minion-command", "minion-command-chance", "minion-command-cooldown", 14.0D, 14.0D)) commandMinions(zone, boss, target);
            if (shouldUseBossAbility(boss, abilitiesPath, "pull-smash", "pull-smash-chance", "pull-smash-cooldown", 10.0D, 20.0D)) pullAndSmash(boss, nearby);
            if (shouldUseBossAbility(boss, abilitiesPath, "dash-attack", "dash-attack-chance", "dash-attack-cooldown", 12.0D, 12.0D)) dashAttack(boss, target);
            if (shouldUseBossAbility(boss, abilitiesPath, "aoe-knockback", "aoe-knockback-chance", "aoe-knockback-cooldown", 10.0D, 14.0D)) aoeKnockback(boss, nearby);
            if (shouldUseBossAbility(boss, abilitiesPath, "weakness-curse", "weakness-curse-chance", "weakness-curse-cooldown", 10.0D, 18.0D)) weaknessCurse(boss, nearby);
            if (shouldUseBossAbility(boss, abilitiesPath, "healing-pulse", "healing-pulse-chance", "healing-pulse-cooldown", 8.0D, 22.0D)) healingPulse(zone, boss);
            if (shouldUseBossAbility(boss, abilitiesPath, "projectile-barrage", "projectile-barrage-chance", "projectile-barrage-cooldown", 12.0D, 14.0D)) projectileBarrage(boss, nearby);
            if (shouldUseBossAbility(boss, abilitiesPath, "trap-rune", "trap-rune-chance", "trap-rune-cooldown", 10.0D, 18.0D)) trapRune(boss, target);
            if (shouldUseBossAbility(boss, abilitiesPath, "bleed-strike", "bleed-strike-chance", "bleed-strike-cooldown", 10.0D, 16.0D)) bleedStrike(boss, target);
            if (ThreadLocalRandom.current().nextDouble(100.0D) < bossAbilityDouble(abilitiesPath, "pull-chance", "pull-chance", 20.0D)) {
                pullPlayers(boss, nearby);
            }
            if (ThreadLocalRandom.current().nextDouble(100.0D) < bossAbilityDouble(abilitiesPath, "summon-chance", "summon-chance", 18.0D)) {
                summonBossMinions(zone, boss, target);
            }
            if (ThreadLocalRandom.current().nextDouble(100.0D) < bossAbilityDouble(abilitiesPath, "lightning-chance", "lightning-chance", 0.0D)) {
                strikeNearby(boss, target);
            }
            if (ThreadLocalRandom.current().nextDouble(100.0D) < bossAbilityDouble(abilitiesPath, "teleport-chance", "teleport-chance", 0.0D)) {
                teleportNearTarget(boss, target);
            }
            if (ThreadLocalRandom.current().nextDouble(100.0D) < bossAbilityDouble(abilitiesPath, "message-chance", "message-chance", 45.0D)) {
                sendBossMessage(boss, nearby, spawnId);
            }
        }
    }

    private double bossAbilityDouble(String dataPath, String dataKey, String configKey, double fallback) {
        return dataManager.getData().getDouble(dataPath + dataKey, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.boss-abilities." + configKey, fallback));
    }

    private boolean shouldUseBossAbility(LivingEntity boss, String abilitiesPath, String ability, String chanceKey, String cooldownKey, double fallbackChance, double fallbackCooldownSeconds) {
        if (ThreadLocalRandom.current().nextDouble(100.0D) >= bossAbilityDouble(abilitiesPath, chanceKey, chanceKey, fallbackChance)) return false;
        long now = System.currentTimeMillis();
        Map<String, Long> cooldowns = bossAbilityCooldowns.computeIfAbsent(boss.getUniqueId(), ignored -> new HashMap<>());
        if (cooldowns.getOrDefault(ability, 0L) > now) return false;
        double seconds = Math.max(1.0D, bossAbilityDouble(abilitiesPath, cooldownKey, cooldownKey, fallbackCooldownSeconds));
        cooldowns.put(ability, now + (long) (seconds * 1000.0D));
        return true;
    }

    private List<Player> nearbyBossPlayers(LivingEntity boss, String zone, double radius) {
        List<Player> players = new ArrayList<>();
        double radiusSq = radius * radius;
        for (Player player : boss.getWorld().getPlayers()) {
            if (!player.isOnline() || player.isDead() || !isInside(zone, player.getLocation())) continue;
            if (player.getLocation().distanceSquared(boss.getLocation()) <= radiusSq) players.add(player);
        }
        return players;
    }

    private List<Player> visiblePlayers(LivingEntity boss, List<Player> nearby, double radius) {
        double radiusSq = radius * radius;
        return nearby.stream()
                .filter(player -> player.getLocation().distanceSquared(boss.getLocation()) <= radiusSq)
                .filter(boss::hasLineOfSight)
                .toList();
    }

    private double bossAbilityDamage(String key, double fallback) {
        return Math.max(1.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.boss-abilities." + key, fallback));
    }

    private void groundSlam(LivingEntity boss, List<Player> nearby) {
        for (Player player : visiblePlayers(boss, nearby, 5.5D)) {
            Vector push = player.getLocation().toVector().subtract(boss.getLocation().toVector()).normalize().multiply(0.85D).setY(0.42D);
            player.setVelocity(push);
            player.damage(bossAbilityDamage("ground-slam-damage", 10.0D), boss);
        }
        boss.getWorld().spawnParticle(Particle.EXPLOSION, boss.getLocation().add(0.0D, 0.2D, 0.0D), 4, 1.8D, 0.2D, 1.8D);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.75F, 0.8F);
    }

    private void coneSlash(LivingEntity boss, List<Player> nearby) {
        Vector facing = boss.getLocation().getDirection().setY(0.0D).normalize();
        for (Player player : visiblePlayers(boss, nearby, 5.0D)) {
            Vector toward = player.getLocation().toVector().subtract(boss.getLocation().toVector()).setY(0.0D).normalize();
            if (facing.dot(toward) < 0.45D) continue;
            player.damage(bossAbilityDamage("cone-slash-damage", 8.0D), boss);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0.0D, 1.0D, 0.0D), 2, 0.35D, 0.25D, 0.35D);
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
    }

    private void poisonCloud(LivingEntity boss, List<Player> nearby) {
        Location center = boss.getLocation().clone();
        boss.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, center.add(0.0D, 0.8D, 0.0D), 70, 3.0D, 0.8D, 3.0D, 0.02D);
        for (Player player : visiblePlayers(boss, nearby, 6.0D)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 0, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, true, true));
        }
    }

    private void fireRing(LivingEntity boss, List<Player> nearby) {
        Location center = boss.getLocation();
        for (int i = 0; i < 28; i++) {
            double angle = (Math.PI * 2.0D / 28.0D) * i;
            center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(Math.cos(angle) * 4.0D, 0.15D, Math.sin(angle) * 4.0D), 2, 0.05D, 0.05D, 0.05D, 0.01D);
        }
        for (Player player : visiblePlayers(boss, nearby, 5.0D)) {
            double distance = player.getLocation().distance(center);
            if (distance < 3.1D || distance > 5.0D) continue;
            player.setFireTicks(Math.max(player.getFireTicks(), 60));
            player.damage(bossAbilityDamage("fire-ring-damage", 7.0D), boss);
        }
        boss.getWorld().playSound(center, Sound.ITEM_FIRECHARGE_USE, 0.8F, 0.75F);
    }

    private void frostNova(LivingEntity boss, List<Player> nearby) {
        boss.getWorld().spawnParticle(Particle.SNOWFLAKE, boss.getLocation().add(0.0D, 0.8D, 0.0D), 80, 4.0D, 0.7D, 4.0D, 0.02D);
        for (Player player : visiblePlayers(boss, nearby, 7.0D)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2, true, true, true));
            player.damage(bossAbilityDamage("frost-nova-damage", 5.0D), boss);
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8F, 0.55F);
    }

    private void warningLightningStrike(LivingEntity boss, Player target) {
        Location mark = target.getLocation().clone();
        target.sendMessage(CC.color("&eLightning is about to strike!"));
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, mark.add(0.0D, 0.2D, 0.0D), 28, 0.8D, 0.1D, 0.8D, 0.01D);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!target.isOnline() || target.isDead() || target.getLocation().distanceSquared(mark) > 4.0D) return;
            target.getWorld().strikeLightningEffect(target.getLocation());
            target.damage(bossAbilityDamage("lightning-damage", 9.0D), boss);
        }, 25L);
    }

    private void meteorTarget(LivingEntity boss, Player target) {
        Location mark = target.getLocation().clone();
        target.sendMessage(CC.color("&cMeteor incoming!"));
        target.getWorld().spawnParticle(Particle.FLAME, mark.clone().add(0.0D, 0.2D, 0.0D), 35, 1.1D, 0.1D, 1.1D, 0.02D);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            mark.getWorld().spawnParticle(Particle.EXPLOSION, mark.clone().add(0.0D, 0.2D, 0.0D), 5, 1.2D, 0.2D, 1.2D);
            mark.getWorld().playSound(mark, Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 0.9F);
            for (Player player : mark.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(mark) <= 12.25D) player.damage(bossAbilityDamage("meteor-damage", 12.0D), boss);
            }
        }, 35L);
    }

    private void lifeDrain(LivingEntity boss, List<Player> nearby) {
        double healed = 0.0D;
        for (Player player : visiblePlayers(boss, nearby, 6.0D)) {
            double damage = bossAbilityDamage("life-drain-damage", 5.0D);
            player.damage(damage, boss);
            healed += damage * 0.6D;
            player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0.0D, 1.0D, 0.0D), 20, 0.4D, 0.6D, 0.4D, 0.02D);
        }
        if (healed > 0.0D && canBossRegenerate(boss)) {
            double capped = Math.min(healed, bossDisplayMaxHealth(boss) * (bossRegenPercent(boss) / 100.0D));
            healBoss(boss, capped);
            boss.getWorld().spawnParticle(Particle.HEART, boss.getLocation().add(0.0D, 1.4D, 0.0D), 8, 0.5D, 0.4D, 0.5D, 0.02D);
        }
    }

    private void shieldPhase(LivingEntity boss, List<Player> nearby) {
        long durationTicks = 120L;
        boss.getPersistentDataContainer().set(bossShieldUntilKey, PersistentDataType.LONG, System.currentTimeMillis() + durationTicks * 50L);
        String name = boss.getCustomName() == null ? "The boss" : boss.getCustomName();
        for (Player player : nearby) player.sendMessage(CC.color("&c" + name + " raises a shield!"));
        boss.getWorld().spawnParticle(Particle.ENCHANT, boss.getLocation().add(0.0D, 1.2D, 0.0D), 45, 1.0D, 0.8D, 1.0D, 0.02D);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!boss.isValid() || boss.isDead()) return;
            boss.getPersistentDataContainer().remove(bossShieldUntilKey);
            for (Player player : nearbyBossPlayers(boss, boss.getPersistentDataContainer().getOrDefault(zoneKey, PersistentDataType.STRING, ""), 18.0D)) {
                player.sendMessage(CC.color("&a" + (boss.getCustomName() == null ? "The boss" : boss.getCustomName()) + "'s shield fades."));
            }
        }, durationTicks);
    }

    private void tickEnrageBelowHealth(LivingEntity boss, List<Player> nearby, String abilitiesPath) {
        if (enragedBosses.contains(boss.getUniqueId())) return;
        double threshold = Math.clamp(bossAbilityDouble(abilitiesPath, "enrage-health-percent", "enrage-health-percent", 50.0D), 1.0D, 99.0D);
        if (bossHealthPercent(boss) > threshold) return;
        enragedBosses.add(boss.getUniqueId());
        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 90, 0, true, true, true));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 90, 0, true, true, true));
        String name = boss.getCustomName() == null ? "The boss" : boss.getCustomName();
        for (Player player : nearby) player.sendMessage(CC.color("&c" + name + " becomes enraged!"));
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8F, 0.85F);
    }

    private void commandMinions(String zone, LivingEntity boss, Player target) {
        Set<UUID> current = spawned.get(zone.toLowerCase());
        if (current == null) return;
        for (UUID uuid : current) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity instanceof Monster minion && isZoneMob(minion) && !isZoneBoss(minion)
                    && minion.getWorld().equals(boss.getWorld())
                    && minion.getLocation().distanceSquared(boss.getLocation()) <= 400.0D) {
                minion.setTarget(target);
            }
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PILLAGER_CELEBRATE, 0.9F, 0.7F);
    }

    private void pullAndSmash(LivingEntity boss, List<Player> nearby) {
        pullPlayers(boss, visiblePlayers(boss, nearby, 9.0D));
        boss.getWorld().spawnParticle(Particle.PORTAL, boss.getLocation().add(0.0D, 1.0D, 0.0D), 55, 1.5D, 1.0D, 1.5D, 0.03D);
        Bukkit.getScheduler().runTaskLater(plugin, () -> groundSlam(boss, nearby), 25L);
    }

    private void dashAttack(LivingEntity boss, Player target) {
        Vector dash = target.getLocation().toVector().subtract(boss.getLocation().toVector()).normalize().multiply(1.25D).setY(0.12D);
        boss.setVelocity(dash);
        boss.getWorld().spawnParticle(Particle.CLOUD, boss.getLocation().add(0.0D, 0.2D, 0.0D), 18, 0.4D, 0.1D, 0.4D, 0.02D);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline() && !target.isDead() && target.getLocation().distanceSquared(boss.getLocation()) <= 9.0D) {
                target.damage(bossAbilityDamage("dash-attack-damage", 8.0D), boss);
            }
        }, 12L);
    }

    private void aoeKnockback(LivingEntity boss, List<Player> nearby) {
        for (Player player : visiblePlayers(boss, nearby, 6.0D)) {
            Vector push = player.getLocation().toVector().subtract(boss.getLocation().toVector()).normalize().multiply(1.2D).setY(0.35D);
            player.setVelocity(push);
            player.damage(bossAbilityDamage("aoe-knockback-damage", 4.0D), boss);
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.45F, 1.35F);
    }

    private void weaknessCurse(LivingEntity boss, List<Player> nearby) {
        for (Player player : visiblePlayers(boss, nearby, 8.0D)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 0, true, true, true));
            player.getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation().add(0.0D, 1.0D, 0.0D), 12, 0.35D, 0.5D, 0.35D, 0.01D);
        }
    }

    private void healingPulse(String zone, LivingEntity boss) {
        Set<UUID> current = spawned.get(zone.toLowerCase());
        if (current != null) {
            for (UUID uuid : current) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity instanceof LivingEntity living && isZoneMob(living) && !living.equals(boss)
                        && living.getWorld().equals(boss.getWorld())
                        && living.getLocation().distanceSquared(boss.getLocation()) <= 64.0D) {
                    living.setHealth(Math.min(living.getMaxHealth(), living.getHealth() + Math.max(4.0D, living.getMaxHealth() * 0.08D)));
                    updateMobName(living);
                }
            }
        }
        if (canBossRegenerate(boss)) healBossByConfiguredRate(boss);
        boss.getWorld().spawnParticle(Particle.HEART, boss.getLocation().add(0.0D, 1.2D, 0.0D), 18, 1.0D, 0.7D, 1.0D, 0.02D);
    }

    private boolean canBossRegenerate(LivingEntity boss) {
        double pauseSeconds = Math.clamp(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getDouble("zones.boss-regen-damage-pause-seconds", 7.0D), 5.0D, 10.0D);
        return System.currentTimeMillis() - bossLastDamagedAt.getOrDefault(boss.getUniqueId(), 0L) >= (long) (pauseSeconds * 1000.0D);
    }

    private double bossRegenPercent(LivingEntity boss) {
        String zone = boss.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING);
        String key = boss.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
        return switch (bossTier(zone, key)) {
            case "final" -> Math.clamp(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.final-boss-regen-percent", 0.75D), 0.0D, 0.75D);
            case "elite" -> Math.clamp(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.elite-boss-regen-percent", 0.5D), 0.0D, 0.5D);
            default -> Math.clamp(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.boss-regen-percent", 0.25D), 0.0D, 0.25D);
        };
    }

    private void healBossByConfiguredRate(LivingEntity boss) {
        double amount = bossDisplayMaxHealth(boss) * (bossRegenPercent(boss) / 100.0D);
        if (amount <= 0.0D) return;
        healBoss(boss, amount);
    }

    private void healBoss(LivingEntity boss, double amount) {
        if (amount <= 0.0D || boss.isDead() || !boss.isValid()) return;
        DungeonMobData data = dungeonMobData.computeIfAbsent(boss.getUniqueId(), ignored -> readDungeonMobData(boss));
        if (data == null || !data.boss) return;
        data.currentHealth = Math.min(data.maxHealth, data.currentHealth + amount);
        writeBossHealth(boss, data.currentHealth, data.maxHealth);
        syncBossBukkitHealth(boss, data.currentHealth, data.maxHealth);
        updateMobName(boss);
    }

    private void projectileBarrage(LivingEntity boss, List<Player> nearby) {
        for (Player player : visiblePlayers(boss, nearby, 16.0D).stream().limit(3).toList()) {
            Arrow arrow = boss.launchProjectile(Arrow.class);
            arrow.setShooter(boss);
            arrow.setDamage(bossAbilityDamage("projectile-barrage-damage", 5.0D));
            Vector velocity = player.getEyeLocation().toVector().subtract(arrow.getLocation().toVector()).normalize().multiply(1.2D);
            arrow.setVelocity(velocity);
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1.0F, 0.8F);
    }

    private void trapRune(LivingEntity boss, Player target) {
        Location mark = target.getLocation().clone();
        mark.getWorld().spawnParticle(Particle.ENCHANT, mark.clone().add(0.0D, 0.08D, 0.0D), 40, 1.0D, 0.05D, 1.0D, 0.02D);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : mark.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(mark) > 4.0D) continue;
                player.damage(bossAbilityDamage("trap-rune-damage", 7.0D), boss);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2, true, true, true));
            }
            mark.getWorld().spawnParticle(Particle.REVERSE_PORTAL, mark, 35, 1.0D, 0.2D, 1.0D, 0.02D);
        }, 45L);
    }

    private void bleedStrike(LivingEntity boss, Player target) {
        if (!boss.hasLineOfSight(target) || target.getLocation().distanceSquared(boss.getLocation()) > 36.0D) return;
        target.damage(bossAbilityDamage("bleed-strike-damage", 5.0D), boss);
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0.0D, 1.0D, 0.0D), 16, 0.4D, 0.5D, 0.4D, 0.02D);
        for (int i = 1; i <= 3; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (target.isOnline() && !target.isDead()) target.damage(bossAbilityDamage("bleed-tick-damage", 2.0D), boss);
            }, i * 30L);
        }
    }

    private void tickLowHealthPhase(String zone, String spawnId, LivingEntity boss, Player target, List<Player> nearby, String abilitiesPath) {
        String phasePath = abilitiesPath + "low-health.";
        if (!dataManager.getData().getBoolean(phasePath + "enabled", false)) return;
        if (boss.getPersistentDataContainer().has(phaseKey, PersistentDataType.BYTE)) return;

        double threshold = Math.clamp(dataManager.getData().getDouble(phasePath + "threshold-percent", 35.0D), 1.0D, 99.0D);
        if (bossHealthPercent(boss) > threshold) return;

        boss.getPersistentDataContainer().set(phaseKey, PersistentDataType.BYTE, (byte) 1);
        List<String> messages = dataManager.getData().getStringList(phasePath + "messages");
        if (!messages.isEmpty()) {
            String picked = messages.get(ThreadLocalRandom.current().nextInt(messages.size())).replace("%boss%", boss.getCustomName() == null ? "Boss" : boss.getCustomName());
            for (Player player : nearby) player.sendMessage(CC.color(picked));
        }

        if (dataManager.getData().getBoolean(phasePath + "pull", false)) pullPlayers(boss, nearby);
        if (dataManager.getData().getBoolean(phasePath + "teleport-to-spawn", false)) {
            Location spawn = loadLocation(path(zone) + ".boss-spawns." + spawnId + ".location");
            if (spawn != null && spawn.getWorld() != null) boss.teleport(spawn);
        } else if (dataManager.getData().getBoolean(phasePath + "teleport-near-target", false)) {
            teleportNearTarget(boss, target);
        }

        double requestedHeal = Math.clamp(dataManager.getData().getDouble(phasePath + "heal-percent", 0.0D), 0.0D, 100.0D);
        if (requestedHeal > 0.0D && canBossRegenerate(boss)) {
            healBossByConfiguredRate(boss);
            boss.getWorld().spawnParticle(Particle.HEART, boss.getLocation().add(0.0D, 1.4D, 0.0D), 12, 0.7D, 0.5D, 0.7D, 0.02D);
        }

        int summonAmount = Math.max(0, dataManager.getData().getInt(phasePath + "summon-amount", 0));
        if (summonAmount > 0) {
            String typeName = dataManager.getData().getString(phasePath + "summon-type", "ZOMBIE");
            summonMinions(zone, boss, target, parseType(typeName), summonAmount, Math.max(1, boss.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER, 1) - 1));
        }
        updateMobName(boss);
    }

    private void pullPlayers(LivingEntity boss, List<Player> nearby) {
        for (Player player : nearby) {
            Vector pull = boss.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.85D).setY(0.22D);
            player.setVelocity(pull);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 0.65F);
        }
        boss.getWorld().spawnParticle(Particle.PORTAL, boss.getLocation().add(0.0D, 1.0D, 0.0D), 40, 1.4D, 0.9D, 1.4D, 0.03D);
    }

    private void summonBossMinions(String zone, LivingEntity boss, Player target) {
        Set<UUID> current = spawned.computeIfAbsent(zone.toLowerCase(), ignored -> new HashSet<>());
        int maxMinions = Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("zones.boss-abilities.max-minions", 4));
        long existingMinions = current.stream()
                .map(Bukkit::getEntity)
                .filter(entity -> entity instanceof LivingEntity living && isZoneMob(living) && !isZoneBoss(living))
                .count();
        if (existingMinions >= maxMinions) return;
        int level = Math.max(1, boss.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER, 1) - 2);
        int amount = Math.min(maxMinions - (int) existingMinions, ThreadLocalRandom.current().nextInt(1, 3));
        summonMinions(zone, boss, target, EntityType.ZOMBIE, amount, level);
    }

    private void summonMinions(String zone, LivingEntity boss, Player target, EntityType type, int amount, int level) {
        Set<UUID> current = spawned.computeIfAbsent(zone.toLowerCase(), ignored -> new HashSet<>());
        for (int i = 0; i < amount; i++) {
            Location location = boss.getLocation().clone().add(ThreadLocalRandom.current().nextDouble(-2.0D, 2.0D), 0.0D, ThreadLocalRandom.current().nextDouble(-2.0D, 2.0D));
            LivingEntity living = spawnOptimizedZoneMob("zone_" + zone + "_boss_minion", location, type, "Boss Minion", Math.max(20.0D, 80.0D + level * 20.0D), 2.0D + level, level);
            if (living != null) {
                current.add(living.getUniqueId());
                living.setRemoveWhenFarAway(false);
                tagMob(living, zone, "boss_minion", false, level);
                applyLevelStats(living, zone, level);
                updateMobName(living);
                if (living instanceof Monster minion) minion.setTarget(target);
            }
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0F, 0.75F);
    }

    private void strikeNearby(LivingEntity boss, Player target) {
        target.getWorld().strikeLightningEffect(target.getLocation());
        target.damage(Math.max(1.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.boss-abilities.lightning-damage", 6.0D)), boss);
    }

    private void teleportNearTarget(LivingEntity boss, Player target) {
        Location location = target.getLocation().clone().add(ThreadLocalRandom.current().nextDouble(-3.0D, 3.0D), 0.0D, ThreadLocalRandom.current().nextDouble(-3.0D, 3.0D));
        boss.teleport(location);
        boss.getWorld().spawnParticle(Particle.PORTAL, boss.getLocation().add(0.0D, 1.0D, 0.0D), 30, 0.6D, 1.0D, 0.6D, 0.05D);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.7F);
    }

    private void sendBossMessage(LivingEntity boss, List<Player> nearby, String spawnId) {
        String name = boss.getCustomName() == null ? "&cDungeon Boss" : boss.getCustomName();
        String zone = boss.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING);
        List<String> messages = zone == null ? List.of() : dataManager.getData().getStringList(path(zone) + ".boss-spawns." + spawnId + ".abilities.messages");
        if (messages.isEmpty()) messages = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("zones.boss-abilities.messages");
        if (messages.isEmpty()) messages = List.of("&c%boss% &8> &7You cannot run from this dungeon.");
        String picked = messages.get(ThreadLocalRandom.current().nextInt(messages.size())).replace("%boss%", name);
        for (Player player : nearby) player.sendMessage(CC.color(picked));
    }

    private void containZoneMobs(String zone) {
        Set<UUID> entities = spawned.get(zone.toLowerCase());
        if (entities == null) return;
        for (UUID uuid : new HashSet<>(entities)) {
            Entity entity = Bukkit.getEntity(uuid);
            if (!(entity instanceof LivingEntity living) || living.isDead()) {
                entities.remove(uuid);
                continue;
            }
            if (!isInside(zone, living.getLocation())) {
                if (living instanceof Mob mob) mob.setTarget(null);
                Location spawn = getTrackedSpawn(living, zone);
                if (spawn != null) living.teleport(spawn);
                continue;
            }
            if (living instanceof Mob mob && mob.getTarget() instanceof Player target && !isInside(zone, target.getLocation())) {
                mob.setTarget(null);
            }
            updateMobName(living);
        }
    }

    private void despawnZone(String zone) {
        Set<UUID> entities = spawned.remove(zone.toLowerCase());
        if (entities == null) return;
        for (UUID uuid : entities) {
            bossTargets.remove(uuid);
            bossDeadTargets.remove(uuid);
            bossAbilityCooldowns.remove(uuid);
            bossLastDamagedAt.remove(uuid);
            enragedBosses.remove(uuid);
            dungeonMobData.remove(uuid);
            processingDamage.remove(uuid);
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) entity.remove();
        }
        spawnCursor.remove(zone.toLowerCase(Locale.ROOT));
        lastSpawnDebugLog.remove(zone.toLowerCase(Locale.ROOT));
        ConfigurationSection bosses = dataManager.getData().getConfigurationSection(path(zone) + ".boss-spawns");
        if (bosses != null) {
            for (String key : bosses.getKeys(false)) dataManager.getData().set(path(zone) + ".boss-spawns." + key + ".active", null);
        }
    }

    private void clearAllSpawned() {
        for (String zone : new ArrayList<>(spawned.keySet())) despawnZone(zone);
    }

    private boolean hasNearbyPlayer(String zone) {
        Location center = center(zone);
        if (center == null || center.getWorld() == null) return false;
        double radius = dataManager.getData().getDouble(path(zone) + ".activation-radius", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.default-activation-radius", 48.0D));
        double radiusSq = radius * radius;
        for (Player player : center.getWorld().getPlayers()) {
            if (!isInside(zone, player.getLocation()) && player.getLocation().distanceSquared(center) > radiusSq) continue;
            return true;
        }
        return false;
    }

    private void discover(Player player, String zone) {
        String base = path(zone);
        String discoveredPath = discoveryPath(player, zone);
        boolean first = !isDiscovered(player, zone);
        if (first) {
            dataManager.getData().set(discoveredPath, null);
            dataManager.getData().set(discoveredPath + ".discovered", true);
            dataManager.getData().set(discoveredPath + ".discovered-at", System.currentTimeMillis());
            dataManager.getData().set(discoveredPath + ".first-location.world", player.getWorld().getName());
            dataManager.getData().set(discoveredPath + ".first-location.x", player.getLocation().getX());
            dataManager.getData().set(discoveredPath + ".first-location.y", player.getLocation().getY());
            dataManager.getData().set(discoveredPath + ".first-location.z", player.getLocation().getZ());
            if (!hasTravelRequirements(zone)) dataManager.getData().set(discoveredPath + ".travel-unlocked", true);
            dataManager.save();
            if (plugin instanceof club.abelix.games.fakevival.Game game) game.getEventManager().recordZoneDiscovery(player);
        }
        String display = dataManager.getData().getString(base + ".display", pretty(zone));
        String message = dataManager.getData().getString(base + (first ? ".discover-message" : ".enter-message"), first
                ? club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("zones.messages.discover", "&a&lZONE DISCOVERED! &7You discovered &e%zone%&7.")
                : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("zones.messages.enter", "&aEntering &f%zone%&7..."));
        player.sendMessage(CC.color(message.replace("%zone%", display)));
        if (first) {
            player.sendTitle(CC.color("&aZone Discovered"), CC.color("&e" + display), 10, 50, 10);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.1F);
        }
    }

    private List<String> zonesAt(Location location) {
        if (location == null || location.getWorld() == null
                || !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("zones.enabled", true)) return List.of();
        ConfigurationSection section = dataManager.getData().getConfigurationSection("map-zones");
        if (section == null) return List.of();
        ZoneChunkKey key = new ZoneChunkKey(location.getWorld().getUID(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        List<String> candidates = zoneChunkCache.computeIfAbsent(key, ignored -> section.getKeys(false).stream()
                .filter(zone -> zoneIntersectsChunk(zone, location.getWorld(), key.x, key.z))
                .toList());
        List<String> result = new ArrayList<>();
        for (String zone : candidates) {
            if (isInside(zone, location)) result.add(zone);
        }
        return result;
    }

    private boolean zoneIntersectsChunk(String zone, World world, int chunkX, int chunkZ) {
        Location pos1 = loadLocation(path(zone) + ".pos1");
        Location pos2 = loadLocation(path(zone) + ".pos2");
        if (pos1 == null || pos2 == null) return true;
        if (!world.equals(pos1.getWorld()) || !world.equals(pos2.getWorld())) return false;
        double chunkMinX = chunkX << 4;
        double chunkMaxX = chunkMinX + 15.999D;
        double chunkMinZ = chunkZ << 4;
        double chunkMaxZ = chunkMinZ + 15.999D;
        return Math.max(pos1.getX(), pos2.getX()) >= chunkMinX
                && Math.min(pos1.getX(), pos2.getX()) <= chunkMaxX
                && Math.max(pos1.getZ(), pos2.getZ()) >= chunkMinZ
                && Math.min(pos1.getZ(), pos2.getZ()) <= chunkMaxZ;
    }

    private record ZoneChunkKey(UUID worldId, int x, int z) {
    }

    private boolean isInside(String zone, Location location) {
        Location pos1 = loadLocation(path(zone) + ".pos1");
        Location pos2 = loadLocation(path(zone) + ".pos2");
        if (location == null || location.getWorld() == null) return false;
        if (pos1 == null || pos2 == null) return isNearZoneSpawn(zone, location);
        if (!location.getWorld().equals(pos1.getWorld()) || !pos1.getWorld().equals(pos2.getWorld())) return false;
        if (dataManager.getData().getString(path(zone) + ".zone_shape", "CUBOID").equalsIgnoreCase("POLY")) {
            double minY=dataManager.getData().getDouble(path(zone)+".min_y",Math.min(pos1.getY(),pos2.getY()));
            double maxY=dataManager.getData().getDouble(path(zone)+".max_y",Math.max(pos1.getY(),pos2.getY()));
            return location.getY()>=minY&&location.getY()<=maxY&&insidePolygon(location.getX(),location.getZ(),polyPoints(zone));
        }
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return location.getX() >= minX && location.getX() <= maxX
                && location.getY() >= minY && location.getY() <= maxY
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    private Player bossTarget(LivingEntity boss) {
        UUID targetId = bossTargets.get(boss.getUniqueId());
        if (targetId == null) return null;
        Player player = Bukkit.getPlayer(targetId);
        if (player == null || !player.isOnline() || player.isDead()) return null;
        return player;
    }

    private Location center(String zone) {
        Location pos1 = loadLocation(path(zone) + ".pos1");
        Location pos2 = loadLocation(path(zone) + ".pos2");
        if (pos1 == null || pos2 == null || pos1.getWorld() == null || !pos1.getWorld().equals(pos2.getWorld())) return averageSpawnLocation(zone);
        if(dataManager.getData().getString(path(zone)+".zone_shape","CUBOID").equalsIgnoreCase("POLY")){List<PolyPoint>points=polyPoints(zone);if(!points.isEmpty()){double x=points.stream().mapToDouble(PolyPoint::x).average().orElse(pos1.getX()),z=points.stream().mapToDouble(PolyPoint::z).average().orElse(pos1.getZ());double y=(dataManager.getData().getDouble(path(zone)+".min_y",pos1.getY())+dataManager.getData().getDouble(path(zone)+".max_y",pos2.getY()))/2D;return new Location(pos1.getWorld(),x,y,z);}}
        return new Location(pos1.getWorld(), (pos1.getX() + pos2.getX()) / 2.0D, (pos1.getY() + pos2.getY()) / 2.0D, (pos1.getZ() + pos2.getZ()) / 2.0D);
    }

    private List<PolyPoint> polyPoints(String zone){String raw=dataManager.getData().getString(path(zone)+".points_json","");if(raw.isBlank())return List.of();try{JsonArray array=JsonParser.parseString(raw).getAsJsonArray();List<PolyPoint>result=new ArrayList<>();for(var element:array){JsonObject point=element.getAsJsonObject();result.add(new PolyPoint(point.get("x").getAsDouble(),point.get("z").getAsDouble()));}return result;}catch(RuntimeException exception){plugin.getLogger().warning("[SMP] Invalid polygon points for zone "+zone+"; ignoring polygon shape.");return List.of();}}
    private boolean insidePolygon(double x,double z,List<PolyPoint>points){if(points.size()<3)return false;boolean inside=false;for(int i=0,j=points.size()-1;i<points.size();j=i++){PolyPoint a=points.get(i),b=points.get(j);if(((a.z>z)!=(b.z>z))&&(x<(b.x-a.x)*(z-a.z)/(b.z-a.z)+a.x))inside=!inside;}return inside;}
    private record PolyPoint(double x,double z){}

    private boolean isNearZoneSpawn(String zone, Location location) {
        double radius = dataManager.getData().getDouble(path(zone) + ".activation-radius", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.default-activation-radius", 48.0D));
        double radiusSq = radius * radius;
        for (Location spawn : spawnLocations(zone)) {
            if (spawn.getWorld() != null && spawn.getWorld().equals(location.getWorld()) && spawn.distanceSquared(location) <= radiusSq) return true;
        }
        return false;
    }

    private Location averageSpawnLocation(String zone) {
        List<Location> locations = spawnLocations(zone);
        if (locations.isEmpty()) return null;
        World world = locations.getFirst().getWorld();
        if (world == null) return null;
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        int count = 0;
        for (Location location : locations) {
            if (location.getWorld() == null || !location.getWorld().equals(world)) continue;
            x += location.getX();
            y += location.getY();
            z += location.getZ();
            count++;
        }
        if (count == 0) return null;
        return new Location(world, x / count, y / count, z / count);
    }

    private List<Location> spawnLocations(String zone) {
        List<Location> locations = new ArrayList<>();
        ConfigurationSection spawns = dataManager.getData().getConfigurationSection(path(zone) + ".spawns");
        if (spawns != null) {
            for (String key : spawns.getKeys(false)) {
                Location location = loadLocation(path(zone) + ".spawns." + key + ".location");
                if (location != null) locations.add(location);
            }
        }
        ConfigurationSection bosses = dataManager.getData().getConfigurationSection(path(zone) + ".boss-spawns");
        if (bosses != null) {
            for (String key : bosses.getKeys(false)) {
                Location location = loadLocation(path(zone) + ".boss-spawns." + key + ".location");
                if (location != null) locations.add(location);
            }
        }
        return locations;
    }

    private void applyType(String name, String type) {
        String normalized = type == null ? "safe" : type.toLowerCase();
        String base = path(name);
        dataManager.getData().set(base + ".type", normalized);
        dataManager.getData().set(base + ".allow-pvp", normalized.equals("pvp"));
        dataManager.getData().set(base + ".allow-mobs", normalized.equals("combat") || normalized.equals("dungeon"));
        dataManager.getData().set(base + ".allow-build", normalized.equals("pvp"));
        dataManager.getData().set(base + ".allow-break", normalized.equals("mining") || normalized.equals("farming") || normalized.equals("pvp"));
        dataManager.getData().set(base + ".allow-place", normalized.equals("pvp"));
        dataManager.getData().set(base + ".max-mobs", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("zones.default-max-mobs", 12));
        dataManager.getData().set(base + ".activation-radius", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.default-activation-radius", 48.0D));
        if (normalized.equals("mining")) {
            dataManager.getData().set(base + ".regen-delay-ticks", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("zones.regeneration.mining-delay-ticks", 200L));
            dataManager.getData().set(base + ".regen-blocks", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("zones.regeneration.mining-blocks"));
        } else if (normalized.equals("farming")) {
            dataManager.getData().set(base + ".regen-delay-ticks", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("zones.regeneration.farming-delay-ticks", 100L));
        }
    }

    private boolean canBreakInZone(String zone, Block block) {
        String type = dataManager.getData().getString(path(zone) + ".type", "safe").toLowerCase();
        if (!dataManager.getData().getBoolean(path(zone) + ".allow-break", false)) return true;
        if (type.equals("mining")) return !isRegeneratingMiningBlock(zone, block.getType());
        if (type.equals("farming")) return !isFarmingRegeneratingBlock(block);
        return !dataManager.getData().getBoolean(path(zone) + ".allow-build", false);
    }

    private boolean isRegeneratingMiningBlock(String zone, Material material) {
        List<String> blocks = dataManager.getData().getStringList(path(zone) + ".regen-blocks");
        if (blocks.isEmpty()) blocks = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("zones.regeneration.mining-blocks");
        for (String block : blocks) {
            if (material.name().equalsIgnoreCase(block)) return true;
        }
        return false;
    }

    private boolean isFullyGrownCrop(Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) return false;
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private boolean isFarmingRegeneratingBlock(Block block) {
        return isFullyGrownCrop(block);
    }

    private boolean zoneAllowsMob(String zone, EntityType type) {
        ConfigurationSection spawns = dataManager.getData().getConfigurationSection(path(zone) + ".spawns");
        if (spawns != null) for (String key : spawns.getKeys(false)) {
            String configured = dataManager.getData().getString(path(zone) + ".spawns." + key + ".type", "");
            if (type.name().equalsIgnoreCase(configured)) return true;
        }
        ConfigurationSection bosses = dataManager.getData().getConfigurationSection(path(zone) + ".boss-spawns");
        if (bosses != null) for (String key : bosses.getKeys(false)) {
            String configured = dataManager.getData().getString(path(zone) + ".boss-spawns." + key + ".type", "");
            if (type.name().equalsIgnoreCase(configured)) return true;
        }
        return false;
    }

    private boolean isBossAlive(Set<UUID> current, String zone, String key) {
        for (UUID uuid : new HashSet<>(current)) {
            Entity tracked = Bukkit.getEntity(uuid);
            if (!(tracked instanceof LivingEntity living) || living.isDead() || !living.isValid()) continue;
            if (!isExpectedBoss(living, zone, key)) continue;
            dataManager.getData().set(path(zone) + ".boss-spawns." + key + ".active", uuid.toString());
            repairActiveBoss(living, zone, key);
            return true;
        }
        String active = dataManager.getData().getString(path(zone) + ".boss-spawns." + key + ".active");
        if (active == null || active.isBlank()) return false;
        try {
            UUID uuid = UUID.fromString(active);
            Entity entity = Bukkit.getEntity(uuid);
            if (entity instanceof LivingEntity living && !living.isDead()) {
                if (!isExpectedBoss(living, zone, key)) {
                    plugin.getLogger().warning("[ZoneManager] Removing invalid active boss " + key + " in " + zone + ": missing or mismatched boss tags.");
                    living.remove();
                    current.remove(uuid);
                    dataManager.getData().set(path(zone) + ".boss-spawns." + key + ".active", null);
                    return false;
                }
                current.add(uuid);
                repairActiveBoss(living, zone, key);
                return true;
            }
            if (entity != null) entity.remove();
            current.remove(uuid);
        } catch (IllegalArgumentException ignored) {
        }
        dataManager.getData().set(path(zone) + ".boss-spawns." + key + ".active", null);
        return false;
    }

    private boolean isExpectedBoss(LivingEntity living, String zone, String key) {
        String storedZone = living.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING);
        String storedSpawn = living.getPersistentDataContainer().get(spawnKey, PersistentDataType.STRING);
        String storedBoss = living.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
        Integer level = living.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        return storedZone != null && storedZone.equalsIgnoreCase(zone)
                && key.equals(storedSpawn)
                && key.equals(storedBoss)
                && level != null && level > 0;
    }

    private void repairActiveBoss(LivingEntity living, String zone, String key) {
        int level = Math.max(1, living.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER,
                dataManager.getData().getInt(path(zone) + ".boss-spawns." + key + ".level", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("zones.default-boss-level", 10))));
        double expectedHealth = bossScaledHealth(zone, key, level, bossConfiguredHealth(zone, key));
        double expectedDefense = bossScaledDefense(zone, key, level);
        double storedDefense = living.getPersistentDataContainer().getOrDefault(bossDefenseKey, PersistentDataType.DOUBLE, 0.0D);
        double storedMaxHealth = living.getPersistentDataContainer().getOrDefault(bossRealMaxHealthKey, PersistentDataType.DOUBLE,
                living.getPersistentDataContainer().getOrDefault(bossMaxHealthKey, PersistentDataType.DOUBLE, 0.0D));
        double storedCurrentHealth = living.getPersistentDataContainer().getOrDefault(bossCurrentHealthKey, PersistentDataType.DOUBLE,
                living.getPersistentDataContainer().getOrDefault(mobHealthKey, PersistentDataType.DOUBLE, 0.0D));
        if (Math.abs(storedMaxHealth - expectedHealth) > 0.01D || storedCurrentHealth <= 0.0D || storedDefense < expectedDefense) {
            applyBossStats(living, zone, key, level, bossConfiguredHealth(zone, key));
        } else {
            dungeonMobData.putIfAbsent(living.getUniqueId(), new DungeonMobData(storedCurrentHealth, storedMaxHealth,
                    storedDefense, true, DungeonMobRole.ELITE, true));
            syncBossBukkitHealth(living, storedCurrentHealth, storedMaxHealth);
        }
        if (living.getCustomName() == null || living.getCustomName().isBlank()) updateMobName(living);
        living.setCustomNameVisible(true);
        living.setRemoveWhenFarAway(false);
    }

    private void tagMob(LivingEntity living, String zone, String spawnId, boolean boss, int level) {
        living.getPersistentDataContainer().set(zoneKey, PersistentDataType.STRING, zone.toLowerCase());
        living.getPersistentDataContainer().set(spawnKey, PersistentDataType.STRING, spawnId);
        living.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);
        String entityType = boss ? "ZONE_BOSS"
                : spawnId != null && (spawnId.toLowerCase(Locale.ROOT).contains("minion") || spawnId.equalsIgnoreCase("boss_minion"))
                ? "DUNGEON_MINION" : (isDungeonZone(zone) ? "DUNGEON_MOB" : "ZONE_MOB");
        living.getPersistentDataContainer().set(smpEntityTypeKey, PersistentDataType.STRING, entityType);
        living.getPersistentDataContainer().set(smpMobTypeKey, PersistentDataType.STRING, entityType);
        living.getPersistentDataContainer().set(smpMobLevelKey, PersistentDataType.INTEGER, level);
        living.getPersistentDataContainer().set(smpZoneIdKey, PersistentDataType.STRING, zone.toLowerCase(Locale.ROOT));
        if (isDungeonZone(zone)) {
            living.getPersistentDataContainer().set(dungeonMobKey, PersistentDataType.BYTE, (byte) 1);
            living.setFireTicks(0);
        }
        if (boss) {
            living.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, spawnId);
            living.getPersistentDataContainer().set(isZoneBossKey, PersistentDataType.BYTE, (byte) 1);
            living.getPersistentDataContainer().set(bossLevelKey, PersistentDataType.INTEGER, level);
            living.getPersistentDataContainer().set(smpBossKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            living.getPersistentDataContainer().remove(smpBossKey);
        }
    }

    private void syncSmpMobData(LivingEntity living, String zone, String spawnId, String displayName,
                                boolean boss, int level, double expectedMaxHealth) {
        String mobId = "zone_" + zone.toLowerCase(Locale.ROOT) + (boss ? "_boss_" : "_") + spawnId.toLowerCase(Locale.ROOT);
        double maxHealth = isEngineMob(living) ? engineMaxHp(living)
                : boss ? bossDisplayMaxHealth(living) : Math.max(1.0D, living.getMaxHealth());
        double currentHealth = isEngineMob(living) ? engineCurrentHp(living)
                : boss ? bossDisplayCurrentHealth(living) : Math.max(0.0D, living.getHealth());
        if (!Double.isFinite(maxHealth) || maxHealth <= 0.0D) maxHealth = Math.max(1.0D, expectedMaxHealth);
        living.getPersistentDataContainer().set(smpMobIdKey, PersistentDataType.STRING, mobId);
        living.getPersistentDataContainer().set(smpMobLevelKey, PersistentDataType.INTEGER, Math.max(1, level));
        living.getPersistentDataContainer().set(smpMobCurrentHealthKey, PersistentDataType.DOUBLE,
                Math.clamp(currentHealth, 0.0D, maxHealth));
        living.getPersistentDataContainer().set(smpMobMaxHealthKey, PersistentDataType.DOUBLE, maxHealth);
        String plainDisplayName = ChatColor.stripColor(CC.color(displayName));
        living.getPersistentDataContainer().set(smpMobDisplayNameKey, PersistentDataType.STRING,
                plainDisplayName == null || plainDisplayName.isBlank() ? pretty(spawnId) : plainDisplayName);
        living.getPersistentDataContainer().set(smpZoneIdKey, PersistentDataType.STRING, zone.toLowerCase(Locale.ROOT));
    }

    private void rewardKill(LivingEntity entity, Player player, String zone, boolean boss) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("zones.mob-rewards.enabled", true)) return;
        int level = Math.max(1, entity.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER, 1));
        String base = "zones.mob-rewards." + (boss ? "boss." : "normal.");
        double coins = rollScaledDouble(base, "coins", level, boss ? 250.0D : 15.0D, boss ? 25.0D : 2.0D);
        if (plugin instanceof club.abelix.games.fakevival.Game game) coins *= game.getEventManager().activityCoinMultiplier();
        if (plugin instanceof club.abelix.games.fakevival.Game game) coins = game.getMayorManager().applyMayorModifier(
                player, club.abelix.games.fakevival.mayor.MayorModifierType.MOB_COINS, coins);
        if (plugin instanceof club.abelix.games.fakevival.Game game) coins = game.getMayorManager().applyMayorModifier(
                player, club.abelix.games.fakevival.mayor.MayorModifierType.REWARDS, coins);
        int xp = Math.max(0, (int) Math.round(rollScaledDouble(base, "xp", level, boss ? 120.0D : 12.0D, boss ? 12.0D : 1.5D)));
        if (isDungeonZone(zone) && plugin instanceof club.abelix.games.fakevival.Game game) xp = (int) Math.round(
                game.getMayorManager().applyMayorModifier(player,
                        club.abelix.games.fakevival.mayor.MayorModifierType.DUNGEON_XP, xp));
        if (plugin instanceof club.abelix.games.fakevival.Game game) xp = (int) Math.round(
                game.getMayorManager().applyMayorModifier(player,
                        club.abelix.games.fakevival.mayor.MayorModifierType.REWARDS, xp));
        if (coins > 0.0D) economyManager.giveCoins(player.getUniqueId(), coins, "zone-kill:" + zone);
        if (xp > 0) player.giveExp(xp);
        List<String> drops = rollRareDrops(player, entity, zone, level, boss);
        if (!boss && isDungeonZone(zone)) drops.addAll(rollDungeonMaterialDrop(player, entity));
        String message = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("zones.mob-rewards.message", "&e+%coins% coins &7and &b+%xp% XP&7.%drops%");
        player.sendMessage(CC.color(message
                .replace("%zone%", dataManager.getData().getString(path(zone) + ".display", pretty(zone)))
                .replace("%coins%", economyManager.format(coins))
                .replace("%xp%", String.valueOf(xp))
                .replace("%level%", String.valueOf(level))
                .replace("%drops%", drops.isEmpty() ? "" : " &dRare: &9" + String.join("&7, &9", drops))));
    }

    private void runBossDropCommands(String zone, String key, Player killer) {
        List<String> commands = dataManager.getData().getStringList(path(zone) + ".boss-spawns." + key + ".drop-commands");
        List<String> lootBoxCommands = new ArrayList<>();
        for (String command : commands) {
            if (command.toLowerCase(java.util.Locale.ROOT).startsWith("lootbox give ")) lootBoxCommands.add(command);
            else dispatchBossCommand(command, zone, killer);
        }
        if (lootBoxCommands.isEmpty() || killer == null) return;
        double chance = Math.clamp(dataManager.getData().getDouble(path(zone) + ".boss-spawns." + key + ".lootbox-chance",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.boss-lootbox-drop-chance", 20.0D)), 0.0D, 100.0D);
        if (ThreadLocalRandom.current().nextDouble(100.0D) > chance) return;
        String picked = lootBoxCommands.get(ThreadLocalRandom.current().nextInt(lootBoxCommands.size()));
        dispatchBossCommand(picked, zone, killer);
    }

    private void dispatchBossCommand(String command, String zone, Player killer) {
        String parsed = command.replace("%player%", killer == null ? "" : killer.getName()).replace("%zone%", zone);
        if (!parsed.isBlank()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
    }

    private List<String> rollRareDrops(Player player, LivingEntity entity, String zone, int level, boolean boss) {
        List<String> given = new ArrayList<>();
        List<Map<?, ?>> configured = new ArrayList<>();
        configured.addAll(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getMapList("zones.mob-rewards.rare-drops"));
        configured.addAll(dataManager.getData().getMapList(path(zone) + ".rare-drops"));
        if (boss) {
            configured.addAll(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getMapList("zones.mob-rewards.boss.rare-drops"));
            configured.addAll(dataManager.getData().getMapList(path(zone) + ".boss-rare-drops"));
            String spawnId = entity.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
            if (spawnId != null) configured.addAll(dataManager.getData().getMapList(path(zone) + ".boss-spawns." + spawnId + ".rare-drops"));
        }
        boolean elite = entity.getPersistentDataContainer().has(mobEliteKey, PersistentDataType.BYTE);
        int maxDrops = Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("zones.mob-rewards." + (boss ? "boss." : "normal.") + "max-rare-drops", boss ? 2 : 1));
        if (!boss && elite) maxDrops = Math.max(2, maxDrops);
        for (Map<?, ?> raw : configured) {
            if (given.size() >= maxDrops) break;
            if (!matchesDropFilter(raw, entity, zone)) continue;
            int minLevel = asInt(raw.get("min-level"), 1);
            if (level < minLevel) continue;
            double chance = Math.clamp(asDouble(raw.get("chance")), 0.0D, 100.0D);
            String customItem = asString(raw.get("custom-item"), "").trim();
            if (!boss && isDungeonZone(zone) && !customItem.isBlank() && !isDungeonMaterialReward(customItem)) continue;
            if (!boss && elite) chance = Math.min(100.0D, chance * 1.5D);
            if (customItem.equalsIgnoreCase("bone_reaper")) {
                chance = Math.min(chance, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.mob-rewards.bone-reaper-max-chance", 0.6D));
            }
            if (Math.random() * 100.0D > chance) continue;
            int min = Math.max(1, asInt(raw.get("min-amount"), asInt(raw.get("amount"), 1)));
            int max = Math.max(min, asInt(raw.get("max-amount"), min));
            int amount = min + (int) Math.floor(Math.random() * ((max - min) + 1));
            String materialName = asString(raw.get("material"), "").trim();
            String display = asString(raw.get("display"), customItem.isBlank() ? materialName : customItem);
            if (!customItem.isBlank() && customItemManager.itemExists(customItem)) {
                giveOrDrop(player, customItemManager.createItem(customItem, amount));
                given.add(CC.color(display) + " x" + amount);
            }
        }
        return given;
    }

    private List<String> rollDungeonMaterialDrop(Player player, LivingEntity entity) {
        boolean elite = entity.getPersistentDataContainer().has(mobEliteKey, PersistentDataType.BYTE);
        double roll = ThreadLocalRandom.current().nextDouble(100.0D);
        String id = null;
        if (roll < (elite ? 3.0D : 1.0D)) id = "dungeon_entry_token";
        else if (roll < (elite ? 6.0D : 2.0D)) id = "ancient_key_fragment";
        else if (roll < (elite ? 16.0D : 7.0D)) id = "ancient_gear_fragment";
        else if (roll < (elite ? 75.0D : 42.0D)) {
            String[] materials = {"hardened_bone_shard", "spider_silk_bundle", "zombie_fang", "creeper_powder_core", "ancient_ore_chunk", "shadow_fang"};
            id = materials[ThreadLocalRandom.current().nextInt(materials.length)];
        }
        if (id == null || !customItemManager.itemExists(id)) return List.of();
        int amount = elite && ThreadLocalRandom.current().nextBoolean() ? 2 : 1;
        giveOrDrop(player, customItemManager.createItem(id, amount));
        return List.of(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getString("custom-items.items." + id + ".name", pretty(id))) + " x" + amount);
    }

    private boolean isDungeonMaterialReward(String customItem) {
        String raw = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getString("custom-items.items." + customItem.toLowerCase(Locale.ROOT) + ".type", "MISC");
        String normalized = normalizeType(raw);
        if (normalized.equals("RECIPE_FRAGMENT") || normalized.equals("KEY_FRAGMENT")) return true;
        MMOItemType type = MMOItemType.parse(normalized);
        return type == MMOItemType.MATERIAL || type == MMOItemType.COLLECTION_MATERIAL
                || type == MMOItemType.RECIPE_MATERIAL || type == MMOItemType.TOKEN
                || type == MMOItemType.KEY || type == MMOItemType.CURRENCY;
    }

    private boolean matchesDropFilter(Map<?, ?> raw, LivingEntity entity, String zone) {
        Object zones = raw.get("zones");
        if (zones instanceof List<?> list && !list.isEmpty()) {
            boolean match = false;
            for (Object entry : list) {
                if (zone.equalsIgnoreCase(asString(entry, ""))) {
                    match = true;
                    break;
                }
            }
            if (!match) return false;
        }
        Object mobs = raw.get("mobs");
        if (mobs instanceof List<?> list && !list.isEmpty()) {
            boolean match = false;
            String entityType = entity.getType().name();
            for (Object entry : list) {
                if (entityType.equalsIgnoreCase(asString(entry, ""))) {
                    match = true;
                    break;
                }
            }
            if (!match) return false;
        }
        return true;
    }

    private double rollScaledDouble(String base, String key, int level, double defaultBase, double defaultPerLevel) {
        double configuredBase = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(base + key + "-base", defaultBase);
        double configuredPerLevel = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(base + key + "-per-level", defaultPerLevel);
        double min = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(base + key + "-min", configuredBase);
        double max = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(base + key + "-max", configuredBase);
        if (max < min) max = min;
        double rolled = min == max ? min : ThreadLocalRandom.current().nextDouble(min, max + 0.0001D);
        return Math.max(0.0D, rolled + Math.max(0, level - 1) * configuredPerLevel);
    }

    private void giveOrDrop(Player player, org.bukkit.inventory.ItemStack item) {
        Map<Integer, org.bukkit.inventory.ItemStack> leftovers = player.getInventory().addItem(item);
        for (org.bukkit.inventory.ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private String asString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private void applyLevelStats(LivingEntity living, String zone, int level) {
        boolean dungeon = isDungeonZone(zone);
        String spawnId = living.getPersistentDataContainer().get(spawnKey, PersistentDataType.STRING);
        DungeonMobRole role = dungeon ? dungeonMobRole(zone, spawnId, living.getType()) : DungeonMobRole.GRUNT;
        boolean elite = dungeon && spawnId != null && (role == DungeonMobRole.ELITE
                || dataManager.getData().getBoolean(path(zone) + ".spawns." + spawnId + ".elite", false));
        RoleBalance balance = roleBalance(role, elite);
        double health = healthScaler.normalHealth(level);
        setEntityHealth(living, health);
        if (dungeon) {
            double defenseBase = Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.dungeon-mob-defense-base", 10.0D));
            double defensePerLevel = Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.dungeon-mob-defense-per-level", 2.0D));
            double defense = Math.min(elite ? 280.0D : 200.0D, (defenseBase + level * defensePerLevel) * balance.defenseMultiplier);
            living.getPersistentDataContainer().set(bossMaxHealthKey, PersistentDataType.DOUBLE, health);
            living.getPersistentDataContainer().set(mobHealthKey, PersistentDataType.DOUBLE, health);
            living.getPersistentDataContainer().set(bossDefenseKey, PersistentDataType.DOUBLE, defense);
            living.getPersistentDataContainer().set(mobRoleKey, PersistentDataType.STRING, role.name());
            if (elite) living.getPersistentDataContainer().set(mobEliteKey, PersistentDataType.BYTE, (byte) 1);
            applyDungeonMobMovement(living, role, elite, balance.knockbackResistance);
            dungeonMobData.put(living.getUniqueId(), new DungeonMobData(health, health, defense, false, role, elite));
        }
        double damageBase = dungeon
                ? club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.dungeon-mob-damage-base", 15.0D)
                : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.mob-damage-base", 2.0D);
        double damagePerLevel = dungeon
                ? club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.dungeon-mob-damage-per-level", 3.0D)
                : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.mob-damage-per-level", 0.25D);
        setAttackDamage(living, (damageBase + Math.max(0, level) * damagePerLevel) * balance.damageMultiplier);
    }

    private void applyBossStats(LivingEntity living, String zone, String key, int level, double configuredHealth) {
        double finalMaxHealth = bossScaledHealth(zone, key, level, configuredHealth);
        String bossName = dataManager.getData().getString(path(zone) + ".boss-spawns." + key + ".name", pretty(key));
        living.getPersistentDataContainer().set(bossConfiguredHealthKey, PersistentDataType.DOUBLE, Math.max(0.0D, configuredHealth));
        living.getPersistentDataContainer().set(bossRealMaxHealthKey, PersistentDataType.DOUBLE, finalMaxHealth);
        living.getPersistentDataContainer().set(bossCurrentHealthKey, PersistentDataType.DOUBLE, finalMaxHealth);
        living.getPersistentDataContainer().set(bossMaxHealthKey, PersistentDataType.DOUBLE, finalMaxHealth);
        living.getPersistentDataContainer().set(mobHealthKey, PersistentDataType.DOUBLE, finalMaxHealth);
        living.getPersistentDataContainer().set(bossLevelKey, PersistentDataType.INTEGER, level);
        living.getPersistentDataContainer().set(bossNameKey, PersistentDataType.STRING, bossName);
        living.getPersistentDataContainer().set(isZoneBossKey, PersistentDataType.BYTE, (byte) 1);
        syncBossBukkitHealth(living, finalMaxHealth, finalMaxHealth);
        double defense = isDungeonZone(zone) ? bossScaledDefense(zone, key, level) : 0.0D;
        if (defense > 0.0D) living.getPersistentDataContainer().set(bossDefenseKey, PersistentDataType.DOUBLE, defense);
        else living.getPersistentDataContainer().remove(bossDefenseKey);
        if (isDungeonZone(zone)) {
            setKnockbackResistance(living, 0.85D);
            dungeonMobData.put(living.getUniqueId(), new DungeonMobData(finalMaxHealth, finalMaxHealth, defense, true, DungeonMobRole.ELITE, true));
        }
        double damageBase = Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.boss-damage-base", 25.0D));
        double damagePerLevel = Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.boss-damage-per-level", 3.5D));
        setAttackDamage(living, Math.max(configuredBossStat(zone, key, "damage"),
                damageBase + Math.max(0, level - 1) * damagePerLevel));
        AttributeInstance scale = living.getAttribute(Attribute.SCALE);
        if (scale != null) scale.setBaseValue(Math.max(0.1D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.boss-visual-scale", 1.5D)));
    }

    private double bossScaledHealth(String zone, String key, int level, double configuredHealth) {
        return healthScaler.zoneBossHealth(level, configuredHealth);
    }

    private double bossScaledDefense(String zone, String key, int level) {
        double base = Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.default-boss-defense", 50.0D));
        double perLevel = Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.boss-defense-per-level", 5.0D));
        double configured = configuredBossStat(zone, key, "defense");
        double multiplier = switch (bossTier(zone, key)) {
            case "final" -> Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.final-boss-defense-multiplier", 1.5D));
            case "elite" -> Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.elite-boss-defense-multiplier", 1.25D));
            default -> 1.0D;
        };
        double raw = Math.max(configured, base + Math.max(0, level) * perLevel) * multiplier;
        double cap = switch (bossTier(zone, key)) { case "final" -> 600.0D; case "elite" -> 400.0D; default -> 250.0D; };
        double clamped = Math.min(raw, cap);
        if (raw > cap && warnedDefenseClamps.add(zone.toLowerCase(Locale.ROOT) + ":" + key.toLowerCase(Locale.ROOT))) {
            plugin.getLogger().warning("[SMP] Clamped boss defense for " + key + " from " + raw + " to " + clamped + ".");
        }
        return clamped;
    }

    private double configuredBossStat(String zone, String key, String stat) {
        String base = path(zone) + ".boss-spawns." + key;
        double configured = Math.max(0.0D, dataManager.getData().getDouble(base + "." + stat, 0.0D));
        String templateId = dataManager.getData().getString(base + ".template", "");
        if (templateId.isBlank()) return configured;
        double template = Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getDouble("zones.boss-templates." + templateId.toLowerCase(Locale.ROOT) + "." + stat, 0.0D));
        return Math.max(configured, template);
    }

    private double bossHealthScale(String zone, String key) {
        return switch (bossTier(zone, key)) {
            case "final" -> Math.max(0.01D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.final-boss-health-scale", 3.5D));
            case "elite" -> Math.max(0.01D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.elite-boss-health-scale", 2.5D));
            default -> Math.max(0.01D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.boss-health-scale", 1.5D));
        };
    }

    private String bossTier(String zone, String key) {
        String base = path(zone) + ".boss-spawns." + key;
        String tier = dataManager.getData().getString(base + ".tier", dataManager.getData().getString(base + ".boss-tier", ""));
        if (dataManager.getData().getBoolean(base + ".final-boss", false) || "final".equalsIgnoreCase(tier)) return "final";
        if (dataManager.getData().getBoolean(base + ".elite", false) || "elite".equalsIgnoreCase(tier)) return "elite";
        return "normal";
    }

    private void setEntityHealth(LivingEntity living, double requestedHealth) {
        if (isEngineMob(living)) return;
        double health = vanillaHealth(requestedHealth);
        AttributeInstance maxHealth = living.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(health);
        else living.setMaxHealth(health);
        living.setHealth(living.getMaxHealth());
    }

    private void syncBossBukkitHealth(LivingEntity living, double currentHealth, double maxHealth) {
        if (isEngineMob(living)) return;
        double safeConfigured = Math.min(MAX_BUKKIT_HEALTH, Math.max(1.0D,
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                        .getDouble("zones.max-safe-bukkit-boss-health", 1024.0D)));
        double requestedSafeMax = Math.min(Math.max(1.0D, maxHealth), safeConfigured);
        AttributeInstance attribute = living.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null && Math.abs(attribute.getBaseValue() - requestedSafeMax) > 0.001D) {
            attribute.setBaseValue(requestedSafeMax);
        }
        double bukkitMax = Math.max(1.0D, living.getMaxHealth());
        double ratio = Math.clamp(currentHealth / Math.max(1.0D, maxHealth), 0.0D, 1.0D);
        double visualHealth = Math.clamp(bukkitMax * ratio, 0.01D, bukkitMax);
        if (!living.isDead() && currentHealth > 0.0D) living.setHealth(visualHealth);
    }

    private void writeBossHealth(LivingEntity living, double currentHealth, double maxHealth) {
        double current = Math.clamp(currentHealth, 0.0D, Math.max(1.0D, maxHealth));
        double max = Math.max(1.0D, maxHealth);
        living.getPersistentDataContainer().set(bossCurrentHealthKey, PersistentDataType.DOUBLE, current);
        living.getPersistentDataContainer().set(bossRealMaxHealthKey, PersistentDataType.DOUBLE, max);
        living.getPersistentDataContainer().set(mobHealthKey, PersistentDataType.DOUBLE, current);
        living.getPersistentDataContainer().set(bossMaxHealthKey, PersistentDataType.DOUBLE, max);
    }

    private double bossDisplayCurrentHealth(LivingEntity living) {
        if (isEngineMob(living)) return engineCurrentHp(living);
        Double stored = living.getPersistentDataContainer().get(bossCurrentHealthKey, PersistentDataType.DOUBLE);
        if (stored == null) stored = living.getPersistentDataContainer().get(mobHealthKey, PersistentDataType.DOUBLE);
        if (stored != null) return Math.max(0.0D, stored);
        DungeonMobData cached = dungeonMobData.get(living.getUniqueId());
        return cached == null ? Math.max(0.0D, living.getHealth()) : Math.max(0.0D, cached.currentHealth);
    }

    private double bossDisplayMaxHealth(LivingEntity living) {
        if (isEngineMob(living)) return engineMaxHp(living);
        Double stored = living.getPersistentDataContainer().get(bossRealMaxHealthKey, PersistentDataType.DOUBLE);
        if (stored == null) stored = living.getPersistentDataContainer().get(bossMaxHealthKey, PersistentDataType.DOUBLE);
        if (stored != null) return Math.max(1.0D, stored);
        DungeonMobData cached = dungeonMobData.get(living.getUniqueId());
        return cached == null ? Math.max(1.0D, living.getMaxHealth()) : Math.max(1.0D, cached.maxHealth);
    }

    private double bossHealthPercent(LivingEntity living) {
        return bossDisplayCurrentHealth(living) / bossDisplayMaxHealth(living) * 100.0D;
    }

    private double vanillaHealth(double requestedHealth) {
        return Math.clamp(Double.isFinite(requestedHealth) ? requestedHealth : 1.0D, 1.0D, MAX_BUKKIT_HEALTH);
    }

    private double bossConfiguredHealth(String zone, String key) {
        double configured = dataManager.getData().getDouble(path(zone) + ".boss-spawns." + key + ".health", defaultBossHealth());
        return Math.max(1.0D, configured);
    }

    private double defaultBossHealth() {
        return Math.max(1.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("zones.default-boss-health", 15000.0D));
    }

    private long bossRespawnMillis() {
        long seconds = Math.max(1L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("zones.boss-respawn-seconds", 120L));
        return seconds * 1000L;
    }

    private boolean isDungeonZone(String zone) {
        return zone != null && dataManager.getData().getString(path(zone) + ".type", "safe").equalsIgnoreCase("dungeon");
    }

    private void setAttackDamage(LivingEntity living, double damage) {
        AttributeInstance attackDamage = living.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage != null) attackDamage.setBaseValue(Math.max(0.0D, damage));
    }

    private DungeonMobRole dungeonMobRole(String zone, String spawnId, EntityType entityType) {
        if (spawnId == null) return parseDungeonMobRole(null, entityType);
        String base = path(zone) + ".spawns." + spawnId;
        String configured = dataManager.getData().getString(base + ".role", "");
        if (!configured.isBlank()) return parseDungeonMobRole(configured, entityType);
        String hint = (spawnId + " " + dataManager.getData().getString(base + ".template", "") + " "
                + dataManager.getData().getString(base + ".name", "")).toLowerCase(Locale.ROOT);
        for (DungeonMobRole role : DungeonMobRole.values()) {
            if (hint.contains(role.name().toLowerCase(Locale.ROOT))) return role;
        }
        if (hint.contains("shaman") || hint.contains("priest") || hint.contains("cleric")) return DungeonMobRole.HEALER;
        return parseDungeonMobRole(null, entityType);
    }

    private DungeonMobRole parseDungeonMobRole(String raw, EntityType entityType) {
        String role = raw == null ? "" : normalizeType(raw);
        if (role.endsWith("S")) role = role.substring(0, role.length() - 1);
        try {
            if (!role.isBlank()) return DungeonMobRole.valueOf(role);
        } catch (IllegalArgumentException ignored) {
        }
        return switch (entityType) {
            case SKELETON, STRAY, PILLAGER -> DungeonMobRole.ARCHER;
            case CAVE_SPIDER, SPIDER, BREEZE -> DungeonMobRole.ASSASSIN;
            case WITCH, EVOKER, BLAZE -> DungeonMobRole.MAGE;
            case RAVAGER, IRON_GOLEM, VINDICATOR, PIGLIN_BRUTE -> DungeonMobRole.BRUTE;
            default -> DungeonMobRole.GRUNT;
        };
    }

    private RoleBalance roleBalance(DungeonMobRole role, boolean elite) {
        if (elite || role == DungeonMobRole.ELITE) return new RoleBalance(2.0D, 1.5D, 1.4D, 0.65D);
        return switch (role) {
            case GUARD -> new RoleBalance(1.3D, 0.85D, 1.5D, 0.45D);
            case ARCHER -> new RoleBalance(0.75D, 1.15D, 0.7D, 0.10D);
            case ASSASSIN -> new RoleBalance(0.8D, 1.35D, 0.6D, 0.10D);
            case MAGE -> new RoleBalance(0.85D, 1.25D, 0.8D, 0.10D);
            case BRUTE -> new RoleBalance(1.7D, 1.25D, 1.2D, 0.55D);
            case HEALER -> new RoleBalance(0.9D, 0.6D, 0.8D, 0.10D);
            default -> new RoleBalance(1.0D, 1.0D, 1.0D, 0.10D);
        };
    }

    private void applyDungeonMobMovement(LivingEntity living, DungeonMobRole role, boolean elite, double knockbackResistance) {
        setKnockbackResistance(living, knockbackResistance);
        AttributeInstance movement = living.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement == null) return;
        double speed = switch (role) {
            case ASSASSIN -> 0.34D;
            case BRUTE, ELITE -> 0.20D;
            case GUARD -> 0.22D;
            case ARCHER -> 0.27D;
            case MAGE, HEALER -> 0.25D;
            default -> 0.26D;
        };
        movement.setBaseValue(Math.clamp(speed + (elite ? 0.02D : 0.0D), 0.05D, 0.45D));
    }

    private void setKnockbackResistance(LivingEntity living, double resistance) {
        AttributeInstance attribute = living.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (attribute != null) attribute.setBaseValue(Math.clamp(resistance, 0.0D, 1.0D));
    }

    private void updateMobName(LivingEntity living) {
        if (isEngineMob(living)) return;
        String zone = living.getPersistentDataContainer().get(zoneKey, PersistentDataType.STRING);
        String spawnId = living.getPersistentDataContainer().get(spawnKey, PersistentDataType.STRING);
        Integer level = living.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        if (zone == null || spawnId == null || level == null) return;
        boolean boss = living.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING);
        String configuredName = boss
                ? dataManager.getData().getString(path(zone) + ".boss-spawns." + spawnId + ".name", "")
                : dataManager.getData().getString(path(zone) + ".spawns." + spawnId + ".name", dataManager.getData().getString(path(zone) + ".mob-name", ""));
        String base = configuredName == null || configuredName.isBlank()
                ? (boss ? "&c" + pretty(zone) + " Boss" : "&c" + pretty(zone) + " Monster")
                : configuredName;
        double displayCurrent = boss ? bossDisplayCurrentHealth(living) : Math.max(0.0D, living.getHealth());
        double displayMax = boss ? bossDisplayMaxHealth(living) : living.getMaxHealth();
        living.setCustomName(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("zones.mob-name-format", "&8[&7Lv.%level%&8] &c%name% &a%health%&7/&a%max_health%&c❤")
                .replace("%level%", String.valueOf(level))
                .replace("%name%", base)
                .replace("%health%", String.valueOf((long) Math.ceil(displayCurrent)))
                .replace("%max_health%", String.valueOf((long) Math.ceil(displayMax)))));
        living.setCustomNameVisible(true);
    }

    private DungeonMobData readDungeonMobData(LivingEntity living) {
        if (isEngineMob(living)) return null;
        if (!isZoneDungeonMob(living)) return null;
        boolean boss = isZoneBoss(living);
        double maxHealth = boss ? bossDisplayMaxHealth(living) : Math.max(1.0D, living.getPersistentDataContainer().getOrDefault(
                bossMaxHealthKey, PersistentDataType.DOUBLE, living.getMaxHealth()));
        double currentHealth = boss ? Math.clamp(bossDisplayCurrentHealth(living), 0.0D, maxHealth) : Math.clamp(
                living.getPersistentDataContainer().getOrDefault(mobHealthKey, PersistentDataType.DOUBLE,
                        Math.min(living.getHealth(), maxHealth)), 0.0D, maxHealth);
        double defense = Math.max(0.0D, living.getPersistentDataContainer().getOrDefault(
                bossDefenseKey, PersistentDataType.DOUBLE, 0.0D));
        DungeonMobRole role = parseDungeonMobRole(living.getPersistentDataContainer().get(mobRoleKey, PersistentDataType.STRING), living.getType());
        boolean elite = boss || living.getPersistentDataContainer().has(mobEliteKey, PersistentDataType.BYTE);
        return new DungeonMobData(currentHealth, maxHealth, defense, boss, role, elite);
    }

    private boolean isEngineMob(org.bukkit.entity.Entity entity) {
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

    private static final class DungeonMobData {
        private double currentHealth;
        private final double maxHealth;
        private final double defense;
        private final boolean boss;
        private final DungeonMobRole role;
        private final boolean elite;
        private final Map<String, Long> cooldowns = new HashMap<>();
        private long guardUntil;
        private long lastBasicAttack;
        private long lastDamagedAt;

        private DungeonMobData(double currentHealth, double maxHealth, double defense, boolean boss, DungeonMobRole role, boolean elite) {
            this.currentHealth = currentHealth;
            this.maxHealth = maxHealth;
            this.defense = defense;
            this.boss = boss;
            this.role = role == null ? DungeonMobRole.GRUNT : role;
            this.elite = elite;
        }
    }

    private enum DungeonMobRole {
        GRUNT, GUARD, ARCHER, ASSASSIN, MAGE, BRUTE, HEALER, ELITE
    }

    private enum DungeonRunState {
        WAITING, ACTIVE, BOSS_ALIVE, COMPLETED, RESETTING
    }

    private record RoleBalance(double healthMultiplier, double damageMultiplier, double defenseMultiplier,
                               double knockbackResistance) {
    }

    private String baseMobName(String zone, String spawnId, int level) {
        String name = dataManager.getData().getString(path(zone) + ".spawns." + spawnId + ".name",
                dataManager.getData().getString(path(zone) + ".mob-name", "&c" + pretty(zone) + " Monster"));
        return CC.color("&8[&cLv." + level + "&8] " + name);
    }

    private Location getTrackedSpawn(LivingEntity living, String zone) {
        String spawnId = living.getPersistentDataContainer().get(spawnKey, PersistentDataType.STRING);
        if (spawnId == null) return center(zone);
        String section = living.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING) ? ".boss-spawns." : ".spawns.";
        Location location = loadLocation(path(zone) + section + spawnId + ".location");
        return location == null ? center(zone) : location;
    }

    private Material placeholder(String type) {
        String material = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("zones.regeneration." + type + "-placeholder", type.equals("mining") ? "BEDROCK" : "AIR");
        Material parsed = Material.matchMaterial(material);
        return parsed == null ? Material.AIR : parsed;
    }

    private EntityType parseType(String type) {
        try {
            return EntityType.valueOf(String.valueOf(type).trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            return EntityType.ZOMBIE;
        }
    }

    private EntityType resolveDungeonEntityType(String zone, String spawnPath, String rawType) {
        DungeonMobTemplateRegistry.DungeonMobTemplate template = DungeonMobTemplateRegistry.resolve(rawType);
        if (template == null) {
            plugin.getLogger().warning("[SMP] Unknown dungeon mob alias '" + rawType + "' for dungeon '" + zone + "'. Use /dungeon mob aliases.");
            throw new IllegalArgumentException("Unknown dungeon mob alias " + rawType + " at " + spawnPath);
        }
        String storedName = dataManager.getData().getString(spawnPath + ".mob_display_name",
                dataManager.getData().getString(spawnPath + ".name", ""));
        String displayName = DungeonMobTemplateRegistry.cleanDisplayName(storedName, template);
        boolean dirty = !template.mobId().equalsIgnoreCase(dataManager.getData().getString(spawnPath + ".mob_id", ""))
                || !displayName.equals(storedName)
                || !template.entityType().name().equalsIgnoreCase(dataManager.getData().getString(spawnPath + ".entity_type", ""))
                || !template.entityType().name().equalsIgnoreCase(dataManager.getData().getString(spawnPath + ".type", ""));
        if (dirty) {
            dataManager.getData().set(spawnPath + ".mob_id", template.mobId());
            dataManager.getData().set(spawnPath + ".mob_display_name", displayName);
            dataManager.getData().set(spawnPath + ".entity_type", template.entityType().name());
            dataManager.getData().set(spawnPath + ".type", template.entityType().name());
            dataManager.getData().set(spawnPath + ".name", displayName);
            dataManager.getData().set(spawnPath + ".role", template.role());
            dataManager.save();
        }
        return template.entityType();
    }

    private EntityType dungeonRoleAlias(String normalized) {
        return switch (normalized) {
            case "MINION", "MINIONS" -> EntityType.ZOMBIE;
            case "GUARD", "GUARDS" -> EntityType.HUSK;
            case "ARCHER", "ARCHERS" -> EntityType.SKELETON;
            case "SCOUT", "SCOUTS" -> EntityType.PILLAGER;
            case "OFFICER", "CAPTAIN" -> EntityType.VINDICATOR;
            default -> null;
        };
    }

    private void migrateDungeonRoleSpawn(String zone, String spawnPath, String rawType, EntityType entityType) {
        String role = roleName(rawType);
        dataManager.getData().set(spawnPath + ".role", role);
        dataManager.getData().set(spawnPath + ".type", entityType.name());
        if (!dataManager.getData().isString(spawnPath + ".name")) {
            dataManager.getData().set(spawnPath + ".name", defaultRoleDisplayName(zone, role));
        }
        dataManager.save();
    }

    private String normalizeType(String rawType) {
        if (rawType == null || rawType.isBlank()) return "ZOMBIE";
        return rawType.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private String roleName(String rawType) {
        String normalized = normalizeType(rawType).toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "mob" : normalized;
    }

    private String defaultRoleDisplayName(String zone, String role) {
        String singular = singularRole(role);
        if (zone != null && zone.toLowerCase(Locale.ROOT).contains("imperial")) {
            return "&cImperial " + pretty(singular);
        }
        return "&c" + pretty(singular);
    }

    private String singularRole(String role) {
        return switch (role.toLowerCase(Locale.ROOT)) {
            case "minions" -> "minion";
            case "guards" -> "guard";
            case "archers" -> "archer";
            case "scouts" -> "scout";
            default -> role;
        };
    }

    private void logBossSpawn(String zone, String spawnId, Entity entity, int level, double configuredHealth, LivingEntity living) {
        double finalMaxHealth = bossDisplayMaxHealth(living);
        double currentHealth = bossDisplayCurrentHealth(living);
        plugin.getLogger().info("[ZoneManager] Spawned boss " + spawnId
                + " in " + zone
                + " type=" + entity.getType().name()
                + " level=" + level
                + " configuredHealth=" + (long) Math.ceil(configuredHealth)
                + " finalMaxHealth=" + (long) Math.ceil(finalMaxHealth)
                + " currentHealth=" + (long) Math.ceil(currentHealth)
                + " bukkitMaxHealth=" + (long) Math.ceil(living.getMaxHealth())
                + " displayHealth=" + (long) Math.ceil(currentHealth)
                + " customName=" + (living.getCustomName() == null ? "" : living.getCustomName())
                + " isBoss=" + isZoneBoss(living));
    }

    private String discoveryPath(Player player, String zone) {
        return "players." + player.getUniqueId() + ".zones.discovered." + zone.toLowerCase(Locale.ROOT);
    }

    private long discoveredAt(Player player, String zone) {
        return dataManager.getData().getLong(discoveryPath(player, zone) + ".discovered-at", 0L);
    }

    private String zoneDisplayName(String zone) {
        return dataManager.getData().getString(path(zone) + ".display", pretty(zone));
    }

    private Material zoneIcon(String zone) {
        String configured = dataManager.getData().getString(path(zone) + ".icon", "");
        if (configured != null && !configured.isBlank()) {
            try {
                return Material.valueOf(configured.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return switch (dataManager.getData().getString(path(zone) + ".type", "safe").toLowerCase(Locale.ROOT)) {
            case "mining" -> Material.IRON_PICKAXE;
            case "farming" -> Material.WHEAT;
            case "fishing" -> Material.FISHING_ROD;
            case "combat", "dungeon" -> Material.IRON_SWORD;
            case "pvp" -> Material.NETHERITE_SWORD;
            default -> Material.MAP;
        };
    }

    private Location getZoneWarp(String zone) {
        String base = path(zone) + ".warp";
        if (!dataManager.getData().getBoolean(base + ".enabled", false)) return null;
        return loadLocation(base);
    }

    private boolean hasTravelRequirements(String zone) {
        String base = path(zone) + ".warp.requirements";
        ConfigurationSection section = dataManager.getData().getConfigurationSection(base);
        return section != null && !section.getKeys(false).isEmpty();
    }

    private boolean isTravelUnlocked(Player player, String zone) {
        if (!isDiscovered(player, zone)) return false;
        if (player.hasPermission("fakevival.zones.admin") || player.hasPermission("fakevival.admin.zone")) return true;
        if (dataManager.getData().getBoolean(discoveryPath(player, zone) + ".travel-unlocked", false)) return true;
        return requirementsMet(player, zone) && travelUnlockCost(zone) <= 0.0D;
    }

    private boolean unlockTravelIfEligible(Player player, String zone) {
        if (!requirementsMet(player, zone)) return false;
        double cost = travelUnlockCost(zone);
        if (cost > 0.0D && !economyManager.takeCoins(player.getUniqueId(), cost, "zone-travel-unlock:" + zone)) return false;
        dataManager.getData().set(discoveryPath(player, zone) + ".travel-unlocked", true);
        dataManager.getData().set(discoveryPath(player, zone) + ".travel-unlocked-at", System.currentTimeMillis());
        dataManager.save();
        if (cost > 0.0D) player.sendMessage(CC.color("&aUnlocked zone travel for &e" + economyManager.format(cost) + " coins&a."));
        return true;
    }

    private boolean requirementsMet(Player player, String zone) {
        String base = path(zone) + ".warp.requirements";
        if (plugin instanceof club.abelix.games.fakevival.Game game) {
            int playerLevel = Math.max(0, dataManager.getData().getInt(base + ".player-level", 0));
            if (playerLevel > 0 && game.getPlayerLevelManager().getLevel(player) < playerLevel) return false;
            String skill = dataManager.getData().getString(base + ".skill", "");
            int skillLevel = Math.max(0, dataManager.getData().getInt(base + ".skill-level", 0));
            if (!skill.isBlank() && skillLevel > 0 && game.getSkillManager().getLevel(player, skill) < skillLevel) return false;
        }
        String quest = dataManager.getData().getString(base + ".quest", "");
        if (!quest.isBlank() && !dataManager.getData().getBoolean("players." + player.getUniqueId() + ".quests." + quest + ".completed", false)) return false;
        String permission = dataManager.getData().getString(base + ".permission", "");
        return permission.isBlank() || player.hasPermission(permission);
    }

    private double travelUnlockCost(String zone) {
        return Math.max(0.0D, dataManager.getData().getDouble(path(zone) + ".warp.requirements.coins", 0.0D));
    }

    private List<String> travelRequirementLines(Player player, String zone) {
        String base = path(zone) + ".warp.requirements";
        List<String> lines = new ArrayList<>();
        if (plugin instanceof club.abelix.games.fakevival.Game game) {
            int required = Math.max(0, dataManager.getData().getInt(base + ".player-level", 0));
            if (required > 0) lines.add(requirementLine(game.getPlayerLevelManager().getLevel(player) >= required, "PlayerLevel " + required));
            String skill = dataManager.getData().getString(base + ".skill", "");
            int skillLevel = Math.max(0, dataManager.getData().getInt(base + ".skill-level", 0));
            if (!skill.isBlank() && skillLevel > 0) lines.add(requirementLine(game.getSkillManager().getLevel(player, skill) >= skillLevel, pretty(skill) + " " + skillLevel));
        }
        String quest = dataManager.getData().getString(base + ".quest", "");
        if (!quest.isBlank()) lines.add(requirementLine(dataManager.getData().getBoolean("players." + player.getUniqueId() + ".quests." + quest + ".completed", false), "Quest: " + pretty(quest)));
        String permission = dataManager.getData().getString(base + ".permission", "");
        if (!permission.isBlank()) lines.add(requirementLine(player.hasPermission(permission), "Permission: " + permission));
        double cost = travelUnlockCost(zone);
        if (cost > 0.0D && !dataManager.getData().getBoolean(discoveryPath(player, zone) + ".travel-unlocked", false)) {
            lines.add("&eOne-time unlock: &6" + economyManager.format(cost) + " coins");
        }
        if (lines.isEmpty()) lines.add("&aUnlocked by discovering this zone.");
        return List.copyOf(lines);
    }

    private String requirementLine(boolean met, String text) {
        return (met ? "&a✔ &7" : "&cX &7") + text;
    }

    private String zoneMobSummary(String zone) {
        ConfigurationSection section = dataManager.getData().getConfigurationSection(path(zone) + ".spawns");
        if (section == null || section.getKeys(false).isEmpty()) return "None";
        LinkedHashSet<String> mobs = new LinkedHashSet<>();
        for (String key : section.getKeys(false)) {
            String role = section.getString(key + ".role", "");
            mobs.add(role.isBlank() ? pretty(section.getString(key + ".type", "mob")) : pretty(role));
        }
        return String.join(", ", mobs);
    }

    private String zoneBossSummary(String zone) {
        ConfigurationSection section = dataManager.getData().getConfigurationSection(path(zone) + ".boss-spawns");
        if (section == null || section.getKeys(false).isEmpty()) return "None";
        LinkedHashSet<String> bosses = new LinkedHashSet<>();
        for (String key : section.getKeys(false)) bosses.add(ChatColor.stripColor(CC.color(section.getString(key + ".name", pretty(key)))));
        return String.join(", ", bosses);
    }

    private boolean isPlayerInBossFight(Player player) {
        UUID playerId = player.getUniqueId();
        for (Map.Entry<UUID, UUID> entry : bossTargets.entrySet()) {
            if (!playerId.equals(entry.getValue())) continue;
            Entity boss = Bukkit.getEntity(entry.getKey());
            if (boss != null && boss.isValid() && !boss.isDead()) return true;
        }
        return false;
    }

    private Location findSafeWarp(Location origin) {
        World world = origin.getWorld();
        if (world == null) return null;
        for (int radius = 0; radius <= 3; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) continue;
                    for (int yOffset : new int[]{0, 1, -1, 2, -2, 3, -3}) {
                        int y = origin.getBlockY() + yOffset;
                        if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) continue;
                        Location candidate = new Location(world, origin.getBlockX() + x + 0.5D, y,
                                origin.getBlockZ() + z + 0.5D, origin.getYaw(), origin.getPitch());
                        if (isSafeWarp(candidate)) return candidate;
                    }
                }
            }
        }
        return null;
    }

    private boolean isSafeWarp(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block floor = feet.getRelative(0, -1, 0);
        if (!feet.isPassable() || !head.isPassable() || feet.isLiquid() || head.isLiquid() || !floor.getType().isSolid()) return false;
        return !Set.of(Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.MAGMA_BLOCK,
                Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.CACTUS, Material.POWDER_SNOW).contains(floor.getType());
    }

    private String path(String name) {
        return "map-zones." + name.toLowerCase();
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
        if (world == null) return null;
        return new Location(world,
                dataManager.getData().getDouble(path + ".x"),
                dataManager.getData().getDouble(path + ".y"),
                dataManager.getData().getDouble(path + ".z"),
                (float) dataManager.getData().getDouble(path + ".yaw"),
                (float) dataManager.getData().getDouble(path + ".pitch"));
    }

    private ConfigurationSection ensureDataSection(String path) {
        ConfigurationSection current = dataManager.getData();
        for (String part : path.split("\\.")) {
            ConfigurationSection next = current.getConfigurationSection(part);
            current = next != null ? next : current.createSection(part);
        }
        return current;
    }

    private void copySection(ConfigurationSection from, ConfigurationSection to) {
        if (from == null || to == null) return;
        for (String key : from.getKeys(false)) {
            Object value = from.get(key);
            if (value instanceof ConfigurationSection section) {
                copySection(section, to.createSection(key));
            } else {
                to.set(key, value);
            }
        }
    }

    private String pretty(String text) {
        if (text == null || text.isEmpty()) return "Zone";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase().replace('_', ' ');
    }

    private String format(Location location) {
        if (location == null || location.getWorld() == null) return "unset";
        return location.getWorld().getName() + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private String blockKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
