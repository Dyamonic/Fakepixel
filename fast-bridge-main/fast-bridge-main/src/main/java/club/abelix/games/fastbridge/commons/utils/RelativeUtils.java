package club.abelix.games.fastbridge.commons.utils;

import club.abelix.games.fastbridge.commons.object.RelativeLocation;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

@UtilityClass
public class RelativeUtils {

    public Vector convertWorldPointToLocalPoint(final Location worldPoint, final Vector origin) {
        return worldPoint.toVector().subtract(origin);
    }

    private Location convertLocalPointToWorldPoint(
            final RelativeLocation localPoint,
            final Vector islandOriginLocation,
            final World world
    ) {
        return islandOriginLocation.clone().add(localPoint).toLocation(world);
    }


}
