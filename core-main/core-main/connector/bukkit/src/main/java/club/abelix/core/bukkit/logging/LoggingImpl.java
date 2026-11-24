package club.abelix.core.bukkit.logging;

import club.abelix.network.common.logging.Logging;
import org.bukkit.Bukkit;
import w.impl.Impl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author WhileInside
 */
@Impl(types = Logging.class, factory = "<init>")
public final class LoggingImpl extends Logging {

    private final Logger logger;
    private final boolean debug;

    public LoggingImpl() {
        this.logger = Bukkit.getLogger();
        this.debug = Boolean.getBoolean("core.debug");
    }

    @Override
    public void debug(String message) {
        if (debug) {
            info(message);
        }
    }

    @Override
    public void info(String message) {
        this.logger.log(Level.INFO, message);
    }

    @Override
    public void warn(String message) {
        this.logger.log(Level.WARNING, message);
    }

    @Override
    public void error(String message) {
        this.logger.log(Level.SEVERE, message);
    }

    @Override
    public void stacktrace(Throwable t, String message) {
        this.logger.log(Level.SEVERE, message, t);
    }
}
