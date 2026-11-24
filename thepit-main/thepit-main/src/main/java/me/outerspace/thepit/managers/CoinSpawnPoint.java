package me.outerspace.thepit.managers;

import org.bukkit.Location;
import me.outerspace.thepit.registry.definition.StoredLocation;

public final class CoinSpawnPoint {
    private final String id;
    private final StoredLocation location;
    private final int reward;
    private final int respawnSeconds;

    public CoinSpawnPoint(String id, StoredLocation location, int reward, int respawnSeconds) {
        this.id = id;
        this.location = location;
        this.reward = reward;
        this.respawnSeconds = respawnSeconds;
    }

    public String getId() { return id; }
    public StoredLocation getStoredLocation() { return location; }
    public Location getLocation() { return location.resolve(); }
    public int getReward() { return reward; }
    public int getRespawnSeconds() { return respawnSeconds; }
}
