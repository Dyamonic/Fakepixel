package club.abelix.api.server.base.shop.category.joinmessage;

import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.base.shop.ShopCategory;
import club.abelix.commons.data.joinmessage.JoinMessage;
import club.abelix.commons.data.joinmessage.JoinMessageSqlHandler;
import club.abelix.commons.util.word.CommonWords;
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
 * <p>JoinMessageCategory creation on 09.03.2024 at 03:33</p>
 */

public class JoinMessageCategory extends ShopCategory {
    private final PagedZone<JoinMessage> pagedZone;

    public JoinMessageCategory(@NonNull Player player, @NonNull ItemStack item) {
        super(player, "donate_description_gui_title", item);

        pagedZone = new PagedZone<JoinMessage>(this) {
            @Override
            protected Collection<JoinMessage> getItems() {
                return JoinMessageSqlHandler.INSTANCE.getJoinMessages().stream().filter(
                        joinMessage -> !joinMessage.isCustom()
                ).collect(Collectors.toList());
            }

            @Override
            protected void drawItem(JoinMessage joinMessage, int slot) {
                val gamer = getGamer();
                val language = getLanguage();
                val item = ItemUtil.getBuilder(Material.BOOK);

                item.setName(getLanguage().getMessage("joinmessage_name",
                        "%suffix%", joinMessage.getName(language)
                ));

                val purchase = gamer.getPurchasedJoinMessages()
                        .contains(joinMessage);

                val displayMessage = joinMessage.getText(language);
                item.setName(translated("joinmessage_name",
                        "%name%", joinMessage.getName(language)
                )).setLore(translatedLore("joinmessage_lore",
                        "%player%", gamer.getDisplayName(),
                        "%text%", displayMessage
                ));

                val gold = gamer.getGold();
                if (purchase) {
                    item.addLore("");
                    item.addLore(getLanguage().getMessage("joinmessage_purchased"));

                    item.glowing(true);
                } else {
                    item.addLore(translatedLore("joinmessage_price_lore",
                            "%cost%", CommonWords.GOLD.get(getLanguage()).withNumber(joinMessage.getCost()),
                            "%balance%", CommonWords.PLAZMA.get(getLanguage()).withNumber(gold)
                    ));
                }


                add(item.build(), slot, clickEvent -> {
                    if (purchase) {
                        return;
                    }

                    if (gamer.getGold() < joinMessage.getCost()) {
                        gamer.sendMessage(getLanguage().getMessage("purchase_unavailable_cost_message"));
                        return;
                    }

                    gamer.takeGold(joinMessage.getCost());
                    gamer.playSound(YSound.LEVEL_UP);

                    gamer.addPurchasedJoinMessage(joinMessage);
                    gamer.changeActiveJoinMessage(joinMessage);

                    draw();

                    gamer.sendMessage(getLanguage().getMessage("donate_joinmessage_purchased",
                            "%message%", joinMessage.getName(language)
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
