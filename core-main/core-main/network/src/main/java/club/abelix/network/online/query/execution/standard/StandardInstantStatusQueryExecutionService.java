package club.abelix.network.online.query.execution.standard;

import club.abelix.network.online.query.StatusQuery;
import club.abelix.network.online.query.StatusQueryResult;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import club.abelix.network.redis.JedisConnector;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class StandardInstantStatusQueryExecutionService implements StatusQueryExecutionService {

    private static final ExecutorService EXECUTOR = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                    .name("online-query-", 0)
                    .factory()
    );

    JedisConnector connector;
    AtomicBoolean shutdown = new AtomicBoolean(false);

    @Override
    public @NotNull StatusQueryResult executeSync(@NotNull StatusQuery query, boolean refresh) {
        if (shutdown.get()) {
            log.warn("Status query execution attempted during shutdown");
            return StatusQueryResult.EMPTY;
        }

        return query.execute(connector);
    }

    @Override
    public @NotNull CompletableFuture<StatusQueryResult> executeAsync(@NotNull StatusQuery query, boolean refresh) {
        return CompletableFuture.supplyAsync(
                () -> executeSync(query),
                EXECUTOR
        );
    }

    @Override
    public void shutdown() {
        shutdown.set(true);
        EXECUTOR.shutdown();
    }
}
