package club.abelix.commons.command.condition;

import club.abelix.commons.command.execution.ExecutionSession;

/**
 * @author WhileInside
 */
public interface ConditionChecker {

    void checkCondition(ExecutionSession session, String meta) throws Exception;

}
