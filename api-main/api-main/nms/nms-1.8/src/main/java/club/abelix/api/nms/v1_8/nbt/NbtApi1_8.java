package club.abelix.api.nms.v1_8.nbt;

import club.abelix.api.nms.nbt.NbtApi;
import club.abelix.api.nms.nbt.tag.*;
import lombok.NonNull;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class NbtApi1_8 implements NbtApi {

    public Object toNMS(Tag tag) {
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
                    tagList.add((NBTBase) toNMS(element));
                }

                return tagList;
            case 10:
                NBTTagCompound compound = new NBTTagCompound();

                ((CompoundTag) tag).forEach((name, value) -> compound.set(name, (NBTBase) toNMS(value)));

                return compound;
            case 11:
                return new NBTTagIntArray(((IntArrayTag) tag).getValue());
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
            case 1:
                NBTTagByte byteTag = (NBTTagByte) base;

                return new ByteTag(byteTag.f());
            case 2:
                NBTTagShort shortTag = (NBTTagShort) base;

                return new ShortTag(shortTag.e());
            case 3:
                NBTTagInt intTag = (NBTTagInt) base;

                return new IntTag(intTag.d());
            case 4:
                NBTTagLong longTag = (NBTTagLong) base;

                return new LongTag(longTag.c());
            case 5:
                NBTTagFloat floatTag = (NBTTagFloat) base;

                return new FloatTag(floatTag.h());
            case 6:
                NBTTagDouble doubleTag = (NBTTagDouble) base;

                return new DoubleTag(doubleTag.g());
            case 7:
                NBTTagByteArray byteArrayTag = (NBTTagByteArray) base;

                return new ByteArrayTag(byteArrayTag.c());
            case 8:
                NBTTagString stringTag = (NBTTagString) base;

                return new StringTag(stringTag.a_());
            case 9:
                NBTTagList listTag = (NBTTagList) base;

                ListTag newList = new ListTag();

                for (int i = 0; i < listTag.size(); i++) {
                    newList.addTag(fromNMS(listTag.g(i)));
                }

                return newList;
            case 10:
                CompoundTag newCompound = new CompoundTag();

                NBTTagCompound compoundTag = (NBTTagCompound) base;

                for (String key : compoundTag.c()) {
                    newCompound.setTag(key, fromNMS(compoundTag.get(key)));
                }

                return newCompound;
            case 11:
                NBTTagIntArray intArrayTag = (NBTTagIntArray) base;

                return new IntArrayTag(intArrayTag.c());
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
    public Object getItemNmsTag(ItemStack item) {
        net.minecraft.server.v1_8_R3.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);

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
    public CompoundTag getItemFullTag(ItemStack item) {
        return (CompoundTag) fromNMS(getItemFullNmsTag(item));
    }


    @Override
    public Object getItemFullNmsTag(ItemStack item) {
        net.minecraft.server.v1_8_R3.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);

        if (nmsItemStack == null) {
            return new NBTTagCompound();
        }

        return nmsItemStack.save(new NBTTagCompound());
    }

    @Override
    public ItemStack setItemNmsTag(ItemStack item, Object tag) {
        net.minecraft.server.v1_8_R3.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);

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
        net.minecraft.server.v1_8_R3.ItemStack nmsItemStack = net.minecraft.server.v1_8_R3.ItemStack.createStack((NBTTagCompound) tag);

        return CraftItemStack.asCraftMirror(nmsItemStack);
    }

    @Override
    public ItemStack parseBanner(@NonNull String nbt) {
        net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(new ItemStack(Material.BANNER));

        try {
            nms.setTag(MojangsonParser.parse(nbt));
        } catch (MojangsonParseException e) {
            throw new RuntimeException(e);
        }

        return CraftItemStack.asBukkitCopy(nms);
    }
}
