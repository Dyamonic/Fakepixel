package club.abelix.api.nms.nbt.tag;

import lombok.Getter;

import java.util.function.Supplier;

@Getter
public class TagType<InTag extends Tag> {

    private static final TagType<?>[] TAGS = new TagType<?>[13];

    public static final TagType<EndTag> END = new TagType<>(0, EndTag::new);
    public static final TagType<ByteTag> BYTE = new TagType<>(1, ByteTag::new);
    public static final TagType<ShortTag> SHORT = new TagType<>(2, ShortTag::new);
    public static final TagType<IntTag> INT = new TagType<>(3, IntTag::new);
    public static final TagType<LongTag> LONG = new TagType<>(4, LongTag::new);
    public static final TagType<FloatTag> FLOAT = new TagType<>(5, FloatTag::new);
    public static final TagType<DoubleTag> DOUBLE = new TagType<>(6, DoubleTag::new);
    public static final TagType<ByteArrayTag> BYTE_ARRAY = new TagType<>(7, ByteArrayTag::new);
    public static final TagType<StringTag> STRING = new TagType<>(8, StringTag::new);
    public static final TagType<ListTag> LIST = new TagType<>(9, ListTag::new);
    public static final TagType<CompoundTag> COMPOUND = new TagType<>(10, CompoundTag::new);
    public static final TagType<IntArrayTag> INT_ARRAY = new TagType<>(11, IntArrayTag::new);
    public static final TagType<LongArrayTag> LONG_ARRAY = new TagType<>(12, LongArrayTag::new);

    public static TagType<?> getById(int id) {
        return id < 0 || id >= TAGS.length ? END : TAGS[id];
    }

    private final int typeId;
    private final Supplier<InTag> factory;

    private TagType(int typeId, Supplier<InTag> factory) {
        this.typeId = typeId;
        this.factory = factory;

        TAGS[typeId] = this;
    }

    public InTag newTag() {
        return factory.get();
    }

}
