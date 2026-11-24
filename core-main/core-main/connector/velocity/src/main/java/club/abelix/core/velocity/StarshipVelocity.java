package club.abelix.core.velocity;

import club.abelix.commons.data.rank.service.RankUpdateHistoryService;
import club.abelix.commons.data.rank.service.RanksService;
import club.abelix.commons.data.rank.service.UserRanksService;
import club.abelix.commons.data.rank.service.remote.RemoteRankService;
import club.abelix.commons.data.rank.service.remote.RemoteRankUpdateHistoryService;
import club.abelix.commons.data.rank.service.remote.RemoteUserRanksService;
import club.abelix.commons.data.rank.service.remote.grpc.RankServiceGrpcIntegration;
import club.abelix.commons.data.rank.service.remote.grpc.UserRankHistoryGrpcIntegration;
import club.abelix.commons.data.rank.service.remote.grpc.UserRanksGrpcIntegration;
import club.abelix.commons.geo.RemoteGeoLocationManager;
import club.abelix.commons.language.Language;
import club.abelix.commons.loki.LokiHttpClient;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.project.Project;
import club.abelix.commons.util.EnvUtility;
import club.abelix.commons.util.exclusive.ExclusivePeriod;
import club.abelix.core.velocity.alpha.AlphaServerWhitelist;
import club.abelix.core.velocity.alpha.listener.AlphaServerListener;
import club.abelix.core.velocity.auth.AuthManager;
import club.abelix.core.velocity.auth.command.AuthCommand;
import club.abelix.core.velocity.command.CommandManager;
import club.abelix.core.velocity.command.ProxyCommandConditions;
import club.abelix.core.velocity.config.Configuration;
import club.abelix.core.velocity.config.ConfigurationProvider;
import club.abelix.core.velocity.config.YamlConfiguration;
import club.abelix.core.velocity.connection.Connection;
import club.abelix.core.velocity.connection.LocalClientConnection;
import club.abelix.core.velocity.filter.ChatFilterManager;
import club.abelix.core.velocity.hooks.HooksInitializer;
import club.abelix.core.velocity.motd.MotdManager;
import club.abelix.core.velocity.listener.TabCompleteListener;
import club.abelix.core.velocity.listener.player.PlayerChatListener;
import club.abelix.core.velocity.listener.player.PlayerConnectionListener;
import club.abelix.core.velocity.listener.player.PlayerKickListener;
import club.abelix.core.velocity.listener.player.PlayerServerListener;
import club.abelix.core.velocity.listener.player.PlayerUUIDChangeListener;
import club.abelix.core.velocity.listener.player.feature.LinksFeature;
import club.abelix.core.velocity.listener.player.permission.PlayerPermissionListener;
import club.abelix.core.velocity.metrics.KickByReasonMetricsStorage;
import club.abelix.core.velocity.metrics.VelocityMetricsStorage;
import club.abelix.core.velocity.metrics.listener.PingMetricListener;
import club.abelix.core.velocity.metrics.listener.PlayerConnectionMetricListener;
import club.abelix.core.velocity.metrics.listener.PlayerNettyDisconnectMetricListener;
import club.abelix.core.velocity.metrics.task.DisconnectNettyMetricTask;
import club.abelix.core.velocity.metrics.task.JvmHealthMetricTask;
import club.abelix.core.velocity.metrics.task.KickByReasonMetricTask;
import club.abelix.core.velocity.metrics.task.PingMetricTask;
import club.abelix.core.velocity.metrics.task.PlayerByShieldMetricTask;
import club.abelix.core.velocity.metrics.task.PlayerLoginMetricTask;
import club.abelix.core.velocity.metrics.task.PlayerMoveMetricTask;
import club.abelix.core.velocity.metrics.task.ProxyPingMetricTask;
import club.abelix.core.velocity.metrics.task.VelocityGeneralMetricsTask;
import club.abelix.core.velocity.punishment.PunishmentsManager;
import club.abelix.core.velocity.sentry.SentryHook;
import club.abelix.core.velocity.skin.SkinsManager;
import club.abelix.core.velocity.sonar.SonarHooker;
import club.abelix.core.velocity.status.StatusRestoreHandler;
import club.abelix.core.velocity.status.VelocityStatusUpdater;
import club.abelix.core.velocity.task.PlayerRedisSessionRefreshTask;
import club.abelix.core.velocity.unsigned.UnsignedChatListener;
import club.abelix.core.velocity.util.Constants;
import club.abelix.di.api.v1.ScanInfo;
import club.abelix.di.core.StandardDiFactory;
import club.abelix.network.common.logging.Logging;
import club.abelix.core.velocity.chat.NatsChatPublisher;
import club.abelix.network.online.data.Ids;
import club.abelix.network.online.data.ServerKind;
import club.abelix.network.online.info.ServerInfo;
import club.abelix.network.online.info.StandardServerInfo;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import club.abelix.network.online.query.execution.standard.StandardCachedStatusQueryExecutionService;
import club.abelix.network.online.query.execution.standard.StandardInstantStatusQueryExecutionService;
import club.abelix.network.online.service.StatusTrackingService;
import club.abelix.network.online.service.redis.RedisStatusTrackingService;
import club.abelix.network.redis.JedisConnector;
import club.abelix.network.session.PlayerRedisSessionService;
import club.abelix.network.type.mode.Special;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.ServerLink;
import io.prometheus.client.exporter.HTTPServer;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import w.geo.api.GeoLocationManager;
import w.util.Env;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class StarshipVelocity {

    public static final byte[] REDIS_KEY = "players".getBytes();

    @Getter
    private static StarshipVelocity instance;

    @Getter
    private final ProxyServer proxy;

    @Getter
    private final Logger logger;

    @Getter
    private final Path dataFolder;

    //авторизация, обработка того что с ней связано
    @Getter
    private final AuthManager authManager;

    //базы, обработка того что связано с ними
    @Getter
    private final PunishmentsManager punishmentsManager;

    //скины, обработка того что со скинами
    @Getter
    private final SkinsManager skinsManager;

    //фильтр чата
    @Getter
    private final ChatFilterManager chatFilterManager;

    @Getter
    private final CommandManager commandManager;

    @Getter
    private final GeoLocationManager geoLocationManager;

    @Getter
    private final String proxyName;

    private final String coreHost;
    private final int corePort;

    @Getter
    private final JedisConnector jedisConnector;

    @Getter
    private NatsChatPublisher natsChatPublisher;

    @Getter
    private LocalClientConnection connection;

    @Getter
    @Setter
    private Set<String> commands;

    // Вайтлист для альфа сервер
    @Getter
    @Nullable
    private AlphaServerWhitelist alphaServerWhitelist;

    @Getter
    private ExclusivePeriod exclusivePeriod;

    @Getter
    private MotdManager motdManager;

    @Getter
    @Setter
    private Boolean joinable;

    private HTTPServer prometheusHttpServer;

    @Getter
    private final List<ServerLink> serverLinks = new ArrayList<>();

    @Getter
    private final UserRanksService userRanksService;

    @Getter
    private final RanksService ranksService;

    @Getter
    private final RankUpdateHistoryService rankUpdateHistoryService;

    @Getter
    private final PlayerRedisSessionService sessionService;

    @Getter
    private final StatusQueryExecutionService statusQueryExecutionService;

    @Getter
    private final StatusTrackingService statusTrackingService;

    @Getter
    private final ServerInfo serverInfo;

    @Getter
    private final VelocityMetricsStorage velocityMetricsStorage;

    @Getter
    private final KickByReasonMetricsStorage kickByReasonMetricsStorage;

    @Getter
    private final boolean twoFactorEnabled;

    @Getter
    private final LokiHttpClient lokiHttpClient;

    @Inject
    public StarshipVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataFolder) {
        instance = this;
        joinable = false;

        this.proxy = proxy;
        this.logger = logger;
        this.dataFolder = dataFolder;

        this.proxyName = Env.getString("NAME", "proxy");
        this.twoFactorEnabled = Boolean.parseBoolean(Env.getString("2FA_ENABLED", "false"));

        this.coreHost = Env.getString("CORE_HOST", "0.0.0.0");
        this.corePort = Env.getInt("CORE_PORT", 9976);

        val diFactory = new StandardDiFactory();

        diFactory.setup();
        diFactory.getBeanStorageService().store(this);

        val classes = diFactory.scan(
                new ScanInfo() {
                    @Override
                    public @NotNull String[] packageName() {
                        return new String[]{"club.abelix"};
                    }

                    @Override
                    public @NotNull ClassLoader classLoader() {
                        return StarshipVelocity.this.getClass().getClassLoader();
                    }
                }
        );

        diFactory.process(classes);

        lokiHttpClient = new LokiHttpClient(
                Env.getString("LOKI_URL", "http://localhost:3100"),
                Env.getString("LOKI_TENANT_ID", "1")
        );

        diFactory.getBeanStorageService().store(lokiHttpClient);

        jedisConnector = new JedisConnector();
        jedisConnector.establish();

        natsChatPublisher = new NatsChatPublisher();
        natsChatPublisher.connect();

        sessionService = new PlayerRedisSessionService(jedisConnector);
        statusTrackingService = new RedisStatusTrackingService(jedisConnector);
        statusQueryExecutionService = new StandardCachedStatusQueryExecutionService(new StandardInstantStatusQueryExecutionService(jedisConnector));

        val tags = Env.getStringList("TAGS", ",");

        serverInfo = new StandardServerInfo(
                Ids.physicalVelocity(getProxyName().split("-")[2]),
                getProxyName(),
                Special.PROXY.name(),
                ServerKind.PROXY,
                tags,
                1000,
                0,
                0
        );

        jedisConnector.addObserver(new StatusRestoreHandler(serverInfo, statusTrackingService));

        geoLocationManager = new RemoteGeoLocationManager();

        this.velocityMetricsStorage = new VelocityMetricsStorage();
        this.kickByReasonMetricsStorage = new KickByReasonMetricsStorage();

        ranksService = new RemoteRankService(new RankServiceGrpcIntegration());
        userRanksService = new RemoteUserRanksService(new UserRanksGrpcIntegration(), ranksService);
        rankUpdateHistoryService = new RemoteRankUpdateHistoryService(new UserRankHistoryGrpcIntegration());

        diFactory.getBeanStorageService().store(ranksService);
        diFactory.getBeanStorageService().store(userRanksService);
        diFactory.getBeanStorageService().store(rankUpdateHistoryService);

        ranksService.load();

        exclusivePeriod = ExclusivePeriod.getCurrentExclusivePeriod();

        // Нужные вещи для работы
        this.commandManager = new CommandManager(this);
        this.chatFilterManager = new ChatFilterManager(this);
        this.skinsManager = new SkinsManager(this);
        this.punishmentsManager = new PunishmentsManager(this);
        this.authManager = new AuthManager(this, velocityMetricsStorage);

        getCommandManager().addCondition(ProxyCommandConditions.class);
    }

    @Subscribe(async = false)
    @SneakyThrows
    public void onInitialization(ProxyInitializeEvent ignore) {
        Language.reloadLanguages();
        setupMetrics();

        val pluginManager = getProxy().getEventManager();

        proxy.getScheduler()
                .buildTask(this, new PlayerRedisSessionRefreshTask(proxy, sessionService))
                .repeat(30, TimeUnit.SECONDS)
                .delay(30, TimeUnit.SECONDS)
                .schedule();

        pluginManager.register(this, new SentryHook());

        pluginManager.register(this, new PlayerConnectionListener(this, sessionService));
        pluginManager.register(this, new PlayerServerListener(this, velocityMetricsStorage));
        pluginManager.register(this, new PlayerKickListener(kickByReasonMetricsStorage, velocityMetricsStorage));
        pluginManager.register(this, new PlayerChatListener(this));

        if (Project.CURRENT == Project.FAKEPIXEL) {
            pluginManager.register(this, new PlayerUUIDChangeListener());
        }

        skinsManager.initialize();
        authManager.initialize();
        punishmentsManager.initialize();
        chatFilterManager.initialize();

        pluginManager.register(this, new TabCompleteListener(this));
        pluginManager.register(this, new PlayerPermissionListener(this));
        pluginManager.register(this, new UnsignedChatListener(this));

        HooksInitializer.init(proxy);

        initServerLinks();
        initServerMotd();

        if (!serverLinks.isEmpty()) {
            pluginManager.register(this, new LinksFeature(this));
        }

        if (Constants.IS_ALPHA) {
            alphaServerWhitelist = new AlphaServerWhitelist(this);
            alphaServerWhitelist.load();

            pluginManager.register(this, new AlphaServerListener(this));

            Logging.IMP.info("[Alpha] Activate alpha server mode!");
        }

        connection = new LocalClientConnection(this,
                coreHost, corePort, EnvUtility.getEnvIntegerDefault("CORE_NETWORK_THREADS", "4"));

        if (proxy.getPluginManager().isLoaded("sonar")) {
            new SonarHooker().hook(this);
            Logging.IMP.info("Sonar has been successfully hooked!");
        }

        try {
            connection.connectSynchronized();
        } catch (Exception connectException) {
            log.error("[Network] Failed first core connection! Shutdown!", connectException);

            proxy.getScheduler().buildTask(this,
                    () ->
                            proxy.shutdown(
                                    Component.text("Core failed connection!")
                            )
            ).delay(5, TimeUnit.SECONDS).schedule();
        }

        Connection.setConnection(connection);

        statusTrackingService.track(
                serverInfo,
                new VelocityStatusUpdater()
        );

    }

    @Subscribe(async = false, priority = 100)
    @SneakyThrows
    public void onDisabled(ProxyShutdownEvent event) {
        setJoinable(false);

        for (val player : getProxy().getAllPlayers()) {
            sessionService.closeSession(player.getUsername().toLowerCase());
        }

        Logging.IMP.info("Откладываем выключение на 3.5 секунды чтобы всех успело кикнуть");
        try {
            Thread.sleep(3500L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        statusTrackingService.shutdown();
        statusQueryExecutionService.shutdown();
        jedisConnector.shutdown();
        prometheusHttpServer.close();

        Logging.IMP.info("Закрываем подключение к кору...");
        if (connection != null && connection.isConnected()) {
            connection.closeConnection();
        }

        Logging.IMP.info("Закрываем соединение с базами...");
        Database.closeConnections();

        Logging.IMP.info("Пока!");
    }

    private void initServerLinks() {
        Configuration linksConfig = loadConfiguration("links.yml");

        if (linksConfig == null) {
            Logging.IMP.warn("Failed init links!");
            return;
        }

        for (val linkType : linksConfig.getKeys()) {
            serverLinks.add(ServerLink.serverLink(ServerLink.Type.valueOf(linkType),
                    linksConfig.getString(linkType)));
        }

    }

    private void initServerMotd() {
        Configuration motdConfig = loadConfiguration("motd.yml");

        if (motdConfig == null) {
            Logging.IMP.warn("Failed init links!");
            return;
        }

        motdManager = new MotdManager(this, motdConfig);
        motdManager.initialize();
    }

    public void registerCommand(AuthCommand command) {
        getProxy().getCommandManager().register(command.getName(), command, command.getAliases());
    }

    public Configuration loadConfiguration(final String name) {
        File configurationFile = new File(getDataFolder().toFile(), name);

        ConfigurationProvider configurationProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);

        try {
            if (!configurationFile.exists()) {
                return null;
            }

            return configurationProvider.load(configurationFile);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    private void setupMetrics() {
        try {
            prometheusHttpServer = new HTTPServer.Builder()
                    .withDaemonThreads(true)
                    .withInetSocketAddress(new InetSocketAddress(
                            Env.getString("METRICS_HOST", "0.0.0.0"),
                            Env.getInt("METRICS_PORT", 9100)
                    ))
                    .build();
        } catch (IOException e) {
            log.error("Failed to start Prometheus HTTP server for metrics!", e);
            return;
        }

        registerMetricsTasks();
    }

    private void registerMetricsTasks() {
        val pluginManager = getProxy().getEventManager();

        pluginManager.register(this, new PlayerNettyDisconnectMetricListener(velocityMetricsStorage));
        pluginManager.register(this, new PlayerConnectionMetricListener(velocityMetricsStorage));
        pluginManager.register(this, new PingMetricListener(velocityMetricsStorage));

        new DisconnectNettyMetricTask(velocityMetricsStorage).start();
        new KickByReasonMetricTask(kickByReasonMetricsStorage).start();
        new VelocityGeneralMetricsTask(velocityMetricsStorage).start();
        new PlayerLoginMetricTask(velocityMetricsStorage).start();
        new PlayerMoveMetricTask(velocityMetricsStorage).start();
        new ProxyPingMetricTask(velocityMetricsStorage).start();
        new PlayerByShieldMetricTask(Env.getString("GEO_API_URL")).start();
        new JvmHealthMetricTask().start();
        new PingMetricTask().start();
    }

}
