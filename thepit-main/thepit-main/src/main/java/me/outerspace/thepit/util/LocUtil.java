package me.outerspace.thepit.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class LocUtil {
    private LocUtil() {}
    public static Location fromString(String raw) {
        if (raw == null) return null;
        String[] p = raw.split(",");
        if (p.length < 4) return null;
        World w = Bukkit.getWorld(p[0]);
        if (w == null) return null;
        float yaw = p.length >= 5 ? Float.parseFloat(p[4]) : 0f;
        float pitch = p.length >= 6 ? Float.parseFloat(p[5]) : 0f;
        return new Location(w, Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]), yaw, pitch);
    }
    public static String toString(Location l) {
        return l.getWorld().getName()+","+l.getX()+","+l.getY()+","+l.getZ()+","+l.getYaw()+","+l.getPitch();
    }
}
