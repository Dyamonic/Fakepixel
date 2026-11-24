package club.abelix.api.base.antiafk;

import org.bukkit.Location;

public class MovementData {

    private Location lastLocation;
    private long lastMovementTime;

    public MovementData(Location lastLocation, long lastMovementTime) {
        this.lastLocation = lastLocation;
        this.lastMovementTime = lastMovementTime;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    public long getLastMovementTime() {
        return lastMovementTime;
    }

    public void setLastMovementTime(long lastMovementTime) {
        this.lastMovementTime = lastMovementTime;
    }

    public boolean isStay(long time) {
        return getStayTime() >= time;
    }

    public long getStayTime() {
        return System.currentTimeMillis() - lastMovementTime;
    }
}
