package club.abelix.api.base.item.predicate;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;
import club.abelix.commons.data.currency.GameCurrency;
import lombok.NonNull;

import java.util.List;

/**
 * Created by saharNooby on 22:59 17.10.2017
 */
public interface GameItemPredicate {

    boolean match(Gamer gamer);

    /**
     * Возвращает строку, которая отвечает на вопрос "что требуется для выполнения этого предиката",
     * а также краткое описание текущего состояния игрока.<br/>
     * Пример: "что требуется?" - "10 клаудов (у вас 5 клаудов)"
     */
    List<String> getRequirementLore(GameItem item, Gamer gamer);

    /**
     * Не обязательные методы для покупки за GameCurrency
     */

    default int withdraw(@NonNull Gamer gamer) {
        return 0;
    }

    default GameCurrency getType() {
        return null;
    }

}
