package club.abelix.games.buildffa.game.division;

import club.abelix.commons.language.Language;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;

public enum AbstractDivision {
    NOVICE("buildffa_division_novice", 0, 1),
    BRONZE_I("buildffa_division_bronze_i", 100, 1),
    BRONZE_II("buildffa_division_bronze_ii", 300, 1),
    BRONZE_III("buildffa_division_bronze_iii", 850, 1),
    METAL_I("buildffa_division_metal_i", 2100, 1),
    METAL_II("buildffa_division_metal_ii", 3450, 1),
    METAL_III("buildffa_division_metal_iii", 5200, 1),
    GOLD_I("buildffa_division_gold_i", 7500, 2),
    GOLD_II("buildffa_division_gold_ii", 10000, 2),
    GOLD_III("buildffa_division_gold_iii", 14500, 2),
    EMERALD_I("buildffa_division_emerald_i", 18000, 3),
    EMERALD_II("buildffa_division_emerald_ii", 23000, 3),
    EMERALD_III("buildffa_division_emerald_iii", 28000, 3),
    RUBY("buildffa_division_ruby", 35000, 3);

    private final String key;
    @Getter
    private final int requiredRating, penalty;

    AbstractDivision(String key, int requiredRating, int penalty) {
        this.key = key;
        this.requiredRating = requiredRating;
        this.penalty = penalty;
    }

    public static AbstractDivision getRankForRating(int rating) {
        return Arrays.stream(AbstractDivision.values())
                .filter(division -> rating >= division.getRequiredRating())
                .max(Comparator.comparingInt(AbstractDivision::getRequiredRating))
                .orElse(NOVICE);
    }

    public String getName(Language language) {
        String name = language.getMessage(key);
        return name != null ? name : "Unknown Division";
    }

}
