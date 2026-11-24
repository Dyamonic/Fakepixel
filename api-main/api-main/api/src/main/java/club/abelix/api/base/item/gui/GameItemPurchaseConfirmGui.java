package club.abelix.api.base.item.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.predicate.GameItemPredicate;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.base.gui.ConfirmationGui;
import lombok.NonNull;
import lombok.val;

public class GameItemPurchaseConfirmGui extends ConfirmationGui {

    public GameItemPurchaseConfirmGui(@NonNull Gamer gamer,
                                       @NonNull GameItem item,
                                       @NonNull ClickAction action,
                                       @NonNull Gui previousGui) {

        super(gamer.getPlayer(), action, previousGui);

        ItemUtil.Builder builder = ItemUtil.getBuilder(item.getIcon(language));
        builder.addLore(language.getMessageList("game_item_purchase_confirm"));

        add(builder.build(), toSlot(4, 0));
    }

    public static GameItemPurchaseConfirmGui create(@NonNull Gamer gamer,
                                                    @NonNull GameItem item,
                                                    @NonNull Gui currentGui) {
        return create(gamer, item, currentGui, item.getCoinsPredicate());
    }

    public static GameItemPurchaseConfirmGui create(@NonNull Gamer gamer,
                                                    @NonNull GameItem item,
                                                    @NonNull Gui currentGui,
                                                    @NonNull GameItemPredicate predicate) {

        val player = gamer.getPlayer();
        val language = gamer.getLanguage();

        val category = item.getCategory();

        return new GameItemPurchaseConfirmGui(gamer, item, clickEvent -> {
            if (!predicate.match(gamer)) {
                return;
            }

            // добавляем в историю покупок эту транзакцию
            gamer.addPurchase(item.getId(), predicate.withdraw(gamer), predicate.getType());

            gamer.playSound(YSound.LEVEL_UP);

            if (category.isSelectable()) {
                GameItem selected = category.getStorage().getSelected(category, gamer);

                // убираем выбор со старого
                if (selected != null) {
                    selected.onUnselect(player);
                }

                // выбираем автоматически купленный предмет
                category.getStorage().selectItem(gamer, item);

                item.onSelect(player);
            }

            gamer.sendLocalizedMessage(
                    "game_item_buy",
                    "%label%", category.getWord().toString(language),
                    "%item%", item.getDisplayName(language)
            );
        }, currentGui);
    }

}
