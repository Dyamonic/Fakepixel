package club.abelix.commons.data.action;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Getter
public class Action {

    private final String name;
    private final String ip;

    private final LocalDateTime time;

    private final ActionType type;
    private final int vk;

    public boolean hasIp() {
        return this.ip != null;
    }

    public boolean hasVk() {
        return this.vk != 0;
    }
}
