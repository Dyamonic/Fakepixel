package club.abelix.games.buildffa.game.dto.dao.impl;

import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.games.buildffa.game.dto.dao.QuestProgressDAO;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class QuestProgressDAOImpl implements QuestProgressDAO {
    public static final QuestProgressDAOImpl INSTANCE = new QuestProgressDAOImpl();

    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `player_quest_progress` (" +
            "`id` bigint(20) NOT NULL AUTO_INCREMENT, " +
            "`player_id` int(11) NOT NULL, " +
            "`quest_id` varchar(64) NOT NULL, " +
            "`period_type` enum('DAILY','WEEKLY') NOT NULL, " +
            "`period_key` varchar(16) NOT NULL, " +
            "`progress` int(11) NOT NULL DEFAULT 0, " +
            "`claimed` tinyint(1) NOT NULL DEFAULT 0, " +
            "`updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
            "PRIMARY KEY (`id`), " +
            "UNIQUE KEY `uq_player_quest_period` (`player_id`,`quest_id`,`period_type`,`period_key`)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
    private static final String GET_ONE =
            "SELECT `progress`, CAST(`claimed` AS SIGNED) AS `claimed_value` " +
            "FROM `player_quest_progress` " +
            "WHERE `player_id` = ? AND `quest_id` = ? " +
            "AND `period_type` = ? AND `period_key` = ?";
    private static final String ENSURE = "INSERT IGNORE INTO `player_quest_progress` " +
            "(`player_id`,`quest_id`,`period_type`,`period_key`,`progress`,`claimed`) VALUES (?, ?, ?, ?, 0, 0)";
    private static final String ADD = "INSERT INTO `player_quest_progress` " +
            "(`player_id`,`quest_id`,`period_type`,`period_key`,`progress`) VALUES (?, ?, ?, ?, LEAST(?, ?)) " +
            "ON DUPLICATE KEY UPDATE `progress` = IF(`claimed` = 0 AND `progress` < ?, LEAST(?, `progress` + ?), `progress`)";
    private static final String SET_MAX = "INSERT INTO `player_quest_progress` " +
            "(`player_id`,`quest_id`,`period_type`,`period_key`,`progress`) VALUES (?, ?, ?, ?, LEAST(?, ?)) " +
            "ON DUPLICATE KEY UPDATE `progress` = IF(`claimed` = 0 AND `progress` < ?, LEAST(?, GREATEST(`progress`, ?)), `progress`)";
    private static final String CLAIM = "UPDATE `player_quest_progress` SET `claimed` = 1 " +
            "WHERE `player_id` = ? AND `quest_id` = ? AND `period_type` = ? AND `period_key` = ? " +
            "AND `progress` >= ? AND `claimed` = 0";
    private static final String WIPE_PLAYER = "DELETE FROM `player_quest_progress` WHERE `player_id` = ?";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("game_buildffa"));
    private volatile CompletableFuture<Void> initialized;
    private volatile InitializationState initializationState = InitializationState.INITIALIZING;
    private volatile boolean available = true;

    private QuestProgressDAOImpl() {
        initialized = initialize();
    }

    @Override
    public CompletableFuture<QuestProgress> getProgress(int playerId, String questId, String periodType, String periodKey) {
        CompletableFuture<QuestProgress> future = initialization().thenCompose(ignored -> databaseOperation(database.executeQuery(
                GET_ONE, playerId, questId, periodType, periodKey
        ))).thenApply(rows -> rows.isEmpty()
                ? new QuestProgress(0, false)
                : new QuestProgress(
                        rows.get(0).getInt("progress").orElse(0),
                        rows.get(0).getInt("claimed_value").orElse(0) > 0
                ));
        return track(future);
    }

    @Override
    public CompletableFuture<Void> ensureProgress(int playerId, String questId, String periodType, String periodKey) {
        return track(initialization().thenCompose(ignored -> databaseOperation(database.executeVoidUpdate(
                ENSURE, playerId, questId, periodType, periodKey
        ))).thenApply(result -> null));
    }

    @Override
    public CompletableFuture<Void> addProgress(int playerId, String questId, String periodType, String periodKey, int amount, int requiredProgress) {
        return track(initialization().thenCompose(ignored -> databaseOperation(database.executeVoidUpdate(
                ADD, playerId, questId, periodType, periodKey, amount, requiredProgress,
                requiredProgress, requiredProgress, amount
        ))).thenApply(result -> null));
    }

    @Override
    public CompletableFuture<Void> setMaxProgress(int playerId, String questId, String periodType, String periodKey, int value, int requiredProgress) {
        return track(initialization().thenCompose(ignored -> databaseOperation(database.executeVoidUpdate(
                SET_MAX, playerId, questId, periodType, periodKey, value, requiredProgress,
                requiredProgress, requiredProgress, value
        ))).thenApply(result -> null));
    }

    @Override
    public CompletableFuture<ClaimResult> claim(int playerId, String questId, String periodType, String periodKey, int requiredProgress) {
        return initialization().thenCompose(ignored -> databaseOperation(database.executeQuery(
                GET_ONE, playerId, questId, periodType, periodKey
        )).thenCompose(rows -> {
            if (rows.isEmpty()) return CompletableFuture.completedFuture(ClaimResult.INCOMPLETE);
            int progress = rows.get(0).getInt("progress").orElse(0);
            boolean claimed = rows.get(0).getInt("claimed_value").orElse(0) > 0;
            if (claimed) return CompletableFuture.completedFuture(ClaimResult.ALREADY_CLAIMED);
            if (progress < requiredProgress) return CompletableFuture.completedFuture(ClaimResult.INCOMPLETE);
            return databaseOperation(database.executeVoidUpdate(
                    CLAIM, playerId, questId, periodType, periodKey, requiredProgress
            )).thenCompose(execution -> {
                int affectedRows = affectedRows(execution);
                if (affectedRows == 1) return CompletableFuture.completedFuture(ClaimResult.CLAIMED);
                if (affectedRows != 0) {
                    return failedFuture(new IllegalStateException("Conditional quest claim affected " + affectedRows + " rows"));
                }
                return databaseOperation(database.executeQuery(
                        GET_ONE, playerId, questId, periodType, periodKey
                )).thenCompose(currentRows -> {
                    if (currentRows.isEmpty()) return CompletableFuture.completedFuture(ClaimResult.INCOMPLETE);
                    int currentProgress = currentRows.get(0).getInt("progress").orElse(0);
                    boolean currentlyClaimed = currentRows.get(0).getInt("claimed_value").orElse(0) > 0;
                    if (currentlyClaimed) return CompletableFuture.completedFuture(ClaimResult.ALREADY_CLAIMED);
                    if (currentProgress < requiredProgress) return CompletableFuture.completedFuture(ClaimResult.INCOMPLETE);
                    return failedFuture(new IllegalStateException("Conditional quest claim changed no row"));
                });
            });
        }));
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public InitializationState getInitializationState() {
        return initializationState;
    }

    @Override
    public synchronized CompletableFuture<Void> retryInitialization() {
        if (initializationState != InitializationState.FAILED) return initialized;
        initializationState = InitializationState.INITIALIZING;
        available = true;
        initialized = initialize();
        return initialized;
    }

    @Override
    public CompletableFuture<Void> resetPlayerProgress(int playerId) {
        return track(initialization().thenCompose(ignored -> databaseOperation(
                database.executeVoidUpdate(WIPE_PLAYER, playerId))).thenApply(ignored -> null));
    }

    private CompletableFuture<Void> initialization() {
        return initialized;
    }

    private CompletableFuture<Void> initialize() {
        CompletableFuture<Void> future;
        try {
            future = database.executeVoidUpdate(CREATE_TABLE).thenApply(result -> null);
        } catch (Throwable throwable) {
            future = new CompletableFuture<>();
            future.completeExceptionally(throwable);
        }
        future.whenComplete((result, throwable) -> {
            initializationState = throwable == null ? InitializationState.READY : InitializationState.FAILED;
            available = throwable == null;
        });
        return future;
    }

    private int affectedRows(Object execution) {
        if (execution == null) throw new IllegalStateException("Quest claim returned a null BufferedExecution");
        String[] accessors = {
                "getAffectedRows", "affectedRows", "getAffectedRowCount", "affectedRowCount",
                "getRowsAffected", "rowsAffected", "getUpdateCount", "updateCount", "getResult", "result"
        };
        for (String accessor : accessors) {
            try {
                Method method = execution.getClass().getMethod(accessor);
                Object value = method.invoke(execution);
                if (value instanceof Number) return ((Number) value).intValue();
                if (value instanceof OptionalInt) return ((OptionalInt) value).orElse(0);
                if (value instanceof Optional) {
                    Object optionalValue = ((Optional<?>) value).orElse(null);
                    if (optionalValue instanceof Number) return ((Number) optionalValue).intValue();
                }
            } catch (NoSuchMethodException ignored) {
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new CompletionException(exception);
            }
        }
        throw new IllegalStateException("BufferedExecution does not expose a supported affected-row accessor");
    }

    private <T> CompletableFuture<T> databaseOperation(CompletableFuture<T> future) {
        return future.whenComplete((result, throwable) -> available = throwable == null);
    }

    private <T> CompletableFuture<T> failedFuture(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    private <T> CompletableFuture<T> track(CompletableFuture<T> future) {
        return future;
    }
}
