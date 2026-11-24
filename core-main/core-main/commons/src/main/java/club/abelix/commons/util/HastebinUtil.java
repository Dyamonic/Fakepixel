package club.abelix.commons.util;

import club.abelix.network.common.logging.Logging;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@UtilityClass
public class HastebinUtil {

    public String DEFAULT_PASTE_URL = "https://paste.abelix.club/";

    private final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public CompletableFuture<String> paste(@NonNull String text) {
        return paste(DEFAULT_PASTE_URL, text);
    }

    public CompletableFuture<String> paste(@NonNull String url, @NonNull String text) {
        val request = HttpRequest.newBuilder()
                .uri(URI.create(url + "documents"))
                .header("User-Agent", "Abelix")
                .header("charset", "utf-8")
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(text))
                .timeout(Duration.ofSeconds(5))
                .build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).thenCompose(response -> {
            val reader = new BufferedReader(new InputStreamReader(response.body()));
            val responseText = reader.lines().collect(Collectors.joining("\n"));

            val parts = responseText.split("\"");

            if (parts.length <= 3) {
                return CompletableFuture.failedFuture(new RuntimeException("Invalid paste response: " + responseText));
            }

            val identifier = parts[3];

            return CompletableFuture.completedFuture(url + identifier + ".yaml");
        }).exceptionally(ex -> {
            Logging.IMP.stacktrace(ex, "Failed to paste text to {}", url);
            return "none";
        });
    }

}
