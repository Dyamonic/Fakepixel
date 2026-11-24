package club.abelix.api.base.complex;

import club.abelix.api.base.util.location.LocationUtil;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Bed;

import java.util.Arrays;

public class ComplexBed implements ComplexBlock {

    private static final BlockFace[] FACES = new BlockFace[]{
            BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.SELF
    };

    private final Location[] bedLocations;

    public ComplexBed(@NonNull Location location) {
        this(location.getBlock());
    }

    public ComplexBed(@NonNull Block block) {
        bedLocations = new Location[2];

        for (BlockFace face : FACES) {
            Block relative = block.getRelative(face);

            if (relative.getType() == Material.BED_BLOCK) {
                Bed bed = (Bed) relative.getState().getData();
                bedLocations[bed.isHeadOfBed() ? 0 : 1] = relative.getLocation();
            }
        }

        if (bedLocations[0] == null || bedLocations[1] == null || LocationUtil.equalsLenient(bedLocations[0], bedLocations[1])) {
            throw new RuntimeException(String.format("Failed initialize ComplexBed. Current Locations: %s", Arrays.toString(bedLocations)));
        }
    }

    public Location getHeadPart() {
        return bedLocations[0];
    }

    public Location getBodyPart() {
        return bedLocations[1];
    }

    public boolean contains(@NonNull Location location) {
        for (Location part : bedLocations) {
            if (LocationUtil.equalsLenient(part, location)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Location[] getLocations() {
        return bedLocations;
    }

    @Override
    public void destroy() {
        for (Location bedLocation : bedLocations) {
            bedLocation.getBlock().setType(Material.AIR, true);
        }

        //todo: Добавить звук ломания кровати
    }
}
