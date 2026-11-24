package club.abelix.api.base.camera.whirl;

import club.abelix.api.base.camera.AbstractPlayerCamera;
import club.abelix.commons.util.math.MathUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WhirlCamera extends AbstractPlayerCamera {

    private final WhirlCameraProperties props;
    private float angle;

    public WhirlCamera(Player player, Location initialPosition, WhirlCameraProperties props) {
        super(player, initialPosition);

        this.props = props;
    }

    @Override
    protected void runTick() {
        if (angle > 360F) {
            angle = 0F;
        }

        double x = MathUtil.cos(angle) * props.distance;
        double y = props.height == 0 ? 0 : MathUtil.sin(angle) * props.height;
        double z = MathUtil.sin(angle) * props.distance;

        Location newLocation = initialPosition.clone().add(x, y, z);

        Vector direction = newLocation.toVector().subtract(initialPosition.toVector());
        direction.multiply(-1);

        newLocation.setDirection(direction);

        move(newLocation);

        angle += props.speed;
    }
}
