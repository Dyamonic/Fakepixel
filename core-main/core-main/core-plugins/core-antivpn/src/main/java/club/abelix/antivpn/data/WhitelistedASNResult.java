package club.abelix.antivpn.data;

import lombok.NonNull;

import java.time.LocalDateTime;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
public record WhitelistedASNResult(@NonNull String asn, @NonNull String addedBy, @NonNull LocalDateTime addedAt) {
}
