package club.abelix.core.velocity.util;

import club.abelix.commons.util.EnvUtility;

import java.util.Locale;

public final class Constants {
    public final static boolean UNKNOWN_HOST = EnvUtility.getEnvBooleanDefault("unknown_hosts", "false");

    public final static String PROXY_BRAND = EnvUtility.getEnvStringDefault("brand", "AbelixProxy");
    public final static boolean PROXY_BRAND_AUTOGEN = EnvUtility.getEnvBooleanDefault("brand_autogen", "false");
    public final static String GAME_BRAND = EnvUtility.getEnvStringDefault("game_brand", "Abelix");
    public final static Boolean GAME_BRAND_AUTOGEN = EnvUtility.getEnvBooleanDefault("game_brand_autogen", "false");

    public final static boolean IS_ALPHA = EnvUtility.getEnvBooleanDefault("alpha", "false");
    public final static String REGION = EnvUtility.getEnvStringDefault("proxy_region", "eu")
            .toUpperCase(Locale.ROOT);
}
