package club.abelix.games.fakevival.registries;

import java.util.Locale;

public final class DatabaseRegistry {
    public static final boolean ENABLED = true;
    public static final String TYPE = "MARIADB";
    public static final String HOST = "localhost";
    public static final int PORT = 3306;
    public static final String DATABASE = "mmorpg";
    public static final String USER = "username";
    public static final String PASSWORD = "password";
    public static final int POOL_SIZE = 5;
    public static final boolean USE_SSL = false;
    public static final String DATA_TABLE = "data";
    public static final String LIVE_CONTROLS_TABLE = "smp_live_controls";
    public static final String DISCORD_LINKS_TABLE = "smp_discord_links";
    public static final String PLAYER_NETWORTH_CACHE_TABLE = "smp_player_networth_cache";
    public static final String EVENT_NOTIFICATIONS_TABLE = "smp_event_notifications";

    private DatabaseRegistry() {
    }

    public static String value(String key) {
        if (key == null) return null;
        return switch (key.toUpperCase(Locale.ROOT)) {
            case "SMP_DATABASE_ENABLED" -> String.valueOf(ENABLED);
            case "SMP_DATABASE_TYPE" -> TYPE;
            case "SMP_DATABASE_HOST" -> HOST;
            case "SMP_DATABASE_PORT" -> String.valueOf(PORT);
            case "SMP_DATABASE_NAME" -> DATABASE;
            case "SMP_DATABASE_USER" -> USER;
            case "SMP_DATABASE_PASSWORD" -> PASSWORD;
            case "SMP_DATABASE_POOL_SIZE" -> String.valueOf(POOL_SIZE);
            case "SMP_DATABASE_USE_SSL" -> String.valueOf(USE_SSL);
            case "SMP_DATABASE_TABLE" -> DATA_TABLE;
            case "SMP_DATABASE_LIVE_CONTROLS_TABLE" -> LIVE_CONTROLS_TABLE;
            default -> null;
        };
    }
}
