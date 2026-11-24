package club.abelix.games.buildffa.game.event;

import club.abelix.core.bukkit.event.YEvent;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.config.WorldConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * @author milansky
 */
@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public final class GameMapChangeEvent extends YEvent {
    BuildFFAGame game;
    WorldConfig from, to;
}
