package club.abelix.commons.data.rank.service.remote;

import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.data.rank.UserRank;
import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.data.rank.entity.UserRankEntity;
import club.abelix.commons.data.rank.service.RanksService;
import club.abelix.commons.data.rank.service.UserRanksService;
import club.abelix.commons.data.rank.service.mapper.RanksMapper;
import club.abelix.commons.data.rank.service.remote.grpc.UserRanksGrpcIntegration;
import club.abelix.commons.data.rank.type.UserRankType;
import club.abelix.commons.gamer.IGamer;
import club.abelix.ranks.commons.util.TimestampConverter;
import club.abelix.ranks.proto.AddUserRankResponse;
import club.abelix.ranks.proto.RemoveUserRankResponse;
import club.abelix.ranks.proto.UserRanksRefreshResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RemoteUserRanksService implements UserRanksService {

    @Getter
    UserRanksGrpcIntegration grpcIntegration;

    RanksService rankService;

    @Override
    public @NotNull @UnmodifiableView List<UserRank> findAllUserRanks(int playerId) {
        val resp = grpcIntegration.findAllUserRanks(playerId).join();
        val def = rankService.defaultRank();

        if (resp == null || resp.getUserRanksList().isEmpty()) {
            return List.of(
                    new UserRank(
                            def.getId(),
                            playerId,
                            def,
                            null,
                            UserRankType.PERMANENT
                    ));
        }

        return resp.getUserRanksList().stream()
                .map(r -> new UserRank(
                        r.getId(),
                        r.getUserId(),
                        rankService.findRankById(r.getRank().getId()).orElse(def),
                        TimestampConverter.fromProto(r.getExpiresAt()),
                        UserRankType.fromProto(r.getRankType())
                ))
                .toList();
    }

    @Override
    public @NotNull CompletableFuture<AddUserRankResponse> addUserRank(int userId, @NotNull Rank rank, @Nullable LocalDateTime expires) {
        return addUserRank(userId, rank.getId(), expires);
    }

    @Override
    public @NotNull CompletableFuture<AddUserRankResponse> addUserRank(int userId, int rankId, @Nullable LocalDateTime expires) {
        return grpcIntegration.addUserRank(userId, rankId, expires).thenApply(resp -> {
            val newRank = resp.getUserRank();
            val newRankId = newRank.getRank().getId();

            if (rankService.findRankById(newRankId).isEmpty())
                throw new IllegalStateException("Received rank with id " + newRankId + " which does not exist locally");

            return resp;
        });
    }

    @Override
    public @NotNull CompletableFuture<RemoveUserRankResponse> removeUserRank(int userId, int userRankId) {
        return grpcIntegration.removeUserRank(userId, userRankId);
    }

    @Override
    public @NotNull List<UserRank> checkRankExpire(int userId) {
        val resp = grpcIntegration.checkExpiredRanks(userId).join();

        return resp.getExpiredRanksList()
                .stream()
                .map(RanksMapper::mapFromProto)
                .map(this::toUserRankModel)
                .toList();
    }

    @Override
    public @NotNull CompletableFuture<UserRanksRefreshResponse> refreshUserRanks(int userId) {
        return grpcIntegration.refreshUserRanks(userId);
    }

    @Override
    public @NotNull CompletableFuture<Void> refreshUsersRanks(@NotNull Collection<? extends IGamer> gamers) {
        return CompletableFuture.runAsync(() -> {
            CompletableFuture.runAsync(() -> {
                for (IGamer g : gamers) {
                    try {
                        refreshUserRank(g, false);
                    } catch (Exception e) {
                        log.error("Failed to refresh ranks for gamer {}", g.getName(), e);
                    }
                }
            });

        });
    }

    @Override
    public void refreshUserRank(@NotNull IGamer gamer, boolean requestFromNetwork) {
        val oldRanks = gamer.getRanks();

        List<UserRank> newRanks;

        if (requestFromNetwork) {
            newRanks = findAllUserRanks(gamer.getPlayerId());
        } else {
            newRanks = oldRanks.stream()
                    .map(r -> new UserRank(
                            r.id(),
                            r.playerId(),
                            rankService.findRankById(r.rank().getId()).orElse(rankService.defaultRank()),
                            r.expiration(),
                            r.type()
                    ))
                    .toList();
        }

        if (!oldRanks.equals(newRanks)) {
            val ranks = new UserRanks();

            ranks.addAllRanks(newRanks);

            gamer.setRanks(ranks);
        }
    }

    private UserRank toUserRankModel(UserRankEntity userRankEntity) {
        val rankEntity = userRankEntity.getRank();
        val rankOptional = rankService.findRankById(rankEntity.getId());

        if (rankOptional.isEmpty())
            throw new RuntimeException("Rank with id " + rankEntity.getId() + " does not exist");

        val rank = rankOptional.get();

        return new UserRank(userRankEntity.getId(), userRankEntity.getUserId(), rank, userRankEntity.getExpires(), userRankEntity.getType());
    }

}
