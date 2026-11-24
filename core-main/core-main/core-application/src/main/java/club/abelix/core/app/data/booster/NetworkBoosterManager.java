package club.abelix.core.app.data.booster;

import club.abelix.commons.data.booster.sql.NetworkBoosterSqlHandler;
import club.abelix.network.type.booster.NetworkBooster;
import lombok.experimental.Delegate;

import java.util.Collection;

/**
 * @author milansky
 */
public final class NetworkBoosterManager {

    public static final NetworkBoosterManager IMP = new NetworkBoosterManager();
    @Delegate
    private final Collection<NetworkBooster> activeBoosters;

    {
        activeBoosters = NetworkBoosterSqlHandler.loadBoosters().join();
    }

    public void save() {
        NetworkBoosterSqlHandler.save(activeBoosters);
    }
}
