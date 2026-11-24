package club.abelix.api.server.lobby.impl.hub.discount.dto;

import java.time.LocalDateTime;

public record CurrentAdvertisementResponseDto(
    LocalDateTime createdAt,
    LocalDateTime expiresAt,
    PromoCodeDto promoCode
) {
}

