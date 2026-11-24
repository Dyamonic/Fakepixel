package club.abelix.api.server.base.shop;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ShopGuiFactory {

    void open(@NotNull Gamer gamer, @Nullable Gui parent);

}
