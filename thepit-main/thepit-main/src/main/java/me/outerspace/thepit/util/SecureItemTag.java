package me.outerspace.thepit.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;

public final class SecureItemTag {
    public static final String ITEM_KEY = "ThePitItem";
    public static final String LEGACY_ITEM_KEY = "BuildFFAItem";
    private static Logger logger;
    private static long lastConflictLog;
    private static Method asNmsCopy;
    private static Method asBukkitCopy;
    private static Method getTag;
    private static Method setTag;
    private static Method hasKeyOfType;
    private static Method setString;
    private static Method getString;
    private static Method setInt;
    private static Method getInt;
    private static Method setBoolean;
    private static Class<?> compoundClass;
    private static boolean initializationAttempted;
    private static volatile boolean available;

    private SecureItemTag() {}

    public static synchronized boolean initialize(Logger logger) {
        SecureItemTag.logger = logger;
        if (initializationAttempted) return available;
        initializationAttempted = true;
        try {
            String version = org.bukkit.Bukkit.getServer().getClass().getPackage().getName();
            version = version.substring(version.lastIndexOf('.') + 1);
            if (!"v1_8_R3".equals(version)) throw new IllegalStateException("expected v1_8_R3 but found " + version);
            Class<?> craftItem = Class.forName("org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack");
            Class<?> nmsItem = Class.forName("net.minecraft.server.v1_8_R3.ItemStack");
            Class<?> resolvedCompound = Class.forName("net.minecraft.server.v1_8_R3.NBTTagCompound");
            Method resolvedAsNmsCopy = craftItem.getMethod("asNMSCopy", ItemStack.class);
            Method resolvedAsBukkitCopy = craftItem.getMethod("asBukkitCopy", nmsItem);
            Method resolvedGetTag = nmsItem.getMethod("getTag");
            Method resolvedSetTag = nmsItem.getMethod("setTag", resolvedCompound);
            Method resolvedHasKeyOfType = resolvedCompound.getMethod("hasKeyOfType", String.class, int.class);
            Method resolvedSetString = resolvedCompound.getMethod("setString", String.class, String.class);
            Method resolvedGetString = resolvedCompound.getMethod("getString", String.class);
            Method resolvedSetInt = resolvedCompound.getMethod("setInt", String.class, int.class);
            Method resolvedGetInt = resolvedCompound.getMethod("getInt", String.class);
            Method resolvedSetBoolean = resolvedCompound.getMethod("setBoolean", String.class, boolean.class);
            requireStatic(resolvedAsNmsCopy);
            requireStatic(resolvedAsBukkitCopy);
            requireInstance(resolvedGetTag);
            requireInstance(resolvedSetTag);
            requireInstance(resolvedHasKeyOfType);
            requireInstance(resolvedSetString);
            requireInstance(resolvedGetString);
            requireInstance(resolvedSetInt);
            requireInstance(resolvedGetInt);
            requireInstance(resolvedSetBoolean);
            compoundClass = resolvedCompound;
            asBukkitCopy = resolvedAsBukkitCopy;
            getTag = resolvedGetTag;
            setTag = resolvedSetTag;
            hasKeyOfType = resolvedHasKeyOfType;
            setString = resolvedSetString;
            getString = resolvedGetString;
            setInt = resolvedSetInt;
            getInt = resolvedGetInt;
            setBoolean = resolvedSetBoolean;
            asNmsCopy = resolvedAsNmsCopy;
            available = true;
            return true;
        } catch (Throwable failure) {
            available = false;
            if (logger != null) logger.severe("ThePit secure item tags are disabled: " + failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage()));
            return false;
        }
    }

    public static boolean isAvailable() { return available; }

    public static ItemStack string(ItemStack item, String key, String value) {
        return mutate(item, isItemKey(key) ? ITEM_KEY : key, value, null, false);
    }

    public static ItemStack integer(ItemStack item, String key, int value) {
        return mutate(item, key, null, value, false);
    }

    public static ItemStack unbreakable(ItemStack item) {
        return mutate(item, "Unbreakable", null, null, true);
    }

    public static String string(ItemStack item, String key) {
        if (isItemKey(key)) return itemId(item);
        return rawString(item, key);
    }

    public static String itemId(ItemStack item) {
        String current = rawString(item, ITEM_KEY);
        String legacy = rawString(item, LEGACY_ITEM_KEY);
        if (!current.isEmpty() && !legacy.isEmpty() && !current.equals(legacy)) {
            long now = System.currentTimeMillis();
            if (logger != null && now - lastConflictLog >= 60000L) {
                lastConflictLog = now;
                logger.warning("Rejected an item with conflicting ThePitItem and BuildFFAItem tags.");
            }
            return "";
        }
        return !current.isEmpty() ? current : legacy;
    }

    private static String rawString(ItemStack item, String key) {
        if (!readable(item, key)) return "";
        try {
            Object tag = tag(item);
            if (tag == null || !hasType(tag, key, 8)) return "";
            Object value = getString.invoke(tag, key);
            return value instanceof String ? (String) value : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static int getInteger(ItemStack item, String key, int fallback) {
        if (!readable(item, key)) return fallback;
        try {
            Object tag = tag(item);
            if (tag == null || !hasType(tag, key, 99)) return fallback;
            Object value = getInt.invoke(tag, key);
            return value instanceof Number ? ((Number) value).intValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static boolean is(ItemStack item, String id) {
        return id != null && id.equals(itemId(item));
    }

    private static ItemStack mutate(ItemStack item, String key, String text, Integer number, boolean bool) {
        if (!readable(item, key)) return item;
        try {
            Object nms = asNmsCopy.invoke(null, item);
            if (nms == null) return item;
            Object tag = getTag.invoke(nms);
            if (tag == null) tag = compoundClass.getConstructor().newInstance();
            if (text != null) setString.invoke(tag, key, text);
            else if (number != null) setInt.invoke(tag, key, number);
            else if (bool) setBoolean.invoke(tag, key, true);
            setTag.invoke(nms, tag);
            Object result = asBukkitCopy.invoke(null, nms);
            return result instanceof ItemStack ? (ItemStack) result : item;
        } catch (Throwable ignored) {
            return item;
        }
    }

    private static Object tag(ItemStack item) throws Exception {
        Object nms = asNmsCopy.invoke(null, item);
        if (nms == null) return null;
        return getTag.invoke(nms);
    }

    private static boolean hasType(Object tag, String key, int type) throws Exception {
        Object result = hasKeyOfType.invoke(tag, key, type);
        return result instanceof Boolean && (Boolean) result;
    }

    private static boolean readable(ItemStack item, String key) {
        return available && item != null && item.getType() != Material.AIR && key != null && !key.isEmpty();
    }

    private static boolean isItemKey(String key) {
        return ITEM_KEY.equals(key) || LEGACY_ITEM_KEY.equals(key);
    }

    private static void requireStatic(Method method) {
        if (!Modifier.isStatic(method.getModifiers())) throw new IllegalStateException(method.getName() + " must be static");
    }

    private static void requireInstance(Method method) {
        if (Modifier.isStatic(method.getModifiers())) throw new IllegalStateException(method.getName() + " must be an instance method");
    }
}
