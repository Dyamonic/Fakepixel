package club.abelix.commons.data.stat.staff;

import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

public class StaffStatsSqlHandler {

    public static final StaffStatsSqlHandler INSTANCE = new StaffStatsSqlHandler();

    private static final String FETCH_STAFF_STAFF_BY_ID_QUERY = "SELECT * FROM `playerdata`.`staff_stats` WHERE `id` = ?;";
    private static final String UPDATE_STAFF_STATS_BY_ID_QUERY =
            """
                     UPDATE `playerdata`.`staff_stats`
                     SET\s
                         `next_update` = DATE_ADD(NOW(), INTERVAL 7 DAY),
                         `closed_reports` = `closed_reports` + ?,
                         `punishments` = `punishments` + ?
                     WHERE\s
                         NOW() >= `next_update`
                         AND `id` = ?;
                    \s""";
    private static final String INSERT_STAFF_STATS_BY_ID_QUERY = "INSERT INTO `playerdata`.`staff_stats` (`id`, `closed_reports`, `punishments`, `next_update`) VALUES (?, 0, 0, ?);";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    public CompletableFuture<StaffStats> insertNewStaffStats(int playerId) {
        LocalDateTime nextUpdate = LocalDateTime.now().plusDays(7);

        return database.executeUpdate(INSERT_STAFF_STATS_BY_ID_QUERY, playerId, nextUpdate)
                .thenApply(query -> new StaffStats(0, 0, nextUpdate));
    }

    public CompletableFuture<StaffStats> loadStaffStats(int playerId) {
        return loadStaffStats(playerId, true);
    }

    public CompletableFuture<StaffStats> loadStaffStats(int playerId, boolean insertNew) {
        return database.executeQuery(FETCH_STAFF_STAFF_BY_ID_QUERY, playerId).thenApply(rows -> {
            if (rows.isEmpty()) return insertNew ? insertNewStaffStats(playerId).join() : StaffStats.EMPTY;

            BufferedRow rawRecord = rows.getFirst();

            int closedReports = rawRecord.getRequiredInt("closed_reports");
            int punishments = rawRecord.getRequiredInt("punishments");
            LocalDateTime nextUpdate = rawRecord.getRequiredTimestamp("next_update");

            return new StaffStats(closedReports, punishments, nextUpdate);
        });
    }

    public void updateStaffStats(int playerId, int closedReports, int punishments) {
        database.executeVoidUpdate(UPDATE_STAFF_STATS_BY_ID_QUERY, closedReports, punishments, playerId);
    }
}
