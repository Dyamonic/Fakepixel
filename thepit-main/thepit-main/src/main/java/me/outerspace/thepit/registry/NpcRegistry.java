package me.outerspace.thepit.registry;

import me.outerspace.thepit.registry.definition.LocationDefinition;
import me.outerspace.thepit.registry.definition.NpcDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class NpcRegistry extends RegistryNode {
    private final Map<String, NpcDefinition> npcs;

    public NpcRegistry(Map<String, Object> values) {
        super(values);
        Map<String, NpcDefinition> built = new LinkedHashMap<>();
        for (String id : keys("npcs")) {
            String base = "npcs." + id;
            if (getConfigurationSection(base + ".location") == null) continue;
            String location = base + ".location";
            LocationDefinition value = new LocationDefinition(getString(location + ".world", getString(base + ".world", "")),
                    getDouble(location + ".x"), getDouble(location + ".y"), getDouble(location + ".z"),
                    (float) getDouble(location + ".yaw"), (float) getDouble(location + ".pitch"));
            built.put(id, new NpcDefinition(id, getBoolean(base + ".enabled", true), getString(base + ".name", "&bThe Pit"),
                    value, getString(base + ".skin-name", ""), getString(base + ".skin-value", ""),
                    getString(base + ".skin-signature", ""), getString(base + ".command", ""),
                    getBoolean(base + ".look-at-players", false)));
        }
        npcs = Collections.unmodifiableMap(built);
    }

    public Optional<NpcDefinition> getNpc(String id) { return Optional.ofNullable(npcs.get(id)); }
    public Map<String, NpcDefinition> all() { return npcs; }
}
