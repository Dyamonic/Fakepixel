package club.abelix.core.app.connection.server.reservation;

import java.time.LocalDateTime;


public record ArenaReservation(LocalDateTime time) {
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(time.plusSeconds(2));
    }
}
