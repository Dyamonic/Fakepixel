package club.abelix.api.server.base.shop_new;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.commons.language.Language;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ShopCategory {

    @NotNull String getNameKey();

    @NotNull ItemStack getIcon(@NotNull Language language);

    void open(@NotNull Gamer gamer, @Nullable Gui prev);

}
