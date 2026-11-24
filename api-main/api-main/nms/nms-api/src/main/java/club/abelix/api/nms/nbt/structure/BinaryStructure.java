package club.abelix.api.nms.nbt.structure;

import club.abelix.api.nms.nbt.tag.CompoundTag;
import lombok.NonNull;

public interface BinaryStructure {

    int getVersion();

    default String getStructureName() {
        return "struct";
    }

    @Deprecated
    default String getName() {
        return getStructureName();
    }

    void port(int version, @NonNull CompoundTag tag);

    void save(@NonNull CompoundTag tag);

    void load(@NonNull CompoundTag tag);

    default boolean doMakeBackup() {
        return true;
    }

}
