package club.abelix.api.server.game.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Location;

@Getter
public class GameSpawnLocationEvent extends GameEvent {

    private final Gamer gamer;

    @Setter
    private Location location;

    public GameSpawnLocationEvent(@NonNull Game game, @NonNull Gamer gamer, @NonNull Location location) {
        super(game);

        this.gamer = gamer;
        this.location = location;
    }
}
