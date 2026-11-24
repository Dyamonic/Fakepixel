package club.abelix.games.buildffa.game.listener.game.layout;

import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.gui.standard.KitViewerGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

public class LayoutChangeListener extends YListener implements Listener {
    public LayoutChangeListener(final @NotNull BuildFFAGame game) {
        super(game.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(final @NotNull InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof KitViewerGui gui)) return;
        if (gui.isEditableSlot(event.getRawSlot())) gui.queueLayoutUpdate();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof KitViewerGui gui)) return;
        gui.handleEditorDrag(event);
        if (event.isCancelled()) return;
        gui.queueLayoutUpdate();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof KitViewerGui gui) gui.editorOpened();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof KitViewerGui gui) gui.editorClosed();
    }
}