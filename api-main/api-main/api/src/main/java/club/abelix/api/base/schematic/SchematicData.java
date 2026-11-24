package club.abelix.api.base.schematic;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.set.hash.TByteHashSet;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class SchematicData {

    final Schematic schematic;
    byte[] uniqueBlockData;
    SchematicBlock[] blocks;
    Multimap<Material, SchematicBlock> materialMap;

    public SchematicData(Schematic schematic) {
        this.schematic = schematic;

        TByteHashSet set = new TByteHashSet();
        for (byte data : schematic.getBlockData()) {
            set.add(data);
        }
        uniqueBlockData = set.toArray();

        blocks = new SchematicBlock[schematic.width * schematic.height * schematic.length];
        materialMap = ArrayListMultimap.create();

        for (int x = 0; x < schematic.width; x++) {
            for (int y = 0; y < schematic.height; y++) {
                for (int z = 0; z < schematic.length; z++) {
                    Vector vector = new Vector(x, y, z);
                    int index = y * schematic.width * schematic.length + z * schematic.width + x;

                    SchematicBlock block = new SchematicBlock(
                            schematic.getTiles().get(vector),

                            new MaterialData(
                                    Material.getMaterial(schematic.blocks[index] & 0xFF),
                                    schematic.blockData[index]
                            ), x, y, z, index
                    );

                    blocks[index] = block;
                    materialMap.put(block.getMaterial(), block);
                }
            }
        }
    }

    public List<SchematicBlock> getBlocksInclude(Collection<Material> include) {
        List<SchematicBlock> blocks = new ArrayList<>();

        for (Material material : include) {
            blocks.addAll(materialMap.get(material));
        }

        return blocks;
    }

    public List<SchematicBlock> getBlocksInclude(Material... include) {
        return getBlocksInclude(Arrays.asList(include));
    }

    public List<SchematicBlock> getBlocksExclude(Collection<Material> exclude) {
        List<Material> include = new ArrayList<>(materialMap.keySet());
        include.removeAll(exclude);

        return getBlocksInclude(include);
    }

    public List<SchematicBlock> getBlocksExclude(Material... exclude) {
        return getBlocksExclude(Arrays.asList(exclude));
    }

    public byte randomUniqueData() {
        return randomUniqueData(ThreadLocalRandom.current());
    }

    public byte randomUniqueData(Random random) {
        return uniqueBlockData[random.nextInt(uniqueBlockData.length)];
    }
}
