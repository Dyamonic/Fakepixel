package club.abelix.api.base.box.util;

import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public final class RarityWeights {

    public static final Map<Rarity, Integer> BASE = new java.util.LinkedHashMap<>();

    static {
        BASE.put(Rarity.COMMON, 100);
        BASE.put(Rarity.RARE, 25);
        BASE.put(Rarity.EPIC, 5);
        BASE.put(Rarity.LEGENDARY, 1);
    }

}