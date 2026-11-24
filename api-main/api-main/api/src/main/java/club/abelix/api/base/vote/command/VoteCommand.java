package club.abelix.api.base.vote.command;


import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.vote.gui.VoteGui;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;

@WithAlias("votes")
@WithAlias("voting")
@WithAlias("votifier")
@WithAlias("free")
@WithAlias("hack")
@WithAlias("hackop")
@NamedCommand("vote")
@IgnoreLogs
public class VoteCommand {

    @Executor
    public static void executor(BukkitExecutionSession session) throws Exception {
        if (!CoreUtil.isLobby()) {
            throw new CommandTranslationException("command_available_only_in_lobby");
        }

        new VoteGui(session.getPlayer());
    }

}