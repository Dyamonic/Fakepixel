package club.abelix.api.server.lobby.event;

import club.abelix.core.bukkit.event.YEvent;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerTeleportToSpawnEvent extends YEvent implements Cancellable {

    final Player player;
    @Setter
    boolean cancelled;

    public static @NotNull PlayerTeleportToSpawnEvent create(final @NonNull Player player) {
        return new PlayerTeleportToSpawnEvent(player);
    }
}
