package club.abelix.commons.data.rank.service.remote.grpc;

import club.abelix.ranks.commons.util.TimestampConverter;
import club.abelix.ranks.proto.AddUserRankRequest;
import club.abelix.ranks.proto.AddUserRankResponse;
import club.abelix.ranks.proto.ExpiredRanksRequest;
import club.abelix.ranks.proto.ExpiredRanksResponse;
import club.abelix.ranks.proto.RemoveUserRankRequest;
import club.abelix.ranks.proto.RemoveUserRankResponse;
import club.abelix.ranks.proto.UserRanksRefreshRequest;
import club.abelix.ranks.proto.UserRanksRefreshResponse;
import club.abelix.ranks.proto.UserRanksRequest;
import club.abelix.ranks.proto.UserRanksResponse;
import club.abelix.ranks.proto.UserRanksServiceGrpc;
import club.abelix.commons.grpc.AbstractGrpcIntegration;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import w.util.Env;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UserRanksGrpcIntegration extends AbstractGrpcIntegration {

    UserRanksServiceGrpc.UserRanksServiceStub stub;

    public UserRanksGrpcIntegration() {
        super(Env.getString("RANKS_SERVICE_HOST", "localhost"), Env.getInt("RANKS_SERVICE_PORT", 50051));
        this.stub = UserRanksServiceGrpc
                .newStub(getChannel())
                .withExecutor(getExecutor())
                .withMaxInboundMessageSize(MAX_INBOUND_MESSAGE_SIZE)
                .withInterceptors(new RequestsLoggerInterceptor());

        log.info("Initialized user ranks integration: {}", getChannel().authority());
    }

    public CompletableFuture<UserRanksResponse> findAllUserRanks(int userId) {
        val request = UserRanksRequest.newBuilder()
                .setUserId(userId)
                .build();

        return sendUnary(request, stub::getUserRanks, 2, TimeUnit.SECONDS);
    }

    public CompletableFuture<AddUserRankResponse> addUserRank(
            int userId,
            int rankId,
            @Nullable LocalDateTime expires
    ) {
        val requestBuilder = AddUserRankRequest.newBuilder()
                .setUserId(userId)
                .setRankId(rankId);

        if (expires != null) {
            requestBuilder.setExpires(TimestampConverter.toProto(expires));
        }

        val request = requestBuilder.build();
        return sendUnary(request, stub::addUserRank, 2, TimeUnit.SECONDS);
    }

    public CompletableFuture<RemoveUserRankResponse> removeUserRank(int userId, int userRankId) {
        val request = RemoveUserRankRequest.newBuilder()
                .setUserId(userId)
                .setUserRankId(userRankId)
                .build();

        return sendUnary(request, stub::removeUserRank, 2, TimeUnit.SECONDS);
    }

    public CompletableFuture<ExpiredRanksResponse> checkExpiredRanks(int userId) {
        val request = ExpiredRanksRequest.newBuilder().setUserId(userId)
                .build();

        return sendUnary(request, stub::getExpiredRanks, 2, TimeUnit.SECONDS);
    }

    public CompletableFuture<UserRanksRefreshResponse> refreshUserRanks(int userId) {
        val request = UserRanksRefreshRequest.newBuilder().setUserId(userId).build();

        return sendUnary(request, stub::refreshUserRanks, 2, TimeUnit.SECONDS);
    }

}
