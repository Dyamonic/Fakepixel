package club.abelix.api.server.lobby.impl.hub.discount.dto;

import java.time.LocalDateTime;

public record PromoCodeDto(
        String code,
        double discount,
        int maxUsages,
        int currentUsages,
        LocalDateTime expiresAt
) {
}
