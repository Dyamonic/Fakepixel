package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.GamerPerformanceMetrics;
import club.abelix.network.metrics.DatabasePerformanceMetrics;
import club.abelix.network.metrics.RedisPerformanceMetrics;
import club.abelix.network.metrics.PerformanceMetrics;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

@NamedCommand("coremetrics")
@WithAlias("corestat")
public class MetricsCommand {

    private static final MemoryMXBean MEMORY_MX_BEAN = ManagementFactory.getMemoryMXBean();

    @Executor
    @WithPermission("command.corestats")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        double[] receivedPackets = PerformanceMetrics.RECEIVED_PACKETS.get();
        double[] receivedBites = PerformanceMetrics.TRAFFIC_DOWNLOAD.get();
        double[] sentPackets = PerformanceMetrics.SENT_PACKETS.get();
        double[] sentBites = PerformanceMetrics.TRAFFIC_UPLOAD.get();
        double[] gamerLoad = GamerPerformanceMetrics.GAMER_LOAD_TIME.getAvg();
        double[] sqlLoad = DatabasePerformanceMetrics.getGlobalAvgSqlTimeNanos() > 0 
                ? new double[]{DatabasePerformanceMetrics.getGlobalAvgSqlTimeNanos() / 1_000_000.0, 0, 0} // Simplify for command
                : new double[]{0, 0, 0};

        long heapUsed = MEMORY_MX_BEAN.getHeapMemoryUsage().getUsed() / 1024 / 1024;
        long heapMax = MEMORY_MX_BEAN.getHeapMemoryUsage().getMax() / 1024 / 1024;

        session.sendMessage("§dИнформация о сети:\n" +
                        "  §fПак/С: §2%.2f\n" +
                        "  §fБайт/С: §2%.2f\n\n" +
                        "  §fПринято: §2%.2f/§3%.2f/§d%.2f\n" +
                        "  §fПринято Байт: §2%.2f/§3%.2f/§d%.2f\n" +
                        "  §fОтправлено: §2%.2f/§3%.2f/§d%.2f\n" +
                        "  §fОтправлено Байт: §2%.2f/§3%.2f/§d%.2f\n\n" +
                        "  §dИнформация о загрузке:\n" +
                        "  §fЗагрузка Gamer (1с/30с/1м): §2%.2f/§3%.2f/§d%.2f мс\n" +
                        "  §fЗагрузка SQL (avg 30s): §2%.2f мс\n" +
                        "  §fЗагрузка Redis (avg 30s): §2%.2f мс\n\n" +
                        "  §dJVM:\n" +
                        "  §fHeap: §2%dMB / %dMB",
                PerformanceMetrics.getPacketsPerSecond(),
                PerformanceMetrics.getTrafficPerSecond(),
                receivedPackets[0], receivedPackets[1], receivedPackets[2],
                receivedBites[0], receivedBites[1], receivedBites[2],
                sentPackets[0], sentPackets[1], sentPackets[2],
                sentBites[0], sentBites[1], sentBites[2],
                gamerLoad[0], gamerLoad[1], gamerLoad[2],
                sqlLoad[0],
                RedisPerformanceMetrics.getAvgRedisTimeMillis(),
                heapUsed, heapMax);
    }
}
