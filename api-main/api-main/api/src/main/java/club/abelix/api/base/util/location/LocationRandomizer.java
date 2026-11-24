package club.abelix.api.base.util.location;

import club.abelix.api.base.util.item.MaterialUtil;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by saharNooby on 14:58 12.11.2017
 */
public class LocationRandomizer {

    // Предоставляет валидные локации для спауна игроков (на твёрдых блоках) в определённом радиусе
    // от базовой локации

    private static final BlockFace[] FACES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private final Location base;
    private final int radius;

    private final List<Location> valid = new ArrayList<>();

    public LocationRandomizer(@NonNull Location base) {
        this(base, 5);
    }

    public LocationRandomizer(@NonNull Location base, int radius) {
        if (!isValidBlock(base.getBlock().getRelative(BlockFace.DOWN))) {
            throw new IllegalStateException("Spawn is not valid. Location must be on a solid block");
        }

        this.base = base.getBlock().getLocation().add(0.5, 1, 0.5);
        this.radius = radius;
    }

    private static boolean isValidBlock(@NonNull Block block) {
        if (!block.getType().isSolid()) {
            return false;
        }

        Block up = block.getRelative(BlockFace.UP);
        Block upper = up.getRelative(BlockFace.UP);

        return isFreelyPassable(up) && isFreelyPassable(upper);
    }

    private static boolean isFreelyPassable(@NonNull Block block) {
        Material type = block.getType();

        switch (type) {
            case REDSTONE_WIRE:
                return true;
        }

        return MaterialUtil.isFreelyPassable(type, block.getData());
    }

    public List<Location> getValid() {
        List<Location> valid = this.valid;
        if (!valid.isEmpty()) {
            return valid;
        }

        valid.add(this.base);

        Block base = this.base.getBlock().getRelative(BlockFace.DOWN);

        List<Block> queue = new ArrayList<>();
        queue.add(base);

        Set<Block> processed = new HashSet<>();

        while (!queue.isEmpty()) {
            Block block = queue.remove(0);

            for (BlockFace face : FACES) {
                for (int dy = -1; dy <= 1; dy++) {
                    Block rel = block.getRelative(face.getModX(), dy, face.getModZ());

                    if (Math.abs(base.getX() - rel.getX()) > this.radius) {
                        // try another face
                        break;
                    }

                    if (Math.abs(base.getZ() - rel.getZ()) > this.radius) {
                        // try another face
                        break;
                    }

                    if (Math.abs(base.getY() - rel.getY()) > this.radius / 2) {
                        // try another dy
                        continue;
                    }

                    if (!processed.add(rel) || !isValidBlock(rel)) {
                        // try another dy
                        continue;
                    }

                    valid.add(rel.getLocation().add(0.5, 1, 0.5));
                    queue.add(rel);
                }
            }
        }

        return valid;
    }

    // util

    public Location getAny() {
        return getAny(ThreadLocalRandom.current());
    }

    public Location getAny(@NonNull Random random) {
        return getValid().get(random.nextInt(getValid().size()));
    }

}
