package me.outerspace.thepit.registry;

import me.outerspace.thepit.registry.definition.PerkDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PerkRegistry extends RegistryNode {
    private final Map<String, PerkDefinition> perks;

    public PerkRegistry(Map<String, Object> values) {
        super(values);
        Map<String, PerkDefinition> built = new LinkedHashMap<>();
        for (String rawId : keys("perks")) {
            String id = normalize(rawId);
            String base = "perks." + rawId;
            RegistryNode settings = section(base).orElse(new RegistryNode(Collections.emptyMap()));
            java.util.List<String> description = getStringList(base + ".description");
            if (description.isEmpty()) description = getStringList(base + ".lore");
            built.put(id, new PerkDefinition(id, getBoolean(base + ".enabled", true),
                    getString(base + ".display-name", id), getString(base + ".material", "PAPER"),
                    (short) getInt(base + ".data", 0), getInt(base + ".menu-slot", -1),
                    description, Math.max(0, getInt(base + ".price", getInt(base + ".coin-price", 0))),
                    Math.max(1, getInt(base + ".required-level", 1)),
                    Math.max(0, getInt(base + ".required-prestige", 0)),
                    Math.max(1, Math.min(4, getInt(base + ".required-perk-slot", 1))), settings));
        }
        perks = Collections.unmodifiableMap(built);
    }

    public Optional<PerkDefinition> getPerk(String id) { return Optional.ofNullable(perks.get(normalize(id))); }
    public Set<String> perkIds() { return perks.keySet(); }
    public Map<String, PerkDefinition> all() { return perks; }
    public String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ENGLISH).replace('-', '_').replace(' ', '_');
    }
}
