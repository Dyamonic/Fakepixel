package club.abelix.api.base.complex;

import org.bukkit.Location;

public interface ComplexBlock {

    /**
     * Получить локации всех блоков входящих в комплекс
     *
     * @return список локаций
     */
    Location[] getLocations();

    /**
     * Уничтожить блок
     */
    void destroy();

}
