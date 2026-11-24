package club.abelix.commons.data.action;

import club.abelix.commons.mysql.modern.BufferedQuery;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;
import lombok.val;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ActionSqlHandler {

    public static final ActionSqlHandler INSTANCE = new ActionSqlHandler();

    // -------------------------------------

    private static final String INSERT_ACTION = "INSERT IGNORE INTO `AuthActions` (`Name`, `Ip`, `Timestamp`, `Action`, `Vk`) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_IP_ACTIONS = "SELECT * FROM `AuthActions` WHERE `Ip` = ? AND `Timestamp` > ? ORDER BY `Timestamp` ASC";
    private static final String SELECT_NAME_ACTIONS = "SELECT * FROM `AuthActions` WHERE `Name` = ? AND `Timestamp` > ? ORDER BY `Timestamp` ASC";

    // -------------------------------------

    private final MySQL database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    public CompletableFuture<List<Action>> getNameActions(
            final @NonNull String playerName,
            final @NonNull LocalDateTime from
    ) {
        return database.executeQuery(SELECT_NAME_ACTIONS, playerName, from)
                .thenApply(this::readActions);
    }

    public CompletableFuture<List<Action>> getIpActions(
            final @NonNull String ip,
            final @NonNull LocalDateTime from
    ) {
        return database.executeQuery(SELECT_IP_ACTIONS, ip, from)
                .thenApply(this::readActions);
    }

    private List<Action> readActions(final BufferedQuery rows) {
        val actions = new ArrayList<Action>(rows.size());

        for (val row : rows) {
            actions.add(readAction(row));
        }

        return actions;
    }

    private Action readAction(final BufferedRow row) {
        val vk = row.getInt("Vk").orElse(0);

        val name = row.getRequiredString("Name");
        val ip = row.getString("Ip").orElse(null);

        val time = row.getRequiredTimestamp("Timestamp");
        val type = ActionType.getById(row.getRequiredInt("Action"));

        return new Action(name, ip, time, type, vk);
    }

    /**
     * Добавить в логи действие
     *
     * @param attempt - дейстие
     */
    public void addAction(final @NonNull Action attempt) {
        database.executeVoidUpdate(
                INSERT_ACTION,
                attempt.getName(), attempt.getIp(), attempt.getTime(),
                attempt.getType().ordinal(), attempt.hasVk() ? attempt.getVk() : null
        );
    }

}
