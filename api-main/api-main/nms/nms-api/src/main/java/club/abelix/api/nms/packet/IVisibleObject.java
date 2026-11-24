package club.abelix.api.nms.packet;

import lombok.NonNull;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public interface IVisibleObject {

    Set<? extends Player> getReceivers();

    boolean addReceivers(@NonNull Collection<? extends Player> player);

    boolean removeReceivers(@NonNull Collection<? extends Player> player);

    default boolean addReceiver(@NonNull Player player) {
        return addReceivers(Collections.singleton(player));
    }

    default boolean removeReceiver(@NonNull Player player) {
        return removeReceivers(Collections.singleton(player));
    }

    boolean hasReceiver(@NonNull Player player);

    Set<? extends Player> getViewers();

    boolean addViewer(@NonNull Player player);

    boolean removeViewer(@NonNull Player player);

    boolean hasViewer(@NonNull Player player);

    default void update(Player player) {
        if (removeViewer(player)) {
            addViewer(player);
        }
    }

    boolean isPublicStatus();

    void setPublicStatus(boolean publicStatus);

    void remove();

}
