package club.abelix.api.server.game.manager;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.stats.GameStatsCache;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.val;

public class GameStatsManager {

    // в случае если на сервере нет никаких игр, сохранение будет проходить через этот менеджер
    public static GameStatsManager DEFAULT = new GameStatsManager();

    @Getter
    private final TIntObjectMap<GameStatsCache> cacheMap = new TIntObjectHashMap<>();

    public GameStatsCache getStatsCache(Gamer gamer) {
        return cacheMap.get(gamer.getPlayerId());
    }

    public GameStatsCache getOrCreateStatsCache(Gamer gamer) {
        var cache = getStatsCache(gamer);

        if (cache == null) {
            cacheMap.put(gamer.getPlayerId(), cache = new GameStatsCache(gamer));
        } else {
            cache.rejoin(gamer);
        }

        return cache;
    }

    public void transferStatsCache(Gamer gamer, GameStatsManager target) {
        if (target == null || target == this) {
            return;
        }

        GameStatsCache cache = cacheMap.remove(gamer.getPlayerId());

        if (cache != null) {
            target.acceptTransferredCache(gamer, cache);
        }
    }

    private void acceptTransferredCache(Gamer gamer, GameStatsCache transferredCache) {
        GameStatsCache currentCache = getStatsCache(gamer);

        if (currentCache == null) {
            transferredCache.rejoin(gamer);
            cacheMap.put(gamer.getPlayerId(), transferredCache);
            return;
        }

        currentCache.mergeFrom(transferredCache);
        currentCache.rejoin(gamer);
    }

    public void save(Gamer gamer) {
        val cache = getStatsCache(gamer);

        if (cache != null) {
            cache.save();
        }
    }

    public void saveAndRemove(Gamer gamer) {
        saveAndRemove(gamer.getPlayerId());
    }

    public void saveAndRemove(int playerId) {
        val cache = cacheMap.remove(playerId);

        if (cache != null) {
            cache.save();
        }
    }

    public void flushAll(boolean sync) {
        for (val cache : cacheMap.valueCollection()) {
            if (!cache.isSaved()) {
                cache.save(sync);
            }
        }

        cacheMap.clear();
    }
}
