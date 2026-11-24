package club.abelix.commons.data.plus.sql;

import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * @author pro0xy
 * <p>Created on 07.09.2023 at 16:31</p>
 */

public class PlusSqlHandler {
    private static final String GET_PLUS = "SELECT `PlusId` FROM `Pluses` WHERE `Id` = ? AND `PlusId` = ?;";
    private static final String GET_PLUSES = "SELECT * FROM `Pluses` WHERE `Id` = ?;";
    private static final String ADD_PLUS = "INSERT INTO `Pluses` (`Id`, `PlusId`) VALUES (?, ?);";
    private static final String REMOVE_PLUS = "DELETE FROM `Pluses` WHERE `Id` = ? AND `PlusId` = ?;";
    public static PlusSqlHandler INSTANCE = new PlusSqlHandler();
    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Get plus by gamer id and plus id
     *
     * @param playerId - Gamer id
     * @param plusId   - Status id
     * @return - Plus {@link PlusEntry}
     */
    public CompletableFuture<PlusEntry> getPlus(int playerId, int plusId) {
        return database.executeQuery(GET_PLUS, playerId, plusId).thenApply(query -> {
            if (query.isEmpty()) {
                return null;
            }

            BufferedRow row = query.getFirst();
            int plus = row.getRequiredInt("StatusId");

            return new PlusEntry(playerId, plus);
        });
    }

    /**
     * Get unlocked pluses by gamer id
     *
     * @param playerId - Gamer id
     * @return - Plus {@link PlusEntry}
     */
    public CompletableFuture<ArrayList<PlusEntry>> getUnlockedPluses(int playerId) {
        return database.executeQuery(GET_PLUSES, playerId).thenApply(rows -> {
            ArrayList<PlusEntry> result = new ArrayList<>();

            rows.forEach(bufferedRow -> result.add(new PlusEntry(
                    playerId,
                    bufferedRow.getRequiredInt("PlusId")
            )));

            return result;
        });
    }

    /**
     * Add unlocked plus
     *
     * @param playerId - Gamer id
     * @param plusId   - Plus id
     */
    public CompletableFuture<?> unlockPlus(int playerId, @NonNull Integer plusId) {
        return database.executeVoidUpdate(ADD_PLUS, playerId, plusId);
    }

    /**
     * Lock plus
     *
     * @param playerId - Gamer id
     * @param plusId   - Plus id
     */
    public CompletableFuture<?> lockPlus(int playerId, int plusId) {
        return database.executeVoidUpdate(REMOVE_PLUS, playerId, plusId);
    }
}
