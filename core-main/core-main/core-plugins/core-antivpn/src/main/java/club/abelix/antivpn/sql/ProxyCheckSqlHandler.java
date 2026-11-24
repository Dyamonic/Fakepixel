package club.abelix.antivpn.sql;

import club.abelix.antivpn.data.ProxyCheckResult;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
public class ProxyCheckSqlHandler {

    public static final ProxyCheckSqlHandler INSTANCE = new ProxyCheckSqlHandler();

    private static final String CHECK_IP = "SELECT * FROM `proxy_check` WHERE `ip` = ?";
    private static final String INSERT_IP = "INSERT INTO `proxy_check` (`ip`, `verdict`, `added`, `asn`) VALUES (?, ?, ?, ?)";
    private static final String DELETE_IP = "DELETE FROM `proxy_check` WHERE `ip` = ?";

    private final MySQL database = new MySQL(DatabaseCredentials.MAIN.withDatabase("antivpn"));

    public void insert(@NonNull ProxyCheckResult result) {
        database.executeVoidUpdate(INSERT_IP, result.ip(), result.verdict() ? 1 : 0, result.added(), result.asn().toLowerCase());
    }

    public void delete(@NonNull String ip) {
        database.executeVoidUpdate(DELETE_IP, ip);
    }

    public CompletableFuture<ProxyCheckResult> check(@NonNull String ip) {
        return database.executeQuery(CHECK_IP, ip).thenApply(bufferedRows -> {
            if (bufferedRows.isEmpty()) return null;

            BufferedRow row = bufferedRows.getFirst();
            return new ProxyCheckResult(
                    row.getRequiredString("ip"),
                    row.getRequiredInt("verdict") == 1,
                    row.getRequiredTimestamp("added"),
                    row.getRequiredString("asn")
            );
        });
    }

}
