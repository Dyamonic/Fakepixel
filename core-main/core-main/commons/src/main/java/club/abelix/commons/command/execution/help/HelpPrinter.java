package club.abelix.commons.command.execution.help;

import club.abelix.commons.command.CommandWrapper;
import club.abelix.commons.command.condition.WrappedCompiledCommandCondition;
import club.abelix.commons.command.execution.ExecutionSession;
import club.abelix.commons.command.execution.WrappedCommandExecutor;
import club.abelix.commons.command.execution.WrappedSubcommandExecutor;
import club.abelix.commons.language.Language;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class HelpPrinter {

    public void print(CommandWrapper wrapper, ExecutionSession session) {
        StringBuilder builder = new StringBuilder();
        builder.append(session.getLanguage().getMessage("command_help"));

        WrappedCommandExecutor fallback = wrapper.hasFallback() ? wrapper.getFallback() : wrapper.getExecutor();
        boolean doPrintFallback = fallback != null;

        for (Map.Entry<String, WrappedSubcommandExecutor> entry : wrapper.getSubcommands().entrySet()) {
            String name = entry.getKey();
            WrappedSubcommandExecutor executor = entry.getValue();

            if (name.equals(executor.getNames()[0].toLowerCase())) {
                print(executor, session, builder);
            }

            if (doPrintFallback && executor.getMethodHash() == fallback.getMethodHash()) {
                doPrintFallback = false;
            }
        }

        if (doPrintFallback) {
            print(fallback, session, builder);
        }
        builder.append("\n");
        builder.append(session.getGamer().getLanguage().getMessage("party_line"));

        session.sendMessage(builder.toString());
    }

    public void print(WrappedCommandExecutor executor, ExecutionSession session, StringBuilder output) {
        if (executor.isWithoutHelp()) {
            return;
        }

        for (WrappedCompiledCommandCondition condition : executor.getConditions()) {
            if (!condition.isShowInHelp()) {
                try {
                    condition.getChecker().execute(session);
                } catch (Exception e) {
                    return;
                }
            }
        }

        Language language = session.getLanguage();

        String localizationKey = executor.getLocalizationKey();

        String usageKey = localizationKey + "_usage";
        String descriptionKey = localizationKey + "_description";

        String description = language.isMessageTranslated(descriptionKey)
                ? language.getMessage(descriptionKey)
                : null;

        String usage = executor.toString();

        if (language.isMessageTranslated(usageKey)) {
            usage += " " + language.getMessage(usageKey);
        }

        String message = language.getMessage("command_help_row", "%usage%", usage, "%description%", description == null
                ? language.getMessage("command_help_no_description")
                : description);

        output.append("\n").append(message);
    }

}
