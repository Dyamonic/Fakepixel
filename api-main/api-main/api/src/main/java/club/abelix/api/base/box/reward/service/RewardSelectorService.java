package club.abelix.api.base.box.reward.service;

import club.abelix.api.base.box.Box;
import club.abelix.api.base.box.service.BoxesService;
import club.abelix.api.base.box.service.BoxesStorageService;
import club.abelix.api.base.box.type.BoxType;
import club.abelix.api.base.box.type.StandardBoxType;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.currency.GameCurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Log4j2
@RequiredArgsConstructor
public class RewardSelectorService {

    private final BoxesStorageService boxesStorageService;
    private final BoxesService boxesService;

    // --- Коины, если бокс не выпал ---
    private static final int COINS_FLOOR_MIN = 1;
    private static final int COINS_FLOOR_MAX = 3;

    /**
     * Шанс дропа при минимальном учитываемом весе (weight = 1): 0.01%
     */
    private static final double BOX_MIN_CHANCE = 0.0001; // 0.01%

    /**
     * Шанс дропа при максимальном весе (weight = 100): 2.5%
     */
    private static final double BOX_MAX_CHANCE = 0.0250; // 2.5%

    // Редкости по фиксированным шансам
    private static final EnumMap<Rarity, Integer> RARITY_BASE = new EnumMap<>(Rarity.class);
    private static final EnumMap<Rarity, Double> RARITY_PER_WEIGHT = new EnumMap<>(Rarity.class);

    // Типы боксов по фиксированным шансам
    private static final Map<BoxType, Integer> TYPE_BASE = new HashMap<>();
    private static final Map<BoxType, Double> TYPE_PER_WEIGHT = new HashMap<>();

    static {
        // 80% / 15% / 4% / 1%
        RARITY_BASE.put(Rarity.COMMON, 80);
        RARITY_BASE.put(Rarity.RARE, 15);
        RARITY_BASE.put(Rarity.EPIC, 4);
        RARITY_BASE.put(Rarity.LEGENDARY, 1);

        // без влияния веса
        RARITY_PER_WEIGHT.put(Rarity.COMMON, 0.0);
        RARITY_PER_WEIGHT.put(Rarity.RARE, 0.0);
        RARITY_PER_WEIGHT.put(Rarity.EPIC, 0.0);
        RARITY_PER_WEIGHT.put(Rarity.LEGENDARY, 0.0);

        // Типы:
        // Игровая косметика – 32%
        // Косметика лобби – 32%
        // Золото – 10%
        // Опыт – 10%
        // Титулы – 13%
        // Привилегии – 3%
        TYPE_BASE.put(StandardBoxType.GAME_COSMETIC, 32);
        TYPE_BASE.put(StandardBoxType.LOBBY_COSMETIC, 32);
        TYPE_BASE.put(StandardBoxType.GOLD, 10);
        TYPE_BASE.put(StandardBoxType.EXPERIENCE, 10);
        TYPE_BASE.put(StandardBoxType.TITLES, 13);
        TYPE_BASE.put(StandardBoxType.RANKS, 3);

        // без влияния веса
        TYPE_PER_WEIGHT.put(StandardBoxType.GAME_COSMETIC, 0.0);
        TYPE_PER_WEIGHT.put(StandardBoxType.LOBBY_COSMETIC, 0.0);
        TYPE_PER_WEIGHT.put(StandardBoxType.GOLD, 0.0);
        TYPE_PER_WEIGHT.put(StandardBoxType.EXPERIENCE, 0.0);
        TYPE_PER_WEIGHT.put(StandardBoxType.TITLES, 0.0);
        TYPE_PER_WEIGHT.put(StandardBoxType.RANKS, 0.0);
    }

    public void giveReward(
            Gamer gamer,
            @Range(from = 0L, to = 100L) int weight
    ) {
        giveReward(gamer, weight, false);
    }

    public void giveReward(
            Gamer gamer,
            @Range(from = 0L, to = 100L) int weight,
            boolean test
    ) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        double chance = boxDropChance(weight);
        boolean boxDrop = test ? rnd.nextBoolean() : rnd.nextDouble() < chance;
        log.debug("reward: gamer={}, weight={}, chance={}", gamer.getName(), weight, chance);

        if (boxDrop) {
            Collection<Box> registry = getAllRegisteredBoxes();

            Rarity rarity = pickRarityAmongAvailable(registry, weight, rnd);
            BoxType type = pickTypeAmongAvailable(registry, rarity, weight, rnd);

            List<Box> candidates = registry.stream()
                    .filter(b -> b.getType().equals(type) && b.getRarity() == rarity)
                    .collect(Collectors.toList());

            if (candidates.isEmpty()) {
                log.debug("reward: no candidates for type={}, rarity={}, fallback=rarity", type.getId(), rarity);
                candidates = registry.stream()
                        .filter(b -> b.getRarity() == rarity)
                        .collect(Collectors.toList());
            }
            if (candidates.isEmpty()) {
                log.debug("reward: no candidates for rarity={}, fallback=any", rarity);
                candidates = new ArrayList<>(registry);
            }

            Box chosen;
            if (candidates.size() == 1) {
                chosen = candidates.get(0);
            } else {
                Map<Box, Double> weights = new HashMap<>();
                for (Box b : candidates) {
                    int cost = b.getCost(GameCurrency.COINS);
                    double w = cost > 0 ? (1.0 / cost) : 1.0;
                    w += rnd.nextDouble() * 0.01;
                    weights.put(b, w);
                }
                chosen = weightedPick(weights, rnd);
            }

            log.info("Selected reward: box -> gamer={}, rarity={}, type={}, boxId={}, mode={}",
                    gamer.getName(), chosen.getRarity(), chosen.getType().getId(), chosen.getType().getId(), chosen.getMode());

            boxesService.giveBox(
                    gamer,
                    chosen.getRarity(),
                    chosen.getMode(),
                    chosen.getType().getId(),
                    chosen.getType()
            );

            gamer.sendLocalizedMessage("reward_box_received",
                    "%box", gamer.getLanguage().getMessage(chosen.getNameKey()),
                    "%rarity", chosen.getRarity().getName(gamer.getLanguage()),
                    "%type", chosen.getType().getName(gamer.getLanguage())
            );
            return;
        }

        int coins = calcCoins(weight, rnd);
        gamer.addCoins(coins);
        log.info("Selected reward: coins -> gamer={}, coins={}", gamer.getName(), coins);
        gamer.sendLocalizedMessage("reward_coins_received", "%coins", coins);
    }

    private Collection<Box> getAllRegisteredBoxes() {
        return boxesStorageService.getAll();
    }

    /**
     * Линейно: weight=1 -> 0.01%, weight=100 -> 2.5%.
     * Для weight<=0 используем шанс как при weight=1.
     */
    private double boxDropChance(int weight) {
        int w = clamp(weight, 0, 100);
        int wEff = Math.max(1, w); // минимальный учитываемый вес
        double t = (wEff - 1) / 99.0; // [0..1]
        return BOX_MIN_CHANCE + (BOX_MAX_CHANCE - BOX_MIN_CHANCE) * t;
    }

    private int calcCoins(int weight, ThreadLocalRandom rnd) {
        if (weight <= 0) return rnd.nextInt(COINS_FLOOR_MIN, COINS_FLOOR_MAX + 1);
        int min = 8 * weight;
        int max = 15 * weight;
        return rnd.nextInt(min, max + 1);
    }

    private Rarity pickRarityAmongAvailable(Collection<Box> registry, int weight, ThreadLocalRandom rnd) {
        Set<Rarity> available = registry.stream().map(Box::getRarity).collect(Collectors.toSet());
        Map<Rarity, Double> weights = new EnumMap<>(Rarity.class);

        for (Rarity r : available) {
            int base = RARITY_BASE.getOrDefault(r, 0);
            if (base <= 0) continue;
            double mult = RARITY_PER_WEIGHT.getOrDefault(r, 0.0);
            double adjusted = base * (1.0 + mult * clamp(weight, 0, 100));
            weights.put(r, adjusted);
        }
        if (weights.isEmpty()) for (Rarity r : available) weights.put(r, 1.0);

        Rarity pick = weightedPick(weights, rnd);
        log.debug("reward: rarity pick -> {}", pick);
        return pick;
    }

    private BoxType pickTypeAmongAvailable(Collection<Box> registry, Rarity rarity, int weight, ThreadLocalRandom rnd) {
        Set<BoxType> available = registry.stream()
                .filter(b -> b.getRarity() == rarity)
                .map(Box::getType)
                .collect(Collectors.toSet());

        if (available.isEmpty()) {
            available = registry.stream().map(Box::getType).collect(Collectors.toSet());
        }

        Map<BoxType, Double> weights = new HashMap<>();
        for (BoxType t : available) {
            int base = TYPE_BASE.getOrDefault(t, 1);
            double mult = TYPE_PER_WEIGHT.getOrDefault(t, 0.0);
            double adjusted = base * (1.0 + mult * clamp(weight, 0, 100));
            weights.put(t, adjusted);
        }
        BoxType pick = weightedPick(weights, rnd);
        log.debug("reward: type pick -> {}", (pick != null ? pick.getId() : "null"));
        return pick;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static <T> T weightedPick(Map<T, Double> weights, ThreadLocalRandom rnd) {
        double sum = 0.0;
        for (double w : weights.values()) sum += Math.max(0.0, w);
        if (sum <= 0.0) return weights.keySet().iterator().next();

        double roll = rnd.nextDouble() * sum;
        double acc = 0.0;
        T last = null;

        for (Map.Entry<T, Double> e : weights.entrySet()) {
            last = e.getKey();
            acc += Math.max(0.0, e.getValue());
            if (roll < acc) return e.getKey();
        }
        return last;
    }
}
