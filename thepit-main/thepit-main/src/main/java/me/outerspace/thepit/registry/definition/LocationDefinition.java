package me.outerspace.thepit.registry.definition;

public record LocationDefinition(String worldName, double x, double y, double z, float yaw, float pitch) {
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public static LocationDefinition parse(String raw) {
        if (raw == null) return null;
        try {
            String[] p = raw.split(",");
            if (p.length < 4) return null;
            return new LocationDefinition(p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                    p.length > 4 ? Float.parseFloat(p[4]) : 0.0F, p.length > 5 ? Float.parseFloat(p[5]) : 0.0F);
        } catch (Exception ignored) { return null; }
    }

    public String serialize() { return worldName + "," + x + "," + y + "," + z + "," + yaw + "," + pitch; }
}
