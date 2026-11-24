package club.abelix.api.server.lobby.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.util.GuideUtil;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;

@NamedCommand("theguide")
@IgnoreLogs
public class GuideCommand {

    @Executor
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        GuideUtil.activateBook(session.getGamer());
    }

}