package club.abelix.commons.data.auth;

import club.abelix.commons.data.session.Session;
import club.abelix.commons.data.session.SessionSqlHandler;
import club.abelix.commons.language.Language;
import lombok.Data;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
public class BaseAuthPlayerData {

    /**
     * Имя
     */
    protected String name;

    /**
     * Язык
     */
    @Setter
    protected Language language;

    /**
     * Пароль
     */
    protected String password;

    /**
     * ID в базе данных
     */
    protected int playerId;

    /**
     * ID в вк
     */
    protected int vkId;

    /**
     * Дата регистрации
     */
    protected long regDate;

    /**
     * IP, с которого зарегистрирован игрок
     */
    protected String regIp;

    /**
     * Telegram данные
     */
    protected long telegramId;

    /**
     * Лицензионный аккаунт
     */
    protected boolean premiumAuth;

    protected boolean loaded;

    @Setter
    private Session lastSession;

    public BaseAuthPlayerData(String name) {
        this.name = name;
    }

    public boolean hasPlayedBefore() {
        return lastSession != null;
    }

    public void load() {
        AuthSqlHandler.INSTANCE.load(this).join();

        List<Session> sessions = SessionSqlHandler.INSTANCE.loadSessions(playerId, 1)
                .join();

        if (!sessions.isEmpty()) {
            lastSession = sessions.getFirst();
        }

        loaded = true;
    }

    public int getPlayerId() {
        if (playerId < 0) {
            // топовый фикс
            return (int) -(System.nanoTime() % 1000000);
        }

        return playerId;
    }

    public boolean isRegistered() {
        return password != null;
    }

    public boolean has2FA() {
        return telegramId > 0;
    }

    public boolean isSessionExpired() {
        if (lastSession.getLeftTimestamp() == null) {
            return true;
        }

        return Duration.between(lastSession.getLeftTimestamp(), LocalDateTime.now()).getSeconds()
                > TimeUnit.DAYS.toSeconds(1);
    }

}
