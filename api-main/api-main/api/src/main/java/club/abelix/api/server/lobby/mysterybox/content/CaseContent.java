package club.abelix.api.server.lobby.mysterybox.content;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.Reward;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.CoinType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.NonNull;

public interface CaseContent extends Reward {

    /**
     * Получить редкость предмета
     *
     * @return - рарити
     */
    Rarity getRarity();

    /**
     * Количество выдаваемой статистики в случае, если у игрока
     * уже есть вещь. По умолчанию это пыль, может быть заменена
     * на клауды, кейсы, и прочее
     *
     * @return - количество выдаваемой статы в случае дубликата
     */
    default int getDuplicateEquivalent(@NonNull Gamer gamer) {
        return 0;
    }

    /**
     * Получить тип эквивалентной валюты (то, что будет выдано,
     * если такой предмет у игрока уже есть)
     *
     * @return - тип
     */
    default EquivalentType getEquivalentType() {
        return CoinType.INSTANCE;
    }

}
