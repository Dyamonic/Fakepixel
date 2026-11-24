package club.abelix.commons.data.purchase;

import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.network.common.logging.Logging;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import lombok.NonNull;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

public class PurchaseSqlHandler {

    private static final String SELECT_PURCHASES = "SELECT * FROM `playerdata`.`Purchases` WHERE `Id` = ?;";
    private static final String ADD_PURCHASE = "INSERT IGNORE INTO `playerdata`.`Purchases` (`Id`, `Item`, `Date`, `Currency`, `Cost`) VALUES (?, ?, ?, ?, ?);";
    private static final String REMOVE_PURCHASE = "DELETE FROM `playerdata`.`Purchases` WHERE `Id` = ? AND `Item` = ?;";
    private static final String REMOVE_RECENT_PURCHASES = "DELETE FROM `playerdata`.`Purchases` WHERE `Id` = ? AND `Date` >= ? AND `Currency` = ?";
    public static PurchaseSqlHandler INSTANCE = new PurchaseSqlHandler();
    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Получить покупки игрока по его id
     *
     * @param playerId - id игрока
     * @return - покупки игрока
     */
    public CompletableFuture<TIntList> getPurchases(int playerId) {
        return database.executeQuery(SELECT_PURCHASES, playerId).thenApply(rows -> {
            TIntList purchases = new TIntArrayList(rows.size());

            for (BufferedRow row : rows) {
                purchases.add(row.getRequiredInt("Item"));
            }

            return purchases;
        });
    }

    /**
     * Внести информацию о покупке игрока в БД
     *
     * @param playerId - id игрока
     * @param item     - id предмета
     * @param cost     - стоимость предмета
     * @param currency - валюта в которой приобретен предмет
     */
    public void addPurchase(int playerId, int item, int cost, GameCurrency currency) {
        database.executeVoidUpdate(ADD_PURCHASE, playerId, item, LocalDate.now(), currency.getId(), cost).exceptionally(throwable -> {
            if (throwable == null) return null;
            Logging.IMP.error("[Purchases] Failed add purchase for player {}. Item {}", playerId, item);
            throwable.printStackTrace();
            return null;
        });
    }

    /**
     * Удалить информацию о покупке игрока из БД
     *
     * @param playerId - id игрока
     * @param id       - id предмета
     */
    public void removePurchase(int playerId, int id) {
        database.executeVoidUpdate(REMOVE_PURCHASE, playerId, id);
    }

    /**
     * Удалить недавние покупки игрока
     *
     * @param playerId - id игрока
     * @param fromDate - начальная дата поиска
     * @param currency - валюта в которой была совершена покупка
     */
    public void removeRecentPurchases(int playerId,
                                      @NonNull LocalDate fromDate,
                                      @NonNull GameCurrency currency) {

        database.executeVoidUpdate(REMOVE_RECENT_PURCHASES, playerId, fromDate, currency.getId());
    }

}
