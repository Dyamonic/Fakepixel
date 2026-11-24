package club.abelix.core.velocity.sentry;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import io.sentry.Sentry;
import io.sentry.SentryOptions;
import io.sentry.log4j2.SentryAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.Level;
import w.util.Env;

public class SentryHook {

    @Subscribe(priority = Short.MIN_VALUE)
    public void onInitialization(ProxyInitializeEvent ignore) {
        String dsn = Env.getString("SENTRY_DSN");

        Sentry.init((SentryOptions o) -> {
            o.setDsn(dsn);
            o.setEnvironment(Env.getString("NAMESPACE"));
            o.setServerName(Env.getString("NAME"));
            o.setEnableUncaughtExceptionHandler(true);
            o.setTracesSampleRate(1.0);
        });

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration cfg = ctx.getConfiguration();

        SentryAppender appender = SentryAppender.createAppender(
                "Sentry",
                Level.INFO,
                Level.ERROR,
                Level.ERROR,
                dsn,
                false,
                null,
                null
                );

        appender.start();
        cfg.addAppender(appender);

        LoggerConfig root = cfg.getLoggerConfig(org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME);
        root.addAppender(appender, Level.ERROR, null);
        ctx.updateLoggers();
    }

    @Subscribe(priority = Short.MAX_VALUE)
    public void onShutdown(ProxyShutdownEvent e) {
        Sentry.close();
    }
}
