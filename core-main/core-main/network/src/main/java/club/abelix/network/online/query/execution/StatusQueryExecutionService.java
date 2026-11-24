package club.abelix.network.online.query.execution;

import club.abelix.network.online.query.StatusQuery;
import club.abelix.network.online.query.StatusQueryResult;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface StatusQueryExecutionService {

    @NotNull
    StatusQueryResult executeSync(@NotNull StatusQuery query, boolean refresh);

    default @NotNull StatusQueryResult executeSync(@NotNull StatusQuery query) {
        return executeSync(query, false);
    }

    @NotNull
    CompletableFuture<StatusQueryResult> executeAsync(@NotNull StatusQuery query, boolean refresh);

    default @NotNull CompletableFuture<StatusQueryResult> executeAsync(@NotNull StatusQuery query) {
        return executeAsync(query, false);
    }

    void shutdown();

}
