package club.abelix.api.base.box.gui;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.box.type.StandardBoxType;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import lombok.val;
import org.bukkit.entity.Player;

public class BoxPurchaseGui extends Gui {

    private static final String UNAVAILABLE_SKULL_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWJiZmVmZTk3ODY0ODY1YTE5YWFhZDBmN2RjNDdiNmMzMTE3OTFiNGMzY2E4MTE1ZWYwN2FkOTlhYjU1NjU3ZCJ9fX0=";

    public BoxPurchaseGui(@NonNull Player player) {
        super(player, Lang.of("purchase_box_gui"), 6);

        draw();
    }

    @Override
    public void draw() {
        drawBorder(BORDER_SLOTS_FOR_SIX_ROWS_GUI);

        val types = StandardBoxType.values();

        for (int i = 0; i < types.length; i++) {
            val boxType = types[i];
            val boxOpt = FalconAPI.getInstance().getBoxStorageService().findByType(boxType);

            if (boxOpt.isEmpty()) {
                add(ItemUtil.getSkullBuilder(UNAVAILABLE_SKULL_TEXTURE)
                        .setName(translated("box_unavailable_on_server_name"))
                        .setLore(translatedLore("box_lore_available_on_game_lobbies"))
                        .build(), ONE_SLOT_BORDER_SLOTS_PROVIDER.getSlotForItem(i, types.length), e -> {
                    gamer.sendLocalizedMessage("box_unavailable_on_server");
                    gamer.playSound(YSound.VILLAGER_NO);
                });
                continue;
            }

            val box = boxOpt.get();
            val icon = ItemUtil.getBuilder(box.getIcon(getGamer()));

            icon.setName(translated(box.getNameKey()));
            icon.addLore(translatedLore("box_purchase_lore"));

            boolean coinsAllowed = box.isPurchasable(GameCurrency.COINS);
            boolean goldAllowed = box.isPurchasable(GameCurrency.GOLD);

            if (coinsAllowed && goldAllowed) {
                icon.addLore(translatedLore("box_purchasable_coins"));
                icon.addLore(translatedLore("box_purchasable_gold"));
            } else if (coinsAllowed) {
                icon.addLore(translatedLore("box_purchasable_coins_only"));
            } else if (goldAllowed) {
                icon.addLore(translatedLore("box_purchasable_gold_only"));
            } else {
                icon.addLore(translatedLore("box_not_purchasable_any"));
            }

            add(icon.build(), ONE_SLOT_BORDER_SLOTS_PROVIDER.getSlotForItem(i, types.length), e -> {
                GameCurrency currency;

                if (coinsAllowed && goldAllowed) {
                    currency = e.getClick().isLeftClick() ? GameCurrency.COINS : GameCurrency.GOLD;
                } else if (coinsAllowed) {
                    currency = GameCurrency.COINS;
                } else if (goldAllowed) {
                    currency = GameCurrency.GOLD;
                } else {
                    gamer.sendLocalizedMessage("box_not_purchasable_any");
                    gamer.playSound(YSound.VILLAGER_NO);
                    return;
                }

                new BoxRaritySelectorGui(getPlayer(), box.getType(), currency);
                gamer.playSound(YSound.CLICK);
            });
        }

        generateBackItem(49, e -> {
            new PlayerBoxesGui(gamer);
        });
    }

}
