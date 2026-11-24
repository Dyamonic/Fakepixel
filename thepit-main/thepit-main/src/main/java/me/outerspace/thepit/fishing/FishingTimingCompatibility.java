package me.outerspace.thepit.fishing;

import me.outerspace.thepit.ThePitPlugin;
import org.bukkit.entity.Fish;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class FishingTimingCompatibility {
    private FishingTimingCompatibility() {}

    public static void accelerate(ThePitPlugin plugin, Fish hook, double increase) {
        if (hook == null || increase <= 0.0D) return;
        BukkitTaskHandle.schedule(plugin, () -> apply(hook, increase), 20L);
    }

    private static void apply(Fish hook, double increase) {
        try {
            if (hook.isDead()) return;
            Method getHandle = hook.getClass().getMethod("getHandle");
            Object handle = getHandle.invoke(hook);
            for (Field field : handle.getClass().getDeclaredFields()) {
                if (field.getType() != int.class) continue;
                field.setAccessible(true);
                int value = field.getInt(handle);
                if (value >= 100 && value <= 900) {
                    field.setInt(handle, Math.max(1, (int) Math.ceil(value * (1.0D - increase))));
                    return;
                }
            }
        } catch (Throwable ignored) { }
    }

    private static final class BukkitTaskHandle {
        private static void schedule(ThePitPlugin plugin, Runnable task, long delay) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }
}
