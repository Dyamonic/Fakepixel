package club.abelix.games.fastbridge.cosmetic.api.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.item.GameItemCategory;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.games.fastbridge.cosmetic.api.category.FastBridgeCategory;
import lombok.NonNull;

import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: GameCosmetics-NEW</p>
 * <p>GameCosmeticCategoryList creation on 07.02.2023 at 00:16</p>
 */

public class CosmeticCategoryList extends ModeCategoryListGui {

    public static final int[][] CATEGORY_SLOTS = new int[][]{
            {22},
            {21, 23},
            {12, 14, 31},
            {12, 14, 30, 32},
            {11, 13, 15, 30, 32},
            {11, 13, 15, 29, 31, 33},
            {10, 12, 14, 16, 29, 31, 33},
            {10, 12, 14, 16, 28, 30, 32, 34},
            {9, 11, 13, 15, 17, 28, 30, 32, 34},
            {9, 11, 13, 15, 17, 27, 29, 31, 33, 35},
            {9, 11, 13, 15, 17, 27, 29, 31, 33, 35, 22},
    };

    public CosmeticCategoryList(@NonNull Gamer gamer) {
        super(gamer.getPlayer(),
                gamer.getLanguage().getMessage(
                        "fast_bridge_cosmetic_gui_title", "%uncommon%",
                        CoreUtil.getUncommon().getDisplayName()
                ), FastBridgeCategory.CATEGORIES, 6);
    }

    public CosmeticCategoryList(@NonNull Gamer gamer, @NonNull Gui parentGui) {
        super(gamer.getPlayer(),
                gamer.getLanguage().getMessage(
                        "fast_bridge_cosmetic_gui_title", "%uncommon%",
                        CoreUtil.getUncommon().getDisplayName()
                ), FastBridgeCategory.CATEGORIES, parentGui, 6);
    }

    @Override
    public void draw() {
        List<FastBridgeCategory> categories = FastBridgeCategory.CATEGORIES;

        int[] slots = CATEGORY_SLOTS[categories.size() - 1];

        for (int i = 0; i < slots.length; i++) {
            GameItemCategory category = categories.get(i);
            addIcon(category, slots[i]);
        }

        this.basicButtons();
        drawBorder(Gui.BORDER_SLOTS_FOR_SIX_ROWS_GUI);
    }
}
