package me.outerspace.thepit.registry;

import java.util.Map;

public final class SettingsRegistry extends RegistryNode {
    public SettingsRegistry(Map<String, Object> values) { super(values); }
    public int voidY() { return getInt("settings.void-y", 55); }
    public String spawnWorld() { return getString("settings.spawn-world", "buildffa_sky"); }
}
