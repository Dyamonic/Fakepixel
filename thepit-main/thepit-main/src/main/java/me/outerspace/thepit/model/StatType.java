package me.outerspace.thepit.model;

public enum StatType {
    KILLS("kills"), LEVEL("level"), XP("xp"), PRESTIGE("prestige"), DEATHS("deaths"), COINS("coins"), KILLSTREAK("killstreak");
    private final String path;
    StatType(String path) { this.path = path; }
    public String path() { return path; }
    public static StatType from(String s) {
        for (StatType t : values()) if (t.name().equalsIgnoreCase(s) || t.path.equalsIgnoreCase(s)) return t;
        return null;
    }
}
