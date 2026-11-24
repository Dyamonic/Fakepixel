package club.abelix.core.plugin.keda;

import club.abelix.core.app.api.plugin.CorePlugin;
import com.sun.net.httpserver.HttpServer;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import w.util.Env;

import java.io.IOException;
import java.net.InetSocketAddress;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KedaCorePlugin extends CorePlugin {

    HttpServer server;

    @Override
    public void onEnable() {
        val serverPort = Env.getInt("KEDA_SERVER_PORT", 9185);

        try {
            server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        } catch (IOException exception) {
            log.error("Keda web server enable error", exception);
            getCore().getPluginManager().disablePlugin(this);
            return;
        }

        server.createContext("/metrics", new KedaWebServerHandler(getCore().getStatusQueryExecutionService()));
        server.setExecutor(null);
        server.start();

        log.info("Keda web server started on port {}", serverPort);
    }

    @Override
    public void onDisable() {
        server.stop(0);
    }
}
