package club.abelix.api.base.item.predicate;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;

import java.util.List;

/**
 * Created by saharNooby on 23:11 17.10.2017
 */
public class PurchasePredicate implements GameItemPredicate {

    // Проверка на наличие покупки с определённым id у игрока

    private final int itemId;

    public PurchasePredicate(int itemId) {
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
