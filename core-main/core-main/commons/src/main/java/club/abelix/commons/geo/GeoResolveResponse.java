package club.abelix.commons.geo;

public record GeoResolveResponse(
        String city,
        String countyName,
        String isaCode
) {
}
