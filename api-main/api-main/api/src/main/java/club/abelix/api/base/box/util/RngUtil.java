package club.abelix.api.base.box.util;

import club.abelix.api.base.box.Box;
import club.abelix.api.base.box.reward.BoxReward;
import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class RngUtil {

    private RngUtil() {}

    public static @NotNull BoxReward pick(@NotNull Box box) {
        return pickTwoStage(box, 30);
    }

    public static @NotNull BoxReward pickWithLuck(@NotNull Box box, int luck) {
        if (luck < 1 || luck > 100)
            throw new IllegalArgumentException("Luck must be 1..100");
        return pickTwoStage(box, luck);
    }

    private static @NotNull BoxReward pickTwoStage(@NotNull Box box, int luck) {

        List<? super BoxReward> all = box.getRewards();
        if (all.isEmpty()) throw new IllegalArgumentException("Box has no rewards");

        Map<Rarity, List<BoxReward>> byRarity = new EnumMap<>(Rarity.class);
        for (Object o : all) {
            if (!(o instanceof BoxReward r))
                throw new IllegalStateException("Unknown reward type: " + o.getClass().getSimpleName());
            byRarity.computeIfAbsent(r.getRarity(), k -> new ArrayList<>()).add(r);
        }

        List<Rarity> rarities = new ArrayList<>(byRarity.keySet());
        double[] rarityWeights = new double[rarities.size()];
        double rarityTotal = 0;

        for (int i = 0; i < rarities.size(); i++) {
            rarityWeights[i] = RarityWeights.BASE.getOrDefault(rarities.get(i), 1);
            rarityTotal     += rarityWeights[i];
        }

        Rarity chosenRarity = rarities.get(indexByWeight(rarityWeights, rarityTotal));
        List<BoxReward> bucket = byRarity.get(chosenRarity);

        if (luck == 50 || bucket.size() == 1) {
            return bucket.get(ThreadLocalRandom.current().nextInt(bucket.size()));
        }

        final double alpha = 2.0 * (luck - 50) / 50.0;       // −2 … +2
        double[] w = new double[bucket.size()];
        double avg = 0;

        for (int i = 0; i < bucket.size(); i++) {
            double b = Math.max(1.0, clamp0_100(bucket.get(i).getChance()));
            w[i] = b;
            avg += b;
        }
        avg /= bucket.size();

        double total = 0;
        for (int i = 0; i < w.length; i++) {
            double weight = Math.pow(w[i], 1.0 - alpha) * Math.pow(avg, alpha);
            if (!Double.isFinite(weight) || weight <= 0) weight = 1.0;
            w[i] = weight;
            total += weight;
        }

        return bucket.get(indexByWeight(w, total));
    }

    private static int indexByWeight(double[] w, double total) {
        double needle = ThreadLocalRandom.current().nextDouble() * total;
        double acc = 0;
        for (int i = 0; i < w.length; i++) {
            acc += w[i];
            if (needle < acc) return i;
        }
        return w.length - 1;
    }

    private static int clamp0_100(int v) { return Math.max(0, Math.min(100, v)); }
}