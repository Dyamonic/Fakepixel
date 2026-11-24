package club.abelix.commons.data.trust.service.remote;

import club.abelix.commons.data.trust.TrustFactor;
import club.abelix.commons.data.trust.TrustLevel;
import club.abelix.commons.data.trust.service.TrustService;
import club.abelix.commons.data.trust.service.remote.grpc.TrustServiceGrpcIntegration;
import club.abelix.trust.commons.util.TimestampConverter;
import club.abelix.trust.proto.CountByLevelResponse;
import club.abelix.trust.proto.TrustFactorData;
import club.abelix.trust.proto.TrustFactorResponse;
import club.abelix.trust.proto.TrustFactorsBatchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
public class RemoteTrustService implements TrustService {

    private final TrustServiceGrpcIntegration grpc;

    @Override
    public @NotNull CompletableFuture<Optional<TrustFactor>> getTrustFactor(int playerId) {
        return grpc.getTrustFactor(playerId)
                .thenApply(response -> {
                    if (!response.getFound()) return Optional.<TrustFactor>empty();
                    return Optional.of(mapFromProto(response.getTrustFactor()));
                })
                .exceptionally(t -> {
                    log.error("Failed to get trust factor for player {}", playerId, t);
                    return Optional.empty();
                });
    }

    @Override
    public @NotNull CompletableFuture<List<TrustFactor>> getTrustFactors(@NotNull List<Integer> playerIds) {
        return grpc.getTrustFactors(playerIds)
                .thenApply(TrustFactorsBatchResponse::getTrustFactorsList)
                .thenApply(list -> list.stream().map(this::mapFromProto).toList())
                .exceptionally(t -> {
                    log.error("Failed to get batch trust factors", t);
                    return List.of();
                });
    }

    @Override
    public @NotNull CompletableFuture<TrustFactor> recalculateTrustFactor(int playerId) {
        return grpc.recalculateTrustFactor(playerId)
                .thenApply(TrustFactorResponse::getTrustFactor)
                .thenApply(this::mapFromProto)
                .exceptionally(t -> {
                    log.error("Failed to recalculate trust factor for player {}", playerId, t);
                    throw new RuntimeException(t);
                });
    }

    @Override
    public @NotNull CompletableFuture<Integer> countByLevel(@NotNull TrustLevel level) {
        return grpc.countByLevel(level)
                .thenApply(CountByLevelResponse::getCount)
                .exceptionally(t -> {
                    log.error("Failed to count players with {} level", level.name(), t);
                    throw new RuntimeException(t);
                });
    }

    @Override
    public @NotNull CompletableFuture<TrustFactor> setForcedLevel(int playerId, @NotNull TrustLevel level) {
        return grpc.setForcedLevel(playerId, level)
                .thenApply(TrustFactorResponse::getTrustFactor)
                .thenApply(this::mapFromProto)
                .exceptionally(t -> {
                    log.error("Failed to set forced level {} for player {}", level.name(), playerId, t);
                    throw new RuntimeException(t);
                });
    }

    @Override
    public @NotNull CompletableFuture<TrustFactor> clearForcedLevel(int playerId) {
        return grpc.clearForcedLevel(playerId)
                .thenApply(TrustFactorResponse::getTrustFactor)
                .thenApply(this::mapFromProto)
                .exceptionally(t -> {
                    log.error("Failed to clear forced level for player {}", playerId, t);
                    throw new RuntimeException(t);
                });
    }

    @Override
    public void invalidateCache(int playerId) {
        grpc.invalidateCache(playerId)
                .exceptionally(t -> {
                    log.warn("Failed to invalidate trust factor cache for player {}", playerId, t);
                    return null;
                });
    }

    private TrustFactor mapFromProto(TrustFactorData data) {
        return TrustFactor.builder()
                .playerId(data.getPlayerId())
                .level(parseLevel(data.getLevel(), data.getPlayerId()))
                .score(data.getScore())
                .totalPlaytimeSeconds(data.getTotalPlaytimeSeconds())
                .donationEffort(data.getDonationEffort())
                .mutesCount(data.getMutesCount())
                .bansCount(data.getBansCount())
                .lastRecalculated(TimestampConverter.fromProto(data.getLastRecalculated()))
                .createdAt(TimestampConverter.fromProto(data.getCreatedAt()))
                .forcedLevel(data.hasForcedLevel() ? parseLevel(data.getForcedLevel(), data.getPlayerId()) : null)
                .build();
    }

    private TrustLevel parseLevel(String levelStr, int playerId) {
        try {
            return TrustLevel.valueOf(levelStr);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown trust level '{}' for player {}, defaulting to UNTRUSTED", levelStr, playerId);
            return TrustLevel.UNTRUSTED;
        }
    }
}
