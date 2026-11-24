package club.abelix.commons.data.rank;

import club.abelix.commons.data.rank.permission.FullGroupPermissions;
import club.abelix.commons.data.rank.permission.Permissions;
import club.abelix.commons.data.rank.type.UserRankType;
import lombok.Setter;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

@Setter
public class UserRanks {

    public static final UserRanks CONSOLE = new UserRanks();

    static {
        CONSOLE.addRank(
                new UserRank(
                        -1,
                        -1,
                        new Rank(
                                -1,
                                Integer.MAX_VALUE,
                                "CONSOLE",
                                "rank_console",
                                null,
                                null,
                                new FullGroupPermissions(Set.of(Permissions.fullPermission()))
                        ),
                        null,
                        UserRankType.PERMANENT
                ));
    }

    private final NavigableSet<UserRank> userRanks =
            new TreeSet<>(Comparator
                    .comparingInt((club.abelix.commons.data.rank.UserRank r) -> r.rank().getPriority())
                    .reversed()
                    .thenComparingInt(rank -> rank.rank().getId()));

    public UserRank findPriorityRank() {
        return userRanks.getFirst();
    }

    public boolean hasRank(int rankId) {
        return userRanks.stream().anyMatch(r -> r.rank().getId() == rankId);
    }

    public UserRank findUserRankById(int rankId) {
        return userRanks.stream()
                .filter(r -> r.rank().getId() == rankId)
                .findFirst()
                .orElse(null);
    }

    public boolean hasPermission(String permission) {
        return userRanks.stream().anyMatch(r -> r.rank().hasPermission(permission));
    }

    public void addRank(UserRank rank) {
        userRanks.add(rank);
    }

    public void addAllRanks(Collection<UserRank> ranks) {
        userRanks.addAll(ranks);
    }

    public void removeRank(int rankId) {
        userRanks.removeIf(r -> r.rank().getId() == rankId);
    }

    public Stream<UserRank> stream() {
        return userRanks.stream();
    }

    public boolean isStaff() {
        return hasPermission("core.staff");
    }

    public Set<UserRank> asSet() {
        return Set.copyOf(userRanks);
    }

    public void clear() {
        userRanks.clear();
    }

}
