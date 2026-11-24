package club.abelix.core.app.logging;

import club.abelix.network.common.logging.Logging;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import w.impl.Impl;
import w.impl.ImplPriority;

/**
 * @author WhileInside
 */
@Slf4j
@Impl(types = Logging.class, factory = "<init>", priority = ImplPriority.HIGHEST)
@NoArgsConstructor
public final class LoggingImpl extends Logging {

    @Override
    public void debug(String message) {
        log.debug(message);
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
