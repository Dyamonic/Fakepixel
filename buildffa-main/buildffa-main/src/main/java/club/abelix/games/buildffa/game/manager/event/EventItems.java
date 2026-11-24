package club.abelix.games.buildffa.game.manager.event;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import org.bukkit.inventory.meta.SkullMeta;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

public final class EventItems {
    private static final String KIND = "buildffa_event_item";
    private static final String SESSION = "buildffa_event_session";
    private static final String OWNER = "buildffa_event_owner";
    private static final Map<String, ItemStack> CANDY_HEADS = new HashMap<>();

    private EventItems() {
    }

    public static ItemStack mark(ItemStack source, String kind, UUID session, UUID owner) {
        try {
            ItemStack stack = (ItemStack) MinecraftReflection.getCraftItemStackClass()
                    .getMethod("asCraftCopy", ItemStack.class).invoke(null, source);
            NbtCompound tag = (NbtCompound) NbtFactory.fromItemTag(stack);
            if (tag == null) tag = NbtFactory.ofCompound("");
            tag.put(KIND, kind).put(SESSION, session.toString()).put(OWNER, owner.toString())
                    .put("buildffa_event_token", UUID.randomUUID().toString());
            NbtFactory.setItemTag(stack, tag);
            return stack;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot tag BuildFFA event item", exception);
        }
    }

    public static ItemStack candyHead(String texture, String type, UUID session, UUID owner) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Candy heads must be created on the server thread");
        ItemStack head = CANDY_HEADS.get(texture);
        if (head == null) {
            head = texturedHead(texture);
            verifyCandyHead(head, texture);
            CANDY_HEADS.put(texture, head);
        }
        ItemStack tagged = mark(head.clone(), "candy_hunt", session, owner);
        NbtCompound tag = (NbtCompound) NbtFactory.fromItemTag(tagged);
        tag.put("buildffa_candy_item", type);
        NbtFactory.setItemTag(tagged, tag);
        verifyCandyHead(tagged, texture);
        return tagged;
    }

    private static ItemStack texturedHead(String texture) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        return applyCandyTexture(head, texture);
    }

    public static ItemStack applyCandyTexture(ItemStack source, String texture) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Candy textures must be applied on the server thread");
        if (source == null || source.getType() != Material.SKULL_ITEM || source.getDurability() != 3)
            throw new IllegalStateException("Candy texture requires a player skull");
        try {
            ItemStack head = (ItemStack) MinecraftReflection.getCraftItemStackClass()
                    .getMethod("asCraftCopy", ItemStack.class).invoke(null, source);
            NbtCompound tag = (NbtCompound) NbtFactory.fromItemTag(head);
            if (tag == null) tag = NbtFactory.ofCompound("");
            NbtCompound profile = NbtFactory.ofCompound("SkullOwner")
                    .put("Id", UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8)).toString())
                    .put("Name", "CandyHead");
            NbtCompound properties = NbtFactory.ofCompound("Properties");
            properties.put(NbtFactory.ofList("textures", Collections.singletonList(
                    NbtFactory.ofCompound("").put("Value", texture))));
            profile.put(properties);
            tag.put(profile);
            NbtFactory.setItemTag(head, tag);
            verifyCandyHead(head, texture);
            return head;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot apply Candy Hunt skull texture", exception);
        }
    }

    public static boolean matchesCandy(ItemStack stack, String type, UUID session, UUID owner) {
        if (stack == null || stack.getType() != Material.SKULL_ITEM || stack.getDurability() != 3
                || !matches(stack, "candy_hunt", session, owner)) return false;
        ItemStack craft = MinecraftReflection.getBukkitItemStack(MinecraftReflection.getMinecraftItemStack(stack));
        NbtCompound tag = (NbtCompound) NbtFactory.fromItemOptional(craft).orElse(null);
        return tag != null && tag.containsKey("buildffa_candy_item")
                && type.equals(tag.getString("buildffa_candy_item"));
    }

    public static void verifyCandyHead(ItemStack head, String texture) {
        if (head == null || head.getType() != Material.SKULL_ITEM || head.getDurability() != 3)
            throw new IllegalStateException("Candy head is not a player skull");
        ItemStack craft = MinecraftReflection.getBukkitItemStack(MinecraftReflection.getMinecraftItemStack(head));
        NbtCompound tag = (NbtCompound) NbtFactory.fromItemOptional(craft).orElse(null);
        if (tag == null || !tag.containsKey("SkullOwner"))
            throw new IllegalStateException("Candy Hunt skull has no serialized SkullOwner");
        NbtCompound profileTag = tag.getCompound("SkullOwner");
        if (!profileTag.containsKey("Properties") || !profileTag.getCompound("Properties").containsKey("textures"))
            throw new IllegalStateException("Candy Hunt skull has no serialized texture properties");
        NbtList<?> textures = profileTag.getCompound("Properties").getList("textures");
        boolean serializedTexture = false;
        for (int index = 0; index < textures.size(); index++) {
            NbtCompound property = NbtFactory.asCompound(textures.getValue().get(index));
            if (property.containsKey("Value") && texture.equals(property.getString("Value"))) serializedTexture = true;
        }
        if (!serializedTexture) throw new IllegalStateException("Candy Hunt serialized skull texture does not match");
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        try {
            Field field = meta.getClass().getDeclaredField("profile");
            field.setAccessible(true);
            Object handle = field.get(meta);
            if (handle != null) {
                WrappedGameProfile profile = WrappedGameProfile.fromHandle(handle);
                for (WrappedSignedProperty property : profile.getProperties().get("textures"))
                    if (texture.equals(property.getValue())) return;
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot verify Candy Hunt skull texture", exception);
        }
        throw new IllegalStateException("Candy Hunt skull lost its texture property");
    }

    public static boolean isTemporary(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        ItemStack craft = MinecraftReflection.getBukkitItemStack(MinecraftReflection.getMinecraftItemStack(stack));
        NbtCompound tag = (NbtCompound) NbtFactory.fromItemOptional(craft).orElse(null);
        return tag != null && tag.containsKey(KIND);
    }

    public static boolean matches(ItemStack stack, String kind, UUID session, UUID owner) {
        if (!isTemporary(stack) || session == null || owner == null) return false;
        ItemStack craft = MinecraftReflection.getBukkitItemStack(MinecraftReflection.getMinecraftItemStack(stack));
        NbtCompound tag = (NbtCompound) NbtFactory.fromItemOptional(craft).orElse(null);
        return tag != null && tag.containsKey(SESSION) && tag.containsKey(OWNER)
                && tag.containsKey("buildffa_event_token") && !tag.getString("buildffa_event_token").isEmpty()
                && kind.equals(tag.getString(KIND)) && session.toString().equals(tag.getString(SESSION))
                && owner.toString().equals(tag.getString(OWNER));
    }

    public static boolean isKind(ItemStack stack, String kind) {
        if (!isTemporary(stack)) return false;
        ItemStack craft = MinecraftReflection.getBukkitItemStack(MinecraftReflection.getMinecraftItemStack(stack));
        NbtCompound tag = (NbtCompound) NbtFactory.fromItemOptional(craft).orElse(null);
        return tag != null && kind.equals(tag.getString(KIND)) && tag.containsKey(SESSION)
                && tag.containsKey(OWNER) && tag.containsKey("buildffa_event_token");
    }

    public static void preventArrowPickup(org.bukkit.entity.Entity arrow) {
        try {
            Object handle = arrow.getClass().getMethod("getHandle").invoke(arrow);
            handle.getClass().getField("fromPlayer").setInt(handle, 2);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot protect BuildFFA roulette arrow", exception);
        }
    }
}
