package club.abelix.api.server.base.listener;

import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.listener.YListener;
import lombok.NonNull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

public class GuiListener extends YListener {

    public GuiListener(@NonNull Plugin plugin) {
        super(plugin);
    }

    private static Gui parseGui(Inventory inventory) {
        InventoryHolder holder = inventory != null ? inventory.getHolder() : null;

        if (!(holder instanceof Gui)) {
            return null;
        }

        return ((Gui) holder);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Gui gui = parseGui(event.getInventory());

        if (gui == null) {
            return;
        }

        gui.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Gui gui = parseGui(event.getInventory());

        if (gui == null) {
            return;
        }

        gui.handleDrag(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Gui gui = parseGui(event.getInventory());

        if (gui == null) {
            return;
        }

        gui.handleClose(event);
    }

}
