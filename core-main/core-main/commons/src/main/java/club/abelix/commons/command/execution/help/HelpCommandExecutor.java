package club.abelix.commons.command.execution.help;

import club.abelix.commons.command.CommandParams;
import club.abelix.commons.command.CommandWrapper;
import club.abelix.commons.command.execution.WrappedCommandExecutor;
import lombok.NonNull;

public class HelpCommandExecutor extends WrappedCommandExecutor {
    private static final int HASH = "help".hashCode();

    public HelpCommandExecutor(@NonNull CommandWrapper wrapper) {
        super(new CommandParams(wrapper, HASH, session -> HelpPrinter.print(wrapper, session), false));
    }
}
