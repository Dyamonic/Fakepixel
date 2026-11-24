package net.fakepixel.housing.world;

import net.fakepixel.housing.house.House;
import org.bukkit.World;

public interface WorldService {
    World loadHouseWorld(House house) throws Exception;
    String getName();
}
