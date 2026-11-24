package club.abelix.api;

import club.abelix.api.base.actionitem.ActionItemListener;
import club.abelix.api.base.addon.PluginAddonService;
import club.abelix.api.base.addon.impl.PluginAddonServiceImpl;
import club.abelix.api.base.antiafk.AntiAfk;
import club.abelix.api.base.bowfix.BowFixListener;
import club.abelix.api.base.bowfix.BowFixBukkitListener;
import club.abelix.api.base.box.animation.listener.AnimationListener;
import club.abelix.api.base.box.animation.service.AnimationService;
import club.abelix.api.base.box.command.BoxControlCommand;
import club.abelix.api.base.box.command.TestBoxRewardChances;
import club.abelix.api.base.box.command.TestBoxRewardSelectorCommand;
import club.abelix.api.base.box.listener.BoxListener;
import club.abelix.api.base.box.reward.service.RewardSelectorService;
import club.abelix.api.base.box.service.BoxOpenService;
import club.abelix.api.base.box.service.BoxesService;
import club.abelix.api.base.box.service.BoxesStorageService;
import club.abelix.api.base.chat.mode.ChatModeCommand;
import club.abelix.api.base.chat.mode.ChatModeListener;
import club.abelix.api.base.emote.EmotionsCommand;
import club.abelix.api.base.emote.EmotionsListener;
import club.abelix.api.base.gamehistory.GameHistoryService;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.listener.JoinMessageListener;
import club.abelix.api.base.gamersession.GamerSessionListener;
import club.abelix.api.base.hotbar_new.HotbarService;
import club.abelix.api.base.joinmessage.JoinMessageCommand;
import club.abelix.api.base.level.listener.LevelListener;
import club.abelix.api.base.mention.PlayerMentionListener;
import club.abelix.api.base.nickname.FakeNicknameListener;
import club.abelix.api.base.playertop.listener.PlayerTopListener;
import club.abelix.api.base.prefixcolor.PrefixColorCommand;
import club.abelix.api.base.prefixcolor.ResetPrefixColorCommand;
import club.abelix.api.base.privategame.PrivateGameCommand;
import club.abelix.api.base.rank.RankRefreshListener;
import club.abelix.api.base.report.command.ReportCommand;
import club.abelix.api.base.report.command.ReportListCommand;
import club.abelix.api.base.request.RequestManagerImpl;
import club.abelix.api.base.roleplay.SitManager;
import club.abelix.api.base.song.command.SongCommand;
import club.abelix.api.base.song.listener.NoteBlockListener;
import club.abelix.api.base.stream.command.StreamsCommand;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.language.LanguageProviderImpl;
import club.abelix.api.base.util.scoreboard.listener.ScoreboardListener;
import club.abelix.api.base.util.sign.SignInput;
import club.abelix.api.base.vanish.VanishListener;
import club.abelix.api.base.version.PlayerVersionProvider;
import club.abelix.api.base.version.ViaVersionProvider;
import club.abelix.api.base.vote.command.VoteCommand;
import club.abelix.api.integration.labymod.listener.LabyPlayerListener;
import club.abelix.api.integration.labymod.service.LabyIntegrationService;
import club.abelix.api.integration.labymod.service.LabyNpcEmotesService;
import club.abelix.api.integration.labymod.task.LabyEconomyUpdateTask;
import club.abelix.api.integration.labymod.task.LabyNpcEmotesTask;
import club.abelix.api.integration.lunarclient.handler.LunarDisabledModsListener;
import club.abelix.api.integration.lunarclient.handler.LunarDiscordRichListener;
import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.provider.LanguageProvider;
import club.abelix.api.nms.v1_12.NmsApi1_12;
import club.abelix.api.nms.v1_16.NmsApi1_16;
import club.abelix.api.nms.v1_8.NmsApi1_8;
import club.abelix.api.tournament.TournamentClient;
import club.abelix.api.tournament.RemoteTournamentClient;
import club.abelix.api.server.base.command.ClonesCommand;
import club.abelix.api.server.base.command.DeveloperModeCommand;
import club.abelix.api.server.base.command.DonateCommand;
import club.abelix.api.server.base.command.EntitiesCommand;
import club.abelix.api.server.base.command.EntityCommand;
import club.abelix.api.server.base.command.FireWorkCommand;
import club.abelix.api.server.base.command.FuckCommand;
import club.abelix.api.server.base.command.GameModeCommand;
import club.abelix.api.server.base.giftban.GiftBanService;
import club.abelix.api.server.base.giftban.command.GiftBanCommand;
import club.abelix.api.server.base.command.ImageCommand;
import club.abelix.api.server.base.command.MemoryCommand;
import club.abelix.api.server.base.command.SettingsCommand;
import club.abelix.api.server.base.command.SlimeMapCommand;
import club.abelix.api.server.base.command.SpeedCommand;
import club.abelix.api.server.base.command.StatsCommand;
import club.abelix.api.server.base.command.TestCoreAPI;
import club.abelix.api.server.base.command.TestCorpseCommand;
import club.abelix.api.server.base.command.TestCustomNameCommand;
import club.abelix.api.server.base.command.TestFunnyPacketCommand;
import club.abelix.api.server.base.command.TestHologramCommand;
import club.abelix.api.server.base.command.TestItemTranslationCommand;
import club.abelix.api.server.base.command.TestLangCommand;
import club.abelix.api.server.base.command.TranslateCommand;
import club.abelix.api.server.base.command.fun.CheatModeCommand;
import club.abelix.api.server.base.command.fun.TntTargetCommand;
import club.abelix.api.server.base.duels.processor.DuelsCoreProcessor;
import club.abelix.api.server.base.entity.FakeEntityTablistTask;
import club.abelix.api.server.base.giftban.listener.GiftBanListener;
import club.abelix.api.server.base.listener.ChatListener;
import club.abelix.api.server.base.listener.CoreListener;
import club.abelix.api.server.base.listener.DevModeListener;
import club.abelix.api.server.base.listener.ExploitDetectListener;
import club.abelix.api.server.base.listener.FriendListener;
import club.abelix.api.server.base.listener.GamerListener;
import club.abelix.api.server.base.listener.GuiListener;
import club.abelix.api.server.base.listener.HotbarListener;
import club.abelix.api.server.base.listener.IgnoreListener;
import club.abelix.api.server.base.listener.LanguageListener;
import club.abelix.api.server.base.listener.LobbyTagListener;
import club.abelix.api.server.base.listener.MetadataListener;
import club.abelix.api.server.base.listener.PartyListener;
import club.abelix.api.server.base.listener.QuestListener;
import club.abelix.api.server.base.listener.SkinListener;
import club.abelix.api.server.base.listener.WorldListener;
import club.abelix.api.server.game.box.listener.RewardListener;
import club.abelix.api.server.game.command.SetGameHpCommand;
import club.abelix.api.server.game.command.StartCommand;
import club.abelix.api.server.game.hotbar.GameHotbar;
import club.abelix.api.server.game.listener.redirect.GamerRedirectToLobbyListener;
import club.abelix.api.server.game.manager.GameStatsManager;
import club.abelix.api.server.lobby.command.AxClientCommand;
import club.abelix.api.server.lobby.command.AxVpnCommand;
import club.abelix.api.server.lobby.command.ExperienceCommand;
import club.abelix.api.server.lobby.command.GiftCommand;
import club.abelix.api.server.lobby.exclusive.newyear.command.TriggerNewYearShow;
import club.abelix.api.server.lobby.mode.npc.ModeNpcService;
import club.abelix.api.server.lobby.mysterybox.item.CaseManager;
import club.abelix.api.server.lobby.mysterybox.listener.MysteryBoxListener;
import club.abelix.api.server.lobby.nick.NickCommand;
import club.abelix.api.server.lobby.profile.command.AwardsCommand;
import club.abelix.api.tournament.service.TournamentService;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.data.vote.VoteSqlHandler;
import club.abelix.commons.language.parser.I18nParserController;
import club.abelix.commons.project.Project;
import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.di.api.annotation.Autowired;
import club.abelix.di.api.annotation.ScanEntryPoint;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.di.platform.bukkit.DiBukkitPluginListener;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.ServerMode;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.utility.MinecraftReflection;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.Epoll;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.SystemPropertyUtil;
import lombok.Getter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import w.util.Env;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Getter
@ScanEntryPoint
public class FalconAPI extends AbstractPlugin implements DiBukkitPluginListener {

    @Getter
    private static FalconAPI instance;

    @Getter
    private static StatsMode statsMode;

    @Getter
    private static NmsApi nmsApi;

    @Getter
    private static PlayerVersionProvider playerVersionProvider;

    private PluginAddonService addonService;
    private RewardSelectorService rewardSelectorService;

    public static final boolean BOXES_ENABLED = true;

    private BoxesService boxesService;
    private BoxesStorageService boxStorageService;
    private BoxOpenService boxOpenService;
    private AnimationService animationService;
    private LabyNpcEmotesService labyNpcEmotesService;
    private ModeNpcService modeNpcService;
    private GameHistoryService gameHistoryService;

    private TournamentClient tournamentClient;
    private TournamentService tournamentService;
    private ManagedChannel ratingChannel;

    @Autowired
    private HotbarService hotbarService;

    @Autowired
    private GameHotbar gameHotbar;

    public static void unregisterCommand(String cmd) {
        nmsApi.getCommandApi().unregisterCommand(cmd);
    }

    @Override
    public void onEnable() {
        instance = this;
        CoreUtil.initializeServerData();
    }

    @Override
    public void onInjected() {
        DiBukkitPlatform.getFactory().getBeanService().processInjection(this);

        if (nmsApi != null) {
            nmsApi.enable();
        }

        modeNpcService = new ModeNpcService(
                this,
                StarshipBukkit.getInstance().getStatusQueryExecutionService()
        );

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getServer().getMessenger().registerOutgoingPluginChannel(this, "axskin:custom");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "axskin:temps");

        getServer().getMessenger().registerIncomingPluginChannel(this, "axskin:update", new SkinListener(this));

        if (SystemPropertyUtil.getBoolean("service.ext.language", true)) {
            ProtocolLibrary.getProtocolManager().addPacketListener(new LanguageListener(this));
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(new FakeNicknameListener(this));


        LanguageProviderImpl provider = new LanguageProviderImpl();

        getServer().getServicesManager()
                .register(LanguageProvider.class, provider, this, ServicePriority.Normal);

        new AntiAfk(this);

        I18nParserController parser = I18nParserController.getInstance();

        parser.registerValue("name", (language, args) -> {
            if (args == null || args.length == 0) {
                return null;
            }

            String playerName = args[0];

            Gamer gamer;
            Consumer<Gamer> loadOffline = Gamer::loadRanks;

            if (playerName.startsWith("#")) {
                gamer = Gamer.getOrCreate(Integer.parseInt(playerName.substring(1)), loadOffline);
            } else {
                gamer = Gamer.getOrCreate(playerName, loadOffline);
            }

            return gamer == null ? "§4N/A" : gamer.getDisplayName();
        });

        new WorldListener(this);
        new ChatListener(this);
        new GamerListener(this);
        new PartyListener(this);
        new LevelListener(this);
        new FriendListener(this);
        new IgnoreListener(this);
        new HotbarListener(this);
        new GuiListener(this);
        new MetadataListener(this);
        new ScoreboardListener(this);
        new PlayerTopListener(this);
        new NoteBlockListener(this);
        new PlayerMentionListener(this);
        new EmotionsListener(this);
        new SignInput(this);
        new CoreListener(this);
        new QuestListener(this);
        new ChatModeListener(this);
        new ActionItemListener(this);
        new ExploitDetectListener(this);
        BowFixListener bowFixListener = new BowFixListener(this);
        getServer().getPluginManager().registerEvents(new BowFixBukkitListener(bowFixListener), this);
        new GamerSessionListener(this);
        new DuelsCoreProcessor(this);
        new DevModeListener(this);

        getServer().getPluginManager().registerEvents(
                new RankRefreshListener(
                        StarshipBukkit.getInstance().getRanksService(),
                        StarshipBukkit.getInstance().getUserRanksService()
                ),
                this
        );

        initBoxes();

        initLabyModIntegration();
        initLunarClient();

        addShutdownTask(() -> {
            Instant start = Instant.now();

            Logging.IMP.info("Сохранение статистики игроков...");
            GameStatsManager.DEFAULT.flushAll(true);

            Logging.IMP.info("Статистика сохранена ({} мс.)",
                    Duration.between(start, Instant.now()).toMillis());
        });

        StatsManager.INSTANCE.loadStatsModes();
        CaseManager.IMP.initCaseItems();
        VoteSqlHandler.INSTANCE.loadPlatformList();

        RequestManagerImpl.injectManager(this);

        Master master = CoreUtil.getMaster();

        addonService = new PluginAddonServiceImpl();
        playerVersionProvider = new ViaVersionProvider();

        if (master != null) {
            statsMode = StatsManager.INSTANCE.tryGetMode(master.name());
        }

        if (statsMode == null) {
            statsMode = StatsManager.INSTANCE.tryGetMode(CoreUtil.getServerPrefix());
        }

        if (statsMode == null) {
            statsMode = StatsManager.INSTANCE.tryGetMode(CoreUtil.getServerName());
        }

        if (CoreUtil.isMaster()) {
            gameHotbar.registerDefaults();
        }

        if (!CoreUtil.isMaster() && !CoreUtil.isMini()) {
            SitManager.INSTANCE.register(this);
        }

        new FakeEntityTablistTask(this, nmsApi);

//        ExternalPermissionsForGroupManager.INSTANCE.load(this.loadConfig("external_permissions"));

        // анрегаем ненужные команды
        getCommandManager().unregisterCommand("tps");
        getCommandManager().unregisterCommand("pl"); // для работы /pl алиас от /p list
        getCommandManager().unregisterCommand("gamemode");

        getCommandManager().addCommand(StreamsCommand.class);
        getCommandManager().addCommand(new SettingsCommand());
        getCommandManager().addCommand(FireWorkCommand.class);
        getCommandManager().addCommand(MemoryCommand.class);
        getCommandManager().addCommand(new StatsCommand());
        getCommandManager().addCommand(TestLangCommand.class);
        getCommandManager().addCommand(TestItemTranslationCommand.class);
        getCommandManager().addCommand(new TestCustomNameCommand(this));
        getCommandManager().addCommand(new TestCorpseCommand(this));
        getCommandManager().addCommand(TestHologramCommand.class);
        getCommandManager().addCommand(FuckCommand.class);
        getCommandManager().addCommand(TranslateCommand.class);
        getCommandManager().addCommand(ImageCommand.class);
        getCommandManager().addCommand(ReportCommand.class);
        getCommandManager().addCommand(ReportListCommand.class);
        getCommandManager().addCommand(JoinMessageCommand.class);
        getCommandManager().addCommand(GameModeCommand.class);
        getCommandManager().addCommand(SpeedCommand.class);
        getCommandManager().addCommand(EntityCommand.class);
        getCommandManager().addCommand(NickCommand.create(this));
        getCommandManager().addCommand(ChatModeCommand.class);
        getCommandManager().addCommand(new TestFunnyPacketCommand());
        getCommandManager().addCommand(EmotionsCommand.class);
        getCommandManager().addCommand(new SongCommand(this));
        getCommandManager().addCommand(ExperienceCommand.class);
        getCommandManager().addCommand(DeveloperModeCommand.class);
        getCommandManager().addCommand(SlimeMapCommand.class);
        getCommandManager().addCommand(DonateCommand.class);
        getCommandManager().addCommand(CheatModeCommand.class);
        getCommandManager().addCommand(TntTargetCommand.class);
        getCommandManager().addCommand(VoteCommand.class);
        getCommandManager().addCommand(StartCommand.class);
        getCommandManager().addCommand(SetGameHpCommand.class);
        getCommandManager().addCommand(AxVpnCommand.class);
        getCommandManager().addCommand(AxClientCommand.class);
        getCommandManager().addCommand(PrivateGameCommand.class);
        getCommandManager().addCommand(PrefixColorCommand.class);
        getCommandManager().addCommand(ResetPrefixColorCommand.class);
        getCommandManager().addCommand(new AwardsCommand());
        getCommandManager().addCommand(EntitiesCommand.class);
        getCommandManager().addCommand(new ClonesCommand());
        getCommandManager().addCommand(new TriggerNewYearShow());
        getCommandManager().addCommand(TestCoreAPI.class);

        val giftBanService = new GiftBanService();

        getCommandManager().addCommand(new GiftBanCommand(giftBanService));

        getServer().getPluginManager().registerEvents(new GiftBanListener(giftBanService), this);

        ServerMode serverMode = CoreUtil.getMode();

        if (!serverMode.isMaster() && !serverMode.isSpecial()) {
            new LobbyTagListener(this);
            new JoinMessageListener(this);
            new MysteryBoxListener(this);
            new VanishListener();
        }

        if (serverMode.isMaster()) {
            new GamerRedirectToLobbyListener(this);
        }

        if (Connection.isActive()) {
            Connection.setSlots(Bukkit.getMaxPlayers());
        }

        getServer().getPluginManager().registerEvents(new club.abelix.api.base.hotbar_new.listener.HotbarListener(hotbarService), this);

        initTournament(
                Env.getString("TOURNAMENT_SERVICE_HOST"),
                Env.getInt("TOURNAMENT_SERVICE_PORT", 9090)
        );

        initGameHistory();
    }

    @Override
    public void onLoad() {
        String packageVersion = MinecraftReflection.getPackageVersion();

        switch (packageVersion) {
            case "v1_8_R3" -> nmsApi = new NmsApi1_8(this);
            case "v1_12_R1" -> nmsApi = new NmsApi1_12(this);
            case "v1_16_R3" -> nmsApi = new NmsApi1_16(this);
            default -> {
                setEnabled(false);
                log.warn("NMS not found for version {}. Plugin will not work", packageVersion);
                return;
            }
        }

        nmsApi.load();

        log.info("Using NMS protocol version - {}, server version - {}", nmsApi.getProtocolNumber(), packageVersion);
    }

    public void initTournament(String host, int port) {
        if (ratingChannel != null) {
            ratingChannel.shutdownNow();
        }

        EventLoopGroup group = Epoll.isAvailable()
                ? new EpollEventLoopGroup()
                : new NioEventLoopGroup();

        ratingChannel = NettyChannelBuilder
                .forAddress(host, port)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .enableRetry()
                .maxRetryAttempts(3)
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .eventLoopGroup(group)
                .channelType(Epoll.isAvailable()
                        ? EpollSocketChannel.class
                        : NioSocketChannel.class)
                .build();

        tournamentClient = new RemoteTournamentClient(ratingChannel);
        tournamentService = new TournamentService(tournamentClient);
    }

    private void initBoxes() {
        boxStorageService = new BoxesStorageService();
        boxesService = new BoxesService(boxStorageService);
        
        animationService = new AnimationService(this);

        boxOpenService = new BoxOpenService(
                animationService,
                boxesService
        );

        rewardSelectorService = new RewardSelectorService(boxStorageService, boxesService);

        if (!BOXES_ENABLED) {
            log.warn("Boxes are disabled (FalconAPI.BOXES_ENABLED=false). Skipping box listeners and commands.");
            return;
        }

        val pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new RewardListener(rewardSelectorService), this);
        pluginManager.registerEvents(new BoxListener(animationService, boxOpenService, boxesService), this);
        pluginManager.registerEvents(new AnimationListener(animationService), this);

        getCommandManager().addCommand(new BoxControlCommand(boxesService));
        getCommandManager().addCommand(new TestBoxRewardSelectorCommand(rewardSelectorService));
        getCommandManager().addCommand(new TestBoxRewardChances(boxStorageService));
    }

    private void initLabyModIntegration() {
        LabyIntegrationService labyIntegrationService = new LabyIntegrationService();
        labyNpcEmotesService = new LabyNpcEmotesService();

        new LabyPlayerListener(this, labyIntegrationService);

        new LabyEconomyUpdateTask(this, labyIntegrationService).timer(20, 20);
        new LabyNpcEmotesTask(this, labyNpcEmotesService, labyIntegrationService).timer(60, 60);
    }

    private void initGameHistory() {
        String host = Env.getString("GAMEHISTORY_HOST", "gamehistory-service");
        int port = Env.getInt("GAMEHISTORY_PORT", 9090);

        gameHistoryService = new GameHistoryService(host, port);
        log.info("GameHistoryService initialized ({}:{})", host, port);

        addShutdownTask(() -> gameHistoryService.shutdown());
    }

    private void initLunarClient() {
        new LunarDiscordRichListener(this);
        new LunarDisabledModsListener(this);
    }

    @Override
    public void onPluginDisable() {
        log.info("Before disabling FalconAPI, saving all player boxes...");

        animationService.cancelAll();

        if (tournamentClient instanceof RemoteTournamentClient impl) {
            impl.shutdown();
        }

        if (ratingChannel != null) {
            ratingChannel.shutdown();
            try {
                if (!ratingChannel.awaitTermination(5, TimeUnit.SECONDS)) {
                    ratingChannel.shutdownNow();
                }
            } catch (InterruptedException e) {
                ratingChannel.shutdownNow();
            }
        }
    }
}
