package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.DupeDetectionManager;
import club.abelix.games.fakevival.util.SmpInventoryPolicy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public final class DupeDetectionListener implements Listener {
    private final DupeDetectionManager dupeDetectionManager;

    public DupeDetectionListener(DupeDetectionManager dupeDetectionManager) {
        this.dupeDetectionManager = dupeDetectionManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        dupeDetectionManager.requestScan(event.getPlayer(), "player_join");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if (SmpInventoryPolicy.isVanillaCrafting(event.getView().getTopInventory())) {
            dupeDetectionManager.requestCraftingScan(player, "crafting_click");
            return;
        }
        ItemStack hotbar = event.getHotbarButton() >= 0 ? player.getInventory().getItem(event.getHotbarButton()) : null;
        dupeDetectionManager.requestScan(player, "inventory_click", event.getCurrentItem(), event.getCursor(), hotbar);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if (SmpInventoryPolicy.isVanillaCrafting(event.getView().getTopInventory())) {
            dupeDetectionManager.requestCraftingScan(player, "crafting_drag");
            return;
        }
        dupeDetectionManager.requestScan(player, "inventory_drag", event.getOldCursor());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof org.bukkit.entity.Player player)
            dupeDetectionManager.requestScan(player, "inventory_move", event.getItem());
        else if (event.getDestination().getHolder() instanceof org.bukkit.entity.Player player)
            dupeDetectionManager.requestScan(player, "inventory_move", event.getItem());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player player)
            dupeDetectionManager.requestScan(player, "player_pickup", event.getItem().getItemStack());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        dupeDetectionManager.requestScan(event.getPlayer(), "player_drop", event.getItemDrop().getItemStack());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        dupeDetectionManager.forgetPlayer(event.getPlayer().getUniqueId());
    }
}
