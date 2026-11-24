package club.abelix.api.server.base.shop;

import club.abelix.api.base.gamer.Gamer;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * @author - suuft (aka: swiftyclown, Valeriy Shushpanov) ( vk.com/suuft )
 * Created at 15/01/2023 13:40
 */
@UtilityClass
public class ShopUtil {


    /**
     * Its a bad and stupid decision, but
     * i haven't come up with anything better.
     */
    public void open(@NonNull Gamer gamer) {
        ShopCategory.GUI_ITEM_CATEGORY_MAP.get(ShopCategory.RANK).accept(gamer);
    }

    public void openSuffix(@NonNull Gamer gamer) {
        ShopCategory.GUI_ITEM_CATEGORY_MAP.get(ShopCategory.TITLE).accept(gamer);
    }

    public void openJoinMessage(@NonNull Gamer gamer) {
        ShopCategory.GUI_ITEM_CATEGORY_MAP.get(ShopCategory.JOIN_MESSAGE).accept(gamer);
    }

    public void openExchanger(@NonNull Gamer gamer) {
        ShopCategory.GUI_ITEM_CATEGORY_MAP.get(ShopCategory.EXCHANGER).accept(gamer);
    }
}
