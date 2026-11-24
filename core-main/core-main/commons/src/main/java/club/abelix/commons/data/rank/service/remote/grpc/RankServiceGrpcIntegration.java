package club.abelix.commons.data.rank.service.remote.grpc;

import club.abelix.ranks.proto.AllRanksRequest;
import club.abelix.ranks.proto.AllRanksResponse;
import club.abelix.ranks.proto.DefaultRankRequest;
import club.abelix.ranks.proto.DefaultRankResponse;
import club.abelix.ranks.proto.PermissionUpdateRequest;
import club.abelix.ranks.proto.PermissionUpdateResponse;
import club.abelix.ranks.proto.RanksServiceGrpc;
import club.abelix.ranks.proto.RefreshRanksRequest;
import club.abelix.ranks.proto.RefreshRanksResponse;
import club.abelix.commons.grpc.AbstractGrpcIntegration;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import w.util.Env;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RankServiceGrpcIntegration extends AbstractGrpcIntegration {

    RanksServiceGrpc.RanksServiceStub stub;

    public RankServiceGrpcIntegration() {
        super(Env.getString("RANKS_SERVICE_HOST", "localhost"), Env.getInt("RANKS_SERVICE_PORT", 50051));
        this.stub = RanksServiceGrpc
                .newStub(getChannel())
                .withExecutor(getExecutor())
                .withMaxInboundMessageSize(MAX_INBOUND_MESSAGE_SIZE)
                .withInterceptors(new RequestsLoggerInterceptor());

        log.info("Initialized rank service integration: {}", getChannel().authority());
    }

    public CompletableFuture<AllRanksResponse> findAllRanks() {
        val request = AllRanksRequest.newBuilder().build();
        return sendUnary(request, stub::getAllRanks, 5, TimeUnit.SECONDS);
    }

    public CompletableFuture<DefaultRankResponse> defaultRank() {
        val request = DefaultRankRequest.newBuilder().build();
        return sendUnary(request, stub::getDefaultRank, 1, TimeUnit.SECONDS);
    }

    public CompletableFuture<RefreshRanksResponse> refreshRanks() {
        val request = RefreshRanksRequest.newBuilder().build();
        return sendUnary(request, stub::refreshRanks, 5, TimeUnit.SECONDS);
    }

    public CompletableFuture<PermissionUpdateResponse> updatePermissions(int rankId, String permission, PermissionUpdateRequest.Action action) {
        val request = PermissionUpdateRequest.newBuilder()
                .setRankId(rankId)
                .setPermission(permission)
                .setAction(action)
                .build();

        return sendUnary(request, stub::updatePermission, 1, TimeUnit.SECONDS);
    }
}
