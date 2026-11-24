package club.abelix.api.nms.packet;

import club.abelix.api.nms.event.UpdatePublicStatusEvent;
import club.abelix.core.bukkit.HeartBeat;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Данный объект может быть увиден игроком;
 * <p>
 * Данным объектом может являться боссбар, команда или энтити
 * </p>
 */
@Getter
public abstract class AbstractVisibleObject implements IVisibleObject {

    protected final Set<Player> receivers = new HashSet<>();
    protected final Set<Player> viewers = new HashSet<>();

    protected boolean publicStatus;

    public void setPublicStatus(boolean publicStatus) {
        this.publicStatus = publicStatus;

        HeartBeat.callEvent(new UpdatePublicStatusEvent(this, publicStatus));
    }

    public boolean addReceivers(@NonNull @NotNull Collection<? extends Player> players) {
        return receivers.addAll(players);
    }

    public boolean removeReceivers(@NonNull @NotNull Collection<? extends Player> players) {
        for (Player player : players) {
            removeViewer(player);
        }

        return receivers.removeAll(players);
    }

    public boolean hasReceiver(@NonNull @NotNull Player player) {
        return receivers.contains(player);
    }

    public boolean addViewer(@NonNull @NotNull Player player) {
        return viewers.add(player);
    }

    public boolean removeViewer(@NonNull @NotNull Player player) {
        return viewers.remove(player);
    }

    public boolean hasViewer(@NonNull Player player) {
        return viewers.contains(player);
    }

    public void update(Player player) {
        if (hasViewer(player)) {
            removeViewer(player);
            addViewer(player);
        }
    }

    public abstract void remove();
}
