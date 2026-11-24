package club.abelix.metrics;

import club.abelix.core.app.api.plugin.CorePlugin;
import club.abelix.metrics.listener.PlayerJoinsMetricListener;
import club.abelix.metrics.listener.PlayerLeaveListener;
import club.abelix.metrics.listener.ServerMovingListener;
import club.abelix.metrics.task.*;
import club.abelix.network.common.logging.Logging;
import io.prometheus.client.exporter.HTTPServer;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.net.InetSocketAddress;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>MetricsCorePlugin creation on 25.01.2024 at 01:01</p>
 */

@FieldDefaults(level = AccessLevel.PRIVATE)
public class MetricsCorePlugin extends CorePlugin {

    HTTPServer prometheusHttpServer;

    private void registerTasks() {
        new CoreTrafficReceivedPacketPerSecondTask().start();
        new CoreTrafficSentPacketPerSecondTask().start();
        new CoreTrafficBytesReceivedPerSecondTask().start();
        new CoreTrafficBytesSentPerSecondTask().start();
        new GlobalOnlineTask(getCore().getStatusQueryExecutionService()).start();
        new GlobalServerCountTask().start();
        new PartyCountTask().start();
        new OnlineByCountryTask().start();
        new OnlineByDomainTask().start();
        new PlayerTotalAccountsTask().start();
        new BansCountTask().start();
        new MutesCountTask().start();
        new ArenaStatusCountTask().start();
        new ArenaCountTask().start();
        new ArenaInfoTask().start();
        new PingPercentileTask().start();
        new PacketAvgProcessingTimeTask().start();
        new PacketProcessingDetailTask().start();
        new GamerLoadAvgTimeTask().start();
        new DatabaseAvgSqlTimeTask().start();
        new DatabaseSqlDetailTask().start();
        new JvmHealthTask().start();
        new JvmGcTask().start();
        new RedisAvgWaitTimeTask().start();
        new MasterOnlineCountTask(getCore().getStatusQueryExecutionService()).start();
        new MiniOnlineCountTask(getCore().getStatusQueryExecutionService()).start();
        new MasterServerCountTask().start();
        new ServerModeOnlineCountTask().start();
        new ServerModeCountServerTask().start();
        new OnlineByProxyTask().start();

        val joinTask = new PlayerJoinsTask();
        getCore().getPluginManager().registerListener(this,
                new PlayerJoinsMetricListener(joinTask));
        joinTask.start();

        val leaveTask = new PlayerLeaveTask();
        getCore().getPluginManager().registerListener(this,
                new PlayerLeaveListener(leaveTask));
        leaveTask.start();

        val moveTask = new PlayerMoveTask();
        getCore().getPluginManager().registerListener(this,
                new ServerMovingListener(moveTask));
        moveTask.start();
    }

    @Override
    @SneakyThrows
    public void onEnable() {
        saveDefaultConfig();

        if (!getConfig().getBoolean("enable")) {
            return;
        }

        prometheusHttpServer = new HTTPServer.Builder()
                .withDaemonThreads(true)
                .withInetSocketAddress(new InetSocketAddress(
                        getConfig().getString("http.host"),
                        getConfig().getInt("http.port")
                ))
                .build();

        registerTasks();
        Logging.IMP.info("[Metrics] All tasks success registered!");
    }

    @Override
    public void onDisable() {
        prometheusHttpServer.close();
    }
}
