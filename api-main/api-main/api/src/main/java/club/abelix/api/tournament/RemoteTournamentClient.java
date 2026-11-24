package club.abelix.api.tournament;

import club.abelix.tournament.grpc.Action;
import club.abelix.tournament.grpc.CreateTournamentRequest;
import club.abelix.tournament.grpc.CreateTournamentResponse;
import club.abelix.tournament.grpc.GetCurrentTournamentRequest;
import club.abelix.tournament.grpc.GetCurrentTournamentResponse;
import club.abelix.tournament.grpc.GetPlayerTournamentsRequest;
import club.abelix.tournament.grpc.GetPlayerTournamentsResponse;
import club.abelix.tournament.grpc.GetTopPlayersRequest;
import club.abelix.tournament.grpc.GetTopPlayersResponse;
import club.abelix.tournament.grpc.GetUserRatingRequest;
import club.abelix.tournament.grpc.GetUserRatingResponse;
import club.abelix.tournament.grpc.RatingMode;
import club.abelix.tournament.grpc.RatingServiceGrpc;
import club.abelix.tournament.grpc.RegisterPlayerRequest;
import club.abelix.tournament.grpc.RegisterPlayerResponse;
import club.abelix.tournament.grpc.ResetPlayerRatingRequest;
import club.abelix.tournament.grpc.ResetPlayerRatingResponse;
import club.abelix.tournament.grpc.UpdateUserRatingRequest;
import club.abelix.tournament.grpc.UpdateUserRatingResponse;
import com.google.protobuf.UnknownFieldSet;
import io.grpc.ManagedChannel;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RemoteTournamentClient implements TournamentClient {

    private static final long CURRENT_TOURNAMENT_CACHE_TTL_MS = TimeUnit.SECONDS.toMillis(30);

    private final RatingServiceGrpc.RatingServiceBlockingStub blockingStub;
    private final ExecutorService executor;
    private final AtomicReference<CachedTournamentResponse> cachedCurrentTournament = new AtomicReference<>();
    
    public RemoteTournamentClient(ManagedChannel channel) {
        this.blockingStub = RatingServiceGrpc.newBlockingStub(channel);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public CompletableFuture<RegisterPlayerResponse> registerPlayer(long playerId, long tournamentId) {
        return CompletableFuture.supplyAsync(() -> {
            var tournamentIdField = UnknownFieldSet.Field.newBuilder()
                    .addVarint(tournamentId)
                    .build();
            RegisterPlayerRequest request = RegisterPlayerRequest.newBuilder()
                    .setPlayerId(playerId)


                    .setUnknownFields(UnknownFieldSet.newBuilder()
                            .addField(2, tournamentIdField)
                            .build())
                    .build();
            return blockingStub.registerPlayer(request);
        }, executor);
    }

    @Override
    public CompletableFuture<GetUserRatingResponse> getUserRating(long playerId, RatingMode mode) {
        return CompletableFuture.supplyAsync(() -> {
            GetUserRatingRequest request = GetUserRatingRequest.newBuilder()
                    .setPlayerId(playerId)
                    .setMode(mode)
                    .build();
            return blockingStub.getUserRating(request);
        }, executor);
    }

    @Override
    public CompletableFuture<UpdateUserRatingResponse> updateUserRating(long playerId, RatingMode mode, List<Action> actions) {
        return CompletableFuture.supplyAsync(() -> {
            UpdateUserRatingRequest request = UpdateUserRatingRequest.newBuilder()
                    .setPlayerId(playerId)
                    .setMode(mode)
                    .addAllActions(actions)
                    .build();
            return blockingStub.updateUserRating(request);
        }, executor);
    }

    @Override
    public CompletableFuture<GetTopPlayersResponse> getTopPlayers(RatingMode mode, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            GetTopPlayersRequest request = GetTopPlayersRequest.newBuilder()
                    .setMode(mode)
                    .setLimit(limit)
                    .build();
            return blockingStub.getTopPlayers(request);
        }, executor);
    }

    @Override
    public CompletableFuture<GetCurrentTournamentResponse> getCurrentTournament() {
        CachedTournamentResponse cached = cachedCurrentTournament.get();
        long now = System.currentTimeMillis();

        if (cached != null && cached.expiresAtMs() > now) {
            return CompletableFuture.completedFuture(cached.response());
        }

        return CompletableFuture.supplyAsync(() -> {
            GetCurrentTournamentRequest request = GetCurrentTournamentRequest.newBuilder().build();
            GetCurrentTournamentResponse response = blockingStub.getCurrentTournament(request);
            cachedCurrentTournament.set(new CachedTournamentResponse(
                    response,
                    System.currentTimeMillis() + CURRENT_TOURNAMENT_CACHE_TTL_MS
            ));
            return response;
        }, executor);
    }

    public void invalidateCurrentTournamentCache() {
        cachedCurrentTournament.set(null);
    }

    @Override
    public CompletableFuture<GetPlayerTournamentsResponse> getPlayerTournaments(long playerId) {
        return CompletableFuture.supplyAsync(() -> {
            GetPlayerTournamentsRequest request = GetPlayerTournamentsRequest.newBuilder()
                    .setPlayerId(playerId)
                    .build();
            return blockingStub.getPlayerTournaments(request);
        }, executor);
    }

    @Override
    public CompletableFuture<CreateTournamentResponse> createTournament(String title, String description, Instant startTime, Instant endTime, Set<RatingMode> ratingModes) {
        return CompletableFuture.supplyAsync(() -> {
            CreateTournamentRequest request = CreateTournamentRequest.newBuilder()
                    .setTitle(title)
                    .setDescription(description)
                    .setStartTimestamp(startTime.getEpochSecond())
                    .setEndTimestamp(endTime.getEpochSecond())
                    .addAllRatingModes(ratingModes)
                    .build();
            return blockingStub.createTournament(request);
        }, executor);
    }

    @Override
    public CompletableFuture<ResetPlayerRatingResponse> resetPlayerRating(long playerId, RatingMode mode, boolean resetAll) {
        return CompletableFuture.supplyAsync(() -> {
            ResetPlayerRatingRequest request = ResetPlayerRatingRequest.newBuilder()
                    .setPlayerId(playerId)
                    .setMode(mode)
                    .setResetAll(resetAll)
                    .build();
            return blockingStub.resetPlayerRating(request);
        }, executor);
    }

    private record CachedTournamentResponse(GetCurrentTournamentResponse response, long expiresAtMs) {
    }

    public void shutdown() {
        executor.shutdown();
    }
}
