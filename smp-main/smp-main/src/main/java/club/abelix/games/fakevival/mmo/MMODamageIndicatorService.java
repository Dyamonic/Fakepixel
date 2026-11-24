package club.abelix.games.fakevival.mmo;

import club.abelix.games.fakevival.util.CC;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class MMODamageIndicatorService implements Listener {
    private static final long TARGET_THROTTLE_MS = 150L;
    private static final int MAX_ACTIVE_PER_TARGET = 3;
    private static final long DAMAGE_LIFETIME_TICKS = 22L;
    private static final long HEALING_LIFETIME_TICKS = 20L;

    private final JavaPlugin plugin;
    private final Set<Entity> active = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<Entity>> activeByTarget = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastShown = new ConcurrentHashMap<>();
    private final Map<UUID, PendingDamage> pendingDamage = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> pendingTasks = new ConcurrentHashMap<>();
    private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.US);

    public MMODamageIndicatorService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public DisplayResult showDamage(LivingEntity target, MMODamageResult result) {
        if (target == null || result == null || result.finalDamage() <= 0.0D || !target.isValid() || target.isDead()) return DisplayResult.SKIPPED;
        if (shouldMerge(target)) {
            mergePending(target, result);
            return DisplayResult.RATE_LIMITED;
        }
        showNow(target, result);
        if (result.lifestealAmount() > 0.0D) {
            spawn(target, "&a+" + numbers.format(Math.round(result.lifestealAmount())) + " HP", HEALING_LIFETIME_TICKS);
        }
        return DisplayResult.SPAWNED;
    }

    public void showHealing(LivingEntity target, double amount) {
        if (target == null || amount <= 0.0D || !target.isValid() || target.isDead()) return;
        if (activeCount(target.getUniqueId()) >= MAX_ACTIVE_PER_TARGET || shouldMerge(target)) return;
        spawn(target, "&a+" + numbers.format(Math.round(amount)) + " HP", HEALING_LIFETIME_TICKS);
    }

    public void shutdown() {
        for (BukkitTask task : pendingTasks.values()) task.cancel();
        pendingTasks.clear();
        pendingDamage.clear();
        Iterator<Entity> iterator = active.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (entity != null && entity.isValid()) entity.remove();
            iterator.remove();
        }
        activeByTarget.clear();
        lastShown.clear();
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        cleanupTarget(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (!active.remove(entity)) continue;
            removeFromTargets(entity);
            if (entity.isValid()) entity.remove();
        }
    }

    private boolean shouldMerge(LivingEntity target) {
        long now = System.currentTimeMillis();
        Long previous = lastShown.get(target.getUniqueId());
        return activeCount(target.getUniqueId()) >= MAX_ACTIVE_PER_TARGET
                || previous != null && now - previous < TARGET_THROTTLE_MS;
    }

    private void mergePending(LivingEntity target, MMODamageResult result) {
        UUID targetId = target.getUniqueId();
        pendingDamage.compute(targetId, (ignored, pending) -> pending == null
                ? new PendingDamage(result.finalDamage(), result.lifestealAmount(), result.critical(), result.sourceType())
                : pending.merge(result));
        pendingTasks.computeIfAbsent(targetId, ignored -> plugin.getServer().getScheduler().runTaskLater(plugin, () -> flushPending(targetId), 4L));
    }

    private void flushPending(UUID targetId) {
        pendingTasks.remove(targetId);
        PendingDamage pending = pendingDamage.remove(targetId);
        if (pending == null || activeCount(targetId) >= MAX_ACTIVE_PER_TARGET) return;
        Entity entity = plugin.getServer().getEntity(targetId);
        if (!(entity instanceof LivingEntity target) || !target.isValid() || target.isDead()) return;
        showNow(target, new MMODamageResult(pending.damage, pending.critical, pending.sourceType, pending.lifesteal));
    }

    private void showNow(LivingEntity target, MMODamageResult result) {
        lastShown.put(target.getUniqueId(), System.currentTimeMillis());
        spawn(target, format(result), DAMAGE_LIFETIME_TICKS);
    }

    private void spawn(LivingEntity target, String rawText, long lifetimeTicks) {
        UUID targetId = target.getUniqueId();
        if (activeCount(targetId) >= MAX_ACTIVE_PER_TARGET) return;
        Location location = target.getLocation().clone().add(randomOffset()).add(0.0D, Math.max(1.0D, target.getHeight() + 0.25D), 0.0D);
        try {
            TextDisplay display = target.getWorld().spawn(location, TextDisplay.class, entity -> {
                entity.text(LegacyComponentSerializer.legacySection().deserialize(CC.color(rawText)));
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setSeeThrough(true);
                entity.setShadowed(false);
                entity.setPersistent(false);
                entity.setGravity(false);
                entity.setInvulnerable(true);
                entity.setInterpolationDuration(2);
                entity.setTeleportDuration(8);
            });
            track(targetId, display, lifetimeTicks);
        } catch (Throwable ignored) {
            ArmorStand stand = target.getWorld().spawn(location, ArmorStand.class, entity -> {
                entity.setInvisible(true);
                entity.setMarker(true);
                entity.setSmall(true);
                entity.setGravity(false);
                entity.setInvulnerable(true);
                entity.setPersistent(false);
                entity.customName(LegacyComponentSerializer.legacySection().deserialize(CC.color(rawText)));
                entity.setCustomNameVisible(true);
            });
            track(targetId, stand, lifetimeTicks);
        }
    }

    private void track(UUID targetId, Entity entity, long lifetimeTicks) {
        active.add(entity);
        activeByTarget.computeIfAbsent(targetId, ignored -> ConcurrentHashMap.newKeySet()).add(entity);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            active.remove(entity);
            Set<Entity> targetDisplays = activeByTarget.get(targetId);
            if (targetDisplays != null) {
                targetDisplays.remove(entity);
                if (targetDisplays.isEmpty()) activeByTarget.remove(targetId);
            }
            if (entity.isValid()) entity.remove();
        }, Math.max(1L, lifetimeTicks));
    }

    private int activeCount(UUID targetId) {
        Set<Entity> targetDisplays = activeByTarget.get(targetId);
        return targetDisplays == null ? 0 : targetDisplays.size();
    }

    private void cleanupTarget(UUID targetId) {
        BukkitTask task = pendingTasks.remove(targetId);
        if (task != null) task.cancel();
        pendingDamage.remove(targetId);
        lastShown.remove(targetId);
        Set<Entity> displays = activeByTarget.remove(targetId);
        if (displays == null) return;
        for (Entity display : displays) {
            active.remove(display);
            if (display.isValid()) display.remove();
        }
    }

    private void removeFromTargets(Entity entity) {
        for (Iterator<Map.Entry<UUID, Set<Entity>>> iterator = activeByTarget.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, Set<Entity>> entry = iterator.next();
            entry.getValue().remove(entity);
            if (entry.getValue().isEmpty()) iterator.remove();
        }
    }

    private String format(MMODamageResult result) {
        String damage = numbers.format(Math.round(result.finalDamage()));
        if (result.sourceType() == MMODamageSourceType.ABILITY) return "&b-" + damage;
        if (result.sourceType() == MMODamageSourceType.DOT) return "&c-" + damage;
        if (result.critical() || result.sourceType() == MMODamageSourceType.CRITICAL) {
            return result.finalDamage() >= 50000.0D ? "&f*&6" + damage + "&f*" : "&f*&e" + damage + "&f*";
        }
        return "&7-" + damage;
    }

    private Vector randomOffset() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new Vector(random.nextDouble(-0.35D, 0.35D), random.nextDouble(0.0D, 0.25D), random.nextDouble(-0.35D, 0.35D));
    }

    private static final class PendingDamage {
        private double damage;
        private double lifesteal;
        private boolean critical;
        private MMODamageSourceType sourceType;

        private PendingDamage(double damage, double lifesteal, boolean critical, MMODamageSourceType sourceType) {
            this.damage = damage;
            this.lifesteal = lifesteal;
            this.critical = critical;
            this.sourceType = sourceType;
        }

        private PendingDamage merge(MMODamageResult result) {
            damage += result.finalDamage();
            lifesteal += result.lifestealAmount();
            critical = critical || result.critical();
            if (result.sourceType() == MMODamageSourceType.ABILITY || sourceType == null) sourceType = result.sourceType();
            else if (result.sourceType() == MMODamageSourceType.CRITICAL) sourceType = MMODamageSourceType.CRITICAL;
            return this;
        }
    }

    public enum DisplayResult {
        SPAWNED(true, false),
        RATE_LIMITED(false, true),
        SKIPPED(false, false);

        private final boolean spawned;
        private final boolean rateLimited;

        DisplayResult(boolean spawned, boolean rateLimited) {
            this.spawned = spawned;
            this.rateLimited = rateLimited;
        }

        public boolean spawned() { return spawned; }
        public boolean rateLimited() { return rateLimited; }
    }
}
