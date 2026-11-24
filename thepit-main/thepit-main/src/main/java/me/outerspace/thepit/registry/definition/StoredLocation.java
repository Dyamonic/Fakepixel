package me.outerspace.thepit.registry.definition;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record StoredLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public Location resolve() {
        World world = Bukkit.getWorld(worldName);
        return world == null ? null : new Location(world, x, y, z, yaw, pitch);
    }

    public static StoredLocation fromBukkit(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return new StoredLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static StoredLocation parseLegacy(String value) {
        if (value == null) return null;
        String[] parts = value.split(",", -1);
        if (parts.length < 4) return null;
        try {
            return new StoredLocation(
                    parts[0].trim(),
                    Double.parseDouble(parts[1].trim()),
                    Double.parseDouble(parts[2].trim()),
                    Double.parseDouble(parts[3].trim()),
                    parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0.0F,
                    parts.length > 5 ? Float.parseFloat(parts[5].trim()) : 0.0F
            );
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
