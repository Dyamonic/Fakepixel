package club.abelix.api.base.item.gui;

import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.GameItemCategory;
import club.abelix.api.base.item.GameMenuCategory;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.level.Leveling;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.HeartBeat;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CategoryListGui extends Gui {


    public static final int[][] CATEGORY_SLOTS = {
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

    // Меню со списком категорий

    protected final Collection<? extends GameMenuCategory> categories;

    public CategoryListGui(@NonNull Player player, GameMenuCategory... categories) {
        this(player, 6, categories);
    }

    public CategoryListGui(@NonNull Player player, Gui parentGui, GameMenuCategory... categories) {
        this(player, 6, parentGui, categories);
    }

    public CategoryListGui(@NonNull Player player, int rowCount, GameMenuCategory... categories) {
        this(
                player, Lang.of("category_gui_title"),
                Arrays.asList(categories), rowCount
        );
    }

    public CategoryListGui(@NonNull Player player, int rowCount, Gui parentGui, GameMenuCategory... categories) {
        this(
                player, Lang.of("category_gui_title"),
                Arrays.asList(categories), parentGui, rowCount
        );
    }

    public CategoryListGui(@NonNull Player player,
                           @NonNull Lang lang,
                           @NonNull Collection<? extends GameMenuCategory> categories,
                           int rowCount) {
        this(player, lang.compile(), categories, null, rowCount);
    }

    public CategoryListGui(@NonNull Player player,
                           @NonNull Lang lang,
                           @NonNull Collection<? extends GameMenuCategory> categories,
                           @Nullable Gui parentGui,
                           int rowCount) {
        this(player, lang.compile(), categories, parentGui, rowCount);
    }

    public CategoryListGui(@NonNull Player player,
                           @NonNull String title,
                           @NonNull Collection<? extends GameMenuCategory> categories,
                           @Nullable Gui parentGui,
                           int rowCount) {
        super(player, title, rowCount);

        setParent(parentGui);
        this.categories = categories;

        draw();
    }

    @Override
    public void draw() {
        drawBorder(BORDER_SLOTS_FOR_SIX_ROWS_GUI);
        Iterator<? extends GameMenuCategory> i = categories.iterator();

        for (int slot : CATEGORY_SLOTS[categories.size() - 1]) {
            GameMenuCategory category = i.next();
            addIcon(category, slot);
        }
    }

    public ItemStack getIcon(GameMenuCategory category) {
        return category.getIcon();
    }

    public String getDisplayName(GameMenuCategory category) {
        return category.getDisplayName(language);
    }

    protected void addIcon(GameMenuCategory category, int slot) {
        ItemUtil.Builder iconBuilder = ItemUtil.getBuilder(getIcon(category));
        iconBuilder.addItemFlags();
        iconBuilder.setName(language.getMessage("game_item_name", "%name%", getDisplayName(category)));

        List<String> description = category.getDescription(language);

        if (category instanceof GameItemCategory itemCategory) {
            List<GameItem> items = itemCategory.getStorage().getItems(itemCategory);

            int available = 0;

            for (GameItem item : items) {
                if (item.has(gamer)) {
                    available++;
                }
            }

            double percentage = ((float) available) / items.size();

            List<String> lore = new ArrayList<>(translatedLore(
                    "category_gui_category_lore",
                    "%size%", items.size(),
                    "%available%", available,
                    "%scale%", Leveling.getScale(20, percentage, "■", "§a", "§7"),
                    "%percentage%", (int) (percentage * 100)
            ));

            //описание нужно заменять такими костылями, иначе там еще более жопа в локализации будет
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);

                if (line.equalsIgnoreCase("%description%")) {
                    lore.remove(i);

                    if (!description.isEmpty()) {
                        lore.addAll(i, description);
                    }
                }

                if (line.equalsIgnoreCase("%selected%")) {
                    lore.remove(i);

                    if (itemCategory.isSelectable()) {
                        GameItem selected = itemCategory.getStorage().getSelected(itemCategory, gamer);

                        if (selected != null) {
                            lore.addAll(i,
                                    translatedLore(
                                            "category_gui_selected_lore",
                                            "%selected_item%", selected.getDisplayName(language)
                                    )
                            );
                        }
                    }

                    break;
                }
            }

            iconBuilder.addLore(lore);
        } else {
            iconBuilder.addLore(description);

            CompletableFuture<List<String>> future = category.getFutureDescription(gamer);

            if (future != null) {
                future.whenComplete((result, cause) -> {
                    if (cause != null) {
                        cause.printStackTrace();

                        return;
                    }

                    HeartBeat.runSync(() -> {
                        iconBuilder.setLore(result);
                        add(iconBuilder.build(), slot, e -> category.openGui(player, this));
                    });
                });
            }
        }

        add(iconBuilder.build(), slot, e -> category.openGui(player, this));
    }
}
