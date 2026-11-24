package club.abelix.games.fakevival.mobengine.mob;

import club.abelix.games.fakevival.mobengine.api.SMPMobAPI;
import club.abelix.games.fakevival.mobengine.profile.SMPMobPerformanceProfiler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Queue;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class SMPMobManager implements SMPMobAPI {
    private final Plugin plugin;
    private final NamespacedKey engineMarkerKey;
    private final SMPMobEngine engine;
    private final SMPMobTickScheduler tickScheduler;
    private final Map<UUID, SMPMob> mobs = new ConcurrentHashMap<>();
    private final Queue<QueuedSpawn> spawnQueue = new ConcurrentLinkedQueue<>();
    private BukkitTask task;

    public SMPMobManager(Plugin plugin, SMPMobPerformanceProfiler profiler) {
        this.plugin = plugin;
        NamespacedKey legacyMarkerKey = new NamespacedKey(plugin, "smp_mob_id");
        this.engineMarkerKey = new NamespacedKey(plugin, "smpmobengine");
        NamespacedKey mobIdKey = new NamespacedKey(plugin, "smpmob_id");
        NamespacedKey mobUuidKey = new NamespacedKey(plugin, "smpmob_uuid");
        this.engine = new SMPMobEngine(plugin, this, profiler, legacyMarkerKey, engineMarkerKey, mobIdKey, mobUuidKey);
        this.tickScheduler = new SMPMobTickScheduler(this, engine.brain(), profiler);
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(engine.damageController(), plugin);
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, tickScheduler::tick, 1L, 1L);
    }

    public SMPMobEngine engine() {
        return engine;
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        clearAll();
    }

    @Override
    public boolean isSMPMob(Entity entity) {
        return entity != null && (mobs.containsKey(entity.getUniqueId())
                || entity.getPersistentDataContainer().has(engineMarkerKey, PersistentDataType.BYTE));
    }

    @Override
    public Optional<SMPMob> getSMPMob(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mobs.get(entity.getUniqueId()));
    }

    @Override
    public void spawnSMPMob(String mobId, Location location) {
        queueSpawn(mobId, location);
    }

    public SMPMob spawnMob(String mobId, Location location) {
        SMPMob mob = engine.spawner().spawn(mobId, location);
        mob.setDebugMob(true);
        mobs.put(mob.getUuid(), mob);
        return mob;
    }

    public LivingEntity spawnMob(String mobId, Location location, EntityType type, String displayName,
                                double maxMmoHealth, double attackDamage, int level, java.util.List<ItemStack> drops) {
        return spawnMob(mobId, location, type, displayName, maxMmoHealth, attackDamage, level, drops, false).getEntity();
    }

    public SMPMob spawnMob(String mobId, Location location, EntityType type, String displayName,
                           double maxMmoHealth, double attackDamage, int level, java.util.List<ItemStack> drops,
                           boolean externalDropController) {
        return spawnMobData(mobId, location, type, displayName, maxMmoHealth, attackDamage, level, drops, externalDropController, false);
    }

    SMPMob spawnMobData(String mobId, Location location, EntityType type, String displayName,
                        double maxMmoHealth, double attackDamage, int level, java.util.List<ItemStack> drops) {
        return spawnMobData(mobId, location, type, displayName, maxMmoHealth, attackDamage, level, drops, false, false);
    }

    SMPMob spawnMobData(String mobId, Location location, EntityType type, String displayName,
                        double maxMmoHealth, double attackDamage, int level, java.util.List<ItemStack> drops,
                        boolean externalDropController, boolean debugMob) {
        SMPMob mob = engine.spawner().spawn(mobId, location, type, displayName, maxMmoHealth, attackDamage, level, drops);
        mob.setExternalDropController(externalDropController);
        mob.setDebugMob(debugMob);
        mobs.put(mob.getUuid(), mob);
        return mob;
    }

    public void queueSpawn(String mobId, Location location) {
        spawnQueue.add(new QueuedSpawn(mobId, location.clone()));
    }

    void processSpawnQueue() {
        for (int i = 0; i < SMPMobEngineSettings.MAX_COMMAND_SPAWNS_PER_TICK; i++) {
            QueuedSpawn spawn = spawnQueue.poll();
            if (spawn == null) return;
            Location loc = spread(spawn.location(), i);
            spawnMob(spawn.mobId(), loc);
        }
    }

    int spawnQueueSize() {
        return spawnQueue.size();
    }

    @Override
    public void removeSMPMob(UUID uuid) {
        SMPMob mob = mobs.remove(uuid);
        if (mob != null && mob.isValid()) {
            mob.destroyBossBar();
            mob.getEntity().remove();
        }
    }

    void forgetSMPMob(UUID uuid) {
        mobs.remove(uuid);
    }

    public void clearInvalid() {
        mobs.entrySet().removeIf(entry -> {
            boolean invalid = !entry.getValue().isValid();
            if (invalid) {
                entry.getValue().destroyBossBar();
            }
            return invalid;
        });
    }

    public void clearAll() {
        for (SMPMob mob : new ArrayList<>(mobs.values())) {
            mob.destroyBossBar();
            if (mob.isValid()) {
                mob.getEntity().remove();
            }
        }
        mobs.clear();
    }

    public int count() {
        return mobs.size();
    }

    public Collection<SMPMob> getMobsSnapshot() {
        return mobs.values().stream()
                .filter(SMPMob::isValid)
                .sorted(Comparator.comparing(SMPMob::getUuid))
                .toList();
    }

    private Location spread(Location location, int index) {
        double angle = index * 2.399963229728653D;
        double radius = 0.35D + (index % SMPMobEngineSettings.MAX_COMMAND_SPAWNS_PER_TICK) * 0.12D;
        return location.clone().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
    }

    private record QueuedSpawn(String mobId, Location location) {
    }
}
