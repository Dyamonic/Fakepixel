package club.abelix.api.base.camera.follow;

import club.abelix.api.base.camera.AbstractPlayerCamera;
import club.abelix.api.base.camera.CameraDestroyReason;
import club.abelix.api.base.camera.PlayerCameraManager;
import com.google.common.collect.Iterables;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FollowCamera extends AbstractPlayerCamera {

    private final FollowCameraProperties properties;

    private Iterator<List<Location>> generatedPaths;
    private Iterator<Location> generatedPath;

    public FollowCamera(Player player, Location initialPosition, FollowCameraProperties properties) {
        super(player, initialPosition);

        this.properties = properties;
    }

    private static double getDifference(Location cLoc, Location eLoc) {
        double cX = cLoc.getX();
        double cY = cLoc.getY();
        double cZ = cLoc.getZ();

        double eX = eLoc.getX();
        double eY = eLoc.getY();
        double eZ = eLoc.getZ();

        double dX = eX - cX;
        if (dX < 0.0D) {
            dX = -dX;
        }
        double dZ = eZ - cZ;
        if (dZ < 0.0D) {
            dZ = -dZ;
        }
        double dXZ = Math.hypot(dX, dZ);

        double dY = eY - cY;
        if (dY < 0.0D) {
            dY = -dY;
        }
        return Math.hypot(dXZ, dY);
    }

    public static List<Location> travel(List<Location> locations, int speed) {
        if (locations.size() == 1) {
            return Collections.nCopies(speed, Iterables.getOnlyElement(locations));
        }

        TDoubleList differences = new TDoubleArrayList();
        TIntList travelTimes = new TIntArrayList();

        double totalDiff = 0.0D;

        for (int i = 0; i < locations.size() - 1; i++) {
            Location s = locations.get(i);
            Location n = locations.get(i + 1);

            double diff = getDifference(s, n);
            totalDiff += diff;

            differences.add(diff);
        }

        for (double d : differences.toArray()) {
            travelTimes.add((int) (d / totalDiff * speed));
        }

        List<Location> travelLocations = new ArrayList<>();

        for (int i = 0; i < locations.size() - 1; i++) {
            Location s = locations.get(i);
            Location n = locations.get(i + 1);
            int t = travelTimes.get(i);

            double moveX = n.getX() - s.getX();
            double moveY = n.getY() - s.getY();
            double moveZ = n.getZ() - s.getZ();
            double movePitch = n.getPitch() - s.getPitch();

            double yawDiff = Math.abs(n.getYaw() - s.getYaw());
            double c;

            if (yawDiff <= 180.0D) {
                if (s.getYaw() < n.getYaw()) {
                    c = yawDiff;
                } else {
                    c = -yawDiff;
                }
            } else if (s.getYaw() < n.getYaw()) {
                c = -(360.0D - yawDiff);
            } else {
                c = 360.0D - yawDiff;
            }

            double d = c / t;

            for (int x = 0; x < t; x++) {
                Location l = new Location(
                        s.getWorld(), s.getX() + moveX / t * x,
                        s.getY() + moveY / t * x, s.getZ() + moveZ / t * x,
                        (float) (s.getYaw() + d * x), (float) (s.getPitch() + movePitch / t * x)
                );

                travelLocations.add(l);
            }
        }

        return travelLocations;
    }

    public void onFollow() {

    }

    public boolean follow() {
        if (generatedPaths.hasNext()) {
            generatedPath = generatedPaths.next().iterator();

            this.onFollow();

            return true;
        }

        PlayerCameraManager.IMP.removeCamera(player, CameraDestroyReason.CUSTOM);
        return false;
    }

    @Override
    public void onStart() {
        if (properties.segments == null) {
            properties.process();
        }

        this.generatedPaths = properties.segments.iterator();

        if (!follow()) {
            throw new IllegalStateException("Can't follow, please add locations first");
        }
    }

    @Override
    protected void runTick() {
        if (generatedPath.hasNext()) {
            move(generatedPath.next());
            return;
        }

        follow();
    }
}
