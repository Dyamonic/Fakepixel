package club.abelix.core.app.data.mapbalancer;

import club.abelix.network.type.map.GameMap;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>ModeMapBalancer creation on 23.12.2023 at 23:34</p>
 */

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameMapMiniBalancer {
    ConcurrentLinkedQueue<GameMap> mapsQueue = new ConcurrentLinkedQueue<>();
    List<GameMap> gameMapImage = new CopyOnWriteArrayList<>();

    public GameMap getBestMap() {
        GameMap bestMap = mapsQueue.poll();
        if (bestMap == null) {
            throw new IllegalStateException("Trying to get game map before filling the manager!");
        }
        mapsQueue.add(bestMap);
        return bestMap;
    }

    public void addMap(@NonNull GameMap gameMap) {
        if (gameMapImage.contains(gameMap)) return;
        gameMapImage.add(gameMap);

        for (int i = 0; i < gameMap.getMultiplier(); ++i) {
            mapsQueue.add(gameMap);
        }
    }
}
