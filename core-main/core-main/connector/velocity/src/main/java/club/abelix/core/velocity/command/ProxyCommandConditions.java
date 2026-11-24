package club.abelix.core.velocity.command;

import club.abelix.commons.command.condition.ConditionProvider;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.core.velocity.gamer.Gamer;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
public class ProxyCommandConditions {

    private ProxyCommandConditions() {
        throw new UnsupportedOperationException();
    }

    @ConditionProvider(value = "onlyAuthorized", priority = -1)
    public static void notBanned(CommandExecutionSession session, String meta) throws CommandTranslationException {
        Gamer gamer = session.getGamer();

        if (!gamer.isLoggedIn()) {
            throw new CommandTranslationException("command_requires_authorization");
        }
    }

}
