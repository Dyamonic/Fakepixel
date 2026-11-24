package club.abelix.commons.data.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public class SessionServer {

    private final String name;
    private final LocalDateTime joinedTimestamp;

    private LocalDateTime endTimestamp;

    public SessionServer(@NotNull String name, @NotNull LocalDateTime joinedTimestamp) {
        this.name = name;
        this.joinedTimestamp = joinedTimestamp;
    }

    public SessionServer(@NotNull String name, @NotNull LocalDateTime joinedTimestamp,
                         @Nullable LocalDateTime endTimestamp) {
        this(name, joinedTimestamp);

        this.endTimestamp = endTimestamp;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull LocalDateTime getJoinedTimestamp() {
        return joinedTimestamp;
    }

    public @Nullable LocalDateTime getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(@NotNull LocalDateTime endTimestamp) {
        this.endTimestamp = endTimestamp;
    }
}
