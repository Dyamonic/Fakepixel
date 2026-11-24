package club.abelix.api.base.util.nms.nbt;

import club.abelix.api.nms.nbt.structure.BinaryStructure;
import club.abelix.api.nms.nbt.tag.NamedCompoundTag;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.nio.file.Path;

/**
 * @see NBT
 * @deprecated
 */
@Deprecated
@UtilityClass
public class NbtUtil {

    public NamedCompoundTag loadTag(@NonNull Path path) {
        return NBT.loadTag(path);
    }

    public void saveTag(@NonNull Path path, @NonNull NamedCompoundTag tag) {
        NBT.saveTag(path, tag);
    }

    public void loadStructure(@NonNull Path path, @NonNull BinaryStructure structure) {
        NBT.loadStructure(path, structure);
    }

    public void makeBackup(@NonNull Path path) {
        NBT.makeBackup(path);
    }

    public void saveStructure(@NonNull Path path, @NonNull BinaryStructure structure) {
        NBT.saveStructure(path, structure);
    }

}
