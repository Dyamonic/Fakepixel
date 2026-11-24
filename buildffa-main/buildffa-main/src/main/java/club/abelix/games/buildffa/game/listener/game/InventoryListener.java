package club.abelix.games.buildffa.game.listener.game;

import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.kit.BaseKit;
import club.abelix.games.buildffa.game.gui.standard.KitViewerGui;
import lombok.val;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * @author milansky
 */
public final class InventoryListener extends YListener {
    private final BuildFFAGame game;

    public InventoryListener(final @NotNull BuildFFAGame game) {
        super(game.getPlugin());

        this.game = game;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(final @NotNull InventoryClickEvent event) {
        if (game.isFrozen((org.bukkit.entity.Player) event.getWhoClicked())) { event.setCancelled(true); return; }

        if (event.getView().getTopInventory().getHolder() instanceof KitViewerGui) {
            ((KitViewerGui) event.getView().getTopInventory().getHolder()).handleEditorClick(event);
            return;
        }
        InventoryAction action = event.getAction();
        if ((action == InventoryAction.DROP_ALL_CURSOR || action == InventoryAction.DROP_ONE_CURSOR || event.getRawSlot() < 0)
                && BaseKit.isProtectedKitItem(event.getCursor())
                || (action == InventoryAction.DROP_ALL_SLOT || action == InventoryAction.DROP_ONE_SLOT)
                && BaseKit.isProtectedKitItem(event.getCurrentItem())) {
            event.setCancelled(true);
            return;
        }
        boolean isClickedChest = event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.CHEST;
        boolean isOpenChest = event.getWhoClicked().getOpenInventory().getType() == InventoryType.CHEST;

        if ((event.isShiftClick() && isOpenChest)
                || game.isLobbyZone(event.getWhoClicked().getLocation())
                || isClickedChest) {
            event.setCancelled(true);
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (game.isFrozen((org.bukkit.entity.Player) event.getWhoClicked())) { event.setCancelled(true); return; }
        if (event.getView().getTopInventory().getHolder() instanceof KitViewerGui) {
            ((KitViewerGui) event.getView().getTopInventory().getHolder()).handleEditorDrag(event);
            return;
        }
        if (BaseKit.isProtectedKitItem(event.getOldCursor()))
            for (int slot : event.getRawSlots()) if (slot < 0) { event.setCancelled(true); return; }
        if (event.getInventory().getType() == InventoryType.CHEST) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraft(final @NotNull CraftItemEvent event) {
        event.setCancelled(true);
    }
}
