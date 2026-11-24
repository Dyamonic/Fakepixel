package club.abelix.api.server.game.listener.base;

import club.abelix.commons.language.Language;

/**
 * @author Unidentified Person
 */
public enum JoinResult {

    NOT_FOUND, ENDING, IS_FULL, SUCCESS, CANT_SPECTATE, PRIVATE_GAME, DISALLOWED;

    public String getMessage(String server, Language language) {
        return language.getMessage("game_error",
                "%game%", server,
                "%error%", language.getMessage("game_" + name()));
    }

}

