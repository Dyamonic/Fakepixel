package club.abelix.commons.data.gamehistory;

import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.mode.Mini;
import lombok.NonNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>GameHistorySqlHandler creation on 03.09.2023 at 20:15</p>
 */

public class GameHistorySqlHandler {

    public static final GameHistorySqlHandler INSTANCE = new GameHistorySqlHandler();

    private static final String INSERT_NEW_GAME = "INSERT INTO `GameHistory` (`Mini`, `Map`, `StartTime`, `EndTime`) VALUES (?, ?, ?, ?)";
    private static final String INSERT_NEW_GAME_PLAYER = "INSERT INTO `GameHistoryPlayers` (`GameId`, `PlayerId`, `Winner`) VALUES (?, ?, ?)";
    private static final String GET_GAMER_LAST_GAMES = "SELECT * FROM  `GameHistoryPlayers` WHERE `PlayerId` = ? ORDER BY id DESC LIMIT 50";
    private static final String GET_GAME_GAMERS = "SELECT * FROM `GameHistoryPlayers` WHERE `GameId` = ?";
    private static final String GET_GAME = "SELECT * FROM `GameHistory` WHERE `Id` = ?";
    private static final String GAMER_GAME_BY_MODE = "SELECT GameHistoryPlayers.GameId FROM GameHistoryPlayers INNER JOIN GameHistory ON GameHistory.Mini = '?' WHERE GameHistoryPlayers.PlayerId=? ORDER BY id DESC LIMIT 50";
    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("gamehistory"));

    public void saveGame(@NonNull GameHistoryEntry gameHistoryEntry) {
        database.executeUpdate(INSERT_NEW_GAME, gameHistoryEntry.getMini().getName(), gameHistoryEntry.getMapName(),
                gameHistoryEntry.getStartTime(), gameHistoryEntry.getEndTIme()).whenCompleteAsync((generateId, throwable) -> {
            if (throwable != null) {
                Logging.IMP.error("[GameHistory] Failed save game! Details: ");
                throwable.printStackTrace();
                return;
            }

            int gameId = generateId.getGeneratedKey();
            gameHistoryEntry.setId(gameId);

            gameHistoryEntry.getPlayers().forEach(
                    player ->
                            database.executeVoidUpdate(INSERT_NEW_GAME_PLAYER,
                                    gameHistoryEntry.getId(),
                                    player.getPlayerId(),
                                    player.isWinner() ? 1 : 0));
        });
    }

    public CompletableFuture<List<GameHistoryEntry>> getPlayerAllGameHistory(int playerId) {
        return database.executeQuery(GET_GAMER_LAST_GAMES, playerId).thenApply(rows -> {
            List<GameHistoryEntry> history = new ArrayList<>();

            rows.forEach(rawEntry -> {
                int gameId = rawEntry.getRequiredInt("GameId");

                history.add(getGame(gameId).join());
            });

            return history;
        });
    }

    public CompletableFuture<List<GameHistoryEntry>> getPlayerMiniGameHistory(int playerId, Mini mini) {
        return database.executeQuery(GAMER_GAME_BY_MODE, mini.getName(), playerId).thenApply(rows -> {
            List<GameHistoryEntry> history = new ArrayList<>();

            rows.forEach(rawEntry -> {
                int gameId = rawEntry.getRequiredInt("GameId");

                history.add(getGame(gameId).join());
            });

            return history;
        });

    }

    public CompletableFuture<GameHistoryEntry> getGame(int gameId) {
        return database.executeQuery(GET_GAME, gameId).thenApply(bufferedRows -> {
            if (bufferedRows.isEmpty()) {
                return null;
            }

            BufferedRow data = bufferedRows.getFirst();

            int id = data.getRequiredInt("Id");
            Mini mini = Mini.getByName(data.getRequiredString("Mini"));
            String map = data.getRequiredString("Map");
            long startTime = data.getRequiredLong("StartTime");
            long stopTime = data.getRequiredLong("StopTime");

            List<GameHistoryPlayerEntry> players = getPlayersFromGame(id).join();

            return new GameHistoryEntry(id, new Timestamp(startTime),
                    new Timestamp(stopTime), mini, map, players);
        });

    }

    public CompletableFuture<List<GameHistoryPlayerEntry>> getPlayersFromGame(int gameId) {
        return database.executeQuery(GET_GAME_GAMERS, gameId).thenApply(rows -> {
            List<GameHistoryPlayerEntry> players = new ArrayList<>();

            rows.forEach(row -> players.add(
                    new GameHistoryPlayerEntry(
                            row.getRequiredInt("PlayerId"),
                            row.getRequiredInt("Winner") == 1)
            ));

            return players;
        });
    }
}