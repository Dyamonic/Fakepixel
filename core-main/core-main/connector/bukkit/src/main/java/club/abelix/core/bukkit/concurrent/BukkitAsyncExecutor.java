package club.abelix.core.bukkit.concurrent;

import club.abelix.core.bukkit.HeartBeat;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

/**
 * @author Unidentified Person
 */
public class BukkitAsyncExecutor implements Executor {

    public static final BukkitAsyncExecutor INSTANCE = new BukkitAsyncExecutor();

    private BukkitAsyncExecutor() {
    }

    @Override
    public void execute(@NotNull Runnable command) {
        HeartBeat.runAsync(command);
    }
}
