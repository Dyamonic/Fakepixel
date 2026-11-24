package club.abelix.core.app.data.stats;

import club.abelix.commons.mysql.modern.BufferedExecution;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * @author Unidentified Person
 */
public final class StatsWipeSqlHandler {

    // ==================================================
    public static final StatsWipeSqlHandler INSTANCE = new StatsWipeSqlHandler();
    // ==================================================

    private static final String INSERT_ROW
            = "INSERT INTO `StatsWipe`(`id`, `date`, `from_day`) VALUES (?, ?, ?)";

    private static final String SELECT_ROWS
            = "SELECT `id`, `from_day` FROM `StatsWipe` WHERE `date` < NOW();";

    private static final String DELETE_ROW
            = "DELETE FROM `StatsWipe` WHERE `id` = ?";

    private static final String DELETE_ROWS
            = "DELETE FROM `StatsWipe` WHERE `id` IN %s";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    public void insert(int id, int wipeDay, LocalDateTime time) {
        database.executeVoidUpdate(INSERT_ROW, id, time, wipeDay);
    }

    public void delete(int id) {
        database.executeVoidUpdate(DELETE_ROW, id);
    }

    private String join(int[] ids) {
        StringBuilder sb = new StringBuilder("(");

        for (int i = 0; i < ids.length; i++) {
            if (i != 0) {
                sb.append(',');
            }

            sb.append(ids[i]);
        }

        sb.append(')');

        return sb.toString();
    }

    public CompletableFuture<Integer> deleteAndCheck(int[] ids) {
        return database.executeVoidUpdate(String.format(DELETE_ROWS, join(ids)))
                .thenApply(BufferedExecution::getAffectedRows);
    }

    public void delete(int[] ids) {
        database.executeVoidUpdate(String.format(DELETE_ROWS, join(ids)));
    }

    public CompletableFuture<WipingStatEntry[]> select() {
        return database.executeQuery(SELECT_ROWS)
                .thenApply(resp -> resp.stream().map(row ->
                        new WipingStatEntry(
                                row.getRequiredInt("id"),
                                row.getRequiredInt("from_day")
                        )).toArray(WipingStatEntry[]::new));
    }

    @RequiredArgsConstructor
    @Getter
    public static class WipingStatEntry {

        private final int playerId;
        private final int fromDay;

    }

}
