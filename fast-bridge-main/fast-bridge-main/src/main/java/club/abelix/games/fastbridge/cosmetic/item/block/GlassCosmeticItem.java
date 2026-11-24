package club.abelix.games.fastbridge.cosmetic.item.block;

import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class GlassCosmeticItem extends ColorizedCosmeticItem {

    protected GlassCosmeticItem(int id, @NonNull String displayName, int dataValue) {
        this(id, displayName, (short) dataValue);
    }

    protected GlassCosmeticItem(int id, @NonNull String displayName, short dataValue) {
        super(id, displayName, Rarity.RARE, 2500, Material.STAINED_GLASS, dataValue);

        icon(getItem());
    }

    protected GlassCosmeticItem(int id, @NonNull String displayName) {
        this(id, displayName, -1);
    }

    @Override
    public ItemStack getItem() {
        if (dataValue == -1) return new ItemStack(Material.GLASS);
        return new ItemStack(material, 1, dataValue);
    }

}