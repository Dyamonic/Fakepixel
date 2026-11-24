package club.abelix.api.server.game.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Вызывается, когда челик умер без возможности респана
 *
 * @author tomskiy
 */
public class GameFinalKillEvent extends GameEvent {

    /**
     * Челик, который умер
     */
    @Getter
    private final Gamer gamer;

    /**
     * Челик, который убил ( Может быть NULL )
     */
    @Getter
    private final Gamer killerGamer;

    public GameFinalKillEvent(@NonNull Game game, @NonNull Gamer gamer, @Nullable Gamer killer) {
        super(game);
        this.gamer = gamer;
        this.killerGamer = killer;
    }

    public Player getPlayer() {
        return gamer.getPlayer();
    }

    public Player getKiller() {
        return killerGamer != null ? killerGamer.getPlayer() : null;
    }

}
