package club.abelix.games.fastbridge.cosmetic.item.block;

import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public abstract class ColorizedCosmeticItem extends BlocksCosmeticItem {

    protected final short dataValue;
    protected final Material material;

    protected ColorizedCosmeticItem(int id, @NonNull String displayName, Rarity rarity, int coinPrice, Material material, int dataValue) {
        this(id, displayName, rarity, coinPrice, material, (short) dataValue);
    }

    protected ColorizedCosmeticItem(int id, @NonNull String displayName, Rarity rarity, int coinPrice, Material material, short dataValue) {
        super(id, displayName, rarity, coinPrice);

        this.dataValue = dataValue;
        this.material = material;

        icon(getItem());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(material, 1, dataValue);
    }


}
