package club.abelix.api.server.base.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.language.Language;

@WithAlias("testlanguage")
@NamedCommand("testlang")
public class TestLangCommand {

    @Executor
    @WithPermission("command.test.lang")
    @WithCondition("minArgs: 2")
    public static void execute(BukkitExecutionSession session) {
        Gamer gamer = session.getGamer();

        String languageKey = session.getArgument(1);

        Language language = gamer.getLanguage();

        gamer.sendMessage("Language: " + language.getLanguageName());

        switch (session.getArgument(0)) {
            case "title": {
                gamer.sendTitle(language.getMessage(languageKey), "");
                break;
            }
            case "subtitle": {
                gamer.sendTitle("", language.getMessage(languageKey));
                break;
            }
            case "actionbar": {
                gamer.sendLocalizedActionBar(languageKey);
                break;
            }
            default: {
                gamer.sendLocalizedMessage(languageKey);
            }
        }
    }
}
