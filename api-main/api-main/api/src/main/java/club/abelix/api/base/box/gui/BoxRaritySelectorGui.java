package club.abelix.api.base.box.gui;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.box.Box;
import club.abelix.api.base.box.type.BoxType;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.base.gui.ConfirmationGui;
import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.commons.language.Lang;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.entity.Player;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BoxRaritySelectorGui extends Gui {

    BoxType type;
    GameCurrency gameCurrency;

    public BoxRaritySelectorGui(@NonNull Player player, BoxType type, GameCurrency currency) {
        super(player, Lang.of("box_rarity_selector_gui"), 4);

        this.gameCurrency = currency;
        this.type = type;

        draw();
    }

    @Override
    public void draw() {
        val boxes = FalconAPI.getInstance().getBoxStorageService().findAllByType(type);

        for (int i = 0; i < boxes.size(); i++) {
            val box = boxes.get(i);
            val rarity = box.getRarity();

            val icon = ItemUtil.getBuilder(RaritiesIcon.find(rarity))
                    .setName(rarity.getName(getLanguage()))
                    .setLore(translatedLore("box_rarity_selector_gui_lore",
                            "%rarity%", rarity.getName(getLanguage()),
                            "%price%", box.getCost(gameCurrency),
                            "%currency%", gameCurrency.getWord().get(language).fromNumber(box.getCost(gameCurrency))
                    ))
                    .build();

            add(icon, ONE_SLOT_BORDER_SLOTS_PROVIDER.getSlotForItem(i, boxes.size()), e -> {
                new BoxPurchaseConfirmationGui(player, type, rarity, gameCurrency, this);
            });
        }

        generateBackItem(31, e -> new BoxPurchaseGui(player));
    }
}
