package club.abelix.commons.command.execution.help;

import club.abelix.commons.command.CommandParams;
import club.abelix.commons.command.CommandWrapper;
import club.abelix.commons.command.execution.WrappedSubcommandExecutor;
import lombok.NonNull;

public class HelpSubcommandExecutor extends WrappedSubcommandExecutor {

    private static final String[] NAMES = new String[]{"help"};
    private static final int HASH = "help".hashCode();

    public HelpSubcommandExecutor(@NonNull CommandWrapper wrapper) {
        super(
                NAMES, new CommandParams(wrapper, HASH, session -> HelpPrinter.print(wrapper, session), false)
        );
    }

}
