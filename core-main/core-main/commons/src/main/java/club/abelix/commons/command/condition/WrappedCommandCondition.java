package club.abelix.commons.command.condition;

public class WrappedCommandCondition {

    private final int priority;
    private final String name;
    private final boolean showInHelp;

    private final ConditionChecker checker;

    public WrappedCommandCondition(int priority, String name, boolean showInHelp, ConditionChecker checker) {
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

    public ConditionChecker getChecker() {
        return checker;
    }

    public WrappedCompiledCommandCondition compile(String meta) {
        return new WrappedCompiledCommandCondition(priority, name, showInHelp,
                session -> checker.checkCondition(session, meta));
    }

    public String toString() {
        return "WrappedCommandCondition[name=" + name + ", priority=" + priority + "]";
    }

}
