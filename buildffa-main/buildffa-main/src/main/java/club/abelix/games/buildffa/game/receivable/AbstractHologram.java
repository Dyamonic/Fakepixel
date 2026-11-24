package club.abelix.games.buildffa.game.receivable;

import club.abelix.api.base.util.hologram.Hologram;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public abstract class AbstractHologram implements Receivable {

    private final Location location;

    public abstract Hologram createHologram(@NotNull Player receiver);

    protected Location hologramLocation() {
        return location.clone().add(0, 1.6, 0);
    }

    @Override
    public final void addReceiver(@NotNull Player player) {
        val hologram = createHologram(player);

        hologram.addReceiver(player);
    }

}
