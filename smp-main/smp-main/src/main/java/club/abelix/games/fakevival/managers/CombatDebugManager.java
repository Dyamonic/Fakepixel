package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.mmo.MMODamageIndicatorService;
import club.abelix.games.fakevival.mobengine.mob.SMPMobEngine;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatDebugManager implements Listener {
    private final DungeonManager dungeonManager;
    private final ZoneManager zoneManager;
    private final SMPMobEngine smpMobEngine;
    private final Set<UUID> combatDebugPlayers = ConcurrentHashMap.newKeySet();
    private final Map<EntityDamageByEntityEvent, CombatTrace> traces = Collections.synchronizedMap(new WeakHashMap<>());

    public CombatDebugManager(DungeonManager dungeonManager, ZoneManager zoneManager, SMPMobEngine smpMobEngine) {
        this.dungeonManager = dungeonManager;
        this.zoneManager = zoneManager;
        this.smpMobEngine = smpMobEngine;
    }

    public boolean toggleCombatDebug(Player player) {
        if (combatDebugPlayers.remove(player.getUniqueId())) return false;
        combatDebugPlayers.add(player.getUniqueId());
        return true;
    }

    public boolean setCombatDebug(Player player, boolean enabled) {
        if (enabled) combatDebugPlayers.add(player.getUniqueId());
        else combatDebugPlayers.remove(player.getUniqueId());
        return enabled;
    }

    public boolean isCombatDebugEnabled(Player player) {
        return player != null && combatDebugPlayers.contains(player.getUniqueId());
    }

    public void sendCombatDebug(Player player, String message) {
        if (isCombatDebugEnabled(player)) player.sendMessage(CC.color("&8[CombatDebug] " + message));
    }

    public void sendToNearbyDebuggers(Entity entity, String message) {
        if (entity == null) return;
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(entity.getLocation()) <= 32.0D * 32.0D) sendCombatDebug(player, message);
        }
    }

    public void recordCustomDamage(EntityDamageByEntityEvent event, double customDamage, boolean critical) {
        CombatTrace trace = traces.get(event);
        if (trace != null) {
            trace.customDamage = customDamage;
            trace.critical = critical;
        }
    }

    public void recordCancellation(EntityDamageByEntityEvent event, String reason) {
        CombatTrace trace = traces.get(event);
        if (trace != null) trace.cancelReason = reason;
    }

    public void recordDamageCounter(EntityDamageByEntityEvent event, MMODamageIndicatorService.DisplayResult result) {
        CombatTrace trace = traces.get(event);
        if (trace != null) trace.displayResult = result;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void begin(EntityDamageByEntityEvent event) {
        Player attacker = attackingPlayer(event.getDamager());
        Player victim = event.getEntity() instanceof Player player ? player : null;
        Player debugger = isCombatDebugEnabled(attacker) ? attacker : isCombatDebugEnabled(victim) ? victim : null;
        if (debugger != null) traces.put(event, new CombatTrace(debugger, event.getDamage()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void finish(EntityDamageByEntityEvent event) {
        CombatTrace trace = traces.remove(event);
        if (trace == null || !isCombatDebugEnabled(trace.debugger)) return;
        Entity target = event.getEntity();
        boolean dungeonMob = dungeonManager.isDungeonEntity(target) || zoneManager.isZoneDungeonMob(target);
        boolean zoneMob = zoneManager.isZoneMob(target);
        boolean engineMob = smpMobEngine != null && smpMobEngine.isEngineMob(target);
        boolean smpMob = engineMob || dungeonMob || zoneMob;
        DungeonManager.DungeonCombatContext dungeon = dungeonManager.combatContext(target);
        String bossContext = dungeon.boss() ? "DUNGEON" : zoneManager.isZoneBoss(target) ? "ZONE" : "NONE";
        String mobType = dungeonMob ? "DUNGEON" : zoneMob ? "ZONE" : smpMob ? "SMP" : "VANILLA";
        String inferredReason = dungeon.dungeonId().equals("none") || dungeonManager.dungeonState(dungeon.dungeonId()) == DungeonManager.DungeonState.ACTIVE
                ? "unknown/other listener" : "dungeon not active";
        String reason = event.isCancelled() ? trace.cancelReason == null ? inferredReason : trace.cancelReason : "none";
        String counter = trace.displayResult == null ? "false" : Boolean.toString(trace.displayResult.spawned());
        boolean limited = trace.displayResult != null && trace.displayResult.rateLimited();

        sendCombatDebug(trace.debugger, "&7Attacker=&e" + entityName(event.getDamager()) + " &7Target=&c" + entityName(target)
                + " &7World=&e" + target.getWorld().getName());
        sendCombatDebug(trace.debugger, "&7SmpMob=&e" + smpMob + " &7EngineMob=&e" + engineMob
                + " &7DungeonMob=&e" + dungeonMob + " &7ZoneMob=&e" + zoneMob);
        sendCombatDebug(trace.debugger, "&7Base=&e" + number(trace.baseDamage) + " &7Custom=&e" + number(trace.customDamage)
                + " &7Final=&e" + number(event.getFinalDamage()) + " &7Critical=&e" + trace.critical
                + " &7Cancelled=&c" + event.isCancelled() + " &7Reason=&e" + reason);
        sendCombatDebug(trace.debugger, "&7MobType=&e" + mobType + " &7BossContext=&e" + bossContext
                + " &7DamageCounter=&a" + counter + " &7RateLimited=&e" + limited);
        if (target instanceof LivingEntity living) {
            AttributeInstance max = living.getAttribute(Attribute.MAX_HEALTH);
            double bukkitMax = max == null ? living.getMaxHealth() : max.getValue();
            if (engineMob) {
                sendCombatDebug(trace.debugger, "&7EngineHP=&e" + number(smpMobEngine.getCurrentHp(living)) + "&7/&e"
                        + number(smpMobEngine.getMaxHp(living)) + " &7BukkitVisual=&e"
                        + number(living.getHealth()) + "&7/&e" + number(bukkitMax));
            } else {
                double after = event.isCancelled() ? living.getHealth() : Math.max(0.0D, living.getHealth() - event.getFinalDamage());
                sendCombatDebug(trace.debugger, "&7HealthAfter=&e" + number(after) + "&7/&e" + number(bukkitMax)
                        + " &7Dungeon=&e" + dungeon.dungeonId() + " &7Run=&e" + dungeon.runId());
            }
            if (!bossContext.equals("NONE")) {
                String bossId = pdcString(living, "zone_boss");
                if (bossId == null) bossId = pdcString(living, "mob_id");
                if (bossId == null) bossId = pdcString(living, "dungeon_boss");
                double incoming = Math.max(0.0D, trace.customDamage);
                double defense = pdcDouble(living, "mmo_defense", pdcDouble(living, "dungeon_defense", 0.0D));
                double reduced = incoming * (100.0D / (100.0D + Math.max(0.0D, defense)));
                double minimum = Math.max(1.0D, incoming * 0.10D);
                double finalDamage = Math.max(reduced, minimum);
                double current = engineMob ? smpMobEngine.getCurrentHp(living) : pdcDouble(living, "boss_current_health", pdcDouble(living, "dungeon_current_health", living.getHealth()));
                double maxHp = engineMob ? smpMobEngine.getMaxHp(living) : pdcDouble(living, "boss_max_health", pdcDouble(living, "dungeon_max_health", bukkitMax));
                sendCombatDebug(trace.debugger, "&7boss=&e" + (bossId == null ? "unknown" : bossId)
                        + " &7incoming=&e" + number(incoming) + " &7defense=&e" + number(defense)
                        + " &7reduced=&e" + number(reduced) + " &7minimum=&e" + number(minimum)
                        + " &7final=&e" + number(finalDamage) + " &7currentHp=&e" + number(current)
                        + " &7maxHp=&e" + number(maxHp) + " &7cancelled=&e" + event.isCancelled());
            }
            if (living instanceof Mob mob && !mob.hasAI()) sendCombatDebug(trace.debugger, "&cWarning: target AI is disabled during combat.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        combatDebugPlayers.remove(event.getPlayer().getUniqueId());
    }

    private Player attackingPlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }

    private String entityName(Entity entity) {
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) return entityName(shooter);
        return entity.getName();
    }

    private String number(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private String pdcString(LivingEntity entity,String key){for(NamespacedKey found:entity.getPersistentDataContainer().getKeys())if(found.getKey().equalsIgnoreCase(key))return entity.getPersistentDataContainer().get(found,PersistentDataType.STRING);return null;}
    private double pdcDouble(LivingEntity entity,String key,double fallback){for(NamespacedKey found:entity.getPersistentDataContainer().getKeys())if(found.getKey().equalsIgnoreCase(key)){Double value=entity.getPersistentDataContainer().get(found,PersistentDataType.DOUBLE);return value==null?fallback:value;}return fallback;}

    private static final class CombatTrace {
        private final Player debugger;
        private final double baseDamage;
        private double customDamage;
        private boolean critical;
        private String cancelReason;
        private MMODamageIndicatorService.DisplayResult displayResult;

        private CombatTrace(Player debugger, double baseDamage) {
            this.debugger = debugger;
            this.baseDamage = baseDamage;
            this.customDamage = baseDamage;
        }
    }
}
