package club.abelix.api.server.base.shop.category.title;

import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.base.shop.ShopCategory;
import club.abelix.commons.data.suffix.Suffix;
import club.abelix.commons.data.suffix.SuffixSqlHandler;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>TitleCategory creation on 09.03.2024 at 00:37</p>
 */

public class TitleCategory extends ShopCategory {
    private final PagedZone<Suffix> pagedZone;

    public TitleCategory(@NonNull Player player, @NonNull ItemStack item) {
        super(player, "donate_description_gui_title", item);

        pagedZone = new PagedZone<Suffix>(this) {
            @Override
            protected Collection<Suffix> getItems() {
                return SuffixSqlHandler.INSTANCE.getSuffixes().stream().filter(suffix -> suffix.getCost() != 0).toList();
            }

            @Override
            protected void drawItem(Suffix suffix, int slot) {
                val item = ItemUtil.getBuilder(Material.NAME_TAG);

                item.setName(getLanguage().getMessage("donate_suffix_name",
                        "%suffix%", suffix.getDisplaySuffix()
                ));

                val purchase = gamer.getPurchasedSuffixes()
                        .stream().map(Suffix::getId)
                        .collect(Collectors.toSet())
                        .contains(suffix.getId());

                item.setLore(getLanguage().getMessage("donate_suffix_lore",
                        "%gamer%", gamer.getDisplayName(),
                        "%suffix%", suffix.getDisplaySuffix(),
                        "%price%", suffix.getCost()));


                if (purchase) {
                    item.addLore("");
                    item.addLore(getLanguage().getMessage("donate_suffix_purchased"));

                    item.glowing(true);
                }


                add(item.build(), slot, clickEvent -> {
                    if (purchase) {
                        return;
                    }

                    if (gamer.getGold() < suffix.getCost()) {
                        gamer.sendMessage(getLanguage().getMessage("purchase_unavailable_cost_message"));
                        return;
                    }

                    gamer.takeGold(suffix.getCost());
                    gamer.playSound(YSound.LEVEL_UP);

                    gamer.addPurchasedSuffix(suffix.getId());
                    gamer.changeActiveSuffix(suffix);

                    draw();

                    gamer.sendMessage(getLanguage().getMessage("donate_suffix_purchased",
                            "%suffix%", suffix.getDisplaySuffix()
                    ));
                });
            }
        };
        pagedZone.addSlotRect(1, 1, 7, 2);
        draw();
    }

    @Override
    public void drawCategory() {
        pagedZone.draw();
        pagedZone.drawButtons(28, 34);
    }
}

