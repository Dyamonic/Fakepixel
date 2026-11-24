package club.abelix.core.app;

import club.abelix.commons.chat.ChatPublisherHolder;
import club.abelix.commons.chatfilter.ChatFilterGrpcIntegration;
import club.abelix.commons.command.ICommand;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.tabcomplete.AbstractTabcompleter;
import club.abelix.commons.command.tabcomplete.TabcompleteManager;
import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.data.rank.service.RankUpdateHistoryService;
import club.abelix.commons.data.rank.service.RanksService;
import club.abelix.commons.data.rank.service.UserRanksService;
import club.abelix.commons.data.rank.service.remote.RemoteRankService;
import club.abelix.commons.data.rank.service.remote.RemoteRankUpdateHistoryService;
import club.abelix.commons.data.rank.service.remote.RemoteUserRanksService;
import club.abelix.commons.data.rank.service.remote.grpc.RankServiceGrpcIntegration;
import club.abelix.commons.data.rank.service.remote.grpc.UserRankHistoryGrpcIntegration;
import club.abelix.commons.data.rank.service.remote.grpc.UserRanksGrpcIntegration;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.trust.service.TrustService;
import club.abelix.commons.data.trust.service.remote.RemoteTrustService;
import club.abelix.commons.data.trust.service.remote.grpc.TrustServiceGrpcIntegration;
import club.abelix.commons.gamer.ICommandSender;
import club.abelix.commons.geo.RemoteGeoLocationManager;
import club.abelix.commons.language.Language;
import club.abelix.commons.loki.LokiHttpClient;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.project.Project;
import club.abelix.commons.util.collection.CollectionUtil;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.core.app.api.command.CommandSender;
import club.abelix.core.app.api.command.CoreCommandManager;
import club.abelix.core.app.api.config.Configuration;
import club.abelix.core.app.api.config.ConfigurationProvider;
import club.abelix.core.app.api.config.YamlConfiguration;
import club.abelix.core.app.api.plugin.CorePlugin;
import club.abelix.core.app.api.plugin.PluginManager;
import club.abelix.core.app.api.request.RequestManagerImpl;
import club.abelix.core.app.chat.NatsChatPublisher;
import club.abelix.core.app.command.*;
import club.abelix.core.app.command.condition.CoreCommandConditions;
import club.abelix.core.app.connection.LocalServerConnection;
import club.abelix.core.app.connection.ping.ServerPingService;
import club.abelix.core.app.connection.ping.command.ServerPingCommand;
import club.abelix.core.app.connection.ping.task.ServersHealthCheckTask;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.MasterServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.action.ActionsCommand;
import club.abelix.core.app.data.booster.NetworkBoosterManager;
import club.abelix.core.app.data.friend.FriendCommand;
import club.abelix.core.app.data.friend.FriendListener;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.gamer.GamerPerformanceMetrics;
import club.abelix.core.app.data.group.GroupListener;
import club.abelix.core.app.data.ignorelist.IgnoreCommand;
import club.abelix.core.app.data.ignorelist.IgnoreListListener;
import club.abelix.core.app.data.mapbalancer.listener.GameMapBalancerListener;
import club.abelix.core.app.data.mapbalancer.listener.GameMapBalancerPacketListener;
import club.abelix.core.app.data.message.MessageManager;
import club.abelix.core.app.data.message.command.AppendCommand;
import club.abelix.core.app.data.message.command.MessageCommand;
import club.abelix.core.app.data.message.command.ReplyCommand;
import club.abelix.core.app.data.message.command.SocialSpyCommand;
import club.abelix.core.app.data.nickname.NickHistoryCommand;
import club.abelix.core.app.data.party.command.PartyChatCommand;
import club.abelix.core.app.data.party.command.PartyCommand;
import club.abelix.core.app.data.party.command.PartyListCommand;
import club.abelix.core.app.data.party.listener.PartyListener;
import club.abelix.core.app.data.privategame.PrivateGameListener;
import club.abelix.core.app.data.privategame.PrivateGamePacketListener;
import club.abelix.core.app.data.punishment.PunishmentManager;
import club.abelix.core.app.data.punishment.command.ban.*;
import club.abelix.core.app.data.punishment.command.kick.KickCommand;
import club.abelix.core.app.data.punishment.command.mute.*;
import club.abelix.core.app.data.punishment.listeners.PunishmentListener;
import club.abelix.core.app.data.quest.QuestManager;
import club.abelix.core.app.data.report.ReportPlayerListener;
import club.abelix.core.app.data.staff.StaffChatPacketProcessor;
import club.abelix.core.app.data.staff.StaffNotifierListener;
import club.abelix.core.app.data.stats.StatsWipeManager;
import club.abelix.core.app.data.stream.command.StreamCommand;
import club.abelix.core.app.data.sync.ProxyGamerSyncTask;
import club.abelix.core.app.data.trust.listener.TrustFactorListener;
import club.abelix.core.app.data.tunnel.TunnelManager;
import club.abelix.core.app.data.tunnel.command.TunnelCommand;
import club.abelix.core.app.event.gamer.CommandEvent;
import club.abelix.core.app.joinme.command.JoinMeCommand;
import club.abelix.core.app.joinme.listener.JoinMeListener;
import club.abelix.core.app.joinme.service.JoinMeService;
import club.abelix.core.app.screenshare.command.ScreenShareCommand;
import club.abelix.core.app.screenshare.service.ScreenShareService;
import club.abelix.core.app.util.BuildInfo;
import club.abelix.di.api.v1.ScanInfo;
import club.abelix.di.core.StandardDiFactory;
import club.abelix.network.common.thread.ThreadUncaughtExceptionHandler;
import club.abelix.network.exception.BindException;
import club.abelix.network.metrics.PerformanceMetrics;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import club.abelix.network.online.query.execution.standard.StandardCachedStatusQueryExecutionService;
import club.abelix.network.online.query.execution.standard.StandardInstantStatusQueryExecutionService;
import club.abelix.network.redis.JedisConnector;
import club.abelix.network.session.PlayerRedisSessionService;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.Special;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.netty.channel.epoll.Epoll;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import w.geo.api.GeoLocationManager;
import w.util.Env;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Getter
public final class CoreApplication {

    @Getter
    @Dependency
    private static CoreApplication instance;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final Path currentDirectory = Paths.get(".");
    private final Path pluginsDirectory = currentDirectory.resolve("plugins");
    private final PluginManager pluginManager;
    private final CoreCommandManager commandManager = new CoreCommandManager();
    private final LocalServerConnection connection;
    private final Map<String, ICommand> commands = CollectionUtil.createConcurrentMap();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ProxyGamerSyncTask syncTask;
    private final KubernetesClient kubernetesClient;
    private final JedisConnector jedisConnector;
    private final StatusQueryExecutionService statusQueryExecutionService;
    private final UserRanksService userRanksService;
    private final RanksService ranksService;
    private final TrustService trustService;
    private final ChatFilterGrpcIntegration chatFilterGrpc;
    private final RankUpdateHistoryService rankUpdateHistoryService;
    private final PlayerRedisSessionService sessionService;
    private final LokiHttpClient lokiHttpClient;

    private GeoLocationManager geoLocationManager;
    private Configuration configuration;
    private String token;
    private boolean testMode;

    @Getter
    public Long startTime;

    public CoreApplication() {
        log.info("Starting core application...");
        log.info("Running for {} project", Project.CURRENT);

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
                        return CoreApplication.this.getClass().getClassLoader();
                    }
                }
        );

        diFactory.process(classes);

        reloadConfiguration();

        jedisConnector = new JedisConnector();
        jedisConnector.establish();

        val natsChatPublisher = new NatsChatPublisher(
                Env.getString("NATS_URL", "nats://nats.nats.svc.cluster.local:4222")
        );

        natsChatPublisher.connect();
        ChatPublisherHolder.set(natsChatPublisher);

        val instantOnlineQueryExecutionService = new StandardInstantStatusQueryExecutionService(jedisConnector);
        statusQueryExecutionService = new StandardCachedStatusQueryExecutionService(instantOnlineQueryExecutionService);

        sessionService = new PlayerRedisSessionService(jedisConnector);

        ranksService = new RemoteRankService(new RankServiceGrpcIntegration());
        userRanksService = new RemoteUserRanksService(new UserRanksGrpcIntegration(), ranksService);
        rankUpdateHistoryService = new RemoteRankUpdateHistoryService(new UserRankHistoryGrpcIntegration());
        trustService = new RemoteTrustService(new TrustServiceGrpcIntegration());
        chatFilterGrpc = new ChatFilterGrpcIntegration();

        kubernetesClient = new KubernetesClientBuilder().build();

        lokiHttpClient = new LokiHttpClient(
                Env.getString("LOKI_URL", "http://loki.monitoring.svc.cluster.local:3100"),
                Env.getString("LOKI_TENANT_ID", "1")
        );

        diFactory.getBeanStorageService().store(lokiHttpClient);
        diFactory.getBeanStorageService().store(ranksService);
        diFactory.getBeanStorageService().store(userRanksService);
        diFactory.getBeanStorageService().store(rankUpdateHistoryService);

        instance = this;

        ranksService.load();

        scheduler = Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors() / 2,
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("[Core/FP] Core scheduler pool #%1$d")
                        .setUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler())
                        .build()
        );

        executor = Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors() * 2,
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("[Core/FP] Core executor pool #%1$d")
                        .setUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler())
                        .build()
        );

        try {
            geoLocationManager = new RemoteGeoLocationManager();
        } catch (Exception e) {
            log.error("Failed load maxmind data!", e);
        }

        this.connection = new LocalServerConnection(
                instantOnlineQueryExecutionService,
                this,
                "0.0.0.0",
                Env.getInt("CORE_PORT", 9976),
                Env.getInt("CORE_NETWORK_THREADS", 12)
        );

        pluginManager = new PluginManager(this);

        syncTask = new ProxyGamerSyncTask(sessionService);
        log.info("Core loading completed");
    }

    public boolean isRunning() {
        return running.get();
    }

    public ICommand getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    public Set<String> getCommands() {
        return commands.keySet();
    }

    public void registerCommand(
            @NonNull String name,
            @NonNull ICommand command
    ) {
        commands.put(name.toLowerCase(), command);
    }

    public void unregisterCommand(@NonNull String name) {
        commands.remove(name);
    }

    private String coloredEpoll(boolean used) {
        return used
                ? ChatColor.GREEN + "Using"
                : ChatColor.RED + "Not using";
    }

    private String coloredStartupTime(long time) {
        return (time > 1000 ? time > 2500 ? time > 5000 ? ChatColor.RED : ChatColor.GOLD : ChatColor.YELLOW : ChatColor.GREEN)
                + String.valueOf(time);
    }

    public void launch() {
        log.info("Starting...");
        Language.reloadLanguages();

        PerformanceMetrics.startMetrics();
        GamerPerformanceMetrics.startMetrics();

        getCommandManager().addCondition(CoreCommandConditions.class);

        registerCommandCompleters();
        registerCoreCommands();

        registerCoreListeners();
        registerCoreManagers();

        enablePlugins();

        val pingService = new ServerPingService();
        val task = new ServersHealthCheckTask(ServerManager.IMP, pingService);

        getCommandManager().addCommand(new ServerPingCommand(pingService, ServerManager.IMP));

        scheduler.scheduleAtFixedRate(task, 0, 15, TimeUnit.SECONDS);

        val service = new JoinMeService();
        commandManager.addCommand(new JoinMeCommand(service));
        getPluginManager().registerListener(new JoinMeListener(service));

        if (testMode) {
            log.info("Test mode enabled!");
        }

        log.info("Opening connection...");
        try {
            this.connection.bindSynchronized();
        } catch (BindException e) {
            log.error("An error occurred while opening connection!", e);
            return;
        }

        log.info("Connection opened successfully on address {}", connection.getChannel().localAddress());

        log.info("Epoll usage: {}", coloredEpoll(connection.isEpoll()));
        log.info("Version: {}", ChatColor.AQUA + BuildInfo.getVersion());
        log.info(
                "Author: {}. Branch: {}. Build date: {}", ChatColor.AQUA + BuildInfo.getAuthor(),
                ChatColor.GREEN + BuildInfo.getBranch(), ChatColor.GOLD + BuildInfo.getBuildTime()
        );
        log.info("Startup time {}ms", coloredStartupTime(PerformanceMetrics.getUptime()));

        if (!connection.isEpoll()) {
            log.info("Epoll connection is not used!");
            if (!Epoll.isAvailable()) {
                log.warn("Epoll is not available. Reason: {}", Epoll.unavailabilityCause().getMessage());
            }
        }

        syncTask.start();
        log.info("Sync started!");

        running.set(true);
        log.info("Core starting completed!");
        startTime = System.currentTimeMillis();
    }

    public void shutdown() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        NetworkBoosterManager.IMP.save();

        log.info("Closing network listener {}", connection.getConnectAddress());
        connection.closeConnection();

        log.info("Disconnecting players...");
        Gamer.kickAll();

        log.info("unload and disabling plugins");

        for (CorePlugin corePlugin : pluginManager.getPlugins()) {
            try {
                pluginManager.disablePlugin(corePlugin);
            } catch (Exception ex) {
                log.error("Unable to disable plugin {}. Details: ", corePlugin.getDescription().getName(), ex);
            }
        }

        statusQueryExecutionService.shutdown();
        jedisConnector.shutdown();

        log.info("Closing database connections...");
        Database.closeConnections();

        log.info("Shutting down executors...");
        executor.shutdownNow();
        scheduler.shutdownNow();

        System.exit(0);
    }

    private void enablePlugins() {
        if (!Files.exists(pluginsDirectory)) {
            try {
                Files.createDirectories(pluginsDirectory);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create a 'plugins' directory", e);
            }
        }

        pluginManager.detectPlugins(pluginsDirectory);

        pluginManager.loadPlugins();
        pluginManager.enablePlugins();
    }

    private void registerCoreManagers() {
        PunishmentManager.INSTANCE.loadPunishments().join();

        StatsManager.INSTANCE.loadStatsModes();

        QuestManager.INSTANCE.startQuestGenerator();

        StatsWipeManager.INSTANCE.runTimer();

        RequestManagerImpl.injectManager(this);
    }

    private void registerCoreListeners() {
        pluginManager.registerListener(new PunishmentListener());
        pluginManager.registerListener(new PartyListener());
        pluginManager.registerListener(new GroupListener(userRanksService));
        pluginManager.registerListener(new FriendListener());
        pluginManager.registerListener(new IgnoreListListener());
        pluginManager.registerListener(new ReportPlayerListener());
        pluginManager.registerListener(new StaffChatPacketProcessor());
        pluginManager.registerListener(new GameMapBalancerPacketListener());
        pluginManager.registerListener(new GameMapBalancerListener());
        pluginManager.registerListener(new PrivateGamePacketListener());
        pluginManager.registerListener(new PrivateGameListener());

        if (Project.CURRENT != Project.FAKEPIXEL) {
            pluginManager.registerListener(new TrustFactorListener(trustService));
        }

        if (this.geoLocationManager != null) {
            pluginManager.registerListener(new StaffNotifierListener(this.geoLocationManager));
        }

        pluginManager.registerListener(TunnelManager.INSTANCE);
        pluginManager.registerListener(MessageManager.INSTANCE);

        log.info("Core listeners were registered");
    }

    private void registerCommandCompleters() {
        val completeManager = TabcompleteManager.IMP;

        completeManager.addCompleter(
                "void", new AbstractTabcompleter() {
                    @Override
                    public List<String> complete(
                            ICommandSender sender,
                            String label
                    ) {
                        return Collections.emptyList();
                    }
                }
        );

        completeManager.addCompleter(
                "playerName", new AbstractTabcompleter() {
                    @Override
                    public List<String> complete(
                            ICommandSender sender,
                            String label
                    ) {
                        Gamer senderGamer = sender instanceof Gamer ? (Gamer) sender : null;

                        return any(
                                label, Gamer.getGamers().stream()
                                        .filter(gamer -> senderGamer == null || senderGamer.canSeeVanish() || !gamer.isVanished())
                                        .map(Gamer::getName)
                        );
                    }
                }
        );

        completeManager.addCompleter(
                "serverName", new AbstractTabcompleter() {
                    @Override
                    public List<String> complete(
                            ICommandSender sender,
                            String label
                    ) {
                        return any(
                                label, ServerManager.IMP.getServers().stream()
                                        .filter(server -> (server.getMode() != Special.LIMBO && server.getMode() != Special.LOGIN) && !(server instanceof MasterServer) && server.canJoin())
                                        .map(AbstractServer::getName)
                        );
                    }
                }
        );

        completeManager.addCompleter(
                "miniName", new AbstractTabcompleter() {
                    @Override
                    public List<String> complete(
                            ICommandSender sender,
                            String label
                    ) {
                        return any(label, ServerManager.IMP.findAvailableMinis().stream().map(Mini::name));
                    }
                }
        );

        completeManager.addCompleter(
                "masterName", new AbstractTabcompleter() {
                    @Override
                    public List<String> complete(
                            ICommandSender sender,
                            String label
                    ) {
                        return any(label, Arrays.stream(Master.VALUES).map(Master::name));
                    }
                }
        );

        completeManager.addCompleter(
                "pluginName", new AbstractTabcompleter() {
                    @Override
                    public List<String> complete(
                            ICommandSender sender,
                            String label
                    ) {
                        return any(
                                label, CoreApplication.getInstance()
                                        .getPluginManager()
                                        .getPlugins()
                                        .stream()
                                        .map(plugin -> plugin.getDescription().getName())
                                        .toArray(String[]::new)
                        );
                    }
                }
        );

        completeManager.addCompleter(
                "group", new AbstractTabcompleter() {
                    @Override
                    public List<String> complete(
                            ICommandSender sender,
                            String label
                    ) {
                        return any(label, ranksService.findAllRanks().stream().map(Rank::getName));
                    }
                }
        );
    }

    private void registerCoreCommands() {
        commandManager.addCommand(TimeCommand.class);
        commandManager.addCommand(GiveSuffixCommand.class);
        commandManager.addCommand(StreamCommand.class);
        commandManager.addCommand(new BanCommand());
        commandManager.addCommand(new MassBanCommand());
        commandManager.addCommand(new RegexBan());
        commandManager.addCommand(new TempBanCommand());
        commandManager.addCommand(new UnBanCommand());
        commandManager.addCommand(new BanInfoCommand());
        commandManager.addCommand(new BanByInfoCommand());
        commandManager.addCommand(new BanIdInfoCommand());
        commandManager.addCommand(new BanListCommand());
        commandManager.addCommand(new MuteCommand());
        commandManager.addCommand(new UnMuteCommand());
        commandManager.addCommand(new MuteInfoCommand());
        commandManager.addCommand(new MuteByInfoCommand());
        commandManager.addCommand(new MuteIdInfoCommand());
        commandManager.addCommand(new MuteListCommand());
        commandManager.addCommand(new KickCommand());
        commandManager.addCommand(new BanHistoryCommand());
        commandManager.addCommand(new MuteHistoryCommand());
        commandManager.addCommand(FixBansCommand.class);
        commandManager.addCommand(new ExecuteCommand(this));
        commandManager.addCommand(new AnalyticsCommand());
        commandManager.addCommand(new TunnelCommand());
        commandManager.addCommand(HowMuchIPlayedCommand.class);
        commandManager.addCommand(PingCommand.class);
        commandManager.addCommand(CloseJoinCommand.class);
        commandManager.addCommand(new RankCommand(userRanksService, ranksService, rankUpdateHistoryService));
        commandManager.addCommand(CancelWipeCommand.class);
        commandManager.addCommand(AlertCommand.class);
        commandManager.addCommand(AdminDumpGroupsCommand.class);
        commandManager.addCommand(AdminReset2FACommand.class);
        commandManager.addCommand(AdminResetPasswordCommand.class);
        commandManager.addCommand(FindCommand.class);
        commandManager.addCommand(WhereCommand.class);
        commandManager.addCommand(ProxyCommand.class);
        commandManager.addCommand(ListCommand.class);
        commandManager.addCommand(HubCommand.class);
        commandManager.addCommand(LobbyCommand.class);
        commandManager.addCommand(StatusCommand.class);
        commandManager.addCommand(new OnlineCommand(statusQueryExecutionService));
        commandManager.addCommand(PlayCommand.class);
        commandManager.addCommand(RegenerateNicksCommand.class);
        commandManager.addCommand(SendAllCommand.class);
        commandManager.addCommand(SendFromCommand.class);
        commandManager.addCommand(SendCommand.class);
        commandManager.addCommand(WhoIsCommand.class);
        commandManager.addCommand(GetNameCommand.class);
        commandManager.addCommand(FindAccountsCommand.class);
        commandManager.addCommand(new FetchEvadingPlayersCommand());
        commandManager.addCommand(WipeStatCommand.class);
        commandManager.addCommand(StaffChatToggleCommand.class);
        commandManager.addCommand(StaffChatCommand.class);
        commandManager.addCommand(ExternalPermissionsCommand.class);
        commandManager.addCommand(DonateChatCommand.class);
        commandManager.addCommand(TranslateCommand.class);
        commandManager.addCommand(GotoCommand.class);
        commandManager.addCommand(SendToMeCommand.class);
        commandManager.addCommand(StaffListCommand.class);
        commandManager.addCommand(VanishCommand.class);
        commandManager.addCommand(SetPropertyCommand.class);
        commandManager.addCommand(PartyCommand.class);
        commandManager.addCommand(PartyListCommand.class);
        commandManager.addCommand(FriendCommand.class);
        commandManager.addCommand(PartyChatCommand.class);
        commandManager.addCommand(ReloadLanguagesCommand.class);
        commandManager.addCommand(ServersCommand.class);
        commandManager.addCommand(ServerCommand.class);
        commandManager.addCommand(SocialSpyCommand.class);
        commandManager.addCommand(ReplyCommand.class);
        commandManager.addCommand(AppendCommand.class);
        commandManager.addCommand(MessageCommand.class);
        commandManager.addCommand(ActionsCommand.class);
        commandManager.addCommand(RemoveFromCoreCommand.class);
        commandManager.addCommand(RemoveServerFromCoreCommand.class);
        commandManager.addCommand(IgnoreCommand.class);
        commandManager.addCommand(NickHistoryCommand.class);
        commandManager.addCommand(MetricsCommand.class);
        commandManager.addCommand(GoldManagerCommand.class);
        commandManager.addCommand(MysteryDustManagerCommand.class);
        commandManager.addCommand(CorePluginManagerCommand.class);
        commandManager.addCommand(StrCommand.class);
        commandManager.addCommand(CoreVersionCommand.class);
        commandManager.addCommand(VersionCommand.class);
        commandManager.addCommand(HelpCommand.class);
        commandManager.addCommand(ProxyVersionCommand.class);
        commandManager.addCommand(SetPrefixCommand.class);
        commandManager.addCommand(GiveCompanionCommand.class);
        commandManager.addCommand(TransferCommand.class);
        commandManager.addCommand(new EvacuateCommand());
        commandManager.addCommand(new RefreshRanksCommand(ranksService, userRanksService));
        commandManager.addCommand(new PermissionCommand(ranksService));
        commandManager.addCommand(new RefreshSkinCommand());
        commandManager.addCommand(new CoreLoggerCommand());
        commandManager.addCommand(new ApiKeyCommand());
        commandManager.addCommand(new UnlinkLicenseCommand());
        commandManager.addCommand(new TrustCommand(trustService));
        commandManager.addCommand(new TestChatCommand(chatFilterGrpc));
        commandManager.addCommand(new ChatLogCommand(chatFilterGrpc));
        commandManager.addCommand(new ForceSkinCommand());
    }

    public List<String> tabComplete(
            @NonNull CommandSender sender,
            @NonNull String line
    ) {
        String[] params;

        try {
            params = line.substring(1).split(" +", -1);
        } catch (ArrayIndexOutOfBoundsException ignore) {
            return null;
        }

        ICommand command = CoreApplication.getInstance().getCommand(params[0]);

        if (command == null) {
            return null;
        }

        List<String> suggests;
        try {
            suggests = command.tabcomplete(sender, Arrays.copyOfRange(params, 1, params.length));
        } catch (ArrayIndexOutOfBoundsException ignore) {
            return null;
        }

        if (suggests == null) {
            suggests = Collections.emptyList();
        }

        return suggests;
    }

    public void executeCommand(
            @NonNull CommandSender sender,
            @NonNull String line
    ) {
        Async.run(() -> _executeCommand(sender, line));
    }

    private void _executeCommand(
            @NonNull CommandSender sender,
            @NonNull String line
    ) {
        if (line.isBlank() || !line.startsWith("/")) {
            return;
        }

        boolean isPlayer = sender instanceof Gamer;

        String[] split = line.substring(1).split("\\s+");
        String label = split[0].toLowerCase();
        String[] args = Arrays.copyOfRange(split, 1, split.length);

        if (isPlayer && pluginManager.callEvent(new CommandEvent((Gamer) sender, label, args, false)).isCancelled()) {
            return;
        }

        ICommand command = getCommand(label);

        if (command == null) {
            sender.sendMessage(ChatColor.RED + "Command not found");
            return;
        }

        command.execute(sender, args);
    }

    public void reloadConfiguration() {
        ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);

        File configFile = new File("config.yml");

        try {
            if (!configFile.exists()) {
                Configuration defaultConfiguration = new Configuration();
                defaultConfiguration.set("Host", "0.0.0.0");
                defaultConfiguration.set("Port", 5000);
                defaultConfiguration.set("Token", "QwErTy");
                defaultConfiguration.set("Test", false);

                //noinspection ResultOfMethodCallIgnored
                configFile.createNewFile();

                provider.save(defaultConfiguration, configFile);
            }

            configuration = provider.load(configFile);
            token = Env.getString("CORE_TOKEN", "milanskyy_gay");
            testMode = configuration.getBoolean("Test", false);
        } catch (IOException e) {
            log.error("Can't load configuration file", e);
        }
    }

    public boolean isTestModeEnabled() {
        return testMode;
    }

    private void initScreenShare() {
        val screenShareService = new ScreenShareService();

        getCommandManager().addCommand(new ScreenShareCommand(screenShareService));
    }
}
