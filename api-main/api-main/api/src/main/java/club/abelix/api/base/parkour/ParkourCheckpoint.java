package club.abelix.api.base.parkour;

import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.commons.language.Lang;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Getter
@RequiredArgsConstructor
public class ParkourCheckpoint {

    private final LobbyParkour parkour;

    private final int minY;

    private final Location location;
    private final int index;

    public void createHologram() {
        Location hologramLocation = location.clone().add(0, 0.5, 0);

        Hologram hologram = new Hologram(hologramLocation);
        hologram.setPublicStatus(true);

        if (index == 0) {
            hologram.addTextLine(Lang.of("parkour_start_line_1_hologram").compile());
            hologram.addTextLine(Lang.of("parkour_start_line_3_hologram").compile());
        } else if (index == parkour.getTotalCheckpoints() - 1) {
            hologram.addTextLine(Lang.of("parkour_finish_line_1_hologram").compile());
            hologram.addTextLine(Lang.of("parkour_finish_line_2_hologram").compile());
        } else {
            hologram.addTextLine(Lang.of("parkour_checkpoint_line_1_hologram")
                    .set("%index%", index).compile());
            hologram.addTextLine(Lang.of("parkour_checkpoint_line_2_hologram").compile());
        }
    }

    public Location getTeleportLocation() {
        return index == 0 ? parkour.getParkourLocation() : location.clone().add(0, 1, 0);
    }

    public void teleport(Player player) {
        player.teleport(getTeleportLocation());
    }

}
