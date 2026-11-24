package club.abelix.commons.data.action;

import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ActionType {

    /**
     * Игрок зарегистрировался
     */
    REGISTER(0),

    /**
     * Игрок авторизовался
     */
    LOGIN(1),

    /**
     * Игрок авторизовался по сессии (т.е он ранее был авторизован с данного IP)
     */
    SESSION_LOGIN(2),

    /**
     * Игрок авторизовался по лицензионному аккаунту
     */
    PREMIUM_LOGIN(3),

    /**
     * Игрок не смог авторизоваться
     */
    FAIL_LOGIN(4),

    /**
     * Игрок изменил пароль
     */
    CHANGE_PASS(5),

    /**
     * Игрок вышел из сессии (т.е прописал команду /logout)
     */
    LOG_OUT(6),

    // =====================
    // >     Begin: VK     <
    // =====================
    /**
     * Игрок привязал VK
     */
    ASSIGN_VK(7),

    /**
     * Игрок ввёл неверный код, который пришёл ему в VK
     */
    FAIL_LOGIN_VK(8),

    /**
     * Игрок восстанавливает аккаунт через VK
     */
    CHANGE_PASS_VK(9),

    /**
     * Игрок отвязал VK
     */
    REMOVE_VK(10),

    /**
     * Владелец аккаунта кикнул игрока с сервера через VK
     */
    KICK_VK(11),
    // =====================
    // >      End: VK      <
    // =====================
    /**
     * Игрок изменил пароль
     */
    LOGIN_WEB(12),
    /**
     * Игрок изменил пароль
     */
    CHANGE_PASS_WEB(13);

    private final int id;

    public static ActionType getById(int id) {
        for (ActionType actionType : values()) {
            if (actionType.getId() == id) {
                return actionType;
            }
        }

        return null;
    }

    public String getDescription(@NonNull Language language) {
        return language.getMessage("action_" + name());
    }
}
