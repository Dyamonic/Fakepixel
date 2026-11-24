package club.abelix.api.nms.nbt;

import club.abelix.api.nms.nbt.compression.CompressionType;
import club.abelix.api.nms.nbt.tag.CompoundTag;
import club.abelix.api.nms.nbt.tag.NamedCompoundTag;
import club.abelix.api.nms.nbt.tag.Tag;
import club.abelix.api.nms.nbt.tag.TagType;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public interface NbtApi {

    /**
     * Перевести тэг из апи в тэг из NMS
     */
    Object toNMS(Tag tag);

    /**
     * Перевести тэг из NMS в тэг из апи
     */
    Tag fromNMS(Object object);

    /**
     * Получить полный NMS тэг предмета, в этом тэге будет ID, Count и т.д
     */
    Object getItemFullNmsTag(@NonNull ItemStack item);

    /**
     * Получить кастомные NMS тэги предмета, в этом тэге не будет ID, Count и т.д
     */
    Object getItemNmsTag(@NonNull ItemStack item);

    /**
     * Получить кастомные тэги предмета, в этом тэге не будет ID, Count и т.д
     */
    CompoundTag getItemTag(@NonNull ItemStack item);

    /**
     * Получить полный тэг предмета, в этом тэге будет ID, Count и т.д
     */
    CompoundTag getItemFullTag(@NonNull ItemStack item);

    /**
     * Установить новый NMS тэг для предмета
     */
    ItemStack setItemNmsTag(@NonNull ItemStack item, @NonNull Object tag);

    /**
     * Установить новый тэг для предмета
     */
    ItemStack setItemTag(@NonNull ItemStack item, @NonNull CompoundTag tag);

    ItemStack loadItem(@NonNull CompoundTag tag);

    ItemStack loadItem(@NonNull Object tag);

    ItemStack parseBanner(@NonNull String nbt);

    /**
     * Загрузить тэг из файла
     */
    default NamedCompoundTag load(@NonNull Path path, @NonNull CompressionType compressionType) {
        try (InputStream is = Files.newInputStream(path)) {
            return load(is, compressionType);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot open file", e);
        }
    }

    /**
     * Загрузить тэг из стрима
     */
    default NamedCompoundTag load(@NonNull InputStream is, @NonNull CompressionType compressionType) {
        try (DataInputStream input = new DataInputStream(compressionType.create(is))) {
            TagType<?> type = TagType.getById(input.readByte()); // type

            if (type != TagType.COMPOUND) {
                throw new IOException(String.format("%s != %s", type, TagType.COMPOUND));
            }

            String tagName = input.readUTF();

            NamedCompoundTag tag = new NamedCompoundTag(tagName);
            tag.pull(input); // payload

            return tag;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read file", e);
        }
    }

    /**
     * Сохранить тэг в стрим
     */
    default void save(@NonNull OutputStream os, @NonNull CompoundTag compound,
                      @NonNull CompressionType compressionType) {

        try (DataOutputStream output = new DataOutputStream(compressionType.create(os))) {
            output.writeByte(10); // type - compound

            output.writeUTF(compound instanceof NamedCompoundTag
                    ? ((NamedCompoundTag) compound).getName() : ""); // name

            compound.push(output); // payload
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Сохранить тэг в файл
     */
    default void save(@NonNull Path path, @NonNull CompoundTag compound,
                      @NonNull CompressionType compressionType) {

        try (OutputStream os = Files.newOutputStream(path)) {
            save(os, compound, compressionType);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot open file", e);
        }
    }

}
