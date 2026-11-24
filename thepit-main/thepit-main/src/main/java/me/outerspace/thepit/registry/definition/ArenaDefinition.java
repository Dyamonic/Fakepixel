package me.outerspace.thepit.registry.definition;

public record ArenaDefinition(String id, boolean enabled, String displayName, String worldName, String worldType,
                              StoredLocation spawn, StoredLocation spawnProtectionPos1,
                              StoredLocation spawnProtectionPos2, StoredLocation pvpPos1,
                              StoredLocation pvpPos2) {
    public String getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public String getDisplayName() { return displayName; }
    public String getWorldName() { return worldName; }
    public String getWorldType() { return worldType; }
    public StoredLocation getSpawn() { return spawn; }
    public StoredLocation getSpawnProtectionPos1() { return spawnProtectionPos1; }
    public StoredLocation getSpawnProtectionPos2() { return spawnProtectionPos2; }
    public StoredLocation getPvpPos1() { return pvpPos1; }
    public StoredLocation getPvpPos2() { return pvpPos2; }
}
