package club.abelix.commons.data.bestplayer;

import club.abelix.commons.mysql.modern.BufferedExecution;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;


public class BestPlayerSqlHandler {

    public static final BestPlayerSqlHandler INSTANCE = new BestPlayerSqlHandler();

    private static final String SQL_GET_BEST = "SELECT `Id`, `PlayerId`, `Price`, `Time` FROM `BestPlayers` ORDER BY `Time` DESC LIMIT 1";
    private static final String SQL_ADD_BEST = "INSERT INTO `BestPlayers` (`PlayerId`, `Price`, `Time`) VALUES (?, ?, ?)";

    private static final String SQL_GET_DONATION = "SELECT `Id`, `DonaterId`, `Time` FROM `BestPlayersDonate` WHERE `Id` = ? ORDER BY `Time` DESC;";
    private static final String SQL_ADD_DONATION = "INSERT INTO `BestPlayersDonate` (`Id`, `DonaterId`, `Time`) VALUES (?, ?, ?)";


    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    @Nullable
    public CompletableFuture<BestPlayerEntry> getBestPlayer() {
        return database.executeQuery(SQL_GET_BEST).thenApply(bufferedRows -> {

            BufferedRow rawRecord;
            try {
                rawRecord = bufferedRows.getFirst();
            } catch (NoSuchElementException error) {
                return null;
            }

            BestPlayerEntry entry = new BestPlayerEntry(rawRecord.getRequiredInt("Id"),
                    rawRecord.getRequiredInt("PlayerId"),
                    rawRecord.getRequiredInt("Price"),
                    Timestamp.valueOf(rawRecord.getRequiredTimestamp("Time")).getTime());

            if (!entry.isExpired()) {
                return entry;
            }

            return null;
        });
    }

    public CompletableFuture<BufferedExecution> changeBestPlayer(int gamerId, int price) {
        return database.executeVoidUpdate(SQL_ADD_BEST, gamerId, price, new Timestamp(System.currentTimeMillis()));
    }

    public CompletableFuture<Set<BestPlayerDonationEntry>> getPlayerDonations(int recordId) {
        return database.executeQuery(SQL_GET_DONATION, recordId).thenApply(bufferedRows -> {
            Set<BestPlayerDonationEntry> donations = new HashSet<>();

            bufferedRows.forEach(row -> {
                donations.add(new BestPlayerDonationEntry(
                        row.getRequiredInt("DonaterId"),
                        Timestamp.valueOf(row.getRequiredTimestamp("Time")).getTime()
                ));
            });

            return donations;

        });
    }

    public void addDonation(int recordId, @NonNull BestPlayerDonationEntry donation) {
        database.executeVoidUpdate(SQL_ADD_DONATION, recordId, donation.donaterId(), new Timestamp(donation.time()));
    }
}
