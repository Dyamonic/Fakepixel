package club.abelix.commons.data.companion.sql;

import club.abelix.commons.data.companion.CompanionType;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;

import java.util.concurrent.CompletableFuture;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
public class ActiveCompanionSqlHandler {

    public static final ActiveCompanionSqlHandler INSTANCE = new ActiveCompanionSqlHandler();

    private static final String SELECT_ACTIVE_COMPANION = "SELECT `Type` FROM `ActiveCompanions` WHERE PlayerId = ?";
    private static final String INSERT_ACTIVE_COMPANION = "INSERT INTO `ActiveCompanions` (`PlayerId`, `Type`) VALUES (?, ?)";
    private static final String DELETE_ACTIVE_COMPANION = "DELETE FROM `ActiveCompanions` WHERE PlayerId = ?";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    public CompletableFuture<CompanionType> getActiveCompanion(int playerId) {
        return database.executeQuery(SELECT_ACTIVE_COMPANION, playerId).thenApply(rows -> {
            if (rows.isEmpty()) {
                return null;
            }

            return CompanionType.getByName(rows.getFirst().getRequiredString("Type"));
        });
    }

    public void setActiveCompanion(int playerId, CompanionType companionType) {
        database.executeVoidUpdate(INSERT_ACTIVE_COMPANION, playerId, companionType.name());
    }

    public void removeActiveCompanion(int playerId) {
        database.executeVoidUpdate(DELETE_ACTIVE_COMPANION, playerId);
    }

}
