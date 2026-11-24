package club.abelix.commons.data.rank.service.remote.grpc;

import club.abelix.commons.grpc.AbstractGrpcIntegration;
import club.abelix.ranks.proto.RankUpdateHistoryRequest;
import club.abelix.ranks.proto.RankUpdateHistoryResponse;
import club.abelix.ranks.proto.UserRankHistoryServiceGrpc;
import lombok.val;
import w.util.Env;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class UserRankHistoryGrpcIntegration extends AbstractGrpcIntegration {

    private final UserRankHistoryServiceGrpc.UserRankHistoryServiceStub stub;

    public UserRankHistoryGrpcIntegration() {
        super(Env.getString("RANKS_SERVICE_HOST", "localhost"), Env.getInt("RANKS_SERVICE_PORT", 50051));
        this.stub = UserRankHistoryServiceGrpc.newStub(getChannel())
                .withExecutor(getExecutor())
                .withMaxInboundMessageSize(MAX_INBOUND_MESSAGE_SIZE)
                .withInterceptors(new RequestsLoggerInterceptor());
    }

    public CompletableFuture<RankUpdateHistoryResponse> fetchHistory(int userId) {
        val request = RankUpdateHistoryRequest.newBuilder()
                .setUserId(userId)
                .build();

        return sendUnary(request, stub::getRankUpdateHistory, 5, TimeUnit.SECONDS);
    }

}
