package club.abelix.api.server.lobby.impl.lobby;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar.manager.HotbarManager;
import club.abelix.api.base.playertop.builder.PlayerTopBuilder;
import club.abelix.api.base.playertop.builder.StatisticTopBuilder;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.server.lobby.BaseLobbyServer;
import club.abelix.api.server.lobby.exclusive.newyear.NewYearHubListener;
import club.abelix.api.server.lobby.impl.lobby.portal.GameLobbyPortalListener;
import club.abelix.api.server.lobby.impl.lobby.quest.QuestsCommand;
import club.abelix.api.server.lobby.impl.lobby.quest.QuestsNpc;
import club.abelix.api.server.lobby.impl.lobby.spectator.SpectatorNpc;
import club.abelix.api.server.lobby.impl.lobby.stats.listener.StatsNpcListener;
import club.abelix.api.server.lobby.impl.lobby.stats.manager.StatsNpcManager;
import club.abelix.api.server.lobby.portal.PortalAnimation;
import club.abelix.api.server.lobby.profile.ProfileGuisFactory;
import club.abelix.commons.data.quest.Quest;
import club.abelix.commons.data.quest.QuestRequirement;
import club.abelix.commons.data.quest.QuestSqlHandler;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.exclusive.ExclusivePeriod;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.network.packet.play.both.BQueueJoinQueue;
import club.abelix.network.packet.play.client.CQueueQuitQueue;
import club.abelix.network.type.mode.Mini;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameLobbyServer extends BaseLobbyServer {

    private final List<Quest> quests = new ArrayList<>();
    /**
     * Использовать ли очереди
     * <p>
     * При использовании данной системы войти на арены
     * нужного пордежима нельзя будет просто выбрав карту
     * или сервер, нужно будет войти в очередь и дождаться
     * игроков
     */
    @Getter
    private boolean useQueues;
    private boolean questsLoading;

    @Getter
    private StatsNpcManager statsNpcManager;

    public GameLobbyServer(@NonNull AbstractPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onLobbyLoad() {
        plugin.getCommandManager().addCommand(new QuestsCommand(this));

        useQueues = configuration.getBoolean("Queues");

        //регистрируем игровой портал
        String portalCenterLocationString = configuration.getString("PortalCenter");

        Location portalCenterLocation = null;

        if (portalCenterLocationString != null) {
            portalCenterLocation = LocationUtil.stringToLocation(portalCenterLocationString);
        }

        if (portalCenterLocation != null) {
            new GameLobbyPortalListener();
        }

        String statusHologramLocationString = configuration.getString("StatusHologram");

        Location statusHologramLocation = null;

        if (statusHologramLocationString != null) {
            statusHologramLocation = LocationUtil.stringToLocation(statusHologramLocationString);
        }

        if (portalCenterLocation != null) {
            new PortalAnimation(
                    plugin, portalCenterLocation, Lang.of("start_game_on_gamemode").compile()
            );
        }

        String questLocationString = configuration.getString("Quest");

        if (questLocationString != null) {
            QuestsNpc questsNpc = new QuestsNpc(LocationUtil.stringToLocation(questLocationString));
            questsNpc.create();
        }

        String spectatorLocationString = configuration.getString("Spectator");

        if (spectatorLocationString != null) {
            SpectatorNpc spectatorNpc = new SpectatorNpc(LocationUtil.stringToLocation(spectatorLocationString));
            spectatorNpc.create();
        }

        if (configuration.contains("Top")) {
            StatisticTopBuilder builder = PlayerTopBuilder.statisticTopBuilder();

            String modeChangerLocationString = configuration.getString("Top.ModeChangerLocation");

            if (modeChangerLocationString != null) {
                builder.modeChangerLocation(LocationUtil.stringToLocation(modeChangerLocationString));
            }

            for (String periodName : configuration.getStringList("Top.Periods")) {
                builder.addPeriod(periodName);
            }

            ConfigurationSection fieldsSection = configuration.getConfigurationSection("Top.Fields");

            if (fieldsSection != null) {
                for (String key : fieldsSection.getKeys(false)) {
                    Location topLocation = LocationUtil.stringToLocation(fieldsSection.getString(key + ".TopLocation"));

                    String fieldName = fieldsSection.getString(key + ".Name");
                    String localizationKey = fieldsSection.getString(key + ".LocalizationKey");

                    builder.addField(topLocation, localizationKey, fieldName);
                }
            }

            ConfigurationSection modesSection = configuration.getConfigurationSection("Top.Modes");

            if (modesSection != null) {
                for (String key : modesSection.getKeys(false)) {
                    System.out.println("Mode key: " + key);
                    String localizationKey = modesSection.getString(key + ".DisplayLocalizationKey");

                    builder.addMode(key, localizationKey);
                }
            }

            builder.create();
        }

        String rawStatsNpcLocation = configuration.getString("StatsNpcLocation");
        if (rawStatsNpcLocation != null) {
            statsNpcManager = new StatsNpcManager(LocationUtil.stringToLocation(rawStatsNpcLocation));
            new StatsNpcListener(plugin, statsNpcManager);

            // Если настроен, то настраиваем линии
            onStatsNpcSetup();
        }

        // Быстрая игра
//        LobbyHotbar.registerFastStart(LobbyHotbar.LOBBY_HOTBAR_ID);

        new AbstractTask(plugin) {
            @Override
            protected void run() {
                loadQuests();
            }
        }.async().timer(0, 20 * 60 * 5);
    }

    @Override
    public void onExclusivePeriod() {
        if (Objects.requireNonNull(getExclusivePeriod()) == ExclusivePeriod.NEW_YEAR) {
            new NewYearHubListener(this.getPlugin());
        }
    }

    // Настройка линий NPC статистики
    public void onStatsNpcSetup() {
    }

    // Статистика игрока
    public void openDetailedStatsGui(@NonNull Gamer gamer) {
        DiBukkitPlatform.getFactory()
                .getBeanService()
                .withBean(ProfileGuisFactory.class, f -> f.openDetailedStatsGui(gamer, CoreUtil.getMaster(), null));
    }

    public List<Quest> getQuests() {
        if (!quests.isEmpty()) {
            Quest quest = quests.get(0);

            // загружаем квесты заново если наступил новый день
            if (quest.getDay() != TimeUtil.getDayUTC()) {
                if (!questsLoading) {
                    HeartBeat.runAsync(this::loadQuests);
                }

                return null;
            }
        }

        return quests;
    }

    public void loadQuests() {
        questsLoading = true;

        // очищаем существующие квесты
        quests.clear();

        // для начала определяем режимы для которых будут доступны квесты
        List<StatsMode> statsModeList = new ArrayList<>();

        for (Mini mini : CoreUtil.getMaster().getMinis()) {
            StatsMode statsMode = StatsManager.INSTANCE.tryGetMode(mini.name());

            if (statsMode != null) {
                statsModeList.add(statsMode);
            }
        }

        int currentDay = (int) TimeUtil.getDayUTC();

        // загружаем для каждого режима квест, и если он есть добавляем в лист
        for (StatsMode statsMode : statsModeList) {
            Quest quest = QuestSqlHandler.INSTANCE.getQuest(statsMode, currentDay)
                    .join();

            if (quest != null) {
                quests.add(quest);
            }
        }

        if (quests.isEmpty()) {
            Quest quest = QuestSqlHandler.INSTANCE.getQuest(FalconAPI.getStatsMode(), currentDay)
                    .join();

            if (quest != null) {
                quests.add(quest);
            }
        }

        questsLoading = false;
    }

    public void joinQueue(@NonNull Gamer gamer, @NonNull Mini mini) {
        if (gamer.isVanished()) {
            gamer.sendLocalizedMessage("queue_vanish_cannot_join");
            return;
        }

        Connection.sendPacket(new BQueueJoinQueue(gamer.getPlayerId(), mini));

        Player player = gamer.getPlayer();

        player.setMetadata("queued", new FixedMetadataValue(plugin, true));

        HotbarManager.INSTANCE.updateIcons(gamer);
    }

    public void leaveQueue(@NonNull Gamer gamer) {
        Player player = gamer.getPlayer();

        player.removeMetadata("queued", plugin);

        Connection.sendPacket(new CQueueQuitQueue(gamer.getPlayerId()));

        HotbarManager.INSTANCE.updateIcons(gamer);
    }

    @Override
    public void onPlayerJoin(@NonNull Gamer gamer) {
        super.onPlayerJoin(gamer);

        int availableQuests = 0;

        for (Quest quest : quests) {
            List<QuestRequirement> requirements = quest.getRequirements();

            int completed = 0;
            int required = requirements.size();

            for (QuestRequirement requirement : quest.getRequirements()) {
                if (gamer.getTodayStat(requirement.getField()) >= requirement.getValue()) {
                    completed++;
                }
            }

            if (completed < required) {
                availableQuests++;
            }
        }

        if (availableQuests > 0) {
            gamer.sendLocalizedMessage("quests_available", "%quests%", availableQuests);
        }
    }
}
