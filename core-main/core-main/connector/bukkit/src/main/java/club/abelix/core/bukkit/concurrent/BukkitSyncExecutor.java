package club.abelix.core.bukkit.concurrent;

import club.abelix.core.bukkit.HeartBeat;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

/**
 * @author Unidentified Person
 */
public class BukkitSyncExecutor implements Executor {

    public static final BukkitSyncExecutor INSTANCE = new BukkitSyncExecutor();

    private BukkitSyncExecutor() {
    }

    @Override
    public void execute(@NotNull Runnable command) {
        HeartBeat.runSync(command);
    }
}
