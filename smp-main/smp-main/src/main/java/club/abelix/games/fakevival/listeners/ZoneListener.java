package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.ZoneManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZoneListener implements Listener {
    private final JavaPlugin plugin;
    private final ZoneManager zoneManager;

    public ZoneListener(JavaPlugin plugin, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.zoneManager = zoneManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        zoneManager.checkMove(event.getPlayer());
    }

    @EventHandler
    public void onPvp(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity victim && zoneManager.isZoneBoss(victim)) {
            Player attacker = getAttacker(event);
            if (attacker != null) zoneManager.handleBossDamagedByPlayer(victim, attacker);
        }
        if (zoneManager.isZoneBoss(event.getDamager())) {
            if (!(event.getEntity() instanceof Player player) || zoneManager.canBossTarget((LivingEntity) event.getDamager(), player)) {
                event.setCancelled(true);
                return;
            }
        }
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = getAttacker(event);
        if (attacker == null || attacker.equals(victim)) return;
        if (!zoneManager.isPvpBlocked(attacker.getLocation()) && !zoneManager.isPvpBlocked(victim.getLocation())) return;
        event.setCancelled(true);
        attacker.sendMessage(CC.color("&cPvP is disabled in this zone."));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getPlayer().hasPermission("fakevival.admin.zone.bypass")) return;
        if (zoneManager.canBreak(event.getPlayer(), event.getBlock())) {
            zoneManager.scheduleRegeneration(event.getBlock());
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cYou cannot break that block in this zone."));
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (zoneManager.canPlace(event.getPlayer(), event.getBlock()) || event.getPlayer().hasPermission("fakevival.admin.zone.bypass")) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cYou cannot place blocks in this zone."));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onZoneMobHit(EntityDamageByEntityEvent event) {
        if (!zoneManager.isZoneDungeonMob(event.getEntity())) return;
        zoneManager.handleZoneMobDamage(event, getAttacker(event));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onZoneMobEnvironmentDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent || !zoneManager.isZoneDungeonMob(event.getEntity())) return;
        zoneManager.handleZoneMobDamage(event, null);
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!zoneManager.isZoneMob(event.getEntity())) return;
        if (event.getEntity() instanceof LivingEntity living && zoneManager.isZoneBoss(living)) {
            if (!(event.getTarget() instanceof Player player) || zoneManager.canBossTarget(living, player)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!(event.getTarget() instanceof Player player)) return;
        if (!zoneManager.isPvpBlocked(player.getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        zoneManager.handleBossTargetPlayerDeath(event.getEntity());
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        boolean customSpawn = event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM;
        if (zoneManager.blocksDungeonCreatureSpawn(event.getLocation(), event.getEntityType(), customSpawn)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("entity-death-messages.hide-named-entity-deaths", true)
                && !(event.getEntity() instanceof Player)
                && event.getEntity().getCustomName() != null) {
            event.getEntity().setCustomName(null);
            event.getEntity().setCustomNameVisible(false);
        }
        if (!zoneManager.isZoneMob(event.getEntity())) return;
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("zones.suppress-zone-mob-death-messages", true)) {
            event.getEntity().setCustomName(null);
            event.getEntity().setCustomNameVisible(false);
        }
        if (zoneManager.isZoneBoss(event.getEntity())) {
            Player killer = event.getEntity().getKiller();
            zoneManager.handleBossDeath(event.getEntity(), killer == null ? zoneManager.getZoneMobKiller(event.getEntity()) : killer);
        } else {
            event.getDrops().clear();
            event.setDroppedExp(0);
            Player killer = event.getEntity().getKiller();
            zoneManager.handleZoneMobDeath(event.getEntity(), killer == null ? zoneManager.getZoneMobKiller(event.getEntity()) : killer);
        }
    }

    private Player getAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }
}
