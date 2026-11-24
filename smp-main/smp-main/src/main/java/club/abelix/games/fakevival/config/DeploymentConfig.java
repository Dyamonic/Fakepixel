package club.abelix.games.fakevival.config;

import club.abelix.games.fakevival.registries.DatabaseRegistry;
import club.abelix.games.fakevival.registries.WebhookRegistry;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Optional;

public final class DeploymentConfig {
    private DeploymentConfig() {
    }

    public static DatabaseSettings database(JavaPlugin plugin) {
        ResolvedValue enabledValue = resolve("SMP_DATABASE_ENABLED");
        boolean enabled = Boolean.parseBoolean(enabledValue.value());
        ResolvedValue typeValue = resolve("SMP_DATABASE_TYPE");
        String type = valueOr(typeValue.value(), DatabaseRegistry.TYPE).toUpperCase(Locale.ROOT);
        plugin.getLogger().info("SMP database mode enabled=" + enabled + " (source: " + enabledValue.source() + ")");
        if (!enabled) {
            plugin.getLogger().warning("SMP database is disabled. Set SMP_DATABASE_ENABLED=true to use MariaDB/MySQL persistence.");
            return DatabaseSettings.disabled();
        }

        ResolvedValue hostValue = resolve("SMP_DATABASE_HOST");
        ResolvedValue portValue = resolve("SMP_DATABASE_PORT");
        ResolvedValue nameValue = resolve("SMP_DATABASE_NAME");
        ResolvedValue userValue = resolve("SMP_DATABASE_USER");
        ResolvedValue passwordValue = resolve("SMP_DATABASE_PASSWORD");
        ResolvedValue poolSizeValue = resolve("SMP_DATABASE_POOL_SIZE");
        ResolvedValue useSslValue = resolve("SMP_DATABASE_USE_SSL");
        ResolvedValue dataTableValue = resolve("SMP_DATABASE_TABLE");
        ResolvedValue liveControlsTableValue = resolve("SMP_DATABASE_LIVE_CONTROLS_TABLE");
        String host = blankToNull(hostValue.value());
        String name = blankToNull(nameValue.value());
        String user = blankToNull(userValue.value());
        String password = blankToNull(passwordValue.value());
        if (host == null || name == null || user == null || password == null) {
            String message = "SMP_DATABASE_ENABLED=true but one or more required database settings are missing: "
                    + "SMP_DATABASE_HOST, SMP_DATABASE_NAME, SMP_DATABASE_USER, SMP_DATABASE_PASSWORD.";
            plugin.getLogger().severe(message);
            throw new IllegalStateException(message);
        }
        plugin.getLogger().info("SMP database settings sources: type=" + typeValue.source()
                + ", host=" + hostValue.source()
                + ", port=" + portValue.source()
                + ", name=" + nameValue.source()
                + ", user=" + userValue.source()
                + ", password=" + passwordValue.source()
                + ", poolSize=" + poolSizeValue.source()
                + ", useSsl=" + useSslValue.source()
                + ", dataTable=" + dataTableValue.source()
                + ", liveControlsTable=" + liveControlsTableValue.source());
        plugin.getLogger().info("SMP database target: " + type + " " + host + ":" + parseInt(portValue.value(), DatabaseRegistry.PORT)
                + "/" + name + " as user " + user + " (password hidden)");

        return new DatabaseSettings(
                true,
                type,
                host,
                parseInt(portValue.value(), DatabaseRegistry.PORT),
                name,
                user,
                password,
                parseInt(poolSizeValue.value(), DatabaseRegistry.POOL_SIZE),
                Boolean.parseBoolean(valueOr(useSslValue.value(), String.valueOf(DatabaseRegistry.USE_SSL))),
                valueOr(dataTableValue.value(), DatabaseRegistry.DATA_TABLE),
                valueOr(liveControlsTableValue.value(), DatabaseRegistry.LIVE_CONTROLS_TABLE)
        );
    }

    public static Optional<String> webhook(String key) {
        return webhookEnvName(key)
                .flatMap(DeploymentConfig::optionalExternal)
                .or(() -> WebhookRegistry.defaultWebhookUrl(key));
    }

    public static Optional<String> webhookEnvName(String key) {
        return WebhookRegistry.variableName(key);
    }

    private static String required(String name) {
        return blankToNull(resolve(name).value());
    }

    private static Optional<String> optional(String name) {
        String value = resolve(name).value();
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static Optional<String> optionalExternal(String name) {
        String property = System.getProperty(name);
        if (property != null && !property.isBlank()) return Optional.of(property);
        String environment = System.getenv(name);
        return environment == null || environment.isBlank() ? Optional.empty() : Optional.of(environment);
    }

    private static String string(String name, String fallback) {
        String value = resolve(name).value();
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean bool(String name, boolean fallback) {
        String value = resolve(name).value();
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value);
    }

    private static int integer(String name, int fallback) {
        return parseInt(resolve(name).value(), fallback);
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static ResolvedValue resolve(String name) {
        String property = System.getProperty(name);
        if (property != null && !property.isBlank()) return new ResolvedValue(property, "system property");
        String environment = System.getenv(name);
        if (environment != null && !environment.isBlank()) return new ResolvedValue(environment, "environment variable");
        String registry = DatabaseRegistry.value(name);
        if (registry != null && !registry.isBlank()) return new ResolvedValue(registry, "DatabaseRegistry");
        return new ResolvedValue(null, "unset");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record ResolvedValue(String value, String source) {
    }

    public record DatabaseSettings(boolean enabled, String type, String host, int port, String database, String user,
                                   String password, int poolSize, boolean useSsl, String table,
                                   String liveControlsTable) {
        public static DatabaseSettings disabled() {
            return new DatabaseSettings(false, "YAML", "", 3306, "", "", "", 0, false, "data", "smp_live_controls");
        }
    }
}
