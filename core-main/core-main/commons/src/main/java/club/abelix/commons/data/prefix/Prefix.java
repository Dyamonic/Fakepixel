package club.abelix.commons.data.prefix;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Prefix {

    private final LocalDateTime expireTime;
    @Setter
    private String text;

    public boolean isExpired() {
        return expireTime.isBefore(LocalDateTime.now());
    }
}
