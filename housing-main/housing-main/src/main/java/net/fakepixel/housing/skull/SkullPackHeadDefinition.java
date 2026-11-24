package net.fakepixel.housing.skull;

public final class SkullPackHeadDefinition {
    private final String id, displayName, texture;
    public SkullPackHeadDefinition(String id, String displayName, String texture) {
        this.id=id; this.displayName=displayName; this.texture=texture;
    }
    public String getId(){return id;} public String getDisplayName(){return displayName;} public String getTexture(){return texture;}
}
