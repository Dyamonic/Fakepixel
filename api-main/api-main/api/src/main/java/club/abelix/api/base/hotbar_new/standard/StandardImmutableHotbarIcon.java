package club.abelix.api.base.hotbar_new.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar_new.ActionType;
import club.abelix.api.base.hotbar_new.HotbarClickAction;
import club.abelix.api.base.hotbar_new.HotbarIcon;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Setter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EqualsAndHashCode(of = "slot")
@RequiredArgsConstructor
public class StandardImmutableHotbarIcon implements HotbarIcon {

    int slot;
    Function<@NotNull Gamer, @NotNull ItemStack> itemSupplier;
    Map<ActionType, Consumer<HotbarClickAction>> actions;

    @Override
    public @NotNull ItemStack getItem(@NotNull Gamer gamer) {
        return itemSupplier.apply(gamer);
    }

    @Override
    public @Range(from = 0, to = 8) int slot() {
        return slot;
    }

    @Override
    public void onClick(@NotNull HotbarClickAction action) {
        val handler = actions.get(action.type());

        if (handler != null) {
            handler.accept(action);
        }
    }
}
