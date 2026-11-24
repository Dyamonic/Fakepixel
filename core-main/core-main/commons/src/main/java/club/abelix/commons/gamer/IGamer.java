package club.abelix.commons.gamer;

import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.time.Cooldown;
import club.abelix.commons.util.time.TimeUtil;

import java.util.concurrent.TimeUnit;

public interface IGamer extends ICommandSender {

    String GLOBAL_TIME_PROPERTY = "GAME_TIME_GLOBAL";

    int getPlayerId();

    String getName();

    String getDisplayName();

    default void sendRawMessage(String message) {
        sendMessage(message);
    }

    UserRanks getRanks();

    Language getLanguage();

    int getProperty(String key);

    void addProperty(String key, int value);

    void setProperty(String key, int value);

    void removeProperty(String key);

    boolean hasPermission(String permission);

    // Cooldowns
    // CT = Cooldown Time
    // CE = Cooldown Expiration

    default Cooldown getCooldown(String cooldown) {
        return new Cooldown(getProperty("CT/" + cooldown), getProperty("CE/" + cooldown));
    }

    default void removeCooldown(String cooldown) {
        removeProperty(cooldown);
    }

    default void setCooldown(String cooldown, long time, TimeUnit unit) {
        int seconds = (int) unit.toSeconds(time);

        setProperty("CE/" + cooldown, TimeUtil.getNow() + seconds);
        setProperty("CT/" + cooldown, seconds);
    }

    void setRanks(UserRanks ranks);

}
