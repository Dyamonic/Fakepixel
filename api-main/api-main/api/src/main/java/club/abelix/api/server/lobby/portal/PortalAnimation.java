package club.abelix.api.server.lobby.portal;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.commons.util.math.MathUtil;
import lombok.NonNull;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PortalAnimation extends AbstractTask {

    private static final double LINE_HEIGHT = 0.25;

    private static final double AMPLITUDE = 10 * LINE_HEIGHT;
    private static final double STEP = 0.028;

    @NonNull
    private final Location bottom;

    private final Hologram[] holograms;
    private double x;
    private boolean down;

    public PortalAnimation(@NonNull AbstractPlugin plugin, @NonNull Location bottom, @NonNull String info) {
        super(plugin);

        this.bottom = bottom.add(0, LINE_HEIGHT, 0);

        String outer = "§3";
        String inner = "§b";

        List<String> lines = new ArrayList<>(Arrays.asList(
                info,
                "",
                outer + "█" + inner + "█" + outer + "█",
                outer + "█" + inner + "█" + outer + "█",
                outer + "█" + inner + "█" + outer + "█",
                outer + "█" + inner + "█" + outer + "█",
                outer + "█" + inner + "██   " + inner + "█" + outer + "█",
                outer + "█" + inner + "██" + outer + "█",
                outer + "█" + inner + "█" + outer + "█",
                outer + "██",
                outer + "█"
        ));

        this.holograms = new Hologram[lines.size()];

        for (int i = 0; i < holograms.length; i++) {
            holograms[i] = new Hologram(bottom);
            holograms[i].addTextLine("§e" + lines.get(i));
            holograms[i].setPublicStatus(true);
        }

        timer(0, 1);
    }

    @Override
    public void run() {
        if (x > 1) {
            x = 0;

            down ^= true;
        }

        double x = down ? 1 - this.x : this.x;

        Location loc = bottom.clone().add(0, MathUtil.getParametrized(x, 2) * AMPLITUDE - AMPLITUDE, 0);

        for (int i = holograms.length - 1; i >= 0; i--) {
            holograms[i].getHologramLines().forEach(fakeArmorStand -> fakeArmorStand.teleport(loc));

            loc.add(0, 0.25D, 0);
        }

        this.x += STEP;
    }

}
