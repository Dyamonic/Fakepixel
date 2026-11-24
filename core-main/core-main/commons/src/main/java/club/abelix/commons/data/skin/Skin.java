package club.abelix.commons.data.skin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@ToString(exclude = {"value", "signature"})
@AllArgsConstructor
public class Skin {

    private final String name;
    private final String uuid;
    private final String value;
    private final String signature;
    private final LocalDateTime timestamp;

    public Skin(final @NonNull String name) {
        this(name, null, null, null);
    }

    public Skin(
            final @NonNull String name,
            final String uuid,
            final String value,
            final String signature
    ) {
        this(name, uuid, value, signature, LocalDateTime.now());
    }

    public boolean isExpired() {
        return timestamp != null &&
                Duration.between(timestamp, LocalDateTime.now()).toHours() > 32;
    }

}