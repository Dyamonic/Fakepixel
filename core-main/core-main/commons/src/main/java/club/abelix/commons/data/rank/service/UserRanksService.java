package club.abelix.commons.data.rank.service;

import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.data.rank.UserRank;
import club.abelix.commons.gamer.IGamer;
import club.abelix.ranks.proto.AddUserRankResponse;
import club.abelix.ranks.proto.RemoveUserRankResponse;
import club.abelix.ranks.proto.UserRanksRefreshResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface UserRanksService {

    @NotNull
    @UnmodifiableView
    List<UserRank> findAllUserRanks(int playerId);

    @NotNull
    CompletableFuture<AddUserRankResponse> addUserRank(int userId, @NotNull Rank rank, @Nullable LocalDateTime expires);

    @NotNull
    CompletableFuture<AddUserRankResponse> addUserRank(int userId, int rankId, @Nullable LocalDateTime expires);

    @NotNull
    CompletableFuture<RemoveUserRankResponse> removeUserRank(int userId, int userRankId);

    @NotNull
    @UnmodifiableView
    List<UserRank> checkRankExpire(int userId);

    @NotNull
    CompletableFuture<UserRanksRefreshResponse> refreshUserRanks(int userId);

    @NotNull
    CompletableFuture<Void> refreshUsersRanks(@NotNull Collection<? extends IGamer> gamers);

    void refreshUserRank(@NotNull IGamer gamer, boolean requestFromNetwork);
}
