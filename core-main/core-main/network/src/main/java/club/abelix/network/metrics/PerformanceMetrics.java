package club.abelix.network.metrics;

public class PerformanceMetrics {

    /**
     * Кол-во полученных байтов
     */
    public static Metric TRAFFIC_DOWNLOAD = new Metric();

    /**
     * Кол-во отправленных байтов
     */
    public static Metric TRAFFIC_UPLOAD = new Metric();

    /**
     * Кол-во отправленных пакетов
     */
    public static Metric SENT_PACKETS = new Metric();

    /**
     * Кол-во обработанных (полученных) пакетов
     */
    public static Metric RECEIVED_PACKETS = new Metric();

    public static Metric PROCESSING_TIME = new Metric();

    /**
     * Время, когда подключене было инициализированно
     */
    public static long START_TIME;

    /**
     * Сумма отправленных и входящих пакетов в секунду
     */
    public static double getPacketsPerSecond() {
        return SENT_PACKETS.getSecond() + RECEIVED_PACKETS.getSecond();
    }

    /**
     * Кол-во трафика в секунду
     */
    public static double getTrafficPerSecond() {
        return TRAFFIC_UPLOAD.getSecond() + TRAFFIC_DOWNLOAD.getSecond();
    }

    public static boolean isMetricsEnabled() {
        return START_TIME != 0;
    }

    public static long getUptime() {
        return System.currentTimeMillis() - START_TIME;
    }

    public static void startMetrics() {
        START_TIME = System.currentTimeMillis();

        // сбрасываем все переменные
        TRAFFIC_DOWNLOAD.reset();
        TRAFFIC_UPLOAD.reset();
        SENT_PACKETS.reset();
        RECEIVED_PACKETS.reset();
        PROCESSING_TIME.reset();
    }

}
