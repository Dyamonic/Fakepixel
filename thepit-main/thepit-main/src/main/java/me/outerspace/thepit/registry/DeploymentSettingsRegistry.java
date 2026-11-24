package me.outerspace.thepit.registry;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DeploymentSettingsRegistry extends RegistryNode {
    private static final Pattern MYSQL_URL = Pattern.compile("^jdbc:mysql://([^/:?]+)(?::([^/?]+))?/([^?]+)(?:\\?.*)?$");
    private volatile DatabaseSettings database;

    public DeploymentSettingsRegistry(Map<String, Object> values) {
        super(values);
        refreshEnvironment();
    }

    public void refreshEnvironment() {
        boolean enabled = bool("THEPIT_DATABASE_ENABLED", "BUILDFFA_DATABASE_ENABLED", getBoolean("database.enabled", false));
        String type = setting("database.type", "MYSQL", "THEPIT_DATABASE_TYPE", "BUILDFFA_DATABASE_TYPE", "MYSQL_TYPE").value().toUpperCase(Locale.ENGLISH);
        Setting urlSetting = setting("database.jdbc-url", "", "THEPIT_DATABASE_JDBC_URL", "BUILDFFA_DATABASE_JDBC_URL", "JDBC_URL");
        Setting hostSetting = setting("database.host", "localhost", "THEPIT_DATABASE_HOST", "BUILDFFA_DATABASE_HOST", "MYSQL_HOST", "DB_HOST");
        Setting portSetting = setting("database.port", "3306", "THEPIT_DATABASE_PORT", "BUILDFFA_DATABASE_PORT", "MYSQL_PORT", "DB_PORT");
        Setting nameSetting = setting("database.name", "buildffa", "THEPIT_DATABASE_NAME", "BUILDFFA_DATABASE_NAME", "MYSQL_DATABASE", "DB_NAME");
        String host;
        int port;
        String name;
        String source;
        String jdbcUrl;
        if (!urlSetting.value().isEmpty()) {
            Matcher matcher = MYSQL_URL.matcher(urlSetting.value());
            if (!matcher.matches()) throw new IllegalArgumentException(urlSetting.source() + " must be a valid jdbc:mysql://host:port/database URL");
            host = matcher.group(1);
            port = mysqlPort(urlSetting.source(), matcher.group(2) == null ? "3306" : matcher.group(2));
            name = matcher.group(3);
            source = urlSetting.source();
            jdbcUrl = urlSetting.value();
        } else {
            host = hostSetting.value();
            if (host.isEmpty() || host.contains(":")) throw new IllegalArgumentException(hostSetting.source() + " must contain only the MySQL hostname without a port");
            port = mysqlPort(portSetting.source(), portSetting.value());
            name = nameSetting.value();
            if (name.isEmpty()) throw new IllegalArgumentException(nameSetting.source() + " must contain the MySQL database name");
            source = hostSetting.source() + "," + portSetting.source() + "," + nameSetting.source();
            jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + name + "?useSSL=false&autoReconnect=true";
        }
        String username = env("THEPIT_DATABASE_USERNAME", "BUILDFFA_DATABASE_USERNAME", getString("database.username", ""));
        String password = env("THEPIT_DATABASE_PASSWORD", "BUILDFFA_DATABASE_PASSWORD", getString("database.password", ""));
        int poolSize = integer("THEPIT_DATABASE_POOL_SIZE", "BUILDFFA_DATABASE_POOL_SIZE", getInt("database.pool-size", 4), 1, 64);
        database = new DatabaseSettings(enabled, type, host, port, name, username, password, poolSize, source, jdbcUrl);
    }

    public DatabaseSettings database() { return database; }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String env(String key, String legacyKey, String fallback) {
        return env(key, env(legacyKey, fallback));
    }

    private static boolean bool(String key, String legacyKey, boolean fallback) {
        String value = env(key, legacyKey, "");
        return value.isEmpty() ? fallback : value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("yes");
    }

    private static int integer(String key, String legacyKey, int fallback, int min, int max) {
        try { return Math.clamp(Integer.parseInt(env(key, legacyKey, String.valueOf(fallback))), min, max); }
        catch (Exception ignored) { return fallback; }
    }

    private static int mysqlPort(String source, String value) {
        if (!value.matches("[0-9]+")) {
            throw new IllegalArgumentException(source + " must be one numeric MySQL port (for example 3306), not '" + value + "'");
        }
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) throw new NumberFormatException();
            return port;
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException(source + " must be a MySQL port from 1 through 65535, not '" + value + "'");
        }
    }

    private Setting setting(String path, String fallback, String... names) {
        String value = getString(path, fallback).trim();
        String source = "plugin-data/database.yml:" + path;
        for (int index = names.length - 1; index >= 0; index--) {
            String environment = System.getenv(names[index]);
            if (environment != null && !environment.trim().isEmpty()) {
                value = environment.trim();
                source = names[index];
            }
        }
        String property = System.getProperty("thepit." + path);
        if (property != null && !property.trim().isEmpty()) {
            value = property.trim();
            source = "-Dthepit." + path;
        }
        return new Setting(source, value);
    }

    private record Setting(String source, String value) { }
}
