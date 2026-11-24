package club.abelix.core.telegram.user;

import club.abelix.commons.language.Language;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.telegram.user.sql.BotUserSqlHandler;
import gnu.trove.map.TObjectIntMap;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@AllArgsConstructor
public class BotUser {

    /**
     * Пустой юзер, используется для тех кто еще не зареган, чтобы не возвращать NULL
     */
    public static final BotUser EMPTY_BOT_USER = new BotUser(-1, new ArrayList<>(), null,
            Language.defaultLanguage(), null);

    @Getter
    private final long telegramId;

    /**
     * Все привязанные к боту аккаунты
     */
    private final List<String> linkedAccounts;

    /**
     * Имя основного аккаунта, используемоего на сервере
     */
    @Getter
    @Setter
    private String primaryAccountName;

    /**
     * Язык игрока
     */
    @Getter
    @Setter
    private Language language;

    /**
     * Проперти аккаунта
     */
    @Getter
    private TObjectIntMap<String> properties;

    /**
     * Добавить новый привязанный аккаунт к Telegram
     *
     * @param playerName - ник игрока
     */
    public void addLinkedAccount(@NonNull String playerName) {
        playerName = playerName.toLowerCase();

        if (!hasLinkedAccount(playerName)) {
            linkedAccounts.add(playerName);
        }
    }

    /**
     * Удалить привязанный к Telegram аккаунт
     *
     * @param playerName - ник игрока
     */
    public void removeLinkedAccount(@NonNull String playerName) {
        if (playerName.equalsIgnoreCase(primaryAccountName)) {
            primaryAccountName = null;
        }

        linkedAccounts.remove(playerName.toLowerCase());
    }

    /**
     * Проверить, привязан ли к данному пользователю Telegram данный аккаунт
     *
     * @param playerName - ник игрока
     * @return - true, если аккаунт привязан
     * false, если нет
     */
    public boolean hasLinkedAccount(@NonNull String playerName) {
        return linkedAccounts.contains(playerName.toLowerCase());
    }

    /**
     * Получить список привязанных аккаунтов к боту
     *
     * @return - список привязанных аккаунтов
     */
    public List<String> getLinkedAccounts() {
        return Collections.unmodifiableList(linkedAccounts);
    }

    /**
     * Проверить, установлен ли у игрока основной аккаунт
     *
     * @return - установлен ли у игрока основной аккаунт
     */
    public boolean hasPrimaryAccount() {
        val hasPrimaryAccount = primaryAccountName != null && hasLinkedAccount(primaryAccountName);

        if (!hasPrimaryAccount && getLinkedAccounts().size() == 1) {
            BotUserSqlHandler.INSTANCE.linkPrimaryAccount(this, getLinkedAccounts().getFirst());

            return true;
        }

        return hasPrimaryAccount;
    }

    /**
     * Получить объект онлайн игрока по нику
     *
     * @return - игрок
     * null, если такого нет на сервере
     */
    public Gamer getPlayer() {
        return getPlayer(null);
    }

    public Gamer getPlayer(Consumer<Gamer> loadConsumer) {
        if (primaryAccountName == null) {
            return null;
        }

        return Gamer.getOfflineGamer(primaryAccountName, offline -> {
            offline.loadLanguage();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadProperties();
            offline.updateDisplayName();

            if (loadConsumer != null) {
                loadConsumer.accept(offline);
            }
        });
    }

    /**
     * Есть ли у игрока проперти
     *
     * @param property - название проперти
     * @return - есть ли
     */
    public boolean hasProperty(@NonNull String property) {
        return properties != null && properties.containsKey(property.toLowerCase());
    }

    /**
     * Получить значение проперти
     *
     * @param property - название проперти
     * @return - значение проперти
     */
    public int getProperty(@NonNull String property) {
        return properties == null ? 0 : properties.get(property.toLowerCase());
    }

    /**
     * Добавить значение к проперти
     *
     * @param property - название проперти
     * @param value    - значение
     */
    public void addProperty(@NonNull String property, int value) {
        if (properties == null) {
            return;
        }

        setProperty(property, getProperty(property) + value);
    }

    /**
     * Установить значение проперти
     *
     * @param property - название проперти
     * @param value    - значение
     */
    public void setProperty(@NonNull String property, int value) {
        if (updateProperty(property, value)) {
            BotUserSqlHandler.INSTANCE.setPropertyValue(telegramId, property, value);
            putProperty(property, value);
        }
    }

    /**
     * Обновить проперти
     *
     * @param property - название проперти
     * @param value    - новое значение
     * @return - обновилось ли значение
     */
    public boolean updateProperty(@NonNull String property, int value) {
        if (properties == null) {
            return false;
        }

        int oldValue = getProperty(property);

        return value != oldValue;
    }

    /**
     * Добавить проперти
     *
     * @param property - название проперти
     * @param value    - значение
     */
    public void putProperty(@NonNull String property, int value) {
        if (properties == null) {
            return;
        }

        properties.put(property.toLowerCase(), value);
    }

    /**
     * Удалить проперти
     *
     * @param property - название проперти
     * @return - удалилось ли
     */
    public boolean dropProperty(@NonNull String property) {
        val propertyName = property.toLowerCase();

        return properties != null && properties.containsKey(propertyName);
    }

    /**
     * Удалить проперти
     *
     * @param property - название проперти
     */
    public void removeProperty(@NonNull String property) {
        if (dropProperty(property)) {
            BotUserSqlHandler.INSTANCE.removeProperty(telegramId, property);
            deleteProperty(property);
        }
    }

    /**
     * Удалить проперти
     *
     * @param property - название проперти
     */
    public void deleteProperty(@NonNull String property) {
        if (properties == null) {
            return;
        }

        properties.remove(property.toLowerCase());
    }

}
