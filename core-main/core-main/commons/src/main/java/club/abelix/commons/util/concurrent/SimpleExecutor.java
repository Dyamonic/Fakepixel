package club.abelix.commons.util.concurrent;

import java.util.concurrent.Executor;

/**
 * @author Unidentified Person
 */
public class SimpleExecutor implements Executor {

    public static final SimpleExecutor INSTANCE = new SimpleExecutor();

    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
