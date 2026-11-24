package club.abelix.games.fakevival.mobengine.mob;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;

public final class SMPMobDamageController implements Listener {
    private static final double SAFE_ENTITY_HEALTH = 20.0;
    private static final double VISUAL_DAMAGE = 0.25;

    private final Plugin plugin;
    private final SMPMobManager manager;
    private final SMPMobDropController dropController;

    public SMPMobDamageController(Plugin plugin, SMPMobManager manager, SMPMobDropController dropController) {
        this.plugin = plugin;
        this.manager = manager;
        this.dropController = dropController;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        manager.getSMPMob(event.getEntity()).ifPresent(mob -> {
            mob.markCombat(System.currentTimeMillis());
            double incoming = resolveMmoDamage(event);
            double mmoDamage = reducedBossDamage(mob, incoming);
            mob.damageMmo(mmoDamage);
            writeBossHealth(mob);
            showRateLimitedHitEffect(mob, attackingPlayer(event));

            LivingEntity entity = mob.getEntity();
            if (mob.getMmoHealth() <= 0.0) {
                event.setDamage(Math.max(0.01D, entity.getHealth()));
                return;
            }

            event.setDamage(Math.min(event.getDamage(), VISUAL_DAMAGE));
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (mob.isValid()) {
                    entity.setHealth(safeVisualHealth(entity));
                    mob.updateNameplate();
                }
            });
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(EntityDeathEvent event) {
        manager.getSMPMob(event.getEntity()).ifPresent(mob -> {
            List<org.bukkit.inventory.ItemStack> drops = dropController.createDrops(mob);
            if (!mob.hasExternalDropController() || mob.hasCustomDrops() || mob.isDebugMob()) {
                event.getDrops().clear();
                event.getDrops().addAll(drops);
            }
            mob.destroyBossBar();
            manager.forgetSMPMob(mob.getUuid());
        });
    }

    private double resolveMmoDamage(EntityDamageByEntityEvent event) {
        double damage = event.getFinalDamage();
        if (event.getDamager() instanceof Player player) {
            damage += Math.max(0.0, player.getAttackCooldown() - 0.2) * 3.0;
        }
        return Math.max(1.0, damage);
    }

    private double reducedBossDamage(SMPMob mob, double incoming) {
        if (!mob.isRealBoss() || incoming <= 0.0D) return incoming;
        LivingEntity entity = mob.getEntity();
        double defense = pdcDouble(entity, "mmo_defense", pdcDouble(entity, "dungeon_defense", 0.0D));
        String type = pdcString(entity, "smp_entity_type");
        double cap = "DUNGEON_BOSS".equalsIgnoreCase(type) ? 600.0D : 250.0D;
        defense = Math.clamp(defense, 0.0D, cap);
        double reduced = incoming * (100.0D / (100.0D + defense));
        double minimum = Math.max(1.0D, incoming * 0.10D);
        return Math.max(reduced, minimum);
    }

    private void writeBossHealth(SMPMob mob) {
        if (!mob.isRealBoss()) return;
        LivingEntity entity = mob.getEntity();
        long now = System.currentTimeMillis();
        setDouble(entity, "boss_current_health", mob.getMmoHealth());
        setDouble(entity, "mmo_health", mob.getMmoHealth());
        setDouble(entity, "boss_max_health", mob.getMaxMmoHealth());
        setLong(entity, "dungeon_last_damaged", now);
    }

    private double pdcDouble(LivingEntity entity, String key, double fallback) { for (NamespacedKey value:entity.getPersistentDataContainer().getKeys()) if(value.getKey().equalsIgnoreCase(key)){Double found=entity.getPersistentDataContainer().get(value,PersistentDataType.DOUBLE);return found==null?fallback:found;} return fallback; }
    private String pdcString(LivingEntity entity, String key) { for (NamespacedKey value:entity.getPersistentDataContainer().getKeys()) if(value.getKey().equalsIgnoreCase(key))return entity.getPersistentDataContainer().get(value,PersistentDataType.STRING); return null; }
    private void setDouble(LivingEntity entity,String key,double value){for(NamespacedKey found:entity.getPersistentDataContainer().getKeys())if(found.getKey().equalsIgnoreCase(key)){entity.getPersistentDataContainer().set(found,PersistentDataType.DOUBLE,value);return;}}
    private void setLong(LivingEntity entity,String key,long value){for(NamespacedKey found:entity.getPersistentDataContainer().getKeys())if(found.getKey().equalsIgnoreCase(key)){entity.getPersistentDataContainer().set(found,PersistentDataType.LONG,value);return;}}

    private double safeVisualHealth(LivingEntity entity) {
        return Math.clamp(SAFE_ENTITY_HEALTH, 0.01D, Math.max(0.01D, entity.getMaxHealth()));
    }

    private void showRateLimitedHitEffect(SMPMob mob, Player attacker) {
        if (attacker == null || mob.getRenderTier() == SMPMob.RenderTier.HIDDEN) return;
        if (!manager.engine().clientLoadController().tryHitParticles(attacker, 3)) return;
        attacker.spawnParticle(Particle.CRIT, mob.getEntity().getLocation().add(0.0D, 1.0D, 0.0D),
                3, 0.25D, 0.25D, 0.25D, 0.0D);
    }

    private Player attackingPlayer(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }
}
