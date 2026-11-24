package club.abelix.api.nms.nbt.tag;

import com.google.common.base.Objects;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Getter
@Setter
public class ListTag extends Tag {

    private List<Tag> tags;
    private TagType<?> listType;

    public ListTag() {
        super(TagType.LIST);

        this.tags = new ArrayList<>();
    }

    public <InTag extends Tag> InTag addTag(@NonNull InTag tag) {
        if (listType == null) {
            listType = tag.getType();
        }

        if (tag.getType() != listType) {
            throw new IllegalArgumentException(String.format("%s != %s", listType, tag.getType()));
        }

        tags.add(tag);

        return tag;
    }

    public int size() {
        return tags.size();
    }

    public Tag getTag(int i) {
        return tags.get(i);
    }

    public void removeTag(@NonNull Tag tag) {
        tags.remove(tag);
    }

    public boolean contains(@NonNull Tag tag) {
        return tags.contains(tag);
    }

    @Override
    public void push(@NonNull DataOutput output) throws IOException {
        if (tags.isEmpty()) {
            output.writeByte(0);
            output.writeInt(0);

            return;
        }

        output.writeByte(listType.getTypeId());
        output.writeInt(tags.size());

        for (Tag tag : tags) {
            tag.push(output);
        }
    }

    @Override
    public void pull(@NonNull DataInputStream input) throws IOException {
        boolean oldStyle = true;

        listType = TagType.getById(input.readByte());
        input.mark(5);

        int size = -1;

        if (listType == TagType.END) {
            size = input.readInt();

            if (size == 0) {
                oldStyle = false;
            } else {
                size = 0;

                input.reset();
            }
        }

        if (listType == TagType.END) {
            return;
        }

        if (oldStyle) {
            size = input.readInt();
        }

        tags = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            Tag tag = listType.newTag();
            tag.pull(input);

            tags.add(tag);
        }

    }

    public ByteTag newByte() {
        return addTag(new ByteTag());
    }

    public ByteArrayTag newByteArray() {
        return addTag(new ByteArrayTag());
    }

    public ShortTag newShort() {
        return addTag(new ShortTag());
    }

    public IntTag newInt() {
        return addTag(new IntTag());
    }

    public FloatTag newFloat() {
        return addTag(new FloatTag());
    }

    public DoubleTag newDouble() {
        return addTag(new DoubleTag());
    }

    public IntArrayTag newIntArray() {
        return addTag(new IntArrayTag());
    }

    public LongTag newLong() {
        return addTag(new LongTag());
    }

    public LongArrayTag newLongArray() {
        return addTag(new LongArrayTag());
    }

    public ListTag newList() {
        return addTag(new ListTag());
    }

    public StringTag newString() {
        return addTag(new StringTag());
    }

    public CompoundTag newCompound() {
        return addTag(new CompoundTag());
    }

    public void addByte(int value) {
        newByte().setValue(value);
    }

    public void addByteArray(@NonNull byte[] value) {
        newByteArray().setValue(value);
    }

    public void addShort(int value) {
        newShort().setValue(value);
    }

    public void addInt(int value) {
        newInt().setValue(value);
    }

    public void addFloat(float value) {
        newFloat().setValue(value);
    }

    public void addDouble(double value) {
        newDouble().setValue(value);
    }

    public void addIntArray(@NonNull int[] value) {
        newIntArray().setValue(value);
    }

    public void addLong(long value) {
        newLong().setValue(value);
    }

    public void addLongArray(@NonNull long[] value) {
        newLongArray().setValue(value);
    }

    public void addString(@NonNull String value) {
        newString().setValue(value);
    }

    public void addList(@NonNull Collection<Tag> value) {
        ListTag tag = newList();

        value.forEach(tag::addTag);
    }

    @Override
    public ListTag copy() {
        ListTag copy = new ListTag();

        tags.forEach(tag -> copy.addTag(tag.copy()));

        return copy;
    }

    public int hashCode() {
        return Objects.hashCode(type, tags);
    }

    public boolean equals(Object o) {
        if (!(o instanceof ListTag)) {
            return false;
        }

        ListTag tag = (ListTag) o;

        return tag.listType == listType && Objects.equal(tag.tags, tags);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");

        Tag[] tags = this.tags.toArray(new Tag[0]);

        for (int i = 0; i < tags.length; i++) {
            Tag tag = tags[i];

            if (tag.getType() == TagType.END) {
                continue;
            }

            builder.append(tag.toString());

            if (i != tags.length - 1) {
                builder.append(",");
            }
        }

        return builder.append("]").toString();
    }

    public <T extends Tag> Iterable<T> iterable() {
        Iterator<Tag> iterator = tags.iterator();

        return () -> new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                return (T) iterator.next();
            }
        };
    }

}
