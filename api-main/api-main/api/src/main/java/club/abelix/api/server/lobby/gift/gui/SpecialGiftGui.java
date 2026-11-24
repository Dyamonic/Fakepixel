package club.abelix.api.server.lobby.gift.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.lobby.gift.data.GiftType;
import lombok.NonNull;
import org.bukkit.Material;

public class SpecialGiftGui extends RankGiftGui {

    public SpecialGiftGui(@NonNull Gamer gamer, @NonNull Gamer targetGamer) {
        super(gamer, targetGamer);
    }

    @Override
    public void openPreviousGui() {
        new RankGiftGui(gamer, getTargetGamer());
    }

    @Override
    public void drawRanks() {
        drawItem(30, GiftType.SPECIAL_365);
        drawItem(32, GiftType.SPECIAL);
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
                .setSkullValue(RED_PRESENT)
                .setName(language.getMessage("gift_rank_" + type.name().toLowerCase() + "_name"))
                .setLore(language.getMessageList("donate_description_gui_special_lore_with_cost",
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
