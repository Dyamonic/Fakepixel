package club.abelix.api.base.util.location;

import club.abelix.network.common.logging.Logging;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import w.util.RandomUtils;

import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class LocationUtil {

    public final Set<Material> NON_FLOOR_BLOCKS = Sets.newHashSet(
            Material.AIR,
            Material.CARPET,
            Material.GRASS,
            Material.LONG_GRASS,
            Material.YELLOW_FLOWER,
            Material.RED_ROSE,
            Material.DEAD_BUSH,
            Material.WATER,
            Material.STATIONARY_WATER,
            Material.LAVA,
            Material.STATIONARY_LAVA,
            Material.FLOWER_POT,
            Material.BARRIER,
            Material.WOOD_PLATE,
            Material.IRON_PLATE,
            Material.GOLD_PLATE,
            Material.STONE_PLATE,
            Material.WHEAT);

    public final BlockFace[] axis = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    public final BlockFace[] radial = {BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST};

    public Location stringToLocation(@NonNull String line) {
        try {
            String[] locationInfo = line.split(";");

            World world = WorldUtil.getOrCreateWorld(locationInfo[0]);

            Location location = new Location(world,
                    Double.parseDouble(locationInfo[1]), Double.parseDouble(locationInfo[2]), Double.parseDouble(locationInfo[3]));

            if (locationInfo.length > 4) {
                location.setYaw(Float.parseFloat(locationInfo[4]));
                location.setPitch(Float.parseFloat(locationInfo[5]));
            }

            return location;
        } catch (Exception exception) {
            Logging.IMP.error("Failed parse location: {}", line);
            exception.printStackTrace();
            throw exception;
        }

    }

    public String locationToString(Location location, boolean pitch) {
        return locationToString(location, location.getWorld().getName(), pitch);
    }

    public String locationToString(Location location, String worldName, boolean pitch) {
        String locationString
                = String.format("%s;%s;%s;%s", worldName, location.getX(), location.getY(), location.getZ());

        return locationString + (pitch ? String.format(";%s;%s", location.getYaw(), location.getPitch()) : "");
    }

    public Collection<String> locationToString(Collection<Location> locations, boolean pitch) {
        return locations.stream().map(loc -> locationToString(loc, pitch))
                .collect(Collectors.toList());
    }

    public boolean isNearby(Location first, Location second, double dist) {
        return first.getWorld() == second.getWorld() && first.distance(second) <= dist;
    }

    public Collection<Location> stringToLocation(Collection<String> locations) {
        return locations.stream().map(LocationUtil::stringToLocation)
                .collect(Collectors.toList());
    }

    public RawLocation stringToRawLocation(@NonNull String line) {
        try {
            String[] locationInfo = line.split(";");

            String world = locationInfo[0];

            float yaw = 0;
            float pitch = 0;

            if (locationInfo.length > 4) {
                yaw = Float.parseFloat(locationInfo[4]);
                pitch = Float.parseFloat(locationInfo[5]);
            }

            return new RawLocation(world,
                    Double.parseDouble(locationInfo[1]),
                    Double.parseDouble(locationInfo[2]),
                    Double.parseDouble(locationInfo[3]),
                    yaw, pitch);
        } catch (Exception exception) {
            Logging.IMP.error("Failed parse location: {}", line);
            exception.printStackTrace();
            throw exception;
        }
    }

    public void teleportPlayers(Location location) {
        Bukkit.getOnlinePlayers().forEach(player -> player.teleport(location));
    }

    public void teleportPlayers(Iterator<Location> locationIterator) {
        List<Location> cache = new ArrayList<>();

        while (locationIterator.hasNext()) {
            cache.add(locationIterator.next());
        }

        Iterator<Location> iteratorCopy = cache.iterator();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!iteratorCopy.hasNext()) {
                iteratorCopy = cache.iterator();
            }

            player.teleport(iteratorCopy.next());
        }
    }

    public Location toBlockLocation(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public Location getNearestY(Location location) {
        Location start = location.clone();

        while (!start.getBlock().isEmpty()) {
            start.setY(start.getY() - 1);
        }

        return start;
    }

    public Location[] getCircleAround(Location loc, double radius, int numPoints) {
        Location[] retVal = new Location[numPoints];
        double piSlice = 2.0d * Math.PI / numPoints;

        for (int i = 0; i < numPoints; i++) {
            double angle = piSlice * i;

            retVal[i] = new Location(
                    loc.getWorld(),
                    loc.getX() + radius * Math.cos(angle),
                    loc.getY(),
                    loc.getZ() + radius * Math.sin(angle)
            );
        }

        return retVal;
    }

    public Location getBlockCenter(Block block) {
        return block.getLocation().add(0.5, 0, 0.5);
    }

    public boolean equalsLenient(Location first, Location second) {
        return first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ()
                && first.getBlock().getWorld().getName().equals(second.getBlock().getWorld().getName());
    }

    public Location getBlockCenter(Block first, Block second) {
        Preconditions.checkArgument(first.getWorld() == second.getWorld(),
                "Block worlds can not be different");

        double x = (first.getX() + second.getX()) / 2f;
        double y = (first.getY() + second.getY()) / 2f;
        double z = (first.getZ() + second.getZ()) / 2f;

        return new Location(first.getWorld(), x + 0.5, y, z + 0.5);
    }

    public YawPitch calculateYawPitch(Location src, Location target) {
        double dX = src.getX() - target.getX();
        double dY = src.getY() - target.getY();
        double dZ = src.getZ() - target.getZ();

        double yaw = Math.atan2(dZ, dX);

        double pitch = Math.atan2(Math.sqrt(dZ * dZ + dX * dX), dY) + Math.PI;

        return new YawPitch(yaw, pitch);
    }

    public void rotateBlock(double yaw, double pitch, Block block) {
        BlockFace blockFace = getFacingDirection(yaw, true);

        MaterialData materialData = block.getState().getData();

        if (block.getState().getData() instanceof Directional) {
            //некоторые ваще какого-то члена смотрят в ту же сторону, но противоположную
            if (materialData.getItemType() == Material.PUMPKIN
                    || materialData.getItemType() == Material.JACK_O_LANTERN
                    || materialData.getItemType().name().contains("STAIRS")) {

                blockFace = blockFace.getOppositeFace();
            }

            if (pitch <= -70) {
                blockFace = BlockFace.UP;
            } else if (pitch >= 70) {
                blockFace = BlockFace.DOWN;
            }

            ((Directional) materialData).setFacingDirection(blockFace);
        } else if (block.getType() == Material.ANVIL) {
            //а наковальня вообще уебищно выставляет себе направление взгляда, нет бы тоже Directional сделать
            materialData.setData(blockFace == BlockFace.EAST || blockFace == BlockFace.WEST ? (byte) 1 : (byte) 0);
        }

        block.setTypeIdAndData(materialData.getItemTypeId(), materialData.getData(), true);
    }

    public BlockFace getFacingDirection(double yaw, boolean cardinal) {
        yaw += 180.0d;

        while (yaw < 0.0d) {
            yaw += 360.0d;
        }
        while (yaw > 360.0d) {
            yaw -= 360.0d;
        }

        if (cardinal) {
            if (yaw >= 315.0d || yaw < 45.0d) {
                return BlockFace.NORTH;
            } else if (yaw >= 45.0d && yaw < 135.0d) {
                return BlockFace.EAST;
            } else if (yaw >= 135.0d && yaw < 225.0d) {
                return BlockFace.SOUTH;
            }

            return BlockFace.WEST;
        }

        if (yaw >= 348.75d || yaw < 11.25d) {
            return BlockFace.NORTH;
        } else if (yaw >= 11.25d && yaw < 33.75d) {
            return BlockFace.NORTH_NORTH_EAST;
        } else if (yaw >= 33.75d && yaw < 56.25d) {
            return BlockFace.NORTH_EAST;
        } else if (yaw >= 56.25d && yaw < 78.75d) {
            return BlockFace.EAST_NORTH_EAST;
        } else if (yaw >= 78.75d && yaw < 101.25d) {
            return BlockFace.EAST;
        } else if (yaw >= 101.25d && yaw < 123.75d) {
            return BlockFace.EAST_SOUTH_EAST;
        } else if (yaw >= 123.75d && yaw < 146.25d) {
            return BlockFace.SOUTH_EAST;
        } else if (yaw >= 146.25d && yaw < 168.75d) {
            return BlockFace.SOUTH_SOUTH_EAST;
        } else if (yaw >= 168.75d && yaw < 191.25d) {
            return BlockFace.SOUTH;
        } else if (yaw >= 191.25d && yaw < 213.75d) {
            return BlockFace.SOUTH_SOUTH_WEST;
        } else if (yaw >= 213.75d && yaw < 236.25d) {
            return BlockFace.SOUTH_WEST;
        } else if (yaw >= 236.25d && yaw < 258.75d) {
            return BlockFace.WEST_SOUTH_WEST;
        } else if (yaw >= 258.75d && yaw < 281.25d) {
            return BlockFace.WEST;
        } else if (yaw >= 281.25d && yaw < 303.75d) {
            return BlockFace.WEST_NORTH_WEST;
        } else if (yaw >= 303.75d && yaw < 326.25d) {
            return BlockFace.NORTH_WEST;
        }

        return BlockFace.NORTH_NORTH_WEST;
    }

    public static float getYaw(int data) {
        switch (data) {
            case 0:
                return 360.0F;
            case 1:
                return 382.5F;
            case 2:
                return 405.0F;
            case 3:
                return 427.5F;
            case 4:
                return 450.0F;
            case 5:
                return 472.5F;
            case 6:
                return 495.0F;
            case 7:
                return 517.5F;
            case 8:
                return 180F;
            case 9:
                return 202.5F;
            case 10:
                return 225.0F;
            case 11:
                return 247.5F;
            case 12:
                return 270.0F;
            case 13:
                return 292.5F;
            case 14:
                return 315.0F;
            case 15:
                return 337.5F;
            default:
                throw new IllegalStateException("Unknown data: " + data);
        }
    }

    public double getYaw(BlockFace facing, boolean cardinal) {
        if (cardinal) {
            if (facing == BlockFace.NORTH) {
                return 0.0d + 180.0d;
            } else if (facing == BlockFace.EAST) {
                return 90.0d + 180.0d;
            } else if (facing == BlockFace.SOUTH) {
                return 180.0d + 180.0d;
            }

            return 270.0d + 180.0d;
        }

        if (facing == BlockFace.NORTH) {
            return 0.0d + 180.0d;
        } else if (facing == BlockFace.NORTH_NORTH_EAST) {
            return 22.5d + 180.0d;
        } else if (facing == BlockFace.NORTH_EAST) {
            return 45.0d + 180.0d;
        } else if (facing == BlockFace.EAST_NORTH_EAST) {
            return 67.5d + 180.0d;
        } else if (facing == BlockFace.EAST) {
            return 90.0d + 180.0d;
        } else if (facing == BlockFace.EAST_SOUTH_EAST) {
            return 112.5d + 180.0d;
        } else if (facing == BlockFace.SOUTH_EAST) {
            return 135.0d + 180.0d;
        } else if (facing == BlockFace.SOUTH_SOUTH_EAST) {
            return 157.5d + 180.0d;
        } else if (facing == BlockFace.SOUTH) {
            return 180.0d + 180.0d;
        } else if (facing == BlockFace.SOUTH_SOUTH_WEST) {
            return 202.5d + 180.0d;
        } else if (facing == BlockFace.SOUTH_WEST) {
            return 225.0d + 180.0d;
        } else if (facing == BlockFace.WEST_SOUTH_WEST) {
            return 247.5d + 180.0d;
        } else if (facing == BlockFace.WEST) {
            return 270.0d + 180.0d;
        } else if (facing == BlockFace.WEST_NORTH_WEST) {
            return 292.5d + 180.0d;
        } else if (facing == BlockFace.NORTH_WEST) {
            return 315.0d + 180.0d;
        }

        return 337.5d + 180.0d;
    }

    public BlockFace locationToFace(Location location, boolean useSubCardinalDirections) {
        return yawToFace(location.getYaw(), useSubCardinalDirections);
    }

    public BlockFace yawToFace(float yaw, boolean useSubCardinalDirections) {
        if (useSubCardinalDirections) {
            return radial[Math.round(yaw / 45f) & 0x7];
        } else {
            return axis[Math.round(yaw / 90f) & 0x3];
        }
    }

    public Location getCenter(List<Location> locations) {
        World world;
        if (locations.isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        } else {
            world = locations.stream().findFirst().get().getWorld();
        }

        double x = 0;
        double y = 0;
        double z = 0;
        float yaw = 0.0f;
        float pitch = 0.0f;

        for (Location location : locations) {
            x += location.getX();
            y += location.getY();
            z += location.getZ();
            yaw = location.getYaw();
            pitch = location.getPitch();
        }

        int size = locations.size();

        return new Location(world, x / size, y / size, z / size, yaw, pitch);
    }

    public Location getMiddleLocation(Location location) {
        return new Location(
                location.getWorld(),
                location.getBlockX() + 0.5,
                location.getBlockY(),
                location.getBlockZ() + 0.5,
                location.getYaw(),
                location.getPitch()
        );
    }

    public List<Location> getLoc(FileConfiguration config, String patch) {
        return config.getStringList(patch)
                .stream()
                .map(LocationUtil::stringToLocation)
                .filter(loc -> loc.getWorld() != null)
                .collect(Collectors.toList());
    }

    public byte getFixRotation(float yawPitch) {
        return (byte) (yawPitch * 256.0F / 360.0F);
    }

    public Location faceEntity(Location location, Entity entity) {
        Vector direction = location.toVector().subtract(entity.getLocation().toVector());
        direction.multiply(-1);
        location.setDirection(direction);
        return location;
    }

    public void setYawPitchEntity(Entity entity, Location center) {
        Location entityLoc = entity.getLocation();
        Vector direction = entityLoc.toVector().subtract(center.toVector());
        direction.multiply(-1);
        entityLoc.setDirection(direction);
        entity.teleport(entityLoc);
    }

    public Location getDirection(Location location, double amount) {
        int degrees = (Math.round(location.getYaw()) + 270) % 360;
        if (degrees <= 22) {
            return location.subtract(amount, 0.0, 0.0);
        }
        if (degrees <= 112) {
            return location.subtract(0.0, 0.0, amount);
        }
        if (degrees <= 202) {
            return location.subtract(-amount, 0.0, 0.0);
        }
        if (degrees <= 292) {
            return location.subtract(0.0, 0.0, -amount);
        }
        return location.subtract(amount, 0.0, 0.0);
    }

    public boolean compareLocations(Location loc1, Location loc2) {
        return loc1.getWorld().getName().equals(loc2.getWorld().getName())
                && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }

    public Location getRandomLocation(Location old) {
        Location location = old.clone();
        location.setZ(old.getZ() + ((Math.random() * 3) - 1.5));
        location.setX(old.getX() + ((Math.random() * 3) - 1.5));
        return location;
    }

    public Location getRandomLocation(Location start, int radius) {
        return start.clone().add(
                (RandomUtils.getBoolean() ? 1 : -1) * RandomUtils.getInt(radius),
                (RandomUtils.getBoolean() ? 1 : -1) * RandomUtils.getInt(radius),
                (RandomUtils.getBoolean() ? 1 : -1) * RandomUtils.getInt(radius)
        );
    }

    public Location getHighestLocationAt(Location location) {
        location = LocationUtil.toBlockLocation(location);
        location.setY(location.getWorld().getHighestBlockYAt(location));

        Material type = location.getBlock().getType();

        while (NON_FLOOR_BLOCKS.contains(type) && location.getY() > 0) {
            location.subtract(0, 1.0, 0);
            type = location.getBlock().getType();
        }

        return location;
    }
}
