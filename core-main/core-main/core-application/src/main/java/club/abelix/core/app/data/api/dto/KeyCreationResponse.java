package club.abelix.core.app.data.api.dto;

public record KeyCreationResponse(
        String key,
        long expireAt
) {

}
