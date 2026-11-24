package club.abelix.api.base.playertop.view;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import lombok.NonNull;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public abstract class HologramPlayerTopView extends PlayerTopView {

    private final List<Hologram> holograms = new ArrayList<>();

    public HologramPlayerTopView(Gamer gamer) {
        super(gamer);
    }

    public void addHologram(@NonNull Hologram hologram) {
        holograms.add(hologram);
    }

    protected Hologram createHologram(@NonNull Location location) {
        Hologram hologram = new Hologram(location);
        hologram.addReceiver(gamer.getPlayer());

        holograms.add(hologram);

        return hologram;
    }

    @Override
    public void remove() {
        holograms.forEach(Hologram::remove);
        holograms.clear();
    }
}
