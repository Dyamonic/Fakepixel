package club.abelix.api.base.item.gui;

import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.gui.ScrollingZone;
import club.abelix.api.base.gui.icon.GuiIcon;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.GameItemCategory;
import club.abelix.api.base.util.item.Heads;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by saharNooby on 22:11 27.10.2017
 */
public class UpgradeableItemsGui extends GameItemGui {

    private static final ItemStack EMPTY_UPGRADE_LEVEL_ITEM = ItemUtil.getBuilder(Material.STAINED_GLASS)
            .setDurability(7)
            .setName(Lang.of("game_item_no_upgrades").compile())
            .build();

    private final PagedZone<ScrollingZone<GameItem>> zone;

    public UpgradeableItemsGui(@NonNull Player player, @NonNull GameItemCategory category) {
        this(player, category, null);
    }

    public UpgradeableItemsGui(@NonNull Player player, @NonNull GameItemCategory category, Gui parent) {
        super(player, 5, category, parent);

        List<List<GameItem>> rows = new ArrayList<>();
        List<ScrollingZone<GameItem>> zones = new ArrayList<>();

        for (GameItem item : category.getStorage().getItems(category)) {
            if (item.getLevel() != 1) {
                continue;
            }

            List<GameItem> row = new ArrayList<>();

            GameItem next = item;

            do {
                row.add(next);
            } while ((next = next.getNext()) != null);

            rows.add(row);
        }

        for (List<GameItem> items : rows) {
            zones.add(new ScrollingZone<GameItem>(this) {

                @Override
                protected Collection<GameItem> getItems() {
                    return items;
                }

                @Override
                protected void drawItem(GameItem item, int slot) {
                    boolean hide = false;
                    boolean hasItem = item.has(gamer);

                    GuiIcon icon = getIcon(item);

                    ItemStack stack = icon.getItem();

                    if (!hasItem && item.getPrev() != null) {
                        hide = true;
                    }

                    if (hide) {
                        // чтобы не мозолило глаза, замажем уровни, до которых ещё далеко
                        stack.setType(Material.STAINED_GLASS);
                        stack.setDurability((short) (item.getPrev().has(gamer) ? 4 : 14));
                        stack.setAmount(1);

                        for (Enchantment ench : stack.getEnchantments().keySet()) {
                            stack.removeEnchantment(ench);
                        }
                    } else if (hasItem) {
                        stack.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);

                        ItemMeta itemMeta = stack.getItemMeta();
                        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

                        stack.setItemMeta(itemMeta);
                    }

                    add(stack, slot, icon.getAction());
                }

                @Override
                protected ItemStack getNextPageIcon() {
                    return ItemUtil.getBuilder(Heads.Wood_ArrowRight.getItem())
                            .setName(translated("page_next_name"))
                            .setLore(translatedLore("page_next_lore"))
                            .build();
                }

                @Override
                protected ItemStack getPrevPageIcon() {
                    return ItemUtil.getBuilder(Heads.Wood_ArrowLeft.getItem())
                            .setName(translated("page_prev_name"))
                            .setLore(translatedLore("page_prev_lore"))
                            .build();
                }
            });
        }

        this.zone = new PagedZone<ScrollingZone<GameItem>>(this) {

            @Override
            protected Collection<ScrollingZone<GameItem>> getItems() {
                return zones;
            }

            @Override
            protected void drawItem(ScrollingZone<GameItem> scrollingZone, int slotStart) {
                scrollingZone.clear();

                for (int slot = slotStart; slot < slotStart + 7; slot++) {
                    scrollingZone.addSlot(slot);
                }

                scrollingZone.draw();
                scrollingZone.drawButtons(slotStart - 1, slotStart + 7);

                //ставим айтемы пустых предметов
                for (int slot = slotStart; slot < slotStart + 7; slot++) {
                    if (get(slot) == null) {
                        add(EMPTY_UPGRADE_LEVEL_ITEM, slot);
                    }
                }
            }
        };

        this.zone.addSlotRect(1, 1, 1, 3);

        draw();
    }

    @Override
    public void draw() {
        if (this.zone == null) {
            return;
        }

        removeRange(0, 5 * 9);
        this.zone.draw();

        if (this.category.isSelectable() && this.getParent() != null) {
            this.zone.drawButtons(Gui.toSlot(1, 4), Gui.toSlot(7, 4));

            drawBackIcon(Gui.toSlot(4, 4));
        } else {
            this.zone.drawButtons(Gui.toSlot(2, 4), Gui.toSlot(6, 4));

            if (this.getParent() != null) {
                drawBackIcon(Gui.toSlot(4, 4));
            }
        }
    }

    @Override
    protected GameItem getVisibleItem(GameItem gameItem) {
        return gameItem;
    }
}
