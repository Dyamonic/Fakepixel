package club.abelix.games.fakevival.network;

public enum SMPServerType {
    HUB, OVERWORLD, DUNGEON, UNKNOWN;

    public static SMPServerType parse(String value) {
        if (value == null || value.isBlank()) return UNKNOWN;
        try { return valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return UNKNOWN; }
    }
}
