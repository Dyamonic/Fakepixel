package club.abelix.api.server.lobby.gift.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.lobby.gift.data.GiftType;
import lombok.NonNull;
import org.bukkit.Material;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
public class SponsorGiftGui extends RankGiftGui {

    public SponsorGiftGui(@NonNull Gamer gamer, @NonNull Gamer targetGamer) {
        super(gamer, targetGamer);
    }

    @Override
    public void openPreviousGui() {
        new RankGiftGui(gamer, getTargetGamer());
    }

    @Override
    public void drawRanks() {
        drawItem(29, GiftType.SPONSOR_1);
        drawItem(31, GiftType.SPONSOR_2);
        drawItem(33, GiftType.SPONSOR_3);
    }

    private void drawItem(int slot, GiftType type) {
        String actionLine;

        if (getTargetGamer().getRanks().hasRank(type.getRankId())) {
            actionLine = language.getMessage("gift_rank_item_already_owned");
        } else if (!gamer.hasGold(type.getPrice())) {
            actionLine = language.getMessage("gift_rank_not_enough_coins");
        } else {
            actionLine = language.getMessage("gift_rank_item_buy");
        }

        add(ItemUtil.getBuilder(Material.SKULL_ITEM)
                .setDurability(3)
                .setSkullValue(PURPLE_PRESENT)
                .setName(language.getMessage("gift_rank_" + type.name().toLowerCase() + "_name"))
                .setLore(language.getMessageList("donate_description_gui_sponsor_lore_with_cost",
                        "%price%", type.getPrice()))
                .addLore(actionLine)
                .glowing()
                .build(), slot, event -> {
            if (getTargetGamer().getRanks().hasRank(type.getRankId())) {
                gamer.getPlayer().closeInventory();
                gamer.sendLocalizedMessage("gift_rank_item_already_owned");
                return;
            } else if (!gamer.hasGold(type.getPrice())) {
                gamer.getPlayer().closeInventory();
                gamer.sendLocalizedMessage("gift_rank_not_enough_coins");
                return;
            }

            sendGift(type);
        });
    }

}
