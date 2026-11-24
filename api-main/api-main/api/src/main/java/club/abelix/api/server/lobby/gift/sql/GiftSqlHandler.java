package club.abelix.api.server.lobby.gift.sql;

import club.abelix.api.server.lobby.gift.data.Gift;
import club.abelix.api.server.lobby.gift.data.GiftType;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
public class GiftSqlHandler {

    public static final GiftSqlHandler INSTANCE = new GiftSqlHandler();

    private static final String INSERT_QUERY = "INSERT INTO `GiftHistory` (`From`, `To`, `Type`, `GiftedAt`) VALUES (?, ?, ?, ?)";
    private static final String SELECT_BY_SENDER_QUERY = "SELECT * FROM `GiftHistory` WHERE `From` = ?";
    private static final String COUNT_BY_SENDER_QUERY = "SELECT COUNT(*) FROM `GiftHistory` WHERE `From` = ?";

    private final MySQL database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    public CompletableFuture<?> insert(Gift gift) {
        return database.executeVoidUpdate(INSERT_QUERY,
                gift.from(),
                gift.to(),
                gift.type().name(),
                gift.giftedAt()
        );
    }

    public CompletableFuture<Collection<Gift>> selectBySender(int sender) {
        return database.executeQuery(SELECT_BY_SENDER_QUERY, sender).thenApply(bufferedRows ->
            bufferedRows.stream().map(row -> new Gift(
                row.getRequiredInt("From"),
                row.getRequiredInt("To"),
                GiftType.valueOf(row.getRequiredString("Type")),
                row.getRequiredTimestamp("GiftedAt")
            )).toList()
        );
    }

    public CompletableFuture<Integer> countBySender(int sender) {
        return database.executeQuery(COUNT_BY_SENDER_QUERY, sender)
                .thenApply(rows -> rows.getFirst().getRequiredInt("count"));
    }

}
