package club.abelix.games.fastbridge.game.listener.standard;

import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.game.FastBridgeGame;
import club.abelix.games.fastbridge.game.inventory.item.ChangeWorldItem;
import club.abelix.games.fastbridge.game.listener.FastBridgeListener;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class InventorySlotsManager extends FastBridgeListener {

    public InventorySlotsManager(FastBridgeGame game) {
        super(game);
    }

    private static void extractItems(FastBridgeGamer gamer) {
        val player = gamer.getGamer().getPlayer();
        val inventory = player.getInventory();

        if (gamer.getIsland() == null) {
            player.kickPlayer("An error occurred!");
            log.error("Player without island tried to extract items");
            log.error(gamer.toString());
            return;
        }

        if (gamer.getCurrentBuildingSession() == null) {
            List<Integer> foundSlots = new ArrayList<>(2);

            for (int i = 0; i < 8 && i < inventory.getSize(); i++) {
                val item = inventory.getItem(i);
                if (item == null) continue;
                val itemType = item.getType();

                if (foundSlots.size() < 3 && itemType.isBlock()) foundSlots.add(i);

                if (!itemType.isBlock() && !itemType.equals(ChangeWorldItem.getItemStack().getType())) {
                    gamer.setToolSlot(i);
                }
            }

            if (foundSlots.size() >= 3) gamer.setBlockSlots(foundSlots.stream().mapToInt(Integer::intValue).toArray());
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        checkSession(event, (e, gamer) -> e.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSessionStart(BlockPlaceEvent event) {
        checkSession(event, (e, gamer) -> extractItems(gamer));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSlotChangeEvent(PlayerTeleportEvent event) {
        checkSession(event, (e, gamer) -> extractItems(gamer));
    }

    @EventHandler
    public void onPlayerClickEvent(InventoryClickEvent event) {
        checkSession(event, (e, gamer) -> {
            if (e.getCurrentItem() != null) {
                if (e.getCurrentItem().getType().equals(ChangeWorldItem.getItemStack().getType())) e.setCancelled(true);
                else extractItems(gamer);
            }
        });
    }

}
