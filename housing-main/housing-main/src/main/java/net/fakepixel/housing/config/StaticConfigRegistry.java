package net.fakepixel.housing.config;

public final class StaticConfigRegistry {
    private static final HousingDefaults DEFAULTS = new HousingDefaults();
    private StaticConfigRegistry() {}
    public static HousingDefaults defaults() { return DEFAULTS; }
}
