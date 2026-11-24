package club.abelix.games.fastbridge.cosmetic.api.gui;

import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.item.GameMenuCategory;
import club.abelix.api.base.item.gui.CategoryListGui;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-gamecosmetic</p>
 * <p>ModeCategoryListGui creation on 29.06.2023 at 22:24</p>
 */

public class ModeCategoryListGui extends CategoryListGui {

    public ModeCategoryListGui(@NonNull Player player, GameMenuCategory... categories) {
        super(player, categories);
    }

    public ModeCategoryListGui(@NonNull Player player, Gui parentGui, GameMenuCategory... categories) {
        super(player, parentGui, categories);
    }

    public ModeCategoryListGui(@NonNull Player player, int rowCount, GameMenuCategory... categories) {
        super(player, rowCount, categories);
    }

    public ModeCategoryListGui(@NonNull Player player, int rowCount, Gui parentGui, GameMenuCategory... categories) {
        super(player, rowCount, parentGui, categories);
    }

    public ModeCategoryListGui(@NonNull Player player, @NonNull Lang lang, @NonNull Collection<? extends GameMenuCategory> categories, int rowCount) {
        super(player, lang, categories, rowCount);
    }

    public ModeCategoryListGui(@NonNull Player player, @NonNull Lang lang, @NonNull Collection<? extends GameMenuCategory> categories, @Nullable Gui parentGui, int rowCount) {
        super(player, lang, categories, parentGui, rowCount);
    }

    public ModeCategoryListGui(@NonNull Player player, @NonNull String title, @NonNull Collection<? extends GameMenuCategory> categories, int rowCount) {
        super(player, title, categories, null, rowCount);
    }

    public ModeCategoryListGui(@NonNull Player player, @NonNull String title, @NonNull Collection<? extends GameMenuCategory> categories, @Nullable Gui parentGui, int rowCount) {
        super(player, title, categories, parentGui, rowCount);
    }

    public void basicButtons() {
        if (getParent() == null) {
            // Кристалик с ифой
            generateShopItem(50);
            // Закрыть нахуй
            generateCloseItem(49);
        } else {
            // Иконка назад
            add(ItemUtil.getBuilder(Material.ARROW).setName(
                    language.getMessage("game_cosmetic_back")
            ).addLore(
                    language.getMessageList("game_cosmetic_back_shop_lore",
                            "%master%", CoreUtil.getUncommon().getDisplayName())
            ).build(), 48, inventoryClickEvent -> getParent().reopen());
            // Кристалик с ифой
            generateShopItem(49);
        }
    }

    @Override
    public void draw() {
        super.draw();

        this.basicButtons();

        drawBorder(Gui.BORDER_SLOTS_FOR_SIX_ROWS_GUI);
    }
}
