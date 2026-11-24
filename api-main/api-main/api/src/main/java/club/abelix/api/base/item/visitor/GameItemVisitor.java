package club.abelix.api.base.item.visitor;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;

/**
 * @author Unidentified Person
 */
public class GameItemVisitor {

    private GameItem item;

    public GameItemVisitor(GameItem entry) {
        item = entry;
    }

    public boolean mov(GameItemVisitorPosition position) {
        GameItem res = position.mov(item);
        if (res == null) return false;

        item = res;
        return true;
    }

    public boolean movLastPurchased(Gamer gamer) {
        GameItem tmp = item;

        mov(GameItemVisitorPosition.LAST);

        do {
            if (has(gamer)) {
                return true;
            }
        } while (mov(GameItemVisitorPosition.PREV));

        item = tmp;
        return false;
    }

    public GameItem getItem() {
        return item;
    }

    public boolean has(Gamer gamer) {
        return item.has(gamer);
    }

}
