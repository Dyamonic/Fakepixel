package club.abelix.games.buildffa.game.utility;

import lombok.experimental.UtilityClass;

/**
 * @author milansky
 */
@UtilityClass
public final class ConvertUtility {
    private static final int COINS_GOLDS_CURRENCY = 1000;

    public int toGold(final int coins) {
        if (coins < COINS_GOLDS_CURRENCY) return 1;

        return coins / COINS_GOLDS_CURRENCY;
    }
}
