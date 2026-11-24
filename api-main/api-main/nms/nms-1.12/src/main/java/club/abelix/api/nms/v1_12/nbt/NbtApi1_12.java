package club.abelix.api.nms.v1_12.nbt;

import club.abelix.api.nms.nbt.NbtApi;
import club.abelix.api.nms.nbt.tag.*;
import lombok.NonNull;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;

public class NbtApi1_12 implements NbtApi {

    private Field LONG_ARRAY_B;

    public NbtApi1_12() {
        try {
            LONG_ARRAY_B = NBTTagLongArray.class.getDeclaredField("b");
            LONG_ARRAY_B.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    public NBTBase toNMS(Tag tag) {
        switch (tag.getTypeId()) {
            case 1:
                return new NBTTagByte(((ByteTag) tag).getValue());
            case 2:
                return new NBTTagShort(((ShortTag) tag).getValue());
            case 3:
                return new NBTTagInt(((IntTag) tag).getValue());
            case 4:
                return new NBTTagLong(((LongTag) tag).getValue());
            case 5:
                return new NBTTagFloat(((FloatTag) tag).getValue());
            case 6:
                return new NBTTagDouble(((DoubleTag) tag).getValue());
            case 7:
                return new NBTTagByteArray(((ByteArrayTag) tag).getValue());
            case 8:
                return new NBTTagString(((StringTag) tag).getValue());
            case 9:
                NBTTagList tagList = new NBTTagList();

                for (Tag element : ((ListTag) tag).iterable()) {
                    tagList.add(toNMS(element));
                }

                return tagList;
            case 10:
                NBTTagCompound compound = new NBTTagCompound();

                ((CompoundTag) tag).forEach((key, value) -> compound.set(key, toNMS(value)));

                return compound;
            case 11:
                return new NBTTagIntArray(((IntArrayTag) tag).getValue());
            case 12:
                return new NBTTagLongArray(((LongArrayTag) tag).getValue());
            case 0:
            default:
                throw new IllegalStateException(
                        String.format(
                                "Cannot get nms tag: %s",
                                tag
                        )
                );
        }
    }

    @Override
    public Tag fromNMS(Object tag) {
        if (!(tag instanceof NBTBase)) {
            throw new IllegalArgumentException("Unsupported nbt tag: " + tag);
        }

        NBTBase base = (NBTBase) tag;

        switch (base.getTypeId()) {
            case 1: {
                NBTTagByte byteTag = (NBTTagByte) base;

                return new ByteTag(byteTag.g());
            }
            case 2: {
                NBTTagShort shortTag = (NBTTagShort) base;

                return new ShortTag(shortTag.f());
            }
            case 3: {
                NBTTagInt intTag = (NBTTagInt) base;

                return new IntTag(intTag.e());
            }
            case 4: {
                NBTTagLong longTag = (NBTTagLong) base;

                return new LongTag(longTag.d());
            }
            case 5: {
                NBTTagFloat floatTag = (NBTTagFloat) base;

                return new FloatTag(floatTag.g());
            }
            case 6: {
                NBTTagDouble doubleTag = (NBTTagDouble) base;

                return new DoubleTag(doubleTag.asDouble());
            }
            case 7: {
                NBTTagByteArray byteArrayTag = (NBTTagByteArray) base;

                return new ByteArrayTag(byteArrayTag.c());
            }
            case 8: {
                NBTTagString stringTag = (NBTTagString) base;

                return new StringTag(stringTag.c_());
            }
            case 9: {
                NBTTagList listTag = (NBTTagList) base;

                ListTag newList = new ListTag();

                for (int i = 0; i < listTag.size(); i++) {
                    newList.addTag(fromNMS(listTag.i(i)));
                }

                return newList;
            }
            case 10: {
                CompoundTag newCompound = new CompoundTag();

                NBTTagCompound compoundTag = (NBTTagCompound) base;

                for (String key : compoundTag.c()) {
                    newCompound.setTag(key, fromNMS(compoundTag.get(key)));
                }

                return newCompound;
            }
            case 11: {
                NBTTagIntArray intArrayTag = (NBTTagIntArray) base;

                return new IntArrayTag(intArrayTag.d());
            }
            case 12: {
                NBTTagLongArray longArrayTag = (NBTTagLongArray) base;

                try {
                    return new LongArrayTag((long[]) LONG_ARRAY_B.get(longArrayTag));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
            case 0:
            default:
                throw new IllegalStateException(
                        String.format(
                                "Cannot get tag from nms: [%s]: %s",
                                base.getTypeId(), base
                        )
                );
        }
    }

    @Override
    public NBTTagCompound getItemFullNmsTag(ItemStack item) {
        net.minecraft.server.v1_12_R1.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);

        if (nmsItemStack == null) {
            return new NBTTagCompound();
        }

        return nmsItemStack.save(new NBTTagCompound());
    }

    @Override
    public CompoundTag getItemFullTag(ItemStack item) {
        return (CompoundTag) fromNMS(getItemFullNmsTag(item));
    }

    @Override
    public NBTTagCompound getItemNmsTag(ItemStack item) {
        net.minecraft.server.v1_12_R1.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);

        NBTTagCompound tag = nmsItemStack.getTag();

        if (tag == null) {
            tag = new NBTTagCompound();

            nmsItemStack.setTag(tag);
        }

        return tag;
    }

    @Override
    public CompoundTag getItemTag(ItemStack item) {
        return (CompoundTag) fromNMS(getItemNmsTag(item));
    }

    @Override
    public ItemStack setItemNmsTag(ItemStack item, Object tag) {
        net.minecraft.server.v1_12_R1.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);

        nmsItemStack.setTag((NBTTagCompound) tag);

        return CraftItemStack.asCraftMirror(nmsItemStack);
    }

    @Override
    public ItemStack setItemTag(ItemStack item, CompoundTag tag) {
        return setItemNmsTag(item, toNMS(tag));
    }

    @Override
    public ItemStack loadItem(CompoundTag tag) {
        return loadItem(toNMS(tag));
    }

    @Override
    public ItemStack loadItem(Object tag) {
        net.minecraft.server.v1_12_R1.ItemStack nmsItemStack = new net.minecraft.server.v1_12_R1.ItemStack(
                (NBTTagCompound) tag
        );

        return CraftItemStack.asCraftMirror(nmsItemStack);
    }

    @Override
    public ItemStack parseBanner(@NonNull String nbt) {
        net.minecraft.server.v1_12_R1.ItemStack nms = CraftItemStack.asNMSCopy(new ItemStack(Material.BANNER));

        try {
            nms.setTag(MojangsonParser.parse(nbt));
        } catch (MojangsonParseException e) {
            throw new RuntimeException(e);
        }

        return CraftItemStack.asBukkitCopy(nms);
    }
}
