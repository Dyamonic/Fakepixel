package club.abelix.api.base.schematic;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.nms.nbt.WorldEditNbtUtil;
import club.abelix.api.nms.nbt.tag.CompoundTag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;

@Getter
@AllArgsConstructor
public class SchematicBlock {

    private final CompoundTag tag;
    private final MaterialData materialData;
    private final int relX, relY, relZ;

    private final int index;

    public Material getMaterial() {
        return materialData.getItemType();
    }

    public byte getData() {
        return materialData.getData();
    }

    public void directUpdate(Location location, boolean pasteAir) {
        Material material = getMaterial();

        if (material == Material.AIR && !pasteAir) {
            return;
        }

        Block block = location.getBlock();
        block.setTypeIdAndData(material.getId(), getData(), false);

        if (tag != null) {
            FalconAPI.getNmsApi().getWorldApi().setBlockTag(block, tag);
        }
    }

    public void directUpdate(Extent extent, int x, int y, int z, boolean pasteAir) {
        Material material = getMaterial();

        if (material == Material.AIR && !pasteAir) {
            return;
        }

        try {
            BaseBlock block = new BaseBlock(material.getId(), getData());

            if (tag != null) {
                block.setNbtData(WorldEditNbtUtil.asWorldEditTag(tag));
            }

            extent.setBlock(new Vector(x, y, z), block);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
    }

    public void update(Extent extent, int x, int y, int z, boolean pasteAir) {
        directUpdate(extent, x + relX, y + relY, z + relZ, pasteAir);
    }

    public void update(Location origin, boolean pasteAir) {
        directUpdate(toAbsolute(origin), pasteAir);
    }

    public Location toAbsolute(Location origin) {
        return origin.clone().add(relX, relY, relZ);
    }

    public Location toRelative(World world) {
        return new Location(world, relX, relY, relZ);
    }
}
