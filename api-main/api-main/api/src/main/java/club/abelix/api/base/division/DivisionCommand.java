package club.abelix.api.base.division;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.division.gui.DivisionGui;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>DivisionCommand creation on 03.04.2024 at 00:17</p>
 */

@NamedCommand("division")
@IgnoreLogs
public class DivisionCommand {

    @Executor
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        DivisionGui.openGui(session.getGamer());
    }
}
