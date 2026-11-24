package club.abelix.games.fastbridge.commons.island;

import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.commons.config.IslandConfig;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

@Log4j2
@RequiredArgsConstructor(staticName = "create")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IslandService {

    IslandSelector selector;

    /**
     * Попытка присоединения игрока (FastBridgeGamer) к острову.
     * Если остров уже занят, возвращается false.
     * Остров извлекается из приоритетной очереди свободных, и добавляется в список занятых.
     */
    public boolean joinIsland(final Island island, final FastBridgeGamer gamer) {
        if (island.getOccupiedBy() != null) {
            return false;
        }

        val config = island.getRawConfig();
        Queue<Island> freeQueue = selector.getFreeIslandsByConfig().get(config);

        if (freeQueue != null && !freeQueue.isEmpty() && freeQueue.peek().equals(island)) {
            freeQueue.poll();
        } else if (freeQueue != null) {
            freeQueue.remove(island);
        }

        selector.getOccupiedIslands().add(island);
        island.joinGamer(gamer);
        log.info("Gamer {} joined island: {} Available islands for config {}: {}/{}",
                gamer.getGamer().getName(),
                island,
                config.worldName(),
                freeQueue != null ? freeQueue.size() : 0,
                selector.getIslandsByConfig().get(config).size());
        return true;
    }

    /**
     * Освобождение острова.
     * Остров удаляется из списка занятых и возвращается в приоритетную очередь свободных.
     */
    public void exitIsland(final Island island) {
        selector.getOccupiedIslands().remove(island);
        val config = island.getRawConfig();

        Queue<Island> freeQueue = selector.getFreeIslandsByConfig()
                .computeIfAbsent(config, k ->
                        new PriorityQueue<>(Comparator.comparingInt(Island::getNumber)
                        )
                );

        if (!freeQueue.contains(island)) {
            freeQueue.add(island);
        }

        island.exitGamer();

        log.info("Gamer left island: {} Available islands for config {}: {}/{}",
                island,
                config.worldName(),
                freeQueue.size(),
                selector.getIslandsByConfig().get(config).size());
    }

    /**
     * Переключение островов.
     * Сначала освобождается старый остров, затем игрок пытается занять новый.
     * Если новый остров уже занят, возвращается false.
     */
    public boolean switchIsland(final Island oldIsland, final Island newIsland, final FastBridgeGamer gamer) {
        if (newIsland.getOccupiedBy() != null) {
            return false;
        }
        exitIsland(oldIsland);
        log.info("Gamer switched from island: {} to island: {}",
                oldIsland,
                newIsland);
        return joinIsland(newIsland, gamer);
    }

    /**
     * Переключение островов по конфигу.
     * Освобождается текущий остров игрока и пытается занять свободный остров из указанного конфига.
     */
    public boolean switchIsland(final FastBridgeGamer gamer, final IslandConfig config) {
        val oldIsland = gamer.getIsland();

        if (oldIsland == null) {
            return false;
        }

        val newIslandOpt = selector.findFreeIsland(config);

        return newIslandOpt.filter(island -> switchIsland(oldIsland, island, gamer)).isPresent();
    }
}
