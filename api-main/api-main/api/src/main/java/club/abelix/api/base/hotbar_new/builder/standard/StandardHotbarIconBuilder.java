package club.abelix.api.base.hotbar_new.builder.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar_new.ActionType;
import club.abelix.api.base.hotbar_new.HotbarClickAction;
import club.abelix.api.base.hotbar_new.HotbarIcon;
import club.abelix.api.base.hotbar_new.builder.HotbarBuilder;
import club.abelix.api.base.hotbar_new.builder.HotbarIconBuilder;
import club.abelix.api.base.hotbar_new.standard.StandardImmutableHotbarIcon;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.function.Consumer;
import java.util.function.Function;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class StandardHotbarIconBuilder implements HotbarIconBuilder {

    final HotbarBuilder parent;
    final int slot;

    public StandardHotbarIconBuilder(HotbarBuilder parent, int slot) {
        this.parent = parent;
        this.slot = slot;
    }

    public StandardHotbarIconBuilder(int slot) {
        this.slot = slot;
        this.parent = null;
    }

    Function<Gamer, ItemStack> supplier = g -> new ItemStack(Material.DIAMOND_PICKAXE);

    final EnumMap<ActionType, Consumer<HotbarClickAction>> actions =
            new EnumMap<>(ActionType.class);

    @Override
    public HotbarIconBuilder item(ItemStack itemStack) {
        this.supplier = g -> itemStack;
        return this;
    }

    @Override
    public HotbarIconBuilder itemSupplier(Function<Gamer, ItemStack> itemStackFunction) {
        this.supplier = itemStackFunction;
        return this;
    }

    @Override
    public HotbarIconBuilder on(ActionType actionType, @NotNull Consumer<HotbarClickAction> consumer) {
        actions.put(actionType, consumer);
        return this;
    }

    @Override
    public HotbarIconBuilder onAny(@NotNull Consumer<HotbarClickAction> consumer) {
        for (ActionType t : ActionType.values()) {
            on(t, consumer);
        }

        return this;
    }

    @Override
    public HotbarBuilder done() {
        return parent;
    }

    @Override
    public HotbarIcon asIcon() {
        return toImmutable();
    }

    public StandardImmutableHotbarIcon toImmutable() {
        return new StandardImmutableHotbarIcon(
                slot,
                supplier,
                actions
        );
    }

}
