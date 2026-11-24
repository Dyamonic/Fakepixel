package me.outerspace.thepit.registry.definition;

public record NpcDefinition(String id, boolean enabled, String name, LocationDefinition location,
                            String skinName, String skinValue, String skinSignature,
                            String command, boolean lookAtPlayers) {
    public String getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public String getName() { return name; }
    public LocationDefinition getLocation() { return location; }
    public String getSkinName() { return skinName; }
    public String getSkinValue() { return skinValue; }
    public String getSkinSignature() { return skinSignature; }
    public String getCommand() { return command; }
    public boolean isLookAtPlayers() { return lookAtPlayers; }
}
