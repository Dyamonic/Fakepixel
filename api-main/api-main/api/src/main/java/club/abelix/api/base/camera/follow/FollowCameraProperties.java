package club.abelix.api.base.camera.follow;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import lombok.Getter;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Getter
public class FollowCameraProperties {

    TIntList speed;
    List<List<Location>> locations = new ArrayList<>();
    List<List<Location>> segments;

    public static FollowCameraProperties newProperties() {
        return new FollowCameraProperties();
    }

    public void process() {
        this.segments = new ArrayList<>();

        TIntIterator speed = this.speed.iterator();
        Iterator<List<Location>> locations = this.locations.iterator();

        while (speed.hasNext() && locations.hasNext()) {
            this.segments.add(FollowCamera.travel(locations.next(), speed.next()));
        }
    }

    public FollowCameraProperties speed(TIntList speed) {
        this.speed = speed;
        return this;
    }

    public FollowCameraProperties locations(Collection<Location> locations) {
        this.locations.add(new ArrayList<>(locations));
        return this;
    }

}
