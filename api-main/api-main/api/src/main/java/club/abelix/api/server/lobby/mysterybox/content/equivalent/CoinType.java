package club.abelix.api.server.lobby.mysterybox.content.equivalent;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.util.word.CommonWords;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Uncommon;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Игроку выдаются коины
 */
@NoArgsConstructor
public class CoinType implements EquivalentType {

    public static CoinType INSTANCE = new CoinType();

    // ---------------------------------------------------------

    private Uncommon uncommon;

    private Master master;

    public CoinType(Uncommon uncommon) {
        this.uncommon = uncommon;
    }

    public CoinType(Master master) {
        this.master = master;
    }

    @Override
    public boolean giveDuplicateEquivalent(@NonNull Gamer gamer, int coins) {
        if (uncommon != null) {
            gamer.addCoins(uncommon, coins, false, null);
        } else if (master != null) {
            gamer.addCoins(master, coins, false, null);
        }

        return true;
    }

    @Override
    public void sendDuplicateEquivalentMessage(@NonNull Gamer gamer, int equivalent) {
        String key = uncommon == null ?
                "case_duplicate_coins" :
                "case_duplicate_mode_coins";

        gamer.sendLocalizedMessage(key,
                "%mode%", uncommon == null ? "" : uncommon.getDisplayName(),
                "%coins%", CommonWords.COIN.get(gamer.getLanguage()).withNumber(equivalent));
    }

}
