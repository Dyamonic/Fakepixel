package club.abelix.api.base.roleplay;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Getter
public class SitSession {

    private final Gamer gamer;
    private final FakeEntityArmorStand stand;

    public SitSession(@NonNull Gamer gamer) {
        this.gamer = gamer;

        Player player = gamer.getPlayer();

        Location location = player.getLocation().subtract(0.0, 1.7, 0.0);

        this.stand = FalconAPI.getNmsApi().getFakeEntityApi().createArmorStand(location);
        this.stand.setGravity(false);
        this.stand.setInvisible(true);
        this.stand.setPublicStatus(true);
        this.stand.setPassenger(player.getEntityId());
    }

    public void remove() {
        Player player = gamer.getPlayer();

        player.eject();
        player.teleport(stand.getLocation().add(0, 1.7, 0));

        stand.remove();
    }

}
