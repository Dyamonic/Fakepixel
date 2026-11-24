package club.abelix.api.base.emote;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;

@NamedCommand("emotes")
@WithAlias("emoji")
@WithAlias("emojis")
@WithAlias("emojihelp")
@IgnoreLogs
public class EmotionsCommand {

    @Executor
    @WithAlias("disableConsole")
    @WithPermission("lobby.emotions")
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        session.sendLocalizedMessage("emotion_list");

        for (EmotionType type : EmotionType.VALUES) {
            session.sendLocalizedMessage("emotion_entry_format",
                    "%emote%", type.getReplaceTo(), "%aliases%", String.join(", ", type.getAliases()));
        }
    }

}
