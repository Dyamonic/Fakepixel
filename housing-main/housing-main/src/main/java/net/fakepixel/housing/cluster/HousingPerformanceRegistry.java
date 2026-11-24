package net.fakepixel.housing.cluster;

/** Central, code-reviewed operational limits shared by every Housing server. */
public final class HousingPerformanceRegistry {
    public static final int HEARTBEAT_SECONDS = 5;
    public static final int INSTANCE_HEALTH_TIMEOUT_SECONDS = 20;
    public static final int PENDING_ROUTE_SECONDS = 45;
    public static final int HOUSE_LOCK_SECONDS = 60;
    public static final int EMPTY_HOUSE_UNLOAD_SECONDS = 120;
    public static final int BROWSE_CACHE_REFRESH_SECONDS = 15;
    public static final int MAX_ACTIVE_THEME_PASTES = 2;
    public static final int THEME_BLOCKS_PER_TICK = 5000;
    public static final int MAX_NPCS_PER_HOUSE = 50;
    public static final int MAX_HOLOGRAMS_PER_HOUSE = 50;
    public static final int MAX_LOADED_HOUSES = 25;
    private HousingPerformanceRegistry() {}
}
