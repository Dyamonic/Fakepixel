package club.abelix.commons.data.suffix;

import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.suffix.color.SuffixColor;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SuffixSqlHandler {

    // ------------------------------------------------------------- //

    public static final SuffixSqlHandler INSTANCE = new SuffixSqlHandler();

    // ------------------------------------------------------------- //

    // ------------------------------------------------------------- //

    private static final String LOAD_SUFFIXES = "SELECT * FROM `playerdata`.`Suffixes`;";

    private static final String GET_PLAYER_SUFFIXES = "SELECT * FROM `playerdata`.`PlayerSuffixes` WHERE `PlayerId` = ?";

    private static final String ADD_PLAYER_SUFFIX = "INSERT INTO `playerdata`.`PlayerSuffixes` (`PlayerId`, `SuffixId`, `Color`, `Cost`) VALUES (?, ?, ?, ?);";

    private static final String UPDATE_SUFFIX_COLOR = "UPDATE `playerdata`.`PlayerSuffixes` SET `Color` = ? WHERE `PlayerId` = ? AND `SuffixId` = ?;";

    private static final String DELETE_PLAYER_SUFFIX = "DELETE FROM `playerdata`.`PlayerSuffixes` WHERE `PlayerId` = ? AND `SuffixId` = ?;";

    private static final String ADD_SERVER_SUFFIX = "INSERT INTO `playerdata`.`Suffixes` (`Text`, `Color`, `Rarity`, `Cost`) VALUES (?, ?, ?, ?);";

    private static final String DELETE_SERVER_SUFFIX = "DELETE FROM `playerdata`.`Suffixes` WHERE `Id` = ?";


    private final TIntObjectMap<Suffix> suffixMap = new TIntObjectHashMap<>();

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    // ------------------------------------------------------

    public SuffixSqlHandler() {
        loadSuffixes();
    }

    /**
     * Получить список суффиксов на сервере
     *
     * @return - список суффиксов
     */
    public List<Suffix> getSuffixes() {
        List<Suffix> suffixes = new ArrayList<>(suffixMap.valueCollection());
        suffixes.sort(Comparator.comparingInt(Suffix::getId));

        return suffixes;
    }

    /**
     * Получить суффикс по его ID
     *
     * @param suffixId - id суффикса
     * @return - суффикс
     */
    public Suffix getSuffixById(int suffixId) {
        return suffixMap.get(suffixId);
    }

    /**
     * Загрузить список суффиксов
     */
    public void loadSuffixes() {
        suffixMap.clear();

        database.executeQuery(LOAD_SUFFIXES).thenAccept(rows -> {
            for (BufferedRow row : rows) {
                int suffixId = row.getRequiredInt("Id");
                String text = row.getRequiredString("Text");
                int cost = row.getRequiredInt("Cost");
                SuffixColor color = SuffixColor.getById(row.getRequiredInt("Color"));

                if (color == null) {
                    continue;
                }

                Rarity rarity = Rarity.getById(row.getRequiredInt("Rarity"));

                if (rarity == null) {
                    continue;
                }

                suffixMap.put(suffixId, new Suffix(suffixId, text, color, cost, rarity));
            }
        }).join();
    }

    /**
     * Получить суффиксы игрока по его id
     *
     * @param playerId - id игрока
     * @return - список суффиксов
     */
    public CompletableFuture<TIntObjectMap<Suffix>> getPlayerSuffixes(int playerId) {
        return database.executeQuery(GET_PLAYER_SUFFIXES, playerId).thenApply(rows -> {
            TIntObjectMap<Suffix> suffixes = new TIntObjectHashMap<>();

            for (BufferedRow row : rows) {
                int suffixId = row.getRequiredInt("SuffixId");
                int cost = row.getRequiredInt("Cost");
                Suffix base = getSuffixById(suffixId);

                if (base == null) {
                    continue;
                }

                SuffixColor color = SuffixColor.getById(row.getRequiredInt("Color"));

                if (color == null) {
                    color = base.getColor();
                }

                suffixes.put(suffixId, new Suffix(suffixId, base.getText(), color, cost, base.getRarity()));
            }

            return suffixes;
        });
    }

    /**
     * Изменить цвет суффикса для игрока
     *
     * @param playerId - id игрока
     * @param suffixId - id суффикса
     * @param color    - новый цвет
     */
    // SET `Color` = ? WHERE `PlayerId` = ? AND `SuffixId` = ?;
    public void changeSuffixColor(int playerId, int suffixId, @NonNull SuffixColor color) {
        database.executeVoidUpdate(UPDATE_SUFFIX_COLOR, color.getId(), playerId, suffixId);
    }

    /**
     * Добавить суффикс игроку по его id
     *
     * @param playerId - id игрока
     * @param suffix   - добавляемый суффикс
     */
    public void addPlayerSuffix(int playerId, @NonNull Suffix suffix) {
        database.executeVoidUpdate(ADD_PLAYER_SUFFIX, playerId, suffix.getId(), suffix.getColor().getId(), suffix.getCost());
    }

    /**
     * Добавить суффикс в серверный кеш суффиксов
     *
     * @param suffix - суффикс
     */
    public void addServerSuffix(@NonNull Suffix suffix) {
        suffixMap.put(suffix.getId(), suffix);
    }

    /**
     * Добавить новый суффикс на сервер
     *
     * @param text   - текст суффикса
     * @param color  - базовый цвет суффика
     * @param rarity - рарити суффикса
     */
    public CompletableFuture<Suffix> createSuffix(@NonNull String text,
                                                  @NonNull SuffixColor color,
                                                  @NonNull Rarity rarity,
                                                  int cost) {
        return database.executeUpdate(ADD_SERVER_SUFFIX, text, color.getId(), rarity.getId(), cost)
                .thenApply(execution -> {
                    int suffixId = execution.getGeneratedKey();

                    Suffix suffix = new Suffix(suffixId, text, color, cost, rarity);
                    suffixMap.put(suffixId, suffix);

                    return suffix;
                });
    }

    /**
     * Удалить суффикс по id
     *
     * @param suffix - суффикс
     */
    public void deleteSuffix(@NonNull Suffix suffix) {
        suffixMap.remove(suffix.getId());

        database.executeVoidUpdate(DELETE_SERVER_SUFFIX, suffix.getId());
    }

    public void deletePlayerSuffix(int playerId, int suffixId) {
        database.executeVoidUpdate(DELETE_PLAYER_SUFFIX, playerId, suffixId);
    }

}
