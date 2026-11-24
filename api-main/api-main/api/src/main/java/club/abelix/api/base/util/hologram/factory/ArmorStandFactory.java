package club.abelix.api.base.util.hologram.factory;

import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import org.bukkit.Location;

public interface ArmorStandFactory {
    FakeEntityArmorStand create(Location location);
}
