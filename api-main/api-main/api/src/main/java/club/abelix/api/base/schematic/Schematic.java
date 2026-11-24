package club.abelix.api.base.schematic;

import club.abelix.api.FalconAPI;
import club.abelix.api.nms.nbt.compression.CompressionType;
import club.abelix.api.nms.nbt.tag.CompoundTag;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class Schematic {

    final short width;
    final short length;
    final short height;
    @NonNull
    byte[] blocks;
    @NonNull
    byte[] blockData;
    @NonNull
    Map<Vector, CompoundTag> tiles;
    private SchematicData schematicData;

    public static Schematic loadSchematic(Path path) {
        CompoundTag tag = FalconAPI.getNmsApi().getNbtApi().load(path, CompressionType.GZ);

        if (!tag.hasTag("Blocks")) {
            throw new IllegalArgumentException("Schematic file is missing a \"Blocks\" tag");
        }

        Map<Vector, CompoundTag> tags = new HashMap<>();

        for (CompoundTag tile : tag.<CompoundTag>getList("TileEntities")) {
            int x = tile.getInt("x");
            int y = tile.getInt("y");
            int z = tile.getInt("z");

            tags.put(new Vector(x, y, z), tile);
        }

        return new Schematic(
                tag.getShort("Width"), tag.getShort("Length"), tag.getShort("Height"),
                tag.getByteArray("Blocks"), tag.getByteArray("Data"), tags
        );
    }

    public void pasteSchematic(@NonNull Location origin, boolean pasteAir) {
        for (SchematicBlock block : getSchematicData().blocks) {
            block.update(origin, pasteAir);
        }
    }

    /**
     * Чтобы асинхронно вставить схематику используйте TaskManager
     * <p>
     * EditSession session;
     * <p>
     * TaskManager.IMP.async(() -> {
     * schematic.fastPaste(session, 0, 0, 0, false);
     * <p>
     * session.flushQueue();
     * });
     * <p>
     * {@link #fastPaste(Location, boolean)}
     */
    public void fastPaste(@NonNull Extent extent, int x, int y, int z, boolean pasteAir) {
        for (SchematicBlock block : getSchematicData().blocks) {
            block.update(extent, x, y, z, pasteAir);
        }
    }

    public void fastPaste(@NonNull Extent extent, Vector vector, boolean pasteAir) {
        fastPaste(extent, vector.getBlockX(), vector.getBlockY(), vector.getBlockZ(), pasteAir);
    }

    public void fastPaste(@NonNull Location location, boolean pasteAir) {
        TaskManager.IMP.async(() -> {
            EditSession session = new EditSessionBuilder(location.getWorld().getName())
                    .changeSet(false, UUID.randomUUID(), -1)
                    .fastmode(true)
                    .autoQueue(false)
                    .limitUnlimited()
                    .build();

            fastPaste(session, location.getBlockX(),
                    location.getBlockY(), location.getBlockZ(), pasteAir);

            session.flushQueue();
        });
    }

    public SchematicData getSchematicData() {
        if (schematicData == null) {
            schematicData = new SchematicData(this);
        }

        return schematicData;
    }

}
