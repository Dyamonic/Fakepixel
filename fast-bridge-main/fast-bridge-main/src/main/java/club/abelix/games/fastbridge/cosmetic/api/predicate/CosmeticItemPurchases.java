package club.abelix.games.fastbridge.cosmetic.api.predicate;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.predicate.GameItemPredicate;

import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-gamecosmetic</p>
 * <p>GameCosmeticItemPurchases creation on 08.09.2023 at 18:42</p>
 */

public class CosmeticItemPurchases implements GameItemPredicate {

    // Проверка на наличие покупки с определённым id у игрока

    private final int itemId;

    public CosmeticItemPurchases(int itemId) {
        this.itemId = itemId;
    }

    @Override
    public boolean match(Gamer gamer) {
        return gamer.getPurchases().contains(this.itemId);
    }

    @Override
    public List<String> getRequirementLore(GameItem item, Gamer gamer) {
        throw new IllegalStateException("Can't get requirement lore for PurchasePredicate");
    }
}
