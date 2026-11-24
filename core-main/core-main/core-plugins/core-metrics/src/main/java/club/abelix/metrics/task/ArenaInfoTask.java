package club.abelix.metrics.task;

import club.abelix.commons.metric.base.MetricInfoTask;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.network.type.mode.Mini;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.concurrent.TimeUnit;

@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class ArenaInfoTask extends MetricInfoTask {

    ServerManager manager = ServerManager.IMP;

    public ArenaInfoTask() {
        super("arena",
                "Arena information", 30, TimeUnit.SECONDS,
                "server", "can_join", "state", "mini", "master", "game_map", "health");
    }

    @Override
    public void update() {
        for (val mini : Mini.VALUES) {
            val serverList = manager.getServers(mini);
            
            for (val server : serverList) {
                if (!(server instanceof ArenaServer arena)) {
                    continue;
                }
                
                val state = arena.getState();
                
                getCollector().labels(
                        server.getName(),
                        String.valueOf(server.canJoin()),
                        state.name(),
                        mini.getName(),
                        mini.getMaster().getName(),
                        arena.getMap().getTechnicalName(),
                        String.valueOf(!server.isUnhealthy())
                ).info();
            }
        }
    }
}