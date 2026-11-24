package club.abelix.api.server.lobby.impl.hub.discount;


import club.abelix.api.server.lobby.impl.hub.discount.dto.CurrentAdvertisementResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class DiscountApiService {

    private final String endpointApi;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModules(new JavaTimeModule());
    private LocalDateTime lastFetch = null;
    private CurrentAdvertisementResponseDto lastResponse = null;

    private Optional<CurrentAdvertisementResponseDto> fetchCurrentAdvertisement() {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpointApi + "/api/v1/advertisement/current"))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode == 404) {
                return Optional.empty();
            }

            if (statusCode != 200) {
                throw new RuntimeException(
                        "Failed to fetch adv: HTTP " + statusCode + ", body: " + responseBody
                );
            }

            return Optional.of(OBJECT_MAPPER.readValue(responseBody, CurrentAdvertisementResponseDto.class));

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error while discount API", e);
        }
    }

    public Optional<CurrentAdvertisementResponseDto> getCurrentAdvertisement() {
        if (lastFetch == null || LocalDateTime.now().minusHours(1).isAfter(lastFetch)) {
            try {
                val apiResult = fetchCurrentAdvertisement();

                if (apiResult.isEmpty()) {
                    lastFetch = LocalDateTime.now();
                    return Optional.empty();
                }

                lastResponse = apiResult.get();
                lastFetch = LocalDateTime.now();
            } catch (Exception e) {
                log.error("Failed get current advertisement", e);
                return Optional.empty();
            }
        }

        return Optional.ofNullable(lastResponse);

    }
}