package club.abelix.api.base.box.gui;
    
import club.abelix.api.FalconAPI;
import club.abelix.api.base.box.Box;
import club.abelix.api.base.box.type.BoxType;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.base.gui.ConfirmationGui;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class BoxPurchaseConfirmationGui extends Gui {

    private final BoxType type;
    private final Rarity rarity;
    private final GameCurrency gameCurrency;
    private final Gui previousGui;
    private int amount = 1;

    public BoxPurchaseConfirmationGui(@NonNull Player player,
                                    @NonNull BoxType type,
                                    @NonNull Rarity rarity,
                                    @NonNull GameCurrency currency,
                                    Gui previousGui) {
        super(player, Lang.of("box_purchase_confirm"), 4);
        this.type = type;
        this.rarity = rarity;
        this.gameCurrency = currency;
        this.previousGui = previousGui;

        draw();
    }

    @Override
    public void draw() {
        add(ItemUtil.getBuilder(Material.GOLD_NUGGET)
                .setName(Lang.of("box_purchase_amount_decrease"))
                .build(), toSlot(3, 1), e -> {
            if (amount > 1) {
                amount--;
                gamer.playSound(YSound.WOOD_CLICK);
                draw();
            }
        });

        val optionalBox = FalconAPI.getInstance().getBoxStorageService().findByTypeAndRarity(type, rarity);
        int pricePerOne = optionalBox.isPresent() ? optionalBox.get().getCost(gameCurrency) : 0;

        ItemStack boxIcon = ItemUtil.getBuilder(RaritiesIcon.find(rarity))
                .setName(rarity.getName(getLanguage()))
                .setAmount(amount)
                .setLore(translatedLore("box_rarity_selector_gui_lore",
                        "%rarity%", rarity.getName(getLanguage()),
                        "%amount%", amount,
                        "%price%", pricePerOne * amount,
                        "%currency%", gameCurrency.getWord().get(language).fromNumber(pricePerOne * amount)
                ))
                .build();
        add(boxIcon, toSlot(4, 1));

        add(ItemUtil.getBuilder(Material.GOLD_INGOT)
                .setName(Lang.of("box_purchase_amount_increase"))
                .build(), toSlot(5, 1), e -> {
            if (amount < 64) {
                amount++;
                gamer.playSound(YSound.WOOD_CLICK);
                draw();
            }
        });

        add(ItemUtil.getBuilder(Material.STAINED_GLASS)
                .setDurability(13)
                .setName(Lang.of("confirmation_gui_yes"))
                .build(), toSlot(3, 3), e -> {
            FalconAPI.getInstance().getBoxesService().attemptPurchase(
                    gamer,
                    gameCurrency,
                    CoreUtil.getMaster(),
                    type,
                    rarity,
                    amount
            );
            gamer.playSound(YSound.NOTE_PLING);
            closeOrOpenPrevious();
        });

        add(ItemUtil.getBuilder(Material.STAINED_GLASS)
                .setDurability(14)
                .setName(Lang.of("confirmation_gui_no"))
                .build(), toSlot(5, 3), e -> {
            gamer.playSound(YSound.ITEM_BREAK);
            closeOrOpenPrevious();
        });

        if (previousGui != null) {
            generateBackItem(toSlot(4, 3), clickEvent -> previousGui.reopen());
        }
    }

    private void closeOrOpenPrevious() {
        if (previousGui != null) {
            previousGui.reopen();
        } else {
            player.closeInventory();
        }
    }
}
