package club.abelix.commons.geo;

import club.abelix.commons.util.json.JsonBodyHandler;
import club.abelix.commons.util.json.JsonBodyPublisher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import w.geo.api.GeoLocation;
import w.geo.api.GeoLocationManager;
import w.geo.api.ImmutableCountry;
import w.geo.api.ImmutableGeoLocation;
import w.util.Env;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RemoteGeoLocationManager implements GeoLocationManager {

    private static final String GEOLOCATION_API_URL = Env.getString("GEO_API_URL", "https://geolocation-service.k8s.abelix.club/") + "geo";
    private static final HttpClient CLIENT = HttpClient.newBuilder().build();

    @Override
    @SneakyThrows
    public @NotNull GeoLocation lookup(@NotNull InetAddress inetAddress) {
        try {
            val request = HttpRequest.newBuilder()
                    .uri(URI.create(GEOLOCATION_API_URL))
                    .POST(JsonBodyPublisher.create(new GeoResolveRequest(inetAddress.getHostAddress())))
                    .build();

            val publisher = JsonBodyHandler.create(GeoResolveResponse.class);
            val response = CLIENT.send(request, publisher).body();

            return ImmutableGeoLocation.create(
                    ImmutableCountry.create(
                            response.countyName(),
                            response.isaCode()),
                    response.city()
            );
        } catch (Exception e) {
            log.error("Failed fetch geolocation to ip {}: {}", inetAddress, e);
        }

        return ImmutableGeoLocation.create(
                ImmutableCountry.create(
                        "unknown",
                        "unknown"),
                "unknown"
        );

    }

}
