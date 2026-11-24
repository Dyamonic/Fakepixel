package club.abelix.commons.data.rank.service;

import club.abelix.ranks.proto.RankUpdateHistoryResponse;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CompletableFuture;

public interface RankUpdateHistoryService {

    @NotNull
    CompletableFuture<RankUpdateHistoryResponse> fetchHistory(int userId);
}
