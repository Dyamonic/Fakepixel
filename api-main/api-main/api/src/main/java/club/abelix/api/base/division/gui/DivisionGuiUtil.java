package club.abelix.api.base.division.gui;

import club.abelix.api.base.division.Division;
import club.abelix.api.base.division.DivisionManager;
import club.abelix.api.base.division.DivisionStar;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.item.ItemUtil;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>GuiUtil creation on 02.04.2024 at 21:48</p>
 */

@UtilityClass
public class DivisionGuiUtil {

    public ItemStack getDivisionIcon(@NonNull Division division, @NonNull Gamer gamer, boolean glowing) {
        return ItemUtil.getBuilder(Material.BOOK)
                .setName(division.getName(gamer))
                .setLore(division.getLore(gamer))
                .glowing(glowing)
                .build();
    }

    public ItemStack getStarIcon(@NonNull DivisionStar star, @NonNull Gamer gamer) {
        return ItemUtil.getBuilder(Material.NETHER_STAR)
                .setName(star.getDisplayName(gamer))
                .setLore(star.getLore(gamer))
                .glowing(star.equals(DivisionManager.IMP.getSelectedStar(gamer)))
                .build();

    }
}
