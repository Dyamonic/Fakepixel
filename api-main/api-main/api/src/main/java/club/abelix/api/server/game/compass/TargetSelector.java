package club.abelix.api.server.game.compass;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.object.Pair;
import lombok.NonNull;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface TargetSelector {

    Optional<Pair<Player, Double>> select(@NonNull Game game, @NonNull Gamer gamer);

    String getIdentifier();

    default String getDisplayName(@NonNull Language language) {
        return language.getMessage("game_tracker_mode_" + getIdentifier());
    }

    default boolean canSpectatorUse() {
        return false;
    }

}
