package club.abelix.api.base.discord;

import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;

import java.util.concurrent.CompletableFuture;

public class DiscordSyncQueueSqlHandler {

    public static final DiscordSyncQueueSqlHandler INSTANCE = new DiscordSyncQueueSqlHandler();

    private static final String UPSERT_QUERY = """
            INSERT INTO `DiscordSyncQueue` (`PlayerId`, `Reason`)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE
                `Reason` = VALUES(`Reason`),
                `CreatedAt` = CURRENT_TIMESTAMP
            """;

    private final MySQL database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    public CompletableFuture<?> enqueuePlayerSync(int playerId, String reason) {
        return database.executeVoidUpdate(UPSERT_QUERY, playerId, reason);
    }

}
