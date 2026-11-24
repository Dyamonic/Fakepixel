package club.abelix.games.fastbridge.cosmetic.item.block;

import club.abelix.commons.data.cases.rarity.Rarity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MaterialCosmeticItem extends BlocksCosmeticItem {

    private final Material material;


    protected MaterialCosmeticItem(int id, Material material, Rarity rarity, int cost) {
        super(id, material.name().toLowerCase(), rarity, cost);

        this.material = material;
        icon(getItem());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(material);
    }

}
