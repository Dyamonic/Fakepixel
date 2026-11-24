package club.abelix.api.server.lobby.mysterybox.content.equivalent;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.util.word.CommonWords;
import lombok.NonNull;

/**
 * Игроку выдаются золото
 */
public class GoldType implements EquivalentType {

    public static GoldType INSTANCE = new GoldType();

    // ------------------------------------------------------------------

    @Override
    public boolean giveDuplicateEquivalent(@NonNull Gamer gamer, int clouds) {
        gamer.addGold(clouds);
        return true;
    }

    @Override
    public void sendDuplicateEquivalentMessage(@NonNull Gamer gamer, int equivalent) {
        gamer.sendLocalizedMessage("case_duplicate_gold",
                "%gold%", CommonWords.GOLD.get(gamer.getLanguage()).withNumber(equivalent));
    }

}
