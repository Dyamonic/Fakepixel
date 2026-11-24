package club.abelix.api.base.item.impl.perk;

import club.abelix.api.base.item.GameItemCategory;
import club.abelix.commons.util.word.CommonWords;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Created by saharNooby on 19:56 18.10.2017
 */
public class PerkCategory extends GameItemCategory {

    public PerkCategory(@NonNull String id) {
        this(id, new ItemStack(Material.BLAZE_POWDER));
    }

    public PerkCategory(@NonNull String id, @NonNull ItemStack icon) {
        super(id, CommonWords.PERK, icon, true);
    }

}
