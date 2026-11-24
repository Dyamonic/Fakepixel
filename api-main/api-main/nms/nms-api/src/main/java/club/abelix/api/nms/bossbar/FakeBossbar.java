package club.abelix.api.nms.bossbar;

import club.abelix.api.nms.packet.IVisibleObject;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface FakeBossbar extends IVisibleObject {

    void setBarStyle(String style);

    String getBarStyle();

    void setBarColor(String color);

    String getBarColor();

    /**
     * 0 <= v <= 1
     */
    void setValue(float v);

    /**
     * 0 <= v <= max
     * <p>
     * Тоже самое, что и setValue(v / max);
     */
    void setValue(float v, float max);

    /**
     * Заголовок боссбара
     */
    void setTitle(String name);

    void updateBossbar(Player player);

    @Override
    default Set<? extends Player> getViewers() {
        return getReceivers();
    }

    @Override
    default boolean addViewer(@NonNull @NotNull Player player) {
        return false;
    }

    @Override
    default boolean removeViewer(@NonNull @NotNull Player player) {
        return false;
    }

    @Override
    default boolean hasViewer(@NonNull Player player) {
        return false;
    }
}
