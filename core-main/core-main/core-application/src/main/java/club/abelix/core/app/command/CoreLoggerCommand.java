package club.abelix.core.app.command;

import ch.qos.logback.classic.Logger;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import lombok.val;
import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory;

@NamedCommand("corelogger")
public class CoreLoggerCommand {

    @Executor
    @WithPermission("core.logger")
    public void execute(CoreExecutionSession session) {
        val newLevel = parseLevel(session.getArgument(0));

        if (newLevel == null) {
            session.sendMessage("Unknown logging level!");
            return;
        }

        val loggerName = session.getArgument(1);

        Logger logger;

        if (loggerName.equalsIgnoreCase("ROOT")) {
            logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        } else {
            logger = (Logger) LoggerFactory.getLogger(loggerName);
        }

        if (logger == null) {
            session.sendMessage("Cannot find logger with given name!");
            return;
        }

        logger.setLevel(newLevel);

        session.sendMessage("Logger changed successfully!");
    }

    private Level parseLevel(String levelName) {
        if (levelName == null) return null;
        return switch (levelName.toUpperCase()) {
            case "OFF" -> Level.OFF;
            case "ERROR" -> Level.ERROR;
            case "WARN" -> Level.WARN;
            case "INFO" -> Level.INFO;
            case "DEBUG" -> Level.DEBUG;
            case "TRACE", "ALL" -> Level.TRACE;
            default -> null;
        };
    }
}
