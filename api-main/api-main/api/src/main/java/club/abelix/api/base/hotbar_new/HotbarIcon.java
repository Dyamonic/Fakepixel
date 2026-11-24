package club.abelix.api.base.hotbar_new;

import club.abelix.api.base.gamer.Gamer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public interface HotbarIcon {

    ItemStack getItem(@NotNull Gamer gamer);

    @Range(from = 0, to = 8)
    int slot();

    void onClick(@NotNull HotbarClickAction action);

}
