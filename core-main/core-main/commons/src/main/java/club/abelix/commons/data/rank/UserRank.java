package club.abelix.commons.data.rank;

import club.abelix.commons.data.rank.type.UserRankType;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

public record UserRank(int id, int playerId, Rank rank, LocalDateTime expiration, UserRankType type) {

    public boolean isExpired() {
        return expiration != null && expiration.isBefore(LocalDateTime.now());
    }

    public long getExpirationTime(final @NotNull TimeUnit unit) {
        return expiration != null
                ? unit.convert(expiration.atZone(ZoneId.systemDefault()).toEpochSecond(), TimeUnit.SECONDS)
                : -1;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        UserRank other = (UserRank) obj;

        return id == other.id;
    }
}
