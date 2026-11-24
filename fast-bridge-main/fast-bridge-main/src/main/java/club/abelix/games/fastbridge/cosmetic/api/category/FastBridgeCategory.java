package club.abelix.games.fastbridge.cosmetic.api.category;

import club.abelix.api.base.item.GameItemCategory;
import club.abelix.commons.util.word.Noun;
import com.google.common.collect.Lists;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public abstract class FastBridgeCategory extends GameItemCategory {

    // region static

    private static final Map<Class<? extends FastBridgeCategory>, FastBridgeCategory> BY_CLASS = new HashMap<>();
    // в порядке появления иконок в гуи
    public static List<FastBridgeCategory> CATEGORIES = Lists.newArrayList(
            getCategory(BlocksCategory.class)
    );


    public FastBridgeCategory(@NonNull String id, @NonNull Noun word) {
        super(id, word, new ItemStack(Material.AIR), true);
    }

    // endregion

    @SuppressWarnings("unchecked")
    public static <T extends FastBridgeCategory> T getCategory(@NonNull Class<T> cls) {
        return (T) BY_CLASS.computeIfAbsent(cls, k -> {
            try {
                return k.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Set<Class<? extends FastBridgeCategory>> getIncompatible() {
        return Collections.emptySet();
    }

}
