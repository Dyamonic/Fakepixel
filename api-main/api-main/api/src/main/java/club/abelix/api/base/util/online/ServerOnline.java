package club.abelix.api.base.util.online;

import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.network.online.query.StatusQueries;

public class ServerOnline {

    public static int getOnline() {
        return StarshipBukkit.getInstance()
                .getStatusQueryExecutionService()
                .executeSync(StatusQueries.totalStatusQuery())
                .getTotalOnline();
    }

}
