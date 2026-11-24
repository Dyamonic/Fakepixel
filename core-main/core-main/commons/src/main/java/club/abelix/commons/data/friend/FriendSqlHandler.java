package club.abelix.commons.data.friend;

import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.commons.util.PermissionUtil;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import lombok.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class FriendSqlHandler {

    public static final FriendSqlHandler INSTANCE = new FriendSqlHandler();
    private static final Pattern LIMIT_PATTERN = Pattern.compile("^friends\\.limit\\.(\\d+)$");
    /**
     * Запрос на получение друзей игрока
     */
    private static final String GET_PLAYER_FRIENDS = "SELECT * FROM `Friends` WHERE `Id` = ? OR `Target_Id` = ?;";

    /**
     * Запрос на добавление друзей игрока
     */
    private static final String ADD_PLAYER_FRIEND = "INSERT IGNORE INTO `Friends` (`Id`, `Target_Id`) VALUES (?, ?);";

    /**
     * Запрос на удаление друзей игрока
     */
    private static final String REMOVE_PLAYER_FRIEND = "DELETE FROM `Friends` WHERE (`Id` = ? AND `Target_Id` = ?) OR (`Target_Id` = ? AND `Id` = ?);";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Получить список id друзей игрока
     *
     * @param playerId - ID игрока
     * @return - список друзей игрока
     */
    public CompletableFuture<TIntSet> getFriends(int playerId) {
        return database.executeQuery(GET_PLAYER_FRIENDS, playerId, playerId)
                .thenApply(rows -> {
                    TIntSet friends = new TIntHashSet(rows.size());

                    for (BufferedRow row : rows) {
                        int firstId = row.getRequiredInt("Id");
                        int secondId = row.getRequiredInt("Target_Id");

                        friends.add(firstId == playerId ? secondId : firstId);
                    }

                    return friends;
                });
    }

    /**
     * Добавить игрока в друзья
     *
     * @param playerId       - id игрока
     * @param targetPlayerId - id друга
     */
    public void addFriend(int playerId, int targetPlayerId) {
        database.executeVoidUpdate(ADD_PLAYER_FRIEND, playerId, targetPlayerId);
    }

    /**
     * Удалить игрока из друзей
     *
     * @param playerId       - id игрока
     * @param targetPlayerId - id друга
     */
    public void removeFriend(int playerId, int targetPlayerId) {
        database.executeVoidUpdate(REMOVE_PLAYER_FRIEND, playerId, targetPlayerId, playerId, targetPlayerId);
    }

    /**
     * Получить лимит друзей у игрока по его группам
     *
     * @param ranks - группы игрока
     * @return - лимит друзей
     */
    public int getFriendLimit(@NonNull UserRanks ranks) {
        return PermissionUtil.extractLimit(ranks, "friends");
    }
}
