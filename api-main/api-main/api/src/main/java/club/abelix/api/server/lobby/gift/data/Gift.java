package club.abelix.api.server.lobby.gift.data;

import java.time.LocalDateTime;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
public record Gift(int from, int to, GiftType type, LocalDateTime giftedAt) {
}