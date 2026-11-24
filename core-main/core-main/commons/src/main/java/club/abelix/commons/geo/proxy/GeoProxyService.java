package club.abelix.commons.geo.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class GeoProxyService {

    String serviceUrl;
    HttpClient httpClient = HttpClient.newHttpClient();
    ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public CompletableFuture<GeoProxyResponse> check(String ip) {
        val request = new GeoProxyRequest(ip);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "proxy"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                .timeout(Duration.ofMillis(1000))
                .build();

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("Failed to make proxy check request (" + response.statusCode() + ")");
                    }

                    JsonNode node;
                    try {
                        node = objectMapper.readTree(response.body());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        return objectMapper.readValue(node.toString(), GeoProxyResponse.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
