package club.abelix.core.app.data.sync;

import club.abelix.core.app.connection.RemoteClientConnection;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.common.thread.ThreadUncaughtExceptionHandler;
import club.abelix.network.session.PlayerRedisSessionService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProxyGamerSyncTask {

    private final long DATA_IS_OLD_THRESHOLD = TimeUnit.MINUTES.toMillis(2);

    private final ScheduledExecutorService scheduler;
    private final PlayerRedisSessionService sessionService;
    private ScheduledFuture<?> future = null;

    public ProxyGamerSyncTask(
            final @NotNull PlayerRedisSessionService sessionService
    ) {
        this.sessionService = sessionService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("core-sync")
                        .setUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler())
                        .build()
        );
    }

    public void start() {
        if (future != null) return;

        this.future =
                this.scheduler.scheduleAtFixedRate(this::sync,
                        1, 1, TimeUnit.MINUTES);
    }

    public void stop() {
        if (future == null) return;

        this.future.cancel(true);
        this.future = null;
    }

    @SneakyThrows
    private void sync() {
        val gamers = Gamer.getGamers();

        if (gamers.isEmpty()) return;

        int initialGamersSize = gamers.size();
        int success = 0;
        int failed = 0;

        log.info("Running sync for {} players", gamers.size());

        for (val gamer : gamers) {
            val gamerName = gamer.getName().toLowerCase();
            val gamerSession = sessionService.getSession(gamerName);

            if (RemoteClientConnection.PENDING_LOGINS.contains(gamerName)) {
                log.warn("Found gamer {} in redis but mark pending login", gamerName);
                continue;
            }

            if (gamerSession == null) {
                log.warn("Found gamer {} in core but session not found", gamerName);

                gamer.quit();
                failed++;

                continue;
            }

            if (gamerSession.lastUpdate() < System.currentTimeMillis() - DATA_IS_OLD_THRESHOLD) {
                log.warn("Found gamer {} in core but session data is old, ignoring (Possibly velocity or redis is dead)", gamerName);

                continue;
            }

            if (!gamer.isOnline()) {
                log.warn("Gamer {} not found in redis, disconnecting...", gamerName);

                gamer.quit();
                failed++;
                continue;
            }

            success++;
        }

        log.info("Sync finished. Result: Success {} Failed {} Gamers: Before sync {} After sync {}",
                success,
                failed,
                initialGamersSize,
                Gamer.getGamers().size()
        );
    }

}
