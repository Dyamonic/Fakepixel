package me.outerspace.thepit.registry.definition;

import me.outerspace.thepit.registry.RegistryNode;

import java.util.List;

public record PerkDefinition(String id, boolean enabled, String displayName, String material, short data,
                             int menuSlot, List<String> lore, int coinPrice, int requiredLevel,
                             int requiredPrestige, int requiredPerkSlot,
                             RegistryNode settings) {
}
