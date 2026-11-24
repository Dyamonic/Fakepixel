package club.abelix.games.fakevival.util;

import club.abelix.games.fakevival.menus.MenuHolder;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public final class SmpInventoryPolicy {
    private SmpInventoryPolicy() {
    }

    public static boolean isCustomMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof MenuHolder;
    }

    public static boolean isVanillaCrafting(Inventory inventory) {
        return inventory != null && isVanillaCrafting(inventory.getType(), isCustomMenu(inventory));
    }

    public static boolean isVanillaCrafting(InventoryType type, boolean customMenu) {
        return !customMenu && (type == InventoryType.WORKBENCH || type == InventoryType.CRAFTING);
    }
}
