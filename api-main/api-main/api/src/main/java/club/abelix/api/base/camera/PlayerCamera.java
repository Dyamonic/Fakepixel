package club.abelix.api.base.camera;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Камера
 */
public interface PlayerCamera {

    /**
     * Игрок
     */
    Player getPlayer();

    /**
     * Изначальная позиция
     * В FollowCamera - это позиция игрока
     * В WhirlCamera - это точка, на которую смотрит игрок
     */
    Location getInitialPosition();

    /**
     * Перенести камеру в определённую позицию
     */
    void move(Location position);

    /**
     * Позиция камеры
     */
    Location getPosition();

    /**
     * Создать камеру: энтити и т.д
     */
    void start();

    /**
     * Удалить камеру
     */
    void destroy(CameraDestroyReason reason);

}
