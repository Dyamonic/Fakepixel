package club.abelix.commons.geo.proxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoProxyResponse(
        String asn,
        String proxy,
        int risk,
        String type
) {
}
