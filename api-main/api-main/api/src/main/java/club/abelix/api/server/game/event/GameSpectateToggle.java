package club.abelix.api.server.game.event;

import club.abelix.api.server.game.Game;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;

@Getter
public class GameSpectateToggle extends GameEvent {

    private final Player player;
    private final boolean spectate;

    public GameSpectateToggle(@NonNull Game game, @NonNull Player player, boolean spectate) {
        super(game);

        this.player = player;
        this.spectate = spectate;
    }

}
