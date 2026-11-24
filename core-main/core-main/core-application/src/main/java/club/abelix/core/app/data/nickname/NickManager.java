package club.abelix.core.app.data.nickname;

import club.abelix.commons.data.nickname.NickSqlHandler;
import club.abelix.core.app.data.gamer.Gamer;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NickManager {

    private static final int RANDOM_NAMES_GENERATION_COUNT = 14;

    // --------------------------------------------------
    public static NickManager IMP = new NickManager();
    private final Cache<Integer, Set<String>> randomNicksCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public void clearNicknameCache(@NonNull Gamer gamer) {
        randomNicksCache.cleanUp();
        randomNicksCache.invalidate(gamer.getPlayerId());
    }

    public Set<String> fetchRandomNicknames(@NonNull Gamer gamer) {
        try {
            return randomNicksCache.get(gamer.getPlayerId(),
                    (integer) -> NickSqlHandler.INSTANCE.getRandomNicknames(RANDOM_NAMES_GENERATION_COUNT).join());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

}
