package club.abelix.antivpn.data;

import lombok.NonNull;

import java.time.LocalDateTime;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
public record ProxyCheckResult(@NonNull String ip, @NonNull Boolean verdict, @NonNull LocalDateTime added,
                               @NonNull String asn) {
}
