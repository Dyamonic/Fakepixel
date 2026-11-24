package club.abelix.api.tournament.service;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.tournament.TournamentClient;
import club.abelix.api.tournament.gui.TournamentGui;
import club.abelix.tournament.grpc.Action;
import club.abelix.tournament.grpc.CreateTournamentResponse;
import club.abelix.tournament.grpc.GetCurrentTournamentResponse;
import club.abelix.tournament.grpc.GetPlayerTournamentsResponse;
import club.abelix.tournament.grpc.GetUserRatingResponse;
import club.abelix.tournament.grpc.RatingMode;
import club.abelix.tournament.grpc.TournamentInfo;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class TournamentService {

    private static final String TOURNAMENT_REGISTRATION_PROPERTY_FORMAT = "Tournament/Registration/%s";

    TournamentClient client;

    public void openTournamentGui(Gamer gamer) {
        new TournamentGui(gamer, this);
    }

    public CompletableFuture<GetCurrentTournamentResponse> getCurrentTournament() {
        return client.getCurrentTournament();
    }

    public CompletableFuture<GetUserRatingResponse> getUserRating(Gamer gamer, RatingMode mode) {
        return client.getUserRating(gamer.getPlayerId(), mode);
    }

    public void updateRating(
            Gamer gamer,
            RatingMode mode,
            List<Action> actions
    ) {
        client.getCurrentTournament().whenComplete((current, currentErr) -> {
            if (currentErr != null) {
                log.error("Failed to fetch current tournament before updating rating for player {}", gamer.getPlayerId(), currentErr);
                return;
            }

            if (!current.getHasActiveTournament() || !current.hasTournament()) {
                log.debug("Skipping tournament rating update for player {}: no active tournament", gamer.getPlayerId());
                return;
            }

            if (!isRegistered(gamer, current.getTournament())) {
                log.info("Skipping tournament rating update for player {}: not registered for tournament {}", gamer.getPlayerId(), current.getTournament().getId());
                return;
            }

            client.updateUserRating(
                    gamer.getPlayerId(),
                    mode,
                    actions
            ).whenComplete((res, err) -> {
                if (err != null) {
                    Throwable cause = err.getCause() != null ? err.getCause() : err;

                    if (cause instanceof StatusRuntimeException e) {
                        if (e.getStatus() == Status.NOT_FOUND) {
                            log.warn("Player {} not found in rating service when updating rating: {}", gamer.getPlayerId(), e.getMessage());
                            return;
                        }

                        log.error("Failed to update tournament rating for player {}: {}", gamer.getPlayerId(), e.getMessage());
                        return;
                    }

                    log.error("Failed to update tournament rating for player {}", gamer.getPlayerId(), cause);
                    return;
                }

                val oldRating = res.getOldRating();
                val newRating = res.getNewRating();
                val delta = res.getRatingAdded();

                gamer.sendLocalizedMessage(
                        "tournament_rating_change",
                        "%old%",
                        oldRating,
                        "%new%",
                        newRating,
                        "%delta%",
                        delta
                );

                gamer.playSound(YSound.LEVEL_UP);
                log.info("Updated player {} rating {} -> {} ({})", gamer.getName(), oldRating, newRating, delta);
            });
        });
    }

    public CompletableFuture<Boolean> register(Gamer gamer, TournamentInfo info) {
        val formattedProperty = TOURNAMENT_REGISTRATION_PROPERTY_FORMAT.formatted(info.getId());

        if (isRegistered(gamer, info)) return CompletableFuture.completedFuture(false);

        long now = Instant.now().getEpochSecond();
        log.info("Checking registration for player {}: now={}, start={}, end={}, id={}",
                gamer.getPlayerId(), now, info.getStartTimestamp(), info.getEndTimestamp(), info.getId());
        if (info.getStartTimestamp() > now || info.getEndTimestamp() < now) {
            log.warn("Skipping registration for player {}: tournament {} is not active (now={}, start={}, end={})",
                    gamer.getPlayerId(), info.getId(), now, info.getStartTimestamp(), info.getEndTimestamp());
            return CompletableFuture.completedFuture(false);
        }

        final long tournamentId;
        try {
            tournamentId = Long.parseLong(info.getId());
        } catch (NumberFormatException e) {
            log.error("Tournament id {} is not numeric, cannot register player {}", info.getId(), gamer.getPlayerId(), e);
            return CompletableFuture.completedFuture(false);
        }

        return client.registerPlayer(gamer.getPlayerId(), tournamentId).handle((res, err) -> {
            if (err != null && err.getCause() instanceof StatusRuntimeException e) {

                boolean alreadyRegistered = e.getStatus().getCode() == Status.Code.ALREADY_EXISTS
                        || (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT
                        && e.getStatus().getDescription() != null
                        && e.getStatus().getDescription().contains("already registered"));

                if (alreadyRegistered) {
                    gamer.setProperty(formattedProperty, 1);
                    return false;
                }

                if (e.getStatus().getCode() == Status.Code.FAILED_PRECONDITION) {
                    log.warn("Tournament {} is not active for player {}: {}", info.getId(), gamer.getPlayerId(), e.getMessage());
                    return false;
                }

                log.error("An error occurred while registering player {} for tournament {}: {}", gamer.getPlayerId(), info.getId(), e.getMessage());
                return false;
            }

            gamer.setProperty(formattedProperty, 1);
            return true;
        });
    }

    public boolean isRegistered(Gamer gamer, TournamentInfo info) {
        val formattedProperty = TOURNAMENT_REGISTRATION_PROPERTY_FORMAT.formatted(info.getId());
        val property = gamer.getProperty(formattedProperty);

        return property == 1;
    }

    public CompletableFuture<GetPlayerTournamentsResponse> getPlayerTournaments(Gamer gamer) {
        return client.getPlayerTournaments(gamer.getPlayerId());
    }

    public CompletableFuture<CreateTournamentResponse> createTournament(String titleKey, String descriptionKey, Instant startTime, Instant endTime, Set<RatingMode> ratingModes) {
        return client.createTournament(titleKey, descriptionKey, startTime, endTime, ratingModes);
    }

}
