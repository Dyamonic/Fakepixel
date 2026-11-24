package club.abelix.games.fastbridge.commons.object;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * @author Migel077
 */
@Log4j2
@Getter
@Setter
public class RelativeLocation extends Vector {

    private float pitch;
    private float yaw;

    public RelativeLocation(double x, double y, double z) {
        this(x, y, z, 0.0F, 0.0F);
    }

    public RelativeLocation(Vector source) {
        this(source.getX(), source.getY(), source.getZ(), 0.0F, 0.0F);
    }

    public RelativeLocation(Location source) {
        this(source.getX(), source.getY(), source.getZ(), source.getYaw(), source.getPitch());
    }

    public RelativeLocation(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public static RelativeLocation fromString(@NonNull String line) {
        try {
            String[] locationInfo = line.split(";");
            RelativeLocation location = new RelativeLocation(
                    Double.parseDouble(locationInfo[0]),
                    Double.parseDouble(locationInfo[1]),
                    Double.parseDouble(locationInfo[2]));

            if (locationInfo.length > 3) {
                location.setYaw(Float.parseFloat(locationInfo[3]));
                location.setPitch(Float.parseFloat(locationInfo[4]));
            }

            return location;
        } catch (Exception exception) {
            log.error("Failed parse location: {}", line);
            throw new RuntimeException(exception);
        }
    }

    @Override
    public String toString() {
        return String.format(";%s;%s;%s;%s;%s", getX(), getY(), getZ(), getYaw(), getPitch());
    }

    @Override
    public RelativeLocation clone() {
        return new RelativeLocation(getX(), getY(), getZ(), getYaw(), getPitch());
    }

    @Override
    public Location toLocation(World world) {
        return new Location(world, getX(), getY(), getZ(), getYaw(), getPitch());
    }

    public RelativeLocation add(Location location) {
        this.x += location.getX();
        this.y += location.getY();
        this.z += location.getZ();
        return this;
    }

    public RelativeLocation subtract(Location location) {
        this.x -= location.getX();
        this.y -= location.getY();
        this.z -= location.getZ();
        return this;
    }

    public RelativeLocation asImmutable() {
        return new ImmutableRelativeLocation(this);
    }

    public static class ImmutableRelativeLocation extends RelativeLocation {

        public ImmutableRelativeLocation(double x, double y, double z) {
            super(x, y, z);
        }

        public ImmutableRelativeLocation(Vector vector) {
            super(vector);
        }

        public ImmutableRelativeLocation(double x, double y, double z, float yaw, float pitch) {
            super(x, y, z, yaw, pitch);
        }

        public ImmutableRelativeLocation(RelativeLocation relativeLocation) {
            super(relativeLocation.x,
                    relativeLocation.y,
                    relativeLocation.z,
                    relativeLocation.yaw,
                    relativeLocation.pitch);
        }

        @Override
        public RelativeLocation add(Vector vec) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector subtract(Vector vec) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector multiply(Vector vec) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector divide(Vector vec) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RelativeLocation add(Location location) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RelativeLocation subtract(Location location) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setYaw(float yaw) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPitch(float pitch) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector multiply(int m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector multiply(double m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector multiply(float m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector crossProduct(Vector o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector setX(int x) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector setX(double x) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector setX(float x) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector setY(int y) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector setY(double y) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector setY(float y) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector setZ(int z) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector setZ(double z) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Vector setZ(float z) {
            throw new UnsupportedOperationException();
        }
    }
}
