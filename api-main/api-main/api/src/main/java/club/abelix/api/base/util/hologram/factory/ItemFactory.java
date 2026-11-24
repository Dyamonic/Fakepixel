package club.abelix.api.base.util.hologram.factory;

import club.abelix.api.nms.entity.type.object.FakeEntityItem;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public interface ItemFactory {
    FakeEntityItem create(Location location, ItemStack item);
}
