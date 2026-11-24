package club.abelix.commons.util;

import lombok.experimental.UtilityClass;
import lombok.val;

@UtilityClass
public final class EnvUtility {
    public String getEnvStringDefault(final String key, final String defaultValue) {
        return System.getenv().getOrDefault(key, defaultValue);
    }

    public boolean getEnvBooleanDefault(final String key, final String defaultValue) {
        return Boolean.parseBoolean(System.getenv().getOrDefault(key, defaultValue));
    }

    public int getEnvIntegerDefault(final String key, final String defaultValue) {
        return Integer.parseInt(System.getenv().getOrDefault(key, defaultValue));
    }

    public String getEnvStrict(final String key) {
        val environment = System.getenv();

        if (!environment.containsKey(key))
            throw new IllegalStateException("Environment doesn't contain " + key);

        return environment.get(key);
    }
}
