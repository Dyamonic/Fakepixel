package club.abelix.commons.data.currency;

import club.abelix.commons.util.word.CommonWords;
import club.abelix.commons.util.word.Noun;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum GameCurrency {

    COINS(1, CommonWords.COIN),
    GOLD(2, CommonWords.GOLD),
    OTHER(3, CommonWords.GOLD);

    private final int id;
    private final Noun word;

}
