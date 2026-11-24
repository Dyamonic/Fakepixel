package club.abelix.commons.data.rank.service;

import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.data.rank.service.remote.RemoteRankService;
import club.abelix.ranks.proto.PermissionUpdateRequest;
import club.abelix.ranks.proto.PermissionUpdateResponse;
import club.abelix.ranks.proto.RefreshRanksResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface RanksService {

    void load();

    @NotNull
    @UnmodifiableView
    Set<Rank> findAllRanks();

    @NotNull
    Rank defaultRank();

    @NotNull
    Optional<Rank> findRankById(int id);

    @NotNull
    Optional<Rank> findByName(@NotNull String name);

    @NotNull
    CompletableFuture<RefreshRanksResponse> refreshRanks();

    @NotNull
    CompletableFuture<PermissionUpdateResponse> updatePermission(
            int rankId,
            @NotNull String permission,
            @NotNull PermissionUpdateRequest.Action action
    );

    @NotNull
    Optional<RemoteRankService.RankRecord> findRawById(int id);
}
