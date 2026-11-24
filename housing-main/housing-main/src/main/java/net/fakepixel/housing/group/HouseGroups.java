package net.fakepixel.housing.group;

import org.bukkit.ChatColor;

public final class HouseGroups {
    public static final String OWNER = "owner";
    public static final String CO_OWNER = "co-owner";
    public static final String RESIDENT = "resident";
    public static final String GUEST = "guest";

    private HouseGroups() {}

    public static String normalize(String group) {
        if (group == null) return GUEST;
        String clean = ChatColor.stripColor(group).toLowerCase().replace("_", "-").trim();
        if (clean.equals("coowner") || clean.equals("co-owner")) return CO_OWNER;
        if (clean.equals("owner")) return OWNER;
        if (clean.equals("resident")) return RESIDENT;
        if (clean.equals("guest")) return GUEST;
        return clean;
    }

    public static String suffix(String group) {
        String normalized = normalize(group);
        return defaultColor(normalized) + "[" + displayName(normalized) + "]";
    }

    public static String displayName(String group) {
        String normalized = normalize(group);
        if (normalized.equals(CO_OWNER)) return "CO-OWNER";
        return normalized.toUpperCase().replace("-", " ");
    }

    public static String defaultColor(String group) {
        String normalized = normalize(group);
        if (normalized.equals(OWNER)) return "&e";
        if (normalized.equals(CO_OWNER)) return "&e";
        if (normalized.equals(RESIDENT)) return "&6";
        if (normalized.equals(GUEST)) return "&7";
        return "&7";
    }

    public static int power(String group) {
        String normalized = normalize(group);
        if (normalized.equals(OWNER)) return 100;
        if (normalized.equals(CO_OWNER)) return 90;
        if (normalized.equals(RESIDENT)) return 50;
        return 0;
    }

    public static boolean canManage(String group) {
        return power(group) >= 90;
    }

    public static boolean canBuild(String group) {
        return power(group) >= 50;
    }
}
