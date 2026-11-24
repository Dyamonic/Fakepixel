package club.abelix.core.app.data.stream.responses;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TwitchTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in")   Long   expiresIn,
        @JsonProperty("token_type")   String tokenType,
        @JsonProperty("status")       Integer status,
        @JsonProperty("message")      String  message
) {
}
