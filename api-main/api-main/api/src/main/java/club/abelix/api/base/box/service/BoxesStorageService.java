package club.abelix.api.base.box.service;

import club.abelix.api.base.box.Box;
import club.abelix.api.base.box.type.BoxType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.currency.GameCurrency;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Log4j2
public class BoxesStorageService {

    @Getter
    private final Map<BoxType, List<Box>> boxes = new HashMap<>();

    public void add(@NotNull final Box box) {
        boxes.computeIfAbsent(box.getType(), k -> new ArrayList<>()).add(box);

        log.info("Added box with rarity: {} Rewards {}", box.getRarity(), box.getRewards().size());

        log.info("[BOX-DEBUG] registered class={} nameKey={} type={}(id={}) rarity={} mode={} "
                        + "goldCost={} coinsCost={} purchasableCoins={} purchasableGold={}",
                box.getClass().getName(),
                box.getNameKey(),
                box.getType().getClass().getSimpleName(),
                box.getType().getId(),
                box.getRarity(),
                box.getMode(),
                box.getCost(GameCurrency.GOLD),
                box.getCost(GameCurrency.COINS),
                box.isPurchasable(GameCurrency.COINS),
                box.isPurchasable(GameCurrency.GOLD)
        );
    }


    public Optional<Box> findByType(BoxType boxType) {
        List<Box> list = boxes.get(boxType);

        if (list == null || list.isEmpty()) return Optional.empty();

        return Optional.of(list.get(0));
    }

    public List<Box> findAllByType(BoxType boxType) {
        List<Box> list = boxes.get(boxType);

        if (list == null) return List.of();

        return list;
    }

    public Optional<Box> findByTypeAndRarity(BoxType boxType, Rarity rarity) {
        List<Box> list = boxes.get(boxType);

        if (list == null) return Optional.empty();

        for (Box b : list) {
            if (b.getRarity() == rarity) return Optional.of(b);
        }

        return Optional.empty();
    }

    public Optional<Box> findByTypeAndRarity(int boxTypeId, Rarity rarity) {
        List<Box> list = null;

        for (Map.Entry<BoxType, List<Box>> e : boxes.entrySet()) {
            if (e.getKey().getId() == boxTypeId) {
                list = e.getValue();
                break;
            }
        }

        if (list == null) return Optional.empty();

        for (Box b : list) {
            if (b.getRarity() == rarity) return Optional.of(b);
        }

        return Optional.empty();
    }

    public List<Rarity> findRaritiesByType(BoxType boxType) {
        List<Box> list = boxes.get(boxType);

        if (list == null || list.isEmpty()) return List.of();

        Set<Rarity> set = new LinkedHashSet<>();

        for (Box b : list) set.add(b.getRarity());

        return new ArrayList<>(set);
    }

    public Collection<Box> getAll() {
        Set<Box> allBoxes = new LinkedHashSet<>();

        for (List<Box> boxList : boxes.values()) {
            allBoxes.addAll(boxList);
        }

        return allBoxes;
    }
}
