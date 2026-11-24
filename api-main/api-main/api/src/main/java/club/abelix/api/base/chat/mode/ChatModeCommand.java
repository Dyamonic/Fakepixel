package club.abelix.api.base.chat.mode;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.language.Language;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NamedCommand("chat")
@WithAlias("chatmode")
@IgnoreLogs
public class ChatModeCommand {

    @Executor
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();
        Language language = gamer.getLanguage();

        List<ChatMode> available = ChatMode.getAvailableFor(gamer);

        Stream<String> availableModeNames = available.stream()
                .map(ChatMode::getPrimaryName);

        if (session.getArgumentCount() == 0) {
            throw new CommandTranslationException("command_chat_usage",
                    "%current%", gamer.getChatModeUnchecked().getLocalizedName(language),
                    "%available%", availableModeNames.collect(Collectors.joining("/")));
        }

        ChatMode mode = ChatMode.getByName(session.getArgument(0));

        if (mode == null || !available.contains(mode)) {
            throw new CommandTranslationException("command_chat_invalid_mode",
                    "%modes%", availableModeNames.collect(Collectors.joining(", ")));
        }

        gamer.setChatMode(mode);
        gamer.sendLocalizedMessage("command_chat_mode_set",
                "%mode%", mode.getLocalizedName(language));
    }

}