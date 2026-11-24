package club.abelix.core.velocity.util.logging;

import club.abelix.network.common.logging.Logging;
import lombok.extern.slf4j.Slf4j;
import w.impl.Impl;

/**
 * @author WhileInside
 */
@Impl(types = Logging.class, factory = "<init>")
@Slf4j
public final class LoggingImpl extends Logging {

    private final boolean debug;

    public LoggingImpl() {
        this.debug = Boolean.getBoolean("core.debug");
    }

    @Override
    public void debug(String message) {
        if (debug) {
            log.info(message);
        }
    }

    @Override
    public void info(String message) {
        log.info(message);
    }

    @Override
    public void warn(String message) {
        log.warn(message);
    }

    @Override
    public void error(String message) {
        log.error(message);
    }

    @Override
    public void stacktrace(Throwable t, String message) {
        log.error(message, t);
    }
}
