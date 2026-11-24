package club.abelix.commons.data.status.sql;

import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * @author pro0xy
 * <p>Created on 02.09.2023 at 21:15</p>
 */

public class StatusSqlHandler {
    private static final String GET_STATUS = "SELECT `StatusId` FROM `Statuses` WHERE `Id` = ? AND `StatusId` = ?;";
    private static final String GET_STATUSES = "SELECT * FROM `Statuses` WHERE `Id` = ?;";
    private static final String SET_STATUS = "INSERT INTO `Statuses` (`Id`, `StatusId`) VALUES (?, ?);";
    private static final String REMOVE_STATUS = "DELETE FROM `Statuses` WHERE `Id` = ? AND `StatusId` = ?;";
    public static StatusSqlHandler INSTANCE = new StatusSqlHandler();
    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Get status by gamer id and status id
     *
     * @param playerId - Gamer id
     * @param statusId - Status id
     * @return - Status {@link StatusEntry}
     */
    public CompletableFuture<StatusEntry> getStatus(int playerId, int statusId) {
        return database.executeQuery(GET_STATUS, playerId, statusId).thenApply(query -> {
            if (query.isEmpty()) {
                return null;
            }

            BufferedRow row = query.getFirst();
            int status = row.getRequiredInt("StatusId");

            return new StatusEntry(playerId, status);
        });
    }

    /**
     * Get unlocked statuses by gamer id
     *
     * @param playerId - Gamer id
     * @return - Status {@link StatusEntry}
     */
    public CompletableFuture<ArrayList<StatusEntry>> getUnlockedStatuses(int playerId) {
        return database.executeQuery(GET_STATUSES, playerId).thenApply(rows -> {
            ArrayList<StatusEntry> result = new ArrayList<>();

            rows.forEach(bufferedRow -> result.add(new StatusEntry(
                    playerId,
                    bufferedRow.getRequiredInt("StatusId")
            )));

            return result;
        });
    }

    /**
     * Add unlocked status
     *
     * @param playerId - Gamer id
     * @param statusId - Status id
     */
    public CompletableFuture<?> unlockStatus(int playerId, @NonNull Integer statusId) {
        return database.executeVoidUpdate(SET_STATUS, playerId, statusId);
    }

    /**
     * Remove status
     *
     * @param playerId - Gamer id
     * @param statusId - Status id
     */
    public CompletableFuture<?> lockStatus(int playerId, int statusId) {
        return database.executeVoidUpdate(REMOVE_STATUS, playerId, statusId);
    }
}
