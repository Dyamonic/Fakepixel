package club.abelix.api.nms.nbt.tag;

import com.google.common.base.Objects;
import lombok.Getter;
import lombok.NonNull;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

@Getter
public class CompoundTag extends Tag {

    public static final CompoundTag EMPTY_TAG = new CompoundTag() {
        @Override
        public <InTag extends Tag> InTag setTag(String name, InTag tag) {
            throw new UnsupportedOperationException("Unable to set tag in CompoundTag$EMPTY_TAG");
        }
    };

    private static final Pattern tagNamePattern = Pattern.compile("[A-Za-z0-9._+-]+");

    private final Map<String, Tag> tags;

    public CompoundTag() {
        super(TagType.COMPOUND);

        this.tags = new LinkedHashMap<>();
    }

    public boolean isEmpty() {
        return tags.isEmpty();
    }

    public void forEach(BiConsumer<String, Tag> tagConsumer) {
        tags.forEach(tagConsumer);
    }

    public void rename(@NonNull String name, @NonNull String newName) {
        if (name.equals(newName)) {
            return;
        }

        Tag tag = tags.remove(name);

        if (tag == null) {
            return;
        }

        tags.put(newName, tag);
    }

    public <T extends Tag, NT extends Tag> void editType(@NonNull String name, @NonNull Class<T> tagClass,
                                                         @NonNull Function<T, NT> port) {
        T tag = getTag(name, tagClass);

        if (tag == null) {
            return;
        }

        setTag(name, port.apply(tag));
    }

    public <InTag extends Tag> InTag setTag(@NonNull String name, @NonNull InTag tag) {
        if (tag.getType() == TagType.END) {
            throw new IllegalArgumentException("You can't add a 'END' tag");
        }

        tags.put(name, tag);

        return tag;
    }

    public ByteTag newByte(@NonNull String name) {
        return setTag(name, new ByteTag());
    }

    public ByteArrayTag newByteArray(@NonNull String name) {
        return setTag(name, new ByteArrayTag());
    }

    public ShortTag newShort(@NonNull String name) {
        return setTag(name, new ShortTag());
    }

    public IntTag newInt(@NonNull String name) {
        return setTag(name, new IntTag());
    }

    public FloatTag newFloat(@NonNull String name) {
        return setTag(name, new FloatTag());
    }

    public DoubleTag newDouble(@NonNull String name) {
        return setTag(name, new DoubleTag());
    }

    public IntArrayTag newIntArray(@NonNull String name) {
        return setTag(name, new IntArrayTag());
    }

    public LongTag newLong(@NonNull String name) {
        return setTag(name, new LongTag());
    }

    public LongArrayTag newLongArray(@NonNull String name) {
        return setTag(name, new LongArrayTag());
    }

    public ListTag newList(@NonNull String name) {
        return setTag(name, new ListTag());
    }

    public StringTag newString(@NonNull String name) {
        return setTag(name, new StringTag());
    }

    public CompoundTag newCompound(@NonNull String name) {
        return setTag(name, new CompoundTag());
    }

    public void setByte(@NonNull String name, int value) {
        newByte(name).setValue(value);
    }

    public void setByteArray(@NonNull String name, @NonNull byte[] value) {
        newByteArray(name).setValue(value);
    }

    public void setShort(@NonNull String name, int value) {
        newShort(name).setValue(value);
    }

    public void setInt(@NonNull String name, int value) {
        newInt(name).setValue(value);
    }

    public void setFloat(@NonNull String name, float value) {
        newFloat(name).setValue(value);
    }

    public void setDouble(@NonNull String name, double value) {
        newDouble(name).setValue(value);
    }

    public void setIntArray(@NonNull String name, @NonNull int[] value) {
        newIntArray(name).setValue(value);
    }

    public void setLong(@NonNull String name, long value) {
        newLong(name).setValue(value);
    }

    public void setLongArray(@NonNull String name, @NonNull long[] value) {
        newLongArray(name).setValue(value);
    }

    public void setString(@NonNull String name, @NonNull String value) {
        newString(name).setValue(value);
    }

    public void setList(@NonNull String name, @NonNull Collection<Tag> value) {
        ListTag tag = newList(name);

        value.forEach(tag::addTag);
    }

    public byte getByte(@NonNull String name) {
        return getByte(name, (byte) 0);
    }

    public int getUnsignedByte(@NonNull String name, int def) {
        return getByte(name, (byte) def) & 0xFF;
    }

    public int getUnsignedByte(@NonNull String name) {
        return getUnsignedByte(name, 0);
    }

    public short getShort(@NonNull String name) {
        return getShort(name, (short) 0);
    }

    public int getUnsignedShort(@NonNull String name, int def) {
        return getShort(name, (short) def) & 0xFFFF;
    }

    public int getUnsignedShort(@NonNull String name) {
        return getUnsignedShort(name, 0);
    }

    public int getInt(@NonNull String name) {
        return getInt(name, 0);
    }

    public long getLong(@NonNull String name) {
        return getLong(name, 0);
    }

    public long[] getLongArray(@NonNull String name) {
        return getLongArray(name, null);
    }

    public String getString(@NonNull String name) {
        return getString(name, null);
    }

    public byte[] getByteArray(@NonNull String name) {
        return getByteArray(name, null);
    }

    public float getFloat(@NonNull String name) {
        return getFloat(name, 0);
    }

    public double getDouble(@NonNull String name) {
        return getDouble(name, 0);
    }

    public int[] getIntArray(@NonNull String name) {
        return getIntArray(name, null);
    }

    public byte getByte(@NonNull String name, byte def) {
        ByteTag tag = getTag(name, ByteTag.class);

        return tag == null ? def : tag.getValue();
    }

    public byte[] getByteArray(@NonNull String name, byte[] def) {
        ByteArrayTag tag = getTag(name, ByteArrayTag.class);

        return tag == null ? def : tag.getValue();
    }

    public int getInt(@NonNull String name, int def) {
        IntTag tag = getTag(name, IntTag.class);

        return tag == null ? def : tag.getValue();
    }

    public short getShort(@NonNull String name, short def) {
        ShortTag tag = getTag(name, ShortTag.class);

        return tag == null ? def : tag.getValue();
    }

    public long getLong(@NonNull String name, long def) {
        LongTag tag = getTag(name, LongTag.class);

        return tag == null ? def : tag.getValue();
    }

    public double getDouble(@NonNull String name, double def) {
        DoubleTag tag = getTag(name, DoubleTag.class);

        return tag == null ? def : tag.getValue();
    }

    public float getFloat(@NonNull String name, float def) {
        FloatTag tag = getTag(name, FloatTag.class);

        return tag == null ? def : tag.getValue();
    }

    public long[] getLongArray(@NonNull String name, long[] def) {
        LongArrayTag tag = getTag(name, LongArrayTag.class);

        return tag == null ? def : tag.getValue();
    }

    public String getString(@NonNull String name, String def) {
        StringTag tag = getTag(name, StringTag.class);

        return tag == null ? def : tag.getValue();
    }

    public int[] getIntArray(@NonNull String name, int[] def) {
        IntArrayTag tag = getTag(name, IntArrayTag.class);

        return tag == null ? def : tag.getValue();
    }

    public CompoundTag getCompound(@NonNull String name, CompoundTag def) {
        CompoundTag tag = getTag(name, CompoundTag.class);
        ;

        return tag == null ? def : tag;
    }

    public CompoundTag getCompound(@NonNull String name) {
        return getCompound(name, null);
    }

    public <InTag extends Tag> Iterable<InTag> getList(@NonNull String name, Iterable<InTag> def) {
        ListTag tag = getTag(name, ListTag.class);

        return tag == null ? def : tag.iterable();
    }

    public <InTag extends Tag> Iterable<InTag> getList(@NonNull String name) {
        return getList(name, Collections.emptyList());
    }

    public boolean hasTag(@NonNull String name) {
        return tags.containsKey(name);
    }

    public void removeTag(@NonNull String name) {
        tags.remove(name);
    }

    public Tag getTag(@NonNull String name) {
        return tags.get(name);
    }

    public <T extends Tag> T getTag(@NonNull String name, @NonNull Class<T> tagClass) {
        Tag tag = getTag(name);

        try {
            return tag == null ? null : tagClass.cast(tag);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Cannot cast " + tag.getClass().getSimpleName() + " to " + tagClass.getSimpleName());
        }
    }

    @Override
    public void push(@NonNull DataOutput output) throws IOException {
        for (Map.Entry<String, Tag> entry : tags.entrySet()) {
            String name = entry.getKey();
            Tag tag = entry.getValue();

            output.writeByte(tag.getTypeId()); // tag id
            output.writeUTF(name); // tag name

            tag.push(output); // tag payload
        }

        output.writeByte(0); // type - end
    }

    @Override
    public void pull(@NonNull DataInputStream input) throws IOException {
        TagType<?> type;

        while ((type = TagType.getById(input.readByte())) != TagType.END) { // tag id
            String name = input.readUTF(); // tag name

            Tag tag = type.newTag(); // tag
            tag.pull(input); // tag payload

            this.tags.put(name, tag);
        }
    }

    @Override
    public CompoundTag copy() {
        CompoundTag copy = new CompoundTag();

        forEach((name, tag) -> copy.setTag(name, tag.copy()));

        return copy;
    }

    private String escape(@NonNull String name) {
        return tagNamePattern.matcher(name).matches() ? name : StringTag.escape(name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tags.entrySet());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CompoundTag)) {
            return false;
        }

        CompoundTag tag = (CompoundTag) o;

        return Objects.equal(tag.tags.entrySet(), tags.entrySet());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        tags.forEach((name, tag) -> builder.append(escape(name)).append(":").append(tag.toString()).append(","));

        return builder.length() == 0
                ? "{}"
                : "{" + builder.substring(0, builder.length() - 1) + "}";
    }

}
