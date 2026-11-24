package club.abelix.api.base.song.utils;

import club.abelix.commons.util.math.MathUtil;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;

@UtilityClass
public class MathUtils {

    public Location stereoSourceLeft(Location location, float distance) {
        float yaw = location.getYaw();
        return location.clone().add(-MathUtil.cos(yaw + 360) * distance, 0, -MathUtil.sin(yaw + 360) * distance);
    }

    public Location stereoSourceRight(Location location, float distance) {
        float yaw = location.getYaw();
        return location.clone().add(MathUtil.cos(yaw + 360) * distance, 0, MathUtil.sin(yaw + 360) * distance);
    }

    /**
     * Calculate new location for stereo
     *
     * @param location origin location
     * @param distance negative for left side, positive for right side
     * @return
     */
    public Location stereoPan(Location location, float distance) {
        float yaw = location.getYaw();
        return location.clone().add(MathUtil.cos(yaw + 360) * distance, 0, MathUtil.sin(yaw + 360) * distance);
    }

}
