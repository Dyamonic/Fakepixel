package club.abelix.api.nms.v1_16.nbt;

import club.abelix.api.nms.nbt.NbtApi;
import club.abelix.api.nms.nbt.tag.Tag;
import club.abelix.api.nms.nbt.tag.*;
import lombok.NonNull;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class NbtApi1_16 implements NbtApi {

    @Override
    public NBTBase toNMS(Tag tag) {
        switch (tag.getTypeId()) {
            case 1:
                return NBTTagByte.a(((ByteTag) tag).getValue());
            case 2:
                return NBTTagShort.a(((ShortTag) tag).getValue());
            case 3:
                return NBTTagInt.a(((IntTag) tag).getValue());
            case 4:
                return NBTTagLong.a(((LongTag) tag).getValue());
            case 5:
                return NBTTagFloat.a(((FloatTag) tag).getValue());
            case 6:
                return NBTTagDouble.a(((DoubleTag) tag).getValue());
            case 7:
                return new NBTTagByteArray(((ByteArrayTag) tag).getValue());
            case 8:
                return NBTTagString.a(((StringTag) tag).getValue());
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
            throw new IllegalArgumentException("Unsupported nbt tag: " + tag + " (" + tag.getClass().getSimpleName() + ")");
        }

        NBTBase base = (NBTBase) tag;

        switch (base.getTypeId()) {
            case 1: {
                NBTTagByte byteTag = (NBTTagByte) base;

                return new ByteTag(byteTag.asByte());
            }
            case 2: {
                NBTTagShort shortTag = (NBTTagShort) base;

                return new ShortTag(shortTag.asShort());
            }
            case 3: {
                NBTTagInt intTag = (NBTTagInt) base;

                return new IntTag(intTag.asInt());
            }
            case 4: {
                NBTTagLong longTag = (NBTTagLong) base;

                return new LongTag(longTag.asLong());
            }
            case 5: {
                NBTTagFloat floatTag = (NBTTagFloat) base;

                return new FloatTag(floatTag.asFloat());
            }
            case 6: {
                NBTTagDouble doubleTag = (NBTTagDouble) base;

                return new DoubleTag(doubleTag.asDouble());
            }
            case 7: {
                NBTTagByteArray byteArrayTag = (NBTTagByteArray) base;

                return new ByteArrayTag(byteArrayTag.getBytes());
            }
            case 8: {
                NBTTagString stringTag = (NBTTagString) base;

                return new StringTag(stringTag.asString());
            }
            case 9: {
                NBTTagList listTag = (NBTTagList) base;

                ListTag newList = new ListTag();

                for (NBTBase nbtBase : listTag) {
                    newList.addTag(fromNMS(nbtBase));
                }

                return newList;
            }
            case 10: {
                CompoundTag newCompound = new CompoundTag();

                NBTTagCompound compoundTag = (NBTTagCompound) base;

                for (String key : compoundTag.getKeys()) {
                    newCompound.setTag(key, fromNMS(compoundTag.get(key)));
                }

                return newCompound;
            }
            case 11: {
                NBTTagIntArray intArrayTag = (NBTTagIntArray) base;

                return new IntArrayTag(intArrayTag.getInts());
            }
            case 12: {
                NBTTagLongArray longArrayTag = (NBTTagLongArray) base;

                return new LongArrayTag(longArrayTag.getLongs());
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
        net.minecraft.server.v1_16_R3.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);

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
        net.minecraft.server.v1_16_R3.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);

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
        net.minecraft.server.v1_16_R3.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);

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
        net.minecraft.server.v1_16_R3.ItemStack nmsItemStack
                = net.minecraft.server.v1_16_R3.ItemStack.a((NBTTagCompound) tag);

        return CraftItemStack.asCraftMirror(nmsItemStack);
    }

    @Override
    public ItemStack parseBanner(@NonNull String nbt) {
        // TODO: 14.11.2020 fix this shit
        return null;
    }
}
