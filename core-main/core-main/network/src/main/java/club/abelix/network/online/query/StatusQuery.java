package club.abelix.network.online.query;

import org.jetbrains.annotations.NotNull;

public interface StatusQuery<C> {

    @NotNull
    StatusQueryResult execute(@NotNull C connector);

    @NotNull
    StatusQueryParams params();
}
