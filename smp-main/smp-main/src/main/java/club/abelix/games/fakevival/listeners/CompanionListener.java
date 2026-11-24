package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.CompanionManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public final class CompanionListener implements Listener {
    private final CompanionManager companionManager;

    public CompanionListener(CompanionManager companionManager) {
        this.companionManager = companionManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof LivingEntity living)) return;
        if (!companionManager.canCapture(living)) return;
        if (companionManager.tryCapture(event.getPlayer(), living, event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUseItem(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        if (companionManager.useCompanionItem(event.getPlayer(), event.getItem())) event.setCancelled(true);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { companionManager.despawn(event.getPlayer(), false); }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) { companionManager.despawn(event.getPlayer(), false); }
    @EventHandler public void onPlayerDeath(PlayerDeathEvent event) { companionManager.despawn(event.getPlayer(), false); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (companionManager.isCompanion(event.getDamager())
                && companionManager.isCosmetic(companionManager.companionId(event.getDamager()))) {
            event.setCancelled(true);
            return;
        }
        if (companionManager.isCompanion(event.getDamager()) && event.getEntity() instanceof Player) {
            event.setCancelled(true);
            return;
        }

        Player attacker = getPlayerAttacker(event);
        if (event.getEntity() instanceof LivingEntity target && attacker != null && !(target instanceof Player) && !companionManager.isCompanion(target)) {
            companionManager.attackOwnerTarget(attacker, target);
        }
        if (event.getEntity() instanceof Player victim && getLivingDamager(event) instanceof LivingEntity damager && !(damager instanceof Player) && !companionManager.isCompanion(damager)) {
            companionManager.attackOwnerTarget(victim, damager);
        }

        if (companionManager.isCompanion(event.getEntity())) {
            UUID owner = companionManager.owner(event.getEntity());
            if (owner == null) return;
            Player ownerPlayer = org.bukkit.Bukkit.getPlayer(owner);
            if (companionManager.isCosmetic(companionManager.companionId(event.getEntity()))) {
                event.setCancelled(true);
                return;
            }
            if (ownerPlayer != null) {
                String id = companionManager.companionId(event.getEntity());
                event.setDamage(companionManager.reduceIncomingDamage(ownerPlayer, id, event.getDamage()));
            }
            if (attacker != null && attacker.getUniqueId().equals(owner)) {
                event.setCancelled(true);
                attacker.sendMessage(CC.color("&cYou cannot damage your own companion."));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (companionManager.isCompanion(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPhysicalInteract(EntityInteractEvent event) {
        if (companionManager.isCompanion(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(EntityBlockFormEvent event) {
        if (companionManager.isCompanion(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!companionManager.isCompanion(event.getEntity())) return;
        if (companionManager.isCosmetic(companionManager.companionId(event.getEntity())) || event.getTarget() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (companionManager.isCompanion(entity)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            companionManager.handleDeath(entity);
            return;
        }
        Player killer = entity.getKiller();
        if (killer == null || !(entity instanceof Monster)) return;
        companionManager.addKillXp(killer, entity);
    }

    private Player getPlayerAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }

    private Entity getLivingDamager(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Entity entity) return entity;
        return event.getDamager();
    }
}
