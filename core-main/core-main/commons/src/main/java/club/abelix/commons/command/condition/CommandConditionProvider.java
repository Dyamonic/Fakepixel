package club.abelix.commons.command.condition;

import club.abelix.commons.command.CommandExecutor;
import club.abelix.commons.command.execution.ExecutionSession;
import lombok.experimental.UtilityClass;

/**
 * @author WhileInside
 */
@Deprecated
@UtilityClass
public class CommandConditionProvider {

    private static final CommandConditionManager MANAGER = new CommandConditionManager();

    @Deprecated
    public <T extends ExecutionSession> void addCondition(int priority, String name, CommandExecutor<T> sessionChecker) {
        MANAGER.addCondition(priority, name, sessionChecker);
    }

    @Deprecated
    public <T extends ExecutionSession> void addCondition(int priority, String name, boolean b, CommandExecutor<T> sessionChecker) {
        MANAGER.addCondition(priority, name, b, sessionChecker);
    }

}
