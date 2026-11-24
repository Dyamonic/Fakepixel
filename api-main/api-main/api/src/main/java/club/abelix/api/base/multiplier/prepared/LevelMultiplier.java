package club.abelix.api.base.multiplier.prepared;

import club.abelix.api.base.multiplier.Multiplier;
import club.abelix.commons.data.booster.MultiplierType;
import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
public class LevelMultiplier extends Multiplier {

    // Multiplier от уровня игрока

    @Getter
    private final int level;

    public LevelMultiplier(int level) {
        super(MultiplierType.COINS, fromLevel(level));

        this.level = level;
    }

    @Override
    public String getDisplayName(Language language) {
        return language.getMessage("level_coin_multiplier_name",
                "%level%", level, "%multiplier%", (1.0 + value));
    }

    /**
     * Вернёт мультипликатор по уровню
     *
     * @param level - уровень
     * @return - добавочный множитель
     */
    public static double fromLevel(int level) {
        // c 1 по 19
        if (level < 20) {
            return 0;
        }

        // c 20 по 39
        if (level < 40) {
            return 0.5;
        }

        // c 40 по 59
        if (level < 60) {
            return 1.0;
        }

        // c 60 по 79
        if (level < 80) {
            return 1.5;
        }

        // c 80 по 99
        if (level < 100) {
            return 2.0;
        }

        //100 и выше
        return 2.5;
    }

}
