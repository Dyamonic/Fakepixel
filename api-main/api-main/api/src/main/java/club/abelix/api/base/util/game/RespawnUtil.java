package club.abelix.api.base.util.game;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import lombok.NonNull;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class RespawnUtil {

    private RespawnUtil() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public void setRespawn(@NonNull Game game, @NonNull Player player, @NonNull Consumer<Player> onRespawn,
                           int seconds) {
        Gamer gamer = Gamer.getGamer(player);

        if (gamer == null) {
            return;
        }

        game.setRespawn(gamer, seconds).thenAccept(x -> onRespawn.accept(x.getPlayer()));
    }

}
