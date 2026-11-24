package club.abelix.games.fastbridge.commons.island;

import club.abelix.api.base.util.slime.SlimeWorldUtil;
import club.abelix.games.fastbridge.commons.config.FastBridgeConfig;
import club.abelix.games.fastbridge.commons.config.IslandConfig;
import club.abelix.games.fastbridge.commons.utils.WorldUtility;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IslandSelector {

    Map<IslandConfig, Queue<Island>> islandsByConfig;
    Map<IslandConfig, Queue<Island>> freeIslandsByConfig;
    List<Island> occupiedIslands;
    List<IslandConfig> islandConfigs;
    IslandConfig defaultConfig;
    private IslandSelector(final Map<IslandConfig, Queue<Island>> islandsByConfig,
                           final Map<IslandConfig, Queue<Island>> freeIslandsByConfig,
                           final List<Island> occupiedIslands,
                           final List<IslandConfig> islandConfigs,
                           final IslandConfig defaultConfig) {
        this.islandsByConfig = islandsByConfig;
        this.freeIslandsByConfig = freeIslandsByConfig;
        this.occupiedIslands = occupiedIslands;
        this.islandConfigs = islandConfigs;
        this.defaultConfig = defaultConfig;
    }

    public static IslandSelector create(final FastBridgeConfig config) {
        Map<IslandConfig, Queue<Island>> islandsByConfig = new HashMap<>();

        for (IslandConfig cnf : config.getIslandConfigs()) {
            val world = SlimeWorldUtil.loadSLimeWorld(cnf.worldName());
            if (world == null) {
                log.error("Unable to load world from SWM worldName={}", cnf.worldName());
                continue;
            }

            WorldUtility.prepareWorld(world);
            Queue<Island> queue = new ArrayDeque<>();
            for (int i = 0; i < cnf.islandsCount(); i++) {
                Island island = cnf.createIsland(world, i);
                queue.add(island);
                log.info("Created island offset={} in world={}", i, cnf.worldName());
            }
            islandsByConfig.put(cnf, queue);
        }

        Map<IslandConfig, Queue<Island>> sortedIslandsByConfig = islandsByConfig.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().getAmount()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldVal, newVal) -> oldVal,
                        LinkedHashMap::new
                ));

        IslandConfig defaultConfig = sortedIslandsByConfig.keySet().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No island config available"));

        Map<IslandConfig, Queue<Island>> freeIslandsByConfig = new HashMap<>();

        for (Map.Entry<IslandConfig, Queue<Island>> entry : sortedIslandsByConfig.entrySet()) {
            Queue<Island> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(Island::getNumber));
            priorityQueue.addAll(entry.getValue());
            freeIslandsByConfig.put(entry.getKey(), priorityQueue);
        }

        return new IslandSelector(
                sortedIslandsByConfig,
                freeIslandsByConfig,
                new ArrayList<>(),
                new ArrayList<>(config.getIslandConfigs()),
                defaultConfig
        );
    }

    /**
     * Возвращает Optional свободного острова для заданного конфига.
     * Если остров свободен, он находится на голове приоритетной очереди.
     */
    public Optional<Island> findFreeIsland(final IslandConfig config) {
        Queue<Island> freeQueue = freeIslandsByConfig.get(config);

        if (freeQueue != null && !freeQueue.isEmpty()) {
            Island island = freeQueue.peek();
            if (island.getOccupiedBy() == null) {
                return Optional.of(island);
            }
        }

        return Optional.empty();
    }

    /**
     * Возвращает Optional первого свободного острова вне зависимости от конфига.
     * Приоритет отдаётся островам дефолтного конфига.
     */
    public Optional<Island> findFreeIsland() {
        Queue<Island> defaultQueue = freeIslandsByConfig.get(defaultConfig);

        if (defaultQueue != null && !defaultQueue.isEmpty()) {
            Island island = defaultQueue.peek();

            if (island.getOccupiedBy() == null) {
                return Optional.of(island);
            }
        }

        return freeIslandsByConfig.entrySet().stream()
                .filter(e -> !e.getKey().equals(defaultConfig))
                .map(Map.Entry::getValue)
                .flatMap(Queue::stream)
                .filter(island -> island.getOccupiedBy() == null)
                .min(Comparator.comparingInt(Island::getNumber));
    }

    /**
     * Поиск свободного острова для заданного конфига по направлению.
     * Выбирается остров, находящийся в нужном направлении и с минимальным расстоянием от точки.
     */
    public Optional<Island> getFreeIsland(final IslandConfig islandConfig,
                                          final Location relativeSearchPoint,
                                          final Vector searchDirection) {
        Queue<Island> queue = freeIslandsByConfig.get(islandConfig);

        if (queue == null || queue.isEmpty()) {
            return Optional.empty();
        }

        return queue.stream()
                .filter(island -> {
                    Vector toRespawn = island.getRespawnLocation().toVector().subtract(relativeSearchPoint.toVector());
                    return toRespawn.dot(searchDirection) > 0;
                })
                .min(Comparator.comparingDouble(island -> island.getRespawnLocation().distance(relativeSearchPoint)));
    }

    /**
     * Альтернативный метод поиска свободного острова по направлению без учёта конфига.
     * Среди всех свободных островов выбирается тот, что удовлетворяет условиям.
     */
    public Optional<Island> findNearestFreeIslandByDirection(final Location relativeSearchPoint, final Vector searchDirection) {
        return freeIslandsByConfig.values().stream()
                .flatMap(Queue::stream)
                .filter(island -> {
                    Vector toRespawn = island.getRespawnLocation().toVector().subtract(relativeSearchPoint.toVector());
                    return toRespawn.dot(searchDirection) > 0;
                })
                .min(Comparator.comparingDouble(island -> island.getRespawnLocation().distance(relativeSearchPoint)));
    }
}
