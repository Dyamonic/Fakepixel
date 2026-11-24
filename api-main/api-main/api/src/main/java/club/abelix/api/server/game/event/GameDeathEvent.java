package club.abelix.api.server.game.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.entity.Player;

@Getter
public class GameDeathEvent extends GameEvent {

    // Вызывается, когда игрок помирает в игре

    private final Gamer gamer;

    @Setter
    private boolean allowItemDrops, allowLevelDrops;

    public GameDeathEvent(@NonNull Game game, @NonNull Gamer gamer) {
        super(game);

        this.gamer = gamer;
    }

    public Player getPlayer() {
        return gamer.getPlayer();
    }

}
