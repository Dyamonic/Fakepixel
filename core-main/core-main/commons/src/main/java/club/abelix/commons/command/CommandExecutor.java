package club.abelix.commons.command;

import club.abelix.commons.command.execution.ExecutionSession;

public interface CommandExecutor<Session extends ExecutionSession> {

    void execute(Session session) throws Exception;

    @SuppressWarnings("unchecked")
    default CommandExecutor<ExecutionSession> unchecked() {
        return session -> execute((Session) session);
    }

}
