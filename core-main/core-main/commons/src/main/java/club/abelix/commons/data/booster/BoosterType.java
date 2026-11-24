package club.abelix.commons.data.booster;

import club.abelix.commons.language.Language;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Getter
public enum BoosterType {

    // name | multiplier | duration | cost
    BOOSTER_COINS_1(1, MultiplierType.COINS, 1.5, 120, 40),
    BOOSTER_COINS_2(2, MultiplierType.COINS, 2, 180, 80),
    BOOSTER_COINS_3(3, MultiplierType.COINS, 3, 240, 150),
    BOOSTER_EXPERIENCE_1(4, MultiplierType.EXPERIENCE, 1.5, 120, 50),
    BOOSTER_EXPERIENCE_2(5, MultiplierType.EXPERIENCE, 2, 180, 95),
    BOOSTER_EXPERIENCE_3(6, MultiplierType.EXPERIENCE, 3, 240, 180);

    private static final TIntObjectMap<BoosterType> BY_ID = new TIntObjectHashMap<>();

    static {
        for (BoosterType boosterType : BoosterType.values()) {
            BY_ID.put(boosterType.getId(), boosterType);
        }
    }

    private final int id;
    private final MultiplierType multiplierType;
    private final double multiplier;
    private final int duration;
    private final int costPlazma;

    public static BoosterType getById(int id) {
        return BY_ID.get(id);
    }

    public String getDisplayName(@NonNull Language language) {
        return language.getMessage(name() + "_name");
    }

    public String getDisplayName() {
        return getDisplayName(Language.defaultLanguage());
    }

    public int durationToMillis() {
        return (int) TimeUnit.MINUTES.toMillis(duration);
    }

}
