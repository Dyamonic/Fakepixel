package club.abelix.api.base.gamersession;

import club.abelix.api.base.gamer.Gamer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: api</p>
 * <p>GamerSession creation on 17.04.2023 at 01:45</p>
 */


@Getter
@RequiredArgsConstructor
public abstract class GamerSession {
    private final Gamer gamer;

    /**
     * Загружаем все необходимые данные
     */
    public abstract void load();

    /**
     * Вызывается при выходе игрока
     */
    public void unload() {
    }

}
