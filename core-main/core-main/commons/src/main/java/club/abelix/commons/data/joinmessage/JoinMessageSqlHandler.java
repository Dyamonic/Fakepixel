package club.abelix.commons.data.joinmessage;

import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.network.common.logging.Logging;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JoinMessageSqlHandler {

    // ------------------------------------------------------------- //

    public static final JoinMessageSqlHandler INSTANCE = new JoinMessageSqlHandler();

    // ------------------------------------------------------------- //

    // ------------------------------------------------------------- //

    private static final String LOAD_JOIN_MESSAGES
            = "SELECT * FROM `playerdata`.`JoinMessages`;";

    private static final String GET_PLAYER_JOIN_MESSAGES
            = "SELECT * FROM `playerdata`.`PlayerJoinMessages` WHERE `PlayerId` = ?";

    private static final String ADD_PLAYER_JOIN_MESSAGE
            = "INSERT INTO `playerdata`.`PlayerJoinMessages` (`PlayerId`, `MessageId`) VALUES (?, ?);";

    private static final String ADD_SERVER_JOIN_MESSAGE
            = "INSERT INTO `playerdata`.`JoinMessages` (`Name`, `Text`, `Cost`, `FreeElite`, `Custom`) VALUES (?, ?, ?, ?);";

    private static final String DELETE_SERVER_JOIN_MESSAGE
            = "DELETE FROM `playerdata`.`JoinMessages` WHERE `Id` = ?";

    private final TIntObjectMap<JoinMessage> joinMessageMap = new TIntObjectHashMap<>();

    // ------------------------------------------------------

    private final MySQL database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    // ------------------------------------------------------

    public JoinMessageSqlHandler() {
        loadJoinMessages();
    }

    /**
     * Получить список сообщений о входе на сервере
     *
     * @return - список сообщений о входе
     */
    public List<JoinMessage> getJoinMessages() {
        List<JoinMessage> messages = new ArrayList<>(joinMessageMap.valueCollection());
        messages.sort(Comparator.comparingInt(JoinMessage::getId));

        return messages;
    }

    /**
     * Получить сообщение о входе по его ID
     *
     * @param messageId - id сообщения о входе
     * @return - сообщение о входе
     */
    public JoinMessage getJoinMessageById(int messageId) {
        return joinMessageMap.get(messageId);
    }

    /**
     * Загрузить список сообщений о входе
     */
    public void loadJoinMessages() {
        joinMessageMap.clear();

        database.executeQuery(LOAD_JOIN_MESSAGES).thenAccept(rows -> {
            for (BufferedRow row : rows) {
                int messageId = row.getRequiredInt("Id");

                String name = row.getRequiredString("Name");
                String text = row.getRequiredString("Text");
                int cost = row.getRequiredInt("Cost");

                boolean freeForElite = row.getBoolean("FreeElite");
                boolean custom = row.getBoolean("Custom");

                joinMessageMap.put(messageId, new JoinMessage(messageId, name, text, cost, freeForElite, custom));
            }
        }).join();
    }

    /**
     * Получить сообщения о входе игрока по его id
     *
     * @param playerId - id игрока
     * @return - список сообщений о входе
     */
    public CompletableFuture<List<JoinMessage>> getPlayerJoinMessages(int playerId) {
        return database.executeQuery(GET_PLAYER_JOIN_MESSAGES, playerId).thenApply(rows -> {
            List<JoinMessage> messages = new ArrayList<>(rows.size());

            for (BufferedRow row : rows) {
                int messageId = row.getRequiredInt("MessageId");

                JoinMessage message = getJoinMessageById(messageId);

                if (message != null) {
                    messages.add(message);
                }
            }

            return messages;
        });
    }

    /**
     * Добавить сообщение о входе игроку по его id
     *
     * @param playerId - id игрока
     * @param message  - добавляемое сообщение
     */
    public void addPlayerJoinMessage(int playerId, @NonNull JoinMessage message) {
        database.executeVoidUpdate(ADD_PLAYER_JOIN_MESSAGE, playerId, message.getId()).exceptionally(throwable -> {
            if (throwable == null) return null;
            Logging.IMP.error("[JoinMessage] Failed to add player ({}) add message {}", playerId, message);
            throwable.printStackTrace();
            return null;
        });
    }

    /**
     * Добавить сообщение о входе в серверный кеш сообщений о входе
     *
     * @param message - сообщение о входе
     */
    public void addServerJoinMessage(@NonNull JoinMessage message) {
        joinMessageMap.put(message.getId(), message);
    }

    /**
     * Добавить новое сообщение о входе на сервер
     *
     * @param name - названия сообщения о входе
     * @param text - текст сообщения о входе
     *             <p>
     *             Можно также передавать в качестве текста или названия
     *             ключ локализации. Визуально он будет отображаться как
     *             переведённое сообщение.
     */
    public CompletableFuture<JoinMessage> createJoinMessage(@NonNull String name,
                                                            @NonNull String text,
                                                            int cost,
                                                            boolean availableForElite,
                                                            boolean custom) {

        return database.executeUpdate(ADD_SERVER_JOIN_MESSAGE, name, text, availableForElite, custom)
                .thenApply(execution -> {
                    int messageId = execution.getGeneratedKey();

                    JoinMessage message = new JoinMessage(messageId, name, text, cost, availableForElite, custom);
                    joinMessageMap.put(messageId, message);

                    return message;
                });
    }

    /**
     * Удалить сообщение о входе
     *
     * @param joinMessage - сообщение
     */
    public void deleteJoinMessage(@NonNull JoinMessage joinMessage) {
        joinMessageMap.remove(joinMessage.getId());

        database.executeVoidUpdate(DELETE_SERVER_JOIN_MESSAGE, joinMessage.getId());
    }
}
