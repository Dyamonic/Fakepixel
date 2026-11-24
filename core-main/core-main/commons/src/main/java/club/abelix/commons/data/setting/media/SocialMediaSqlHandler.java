package club.abelix.commons.data.setting.media;

import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SocialMediaSqlHandler {

    public static final SocialMediaSqlHandler INSTANCE = new SocialMediaSqlHandler();

    /**
     * Загрузить ссылки на профили в соц. сетях игрока
     */
    private static final String LOAD_SOCIAL_MEDIA_VALUES = "SELECT `SocialLink`, `Value` FROM `Media` WHERE `Id` = ?;";

    /**
     * Изменить значение на ссылку на соц. сеть у игрока
     */
    private static final String SET_SOCIAL_MEDIA_VALUE = "INSERT INTO `Media` (`Id`, `SocialLink`, `Value`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `Value` = ?";

    /**
     * Поиск по ссылке на указанную соц. сеть игрока (чтобы нельзя было поставить такой же ДС, например)
     */
    private static final String FIND_BY_SOCIAL_MEDIA_VALUE = "SELECT * FROM `Media` WHERE `SocialLink` = ? AND `Value` = ?";

    /**
     * Удалить значение на ссылку на соц. сеть у игрока
     */
    private static final String REMOVE_SOCIAL_MEDIA_VALUE = "DELETE FROM `Media` WHERE `SocialLink` = ? AND `Id` = ?";

    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    /**
     * Загрузить значения ссылок на социальные сети
     *
     * @param playerId - id игрока
     */
    public Map<SocialMediaType, String> loadSocialMediaLinks(final int playerId) {
        return database.executeQuery(LOAD_SOCIAL_MEDIA_VALUES, playerId).thenApply(rows -> {
            val mediaLinks = new HashMap<SocialMediaType, String>();

            for (val row : rows) {
                val mediaType = SocialMediaType
                        .getById(row.getRequiredInt("SocialLink"));

                if (mediaType != null) {
                    mediaLinks.put(mediaType, row.getRequiredString("Value"));
                }
            }

            return mediaLinks;
        }).join();
    }

    /**
     * Изменить ссылку на соц. сеть игрока
     *
     * @param playerId  - id игрока
     * @param mediaType - тип социальной сети
     * @param value     - новое значение
     */
    public void setSocialMediaLinkValue(
            final int playerId,
            final @NonNull SocialMediaType mediaType,
            final @Nullable String value
    ) {
        if (value == null) {
            database.executeVoidUpdate(REMOVE_SOCIAL_MEDIA_VALUE, mediaType.ordinal(), playerId);
        } else {
            database.executeVoidUpdate(SET_SOCIAL_MEDIA_VALUE, playerId, mediaType.ordinal(), value, value);
        }
    }

    @Deprecated
    public void findSocialMediaLink(
            final @NonNull SocialMediaType mediaType,
            final @NonNull String value,
            final @NonNull Consumer<Boolean> resultConsumer
    ) {
        findSocialMediaLink(mediaType, value).thenAccept(resultConsumer);
    }

    /**
     * Поиск игроков на наличие такой же ссылки на соц. сеть
     *
     * @param mediaType - тип социальной сети
     * @param value     - ссылка для проверки
     */
    public CompletableFuture<Boolean> findSocialMediaLink(
            final @NonNull SocialMediaType mediaType,
            final @NonNull String value
    ) {
        return database.executeQuery(FIND_BY_SOCIAL_MEDIA_VALUE, mediaType.ordinal(), value)
                .thenApply(rows -> !rows.isEmpty());
    }

}
