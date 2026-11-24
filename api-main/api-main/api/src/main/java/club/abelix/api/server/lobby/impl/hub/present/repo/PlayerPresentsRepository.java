package club.abelix.api.server.lobby.impl.hub.present.repo;

import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.TimeUnit;

@Log4j2
@UtilityClass
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerPresentsRepository {

    MySQL DATABASE = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    String FIND_ALL_PLAYER_FOUND_PRESENTS = "SELECT present_id FROM player_presents WHERE player_id = ?";
    String ADD_FOUND_PRESENT = "INSERT INTO player_presents (player_id, present_id) VALUES (?, ?)";

    @SneakyThrows
    public IntSet getFoundPresents(final int playerId) {
        return DATABASE.executeQuery(FIND_ALL_PLAYER_FOUND_PRESENTS, playerId)
                .exceptionally(t -> {
                    log.error("An error occurred while loading found presents of player with id {}", playerId, t);
                    throw new RuntimeException(t);
                })
                .thenApply(rows -> {
                    IntSet presentIds = new IntOpenHashSet();

                    for (BufferedRow row : rows) {
                        presentIds.add(row.getRequiredInt("present_id"));
                    }

                    return presentIds;
                })
                .get(500, TimeUnit.MILLISECONDS);
    }

    public void addFoundPresent(
            final int playerId,
            final int presentId
    ) {
        DATABASE.executeUpdate(ADD_FOUND_PRESENT, playerId, presentId);
    }
}
