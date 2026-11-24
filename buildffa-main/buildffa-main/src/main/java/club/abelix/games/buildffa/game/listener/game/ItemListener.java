package club.abelix.games.buildffa.game.listener.game;

import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.gui.standard.KitViewerGui;
import club.abelix.games.buildffa.game.event.GameMapChangeEvent;
import club.abelix.games.buildffa.game.kit.BaseKit;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.scheduler.BukkitTask;

public final class ItemListener extends YListener {
    private final BuildFFAGame game;
    private final BukkitTask cleanupTask;
    private World cleanupWorld;
    private Chunk[] cleanupChunks = new Chunk[0];
    private Entity[] cleanupEntities = new Entity[0];
    private Chunk cleanupChunk;
    private int chunkCursor;
    private int entityCursor;

    public ItemListener(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
        cleanupTask = Bukkit.getScheduler().runTaskTimer(game.getPlugin(), this::cleanupDrops, 600L, 600L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (game.isFrozen(event.getPlayer())) { event.setCancelled(true); return; }
        if (event.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof KitViewerGui
                || BaseKit.isProtectedKitItem(event.getItemDrop().getItemStack()) || !game.isBuildMode(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickup(PlayerPickupItemEvent event) {
        if (game.isFrozen(event.getPlayer())) { event.setCancelled(true); return; }
        if (BaseKit.isProtectedKitItem(event.getItem().getItemStack())) {
            event.setCancelled(true);
            if (game.getWorldConfig() != null && event.getItem().getWorld().equals(game.getWorldConfig().world()))
                event.getItem().remove();
        } else if (event.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof KitViewerGui
                || !game.isBuildMode(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (game.getWorldConfig() == null || !event.getLocation().getWorld().equals(game.getWorldConfig().world())
                || !BaseKit.isProtectedKitItem(event.getEntity().getItemStack())) return;
        event.setCancelled(true);
        event.getEntity().remove();
    }

    public void cleanupDrops() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Kit drop cleanup must run on the server thread");
        World world = game.getWorldConfig() == null ? null : game.getWorldConfig().world();
        if (world == null || Bukkit.getWorld(world.getUID()) != world) {
            resetCleanup();
            return;
        }
        if (cleanupWorld != world || chunkCursor >= cleanupChunks.length && entityCursor >= cleanupEntities.length) {
            resetCleanup();
            cleanupWorld = world;
            cleanupChunks = world.getLoadedChunks();
        }
        int entities = 0;
        int chunks = 0;
        while (entities < 1024) {
            if (entityCursor < cleanupEntities.length && cleanupChunk != null && cleanupChunk.isLoaded()) {
                Entity entity = cleanupEntities[entityCursor++];
                entities++;
                if (entity instanceof Item && !entity.isDead() && BaseKit.isProtectedKitItem(((Item) entity).getItemStack())) entity.remove();
                continue;
            }
            cleanupEntities = new Entity[0];
            entityCursor = 0;
            cleanupChunk = null;
            if (chunks >= 32 || chunkCursor >= cleanupChunks.length) break;
            Chunk chunk = cleanupChunks[chunkCursor++];
            chunks++;
            if (!chunk.isLoaded()) continue;
            cleanupChunk = chunk;
            cleanupEntities = chunk.getEntities();
        }
    }

    private void resetCleanup() {
        cleanupWorld = null;
        cleanupChunks = new Chunk[0];
        cleanupEntities = new Entity[0];
        cleanupChunk = null;
        chunkCursor = 0;
        entityCursor = 0;
    }

    @EventHandler
    public void onMapChange(GameMapChangeEvent event) {
        if (event.getGame() == game) resetCleanup();
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() != game.getPlugin()) return;
        cleanupTask.cancel();
        resetCleanup();
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFrozenUse(org.bukkit.event.player.PlayerInteractEvent event) {
        if (game.isFrozen(event.getPlayer())) {
            event.setCancelled(true);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFrozenEntityUse(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (game.isFrozen(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFrozenConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
        if (game.isFrozen(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFrozenShoot(org.bukkit.event.entity.EntityShootBowEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player
                && game.isFrozen((org.bukkit.entity.Player) event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFrozenVelocity(org.bukkit.event.player.PlayerVelocityEvent event) {
        if (game.isFrozen(event.getPlayer())) {
            event.setVelocity(new org.bukkit.util.Vector());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFrozenDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player
                && game.isFrozen((org.bukkit.entity.Player) event.getEntity())) event.setCancelled(true);
        if (!(event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent)) return;
        Entity damager = ((org.bukkit.event.entity.EntityDamageByEntityEvent) event).getDamager();
        if (damager instanceof org.bukkit.entity.Projectile) {
            org.bukkit.projectiles.ProjectileSource shooter = ((org.bukkit.entity.Projectile) damager).getShooter();
            if (shooter instanceof org.bukkit.entity.Player && game.isFrozen((org.bukkit.entity.Player) shooter)) event.setCancelled(true);
        }
        if (damager instanceof org.bukkit.entity.Player && game.isFrozen((org.bukkit.entity.Player) damager)) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFrozenEntityInteractAt(org.bukkit.event.player.PlayerInteractAtEntityEvent event) {
        if (game.isFrozen(event.getPlayer())) event.setCancelled(true);
    }
}
