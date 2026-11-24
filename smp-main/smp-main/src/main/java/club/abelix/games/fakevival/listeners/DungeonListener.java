package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.DungeonManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DungeonListener implements Listener {
    private final JavaPlugin plugin;
    private final DungeonManager dungeonManager;
    private final Map<UUID, Long> portalCooldown = new HashMap<>();

    public DungeonListener(JavaPlugin plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!dungeonManager.isRuleEnabled("disable-block-breaking-placing", true)) return;
        if (dungeonManager.isInsideDungeonArea(event.getBlock().getLocation()) && !dungeonManager.canBypassDungeonRules(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CC.color("&cYou cannot break blocks inside a dungeon."));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!dungeonManager.isRuleEnabled("disable-block-breaking-placing", true)) return;
        if (dungeonManager.isInsideDungeonArea(event.getBlock().getLocation()) && !dungeonManager.canBypassDungeonRules(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CC.color("&cYou cannot place blocks inside a dungeon."));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        Player attacker = event instanceof EntityDamageByEntityEvent byEntity ? getAttackingPlayer(byEntity) : null;
        if(attacker!=null&&plugin instanceof club.abelix.games.fakevival.Game game&&game.getDungeonSpectateService().isSpectating(attacker)){event.setCancelled(true);return;}
        dungeonManager.applyDungeonBossOutgoingDamage(event);
        dungeonManager.handleDungeonEntityDamage(event, attacker);
        if (event.getEntity() instanceof LivingEntity living && dungeonManager.isDungeonEntity(living)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!living.isDead()) dungeonManager.updateDungeonEntityDisplay(living);
            }, 1L);
        }
        if (!(event.getEntity() instanceof Player player)) return;
        if (!dungeonManager.isInsideDungeonArea(player.getLocation())) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && dungeonManager.isRuleEnabled("disable-fall-damage", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDungeonMobCombust(EntityCombustEvent event) {
        if (!dungeonManager.isProtectedDungeonEntity(event.getEntity())) return;
        event.setCancelled(true);
        event.getEntity().setFireTicks(0);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!dungeonManager.isRuleEnabled("disable-pvp", true)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = getAttackingPlayer(event);
        if (attacker == null) return;
        if (dungeonManager.isInsideDungeonArea(victim.getLocation()) || dungeonManager.isInsideDungeonArea(attacker.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!dungeonManager.isRuleEnabled("disable-hunger", true)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!dungeonManager.isInsideDungeonArea(player.getLocation())) return;
        if (event.getFoodLevel() >= player.getFoodLevel()) return;
        event.setCancelled(true);
        player.setFoodLevel(20);
        player.setSaturation(Math.max(player.getSaturation(), 20.0F));
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!dungeonManager.isRuleEnabled("disable-bed-sleep", true)) return;
        if (!dungeonManager.isInsideDungeonArea(event.getBed().getLocation()) || dungeonManager.canBypassDungeonRules(event.getPlayer())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cYou cannot sleep inside a dungeon."));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || !dungeonManager.isInsideDungeonArea(event.getClickedBlock().getLocation())) return;
        if (dungeonManager.canBypassDungeonRules(event.getPlayer())) return;
        Material type = event.getClickedBlock().getType();
        boolean dungeonLootChest = plugin instanceof club.abelix.games.fakevival.Game game
                && game.getDungeonLootChestManager().isLootChest(event.getClickedBlock());
        if (dungeonManager.isRuleEnabled("disable-container-access", true) && isProtectedContainer(type) && !dungeonLootChest) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CC.color("&cYou cannot use containers inside a dungeon."));
            return;
        }
        if (dungeonManager.isRuleEnabled("disable-bed-sleep", true)
                && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && type.name().endsWith("_BED")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CC.color("&cYou cannot use beds inside a dungeon."));
        }
    }

    @EventHandler
    public void onFarmlandTrample(EntityChangeBlockEvent event) {
        if (!dungeonManager.isInsideDungeonArea(event.getBlock().getLocation())) return;
        if (event.getBlock().getType() == Material.FARMLAND && dungeonManager.isRuleEnabled("disable-farmland-trample", true)) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Enderman && dungeonManager.isRuleEnabled("disable-enderman-griefing", true)) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof EnderDragon && dungeonManager.isRuleEnabled("disable-enderdragon-griefing", true)) {
            event.setCancelled(true);
            return;
        }
        if (dungeonManager.isRuleEnabled("disable-other-griefing", true)) event.setCancelled(true);
    }

    @EventHandler
    public void onFireSpread(BlockSpreadEvent event) {
        if (dungeonManager.isRuleEnabled("disable-fire-spread", true)
                && event.getSource().getType() == Material.FIRE
                && dungeonManager.isInsideDungeonArea(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (!dungeonManager.isInsideDungeonArea(event.getBlock().getLocation())) return;
        if (event.getBlock().getType() == Material.FIRE && dungeonManager.isRuleEnabled("disable-fire-spread", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (dungeonManager.isInsideDungeonArea(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!dungeonManager.isInsideDungeonArea(event.getLocation())) return;
        if (event.getEntity() instanceof EnderDragon && dungeonManager.isRuleEnabled("disable-enderdragon-griefing", true)) {
            event.blockList().clear();
            return;
        }
        if (dungeonManager.isRuleEnabled("disable-other-griefing", true)) event.blockList().clear();
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!dungeonManager.isInsideDungeonArea(event.getEntity().getLocation())) return;
        if (event.getEntityType().name().contains("FIREBALL") && dungeonManager.isRuleEnabled("disable-enderdragon-fireball-explosion", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!dungeonManager.isInsideDungeonArea(event.getLocation())) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            event.setCancelled(true);
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            dungeonManager.removeUnauthorizedDungeonCustomSpawn(event.getEntity());
        }, 1L);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        dungeonManager.handleDungeonMobDeath(event.getEntity(), event.getEntity().getKiller());
        dungeonManager.isDungeonBoss(event.getEntity());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!dungeonManager.isDungeonDeath(player)) return;
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        dungeonManager.handleDungeonDeath(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        org.bukkit.Location target = dungeonManager.consumePendingDungeonRespawn(event.getPlayer());
        if (target != null && target.getWorld() != null) event.setRespawnLocation(target);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        if (portalCooldown.getOrDefault(player.getUniqueId(), 0L) > now) return;
        String dungeon = dungeonManager.getPortalDungeon(player.getLocation());
        if (dungeon == null) return;
        portalCooldown.put(player.getUniqueId(), now + 3000L);
        dungeonManager.teleportToDungeon(player, dungeon);
    }

    private Player getAttackingPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }

    private boolean isProtectedContainer(Material type) {
        return type == Material.CHEST
                || type == Material.TRAPPED_CHEST
                || type == Material.BARREL
                || type == Material.SHULKER_BOX
                || type.name().endsWith("_SHULKER_BOX")
                || type == Material.FURNACE
                || type == Material.BLAST_FURNACE
                || type == Material.SMOKER
                || type == Material.HOPPER
                || type == Material.DROPPER
                || type == Material.DISPENSER
                || type == Material.BREWING_STAND;
    }
}
