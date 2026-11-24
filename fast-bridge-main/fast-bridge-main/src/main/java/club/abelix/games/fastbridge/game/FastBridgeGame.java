package club.abelix.games.fastbridge.game;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.games.fastbridge.FastBridge;
import club.abelix.games.fastbridge.commands.BridgeStatsCommand;
import club.abelix.games.fastbridge.commands.SelectAnimationCommand;
import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.commons.config.FastBridgeConfig;
import club.abelix.games.fastbridge.commons.config.IslandConfig;
import club.abelix.games.fastbridge.commons.island.Island;
import club.abelix.games.fastbridge.commons.island.IslandSelector;
import club.abelix.games.fastbridge.commons.island.IslandService;
import club.abelix.games.fastbridge.commons.utils.RecordFormat;
import club.abelix.games.fastbridge.cosmetic.api.CosmeticItem;
import club.abelix.games.fastbridge.game.animation.AnimatedBlockRemoversStorage;
import club.abelix.games.fastbridge.game.animation.standard.FallingBlockRemoverAnimation;
import club.abelix.games.fastbridge.game.listener.standard.*;
import club.abelix.network.type.mode.Uncommon;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@Getter
public class FastBridgeGame {

    private static final StatsMode STATS_MODE = StatsManager.INSTANCE.getMode(Uncommon.FASTBRIDGE);
    private static final HashMap<IslandConfig, ConcurrentLinkedDeque<Island>> FREE_ISLANDS_MAP = new LinkedHashMap<>();
    private static final HashMap<IslandConfig, ConcurrentLinkedDeque<Island>> ISLANDS_MAP = new HashMap<>();
    @Getter
    private static FastBridgeGame instance;
    private final FastBridge plugin;
    private final Map<IslandConfig, Map<FastBridgeGamer, Long>> dailyTop = new HashMap<>();
    private FastBridgeConfig config;
    private IslandConfig defaultIslandConfig;

    @Getter
    private IslandSelector islandSelector;

    @Getter
    private IslandService islandService;

    @Getter
    private AnimatedBlockRemoversStorage blocksRemoverStorage;

    public FastBridgeGame(@NonNull FastBridge plugin) {
        this.plugin = plugin;
    }

    public static StatsMode getStatsMode() {
        return STATS_MODE;
    }

    public void loadGame() {
        instance = this;

        config = new FastBridgeConfig(plugin);
        config.loadConfig();

        defaultIslandConfig = config.getIslandConfigs().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Config isn't initialized correctly. Can't find any IslandConfig"));

        config.getIslandConfigs().forEach(cnf -> {
            dailyTop.put(cnf, new HashMap<>());
        });

        islandSelector = IslandSelector.create(config);
        islandService = IslandService.create(islandSelector);
        blocksRemoverStorage = AnimatedBlockRemoversStorage.create();

        blocksRemoverStorage.register(new FallingBlockRemoverAnimation());

        getPlugin().getCommandManager().addCommand(new SelectAnimationCommand(blocksRemoverStorage));
        getPlugin().getCommandManager().addCommand(BridgeStatsCommand.class);

        CosmeticItem.initItems();
        initializeListeners();
        startActionBarUpdater();
    }

    private void initializeListeners() {
        new DefaultFastBridgeListener(this, islandSelector);
        new PlayerManagementListener(this, islandSelector, islandService);
        new InventorySlotsManager(this);
        new GamerRewardListener(this);
        new WorldListener(getPlugin());
    }

    private void startActionBarUpdater() {
        HeartBeat.runTimerSync(() -> {
            Gamer.getGamers().stream()
                    .map(FastBridgeGamer::get)
                    .filter(gamer -> gamer.getIsland() != null)
                    .filter(gamer -> gamer.getCurrentBuildingSession() != null)
                    .forEach(gamer -> {
                        val time = System.currentTimeMillis() - gamer.getCurrentBuildingSession().getStartBuildingAt();
                        gamer.getGamer().sendActionBar("§d" + RecordFormat.formatTime(time));
                    });
        }, 0, 1);
    }

}
