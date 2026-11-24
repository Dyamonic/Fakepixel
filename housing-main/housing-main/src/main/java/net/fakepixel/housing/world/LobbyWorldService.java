package net.fakepixel.housing.world;

import net.fakepixel.housing.house.House;
import org.bukkit.World;

/** Startup safety boundary: lobby code can inspect metadata but can never load a house world. */
public final class LobbyWorldService implements WorldService {
    @Override public World loadHouseWorld(House house) {
        throw new IllegalStateException("House worlds cannot be loaded on a Housing lobby server");
    }
    @Override public String getName() { return "Lobby (house loading disabled)"; }
}
