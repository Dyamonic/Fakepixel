package club.abelix.metrics.task;

import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.commons.metric.base.MetricGaugeTask;
import club.abelix.network.type.mode.Lobby;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Mini;
import lombok.val;

import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>MasterServerCountTask creation on 25.01.2024 at 03:14</p>
 */

public class MasterServerCountTask extends MetricGaugeTask {

    public MasterServerCountTask() {
        super("mode_server_count_by_master",
                "Count of connected servers by master",
                30, TimeUnit.SECONDS,
                "master");
    }


    @Override
    public void update() {
        for (val master : Master.VALUES) {
            int count = 0;

            Lobby lobby = master.getLobby();

            if (lobby != null) {
                count += ServerManager.IMP.getServers(lobby).size();
            }

            for (Mini mini : Mini.values()) {
                if (mini.getMaster() != master) {
                    continue;
                }

                count += ServerManager.IMP.getServers(mini).size();
            }
            getCollector().labels(master.getName()).set(count);
        }
    }

    @Override
    protected Double getValue() {
        return null;
    }
}