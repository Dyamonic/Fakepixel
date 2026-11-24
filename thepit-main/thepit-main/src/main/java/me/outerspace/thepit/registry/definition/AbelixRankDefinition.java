package me.outerspace.thepit.registry.definition;

import java.util.List;

public record AbelixRankDefinition(RankCategory category, String normalizedName, List<String> permissions) {
    public RankCategory getCategory() { return category; }
    public String getNormalizedName() { return normalizedName; }
    public List<String> getPermissions() { return permissions; }
}
