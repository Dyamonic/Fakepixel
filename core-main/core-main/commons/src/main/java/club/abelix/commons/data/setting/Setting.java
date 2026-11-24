package club.abelix.commons.data.setting;

import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

@Getter
public enum Setting {

    /*
     * По умолчанию все настройки = false, поэтому они должны быть инвертированы.
     * (вместо ALLOW_REQUESTS должно быть FORBID_REQUESTS, чтобы по умолчанию реквесты были разрешены)
     */

    /**
     * Если true, запрещает отправлять реквесты в друзья этому игроку
     */
    FORBID_FRIEND_REQUESTS,
    /**
     * Если true, запрещает отправлять реквесты в пати этому игроку
     */
    FORBID_PARTY_REQUESTS,
    /**
     * Если true, запрещает отправлять личные сообщения этому игроку
     */
    FORBID_PRIVATE_MESSAGES,
    /**
     * Если true, скрывает всех игроков жля этого игрока
     */
    HIDE_PLAYERS,
    /**
     * Если true, обычные игроки не смогут смотреть чужой профиль
     */
    FORBID_PROFILE_VIEWING,
    /**
     * Если true, игрока не будет автоматически перемещать в такую же игру по окончанию
     */
    AUTOMATIC_QUEUING,
    /**
     * Если true, сообщение о входе донатера не будет показываться
     * (!warning!) персонал все равно будет видеть его сообщение
     */
    HIDE_JOIN_MESSAGE,
    /**
     * Если true, то в косметики Вы не будете видеть собственного
     * превращения
     */
    HIDE_SELF_COSMETIC_MORPH,
    /**
     * Если true, то в нельзя приглашать на дуэль
     */
    CAN_INVITE_DUEL,
    /**
     * Если true, то донат чат выключен
     */
    DISABLE_DONATE_CHAT,

    FORBID_FRIEND_TELEPORTING,
    /**
     * Если true, то игрок не будет получать уведомления о входе в игру (при привязаном Telegram)
     */
    DISABLE_TELEGRAM_LOGIN_NOTIFICATIONS,
    /**
     * Если true, то у игрока будет запрашивать 2fa при каждом входе
     */
    TFA_CONFIRMATION_EVERY_LOGIN;

    private final int mask = 1 << ordinal();

    public static Setting get(int type) {
        try {
            return values()[type];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    public String getName(@NonNull Language language) {
        return language.getMessage("setting_" + name() + "_name");
    }

    public List<String> getDescription(@NonNull Language language) {
        return language.getMessageList("setting_" + name() + "_description");
    }
}
