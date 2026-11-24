package club.abelix.games.fastbridge.cosmetic.item.block;

import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.language.Language;
import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.cosmetic.api.CosmeticItem;
import club.abelix.games.fastbridge.cosmetic.api.category.BlocksCategory;
import club.abelix.games.fastbridge.cosmetic.api.category.FastBridgeCategory;
import lombok.NonNull;
import lombok.val;
import net.minecraft.server.v1_8_R3.Item;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class BlocksCosmeticItem extends CosmeticItem {

    private Material chooseTool = Material.IRON_PICKAXE;

    protected BlocksCosmeticItem(int id, @NonNull String displayName, Rarity rarity, int coinPrice) {
        super(id, displayName, FastBridgeCategory.getCategory(BlocksCategory.class),
                rarity, coinPrice);

        try {
            icon(getItem());
        } catch (Exception __) {
        }
    }

    public static void init() {
        new WoolCosmeticItem(80_001, "white_wool", DyeColor.WHITE.getWoolData()).setDefault();
        new MaterialCosmeticItem(80_002, Material.COBBLESTONE, Rarity.COMMON, 1500);
        new MaterialCosmeticItem(80_003, Material.BRICK, Rarity.COMMON, 1500);
        new WoodCosmeticItem(80_004, "oak_planks", 0);
        new WoodCosmeticItem(80_005, "spruce_planks", 1);
        new WoodCosmeticItem(80_006, "birch_planks", 2);
        new WoolCosmeticItem(80_007, "orange_wool", DyeColor.ORANGE.getWoolData());
        new WoolCosmeticItem(80_008, "blue_wool", DyeColor.BLUE.getWoolData());
        new WoolCosmeticItem(80_009, "red_wool", DyeColor.RED.getWoolData());
        new WoolCosmeticItem(80_010, "pink_wool", DyeColor.PINK.getWoolData());
        new GlassCosmeticItem(80_011, "glass");
        new GlassCosmeticItem(80_012, "orange_glass", DyeColor.ORANGE.getWoolData());
        new GlassCosmeticItem(80_013, "blue_glass", DyeColor.BLUE.getWoolData());
        new GlassCosmeticItem(80_014, "red_glass", DyeColor.RED.getWoolData());
        new GlassCosmeticItem(80_015, "pink_glass", DyeColor.PINK.getWoolData());
        new MaterialCosmeticItem(80_016, Material.SANDSTONE, Rarity.COMMON, 0).free();
        new MaterialCosmeticItem(80_017, Material.OBSIDIAN, Rarity.LEGENDARY, 7500).chooseTool(Material.DIAMOND_PICKAXE);
        new MaterialCosmeticItem(80_018, Material.DIAMOND_BLOCK, Rarity.LEGENDARY, 7500).chooseTool(Material.DIAMOND_PICKAXE);
        new MaterialCosmeticItem(80_019, Material.GOLD_BLOCK, Rarity.EPIC, 5500).chooseTool(Material.DIAMOND_PICKAXE);
        new MaterialCosmeticItem(80_020, Material.IRON_BLOCK, Rarity.EPIC, 5500).chooseTool(Material.DIAMOND_PICKAXE);
        new MaterialCosmeticItem(80_021, Material.ENDER_STONE, Rarity.EPIC, 5500).chooseTool(Material.DIAMOND_PICKAXE);

    }

    public abstract ItemStack getItem();

    public Material chooseTool() {
        return chooseTool;
    }

    public BlocksCosmeticItem chooseTool(Material material) {
        this.chooseTool = material;
        return this;
    }

    @Override
    public void onSelect(@NonNull Player player) {
        FastBridgeGamer.get(player).resetItems(this);
    }

    public void setDefault() {
        free();

        FastBridgeCategory.getCategory(BlocksCategory.class).defaultItem(this);
    }

    @Override
    public String getDisplayName(@NonNull Language language) {
        val item = getItem();
        val nmsItem = CraftItemStack.asNMSCopy(item);
        if (nmsItem == null) return item.getType().name();

        Item nmsItemType = nmsItem.getItem();
        return "§e" + nmsItemType.a(nmsItem);
    }
}
