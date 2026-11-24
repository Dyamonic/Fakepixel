package club.abelix.commons.data.rank.service.remote;

import club.abelix.commons.data.rank.service.RankUpdateHistoryService;
import club.abelix.commons.data.rank.service.remote.grpc.UserRankHistoryGrpcIntegration;
import club.abelix.di.api.annotation.Bean;
import club.abelix.ranks.proto.RankUpdateHistoryResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RemoteRankUpdateHistoryService implements RankUpdateHistoryService {

    @Getter
    UserRankHistoryGrpcIntegration integration;

    @Override
    public @NotNull CompletableFuture<RankUpdateHistoryResponse> fetchHistory(int userId) {
        return integration.fetchHistory(userId);
    }

}
