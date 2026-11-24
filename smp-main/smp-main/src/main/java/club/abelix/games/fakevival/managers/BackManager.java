package club.abelix.games.fakevival.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BackManager {
    private final Map<UUID, Location> backLocations = new ConcurrentHashMap<>();

    public void setBackLocation(Player player, Location location) {
        if (player == null || location == null || location.getWorld() == null) return;
        backLocations.put(player.getUniqueId(), location.clone());
    }

    public Location getBackLocation(Player player) {
        Location location = backLocations.get(player.getUniqueId());
        return location == null ? null : location.clone();
    }

    public boolean hasBackLocation(Player player) {
        return backLocations.containsKey(player.getUniqueId());
    }

    public void remove(Player player) {
        if (player != null) backLocations.remove(player.getUniqueId());
    }
}
