package club.abelix.api.tournament.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import w.util.Env;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
public final class TelegramChannelSubscriptionClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String botToken;
    private final String apiBaseUrl;

    public TelegramChannelSubscriptionClient(HttpClient httpClient, String botToken, String apiBaseUrl) {
        this.httpClient = httpClient;
        this.botToken = botToken == null ? "" : botToken.trim();
        this.apiBaseUrl = normalizeBaseUrl(apiBaseUrl);
    }

    public static TelegramChannelSubscriptionClient createDefault() {
        return new TelegramChannelSubscriptionClient(
                HttpClient.newHttpClient(),
                Env.getString("TELEGRAM_BOT_TOKEN", ""),
                Env.getString("TELEGRAM_API_BASE_URL", "https://api.telegram.org")
        );
    }

    public boolean isConfigured() {
        return !botToken.isBlank();
    }

    public CompletableFuture<SubscriptionCheckResult> checkSubscription(String chatId, long telegramUserId) {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(SubscriptionCheckResult.failed("Telegram bot token is not configured"));
        }

        if (telegramUserId <= 0) {
            return CompletableFuture.completedFuture(SubscriptionCheckResult.notSubscribed());
        }

        String encodedChatId = URLEncoder.encode(chatId, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("%s/bot%s/getChatMember?chat_id=%s&user_id=%d".formatted(apiBaseUrl, botToken, encodedChatId, telegramUserId)))
                .header("Accept", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseResponse)
                .exceptionally(throwable -> {
                    Throwable cause = unwrapCompletionException(throwable);
                    log.warn("Failed to check Telegram subscription for chat {} and user {}", chatId, telegramUserId, cause);
                    return SubscriptionCheckResult.failed(cause.getMessage());
                });
    }

    private SubscriptionCheckResult parseResponse(HttpResponse<String> response) {
        String body = response.body();

        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);

            if (response.statusCode() == 429 || root.path("error_code").asInt() == 429) {
                return SubscriptionCheckResult.rateLimited(readRetryAfterSeconds(root));
            }

            if (!root.path("ok").asBoolean()) {
                return SubscriptionCheckResult.failed(root.path("description").asText("Telegram API request failed"));
            }

            JsonNode result = root.path("result");
            String status = result.path("status").asText("").toLowerCase(Locale.ROOT);

            return switch (status) {
                case "creator", "administrator", "member" -> SubscriptionCheckResult.subscribed();
                case "restricted" -> result.path("is_member").asBoolean(false)
                        ? SubscriptionCheckResult.subscribed()
                        : SubscriptionCheckResult.notSubscribed();
                case "left", "kicked" -> SubscriptionCheckResult.notSubscribed();
                default -> SubscriptionCheckResult.failed("Unsupported chat member status: " + status);
            };
        } catch (IOException e) {
            throw new CompletionException("Failed to parse Telegram API response", e);
        }
    }

    private static @Nullable Integer readRetryAfterSeconds(JsonNode root) {
        JsonNode retryAfter = root.path("parameters").path("retry_after");

        if (!retryAfter.canConvertToInt()) {
            return null;
        }

        return retryAfter.asInt();
    }

    private static String normalizeBaseUrl(String apiBaseUrl) {
        String normalized = apiBaseUrl == null || apiBaseUrl.isBlank()
                ? "https://api.telegram.org"
                : apiBaseUrl.trim();

        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private static Throwable unwrapCompletionException(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }

        return throwable;
    }

    public record SubscriptionCheckResult(Status status, @Nullable Integer retryAfterSeconds, @Nullable String description) {

        public static SubscriptionCheckResult subscribed() {
            return new SubscriptionCheckResult(Status.SUBSCRIBED, null, null);
        }

        public static SubscriptionCheckResult notSubscribed() {
            return new SubscriptionCheckResult(Status.NOT_SUBSCRIBED, null, null);
        }

        public static SubscriptionCheckResult rateLimited(@Nullable Integer retryAfterSeconds) {
            return new SubscriptionCheckResult(Status.RATE_LIMITED, retryAfterSeconds, null);
        }

        public static SubscriptionCheckResult failed(@Nullable String description) {
            return new SubscriptionCheckResult(Status.FAILED, null, description);
        }
    }

    public enum Status {
        SUBSCRIBED,
        NOT_SUBSCRIBED,
        RATE_LIMITED,
        FAILED
    }
}
