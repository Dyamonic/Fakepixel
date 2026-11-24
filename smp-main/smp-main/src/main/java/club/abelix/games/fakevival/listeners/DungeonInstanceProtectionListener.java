package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.managers.DungeonManager;
import club.abelix.games.fakevival.managers.SpawnProtectionManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Locale;

public final class DungeonInstanceProtectionListener implements Listener {
    private final Game game;
    private final DungeonManager dungeons;
    private final SpawnProtectionManager spawnProtection;

    public DungeonInstanceProtectionListener(Game game, DungeonManager dungeons, SpawnProtectionManager spawnProtection) {
        this.game = game;
        this.dungeons = dungeons;
        this.spawnProtection = spawnProtection;
    }

    public boolean isDungeonInstanceWorld(World world) {
        if (world == null || world.getName().equalsIgnoreCase("lobby")) return false;
        if (dungeons.isActiveDungeonInstanceWorld(world)) return true;
        if (game.getSMPWorldManager() != null && game.getSMPWorldManager().isDungeonInstanceWorld(world)) return true;
        String name = world.getName().replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return name.startsWith("dungeon_") || name.startsWith("dun_") || name.startsWith("smpdun_instance_");
    }

    public boolean isDungeonMonster(Entity entity) {
        return entity instanceof LivingEntity && dungeons.isDungeonEntity(entity) && !isDungeonBoss(entity);
    }

    public boolean isDungeonBoss(Entity entity) {
        return dungeons.isDungeonBossEntity(entity);
    }

    public boolean isDungeonMob(Entity entity) {
        return isDungeonMonster(entity) || isDungeonBoss(entity);
    }

    public boolean isDungeonPlayer(Player player) {
        return player != null && isDungeonInstanceWorld(player.getWorld());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!isDungeonInstanceWorld(event.getBlock().getWorld()) || canEdit(event.getPlayer())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cYou cannot break blocks inside a dungeon."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!isDungeonInstanceWorld(event.getBlock().getWorld()) || canEdit(event.getPlayer())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cYou cannot place blocks inside a dungeon."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isDungeonInstanceWorld(event.getLocation().getWorld())) event.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (isDungeonInstanceWorld(event.getBlock().getWorld())) event.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (isDungeonInstanceWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (isDungeonInstanceWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (isDungeonInstanceWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !isDungeonInstanceWorld(victim.getWorld())) return;
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker == null) return;
        event.setCancelled(true);
        attacker.sendMessage(CC.color("&cPvP is disabled inside dungeons."));
    }

    private boolean canEdit(Player player) {
        return spawnProtection.isBuildMode(player) && (player.hasPermission("fakevival.dungeon.buildmode")
                || player.hasPermission("smp.admin.buildmode") || player.hasPermission("fakevival.admin.buildmode"));
    }

    private Player attackingPlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }
}
