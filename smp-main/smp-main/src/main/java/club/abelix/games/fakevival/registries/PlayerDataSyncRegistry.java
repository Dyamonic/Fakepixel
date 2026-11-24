package club.abelix.games.fakevival.registries;

public final class PlayerDataSyncRegistry {
    public static final boolean ENABLED = true;
    public static final String TABLE = "smp_playerdata_sync";
    public static final int AUTOSAVE_SECONDS = 60;
    public static final int LOCK_TTL_SECONDS = 30;
    public static final int HEARTBEAT_INTERVAL_SECONDS = 10;
    public static final int STALE_LOCK_GRACE_SECONDS = 45;
    public static final int JOIN_LOCK_RETRIES = 20;
    public static final long JOIN_RETRY_DELAY_MILLIS = 500L;
    public static final int TRANSFER_LOCK_SECONDS = 5;
    public static final boolean SYNC_INVENTORY = true;
    public static final boolean SYNC_ARMOR = true;
    public static final boolean SYNC_OFFHAND = true;
    public static final boolean SYNC_ENDER_CHEST = true;
    public static final boolean SYNC_EXP = true;
    public static final boolean SYNC_HEALTH = true;
    public static final boolean SYNC_FOOD = true;
    public static final boolean SYNC_EFFECTS = true;
    public static final boolean SYNC_GAMEMODE = false;
    public static final boolean SYNC_LOCATION = false;
    public static final String SERVER_ID_FALLBACK = "smp-unknown";

    private PlayerDataSyncRegistry() {
    }

    public static boolean booleanValue(String name, boolean fallback) {
        String value = external(name);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    public static int intValue(String name, int fallback) {
        String value = external(name);
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static String stringValue(String name, String fallback) {
        String value = external(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String external(String name) {
        String property = System.getProperty(name);
        if (property != null && !property.isBlank()) return property;
        String environment = System.getenv(name);
        return environment == null || environment.isBlank() ? null : environment;
    }
}
