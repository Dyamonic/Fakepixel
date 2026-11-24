package club.abelix.commons.data.session;

import club.abelix.commons.data.network.NetworkSqlHandler;
import club.abelix.commons.mysql.modern.BufferedQuery;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionSqlHandler {

    public static final SessionSqlHandler INSTANCE = new SessionSqlHandler();

    /**
     * Запрос на внесение данных о новой сессии
     */
    private static final String ADD_NEW_SESSION = "INSERT INTO `Sessions` (`Player_Id`, `Start_Time`, `Domain`, `Address`) VALUES (?, ?, ?, ?)";

    private static final String DELETE_OPEN_SESSIONS
            = "DELETE FROM `Sessions` WHERE `Player_Id` = ? AND `End_Time` IS NULL;";

    /**
     * Запрос на обновление даты выхода с сервера в сессии
     */
    private static final String CHANGE_SESSION_ENDING
            = "UPDATE `Sessions` SET `End_Time` = ? WHERE `Id` = ?;";

    /**
     * Запрос на добавление нового сервера в текущей сессии
     */
    private static final String ADD_SESSION_SERVER
            = "INSERT INTO `SessionServers` (`Session_Id`, `Server`, `Joined_Time`, `Left_Time`) VALUES (?, ?, ?, ?);";

    /**
     * Получить список серверов, посещенных игроком за указанную сессию
     */
    private static final String GET_SESSION_SERVERS
            = "SELECT * FROM `SessionServers` WHERE `Session_Id` = ? ORDER BY `Joined_Time` DESC;";

    /**
     * Запрос на получение всех сессий игрока
     */
    private static final String GET_PLAYER_SESSIONS
            = "SELECT * FROM `Sessions` WHERE `Player_Id` = ? AND `End_Time` IS NOT NULL ORDER BY `Start_Time` DESC LIMIT ?;";

    /**
     * Запрос на получение всех недавних сессий по IP адресу
     * Получаем сессии только за последние 3 дня (самая актуальная инфа)
     */
    private static final String GET_IP_SESSIONS
            = "SELECT `Player_Id` FROM `Sessions` WHERE `Address` = ? AND `Start_Time` >= ?;";

    /**
     * Запрос на получение всех сессий игрока
     */
    private static final String GET_PLAYER_SESSIONS_FROM_TIME
            = "SELECT * FROM `Sessions` WHERE `Player_Id` = ? AND `Start_Time` > ? AND `End_Time` IS NOT NULL ORDER BY `Start_Time` DESC;";

    /**
     * Запрос на получение сессий игроков по опр. группе за опр. срок
     */
    private static final String GET_SESSIONS_FROM_GROUP
            = "SELECT * FROM `Sessions` INNER JOIN `Groups` WHERE `Groups`.`Group_Id` %s ? AND `Groups`.`Id` = `Sessions`.`Player_Id` AND `Sessions`.`End_Time` IS NOT NULL AND `Sessions`.`Start_Time` >= ?;";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Загрузить последние 10 сессий игрока по его id
     *
     * @param playerId - id игрока
     * @return - сессии игрока
     */
    public CompletableFuture<List<Session>> loadSessions(int playerId) {
        return loadSessions(playerId, 10);
    }

    public CompletableFuture<List<Session>> loadSessions(int playerId, int limit) {
        return database.executeQuery(GET_PLAYER_SESSIONS, playerId, limit)
                .thenCompose(this::process);
    }

    /**
     * Загрузить последние сессии игрока по его id
     *
     * @param playerId  - id игрока
     * @param startTime - время начала
     * @return - сессии игрока
     */
    public CompletableFuture<List<Session>> loadSessions(int playerId, LocalDateTime startTime) {
        return database.executeQuery(GET_PLAYER_SESSIONS_FROM_TIME, playerId, startTime)
                .thenCompose(this::process);
    }

    /**
     * Загрузить все аккаунты по указанному IP адресу
     *
     * @param lastIp - ip адрес
     * @return - список аккаунтов
     */
    public CompletableFuture<List<String>> findAccounts(@NonNull String lastIp) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(3);

        // GROUP BY или DISTINCT пиздец как увеличивает время запроса (0.3c -> 2c)
        // поэтому на уникальность id будем проверять тут - побыстрее будет
        return database.executeQuery(GET_IP_SESSIONS, lastIp, startTime)
                .thenApply(rows -> {
                    TIntSet ids = new TIntHashSet(rows.size());
                    List<String> accounts = new ArrayList<>(rows.size());

                    for (BufferedRow row : rows) {
                        int playerId = row.getRequiredInt("Player_Id");

                        if (ids.contains(playerId)) {
                            continue;
                        }

                        ids.add(playerId);

                        String name = NetworkSqlHandler.INSTANCE.getPlayerName(playerId);

                        if (name != null) {
                            accounts.add(name);
                        }
                    }

                    return accounts;
                });
    }

//    public CompletableFuture<List<Session>> loadSessionsInGroups(@NonNull Group start, @NonNull Group end, LocalDateTime startTime) {
//        if (start.getId() > end.getId()) return CompletableFuture.completedFuture(Collections.emptyList());
//
//        return database.executeQuery(GET_SESSIONS_FROM_GROUPS, start.getId(), end.getId(), startTime)
//                .thenCompose(this::process);
//    }

    private CompletableFuture<List<Session>> process(@NonNull BufferedQuery query) {
        if (query.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        final List<Session> sessions = new ArrayList<>(query.size());

        CompletableFuture<List<Session>> future = new CompletableFuture<>();
        AtomicInteger cnt = new AtomicInteger();

        for (BufferedRow row : query) {
            int playerId = row.getRequiredInt("Player_Id");
            int sessionId = row.getRequiredInt("Id");

            LocalDateTime joinTimestamp = row.getRequiredTimestamp("Start_Time");
            LocalDateTime endTimestamp = row.getTimestamp("End_Time").orElse(null);

            String domain = row.getRequiredString("Domain");
            InetAddress address = row.getRequiredAddress("Address");

            cnt.incrementAndGet();

            loadSessionServers(sessionId).whenComplete((result, cause) -> {
                synchronized (sessions) {
                    sessions.add(new Session(
                            sessionId, playerId, joinTimestamp, endTimestamp, domain, address, result
                    ));
                }

                if (cnt.decrementAndGet() == 0) {
                    future.complete(sessions);
                }
            });
        }

        return future;
    }

    /**
     * Загрузить список серверов, посещенных игроком за данную сессию
     *
     * @param sessionId - id сессии
     * @return - сервера
     */
    private CompletableFuture<List<SessionServer>> loadSessionServers(int sessionId) {
        return database.executeQuery(GET_SESSION_SERVERS, sessionId)
                .thenApply(query -> {
                    List<SessionServer> servers = new ArrayList<>(query.size());

                    for (BufferedRow row : query) {
                        String server = row.getRequiredString("Server");

                        LocalDateTime joinTimestamp = row.getTimestamp("Joined_Time")
                                .orElseThrow(NullPointerException::new);
                        LocalDateTime endTimestamp = row.getTimestamp("Left_Time")
                                .orElseThrow(NullPointerException::new);

                        servers.add(new SessionServer(server, joinTimestamp, endTimestamp));
                    }

                    return servers;
                });
    }

    /**
     * Добавить новую сессию
     *
     * @param playerId  - id игрока
     * @param loginDate - дата начала сессии
     * @param domain    - доменное имя с которого вошли
     * @param address   - ip адрес игрока
     * @return - сессия
     */
    public CompletableFuture<Session> addSession(int playerId, @NotNull LocalDateTime loginDate,
                                                 @NotNull String domain, @NotNull InetAddress address) {
        return database.executeVoidUpdate(DELETE_OPEN_SESSIONS, playerId)
                .thenCompose(ignored -> database.executeUpdate(ADD_NEW_SESSION, playerId, loginDate, domain, address.getHostAddress()))
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                }).thenApply(query -> new Session(
                        query.getGeneratedKey(), playerId, loginDate, null,
                        domain, address, new ArrayList<>()
                ));
    }

    /**
     * Добавить сервер, посещенный за указанную сессию
     *
     * @param sessionId - id сессии
     * @param server    - сервер
     */
    public void addSessionServer(int sessionId, @NonNull SessionServer server) {
        database.executeVoidUpdate(
                ADD_SESSION_SERVER,
                sessionId,
                server.getName(),
                server.getJoinedTimestamp(),
                server.getEndTimestamp()
        );
    }

    /**
     * Обновить время завершения сессии по ее id
     *
     * @param sessionId - id сессии
     * @param timestamp - время завершения
     */
    public void changeSessionEndTime(int sessionId, LocalDateTime timestamp) {
        database.executeVoidUpdate(CHANGE_SESSION_ENDING, timestamp, sessionId);
    }
}
