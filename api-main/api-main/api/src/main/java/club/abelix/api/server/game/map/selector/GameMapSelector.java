package club.abelix.api.server.game.map.selector;

import club.abelix.api.base.util.location.RawLocation;
import club.abelix.api.server.game.Game;
import club.abelix.network.type.map.GameMap;
import lombok.NonNull;
import org.bukkit.World;

public interface GameMapSelector {

    /**
     * Загрузка мира
     *
     * @param game игра
     * @return загруженный мир баккита
     */
    World loadWorld(@NonNull Game game, @NonNull RawLocation center);

    /**
     * Отгрузка текущего мира игры
     *
     * @param game игра
     */
    void unloadAndDeleteWorld(@NonNull Game game);

    /**
     * Выбор карты
     *
     * @param game игра
     * @return техническое название карты
     */
    GameMap selectMap(@NonNull Game game);
}
