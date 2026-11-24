package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.language.Language;
import club.abelix.core.app.api.command.CoreExecutionSession;
import lombok.val;

@NamedCommand("translate")
public final class TranslateCommand {

    private static String CHAT_TRANSLATE_PROPERTY = "Chat/Translate";

    @Executor
    @Tabcomplete("[serverName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.translate")
    public static void executor(CoreExecutionSession session) throws CommandTranslationException {
        val gamer = session.getGamer();
        val language = Language.getByName(session.getArgument(0));

        switch (language) {
            case Language.RUSSIAN -> {
                gamer.setProperty(CHAT_TRANSLATE_PROPERTY, language.getId());
                gamer.sendLocalizedMessage("chat_translate_russian");
            }
            case Language.ENGLISH -> {
                gamer.setProperty(CHAT_TRANSLATE_PROPERTY, language.getId());
                gamer.sendLocalizedMessage("chat_translate_english");
            }
            default -> gamer.sendLocalizedMessage("chat_translate_invalid_language");
        }
    }

}
