package club.abelix.api.tournament;

import club.abelix.tournament.grpc.Action;
import club.abelix.tournament.grpc.CreateTournamentResponse;
import club.abelix.tournament.grpc.GetCurrentTournamentResponse;
import club.abelix.tournament.grpc.GetPlayerTournamentsResponse;
import club.abelix.tournament.grpc.GetTopPlayersResponse;
import club.abelix.tournament.grpc.GetUserRatingResponse;
import club.abelix.tournament.grpc.RatingMode;
import club.abelix.tournament.grpc.RegisterPlayerResponse;
import club.abelix.tournament.grpc.ResetPlayerRatingResponse;
import club.abelix.tournament.grpc.UpdateUserRatingResponse;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Set;

public interface TournamentClient {

    CompletableFuture<RegisterPlayerResponse> registerPlayer(long playerId, long tournamentId);

    CompletableFuture<GetUserRatingResponse> getUserRating(long playerId, RatingMode mode);

    CompletableFuture<UpdateUserRatingResponse> updateUserRating(long playerId, RatingMode mode, List<Action> actions);

    CompletableFuture<GetTopPlayersResponse> getTopPlayers(RatingMode mode, int limit);

    CompletableFuture<GetCurrentTournamentResponse> getCurrentTournament();

    CompletableFuture<GetPlayerTournamentsResponse> getPlayerTournaments(long playerId);

    CompletableFuture<CreateTournamentResponse> createTournament(String title, String description, Instant startTime, Instant endTime, Set<RatingMode> ratingModes);

    CompletableFuture<ResetPlayerRatingResponse> resetPlayerRating(long playerId, RatingMode mode, boolean resetAll);
}
