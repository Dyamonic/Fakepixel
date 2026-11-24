package club.abelix.commons.data.booster.sql;

import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.network.type.booster.NetworkBooster;
import club.abelix.network.type.mode.Master;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

/**
 * @author milansky
 */
@UtilityClass
public final class NetworkBoosterSqlHandler {

    private static final Database DATABASE =
            new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));
    private static final String SELECT_QUERY = "SELECT * FROM `NetworkBoosters`",
            INSERT_QUERY = "INSERT INTO `NetworkBoosters` VALUES(null, ?, ?, ?)",
            DELETE_ALL_QUERY = "DELETE FROM `NetworkBoosters`";

    public CompletableFuture<Collection<NetworkBooster>> loadBoosters() {
        return DATABASE.executeQuery(SELECT_QUERY).thenApply(bufferedRows -> {
            val boosters = new HashSet<NetworkBooster>();

            bufferedRows.forEach(bufferedRow -> {
                val master = Master.getByName(bufferedRow.getRequiredString("Master"));
                val owner = bufferedRow.getRequiredInt("Owner");
                val multiplier = bufferedRow.getRequiredDouble("Multiplier");

                boosters.add(new NetworkBooster(master, owner, multiplier));
            });

            return boosters;
        });
    }

    public CompletableFuture<Void> save(@NonNull Collection<NetworkBooster> boosters) {
        val futures = new HashSet<CompletableFuture<?>>();

        futures.add(DATABASE.executeVoidUpdate(DELETE_ALL_QUERY));

        boosters.forEach(booster ->
                futures.add(DATABASE.executeVoidUpdate(INSERT_QUERY,
                        booster.getOwner(), booster.getMaster(), booster.getMultiplier())));

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

}
