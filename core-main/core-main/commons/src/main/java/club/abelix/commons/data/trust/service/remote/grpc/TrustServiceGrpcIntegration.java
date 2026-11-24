package club.abelix.commons.data.trust.service.remote.grpc;

import club.abelix.commons.data.trust.TrustLevel;
import club.abelix.trust.proto.ClearForcedLevelRequest;
import club.abelix.trust.proto.CountByLevelRequest;
import club.abelix.trust.proto.CountByLevelResponse;
import club.abelix.trust.proto.InvalidateCacheRequest;
import club.abelix.trust.proto.InvalidateCacheResponse;
import club.abelix.trust.proto.RecalculateRequest;
import club.abelix.trust.proto.SetForcedLevelRequest;
import club.abelix.trust.proto.TrustFactorRequest;
import club.abelix.trust.proto.TrustFactorResponse;
import club.abelix.trust.proto.TrustFactorsBatchRequest;
import club.abelix.trust.proto.TrustFactorsBatchResponse;
import club.abelix.commons.grpc.AbstractGrpcIntegration;
import club.abelix.trust.proto.TrustServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import w.util.Env;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TrustServiceGrpcIntegration extends AbstractGrpcIntegration {

    TrustServiceGrpc.TrustServiceStub stub;

    public TrustServiceGrpcIntegration() {
        super(Env.getString("TRUST_SERVICE_HOST", "localhost"), Env.getInt("TRUST_SERVICE_PORT", 50052));
        this.stub = TrustServiceGrpc
                .newStub(getChannel())
                .withExecutor(getExecutor())
                .withMaxInboundMessageSize(MAX_INBOUND_MESSAGE_SIZE);

        log.info("Initialized trust service integration: {}", getChannel().authority());
    }

    public CompletableFuture<TrustFactorResponse> getTrustFactor(int playerId) {
        val request = TrustFactorRequest.newBuilder()
                .setPlayerId(playerId)
                .build();

        return sendUnary(request, stub::getTrustFactor, 2, TimeUnit.SECONDS);
    }

    public CompletableFuture<TrustFactorsBatchResponse> getTrustFactors(List<Integer> playerIds) {
        val request = TrustFactorsBatchRequest.newBuilder()
                .addAllPlayerIds(playerIds)
                .build();

        return sendUnary(request, stub::getTrustFactors, 5, TimeUnit.SECONDS);
    }

    public CompletableFuture<TrustFactorResponse> recalculateTrustFactor(int playerId) {
        val request = RecalculateRequest.newBuilder()
                .setPlayerId(playerId)
                .build();

        return sendUnary(request, stub::recalculateTrustFactor, 10, TimeUnit.SECONDS);
    }

    public CompletableFuture<InvalidateCacheResponse> invalidateCache(int playerId) {
        val request = InvalidateCacheRequest.newBuilder()
                .setPlayerId(playerId)
                .build();

        return sendUnary(request, stub::invalidateCache, 1, TimeUnit.SECONDS);
    }

    public CompletableFuture<CountByLevelResponse> countByLevel(TrustLevel level) {
        val request = CountByLevelRequest.newBuilder()
                .setLevel(level.name())
                .build();

        return sendUnary(request, stub::countByLevel, 1, TimeUnit.SECONDS);
    }

    public CompletableFuture<TrustFactorResponse> setForcedLevel(int playerId, TrustLevel level) {
        val request = SetForcedLevelRequest.newBuilder()
                .setPlayerId(playerId)
                .setLevel(level.name())
                .build();

        return sendUnary(request, stub::setForcedLevel, 5, TimeUnit.SECONDS);
    }

    public CompletableFuture<TrustFactorResponse> clearForcedLevel(int playerId) {
        val request = ClearForcedLevelRequest.newBuilder()
                .setPlayerId(playerId)
                .build();

        return sendUnary(request, stub::clearForcedLevel, 5, TimeUnit.SECONDS);
    }
}
