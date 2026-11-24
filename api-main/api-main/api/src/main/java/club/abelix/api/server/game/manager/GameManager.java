package club.abelix.api.server.game.manager;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.nms.NmsConstant;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.factory.GameFactory;
import club.abelix.api.server.game.listener.base.PrimaryMasterListener;
import club.abelix.api.server.game.setting.GameSetting;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.mode.Mini;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GameManager {

    // ===========================================================
    public static final GameManager INSTANCE = new GameManager();
    // ===========================================================

    @Getter
    private final Map<Integer, Game> pendingPlayerGames
            = new ConcurrentHashMap<>();

    private final AtomicInteger totalSlotsCount = new AtomicInteger(0);

    private final Map<String, Game> registeredGames
            = new ConcurrentHashMap<>();

    private final AtomicInteger idCounter = new AtomicInteger(1);

    @Getter
    private final Path worldContainer = Bukkit.getWorldContainer().toPath();

    // ===========================================================

    private void init(AbstractPlugin plugin) {
        new PrimaryMasterListener(plugin, this);

        NmsConstant.c_customEntitySending = true;
        NmsConstant.c_customBossbarSending = true;
        NmsConstant.c_customTeamSending = true;
    }

    public int getPendingGames(String serverName) {
        return (int) pendingPlayerGames.values().stream()
                .filter(game -> game.getName().equalsIgnoreCase(serverName))
                .count();
    }

    /**
     * Получить будущую игру игрока
     * <br>Вызывается в PlayerSpawnLocationEvent
     */
    public Game getPendingGame(int playerId) {
        return pendingPlayerGames.get(playerId);
    }

    /**
     * Удалить будущую игру игрока
     * <br>Вызывается в PlayerJoinEvent
     */
    public Game removePendingGame(int playerId) {
        return pendingPlayerGames.remove(playerId);
    }

    /**
     * Проверить, есть ли у игрока будущая игра,
     * <br>Вызывается в GamerAsyncLoadEvent
     * <br>Если игры нет, то он как-то вошёл на прямую
     * <br>ёбанный волшебник
     */
    public boolean hasPendingGame(int playerId) {
        return pendingPlayerGames.containsKey(playerId);
    }

    /**
     * Установить будущую игру игрока
     */
    public void setPendingGame(int playerId, Game game) {
        pendingPlayerGames.put(playerId, game);
    }

    public Game getGame(String name) {
        return registeredGames.get(name.toLowerCase());
    }

    public void sendCreate(Game game) {
        Connection.createArena(game.getMini(), game.getName(), game.getGameMap(),
                game.getSetting(GameSetting.MAX_SLOTS, 0));

        Connection.setArenaState(game.getName(), game.getGameState());
    }

    private  YamlConfiguration getConfig(AbstractPlugin plugin) {
        YamlConfiguration config = new YamlConfiguration();

        try {
            config.load(new File(plugin.getDataFolder().toPath().toFile(), "settings.yml"));
        } catch (IOException | InvalidConfigurationException e) {
            Logging.IMP.error("Failed load game configuration. Details:");
            e.printStackTrace();
        }

        return config;
    }

    public void loadGames(AbstractPlugin plugin, GameFactory factory) {
        init(plugin);

        Set<Game> pendingGames = new HashSet<>();

        Path serversYml = plugin.getDataFolder().toPath().resolve("servers.yml");
        YamlConfiguration gameConfiguration = getConfig(plugin);

        try (BufferedReader br = Files.newBufferedReader(serversYml)) {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(br);

            ConfigurationSection section = configuration.getConfigurationSection("Servers");

            for (String key : section.getKeys(false)) {
                int i = section.getInt(key);

                for (int j = 0; j < i; j++) {
                    Game game = factory.createGame(Mini.valueOf(key));
                    game.setConfig(gameConfiguration);
                    game.initGame();

                    pendingGames.add(game);
                }
            }
        } catch (IOException e) {
            Logging.IMP.error("[Game Manager] Failed create game {}");
            e.printStackTrace();
        }

        // Для запуска игр, ждем подключение к кору, тк MapBalancer работает через него
        new AbstractTask(FalconAPI.getInstance()) {

            private int waitCounter = 1;

            @Override
            @SneakyThrows
            protected void run() {
                if (waitCounter >= 100) {
                    cancel();
                    Logging.IMP.warn("[GameManager] The connection to core could not be established," +
                            " we shutdown the server");
                    Bukkit.shutdown();
                    return;
                }

                if (!Connection.isActive()) {
                    Logging.IMP.info("[GameManager] Wait core connection before start game load..");
                    ++ waitCounter;
                    return;
                }

                Thread.sleep(2000); // Дадим немного времени, чтобы все карты зарегестрировались

                Logging.IMP.info("[GameManager] Start load games...");
                for (Game game : pendingGames) {
                    HeartBeat.runSync(() -> game.loadGame(() -> {
                        registerGame(game);
                        sendCreate(game);
                        game.getGameQueueManager().synchronization();
                    }));
                }

                cancel();
            }

        }.timer(20L, 20L);
    }

    /**
     * Зарегистрировать новую игру
     *
     * @param game игра
     */
    public void registerGame(@NonNull Game game) {
        registeredGames.put(game.getName().toLowerCase(), game);
        totalSlotsCount.addAndGet(game.getSlotsCount());

        if (Connection.isActive()) {
            sendCreate(game);
        }
    }

    public int nextId() {
        return idCounter.getAndIncrement();
    }

    /**
     * Получить список всех игр на сервере
     *
     * @return список всех игр
     */
    public Collection<Game> getRegisteredGames() {
        return Collections.unmodifiableCollection(registeredGames.values());
    }
}