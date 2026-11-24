package club.abelix.games.fakevival.mobengine.mob;

import club.abelix.games.fakevival.mobengine.profile.SMPMobPerformanceProfiler;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class SMPMobEngine {
    private final SMPMobDropController dropController = new SMPMobDropController();
    private final SMPMobManager manager;
    private final SMPMobBrain brain;
    private final SMPMobDamageController damageController;
    private final SMPMobSpawner spawner;
    private final SMPMobClientLoadController clientLoadController;

    public SMPMobEngine(Plugin plugin, SMPMobManager manager, SMPMobPerformanceProfiler profiler,
                        NamespacedKey legacyMarkerKey, NamespacedKey engineMarkerKey,
                        NamespacedKey mobIdKey, NamespacedKey mobUuidKey) {
        this.manager = manager;
        SMPMobActivationRange activationRange = new SMPMobActivationRange();
        SMPMobPathfinder pathfinder = new SMPMobPathfinder();
        SMPMobCombatController combatController = new SMPMobCombatController();
        SMPMobVirtualizer virtualizer = new SMPMobVirtualizer();
        this.clientLoadController = new SMPMobClientLoadController(plugin, profiler);
        this.brain = new SMPMobBrain(activationRange, pathfinder, combatController, virtualizer);
        this.damageController = new SMPMobDamageController(plugin, manager, dropController);
        this.spawner = new SMPMobSpawner(legacyMarkerKey, engineMarkerKey, mobIdKey, mobUuidKey);
    }

    public SMPMobBrain brain() {
        return brain;
    }

    public SMPMobDamageController damageController() {
        return damageController;
    }

    public SMPMobSpawner spawner() {
        return spawner;
    }

    public SMPMobDropController dropController() {
        return dropController;
    }

    public SMPMobClientLoadController clientLoadController() {
        return clientLoadController;
    }

    public boolean isEngineMob(Entity entity) {
        return manager.isSMPMob(entity);
    }

    public double getCurrentHp(LivingEntity entity) {
        return manager.getSMPMob(entity).map(SMPMob::getMmoHealth)
                .orElseGet(() -> entity == null ? 0.0D : Math.max(0.0D, entity.getHealth()));
    }

    public double getMaxHp(LivingEntity entity) {
        return manager.getSMPMob(entity).map(SMPMob::getMaxMmoHealth)
                .orElseGet(() -> entity == null ? 0.0D : Math.max(1.0D, entity.getMaxHealth()));
    }

    public void setCurrentHp(LivingEntity entity, double hp) {
        manager.getSMPMob(entity).ifPresent(mob -> mob.setMmoHealth(hp));
    }

    public void damageMob(LivingEntity entity, double damage, Entity damager) {
        manager.getSMPMob(entity).ifPresent(mob -> mob.damageMmo(damage));
    }

    public void removeMob(Entity entity) {
        if (entity != null) manager.removeSMPMob(entity.getUniqueId());
    }

    public LivingEntity spawnMob(String mobId, Location location, double maxHp) {
        SMPMob mob = manager.spawnMobData(mobId, location, null, null, maxHp, 4.0D, 1, List.of(), false, true);
        return mob.getEntity();
    }

    public LivingEntity spawnMob(String mobId, Location location, EntityType type, String displayName,
                                double maxMmoHealth, double attackDamage, int level, List<ItemStack> drops) {
        SMPMob mob = manager.spawnMobData(mobId, location, type, displayName, maxMmoHealth, attackDamage, level, drops, false, false);
        return mob.getEntity();
    }

    public LivingEntity spawnMob(String mobId, Location location, EntityType type, String displayName,
                                double maxMmoHealth, double attackDamage, int level, List<ItemStack> drops,
                                boolean externalDropController) {
        SMPMob mob = manager.spawnMobData(mobId, location, type, displayName, maxMmoHealth, attackDamage, level, drops, externalDropController, false);
        return mob.getEntity();
    }
}
