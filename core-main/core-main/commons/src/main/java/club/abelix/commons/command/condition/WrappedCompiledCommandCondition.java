package club.abelix.commons.command.condition;

import club.abelix.commons.command.CommandExecutor;
import club.abelix.commons.command.execution.ExecutionSession;
import org.jetbrains.annotations.NotNull;

/**
 * @author WhileInside
 */
public class WrappedCompiledCommandCondition implements Comparable<WrappedCompiledCommandCondition> {

    private final int priority;
    private final String name;
    private final boolean showInHelp;

    private final CommandExecutor<ExecutionSession> checker;

    public WrappedCompiledCommandCondition(int priority, String name, boolean showInHelp, CommandExecutor<ExecutionSession> checker) {
        this.priority = priority;
        this.name = name;
        this.showInHelp = showInHelp;
        this.checker = checker;
    }

    public int getPriority() {
        return priority;
    }

    public String getName() {
        return name;
    }

    public boolean isShowInHelp() {
        return showInHelp;
    }

    public CommandExecutor<ExecutionSession> getChecker() {
        return checker;
    }

    @Override
    public int compareTo(@NotNull WrappedCompiledCommandCondition o) {
        return Integer.compare(priority, o.priority);
    }
}
