package net.fakepixel.housing.util;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

public final class Compat {
    private Compat() {}

    public static Material mat(String modern) {
        if (modern == null) return net.fakepixel.housing.util.Compat.mat("STONE");
        Material direct = Material.getMaterial(modern);
        if (direct != null) return direct;
        String n = modern.toUpperCase();
        if (n.endsWith("_WOOL")) return net.fakepixel.housing.util.Compat.mat("WOOL");
        if (n.endsWith("_CARPET")) return net.fakepixel.housing.util.Compat.mat("CARPET");
        if (n.endsWith("_STAINED_GLASS")) return net.fakepixel.housing.util.Compat.mat("STAINED_GLASS");
        if (n.endsWith("_TERRACOTTA") || n.endsWith("_GLAZED_TERRACOTTA")) return net.fakepixel.housing.util.Compat.mat("STAINED_CLAY");
        if (n.endsWith("_CONCRETE")) return net.fakepixel.housing.util.Compat.mat("STAINED_CLAY");
        if (n.endsWith("_PLANKS")) return net.fakepixel.housing.util.Compat.mat("WOOD");
        if (n.endsWith("_LOG") || n.startsWith("STRIPPED_")) return net.fakepixel.housing.util.Compat.mat("LOG");
        if (n.endsWith("_SLAB")) return net.fakepixel.housing.util.Compat.mat("STEP");
        if (n.endsWith("_STAIRS")) return net.fakepixel.housing.util.Compat.mat("WOOD_STAIRS");
        if (n.endsWith("_FENCE")) return net.fakepixel.housing.util.Compat.mat("FENCE");
        if (n.endsWith("_FENCE_GATE")) return net.fakepixel.housing.util.Compat.mat("FENCE_GATE");
        if (n.endsWith("_DOOR")) return net.fakepixel.housing.util.Compat.mat("WOOD_DOOR");
        if (n.endsWith("_TRAPDOOR")) return net.fakepixel.housing.util.Compat.mat("TRAP_DOOR");
        if (n.endsWith("_BUTTON")) return net.fakepixel.housing.util.Compat.mat("STONE_BUTTON");
        if (n.endsWith("_PRESSURE_PLATE")) return net.fakepixel.housing.util.Compat.mat("STONE_PLATE");
        if (n.endsWith("_SIGN")) return net.fakepixel.housing.util.Compat.mat("SIGN");
        switch (n) {
            case "PLAYER_HEAD": return net.fakepixel.housing.util.Compat.mat("SKULL_ITEM");
            case "WOODEN_AXE": return net.fakepixel.housing.util.Compat.mat("WOOD_AXE");
            case "GRASS_BLOCK": return net.fakepixel.housing.util.Compat.mat("GRASS");
            case "CRAFTING_TABLE": return net.fakepixel.housing.util.Compat.mat("WORKBENCH");
            case "ENCHANTING_TABLE": return net.fakepixel.housing.util.Compat.mat("ENCHANTMENT_TABLE");
            case "COMPARATOR": return net.fakepixel.housing.util.Compat.mat("REDSTONE_COMPARATOR");
            case "REDSTONE_TORCH": return net.fakepixel.housing.util.Compat.mat("REDSTONE_TORCH_ON");
            case "REDSTONE_LAMP": return net.fakepixel.housing.util.Compat.mat("REDSTONE_LAMP_OFF");
            case "FILLED_MAP": return net.fakepixel.housing.util.Compat.mat("MAP");
            case "WRITABLE_BOOK": return net.fakepixel.housing.util.Compat.mat("BOOK_AND_QUILL");
            case "COBWEB": return net.fakepixel.housing.util.Compat.mat("WEB");
            case "HAY_BLOCK": return net.fakepixel.housing.util.Compat.mat("HAY_BLOCK");
            case "END_STONE": return net.fakepixel.housing.util.Compat.mat("ENDER_STONE");
            case "NETHER_BRICKS": return net.fakepixel.housing.util.Compat.mat("NETHER_BRICK");
            case "BRICKS": return net.fakepixel.housing.util.Compat.mat("BRICK");
            case "STONE_BRICKS": return net.fakepixel.housing.util.Compat.mat("SMOOTH_BRICK");
            case "MOSSY_STONE_BRICKS": return net.fakepixel.housing.util.Compat.mat("SMOOTH_BRICK");
            case "CRACKED_STONE_BRICKS": return net.fakepixel.housing.util.Compat.mat("SMOOTH_BRICK");
            case "CHISELED_STONE_BRICKS": return net.fakepixel.housing.util.Compat.mat("SMOOTH_BRICK");
            case "QUARTZ_SLAB": return net.fakepixel.housing.util.Compat.mat("STEP");
            case "SMOOTH_STONE_SLAB": return net.fakepixel.housing.util.Compat.mat("STEP");
            case "RED_SANDSTONE_SLAB": return net.fakepixel.housing.util.Compat.mat("STEP");
            case "SANDSTONE_SLAB": return net.fakepixel.housing.util.Compat.mat("STEP");
            case "COBBLESTONE_SLAB": return net.fakepixel.housing.util.Compat.mat("STEP");
            case "BRICK_SLAB": return net.fakepixel.housing.util.Compat.mat("STEP");
            case "STONE_BRICK_SLAB": return net.fakepixel.housing.util.Compat.mat("STEP");
            case "COBBLESTONE_WALL": return net.fakepixel.housing.util.Compat.mat("COBBLE_WALL");
            case "STONE_BRICK_WALL": return net.fakepixel.housing.util.Compat.mat("COBBLE_WALL");
            case "BRICK_WALL": return net.fakepixel.housing.util.Compat.mat("COBBLE_WALL");
            case "MOSSY_COBBLESTONE_WALL": return net.fakepixel.housing.util.Compat.mat("COBBLE_WALL");
            case "RED_BED": return net.fakepixel.housing.util.Compat.mat("BED");
            case "BEETROOT_SEEDS": return net.fakepixel.housing.util.Compat.mat("SEEDS");
            case "FARMLAND": return net.fakepixel.housing.util.Compat.mat("SOIL");
            case "MELON": return net.fakepixel.housing.util.Compat.mat("MELON_BLOCK");
            case "PUFFERFISH": return net.fakepixel.housing.util.Compat.mat("RAW_FISH");
            case "LANTERN": return net.fakepixel.housing.util.Compat.mat("TORCH");
            case "SOUL_LANTERN": return net.fakepixel.housing.util.Compat.mat("TORCH");
            case "SOUL_TORCH": return net.fakepixel.housing.util.Compat.mat("TORCH");
            case "SOUL_FIRE": return net.fakepixel.housing.util.Compat.mat("FIRE");
            case "SEA_LANTERN": return net.fakepixel.housing.util.Compat.mat("GLOWSTONE");
            case "BARREL": return net.fakepixel.housing.util.Compat.mat("CHEST");
            case "BLAST_FURNACE": return net.fakepixel.housing.util.Compat.mat("FURNACE");
            case "SMOKER": return net.fakepixel.housing.util.Compat.mat("FURNACE");
            case "LECTERN": return net.fakepixel.housing.util.Compat.mat("BOOKSHELF");
            case "BLUE_ICE": return net.fakepixel.housing.util.Compat.mat("PACKED_ICE");
            case "CRYING_OBSIDIAN": return net.fakepixel.housing.util.Compat.mat("OBSIDIAN");
            case "MOSS_BLOCK": return net.fakepixel.housing.util.Compat.mat("MOSSY_COBBLESTONE");
            case "MOSS_CARPET": return net.fakepixel.housing.util.Compat.mat("CARPET");
            case "SPORE_BLOSSOM": return net.fakepixel.housing.util.Compat.mat("RED_ROSE");
            case "OAK_PRESSURE_PLATE": return net.fakepixel.housing.util.Compat.mat("WOOD_PLATE");
            case "HEAVY_WEIGHTED_PRESSURE_PLATE": return net.fakepixel.housing.util.Compat.mat("IRON_PLATE");
            case "LIGHT_WEIGHTED_PRESSURE_PLATE": return net.fakepixel.housing.util.Compat.mat("GOLD_PLATE");
            case "JACK_O_LANTERN": return net.fakepixel.housing.util.Compat.mat("JACK_O_LANTERN");
            default: return net.fakepixel.housing.util.Compat.mat("STONE");
        }
    }

    public static void setGameRule(World world, String key, String value) {
        try { world.setGameRuleValue(key, value); } catch (Throwable ignored) {}
    }

    public static ItemStack dye(int data) {
        return new ItemStack(Material.INK_SACK, 1, (short) data);
    }

    public static ItemStack limeDye() { return dye(10); }
    public static ItemStack grayDye() { return dye(8); }
    public static ItemStack redDye() { return dye(1); }
    public static ItemStack lightGrayDye() { return dye(7); }
    public static ItemStack greenDye() { return dye(2); }
}
