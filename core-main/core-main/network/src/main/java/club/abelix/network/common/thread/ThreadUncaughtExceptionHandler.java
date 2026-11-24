package club.abelix.network.common.thread;

import club.abelix.network.common.logging.Logging;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: core</p>
 * <p>ThreadExceptionHandler creation on 31.05.2023 at 13:46</p>
 */

public class ThreadUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Logging.IMP.error("[ThreadException] ({}) an error has occurred. Details: ", t.getName());
        e.printStackTrace();
        ;
    }
}