package club.abelix.api.nms.bossbar;

import club.abelix.api.nms.bossbar.listener.FakeBossbarListener;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

public interface FakeBossbarApi {

    /**
     * Зареристировать листенер
     */
    default void registerBossbarListener(@NonNull Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new FakeBossbarListener(this), plugin);
    }

    /**
     * Коллекция зарегистрированных листенеров
     */
    @NonNull
    Collection<FakeBossbar> getBossbars();


    /**
     * Возвращает текущие боссбары игрока
     */
    @NonNull
    Collection<FakeBossbar> getBossbars(Player player);

    /**
     * Удалить боссбар
     */
    void removeBossbar(FakeBossbar bossbar);

    /**
     * Удаляет все боссбары и удаляет игроков
     */
    void resetBossbars();

    /**
     * Вызывается в FakeBossbar::addReceiver и FakeBossbar::removeReceiver
     * Так что использовать его не нужно
     *
     * @param player  Игрок
     * @param bossbar Боссбар
     * @return Предыдущий боссбар
     */
    Collection<FakeBossbar> setBossbar(@NonNull Player player, @NonNull FakeBossbar bossbar);

    void addBossbar(@NonNull Player player, @NonNull FakeBossbar bossbar);

    void removeBossbars(@NonNull Player player);

    void removeBossbar(@NonNull Player player, @NonNull FakeBossbar bossbar);

    /**
     * @param title - Заголовок боссбара
     * @param v     - Значение боссбара (0F <-> 1F)
     * @return новый боссбар
     */
    FakeBossbar createBossbar(@NonNull String title, float v);

    default FakeBossbar createBossbar(@NonNull String title, float v, float max) {
        return createBossbar(title, v / max);
    }


}
