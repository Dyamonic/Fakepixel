package club.abelix.api.base.util.hologram.line;

import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public interface HologramLine {

    /**
     * Установить дистанцию на которой видима
     */

    void setVisibilityDistance(int distance);

    /**
     * Показ линии игроку (добавление получателя энтити)
     */
    void showTo(@NonNull Player player);

    /**
     * Удаление линии для игрока
     */
    void remove(@NonNull Player player);

    /**
     * Телепорт линии на новую локацию
     */
    void teleport(@NonNull Location location);

    /**
     * Получить модификатор расстояния до следующей строки
     */
    double getLineDistanceModifier();

    /**
     * Сделать линию публичной (видной по умолчанию всем)
     */
    void setPublicStatus(boolean publicStatus);

    /**
     * Установить действие при клике на эту голограмму
     */
    void setClickAction(@NonNull Consumer<Player> clickAction);

    /**
     * Полное удаление линии
     */
    void remove();

}
