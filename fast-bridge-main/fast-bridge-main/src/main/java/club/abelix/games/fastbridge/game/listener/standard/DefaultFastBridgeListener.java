package club.abelix.games.fastbridge.game.listener.standard;

import club.abelix.games.fastbridge.commons.island.Island;
import club.abelix.games.fastbridge.commons.island.IslandSelector;
import club.abelix.games.fastbridge.commons.object.BuildingSession;
import club.abelix.games.fastbridge.game.FastBridgeGame;
import club.abelix.games.fastbridge.game.listener.FastBridgeListener;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultFastBridgeListener extends FastBridgeListener {

    IslandSelector islandSelector;

    public DefaultFastBridgeListener(
            final FastBridgeGame game,
            final IslandSelector selector
    ) {
        super(game);

        this.islandSelector = selector;
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) {
        checkSession(event, (__, gamer) -> {
            val island = gamer.getIsland();

            if (island == null) {
                return;
            }

            if (!island.containsPlayableZone(event.getTo())) {
                island.handleGameZoneLeave();
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPushPressurePlate(PlayerInteractEvent event) {
        checkSession(event, (e, gamer) -> {
            assert gamer.getIsland() != null;
            Island island = gamer.getIsland();

            if (!e.getAction().equals(Action.PHYSICAL)) return;
            if (e.getClickedBlock().getType().toString().endsWith("_PLATE")) island.endSession();
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        checkSession(event, (e, gamer) -> {
            val location = e.getBlock().getLocation();

            assert gamer.getIsland() != null;
            val island = gamer.getIsland();
            val player = gamer.getGamer().getPlayer();

            if (!island.containsAllowedBuildingZone(location)) e.setCancelled(true);
            else {
                island.getPlacedBlocks().add(e.getBlock());
                val lastSession = gamer.getCurrentBuildingSession();

                if (lastSession == null || (lastSession.isEnded() & lastSession.isStarted())) {
                    val newSession = new BuildingSession(island.getRawConfig());
                    newSession.setStartBuildingAt(System.currentTimeMillis());
                    gamer.setCurrentBuildingSession(newSession);
                }

                gamer.getBlockPlaced().addAndGet(1);
                val item = player.getItemInHand();
                if (item.getAmount() <= 16) {
                    item.setAmount(64);
                    player.updateInventory();
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        checkSession(event, (e, gamer) -> {
            Block block = e.getBlock();
            Location location = block.getLocation();

            assert gamer.getIsland() != null;
            Island island = gamer.getIsland();

            e.setCancelled(true);

            if (island.containsAllowedBuildingZone(location)
                    && island.getPlacedBlocks().contains(block)) {

                val player = e.getPlayer();

                player.getInventory().addItem(block.getDrops().toArray(new ItemStack[0]));
                block.setType(Material.AIR);
            }
        });
    }

    @EventHandler
    public void onItemSplit(InventoryDragEvent event) {
        checkSession(event, (e, gamer) -> e.setCancelled(true));
    }

    @EventHandler
    public void onItemSplit(InventoryClickEvent event) {
        checkSession(event, (e, gamer) -> {
            if (e.isRightClick()) {
                e.setCancelled(true);
            }
        });
    }
}
