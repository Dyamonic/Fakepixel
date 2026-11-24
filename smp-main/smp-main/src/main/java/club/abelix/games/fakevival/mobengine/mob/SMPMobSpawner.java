package club.abelix.games.fakevival.mobengine.mob;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

public final class SMPMobSpawner {
    private static final double SAFE_ENTITY_HEALTH = 20.0D;
    private final NamespacedKey legacyMarkerKey;
    private final NamespacedKey engineMarkerKey;
    private final NamespacedKey mobIdKey;
    private final NamespacedKey mobUuidKey;

    public SMPMobSpawner(NamespacedKey legacyMarkerKey, NamespacedKey engineMarkerKey,
                         NamespacedKey mobIdKey, NamespacedKey mobUuidKey) {
        this.legacyMarkerKey = legacyMarkerKey;
        this.engineMarkerKey = engineMarkerKey;
        this.mobIdKey = mobIdKey;
        this.mobUuidKey = mobUuidKey;
    }

    public SMPMob spawn(String mobId, Location location) {
        return spawn(mobId, location, resolveType(mobId), null, defaultHealth(mobId), defaultDamage(mobId), 1, List.of());
    }

    public SMPMob spawn(String mobId, Location location, EntityType type, String displayName, double maxMmoHealth,
                        double attackDamage, int level, List<ItemStack> drops) {
        World world = location.getWorld();
        if (world == null) throw new IllegalArgumentException("Location world cannot be null");
        Entity spawned = world.spawnEntity(location, type == null ? resolveType(mobId) : type);
        if (!(spawned instanceof LivingEntity living)) {
            spawned.remove();
            throw new IllegalArgumentException("SMP mob backing entity must be living");
        }
        prepareEntity(living);
        if (living instanceof Ageable ageable) ageable.setAdult();
        if (living instanceof Slime slime && level > 1) slime.setSize(Math.min(4, 1 + level / 12));
        return new SMPMob(mobId, living, maxMmoHealth, attackDamage, displayName, level, drops,
                legacyMarkerKey, engineMarkerKey, mobIdKey, mobUuidKey);
    }

    private void prepareEntity(LivingEntity entity) {
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(SAFE_ENTITY_HEALTH);
        entity.setHealth(Math.min(entity.getMaxHealth(), SAFE_ENTITY_HEALTH));
        entity.setAI(true);
        if (entity instanceof Mob mob) mob.setAware(true);
        entity.setRemoveWhenFarAway(false);
        entity.setCanPickupItems(false);
        entity.setCustomNameVisible(true);
        entity.setCollidable(true);
    }

    private EntityType resolveType(String mobId) {
        String id = mobId.toLowerCase(Locale.ROOT);
        if (id.contains("skeleton")) return EntityType.SKELETON;
        if (id.contains("spider")) return EntityType.SPIDER;
        if (id.contains("enderman")) return EntityType.ENDERMAN;
        if (id.contains("drowned")) return EntityType.DROWNED;
        if (id.contains("slime")) return EntityType.SLIME;
        if (id.contains("guardian")) return EntityType.GUARDIAN;
        return EntityType.ZOMBIE;
    }

    private double defaultHealth(String mobId) {
        String id = mobId.toLowerCase(Locale.ROOT);
        if (id.contains("boss")) return 50_000.0D;
        if (id.contains("elite")) return 5_000.0D;
        return 500.0D;
    }

    private double defaultDamage(String mobId) {
        String id = mobId.toLowerCase(Locale.ROOT);
        if (id.contains("boss")) return 18.0D;
        if (id.contains("elite")) return 9.0D;
        return 4.0D;
    }
}
