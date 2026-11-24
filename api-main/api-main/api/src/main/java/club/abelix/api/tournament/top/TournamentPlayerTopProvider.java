package club.abelix.api.tournament.top;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.playertop.entry.PlayerTopEntry;
import club.abelix.api.base.playertop.provider.PlayerTopProvider;
import club.abelix.api.tournament.TournamentClient;
import club.abelix.tournament.grpc.RatingMode;
import club.abelix.tournament.grpc.TopPlayerEntry;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(staticName = "create")
public final class TournamentPlayerTopProvider implements PlayerTopProvider {

    private static final int TOP_LIMIT = 10;

    RatingMode mode;
    TournamentClient ratingClient;

    @Override
    public List<PlayerTopEntry> updatePlayerTop() throws Exception {
        return ratingClient.getTopPlayers(mode, TOP_LIMIT)
                .thenApply(res -> res.getPlayersList()
                        .stream()
                        .map(this::mapSafely)
                        .filter(Objects::nonNull)
                        .toList()
                ).get(5, TimeUnit.SECONDS);
    }

    private PlayerTopEntry mapSafely(TopPlayerEntry topPlayerEntry) {
        try {
            return map(topPlayerEntry);
        } catch (RuntimeException exception) {
            log.warn("Skipping invalid tournament top entry for player {}", topPlayerEntry.getPlayerId(), exception);
            return null;
        }
    }

    private PlayerTopEntry map(TopPlayerEntry topPlayerEntry) {
        int playerId = Math.toIntExact(topPlayerEntry.getPlayerId());
        int rating = Math.toIntExact(topPlayerEntry.getRating());

        val gamer = Gamer.getOrCreate(playerId, g -> {
            g.loadPlayerId();
            g.loadProperties();
            g.loadRanks();
            g.updateDisplayName();
        });

        if (gamer == null) {
            throw new IllegalStateException("Unable to resolve gamer for playerId=" + topPlayerEntry.getPlayerId());
        }

        return new PlayerTopEntry(
                gamer,
                rating
        );
    }
}
