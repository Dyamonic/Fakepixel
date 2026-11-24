package club.abelix.games.fakevival.furniture;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

public record SmpModelPart(Material material, EntityType entityType, EquipmentSlot slot,
                           double x, double y, double z, float yaw, float pitch, float roll,
                           float scale, float scaleX, float scaleY, float scaleZ,
                           boolean visible, boolean small, boolean marker, boolean gravity,
                           boolean basePlate, boolean arms) {
    public enum EntityType { ARMOR_STAND, ITEM_DISPLAY, BLOCK_DISPLAY }

    public SmpModelPart(Material material, EntityType entityType, EquipmentSlot slot,
                        double x, double y, double z, float yaw, float pitch, float roll, float scale,
                        boolean visible, boolean small, boolean marker, boolean gravity,
                        boolean basePlate, boolean arms) {
        this(material, entityType, slot, x, y, z, yaw, pitch, roll, scale, scale, scale, scale,
                visible, small, marker, gravity, basePlate, arms);
    }

    public SmpModelPart {
        if (material == null || material.isAir()) material = Material.OAK_PLANKS;
        if (entityType == null) entityType = material.isBlock() ? EntityType.BLOCK_DISPLAY : EntityType.ITEM_DISPLAY;
        if (entityType == EntityType.BLOCK_DISPLAY && !material.isBlock()) entityType = EntityType.ITEM_DISPLAY;
        if (slot == null) slot = EquipmentSlot.HEAD;

        scale = clampScale(scale <= 0 ? 1f : scale);
        scaleX = clampScale(scaleX <= 0 ? scale : scaleX);
        scaleY = clampScale(scaleY <= 0 ? scale : scaleY);
        scaleZ = clampScale(scaleZ <= 0 ? scale : scaleZ);
    }

    public static SmpModelPart defaultPart(Material material) {
        return new SmpModelPart(material, material.isBlock() ? EntityType.BLOCK_DISPLAY : EntityType.ITEM_DISPLAY,
                EquipmentSlot.HEAD, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1,
                true, false, true, false, false, false);
    }

    public float maxScale() {
        return Math.max(scaleX, Math.max(scaleY, scaleZ));
    }

    private static float clampScale(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return 1f;
        return Math.max(0.01f, Math.min(8f, value));
    }
}
