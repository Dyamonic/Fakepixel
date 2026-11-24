package club.abelix.api;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Log4j2
@Deprecated
@UtilityClass
public class HeartBeat {

    public final int TWENTY_MINUTES = 24000;
    public final int TEN_MINUTES = 12000;
    public final int FIVE_MINUTES = 6000;
    public final int ONE_MINUTE = 1200;

    public final BukkitScheduler SCHEDULER = Bukkit.getScheduler();

    public BukkitTask runAsync(Runnable async) {
        return SCHEDULER.runTaskAsynchronously(FalconAPI.getInstance(), async);
    }

    public BukkitTask runSync(Runnable sync) {
        return Bukkit.getScheduler().runTask(FalconAPI.getInstance(), sync);
    }

    public void callEvent(Event event) {
        callEvent(event, null);
    }

    public <T extends Event> void callEvent(T event, Consumer<T> callback) {
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

    public BukkitTask runTimerSync(Runnable runnable, long initial, long delay) {
        return runTimerSync(FalconAPI.getInstance(), runnable, initial, delay);
    }

    public BukkitTask runTimerSync(Plugin plugin, Runnable runnable, long initial, long delay) {
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, initial, delay);
    }

    public BukkitTask runLaterSync(Runnable runnable, long delay) {
        return runLaterSync(FalconAPI.getInstance(), runnable, delay);
    }

    public BukkitTask runLaterSync(Plugin plugin, Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    @Deprecated
    public BukkitTask runLaterAsync(Runnable runnable, long delay, TimeUnit timeUnit) {
        log.warn("Method runLaterAsync(Runnable, long, TimeUnit) is deprecated");
        return runLaterAsync(runnable, timeUnit.toMillis(delay) / 50);
    }

    public BukkitTask runLaterAsync(Runnable runnable, long delay) {
        return SCHEDULER.runTaskLaterAsynchronously(FalconAPI.getInstance(), runnable, delay);
    }

    @Deprecated
    public BukkitTask runTimerAsync(Runnable runnable, long initial, long delay, TimeUnit timeUnit) {
        log.warn("Method runTimerAsync(Runnable, long, long, TimeUnit) is deprecated");

        return runTimerAsync(runnable, timeUnit.toMillis(initial) / 50, timeUnit.toMillis(delay) / 50);
    }

    public BukkitTask runTimerAsync(Runnable runnable, long initial, long delay) {
        return SCHEDULER.runTaskTimerAsynchronously(FalconAPI.getInstance(), runnable, initial, delay);
    }

}
