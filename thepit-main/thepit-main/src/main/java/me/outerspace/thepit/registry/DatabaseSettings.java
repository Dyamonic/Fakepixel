package me.outerspace.thepit.registry;

public record DatabaseSettings(boolean enabled, String type, String host, int port, String database,
                               String username, String password, int poolSize, String source, String jdbcUrl) { }
