package club.abelix.api.base.hotbar_new.builder;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar_new.ActionType;
import club.abelix.api.base.hotbar_new.HotbarClickAction;
import club.abelix.api.base.hotbar_new.HotbarIcon;
import club.abelix.api.base.hotbar_new.builder.standard.StandardHotbarIconBuilder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public interface HotbarIconBuilder {

    static HotbarIconBuilder create(int slot) {
        return new StandardHotbarIconBuilder(slot);
    }

    HotbarIconBuilder item(ItemStack itemStack);

    HotbarIconBuilder itemSupplier(Function<Gamer, ItemStack> itemStackFunction);

    HotbarIconBuilder on(ActionType actionType, @NotNull Consumer<HotbarClickAction> consumer);

    HotbarIconBuilder onAny(@NotNull Consumer<HotbarClickAction> consumer);

    HotbarBuilder done();

    HotbarIcon asIcon();
}
