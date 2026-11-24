package club.abelix.games.fastbridge.cosmetic.api.category;

import club.abelix.commons.util.word.CommonWords;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class BlocksCategory extends FastBridgeCategory {
    public BlocksCategory() {
        super("BLOCKS", CommonWords.ITEM);
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Material.WOOL);
    }

}
