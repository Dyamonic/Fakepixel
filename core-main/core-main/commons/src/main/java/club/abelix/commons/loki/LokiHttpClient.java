package club.abelix.commons.loki;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class LokiHttpClient {

    private final HttpClient httpClient;
    private final URI pushUri;
    private final String tenantHeaderName;
    private final String tenantId;
    private final Duration timeout;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_QUEUE_SIZE = 10_000;

    private final Queue<LokiMessage> pendingMessages = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queuedCount = new AtomicInteger(0);
    private final ReentrantLock flushLock = new ReentrantLock();
    private final AtomicLong lastFlushTime = new AtomicLong(0);

    public LokiHttpClient(String baseUrl, String tenantId) {
        this(baseUrl, "X-Scope-OrgID", tenantId, Duration.ofSeconds(10));
    }

    public LokiHttpClient(String baseUrl, String tenantHeaderName, String tenantId, Duration timeout) {
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(tenantHeaderName, "tenantHeaderName");
        Objects.requireNonNull(timeout, "timeout");

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();

        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        this.pushUri = URI.create(normalizedBaseUrl + "/loki/api/v1/push");
        this.tenantHeaderName = tenantHeaderName;
        this.tenantId = tenantId;
        this.timeout = timeout;
    }

    public void queue(Map<String, String> labels, String logLine) {
        LokiMessage msg = new LokiMessage(labels, logLine, Instant.now());

        while (true) {
            int current = queuedCount.get();
            if (current >= MAX_QUEUE_SIZE) {
                log.error("Loki queue is full ({} messages), dropping log: labels={}, logLine={}",
                        current, labels, logLine);
                return;
            }
            if (queuedCount.compareAndSet(current, current + 1)) {
                break;
            }
        }

        pendingMessages.add(msg);

        long now = System.currentTimeMillis();

        if (queuedCount.get() >= BATCH_SIZE || now - lastFlushTime.get() >= timeout.toMillis()) {
            flush();
        }
    }

    public void flush() {
        if (!flushLock.tryLock()) {
            return;
        }

        List<LokiMessage> batch = new ArrayList<>(BATCH_SIZE);

        try {
            while (true) {
                batch.clear();

                for (int i = 0; i < BATCH_SIZE; i++) {
                    LokiMessage msg = pendingMessages.poll();
                    if (msg == null) {
                        break;
                    }
                    queuedCount.decrementAndGet();
                    batch.add(msg);
                }

                if (batch.isEmpty()) {
                    return;
                }

                pushBatchAsync(List.copyOf(batch));
                lastFlushTime.set(System.currentTimeMillis());
            }
        } finally {
            flushLock.unlock();
        }
    }

    private void requeue(List<LokiMessage> batch) {
        for (LokiMessage msg : batch) {
            pendingMessages.add(msg);
            queuedCount.incrementAndGet();
        }
    }

    private void pushBatchAsync(List<LokiMessage> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        Map<Map<String, String>, List<LokiMessage>> grouped = new LinkedHashMap<>();

        for (LokiMessage message : batch) {
            Map<String, String> labelsCopy = new LinkedHashMap<>(message.labels());
            grouped.computeIfAbsent(labelsCopy, k -> new ArrayList<>()).add(message);
        }

        StringBuilder json = new StringBuilder(1024);
        json.append("{\"streams\":[");

        boolean firstStream = true;
        for (Map.Entry<Map<String, String>, List<LokiMessage>> entry : grouped.entrySet()) {
            if (!firstStream) {
                json.append(",");
            }
            firstStream = false;

            json.append("{\"stream\":");
            json.append(mapToJson(entry.getKey()));
            json.append(",\"values\":[");

            boolean firstValue = true;
            for (LokiMessage message : entry.getValue()) {
                if (!firstValue) {
                    json.append(",");
                }
                firstValue = false;

                json.append("[\"")
                        .append(toEpochNanos(message.timestamp()))
                        .append("\",\"")
                        .append(jsonEscape(message.logLine()))
                        .append("\"]");
            }

            json.append("]}");
        }

        json.append("]}");

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(pushUri)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));

        if (tenantId != null && !tenantId.isBlank()) {
            builder.header(tenantHeaderName, tenantId);
        }

        httpClient.sendAsync(
                builder.build(),
                HttpResponse.BodyHandlers.ofString()
        ).whenComplete((response, throwable) -> {
            if (throwable != null) {
                // todo - have a look at it in future
                // log.error("Loki push batch failed", throwable);
                requeue(batch);
                return;
            }

            int code = response.statusCode();
            if (code < 200 || code >= 300) {
                // log.error("Loki push batch failed: HTTP {}, body: {}", code, response.body());
                requeue(batch);
            }
        });
    }

    public void pushLog(Map<String, String> labels, String logLine) throws IOException, InterruptedException {
        pushLog(labels, logLine, Instant.now());
    }

    public void pushLog(Map<String, String> labels, String logLine, Instant timestamp)
            throws IOException, InterruptedException {

        validateLabels(labels);
        Objects.requireNonNull(logLine, "logLine");
        Objects.requireNonNull(timestamp, "timestamp");

        String nanos = toEpochNanos(timestamp);
        String payload = buildPushPayload(labels, logLine, nanos);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(pushUri)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));

        if (tenantId != null && !tenantId.isBlank()) {
            builder.header(tenantHeaderName, tenantId);
        }

        HttpResponse<String> response = httpClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString()
        );

        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            throw new IOException("Loki push failed: HTTP " + code + ", body: " + response.body());
        }
    }

    private static void validateLabels(Map<String, String> labels) {
        Objects.requireNonNull(labels, "labels");
        if (labels.isEmpty()) {
            throw new IllegalArgumentException("Loki stream must contain at least one label");
        }
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("Label key must not be blank");
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("Label value must not be null");
            }
        }
    }

    private static String buildPushPayload(Map<String, String> labels, String logLine, String epochNanos) {
        String streamJson = mapToJson(labels);
        String escapedLogLine = jsonEscape(logLine);

        return """
                {
                  "streams": [
                    {
                      "stream": %s,
                      "values": [
                        ["%s", "%s"]
                      ]
                    }
                  ]
                }
                """.formatted(streamJson, epochNanos, escapedLogLine);
    }

    private static String mapToJson(Map<String, String> map) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            joiner.add("\"" + jsonEscape(entry.getKey()) + "\": \"" + jsonEscape(entry.getValue()) + "\"");
        }
        return joiner.toString();
    }

    private static String toEpochNanos(Instant instant) {
        long seconds = instant.getEpochSecond();
        int nanos = instant.getNano();
        return Long.toString(seconds * 1_000_000_000L + nanos);
    }

    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private record LokiMessage(Map<String, String> labels, String logLine, Instant timestamp) {

    }

}