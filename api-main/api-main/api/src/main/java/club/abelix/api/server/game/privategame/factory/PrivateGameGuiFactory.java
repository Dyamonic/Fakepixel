package club.abelix.api.server.game.privategame.factory;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import org.jetbrains.annotations.NotNull;

public interface PrivateGameGuiFactory {

    void openSettings(@NotNull Gamer gamer, @NotNull Game game);

    void openTeamSelector(@NotNull Gamer gamer, @NotNull Game game);
}
