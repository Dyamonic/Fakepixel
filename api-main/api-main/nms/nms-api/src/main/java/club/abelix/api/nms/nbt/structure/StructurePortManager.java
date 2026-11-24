package club.abelix.api.nms.nbt.structure;

import club.abelix.api.nms.nbt.tag.CompoundTag;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.NonNull;

import java.util.function.Consumer;

/**
 * Переводит старую версию файла в новую
 * просто регистрируем старую версию методом putVersion([oldVersion], tag -> {});
 * в лямбде делаем все изменения, которые произошли между [oldVersion] и [oldVersion]+1
 * <p>
 * Например:
 * Версия структуры: 1
 * Версия файла: 0
 * Изменения:
 * 'ExampleByte': Byte  -> 'ExampleShort': Short
 * 'ExampleId': Short ->    'ExampleId': Int
 * 'ExampleName': Byte  -> 'ExampleName1': Byte
 * 'ExampleNum': Float ->   'ExampleNum': Float / 10F
 * <p>
 * putVersion(0, tag -> {
 * tag.changeTagName("ExampleName", "ExampleName1");
 * tag.changeTagType("ExampleId", ShortTag.class, shortTag -> new IntTag(shortTag.getValue());
 * tag.changeTagName("ExampleByte", "ExampleShort");
 * tag.changeTagType("ExampleShort", ByteTag.class, byteTag -> new ShortTag(byteTag.getValue());
 * <p>
 * FloatTag exampleNumTag = tag.getTag("ExampleNum", FloatTag.class);
 * exampleNumTag.setValue(exampleNumTag.getValue() / 10F);
 * });
 */
public class StructurePortManager {

    private TIntObjectMap<Consumer<CompoundTag>> map = new TIntObjectHashMap<>();

    public StructurePortManager putVersion(int version, @NonNull Consumer<CompoundTag> port) {
        map.put(version, port);

        return this;
    }

    public void port(int fileVersion, int structureVersion, @NonNull CompoundTag tag) {
        while (fileVersion < structureVersion) {
            Consumer<CompoundTag> nextVersionPort = map.get(fileVersion++);

            if (nextVersionPort == null) {
                continue;
            }

            nextVersionPort.accept(tag);
        }
    }

}
