package club.abelix.antivpn.sql;

import club.abelix.antivpn.data.WhitelistedASNResult;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
public class ASNWhitelistSqlHandler {

    public static final ASNWhitelistSqlHandler INSTANCE = new ASNWhitelistSqlHandler();

    private static final String GET_ASN = "SELECT * FROM `asn_whitelist` WHERE `asn` = ?";
    private static final String GET_ALL = "SELECT * FROM `asn_whitelist`";
    private static final String INSERT_ASN = "INSERT INTO `asn_whitelist` (`asn`, `added_by`, `added_at`) VALUES (?, ?, ?)";
    private static final String DELETE_ASN = "DELETE FROM `asn_whitelist` WHERE `asn` = ?";

    private final MySQL database = new MySQL(DatabaseCredentials.MAIN.withDatabase("antivpn"));

    public void insert(@NonNull WhitelistedASNResult entry) {
        database.executeVoidUpdate(INSERT_ASN, entry.asn().toLowerCase(), entry.addedBy(), entry.addedAt());
    }

    public void delete(@NonNull String asn) {
        database.executeVoidUpdate(DELETE_ASN, asn.toLowerCase());
    }

    public CompletableFuture<WhitelistedASNResult> get(@NonNull String asn) {
        return database.executeQuery(GET_ASN, asn.toLowerCase()).thenApply(bufferedRows -> {
            if (bufferedRows.isEmpty()) return null;

            BufferedRow row = bufferedRows.getFirst();
            return new WhitelistedASNResult(
                    row.getRequiredString("asn").toLowerCase(),
                    row.getRequiredString("added_by"),
                    row.getRequiredTimestamp("added_at")
            );
        });
    }

    public CompletableFuture<List<WhitelistedASNResult>> getAll() {
        return database.executeQuery(GET_ALL).thenApply(bufferedRows -> {
            ArrayList<WhitelistedASNResult> results = new ArrayList<>();

            for (BufferedRow row : bufferedRows) {
                results.add(new WhitelistedASNResult(
                        row.getRequiredString("asn"),
                        row.getRequiredString("added_by"),
                        row.getRequiredTimestamp("added_at")
                ));
            }

            return results;
        });
    }

}
