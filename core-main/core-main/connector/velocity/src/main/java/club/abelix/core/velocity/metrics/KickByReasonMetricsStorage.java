package club.abelix.core.velocity.metrics;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Getter
public final class KickByReasonMetricsStorage {

    private static final int MAX_REASONS = 500;
    private final Map<String, LongAdder> reasonsCountMap = new ConcurrentHashMap<>();

    public void incrementReasonCount(String reason) {
        String normalized = normalize(reason);

        if (reasonsCountMap.size() >= MAX_REASONS && !reasonsCountMap.containsKey(normalized)) {
            reasonsCountMap.computeIfAbsent("OTHER", k -> new LongAdder()).increment();
            return;
        }

        reasonsCountMap.computeIfAbsent(normalized, k -> new LongAdder()).increment();
    }

    private String normalize(String reason) {
        if (reason == null || reason.isEmpty()) {
            return "unknown";
        }

        String result = reason.replaceAll("\\d+", "#").trim();

        if (result.length() > 100) {
            result = result.substring(0, 97) + "...";
        }

        return result;
    }

}
