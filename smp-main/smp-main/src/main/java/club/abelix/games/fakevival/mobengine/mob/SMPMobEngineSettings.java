package club.abelix.games.fakevival.mobengine.mob;

public final class SMPMobEngineSettings {
    public static final double FULL_VISIBILITY_RANGE = 32.0D;
    public static final double REDUCED_VISIBILITY_RANGE = 64.0D;
    public static final double HIDDEN_VISIBILITY_RANGE = 96.0D;
    public static final double NAMETAG_RANGE = 48.0D;
    public static final int NAMETAG_UPDATE_INTERVAL_TICKS = 15;
    public static final int FAR_NAMETAG_UPDATE_INTERVAL_TICKS = 40;
    public static final int MAX_VISIBLE_NORMAL_MOBS_PER_PLAYER = 45;
    public static final int MAX_VISIBLE_IMPORTANT_MOBS_PER_PLAYER = 8;
    public static final int MAX_COMMAND_SPAWNS_PER_TICK = 8;
    public static final int HIT_PARTICLES_PER_PLAYER_PER_SECOND = 40;
    public static final int NORMAL_TRACKING_RANGE_BLOCKS = 48;
    public static final int IMPORTANT_TRACKING_RANGE_BLOCKS = 80;

    private SMPMobEngineSettings() {
    }
}
