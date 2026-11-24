package club.abelix.commons.data.companion.sql;

import club.abelix.commons.data.companion.CompanionRow;
import club.abelix.commons.data.companion.CompanionType;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
public class CompanionSqlHandler {

    public static final CompanionSqlHandler INSTANCE = new CompanionSqlHandler();

    private static final String SELECT_ALL_COMPANIONS = "SELECT * FROM `Companions` WHERE PlayerId = ?";
    private static final String INSERT_COMPANION = "INSERT INTO `Companions` (`PlayerId`, `Type`) VALUES (?, ?)";
    private static final String UPDATE_COMPANION_NAME = "UPDATE `Companions` SET `Name` = ? WHERE `PlayerId` = ? AND `Type` = ?";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    public CompletableFuture<Collection<CompanionRow>> loadCompanions(int playerId) {
        return database.executeQuery(SELECT_ALL_COMPANIONS, playerId).thenApply(bufferedRows -> bufferedRows.stream().map(bufferedRow -> {
            CompanionType type = CompanionType.getByName(bufferedRow.getRequiredString("Type"));
            String name = bufferedRow.getString("Name").orElse(null);

            return new CompanionRow(type, name);
        }).collect(Collectors.toList()));
    }

    public void addCompanion(int playerId, CompanionType type) {
        database.executeVoidUpdate(INSERT_COMPANION, playerId, type.name());
    }

    public void updateCompanionName(int playerId, CompanionType type, String name) {
        database.executeVoidUpdate(UPDATE_COMPANION_NAME, name, playerId, type.name());
    }

}
