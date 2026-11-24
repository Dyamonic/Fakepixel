package club.abelix.games.fakevival.menus;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public record MenuHolder(String type) implements InventoryHolder {

    @Override
    public Inventory getInventory() {
        return null;
    }
}
