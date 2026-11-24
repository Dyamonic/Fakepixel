package club.abelix.api.base.util.nms.nbt;

import club.abelix.api.nms.nbt.tag.*;
import com.sk89q.jnbt.EndTag;
import lombok.experimental.UtilityClass;

import java.util.*;

@UtilityClass
public class WorldEditNbtUtil {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <E extends com.sk89q.jnbt.Tag> E asWorldEditTag(Tag tag) {
        switch (tag.getTypeId()) {
            case 0:
                return (E) new EndTag();
            case 1:
                return (E) new com.sk89q.jnbt.ByteTag(((ByteTag) tag).getValue());
            case 2:
                return (E) new com.sk89q.jnbt.ShortTag(((ShortTag) tag).getValue());
            case 3:
                return (E) new com.sk89q.jnbt.IntTag(((IntTag) tag).getValue());
            case 4:
                return (E) new com.sk89q.jnbt.LongTag(((LongTag) tag).getValue());
            case 5:
                return (E) new com.sk89q.jnbt.FloatTag(((FloatTag) tag).getValue());
            case 6:
                return (E) new com.sk89q.jnbt.DoubleTag(((DoubleTag) tag).getValue());
            case 7:
                return (E) new com.sk89q.jnbt.ByteArrayTag(((ByteArrayTag) tag).getValue());
            case 8:
                return (E) new com.sk89q.jnbt.StringTag(((StringTag) tag).getValue());
            case 9: {
                ListTag listTag = (ListTag) tag;

                if (listTag.getTags().isEmpty()) {
                    return (E) new com.sk89q.jnbt.ListTag(null, Collections.emptyList());
                }

                List worldEditTags = new ArrayList<>();

                for (Tag element : listTag.getTags()) {
                    worldEditTags.add(asWorldEditTag(element));
                }

                return (E) new com.sk89q.jnbt.ListTag(getWorldEditClass(listTag.getListType()), worldEditTags);
            }
            case 10: {
                CompoundTag compoundTag = (CompoundTag) tag;
                Map<String, com.sk89q.jnbt.Tag> worldEditTags = new HashMap<>();

                compoundTag.forEach((name, element) -> worldEditTags.put(name, asWorldEditTag(element)));

                return (E) new com.sk89q.jnbt.CompoundTag(worldEditTags);
            }
            case 11:
                return (E) new com.sk89q.jnbt.IntArrayTag(((IntArrayTag) tag).getValue());
            default:
                throw new UnsupportedOperationException("Type unsupported: " + tag.getTypeId());
        }
    }

    public Class<? extends com.sk89q.jnbt.Tag> getWorldEditClass(TagType<?> tagType) {
        switch (tagType.getTypeId()) {
            case 0:
                return EndTag.class;
            case 1:
                return com.sk89q.jnbt.ByteTag.class;
            case 2:
                return com.sk89q.jnbt.ShortTag.class;
            case 3:
                return com.sk89q.jnbt.IntTag.class;
            case 4:
                return com.sk89q.jnbt.LongTag.class;
            case 5:
                return com.sk89q.jnbt.FloatTag.class;
            case 6:
                return com.sk89q.jnbt.DoubleTag.class;
            case 7:
                return com.sk89q.jnbt.ByteArrayTag.class;
            case 8:
                return com.sk89q.jnbt.StringTag.class;
            case 9:
                return com.sk89q.jnbt.ListTag.class;
            case 10:
                return com.sk89q.jnbt.CompoundTag.class;
            case 11:
                return com.sk89q.jnbt.IntArrayTag.class;
            default:
                throw new UnsupportedOperationException("Type unsupported: " + tagType.getTypeId());
        }
    }
}