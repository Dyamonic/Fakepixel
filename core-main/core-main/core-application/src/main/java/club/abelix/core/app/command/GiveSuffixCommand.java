package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.suffix.SuffixSqlHandler;
import club.abelix.core.app.api.command.CoreExecutionSession;
import lombok.val;

@NamedCommand("givesuffix")
public final class GiveSuffixCommand {

    @Executor
    @WithPermission("command.give.suffix")
    public static void execute(final CoreExecutionSession session) throws CommandException {
        val suffix = SuffixSqlHandler.INSTANCE.getSuffixById(
                session.getIntArgument(0, "suffix_id")
        );

        val sender = session.getGamer();
        if (suffix == null) {
            sender.sendLocalizedMessage("suffix_command_not_found");
            return;
        }

        val target = session.getGamer(1);

        target.addSuffix(suffix);

        sender.sendMessage("Suffix %s given to player %s".formatted(
                suffix.getDisplaySuffix(),
                target.getName()));

        target.sendLocalizedMessage("suffix_command_target_received",
                "%suffix%", suffix.getDisplaySuffix(),
                "%sender%", sender.getName());
    }
}
