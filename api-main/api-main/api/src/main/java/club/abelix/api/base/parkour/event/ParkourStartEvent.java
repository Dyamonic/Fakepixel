package club.abelix.api.base.parkour.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerEvent;
import club.abelix.api.base.parkour.LobbyParkour;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.event.Cancellable;

@Getter
public class ParkourStartEvent extends GamerEvent implements Cancellable {

    @Setter
    private boolean cancelled;

    private final LobbyParkour parkour;

    public ParkourStartEvent(@NonNull Gamer gamer, @NonNull LobbyParkour parkour) {
        super(gamer);

        this.parkour = parkour;
    }
}
