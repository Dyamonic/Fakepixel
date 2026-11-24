package club.abelix.commons.data.ignorelist;

import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.concurrent.CompletableFuture;

public class IgnoreListSqlHandler {

    // ----------------------------------------------------------------

    public static final IgnoreListSqlHandler INSTANCE = new IgnoreListSqlHandler();

    // ----------------------------------------------------------------

    private static final String GET_PLAYER_IGNORE_LIST = "SELECT * FROM `IgnoreList` WHERE `PlayerId` = ?";

    private static final String ADD_PLAYER_TO_IGNORED_LIST = "INSERT INTO `IgnoreList` (`PlayerId`, `IgnoredId`) VALUES (?, ?)";
    private static final String REMOVE_PLAYER_FROM_IGNORED_LIST = "DELETE FROM `IgnoreList` WHERE `PlayerId` = ? AND `IgnoredId` = ?";

    // ----------------------------------------------------------------

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Загрузить список игнорируемых игроком игроков
     *
     * @param playerId - id игрока для которого загружаем
     * @return - id игнорируемых игроков
     */
    public CompletableFuture<TIntList> getPlayerIgnoreList(int playerId) {
        return database.executeQuery(GET_PLAYER_IGNORE_LIST, playerId)
                .thenApply(rows -> {
                    TIntList playerIds = new TIntArrayList();

                    for (BufferedRow row : rows) {
                        playerIds.add(row.getRequiredInt("IgnoredId"));
                    }

                    return playerIds;
                });
    }

    /**
     * Добавить игрока в список игнорируемых
     *
     * @param playerId  - игрок которому добавляем
     * @param ignoredId - игрок которого добавляем
     */
    public void addPlayerToIgnoreList(int playerId, int ignoredId) {
        database.executeVoidUpdate(ADD_PLAYER_TO_IGNORED_LIST, playerId, ignoredId);
    }

    /**
     * Удалить игрока из списка игнорируемых
     *
     * @param playerId  - игрок которому удаляем
     * @param ignoredId - игрок которого удаляем
     */
    public void removePlayerFromIgnoreList(int playerId, int ignoredId) {
        database.executeVoidUpdate(REMOVE_PLAYER_FROM_IGNORED_LIST, playerId, ignoredId);
    }

}
