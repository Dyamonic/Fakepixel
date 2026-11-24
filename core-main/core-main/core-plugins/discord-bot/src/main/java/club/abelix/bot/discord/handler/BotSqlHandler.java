package club.abelix.bot.discord.handler;

import club.abelix.bot.discord.user.DiscordBotUser;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NonNull;

import java.util.concurrent.TimeUnit;

/**
 * Репозиторий бота (тут вся инфа о его пользователях)
 */
public class BotSqlHandler {

    public static final BotSqlHandler INSTANCE = new BotSqlHandler();
    /**
     * Запрос на загрузку информации о привязанном аккаунте дискорда по нику
     */
    private static final String GET_DISCORD_ACCOUNT_ID = "SELECT `Id` FROM `DiscordAccounts` WHERE `PlayerId` = ?";
    /**
     * Запрос на получение ника привязанного аккаунта по id в дискорде
     */
    private static final String GET_DISCORD_ACCOUNT_NAME = "SELECT `PlayerId` FROM `DiscordAccounts` WHERE `Id` = ?";
    /**
     * Запрос на привязку дискорда к нику
     */
    private static final String LINK_DISCORD_ACCOUNT = "INSERT INTO `DiscordAccounts` (`Id`, `PlayerId`) VALUES (?, ?);";
    /**
     * Запрос на удаление привязки дискорда
     */
    private static final String UNLINK_DISCORD_ACCOUNT = "DELETE FROM `DiscordAccounts` WHERE `Id` = ?";
    /**
     * Мапа пользователей бота по их ID в Discord
     */
    private final Cache<Long, DiscordBotUser> discordIdCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    /**
     * Мапа пользователей бота по их ID в игре
     */
    private final Cache<Integer, DiscordBotUser> playerIdCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Получить пользователя бота по ID в VK
     *
     * @param discordId - id в discord
     * @return - пользователь бота
     */
    public DiscordBotUser getByDiscordId(final long discordId) {
        discordIdCache.cleanUp();

        DiscordBotUser user = discordIdCache.getIfPresent(discordId);

        if (user == null) {
            user = loadByDiscordId(discordId);

            if (user != null) {
                discordIdCache.put(discordId, user);
            }
        }

        return user;
    }

    /**
     * Получить пользователя бота по ID аккаунта
     *
     * @param playerId - id игрока
     * @return - пользователь бота
     */
    public DiscordBotUser getByPlayerId(final int playerId) {
        playerIdCache.cleanUp();

        DiscordBotUser user = playerIdCache.getIfPresent(playerId);

        if (user == null) {
            user = loadByPlayerId(playerId);

            if (user != null) {
                playerIdCache.put(playerId, user);
            }
        }

        return user;
    }

    // *********************************************** //

    /**
     * Загрузка пользователя бота по ID в Discord
     */
    public DiscordBotUser loadByDiscordId(final long discordId) {
        return database.executeQuery(GET_DISCORD_ACCOUNT_NAME, discordId).thenApply(rows -> {
            if (rows.isEmpty()) {
                return null;
            }

            return new DiscordBotUser(
                    discordId,
                    rows.getFirst().getRequiredInt("PlayerId")
            );
        }).join();
    }

    /**
     * Загрузка пользователя бота по нику
     */
    public DiscordBotUser loadByPlayerId(final int playerId) {
        return database.executeQuery(GET_DISCORD_ACCOUNT_ID, playerId).thenApply(rows -> {
            if (rows.isEmpty()) {
                return null;
            }

            return new DiscordBotUser(
                    rows.getFirst().getRequiredLong("Id"),
                    playerId
            );
        }).join();
    }

    /**
     * Привязать игровой аккаунт к дискорду
     *
     * @param discordId - id в дискорде
     * @param playerId  - id игрока
     */
    public DiscordBotUser linkDiscordAccount(final long discordId, final int playerId) {
        DiscordBotUser user = new DiscordBotUser(discordId, playerId);

        // заносим в кеш
        playerIdCache.put(playerId, user);
        discordIdCache.put(discordId, user);

        // заносим в базу данных
        database.executeVoidUpdate(LINK_DISCORD_ACCOUNT, discordId, playerId);

        return user;
    }

    /**
     * Отвязать аккаунт от Дискорда
     *
     * @param user - пользователь дискорд-бота
     */
    public void unlinkDiscordAccount(@NonNull final DiscordBotUser user) {
        // удаляем из кеша
        playerIdCache.invalidate(user.getPlayerId());
        discordIdCache.invalidate(user.getDiscordId());

        // удаляем из базы
        database.executeVoidUpdate(UNLINK_DISCORD_ACCOUNT, user.getDiscordId());
    }
}

