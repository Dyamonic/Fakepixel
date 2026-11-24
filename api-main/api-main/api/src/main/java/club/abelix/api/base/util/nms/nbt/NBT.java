package club.abelix.api.base.util.nms.nbt;

import club.abelix.api.FalconAPI;
import club.abelix.api.nms.nbt.compression.CompressionType;
import club.abelix.api.nms.nbt.structure.BinaryStructure;
import club.abelix.api.nms.nbt.tag.CompoundTag;
import club.abelix.api.nms.nbt.tag.NamedCompoundTag;
import lombok.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author WhileInside
 */

// TODO Сделать несколько реализаций NBT и удалить NbtApi из nms
public final class NBT {

    private static final Path STRUCTURE_BACKUP_PATH;
    private static final AtomicInteger COUNTER = new AtomicInteger();

    static {
        STRUCTURE_BACKUP_PATH = Paths.get(System.getProperty("user.dir")).resolve("Backups");

        if (!Files.exists(STRUCTURE_BACKUP_PATH)) {
            try {
                Files.createDirectories(STRUCTURE_BACKUP_PATH);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private NBT() {
        throw new UnsupportedOperationException();
    }

    public static NamedCompoundTag loadTag(@NonNull Path path) {
        return FalconAPI.getNmsApi().getNbtApi().load(path, CompressionType.GZ);
    }

    public static NamedCompoundTag loadTag(@NonNull byte[] data) {
        return FalconAPI.getNmsApi().getNbtApi().load(new ByteArrayInputStream(data), CompressionType.GZ);
    }

    public static void saveTag(@NonNull Path path, @NonNull CompoundTag tag) {
        FalconAPI.getNmsApi().getNbtApi().save(path, tag, CompressionType.GZ);
    }

    public static byte[] saveTag(@NonNull CompoundTag tag) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        FalconAPI.getNmsApi().getNbtApi().save(baos, tag, CompressionType.GZ);

        return baos.toByteArray();
    }

    @Deprecated
    public static void loadStructure(@NonNull Path path, @NonNull BinaryStructure structure) {
        if (structure.doMakeBackup()) {
            makeBackup(path);
        }

        CompoundTag tag = loadTag(path);

        int version = tag.getInt("Version");

        if (version > structure.getVersion()) {
            throw new IllegalStateException(
                    "Невозможно загрузить структуру из файла, потому что ожидаемая версия ниже той, которая в файле;"
                            + " fV=" + version + ", sV=" + structure.getVersion()
            );
        }

        if (version < structure.getVersion()) {
            structure.port(version, tag);
        }

        structure.load(tag);
    }

    @Deprecated
    public static void makeBackup(@NonNull Path path) {
        if (Files.exists(path)) {
            String fileName = path.getFileName().toString();
            String newFileName = COUNTER.incrementAndGet() + "-" + (System.currentTimeMillis() / 1000) + "-" + fileName;

            try {
                Files.copy(path, STRUCTURE_BACKUP_PATH.resolve(newFileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Deprecated
    public static void saveStructure(@NonNull Path path, @NonNull BinaryStructure structure) {
        if (structure.doMakeBackup()) {
            makeBackup(path);
        }

        NamedCompoundTag tag = new NamedCompoundTag(structure.getStructureName());
        tag.setInt("Version", structure.getVersion());

        structure.save(tag);

        saveTag(path, tag);
    }


}
