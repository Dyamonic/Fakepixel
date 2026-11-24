package club.abelix.api.server.game;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.state.GamerState;
import club.abelix.api.base.hotbar_new.HotbarService;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.party.Party;
import club.abelix.api.base.region.CuboidRegion;
import club.abelix.api.base.region.Point3Dim;
import club.abelix.api.base.region.Region3Dim;
import club.abelix.api.base.util.chat.Broadcaster;
import club.abelix.api.base.util.chat.MessageSender;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.base.util.location.RawLocation;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.base.util.task.AbstractTickingLaterTask;
import club.abelix.api.base.util.task.cancel.CancelReason;
import club.abelix.api.base.util.task.registration.TaskRegistrationProvider;
import club.abelix.api.base.util.worldtime.WorldSetTime;
import club.abelix.api.base.util.worldtime.WorldTimeState;
import club.abelix.api.nms.bossbar.FakeBossbar;
import club.abelix.api.nms.entity.type.FakeEntity;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.api.nms.team.FakeTeamApi;
import club.abelix.api.server.base.listener.GameTagListener;
import club.abelix.api.server.game.board.EndBoard;
import club.abelix.api.server.game.board.EndBoardBase;
import club.abelix.api.server.game.board.WaitingBoard;
import club.abelix.api.server.game.board.WaitingBoardBase;
import club.abelix.api.server.game.box.GameWeightStorage;
import club.abelix.api.server.game.data.GameDataStorage;
import club.abelix.api.server.game.event.*;
import club.abelix.api.server.game.factory.EntityFactory;
import club.abelix.api.server.game.history.GameHistoryListener;
import club.abelix.api.server.game.hotbar.GameHotbar;
import club.abelix.api.server.game.listener.base.GameCustomListener;
import club.abelix.api.server.game.listener.base.GameRestrictionListener;
import club.abelix.api.server.game.listener.ending.GameEndListener;
import club.abelix.api.server.game.listener.game.*;
import club.abelix.api.server.game.listener.game.fun.CheatGameListener;
import club.abelix.api.server.game.listener.privategame.PrivateGameListener;
import club.abelix.api.server.game.listener.waiting.WaitingListener;
import club.abelix.api.server.game.manager.*;
import club.abelix.api.server.game.map.selector.GameMapSelector;
import club.abelix.api.server.game.map.selector.impl.CoreMapBalancerSelector;
import club.abelix.api.server.game.metadata.GameMetadataStorage;
import club.abelix.api.server.game.privategame.settings.PrivateGameSetting;
import club.abelix.api.server.game.queue.GameQueue;
import club.abelix.api.server.game.setting.GameSetting;
import club.abelix.api.server.game.stats.GameStatsCache;
import club.abelix.api.server.game.status.GameStatusUpdater;
import club.abelix.api.server.game.team.GameTeam;
import club.abelix.commons.data.quest.Quest;
import club.abelix.commons.data.quest.QuestSqlHandler;
import club.abelix.commons.data.stat.StatsField;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import club.abelix.commons.util.collection.CollectionUtil;
import club.abelix.commons.util.exclusive.ExclusivePeriod;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.di.api.annotation.Autowired;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.online.info.ArenaServerInfo;
import club.abelix.network.packet.play.client.CMakePrivateGameResponse;
import club.abelix.network.type.connect.ConnectStatus;
import club.abelix.network.type.map.GameMap;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.ServerMode;
import com.boydti.fawe.util.TaskManager;
import com.google.common.collect.Iterables;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import lombok.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public abstract class Game implements TaskRegistrationProvider {

    private static final boolean USE_FAWE = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
    /**
     * Логгер Log4j для вывода сообщений в консоль
     */
    public final Logger log;

    @Getter
    protected final Set<Gamer> gamers = CollectionUtil.createConcurrentSet();
    /**
     * ID игры (серверный)
     */
    @Getter
    private final int serverGameId;
    /**
     * Имя игры: [MASTER]-[SERVER_LETTER][GAME_ID] (SW-A1)
     */
    @Getter
    @EqualsAndHashCode.Include
    private final String name;
    /**
     * Плагин игры
     */
    @Getter
    private final AbstractPlugin plugin;
    /**
     * Подрежим арены
     */
    @Getter
    private final Mini mini;
    /**
     * Данные игры (энтити, фейк энтити, фейк команды)
     */
    @Getter
    private GameDataStorage dataStorage;
    /**
     * Все листенеры зареганные в игре
     */
    private final Map<String, YListener> listenerMap = new HashMap<>();
    /**
     * Все такси, которые должны выполняться
     */
    private final Map<UUID, BukkitTask> tasks
            = new ConcurrentHashMap<>();

    @Getter
    private List<GameStatsCache> statsCache;

    /**
     * Игровые настройки
     */
    private final Map<GameSetting, Object> gameSettingMap = new EnumMap<>(GameSetting.class);

    /**
     * Снапшот игровых настроек до включения режима приватной игры (используется для отката).
     */
    private EnumMap<GameSetting, Object> settingsSnapshotBeforePrivateGame;

    private int[] coreDisableStatsGamers;
    // победитель в игре
    // устанавливается в методе initializeWinner() игрой
    @Getter
    protected GameTeam winner;

    /**
     * Игровой мир
     */
    @Getter
    @Setter
    private World gameWorld;
    /**
     * Название мира
     */
    @Getter
    @Deprecated
    private String map;

    @Getter
    @Setter
    private GameMap gameMap = null;

    /**
     * WE менеджер. Нужен для вставки лобби и игры
     */
    @Getter
    private SchematicManager waitingSchematicManager;
    /**
     * Метадата для энтити и блоков
     */
    @Getter
    private GameMetadataStorage metadata;
    /**
     * Система очередей
     */
    @Getter
    private GameQueue gameQueueManager;
    /**
     * Хотбар по умолчанию
     */
    @Getter
    @Setter
    private String defaultHotbar;
    /**
     * Статистика
     */
    @Getter
    private StatsMode miniStatsMode, masterStatsMode;
    /**
     * Текущий статус игры
     */
    @Getter
    private GameState gameState;
    @Getter
    private GameStatsManager statsManager;
    @Getter
    private WaitingManager waitingManager;
    @Getter
    private SpectatorManager spectatorManager;
    @Getter
    private TeamManager teamManager;
    @Getter
    private GameGroupSelectorManager selectorManager;

    @Getter
    private GameWeightStorage gameWeightStorage;

    @Getter
    @Setter
    private FakeBossbar gamePrimaryBossbar;

    @Setter
    private YamlConfiguration config;

    @Getter
    @Setter
    Region3Dim playableZone;

    @Getter
    private Set<CuboidRegion> restrictedBuildingsRegions;

    @Getter
    @Setter
    private GameMapSelector gameMapSelector = CoreMapBalancerSelector.INSTANCE;
    private int lastQuestDay;
    private Quest quest;

    @Getter
    private LocalDateTime startTime;

    /**
     * Отключение сохранения статистики
     */
    @Getter
    @Setter
    protected boolean statsSavingDisabled;

    /**
     * Приватная игра (пати)
     */
    @Getter
    @Setter
    private int privateGamePartyId;

    /**
     * Настройки приватной игры
     */
    @Getter
    private PrivateGameSettingsManager privateGameSettings;

    @Autowired
    private HotbarService hotbarService;

    private ArenaServerInfo arenaServerInfo;

    public Game(@NonNull AbstractPlugin plugin) {
        this(plugin, null);
    }

    /**
     * Конструктор игры
     * <br>
     * <br>Обязательно должен быть plugin
     * <br>mini обычно null на смешере, на остальных режимах он не null
     */
    public Game(@NonNull AbstractPlugin plugin, @Nullable Mini mini) {
        DiBukkitPlatform.getFactory().getBeanService().processInjection(this);

        if (plugin.getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") == null) {
            throw new IllegalStateException("FastAsyncWorldEdit is required");
        }

        if (mini == null) {
            this.serverGameId = 0;
            this.name = CoreUtil.getServerName();
        } else {
            Master master = mini.getMaster();

            this.serverGameId = GameManager.INSTANCE.nextId();
            this.name = generateGameName(master, serverGameId);
        }

        this.plugin = plugin;
        this.mini = mini;
        this.gameState = GameState.ENDING;
        this.log = LogManager.getLogger(name);

        // Данные
        dataStorage = new GameDataStorage();

        this.gameQueueManager = new GameQueue(this);
    }

    public static String generateGameName(Master master, int gameId) {
        val serverNumber = CoreUtil.getServerNumber();
        val serverName = new StringBuilder();

        serverName.append((char) ('A' + (serverNumber - 1) % 26));

        if (serverNumber > 26) {
            serverName.append((char) ('A' + ((serverNumber - 1) / 26) - 1));
        }

        return String.format("%s-%s%d", master.getServerName(), serverName, gameId);
    }

    public void onGameInit() {

    }

    public YamlConfiguration getConfig() {
        if (config == null) {
            config = new YamlConfiguration();

            try {
                config.load(new File(getPlugin().getServer().getWorldContainer(), "settings.yml"));
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }

        return config;
    }

    /**
     * Вызывается один раз при создании игры впервые
     */
    public final void initGame() {
        DiBukkitPlatform.getFactory().getBeanService().withBean(GameHotbar.class, bean -> {
            bean.registerGame(this);
        });

        miniStatsMode = StatsManager.INSTANCE.tryGetMode(mini.name());
        masterStatsMode = StatsManager.INSTANCE.tryGetMode(mini.getMaster().name());

        onGameInit();
    }

    public List<Gamer> getGamers() {
        return new ArrayList<>(gamers);
    }

    /**
     * Получить настройку карты
     *
     * @return секция с настройкой карты
     */
    public ConfigurationSection getMapConfiguration() {
        return getMapConfiguration(map);
    }

    /**
     * Получить настройку карты по названию карты
     *
     * @param mapName название карты
     * @return секция с настройкой карты
     */
    public ConfigurationSection getMapConfiguration(@NonNull String mapName) {
        return config.getConfigurationSection(String.format("Worlds.%s", mapName));
    }


    public File getWaitLobbySchematic() {
        val activeExclusive = ExclusivePeriod.getCurrentExclusivePeriod();

        val waitingFolderContainer = new File(FalconAPI.getInstance().getDataFolder(), "waiting");
        val exclusiveFolder = new File(waitingFolderContainer, "exclusive");

        File schematicFolder = waitingFolderContainer;
        // Эксклюзивный период
        if (activeExclusive != null && activeExclusive.isAvailable() && exclusiveFolder.exists()) {
            val periodFolder = new File(exclusiveFolder, activeExclusive.name().toLowerCase());

            if (periodFolder.exists()) {
                schematicFolder = periodFolder;
            }
        }

        val miniWaitingSchematic = new File(schematicFolder,
                mini.name().toLowerCase() + ".schematic");
        val masterWaitingSchematic = new File(schematicFolder,
                mini.getMaster().name().toLowerCase() + ".schematic");

        if (miniWaitingSchematic.exists()) {
            return miniWaitingSchematic;
        }

        if (masterWaitingSchematic.exists()) {
            return masterWaitingSchematic;
        }

        return new File(waitingFolderContainer, "base.schematic");
    }

    public Location getSpawnLocation() {
        return getSetting(GameSetting.LOBBY_LOCATION);
    }

    public Location getCenter() {
        return getSetting(GameSetting.MAP_CENTER_LOCATION);
    }

    public boolean isProtectionLocation(@NotNull Location blockLocation) {
        if (restrictedBuildingsRegions == null || restrictedBuildingsRegions.isEmpty()) {
            return false;
        }

        for (CuboidRegion protectionRegion : getRestrictedBuildingsRegions()) {
            if (protectionRegion.isInRegion(blockLocation)) {
                return true;
            }
        }
        return false;
    }

    public void addProtectionLocation(@NotNull CuboidRegion cuboidRegion) {
        restrictedBuildingsRegions.add(cuboidRegion);
    }

    // region messaging

    public int getNotVanishedOnline() {
        return (int) gamers.stream().filter(gamer -> !gamer.isVanished()).count();
    }

    public Collection<Gamer> getNotVanishedGamers() {
        return gamers.stream().filter(gamer -> !gamer.isVanished()).collect(Collectors.toList());
    }

    public void addGamer(Gamer gamer) {
        gamers.add(gamer);
        updateStatus();
    }

    public void removeGamer(Gamer gamer) {
        gamers.remove(gamer);
        updateStatus();

        tryRollbackPrivateSettings();
    }

    public void updateStatus() {
        if (arenaServerInfo == null) {
            return;
        }

        StarshipBukkit.getInstance().getTrackingService().update(arenaServerInfo);
    }

    public String getPrefix(Language language) {
        return language.getMessage("game_prefix", "%game%", getDisplayName());
    }

    public String getDisplayMapName() {
        return gameMap.getDisplayName();
    }

    public void cleanResources() {
        restrictedBuildingsRegions = null;

        gameQueueManager.deactivate();

        statsSavingDisabled = false;

        if (selectorManager != null) {
            selectorManager.reset();
        }

        gameWeightStorage.reset();
        teamManager.reset();
        spectatorManager.reset();

        if (metadata != null) {
            log.info("[{}] Clearing metadata and settings...", name);
            metadata.clear();
        }

        log.info("[{}] Clearing garbage...", name);

        dataStorage.clearFakeTeams();
        dataStorage.clearEntities();
        dataStorage.clearFakeEntities();

        winner = null;
    }

    @SneakyThrows
    public void refresh() {
        log.info("[{}] Unregistering all game listeners and tasks...", name);
        removeListeners();
        removeTasks();

        log.info("[{}] Clearing gamers list", name);
        gamers.clear();

        gameSettingMap.keySet().removeIf(GameSetting::isClearOnReset);

        // менеджер команд
        teamManager = null;
        // менеджер спектаторов
        spectatorManager = null;
        // менеджер статистики
        statsManager = null;
        // Менеджер ожидания
        waitingManager = null;
        // Схематика
        waitingSchematicManager = null;
        // Приватная игра
        privateGameSettings = null;
        privateGamePartyId = -1;

        settingsSnapshotBeforePrivateGame = null;

        log.info("[{}] Resetting game map...", name);
        resetMap();

        loadGame(null);
    }

    public final void resetMap() {
        log.info("[{}] World {} [{}] start reset", name, gameWorld.getName(), map);
        this.getGameMapSelector().unloadAndDeleteWorld(this);

        this.gameWorld = null;
        this.gameMap = null;
        this.map = null;
        log.info("[{}] Game world success reset!", name);
    }

    public FakeTeam createTeam(Function<FakeTeamApi, FakeTeam> teamFactory) {
        FakeTeam newTeam = teamFactory.apply(FalconAPI.getNmsApi().getFakeTeamApi());

        dataStorage.store(newTeam);

        return newTeam;
    }

    public <T extends FakeEntity> T createEntity(EntityFactory<T> entityFactory) {
        T newEntity = entityFactory.createEntity(FalconAPI.getNmsApi().getFakeEntityApi());

        dataStorage.store(newEntity);

        return newEntity;
    }

    public void addGameEntity(Entity entity) {
        dataStorage.store(entity);
    }

    public String getDisplayName() {
        return mini.getMaster().getDisplayName();
    }

    public void broadcast(@NonNull String message) {
        for (Gamer gamer : gamers) {
            gamer.sendMessage(getPrefix(gamer.getLanguage()) + message);
        }
    }

    public void broadcastLocalized(String key, Function<Gamer, ObjectLinkedBuf> objectSupplier) {
        gamers.forEach(gamer -> sendLocalizedMessage(gamer, key, objectSupplier.apply(gamer).toArray()));
    }

    public void sendLocalizedMessage(Gamer gamer, String key, Object... objects) {
        gamer.sendMessage(getPrefix(gamer.getLanguage()) + gamer.getLanguage().getMessage(key, objects));
    }

    public void broadcastLocalized(@NonNull String locale, @NonNull Object... objects) {
        ObjectLinkedBuf array = new ObjectLinkedBuf(objects);
        broadcastLocalized(locale, gamer -> array);
    }

    public void broadcastLocalized(@NonNull String locale, @NonNull ObjectLinkedBuf objects) {
        broadcastLocalized(locale, gamer -> objects);
    }

    public void broadcastLocalizedActionbar(String key, Function<Gamer, ObjectLinkedBuf> objectSupplier) {
        gamers.forEach(gamer -> gamer.sendLocalizedActionBar(key, objectSupplier.apply(gamer).toArray()));
    }

    public void broadcastLocalizedActionbar(@NonNull String locale, @NonNull Object... objects) {
        ObjectLinkedBuf array = new ObjectLinkedBuf(objects);
        broadcastLocalizedActionbar(locale, gamer -> array);
    }

    public void broadcastLocalizedActionbar(@NonNull String locale, @NonNull ObjectLinkedBuf objects) {
        broadcastLocalizedActionbar(locale, gamer -> objects);
    }

    public FakeBossbar createBossbar(String title, float value) {
        return gamePrimaryBossbar = FalconAPI.getNmsApi().getBossbarApi().createBossbar(title, value);
    }

    /**
     * Регистрация листенера
     */
    public void registerListener(@NonNull YListener listener) {
        registerListener(listener.getClass().getSimpleName(), listener);
    }

    public void registerListener(@NonNull String name, @NonNull YListener listener) {
        listenerMap.put(name.toLowerCase(), listener);

        log.info("[{}] Game processor was registered {}", this.name, name);
    }

    /**
     * Удаление зарегистрированного листенера
     */
    public void removeListener(@NonNull Class<? extends YListener> listenerClazz) {
        removeListener(listenerClazz.getSimpleName());
    }

    public void removeListener(@NonNull String name) {
        YListener listener = listenerMap.remove(name.toLowerCase());

        if (listener != null) {
            listener.unregister();
        }

        log.info("[{}] Listener was unregistered {}", this.name, name);
    }

    /**
     * Удаление всех листенеров
     */
    public void removeListeners() {
        //копируем чтобы избежать CME
        new ArrayList<>(listenerMap.keySet()).forEach(this::removeListener);
    }

    @Override
    public void registerTask(@NonNull UUID id, @NonNull BukkitTask task) {
        tasks.put(id, task);
    }

    @Override
    public boolean hasTask(@NonNull UUID taskId) {
        return tasks.containsKey(taskId);
    }

    @Override
    public boolean removeTask(@NonNull UUID id) {
        BukkitTask ex = tasks.remove(id);
        boolean canceled = ex != null;

        if (canceled) {
            ex.cancel();
        }

        return canceled;
    }

    /**
     * Список запущенных тасков
     */
    @Override
    public List<UUID> getRunningTaskIds() {
        return new ArrayList<>(tasks.keySet());
    }

    // ----------------------------------------------------------------

    /**
     * Изменение значения настройки
     */
    public void setSetting(@NonNull GameSetting setting, Object settingValue) {
        Logging.IMP.info("[{}] Game setting {}=>{}", this.name, setting.name(),
                settingValue);
        synchronized (gameSettingMap) {
            if (settingValue == null) {
                gameSettingMap.remove(setting);
            } else {
                gameSettingMap.put(setting, settingValue);
            }
        }
    }

    /**
     * Получить значение настройки
     */
    @SuppressWarnings("unchecked")
    public <T> T getSetting(@NonNull GameSetting setting) {
        synchronized (gameSettingMap) {
            return (T) gameSettingMap.get(setting);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getSetting(@NonNull GameSetting setting, @NonNull T defaultValue) {
        synchronized (gameSettingMap) {
            return (T) gameSettingMap.getOrDefault(setting, defaultValue);
        }
    }

    public synchronized void setGameState(@NonNull GameState gameState) {
        if (this.gameState == gameState) {
            return;
        }

        val oldGameState = this.gameState;
        this.gameState = gameState;

        log.info("Game {} change state {} -> {}", name, oldGameState, gameState);

        HeartBeat.callEvent(new GameChangeStateEvent(this, oldGameState, gameState));

        if (gameState == GameState.STAND_BY || gameState == GameState.STARTING) {// Игра переключилась в режим "лобби", ставим всем зашедшим игрокам лобби-хотбар
            defaultHotbar = GameHotbar.formattedWaitingLobbyHotbarId(this);
        } else {
            defaultHotbar = null;
        }

        // обновляем статус на коре, если конечно мы подключены к нему
        // и если до этого gameState был не null, т.к если он null,
        // то loadGame вызывается впервые и GameManager сам отправит
        // GameState
        if (Connection.isActive()) {
            Connection.setArenaState(name, gameState);
        } else {
            log.warn("Game {} try change state, but core not connected!");
        }

        updateStatus();
    }

    public StatsMode getStatsMode() {
        if (miniStatsMode != null) {
            return miniStatsMode;
        }

        if (masterStatsMode != null) {
            return masterStatsMode;
        }

        return FalconAPI.getStatsMode();
    }

    /**
     * Получить текущий квест
     */
    public Quest getTodayQuest() {
        int currentDay = (int) TimeUtil.getDayUTC();

        //загружаем квест из БД только если последний загруженный квест - день 0 или не сегодня
        if (lastQuestDay == 0 || lastQuestDay != currentDay) {
            if (masterStatsMode != null) {
                quest = QuestSqlHandler.INSTANCE.getQuest(masterStatsMode, currentDay)
                        .join();
            }

            if (quest == null && miniStatsMode != null) {
                quest = QuestSqlHandler.INSTANCE.getQuest(miniStatsMode, currentDay)
                        .join();
            }

            lastQuestDay = currentDay;
        }

        return quest;
    }

    public Set<FakeTeam> getFakeTeams() {
        return dataStorage.getFakeTeams();
    }

    public Set<FakeEntity> getFakeEntities() {
        return dataStorage.getFakeEntities();
    }

    // ENDING -> STAND_BY
    public final void loadGame(Runnable finish) {
        log.info("[{}] Loading game...", name);

        // Метаданные
        metadata = new GameMetadataStorage(this);

        // Приватные игры
        privateGamePartyId = -1;

        // selectGameMap() не вызывался
        if (gameWorld == null) {
            try {
                selectGameMap();
            } catch (RuntimeException exception) {
                log.error("[{}] Unabled to initialized game world. Shutdown server...", name);
                exception.printStackTrace();

                setGameState(GameState.ENDING);

                GameCrashManager.INSTANCE.onCrash(this);
                return;
            }
        }

        // Регионы где нельзя ломать
        restrictedBuildingsRegions = new HashSet<>();

        // Игровая зона
        val playableZOneConfig = getMapConfiguration().getConfigurationSection("PlayableZone");
        if (playableZOneConfig != null && getSetting(GameSetting.ENABLE_PLAYABLE_ZONE, true)) {
            val zoneMin = loadLocation(playableZOneConfig.getString("Min"));
            val zoneMax = loadLocation(playableZOneConfig.getString("Max"));

            this.playableZone = new Region3Dim(
                    new Point3Dim(zoneMin.getBlockX(), zoneMin.getBlockY(), zoneMin.getBlockZ()),
                    new Point3Dim(zoneMax.getBlockX(), zoneMax.getBlockY(), zoneMax.getBlockZ())
            );

            setSetting(GameSetting.MIN_Y_HEIGHT, playableZone.getMinimumPoint().getY());
            setSetting(GameSetting.MAX_Y_HEIGHT, playableZone.getMaximumPoint().getY());
            setSetting(GameSetting.VOID_FALL_MINIMUM_Y, playableZone.getMinimumPoint().getY());
        }


        PluginManager pluginManager = plugin.getServer().getPluginManager();

        // вызываем эвент
        pluginManager.callEvent(new GameLoadEvent(this));

        // менеджер команд
        teamManager = new TeamManager(this);

        // менеджер спектаторов
        spectatorManager = new SpectatorManager(this);

        // менеджер статистики
        statsManager = new GameStatsManager();

        if (statsCache != null) {
            statsCache.clear();
            statsCache = null;
        }

        // регаем листенеры ожидания, делаем это после загрузки игры, потому что еще сама игра может
        // выставить другой спавн в лобби ожидания
        waitingManager = new WaitingManager(this);

        // Приватная игра
        privateGameSettings = new PrivateGameSettingsManager(this);

        registerListener(new GameCustomListener(this));

        registerListener(new WaitingListener(this));
        registerListener(new GameRestrictionListener(this));

        registerListener(new GameTagListener(this));

        // после генерации производим необходимые действия
        onGameLoad();

        privateGameSettings.init();

        gameWeightStorage = new GameWeightStorage();

        if (getSetting(GameSetting.ENABLE_GROUPS, false)) {
            selectorManager = new GameGroupSelectorManager(this);
            selectorManager.init();
        }

        // Устанавливаем время
        int fixedWorldTime = getSetting(GameSetting.FIXED_WORLD_TIME, WorldTimeState.LIGHT_PEAK.getTime());
        WorldSetTime.setAndFreezeTime(this.gameWorld, fixedWorldTime);

        TaskManager.IMP.async(() -> {
            if (!getSetting(GameSetting.DISABLE_WAITING_LOBBY_PASTING, false)) {
                prepareWaitingLobby();
            }

            // выполняем действия для старта
            if (finish != null) {
                finish.run();
            }
        });

        // idk, state меняетя не верно, подождем пока лобби установится и попробуем
        HeartBeat.runLaterSync(() -> {
            setGameState(GameState.STAND_BY);

            val tags = List.of(getMini().name(), "GAME", mini.getMaster().name(), "MINI");

            arenaServerInfo = ArenaServerInfo.ofParent(
                    StarshipBukkit.getInstance().getServerInfo(),
                    getMini().getName(),
                    tags,
                    getSlotsCount(),
                    0,
                    0,
                    getName(),
                    getGameState().name(),
                    getGameMap().getTechnicalName()
            );

            StarshipBukkit.getInstance()
                    .getTrackingService()
                    .track(
                            arenaServerInfo,
                            new GameStatusUpdater(this)
                    );
        }, 3 * 20);
    }

    /**
     * Подготовка лобби ожидания (вызывать АСИНХРОННО)
     */
    private void prepareWaitingLobby() {
        Location center = getCenter();

        if (!USE_FAWE || center == null || getSpawnLocation() != null) {
            return;
        }

        File waitingLobbySchematic = getWaitLobbySchematic();
        if (!waitingLobbySchematic.exists()) {
            return;
        }

        waitingSchematicManager = new SchematicManager(waitingLobbySchematic, center);

        log.info("[{}] Using FAWE to paste waiting lobby: {}", name, waitingLobbySchematic);

        waitingSchematicManager.resetSession();
        waitingSchematicManager.pasteSchematic(new AbstractDelegateExtent(waitingSchematicManager.getSession()) {
            @Override
            public boolean setBlock(com.sk89q.worldedit.Vector vector, BaseBlock block) throws WorldEditException {
                if (block.getId() == BlockID.DIAMOND_BLOCK) {
                    Location location = new Location(
                            gameWorld, vector.getX(), vector.getY(), vector.getZ(), 0, 0
                    );

                    setSetting(GameSetting.LOBBY_LOCATION, location);

                    log.info("Waiting lobby location was set to: "
                            + LocationUtil.locationToString(location, false));

                    return super.setBlock(vector, new BaseBlock(0));
                }

                return super.setBlock(vector, block);
            }
        });
    }

    public int getPlayersPerTeam() {
        return getSetting(GameSetting.PLAYERS_PER_TEAM);
    }

    public Location[] loadLocations(Collection<String> lines) {
        return lines.stream().map(this::loadLocation).toArray(Location[]::new);
    }

    public Location loadLocation(String line) {
        return LocationUtil.stringToLocation(line.replace("%map%", gameWorld.getName()));
    }

    /**
     * Выбор игровой карты (нужно перед вызовом loadGame())
     */
    public final void selectGameMap() {
        if (this.gameMap == null) {
            this.gameMap = getGameMapSelector().selectMap(this);
            this.map = this.getGameMap().getTechnicalName();
        }

        // Шобы было заебись, пытаемся получить локацию
        String rawCenterLocation;
        if (getMapConfiguration().contains("Center")) {
            rawCenterLocation = getMapConfiguration().getString("Center");
        } else {
            rawCenterLocation = getMapConfiguration().getString("MapCenter");
        }

        RawLocation centerLocation = new RawLocation("", 0, 0, 0, 0, 0);
        if (rawCenterLocation != null && !rawCenterLocation.isEmpty())
            centerLocation = LocationUtil.stringToRawLocation(rawCenterLocation);

        this.gameWorld = getGameMapSelector().loadWorld(
                this, centerLocation
        );

        if (gameWorld == null) {
            plugin.getLogger().warning("Не удалось вычислить игровую карту!");
            throw new RuntimeException("GameMap is null");
        }

        if (Connection.isActive()) {
            Connection.setArenaGameMap(name, gameMap);
        }
    }

    public WaitingBoardBase createWaitingBoard(@NonNull Gamer gamer) {
        return new WaitingBoard(this, gamer);
    }

    public EndBoardBase createEndBoard(@NonNull Gamer gamer, @Nullable GameTeam winner) {
        return new EndBoard(this, gamer, winner);
    }


    // STARTING -> GAME
    public final void startGame() {
        // История игр
        registerListener(new GameHistoryListener(this));

        //вызываем эвент старта игры
        plugin.getServer().getPluginManager().callEvent(new GameStartEvent(this));
//        Logging.IMP.info("[{}] debug, startGame queue deactivate", this.name);
//        gameQueueManager.deactivate();

        startTime = LocalDateTime.now();

        //убираем не нужные листенеры
        removeListener(WaitingListener.class);
        removeListener(GameRestrictionListener.class);

        //нужные
        registerListener(new DefaultGameListener(this));
        registerListener(new DefaultStatsListener(this));
        registerListener(new GameDeathListener(this));
        registerListener(new GameSpectatorListener(this));

        // Фан штуки
        registerListener(new CheatGameListener(this));

        // Устанавливаем время
        int fixedWorldTime = getSetting(GameSetting.FIXED_WORLD_TIME, WorldTimeState.LIGHT_PEAK.getTime());
        WorldSetTime.setAndFreezeTime(this.gameWorld, fixedWorldTime);

        // На данный момент у всех игроков стоят лобби-хотбары
        // Их надо убрать
        hotbarService.clear(getAlivePlayers());

        // Если это приват, то применяем настройки
        if (isPrivateGame()) {
            for (PrivateGameSetting privateGameSetting : privateGameSettings.getSettings()) {
                privateGameSetting.getSelected().apply(this);
            }
        }

        for (Gamer gamer : gamers) {
            Player player = gamer.getPlayer();
            MessageSender.sendTitle(player, "", "", 0, 25, 0);

            if (gamer.isVanished()) {
                spectatorManager.addSpectator(gamer);

                gamer.sendLocalizedMessage("game_vanished");
                gamer.sendLocalizedTitle("game_vanished_title", "game_vanished_subtitle");

                continue;
            }

            player.setGameMode(GameMode.SURVIVAL);

            player.closeInventory();
            player.getInventory().clear();

            player.setExp(0);
            player.setLevel(0);
            player.setMaxHealth((double) getSetting(GameSetting.PLAYER_HP, 20));
            player.setHealth((double) getSetting(GameSetting.PLAYER_HP, 20));
            player.setWalkSpeed(getSetting(GameSetting.PLAYER_SPEED, 0.2f));
            player.setFoodLevel(20);
        }

        if (!statsSavingDisabled) {
            int emptyParties = 0;

            Set<Party> parties = new HashSet<>();

            for (Gamer gamer : gamers) {
                if (gamer.isSpectator()) {
                    continue;
                }

                Party party = gamer.getParty();

                if (party != null) {
                    parties.add(party);
                } else {
                    emptyParties++;
                }
            }

            // если все игроки в пати и на арене всего 1 пати
            // тогда стату не сохраняем, это же "локальная" игра
            if (parties.size() == 1 && emptyParties == 0) {
                disableStatsSaving();
            }

            checkCoreDisableStat();
        }

        if (getSetting(GameSetting.ENABLE_LOBBY_REMOVE, true) || !getSetting(GameSetting.DISABLE_WAITING_LOBBY_PASTING, true)) {
            removeLobby();
        }

        onGameStart();

        if (!teamManager.isInitialized()) {
            throw new IllegalStateException("Game did not initialize teams!");
        }

        if (getSetting(GameSetting.APPLY_ITEMS, true)) {
            // применяем вещи после очистки инвентаря
            applyGameItems();
        }
        // Отправляем броадкаст о начале игры игрокам.
        gamers.forEach(gamer -> {
            broadcastWithLines(gamer, bc -> broadcastStart(gamer, bc));
            gamer.playSound(YSound.LEVEL_UP);
        });

        //изменение статуса игры
        setGameState(GameState.GAME);
    }

    public final void removeLobby() {
        if (waitingSchematicManager == null) {
            return;
        }

        //лобби ожидания сверху карты (если оно есть) надо бы убрать
        TaskManager.IMP.async(() -> waitingSchematicManager.resetLobby());
    }

    public final void disableStatsSaving() {
        if (statsSavingDisabled) return;

        statsSavingDisabled = true;
        HeartBeat.runAsync(() -> broadcastLocalized("game_statistic_disabled"));
    }

    public void coreDisableStats(int[] players) {
        Logging.IMP.info("[Statistic] Core Disable Game {} statistic from player {}",
                getName(), Arrays.toString(players));

        this.coreDisableStatsGamers = players;

        if (gameState == GameState.GAME) {
            if (checkCoreDisableStat()) {
                this.coreDisableStatsGamers = null;
            }
        }
    }


    private boolean checkCoreDisableStat() {
        if (coreDisableStatsGamers == null) {
            return false;
        }

        if (statsSavingDisabled) {
            return true;
        }

        Set<Gamer> disableGamers = Arrays.stream(this.coreDisableStatsGamers).mapToObj(Gamer::getGamer)
                .collect(Collectors.toSet());
        Set<Gamer> inGameGamers = getGamers().stream().filter(gamer -> !gamer.isSpectator())
                .collect(Collectors.toSet());

        if (getAlivePlayers().size() != disableGamers.size()) {
            return false;
        }

        disableGamers.removeAll(inGameGamers);
        if (disableGamers.isEmpty()) {
            disableStatsSaving();
        }

        return disableGamers.isEmpty();
    }


    public boolean enablePrivateGameMode(int partyId) {
        if (privateGamePartyId != -1) {
            return false;
        }

        if (!getGamers().isEmpty()) {
            return false;
        }

        if (gameState != GameState.STAND_BY) {
            return false;
        }

        synchronized (gameSettingMap) {
            settingsSnapshotBeforePrivateGame = new EnumMap<>(gameSettingMap);
        }

        privateGamePartyId = partyId;
        registerListener(new PrivateGameListener(this));

        return true;
    }

    public void disablePrivateGameMode(boolean sendPacket) {
        Logging.IMP.info("[Private] Game {} disabling private mode...", getName());

        if (sendPacket) {
            Connection.sendPacket(new CMakePrivateGameResponse(false, this.getName()));
        }

        removeListener(PrivateGameListener.class);
        privateGamePartyId = -1;

        tryRollbackPrivateSettings();

        updateStatus();
        Logging.IMP.info("[Private] Game {} disabled private mode!", getName());
    }

    private void tryRollbackPrivateSettings() {
        if (settingsSnapshotBeforePrivateGame == null) return;

        boolean canRollback =
                gameState == GameState.STAND_BY
                        && gamers.isEmpty();

        if (!canRollback) return;

        synchronized (gameSettingMap) {
            gameSettingMap.clear();
            gameSettingMap.putAll(settingsSnapshotBeforePrivateGame);
        }

        settingsSnapshotBeforePrivateGame = null;
    }

    /**
     * Автоподгон приватной игры под размер пати.
     * По умолчанию ничего не делает — конкретные игры (например BedWars) переопределяют.
     * Нужно для того чтоб Если у тебя пати 17 или более, а режим только для 16, в приватки
     * это значение заскейлиться и челы смогут поиграть.
     */
    public void autoResizePrivateGameForParty(@NonNull Party party) {

    }

    public boolean isPrivateGame() {
        return privateGamePartyId != -1;
    }

    public String getNameDisplay() {
        StringBuilder sb = new StringBuilder();

        sb.append(getName());

        if (isPrivateGame()) {
            sb.append(" [").append("P").append("]");
        }

        return sb.toString();
    }

    /**
     * Получить список игроков
     */
    public final List<Gamer> getAlivePlayers() {
        return gamers.stream()
                .filter(gamer -> !gamer.isSpectator())
                .collect(Collectors.toList());
    }

    /**
     * Получить список наблюдателей
     */
    public final List<Gamer> getSpectators() {
        return getNotVanishedGamers().stream()
                .filter(Gamer::isSpectator)
                .collect(Collectors.toList());
    }

    /**
     * Получить список живых команд
     */
    public final <T extends GameTeam> List<T> getAliveTeams() {
        List<T> teams = new ArrayList<>();

        for (GameTeam team : teamManager.getTeams()) {
            if (team.isAlive()) {
                teams.add((T) team);
            }
        }

        return teams;
    }

    // GAME -> ENDING
    public final void stopGame() {
        if (getGameState() == GameState.ENDING) {
            throw new IllegalStateException("Game already ending");
        }

        setGameState(GameState.ENDING);

        Logging.IMP.info("[{}] Ending process start...", getName());

        // устанавливаем победителя игры
        if (winner == null) {
            try {
                initializeWinner();
            } catch (Exception any) {
                Logging.IMP.info("[{}] Failed initialize winner. Details: ", name);
                any.printStackTrace();
            }
        }

        //вызываем эвент окончания игры
        HeartBeat.callEvent(new GameEndEvent(this));

        // удаляем все листенеры и все таски
        removeListeners();
        removeTasks();

        // удаляем боссбар чтобы не мазолил глаза
        if (gamePrimaryBossbar != null) {
            gamePrimaryBossbar.remove();
            gamePrimaryBossbar = null;
        }

        // регаем основные листенеры для "защиты" сервера
        registerEndListeners();

        try {
            //выполняем действия от плагина
            onGameEnd();
        } catch (Throwable t) {
            Logging.IMP.info("Game {} failed on game end!", this.name);
            t.printStackTrace();
        }

        val sendTopOnEnd = getSetting(GameSetting.BROADCAST_KILLS_TOP_ON_END, false);

        val statsMode = getStatsMode();

        val field = statsMode == null ? null : statsMode.getField("kills");

        if (sendTopOnEnd && field != null) {
            val killsCache = new ArrayList<>(getStatsManager().getCacheMap()
                    .valueCollection().stream()
                    .filter(s -> s.getCachedStat(field, 0) >= 1)
                    .sorted(Comparator.comparingInt(s -> s.getCachedStat(field, 0)))
                    .toList());

            Collections.reverse(killsCache);

            this.statsCache = killsCache;
        }

        try {
            for (Gamer gamer : gamers) {
                gamer.getPlayer().getInventory().clear();
                hotbarService.apply(GameHotbar.END_HOTBAR_ID, gamer);

                createEndBoard(gamer, winner);
                broadcastWithLines(gamer, bc -> {
                    broadcastEnd(gamer, bc);

                    if (sendTopOnEnd) {
                        broadcastEndTop(gamer, field, bc);
                    }
                });
            }
        } catch (Exception any) {
            log.error("Failed to gamer end process. Details: ");
            any.printStackTrace();
        }

        // сохраняем стату всех игравших игроков за игру
        statsManager.flushAll(false);

        StarshipBukkit.getInstance().getTrackingService().stop(arenaServerInfo.getId());

        if (getSetting(GameSetting.ENABLE_END_TASKS, true)) {
            applyEndTasks(getSetting(GameSetting.END_DELAY, 9));
        }
    }

    public final void registerEndListeners() {
        registerListener(new GameRestrictionListener(this));
        registerListener(new GameEndListener(this));
        registerListener(new GameTagListener(this));
    }

    public final void applyEndTasks(int delay) {
        // выполняем таск телепорта в лобби через delay с
        new AbstractTask(this) {
            @Override
            protected void run() {
                // очищаем все игровые ресурсы и сендим игроков в хаб,
                // либо на другой Mini того же типа
                cleanResources();

                ServerMode targetMode;
                targetMode = mini.getMaster().getLobby();
//                lobbies[1] = Lobby.HUB;

                for (Gamer gamer : gamers) {

                    if (getSetting(GameSetting.ALLOW_GAMER_AUTO_QUEUE_IF_ENABLE, true) && privateGamePartyId == -1) {
                        // если у игрока включен автоматический вход, первым
                        // делом его надо попробовать тпшнуть на сервер того же типа
                        targetMode = mini;
                    }

                    if (getSetting(GameSetting.ALLOW_SEND_AFTER_GAME, true)) {
                        ServerMode finalTargetMode = targetMode;
                        Party party = gamer.getParty();

                        if (party != null && !party.isLeader(gamer)) {
                            continue;
                        }

                        gamer.connectMode(true, targetMode).whenComplete((status, cause) -> {
                            if (cause != null) {
                                Logging.IMP.error("Failed send gamer to servers {}. Details: ", finalTargetMode.getName());
                                cause.printStackTrace();
                            }

                            if (status != ConnectStatus.SUCCESS) {
                                gamer.disconnect("game_lobby_connect_failed", "%details%", status.name());
                            }
                        });
                    }
                }

                // Отключаем приватную игру на коре
                if (isPrivateGame()) {
                    disablePrivateGameMode(true);
                }

                // выполняем таск очистки сервера еще через 5с
                new AbstractTask(Game.this) {
                    @Override
                    protected void run() {
                        gameWorld.getPlayers().forEach(player -> player.kickPlayer("§cRestart..."));
                        refresh();
                    }
                }.later(5 * 20);
            }
        }.later(delay * 20L);
    }

    public void broadcastWithLines(@NonNull Gamer gamer, @NonNull Consumer<Broadcaster> consumer) {
        Supplier<Collection<Gamer>> supplier = () -> Collections.singleton(gamer);

        Broadcaster bc = new Broadcaster(supplier);
        Broadcaster description = new Broadcaster(supplier);

        consumer.accept(description);

        if (description.size() == 0) {
            return;
        }

        Language language = gamer.getLanguage();

        String line = language.getMessage("game_broadcaster_line");

        bc.add(line);
        bc.add();
        bc.addCenter(language.getMessage("game_broadcast_title", "%mini%", mini.getDisplayName()));
        bc.add();

        for (Broadcaster.Line targetLine : description.getLines()) {
            bc.add(targetLine.getText());
            bc.align(targetLine.getAlign());
        }

        bc.add();
        bc.add(line);
        bc.broadcast();
    }

    public void addSpectator(Gamer gamer) {
        getSpectatorManager().addSpectator(gamer);
    }

    public final int getSlotsCount() {
        return getSetting(GameSetting.MAX_SLOTS, 2);
    }

    public final int getPlayersToStart() {
        return getSetting(GameSetting.PLAYERS_TO_START, getSlotsCount() > 2 ? getSlotsCount() / 2 : getSlotsCount());
    }

    public boolean checkPlayableZone(Block block) {
        if (block == null) {
            return true;
        }

        if (!getSetting(GameSetting.ENABLE_PLAYABLE_ZONE, true)) {
            return true;
        }

        val playableZone = getPlayableZone();
        return playableZone == null || playableZone.contains(block.getX(), block.getY(), block.getZ());

    }

    // ----------------- Основные игровые методы обязательные для переопределения ----------------- //

    /**
     * Вызывается, когда state изменено на STAND_BY
     */
    protected abstract void onGameLoad();

    /**
     * Вызывается, когда state изменено на GAME
     */
    protected abstract void onGameStart();

    /**
     * Вызывается, когда state изменено на ENDING
     */
    protected abstract void onGameEnd();


    // ----------------- Какая-то остальная дичь, которая может так и быть не переопределена ----------------- //

    protected void applyGameItems() {

    }

    protected void initializeWinner() {
        if (gamers.isEmpty()) {
            return;
        }

        if (getAliveTeams().size() > 1) {
            return;
        }

        winner = Iterables.getFirst(getAliveTeams(), null);
    }

    protected void broadcastStart(@NonNull Gamer gamer, @NonNull Broadcaster bc) {
        Language language = gamer.getLanguage();

        String descriptionKey = "game_" + mini.name() + "_description";

        if (!language.isMessageTranslated(descriptionKey)) {
            descriptionKey = "game_" + mini.getMaster().name() + "_description";
        }

        for (String line : language.getMessageList(descriptionKey)) {
            bc.addCenter(gamer, line);
        }
    }

    /**
     * Выводит сообщения после названия игры (по центру) и перед выводом статистики.
     * Здесь обычно выводится победившие игрок или команда.
     */
    protected void broadcastEnd(@NonNull Gamer gamer, @NonNull Broadcaster bc) {
        Language language = gamer.getLanguage();

        if (winner == null) {
            bc.addCenter(gamer, language.getMessage("game_end_draw"));
        } else {
            Set<Gamer> aliveGamers = winner.getAliveGamers();
            int winners = aliveGamers.size();

            bc.addCenter(gamer, language.getMessage(
                    "game_end_winner" + (winners > 1 ? "s" : "")
            ));

            Iterator<Gamer> aliveIterator = aliveGamers.iterator();

            if (!aliveIterator.hasNext()) {
                bc.addCenter(gamer, language.getMessage("game_end_empty_winners")); // такое вряд ли будет, но всё же
                return;
            }

            do {
                StringBuilder sb = new StringBuilder();
                sb.append(aliveIterator.next().getColoredName());

                if (aliveIterator.hasNext()) {
                    sb.append("§7, ").append(aliveIterator.next().getColoredName());
                }

                if (aliveIterator.hasNext()) {
                    sb.append("§7,");
                }

                bc.addCenter(sb.toString());
            } while (aliveIterator.hasNext());
        }
    }

    protected void broadcastEndTop(@NonNull Gamer gamer,
                                   @NonNull StatsField field,
                                   @NonNull Broadcaster broadcaster) {
        if (statsCache == null || statsCache.isEmpty()) {
            return;
        }

        val language = gamer.getLanguage();

        broadcaster.add();

        for (int i = 0; i < 3; i++) {
            GameStatsCache stats = null;

            if (statsCache.size() > i) {
                stats = statsCache.get(i);
            }

            val place = i + 1;
            val placeString = language.getMessage("game_end_top_place_" + place);

            broadcaster.addCenter(gamer, language.getMessage("game_end_top_format",
                    "%place%", placeString,
                    "%player%", stats == null ? "N/A" : stats.getGamer().getColoredName(),
                    "%value%", stats == null ? 0 : stats.getCachedStat(field, 0)));
        }
    }

    public CompletableFuture<Gamer> setRespawn(@NonNull Gamer gamer, int seconds) {
        CompletableFuture<Gamer> future = new CompletableFuture<>();

        getSpectatorManager().addSpectator(gamer, true);
        gamer.setGamerState(GamerState.SPECTATOR);

        new AbstractTickingLaterTask(this) {
            @Override
            protected void runTick() {
                if (!gamer.isOnline()) {
                    cancel();
                    return;
                }

                gamer.sendTitle(
                        Lang.of("game_respawning_title").compile(),
                        Lang.of("game_respawning_subtitle").set("%seconds%", left).compile(),
                        0, 25, 0);
            }

            @Override
            protected void handleCancel(@NotNull CancelReason reason) {
                if (reason == CancelReason.END) {
                    gamer.sendTitle(gamer.getLanguage().getMessage("game_respawned_title"), "");

                    getSpectatorManager().removeSpectator(gamer);
                    gamer.setGamerState(GamerState.PLAYER);

                    val player = gamer.getPlayer();
                    player.setMaxHealth((double) getSetting(GameSetting.PLAYER_HP, 20));
                    player.setHealth((double) getSetting(GameSetting.PLAYER_HP, 20));
                    player.setWalkSpeed(getSetting(GameSetting.PLAYER_SPEED, 0.2f));

                    if (getSetting(GameSetting.RESPAWN_PROTECTION_DAMAGE, false)) {
                        val noDamageTime = getSetting(GameSetting.RESPAWN_PROTECTION_DAMAGE_TIME, 3);

                        player.setNoDamageTicks(noDamageTime * 20);
                        getMetadata().set(player, "respawn", System.currentTimeMillis());
                    }

                    future.complete(gamer);

                    HeartBeat.callEvent(new GameRespawnEvent(Game.this, gamer));
                }
            }
        }.setTickDelay(0, 20).start(seconds);

        return future;
    }

    public CompletableFuture<Gamer> setRespawn(@NonNull Gamer gamer) {
        return setRespawn(gamer, getSetting(GameSetting.PLAYER_RESPAWN_TIME, 5));
    }

    public boolean canChangeMasterStats() {
        return masterStatsMode != null;
    }

    public boolean canChangeMiniStats() {
        return miniStatsMode != null;
    }

    public void addStat(@NonNull Gamer gamer, @NonNull String field) {
        addStat(gamer, field, 1);
    }

    public void addStat(@NonNull Gamer gamer, @NonNull String field, int delta) {
        addMasterStat(gamer, field, delta);
        addMiniStat(gamer, field, delta);
    }

    public void addStat(@NonNull Gamer gamer, @NonNull String field, int delta, int day) {
        addMasterStat(gamer, field, delta, day);
        addMiniStat(gamer, field, delta, day);
    }

    public void addMasterStat(@NonNull Gamer gamer, @NonNull String field, int delta) {
        if (masterStatsMode != null && !statsSavingDisabled) {
            gamer.addStat(masterStatsMode, field, delta);
        }
    }

    public void addMiniStat(@NonNull Gamer gamer, @NonNull String field, int delta) {
        if (miniStatsMode != null && !statsSavingDisabled) {
            gamer.addStat(miniStatsMode, field, delta);
        }
    }

    public void addMasterStat(@NonNull Gamer gamer, @NonNull String field, int delta, int day) {
        if (masterStatsMode != null && !statsSavingDisabled) {
            gamer.addStat(masterStatsMode, field, delta, day);
        }
    }

    public void addMiniStat(@NonNull Gamer gamer, @NonNull String field, int delta, int day) {
        if (miniStatsMode != null && !statsSavingDisabled) {
            gamer.addStat(miniStatsMode, field, delta, day);
        }
    }

    public void resetStat(@NonNull Gamer gamer, @NonNull String field) {
        resetMasterStat(gamer, field);
        resetMiniStat(gamer, field);
    }

    public void resetMiniStat(@NonNull Gamer gamer, @NonNull String field) {
        if (miniStatsMode != null) {
            gamer.resetStat(miniStatsMode, field);
        }
    }

    public void resetMasterStat(@NonNull Gamer gamer, @NonNull String field) {
        if (masterStatsMode != null) {
            gamer.resetStat(masterStatsMode, field);
        }
    }

    public int getMasterLifetimeStat(@NonNull Gamer gamer, @NonNull String field) {
        return getMasterStat(gamer, field, 0);
    }

    public int getMiniLifetimeStat(@NonNull Gamer gamer, @NonNull String field) {
        return getMiniStat(gamer, field, 0);
    }

    public int getMasterStat(@NonNull Gamer gamer, @NonNull String field, int day) {
        return masterStatsMode == null ? 0 : gamer.getStatByDay(masterStatsMode, field, day);
    }

    public int getMiniStat(@NonNull Gamer gamer, @NonNull String field, int day) {
        return miniStatsMode == null ? 0 : gamer.getStatByDay(miniStatsMode, field, day);
    }

    @Override
    public String toString() {
        return "Game[name=" + name + ']';
    }
}
