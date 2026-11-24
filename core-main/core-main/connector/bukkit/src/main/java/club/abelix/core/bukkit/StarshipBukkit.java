package club.abelix.core.bukkit;

import club.abelix.commons.data.rank.service.RanksService;
import club.abelix.commons.data.rank.service.UserRanksService;
import club.abelix.commons.data.rank.service.remote.RemoteRankService;
import club.abelix.commons.data.rank.service.remote.RemoteUserRanksService;
import club.abelix.commons.data.rank.service.remote.grpc.RankServiceGrpcIntegration;
import club.abelix.commons.data.rank.service.remote.grpc.UserRanksGrpcIntegration;
import club.abelix.commons.language.Language;
import club.abelix.commons.loki.LokiHttpClient;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.util.EnvUtility;
import club.abelix.core.bukkit.connection.LocalClientConnection;
import club.abelix.core.bukkit.online.OnlineRestoreHandler;
import club.abelix.core.bukkit.online.OnlineSetupListener;
import club.abelix.di.api.annotation.ScanEntryPoint;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.di.platform.bukkit.DiBukkitPluginListener;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.exception.ConnectException;
import club.abelix.network.online.data.Ids;
import club.abelix.network.online.data.ServerKind;
import club.abelix.network.online.info.StandardServerInfo;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import club.abelix.network.online.query.execution.standard.StandardCachedStatusQueryExecutionService;
import club.abelix.network.online.query.execution.standard.StandardInstantStatusQueryExecutionService;
import club.abelix.network.online.service.StatusTrackingService;
import club.abelix.network.online.service.redis.RedisStatusTrackingService;
import club.abelix.network.packet.play.client.CSetJoinableState;
import club.abelix.network.redis.JedisConnector;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.ServerType;
import club.abelix.network.util.ModeUtil;
import io.netty.channel.epoll.Epoll;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import w.util.Env;

import java.util.Locale;

@Slf4j
@Getter
@ScanEntryPoint
public final class StarshipBukkit extends JavaPlugin implements DiBukkitPluginListener {

    @Getter
    private static StarshipBukkit instance;

    private final ServerType serverType;
    private final String coreHost;
    private final int corePort;
    private final JedisConnector jedisConnector;

    private LocalClientConnection connection;
    private UserRanksService userRanksService;
    private RanksService ranksService;
    private StatusTrackingService trackingService;
    private StatusQueryExecutionService statusQueryExecutionService;
    private StandardServerInfo serverInfo;

    public StarshipBukkit() {
        instance = this;
        ServerMode serverMode;

        String serverModeName = getServerModeName();

        if (serverModeName.isEmpty()) {
            log.error("Server mode is not defined! Please set SKYBLOCK_MODE or NAME.");
            Bukkit.shutdown();
        }

        serverMode = ModeUtil.getServerModeByName(serverModeName);

        this.serverType = new ServerType(
                serverModeName,
                serverMode
        );

        logServerInfo(serverModeName, serverType);

        jedisConnector = new JedisConnector();

        this.coreHost = Env.getString("CORE_HOST", "0.0.0.0");
        this.corePort = Env.getInt("CORE_PORT", 9976);
    }

    private String getServerModeName() {
        String mode = Env.getString("SKYBLOCK_MODE", "NORMAL").trim();
        String name = Env.getString("NAME", "undefined").toLowerCase();

        if (mode.equalsIgnoreCase("SANDBOX")) {
            return name.replaceFirst("-(?!\\d)", "_");
        }

        return name;
    }

    public static String getVersion() {
        String version = Bukkit.getVersion().split("\\(MC: ")[1];

        return version.replace(")", "");
    }

    public JedisConnector getJedisConnector() {
        return jedisConnector;
    }

    public void logServerInfo(String serverMode, ServerType serverType) {
        log.info("Server information: Mode {}, Type: {}, Version: {}, Host: {}, Port: {}",
                serverMode, serverType, getVersion(), Bukkit.getIp(), Bukkit.getPort());

        log.info("Server status (Uncommon): {}", serverType.isUncommon());
        log.info("Server status (Master): {}", serverType.isMaster());
        log.info("Server status (Lobby): {}", serverType.isLobby());
        log.info("Server status (Special): {}", serverType.isSpecial());
    }

    @Override
    public void onDisable() {
        statusQueryExecutionService.shutdown();
        trackingService.shutdown();
        jedisConnector.shutdown();

        if (connection != null && connection.isConnected()) {
            connection.sendPacket(new CSetJoinableState(false));
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer("Server restarting....");
        }

        log.info("Откладываем выключение на 2.5 секунды чтобы всех успело кикнуть");

        try {
            Thread.sleep(2500L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Logging.IMP.info("Закрываем подключение к кору...");
        if (connection != null && connection.isConnected()) {
            connection.closeConnection();
        }

        Logging.IMP.info("Закрываем соединение с базами...");
        Database.closeConnections();

        Logging.IMP.info("Пока!");
    }

    @Override
    public void onInjected() {
        Language.reloadLanguages();

        connection = new LocalClientConnection(this, serverType.getRegistrationServerName(), coreHost, corePort,
                EnvUtility.getEnvIntegerDefault("CORE_NETWORK_THREADS", "2"));
        log.info("Core connection details. Host: {}. Port: {}", coreHost, corePort);

        jedisConnector.establish();

        if (!connection.isEpoll()) {
            log.warn("Epoll connection is not used!");
            if (!Epoll.isAvailable()) {
                Epoll.unavailabilityCause().printStackTrace();
            }
        } else {
            log.info("Use Epoll!");
        }

        getServer().getScheduler().runTask(this, () -> {
            try {
                log.info("[CORE] start try sync connection!");
                connection.connectSynchronized();
                log.info("[CORE] The connection with the game coordinator is established!");
            } catch (ConnectException e) {
                e.printStackTrace();
                log.error("[CORE] Failed synchronized core connection! Detail: ");
                HeartBeat.runLaterSync(Bukkit::shutdown, 50L);
            }
        });

        setupOnlineTracking();

        ranksService = new RemoteRankService(new RankServiceGrpcIntegration());
        userRanksService = new RemoteUserRanksService(new UserRanksGrpcIntegration(), ranksService);

        ranksService.load();

        val di = DiBukkitPlatform.getFactory().getBeanStorageService();

        di.store(ranksService);
        di.store(userRanksService);

        val lokiHttpClient = new LokiHttpClient(
                Env.getString("LOKI_URL", "http://localhost:3100"),
                Env.getString("LOKI_TENANT_ID", "1")
        );

        di.store(lokiHttpClient);
    }

    private void setupOnlineTracking() {
        val tags = Env.getStringList("TAGS", ",");

        serverInfo = new StandardServerInfo(
                Ids.parsedPhysicalId(serverType.getRegistrationServerName()),
                serverType.getRegistrationServerName(),
                serverType.getMode().getName(),
                ServerKind.PHYSICAL,
                tags,
                Bukkit.getMaxPlayers(),
                0,
                0
        );

        trackingService = new RedisStatusTrackingService(jedisConnector);
        statusQueryExecutionService = new StandardCachedStatusQueryExecutionService(new StandardInstantStatusQueryExecutionService(jedisConnector));

        jedisConnector.addObserver(new OnlineRestoreHandler(serverInfo, trackingService));

        getServer().getPluginManager().registerEvents(
                new OnlineSetupListener(serverInfo, trackingService),
                this
        );
    }

}




