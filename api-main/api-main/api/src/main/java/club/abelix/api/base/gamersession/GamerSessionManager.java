package club.abelix.api.base.gamersession;

import lombok.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: api</p>
 * <p>GamerSessionManager creation on 17.04.2023 at 02:18</p>
 */

public class GamerSessionManager {

    public final static GamerSessionManager INSTANCE = new GamerSessionManager();

    private final Map<Class<? extends GamerSession>, GamerSessionProvider<?>> providers = new HashMap<>();

    public void register(@NonNull Class<? extends GamerSession> sessionCls) {
        providers.put(sessionCls, new GamerSessionProvider<>(sessionCls));
    }

    public GamerSessionProvider<? extends GamerSession> getProvider(@NonNull Class<? extends GamerSession> sessionCls) {
        GamerSessionProvider<?> provider = providers.get(sessionCls);

        if (provider == null) {
            throw new RuntimeException("The gamer session provider has not been registered before!");
        }

        return provider;
    }


    public List<GamerSessionProvider<? extends GamerSession>> getAllProviders() {
        return providers.values().stream().toList();
    }

}
