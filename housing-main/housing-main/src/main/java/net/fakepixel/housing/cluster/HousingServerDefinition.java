package net.fakepixel.housing.cluster;

public final class HousingServerDefinition {
    private final String id;
    private final HousingServerRole role;

    public HousingServerDefinition(String id, HousingServerRole role) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("server id is required");
        if (role == null) throw new IllegalArgumentException("server role is required");
        this.id = id;
        this.role = role;
    }

    public String getId() { return id; }
    public HousingServerRole getRole() { return role; }
    public boolean isLobby() { return role == HousingServerRole.LOBBY; }
    public boolean isInstance() { return role == HousingServerRole.INSTANCE; }
}
