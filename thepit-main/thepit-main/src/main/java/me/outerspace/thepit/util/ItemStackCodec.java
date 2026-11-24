package me.outerspace.thepit.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class ItemStackCodec {
    private ItemStackCodec() {}
    public static String encode(ItemStack[] items) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
                out.writeInt(items == null ? 0 : items.length);
                if (items != null) for (ItemStack item : items) out.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (Exception e) { throw new IllegalStateException("Could not encode item stacks", e); }
    }
    public static ItemStack[] decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) return new ItemStack[0];
        try {
            return decodeStrict(encoded);
        } catch (Exception e) { return new ItemStack[0]; }
    }

    public static ItemStack[] decodeStrict(String encoded) throws Exception {
        if (encoded == null || encoded.isEmpty()) throw new IllegalArgumentException("empty item payload");
        byte[] bytes = Base64.getDecoder().decode(encoded);
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            int length = in.readInt();
            if (length < 0 || length > 256) throw new IllegalArgumentException("invalid item count " + length);
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < items.length; i++) items[i] = (ItemStack) in.readObject();
            return items;
        }
    }
}
