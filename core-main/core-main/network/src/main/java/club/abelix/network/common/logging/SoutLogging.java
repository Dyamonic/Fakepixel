package club.abelix.network.common.logging;

import w.impl.Impl;
import w.impl.ImplPriority;

import java.lang.management.ManagementFactory;

/**
 * @author WhileInside
 */
@Impl(types = Logging.class, factory = "<init>", priority = ImplPriority.LOWEST)
public final class SoutLogging extends Logging {

    private final boolean debug;

    public SoutLogging() {
        System.out.println("Use SoutLogging :(");
        boolean debug = false;

        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.contains("jdwp=")) {
                debug = true;
                break;
            }
        }

        this.debug = debug;
    }

    @Override
    public void debug(String message) {
        if (debug) {
            System.out.println(message);
        }
    }

    @Override
    public void info(String message) {
        System.out.println(message);
    }

    @Override
    public void warn(String message) {
        System.out.println(message);
    }

    @Override
    public void error(String message) {
        System.err.println(message);
    }

    @Override
    public void stacktrace(Throwable t, String message) {
        System.err.println(message);

        t.printStackTrace();
    }
}
