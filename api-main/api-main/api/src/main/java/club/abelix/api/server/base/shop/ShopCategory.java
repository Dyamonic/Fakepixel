package club.abelix.api.server.base.shop;

import club.abelix.api.base.box.gui.BoxPurchaseGui;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.base.shop.category.booster.BoosterCategory;
import club.abelix.api.server.base.shop.category.joinmessage.JoinMessageCategory;
import club.abelix.api.server.base.shop.category.rank.RankCategory;
import club.abelix.api.server.base.shop.category.title.TitleCategory;
import club.abelix.commons.language.Lang;
import gnu.trove.list.array.TIntArrayList;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

@Getter
public abstract class ShopCategory extends Gui {

    public static Map<ItemStack, Consumer<Gamer>> GUI_ITEM_CATEGORY_MAP = new LinkedHashMap<>();

    public static ItemStack RANK = ItemUtil
            .getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGQ0NmEwYzVkZTgzZjBmMmUzZDA4OTc3NTVmMDYyZTc3M2NhMjBkNmY4MjRkZjVhZThjY2E3NGY0OGU5NzkyMiJ9fX0=")
            .setName(Lang.of("shop_rank"))
            .setLore(Lang.of("shop_rank_lore"))
            .build();

    public static ItemStack CASE = ItemUtil
            .getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2Q4MDYxN2Y2YjQ1MWQwYzJmYmMxYmI5MzljZmVmZmEwNmZhMzc1ZGM1N2Q1ZThmNjFiOGUzZmE0MDQ1MmI0YiJ9fX0=")
            .setName(Lang.of("shop_case"))
            .setLore(Lang.of("shop_case_lore"))
            .build();

    public static ItemStack TITLE = ItemUtil
            .getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTYxZGZkZGE3MTAyMmJmYWQ3OWVhYmJmYTU2YTE1MTE2NDdiMjAyYWQxMTM2ZmY5N2UzMWEwNmRiYWMxMGVjYiJ9fX0=")
            .setName(Lang.of("shop_title"))
            .setLore(Lang.of("shop_title_lore"))
            .build();

    public static ItemStack JOIN_MESSAGE = ItemUtil
            .getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGFlN2JmNDUyMmIwM2RmY2M4NjY1MTMzNjNlYWE5MDQ2ZmRkZmQ0YWE2ZjFmMDg4OWYwM2MxZTYyMTZlMGVhMCJ9fX0=")
            .setName(Lang.of("shop_join_message"))
            .setLore(Lang.of("shop_join_message_lore"))
            .build();

    public static ItemStack BOOSTER = ItemUtil
            .getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjA1NThkY2U5N2NkZWViOGE3NThkY2YzOTQxNTNkNDQ1MzRhZjU0N2YzMGM0ZjU4NThhNTFjOWY0NDNjN2YwYiJ9fX0=")
            .setName(Lang.of("shop_booster"))
            .setLore(Lang.of("shop_booster_lore"))
            .build();

    public static ItemStack EXCHANGER = ItemUtil
            .getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTZlMGQ1ZmU4N2QyZDIxZTJiZGU2Mzc0NDI1YTM0MWE3NTczYjM3NWQ1YmI3ZDQ3ZjRiOGRhZTM1Mjk3ZWE0In19fQ==")
            .setName(Lang.of("shop_exchanger"))
            .setLore(Lang.of("shop_exchanger_lore"))
            .build();

    static {
        // RANKS
        {
            GUI_ITEM_CATEGORY_MAP.put(RANK, gamer -> new RankCategory(gamer.getPlayer(), RANK));
        }

        // CASE
        {
            GUI_ITEM_CATEGORY_MAP.put(CASE, gamer -> new BoxPurchaseGui(gamer.getPlayer()));
        }

        // TITLE
        {
            GUI_ITEM_CATEGORY_MAP.put(TITLE, gamer -> new TitleCategory(gamer.getPlayer(), TITLE));
        }

        // JOIN MESSAGE
        {
            GUI_ITEM_CATEGORY_MAP.put(JOIN_MESSAGE, gamer -> new JoinMessageCategory(gamer.getPlayer(), JOIN_MESSAGE));
        }

        // BOOSTER
        {
            GUI_ITEM_CATEGORY_MAP.put(BOOSTER, gamer -> new BoosterCategory(gamer.getPlayer(), BOOSTER));
        }
    }

    public static final int[][] SLOTS_POSITIONS = {
            {13},
            {12, 14},
            {11, 13, 15},
            {10, 12, 24, 15},
            {10, 12, 14, 16, 22},
            {10, 12, 14, 16, 20, 23},
            {10, 12, 14, 16, 20, 22, 24},
    };

    private final ItemStack item;

    public ShopCategory(@NonNull Player player, @NonNull String langKey, @NonNull ItemStack item) {
        super(player, Lang.of(langKey), 6);
        this.item = item;
    }

    /**
     * Draw items
     */
    public abstract void drawCategory();

    @Override
    public void draw() {
        renderFrame();
        renderFooter();
        drawCategory();
    }

    private void renderFrame() {
        val frames = new int[]{0, 9, 18, 27, 36, 45, 8, 17, 26, 35, 44, 53};

        for (val frame : frames) {
            add(ItemUtil.getBuilder(Material.STAINED_GLASS_PANE)
                    .setDurability(7)
                    .setName(" ")
                    .build(), frame);
        }


    }

    private void renderFooter() {
        val slots = new TIntArrayList();
        slots.add(new int[]{46, 47, 48, 49, 50, 51, 52});

        GUI_ITEM_CATEGORY_MAP.forEach((item, open) -> {
            val slot = slots.get(0);
            val current = item.equals(this.item);

            add(ItemUtil.getBuilder(Material.STAINED_GLASS_PANE)
                    .setDurability(3)
                    .glowing(current)
                    .setName(translated("shop_items"))
                    .setLore(translatedLore("shop_categories"))
                    .build(), slot - 9, (event) -> {
                if (current) return;
                open.accept(gamer);
            });

            add(item, slot, (event) -> {
                if (current) return;
                open.accept(gamer);
            });

            slots.remove(slot);
        });

        // Отрисовываем оставщиеся
        for (int slot: slots.toArray()) {
            add(ItemUtil.getBuilder(Material.STAINED_GLASS_PANE)
                    .setDurability(3)
                    .setName(translated("shop_items"))
                    .setLore(translatedLore("shop_categories"))
                    .build(), slot - 9);
        }
    }
}
