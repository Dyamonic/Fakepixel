package club.abelix.commons.data.vote;

import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.network.common.logging.Logging;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.NonNull;
import lombok.val;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class VoteSqlHandler {

    // -----------------------------------------------------------------

    public static final VoteSqlHandler INSTANCE = new VoteSqlHandler();

    /**
     * Интервал голосования за серв
     */
    public static final long VOTE_INTERVAL = TimeUnit.DAYS.toMillis(1);

    /**
     * Количество опыта за 1 голос
     */
    public static final int EXPERIENCE_FOR_VOTE = 100;

    /**
     * Количество опыта за комбо-голос
     */
    public static final int EXPERIENCE_FOR_COMBO_VOTE = 2000;

    // -----------------------------------------------------------------

    private static final String LOAD_VOTING_PLATFORMS = "SELECT * FROM `playerdata`.`VotePlatforms`;";

    /**
     * Получить список всех голосов игрока
     */
    private static final String GET_PLAYER_VOTES = "SELECT * FROM `playerdata`.`Votes` WHERE `Player_Id` = ? ORDER BY `Time` ASC;";

    /**
     * Добавить голос игроку
     */
    private static final String ADD_PLAYER_VOTE = "INSERT INTO `playerdata`.`Votes` (`Player_Id`, `Platform_Id`, `Time`) VALUES (?, ?, ?)";

    // -----------------------------------------------------------------

    private final MySQL database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    private final TIntObjectMap<VotePlatform> platformMap = new TIntObjectHashMap<>();
    private final HashMap<String, VotePlatform> platformMapping = new HashMap<>();

    public VoteSqlHandler() {
        loadPlatformList();
    }

    /**
     * Загрузить список платформ для голосования
     * <p>
     * Загрузка синхронная, ибо впервые платформы
     * загружаются при первом использовании этого
     * класса плагином или программой
     */
    public void loadPlatformList() {
        platformMap.clear();

        database.executeQuery(LOAD_VOTING_PLATFORMS).thenAccept(rows -> {
            for (BufferedRow row : rows) {
                val platformId = row.getRequiredInt("Id");
                val priority = row.getRequiredInt("Priority");
                val active = row.getBoolean("Status");

                val identifier = row.getRequiredString("Identifier");
                val name = row.getRequiredString("Name");
                val url = row.getRequiredString("Url");

                val texture = row.getRequiredString("Texture");

                val platform = new VotePlatform(platformId, name, identifier, url, texture, priority, active);

                platformMap.put(platformId, platform);
                platformMapping.put(identifier, platform);
            }

            Logging.IMP.info("Loaded {} voting platforms", platformMap.size());
        }).join();
    }

    /**
     * Получить список всех платформ для голосования
     * отсортированный по приоритету платформы
     *
     * @return - список платформ
     */
    public List<VotePlatform> getVotePlatforms() {
        List<VotePlatform> platforms = new ArrayList<>(platformMap.valueCollection());
        platforms.sort(Comparator.comparingInt(VotePlatform::priority));

        return platforms;
    }

    /**
     * Получить список доступных платформ для голосования
     * отсортированный по приоритету платформы
     *
     * @return - список платформ
     */
    public List<VotePlatform> getActiveVotePlatforms() {
        List<VotePlatform> platforms = new ArrayList<>(platformMap.valueCollection());
        platforms.removeIf(platform -> !platform.active());
        platforms.sort(Comparator.comparingInt(VotePlatform::priority));

        return platforms;
    }
    
    /**
     * Получить платформу для голосования по её id
     *
     * @param platformId - id платформы
     * @return - платформа для голосования
     */
    public VotePlatform getVotePlatform(int platformId) {
        return platformMap.get(platformId);
    }

    public VotePlatform getVotePlatform(String identifier) {
        return platformMapping.get(identifier);
    }

    /**
     * Получить список голосов игрока за все время
     *
     * @param playerId - id игрока
     * @return - список голосов игрока
     */
    public CompletableFuture<PlayerVoteData> getPlayerVotes(int playerId) {
        return database.executeQuery(GET_PLAYER_VOTES, playerId).thenApply(rows -> {
            PlayerVoteData playerVoteData = new PlayerVoteData();

            for (BufferedRow row : rows) {
                VotePlatform platform = getVotePlatform(
                        row.getRequiredInt("Platform_Id")
                );

                if (platform == null) {
                    continue;
                }

                LocalDateTime timestamp = row.getRequiredTimestamp("Time");

                playerVoteData.addVote(platform, timestamp);
            }

            return playerVoteData;
        });
    }

    /**
     * Добавить голос игроку за сервер СИНХРОННО
     *
     * @param playerId - id игрока
     * @param platform - мониторинг
     * @param time     - время голосования
     */
    public void addPlayerVote(int playerId, @NonNull VotePlatform platform, LocalDateTime time) {
        database.executeVoidUpdate(ADD_PLAYER_VOTE, playerId, platform.platformId(), time)
                .join();
    }

}
