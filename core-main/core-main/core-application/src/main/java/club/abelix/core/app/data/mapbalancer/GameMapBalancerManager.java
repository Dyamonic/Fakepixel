package club.abelix.core.app.data.mapbalancer;

import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.mode.Mini;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>MapBalancerManager creation on 23.12.2023 at 23:32</p>
 */

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameMapBalancerManager {

    // =========================================================
    public static final GameMapBalancerManager IMP = new GameMapBalancerManager();
    // =========================================================

    ConcurrentHashMap<Mini, GameMapMiniBalancer> miniBalancer
            = new ConcurrentHashMap<>();

    public GameMapMiniBalancer getMini(@NonNull Mini mini) {
        GameMapMiniBalancer balancer = miniBalancer.get(mini);

        if (balancer == null) {
            balancer = new GameMapMiniBalancer();
            miniBalancer.put(mini, balancer);
            Logging.IMP.info("[BALANCER] New balancer {} ({})",
                    mini.getDisplayName(),
                    mini.name());
        }

        return balancer;
    }

    public void removeBalancer(@NonNull Mini mini) {
        miniBalancer.remove(mini);
    }
}
