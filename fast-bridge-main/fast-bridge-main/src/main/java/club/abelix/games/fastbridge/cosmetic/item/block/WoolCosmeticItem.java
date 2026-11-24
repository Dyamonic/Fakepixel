package club.abelix.games.fastbridge.cosmetic.item.block;

import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.NonNull;
import org.bukkit.Material;

public class WoolCosmeticItem extends ColorizedCosmeticItem {

    protected WoolCosmeticItem(int id, @NonNull String displayName, int dataValue) {
        this(id, displayName, (short) dataValue);
    }

    protected WoolCosmeticItem(int id, @NonNull String displayName, short dataValue) {
        super(id, displayName, Rarity.COMMON, 1500, Material.WOOL, dataValue);
    }

    @Override
    public Material chooseTool() {
        return Material.SHEARS;
    }
}
