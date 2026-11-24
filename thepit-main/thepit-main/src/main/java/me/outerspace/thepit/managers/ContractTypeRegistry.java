package me.outerspace.thepit.managers;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ContractTypeRegistry {
    private static final Map<ContractManager.Difficulty, Definition> DEFINITIONS;

    static {
        EnumMap<ContractManager.Difficulty, Definition> definitions = new EnumMap<>(ContractManager.Difficulty.class);
        definitions.put(ContractManager.Difficulty.NOVICE, new Definition(
                30,
                250,
                400,
                800,
                EnumSet.of(
                        ContractManager.Objective.PLAYER_KILLS,
                        ContractManager.Objective.KILLS_ON_GRASS,
                        ContractManager.Objective.KILLS_ON_OBSIDIAN,
                        ContractManager.Objective.GROUND_INGOTS,
                        ContractManager.Objective.GOLDEN_APPLES_EATEN,
                        ContractManager.Objective.BOW_HITS,
                        ContractManager.Objective.ASSISTS,
                        ContractManager.Objective.PLAYER_DAMAGE
                )
        ));
        definitions.put(ContractManager.Difficulty.BIG_TIME, new Definition(
                70,
                1000,
                1500,
                3000,
                EnumSet.allOf(ContractManager.Objective.class)
        ));
        DEFINITIONS = Collections.unmodifiableMap(definitions);
    }

    private ContractTypeRegistry() {
    }

    public static Definition definition(ContractManager.Difficulty difficulty) {
        Definition definition = DEFINITIONS.get(difficulty);
        if (definition == null) throw new IllegalArgumentException("Unknown contract difficulty: " + difficulty);
        return definition;
    }

    public record Definition(int minimumLevel, int baseCost, int rewardMinimum, int rewardMaximum,
                             Set<ContractManager.Objective> objectives) {
        public Definition {
            objectives = Collections.unmodifiableSet(EnumSet.copyOf(objectives));
        }
    }
}
