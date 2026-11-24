package me.outerspace.thepit.registry;

import me.outerspace.thepit.registry.definition.ArenaDefinition;
import me.outerspace.thepit.registry.definition.StoredLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ArenaRegistry extends RegistryNode {
    private final Map<String, ArenaDefinition> arenas;

    public ArenaRegistry(Map<String, Object> values) {
        super(values);
        Map<String, ArenaDefinition> built = new LinkedHashMap<>();
        for (String rawId : keys("arenas")) {
            String id = normalize(rawId);
            String base = "arenas." + rawId;
            String world = getString(base + ".world", rawId);
            built.put(id, new ArenaDefinition(id, getBoolean(base + ".enabled", true),
                    getString(base + ".display-name", id), world, getString(base + ".type", "normal"),
                    location(base + ".spawn", world), location(base + ".spawn-protection.pos1", world),
                    location(base + ".spawn-protection.pos2", world), location(base + ".pvp-arena.pos1", world),
                    location(base + ".pvp-arena.pos2", world)));
        }
        if (built.isEmpty()) throw new IllegalArgumentException("arenas.yml has no arenas");
        arenas = Collections.unmodifiableMap(built);
    }

    public Optional<ArenaDefinition> getArena(String id) { return Optional.ofNullable(arenas.get(normalize(id))); }
    public Set<String> arenaIds() { return arenas.keySet(); }
    public Map<String, ArenaDefinition> all() { return arenas; }

    private StoredLocation location(String path, String world) {
        if (getConfigurationSection(path) == null) return null;
        return new StoredLocation(getString(path + ".world", world), getDouble(path + ".x"), getDouble(path + ".y"),
                getDouble(path + ".z"), (float) getDouble(path + ".yaw"), (float) getDouble(path + ".pitch"));
    }

    private String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(java.util.Locale.ENGLISH).replaceAll("\\s+", "_");
    }
}
