package club.abelix.api.server.game.listener.base;

import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.listener.GameListener;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class GameRestrictionListener extends GameListener<Game> {

    @Getter
    private final World world;

    public GameRestrictionListener(Game game) {
        this(game, game.getGameWorld());
    }

    public GameRestrictionListener(Game game, World world) {
        super(game);

        this.world = world;
    }

    // Листенер запрещает различные евенты

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        checkGame(event, () -> event.setCancelled(event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onLeavesDecayEvent(LeavesDecayEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onPhysic(BlockPhysicsEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        checkGame(event, cancel());
    }

    @EventHandler
    public void onFade(BlockFadeEvent event) {
        checkGame(event, cancel());
    }

}
