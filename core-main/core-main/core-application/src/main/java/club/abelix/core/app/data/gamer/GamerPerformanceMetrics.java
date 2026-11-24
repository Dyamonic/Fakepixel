package club.abelix.core.app.data.gamer;

import club.abelix.network.metrics.Metric;

public class GamerPerformanceMetrics {

    public static final Metric GAMER_LOAD_TIME = new Metric();

    public static void startMetrics() {
        GAMER_LOAD_TIME.reset();
    }
}
