package club.abelix.games.fastbridge.cosmetic.item.block;

import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.NonNull;
import org.bukkit.Material;

public class WoodCosmeticItem extends ColorizedCosmeticItem {

    protected WoodCosmeticItem(int id, @NonNull String displayName, int dataValue) {
        this(id, displayName, (short) dataValue);
    }

    protected WoodCosmeticItem(int id, @NonNull String displayName, short dataValue) {
        super(id, displayName, Rarity.RARE, 2500, Material.WOOD, dataValue);
    }

    @Override
    public Material chooseTool() {
        return Material.IRON_AXE;
    }
}
