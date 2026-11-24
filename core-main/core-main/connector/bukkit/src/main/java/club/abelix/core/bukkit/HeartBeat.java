package club.abelix.core.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

public class HeartBeat {

    public static final int TWENTY_MINUTES = 24000;
    public static final int TEN_MINUTES = 12000;
    public static final int FIVE_MINUTES = 6000;
    public static final int ONE_MINUTE = 1200;

    public static final BukkitScheduler SCHEDULER = Bukkit.getScheduler();

    public static BukkitTask runAsync(Runnable async) {
        return SCHEDULER.runTaskAsynchronously(StarshipBukkit.getInstance(), async);
    }

    public static BukkitTask runSync(Runnable sync) {
        return Bukkit.getScheduler().runTask(StarshipBukkit.getInstance(), sync);
    }

    public static void callEvent(Event event) {
        callEvent(event, null);
    }

    public static <T extends Event> void callEvent(T event, Consumer<T> callback) {
        Runnable callEvent = () -> {
            Bukkit.getPluginManager().callEvent(event);

            if (callback != null) {
                callback.accept(event);
            }
        };

        boolean primaryThread = Bukkit.isPrimaryThread();
        boolean async = event.isAsynchronous();

        if (async && primaryThread) { // если он асинхронный и в главном потоке
            runAsync(callEvent);
        } else if (!async && !primaryThread) { // иначе если он синхронный и не в главном потоке
            runSync(callEvent);
        } else { // иначе если он асинхронный и не в главном или синхронный и в главном
            callEvent.run();
        }
    }

    public static BukkitTask runTimerSync(Runnable runnable, long initial, long delay) {
        return runTimerSync(StarshipBukkit.getInstance(), runnable, initial, delay);
    }

    public static BukkitTask runTimerSync(Plugin plugin, Runnable runnable, long initial, long delay) {
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, initial, delay);
    }

    public static BukkitTask runLaterSync(Runnable runnable, long delay) {
        return runLaterSync(StarshipBukkit.getInstance(), runnable, delay);
    }

    public static BukkitTask runLaterSync(Plugin plugin, Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    public static BukkitTask runLaterAsync(Runnable runnable, long delay) {
        return SCHEDULER.runTaskLaterAsynchronously(StarshipBukkit.getInstance(), runnable, delay);
    }

    public static BukkitTask runTimerAsync(Runnable runnable, long initial, long delay) {
        return SCHEDULER.runTaskTimerAsynchronously(StarshipBukkit.getInstance(), runnable, initial, delay);
    }

}
