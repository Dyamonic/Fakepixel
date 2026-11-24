package club.abelix.games.buildffa.game;

import club.abelix.api.base.belowname.BelowNameManager;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.playertop.builder.PlayerTopBuilder;
import club.abelix.api.base.util.worldtime.WorldRealTimeSync;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.commons.data.stat.StatisticPeriod;
import club.abelix.commons.util.random.RandomUtil;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.games.buildffa.BuildFFA;
import club.abelix.games.buildffa.game.command.ResetKitCommand;
import club.abelix.games.buildffa.game.command.SetRatingRankCommand;
import club.abelix.games.buildffa.game.command.UpdateRatingCommand;
import club.abelix.games.buildffa.game.command.PrestigeCommand;
import club.abelix.games.buildffa.game.command.QuestsCommand;
import club.abelix.games.buildffa.game.command.QuestDebugCommand;
import club.abelix.games.buildffa.game.command.CosmeticsCommand;
import club.abelix.games.buildffa.game.command.CpsCheckCommand;
import club.abelix.games.buildffa.game.command.ReachCheckCommand;
import club.abelix.games.buildffa.game.command.BuildModeCommand;
import club.abelix.games.buildffa.game.command.EventManagerCommand;
import club.abelix.games.buildffa.game.command.EventCommand;
import club.abelix.games.buildffa.game.command.EventStatsCommand;
import club.abelix.games.buildffa.game.command.BuildFFADebugCommand;
import club.abelix.games.buildffa.game.command.BuildFFAAdminCommand;
import club.abelix.games.buildffa.game.command.SpectateCommand;
import club.abelix.games.buildffa.game.command.WipeBuildFFAStatsCommand;
import club.abelix.games.buildffa.game.config.BuildFFAConfig;
import club.abelix.games.buildffa.game.config.WorldConfig;
import club.abelix.games.buildffa.game.data.BuildFFAGamer;
import club.abelix.games.buildffa.game.event.GameMapChangeEvent;
import club.abelix.games.buildffa.game.item.MelonActionItem;
import club.abelix.games.buildffa.game.kit.BaseKit;
import club.abelix.games.buildffa.game.kit.registry.KitRegistry;
import club.abelix.games.buildffa.game.kit.standard.BasicKitItem;
import club.abelix.games.buildffa.game.listener.PlayerJoinListener;
import club.abelix.games.buildffa.game.listener.division.DivisionListener;
import club.abelix.games.buildffa.game.listener.game.*;
import club.abelix.games.buildffa.game.listener.game.layout.LayoutChangeListener;
import club.abelix.games.buildffa.game.manager.AntiAfkManager;
import club.abelix.games.buildffa.game.manager.actionbar.ActionBarManager;
import club.abelix.games.buildffa.game.manager.additional.AdditionalItemManager;
import club.abelix.games.buildffa.game.manager.player.RewardManager;
import club.abelix.games.buildffa.game.manager.progression.PrestigeManager;
import club.abelix.games.buildffa.game.manager.progression.KillSoundManager;
import club.abelix.games.buildffa.game.manager.progression.QuestManager;
import club.abelix.games.buildffa.game.manager.progression.BountyManager;
import club.abelix.games.buildffa.game.manager.progression.CosmeticManager;
import club.abelix.games.buildffa.game.manager.progression.KillEffectManager;
import club.abelix.games.buildffa.game.manager.progression.ChatMessageManager;
import club.abelix.games.buildffa.game.manager.event.BuildFFAEventManager;
import club.abelix.games.buildffa.game.manager.staff.SpectateManager;
import club.abelix.games.buildffa.game.manager.staff.BuildFFAWipeManager;
import club.abelix.games.buildffa.game.manager.staff.CpsCheckManager;
import club.abelix.games.buildffa.game.receivable.Receivable;
import club.abelix.games.buildffa.game.receivable.entity.hologram.RatingHologram;
import club.abelix.games.buildffa.game.receivable.entity.phantom.*;
import club.abelix.games.buildffa.game.task.BlockTracker;
import club.abelix.games.buildffa.game.vote.VoteManager;
import club.abelix.games.buildffa.game.vote.VoteTask;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.util.*;

@Getter
@Setter
public class BuildFFAGame {
    public static final String EVENT_MANAGER_PERMISSION = "buildffa.eventmanager";
    public static final String EVENT_STATS_PERMISSION = "buildffa.eventstats";
    public static final String WIPE_BUILDFFA_STATS_PERMISSION = "buildffa.wipebuildffastats";
    public static final String UNLOCK_ALL_COSMETICS_PERMISSION = "buildffa.unlockallcosmetics";
    public static final String BUILD_MODE_PERMISSION = "buildffa.buildmode";
    public static final String SPECTATE_PERMISSION = "buildffa.spectate";
    public static final String REACH_CHECK_PERMISSION = "buildffa.reachcheck";
    public static final String CPS_CHECK_PERMISSION = "buildffa.cpscheck";
    private final Map<UUID, GameMode> buildModePlayers = new HashMap<>();
    private final Set<UUID> respawningPlayers = new HashSet<>();
    private final Set<Player> playersAppliedKits = new HashSet<>();
    private final Map<UUID, UUID> inventoryRequests = new java.util.concurrent.ConcurrentHashMap<>();
    private final Collection<Receivable> visibleObjects = new HashSet<>();
    private final BuildFFA plugin;
    private RewardManager rewardManager;
    private DivisionListener divisionListener;
    private BuildFFAConfig ffaConfig;
    private ActionBarManager actionBarManager;
    private WorldConfig worldConfig;
    private VoteManager voteManager;
    private BlockTracker blockTracker;
    private PrestigeManager prestigeManager;
    private KillSoundManager killSoundManager;
    private QuestManager questManager;
    private BountyManager bountyManager;
    private CosmeticManager cosmeticManager;
    private KillEffectManager killEffectManager;
    private ChatMessageManager chatMessageManager;
    private BuildFFAEventManager eventManager;
    private SpectateManager spectateManager;
    private final Map<UUID, Location> frozenPlayers = new java.util.concurrent.ConcurrentHashMap<>();

    public boolean isFrozen(Player player) {
        return frozenPlayers.containsKey(player.getUniqueId());
    }
    private BuildFFAWipeManager wipeManager;
    private CpsCheckManager cpsCheckManager;

    public BuildFFAGame(@NonNull BuildFFA plugin) {
        this.plugin = plugin;
    }

    public boolean hasPermission(Player player, String permission) {
        if (player == null || permission == null) return false;
        String canonicalPermission = permission.toLowerCase(Locale.ROOT);
        if (player.hasPermission(canonicalPermission)) return true;
        Gamer gamer = Gamer.getGamer(player);
        return gamer != null && gamer.getRanks() != null && gamer.getRanks().hasPermission(canonicalPermission);
    }

    public void loadGame() {
        KitRegistry.REGISTRY.getKits().forEach(kit -> kit.bindInventoryLifecycle(this));
        ffaConfig = new BuildFFAConfig(getPlugin());
        ffaConfig.loadConfig();

        ffaConfig.getGameWorlds().forEach(config -> {
            ((CraftWorld) config.world()).getHandle().getWorldData().g(false);
            initializeLeaderboards(config);
            initializeVisibleObjects(config);

            WorldRealTimeSync.sync(config.world(), ZoneId.of("Europe/Moscow"));
        });

        blockTracker = new BlockTracker();
        HeartBeat.runTimerSync(plugin, blockTracker, 0, 20);

        eventManager = new BuildFFAEventManager(this);
        spectateManager = new SpectateManager(this);
        wipeManager = new BuildFFAWipeManager(this);
        rewardManager = new RewardManager(this);
        prestigeManager = new PrestigeManager(this);
        killSoundManager = new KillSoundManager(this);
        killEffectManager = new KillEffectManager(this);
        chatMessageManager = new ChatMessageManager(this);
        cosmeticManager = new CosmeticManager(this);
        questManager = new QuestManager(this);
        bountyManager = new BountyManager(this);
        cpsCheckManager = new CpsCheckManager(this);
        cpsCheckManager.enable();

        val task = new VoteTask(this);
        HeartBeat.runTimerSync(plugin, task, 0, 20);

        voteManager = new VoteManager();

        actionBarManager = new ActionBarManager(this, task);
        actionBarManager.startUpdatingActionBar();

        val antiAfkManager = new AntiAfkManager(this);
        antiAfkManager.enable();

        updateWorldConfig(ffaConfig.getGameWorlds()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("There is no maps available")));
        eventManager.startAutomaticScheduler();

        initializeHealthTracker();
        initializeListeners();
        initializeCommands();
    }

    private void initializeCommands() {
        BuildFFAAdminCommand.setManager(cpsCheckManager.getCombatIntegrity());
        new SetRatingRankCommand(this);
        PrestigeCommand.setPrestigeManager(prestigeManager);
        QuestsCommand.setQuestManager(questManager);
        QuestDebugCommand.setQuestManager(questManager);
        CosmeticsCommand.setCosmeticManager(cosmeticManager);
        EventManagerCommand.setEventManager(eventManager);
        EventCommand.setEventManager(eventManager);
        EventStatsCommand.setEventManager(eventManager);
        BuildFFADebugCommand.setEventManager(eventManager);
        plugin.getServer().getPluginManager().registerEvents(new EventManagerCommand(), plugin);
        club.abelix.games.buildffa.game.command.FreezeCommand.setGame(this);
        club.abelix.games.buildffa.game.command.UnfreezeCommand.setGame(this);
        plugin.getCommandManager().addCommand(club.abelix.games.buildffa.game.command.FreezeCommand.class);
        plugin.getCommandManager().addCommand(club.abelix.games.buildffa.game.command.UnfreezeCommand.class);
        SpectateCommand.setManager(spectateManager);
        WipeBuildFFAStatsCommand.setManager(wipeManager);
        CpsCheckCommand.setManager(cpsCheckManager);
        ReachCheckCommand.setManager(cpsCheckManager);
        BuildModeCommand.setGame(this);
        plugin.getServer().getPluginManager().registerEvents(new ReachCheckCommand(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CpsCheckCommand(), plugin);

        plugin.getCommandManager().addCommand(UpdateRatingCommand.class);
        plugin.getCommandManager().addCommand(SetRatingRankCommand.class);
        plugin.getCommandManager().addCommand(ResetKitCommand.class);
        plugin.getCommandManager().addCommand(PrestigeCommand.class);
        plugin.getCommandManager().addCommand(QuestsCommand.class);
        plugin.getCommandManager().addCommand(QuestDebugCommand.class);
        plugin.getCommandManager().addCommand(CosmeticsCommand.class);
        plugin.getCommandManager().addCommand(EventManagerCommand.class);
        plugin.getCommandManager().addCommand(EventCommand.class);
        plugin.getCommandManager().addCommand(EventStatsCommand.class);
        plugin.getCommandManager().addCommand(BuildFFADebugCommand.class);
        plugin.getCommandManager().addCommand(BuildFFAAdminCommand.class);
        plugin.getCommandManager().addCommand(SpectateCommand.class);
        plugin.getCommandManager().addCommand(WipeBuildFFAStatsCommand.class);
        plugin.getCommandManager().addCommand(CpsCheckCommand.class);
        plugin.getCommandManager().addCommand(ReachCheckCommand.class);
        plugin.getCommandManager().addCommand(BuildModeCommand.class);
    }

    private void initializeLeaderboards(final @NotNull WorldConfig config) {
        PlayerTopBuilder.propertyTopBuilder()
                .property(BuildFFAGamer.BEST_KILL_STREAK_PROPERTY_NAME)
                .location(config.killStreakLeaderboard())
                .localizationKey("buildffa_killstreak_leaderboard_name")
                .updateInterval(6000)
                .create();

        PlayerTopBuilder.statisticTopBuilder()
                .addPeriod(StatisticPeriod.ALL)
                .addPeriod(StatisticPeriod.MONTH)
                .addPeriod(StatisticPeriod.WEEK)
                .addPeriod(StatisticPeriod.TODAY)
                .addMode("BUILDFFA")
                .addField(config.killsLeaderboard(), "buildffa_kills_leaderboard_name", "kills")
                .addField(config.ratingLeaderboard(), "buildffa_rating_leaderboard_name", "rating")
                .updateInterval(6000)
                .create();
    }

    private void initializeVisibleObjects(final @NotNull WorldConfig worldConfig) {
        visibleObjects.add(new KitEditorPhantom(this, worldConfig.kitEditorConfig()));
        visibleObjects.add(new ShopPhantom(worldConfig.shopConfig()));
        visibleObjects.add(new LayoutChangePhantom(worldConfig.layoutChangeConfig()));
//        visibleObjects.add(new ExchangerPhantom(worldConfig.exchangerConfig()));
        visibleObjects.add(new DivisionPhantom(this, worldConfig.divisionConfig()));

        visibleObjects.add(new QuestsPhantom(this, worldConfig.questsConfig()));
        visibleObjects.add(new CosmeticPhantom(this, worldConfig.cosmeticConfig()));

        visibleObjects.add(new RatingHologram(worldConfig.ratingHologram()));
    }

    private void initializeHealthTracker() {
        BelowNameManager.IMP.createHealthTracker();
    }

    private void initializeListeners() {
        new LayoutChangeListener(this);
        divisionListener = new DivisionListener(this);
        new PlayerJoinListener(this);
        new PlayerDeathListener(this);
        new PlayerMoveListener(this);
        new BlockPlaceListener(this);
        new ItemListener(this);
        new PlayableZoneListener(this);
        new EnderPearlListener(this);
        new WeatherListener(this);
        new InventoryListener(this);
        new ChatListener(this);
        new ExplosionListener(this);
    }

    public void setSelectedKit(Player player, BaseKit kit) {
        val gamer = Gamer.getGamer(player);

        gamer.setProperty(BuildFFAGamer.SELECTED_KIT_PROPERTY_NAME, kit.getId());

        gamer.playSound(YSound.LEVEL_UP);
        player.closeInventory();

        gamer.sendLocalizedMessage("buildffa_kit_selected", "%kit%", kit.getDisplayName(gamer.getLanguage()));
    }

    public BaseKit getSelectedKit(Gamer gamer) {
        val property = gamer.getProperty(BuildFFAGamer.SELECTED_KIT_PROPERTY_NAME);

        if (property == 0) {
            return getDefaultKit();
        }

        return KitRegistry.REGISTRY.getKitById(property);
    }

    public BaseKit getDefaultKit() {
        BaseKit kit = KitRegistry.REGISTRY.getKitById(8);
        if (kit == null) kit = new BasicKitItem();
        kit.bindInventoryLifecycle(this);
        return kit;
    }

    public void executeBuildMode(Gamer gamer, String[] arguments) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> executeBuildMode(gamer, arguments));
            return;
        }
        Player player = gamer.getPlayer();
        if (!plugin.isEnabled() || !player.isOnline()) return;
        if (!hasPermission(player, BUILD_MODE_PERMISSION)) {
            disableBuildMode(player, false);
            gamer.sendLocalizedMessage("buildffa_buildmode_no_permission");
            return;
        }
        if (arguments.length != 0) {
            gamer.sendLocalizedMessage("buildffa_buildmode_usage");
            return;
        }
        if (disableBuildMode(player, true)) return;
        if (isSpectating(player)) {
            gamer.sendLocalizedMessage("buildffa_buildmode_spectator");
            return;
        }
        buildModePlayers.put(player.getUniqueId(), player.getGameMode());
        invalidateInventoryRequest(player);
        resetPlayerKitApplication(player);
        if (player.getGameMode() == GameMode.ADVENTURE) player.setGameMode(GameMode.SURVIVAL);
        gamer.sendLocalizedMessage("buildffa_buildmode_enabled");
    }

    public boolean isBuildMode(Player player) {
        if (player == null || !buildModePlayers.containsKey(player.getUniqueId())) return false;
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Build mode must be checked on the server thread");
        if (hasPermission(player, BUILD_MODE_PERMISSION)) return true;
        disableBuildMode(player, false);
        Gamer gamer = Gamer.getGamer(player);
        if (gamer != null && player.isOnline()) gamer.sendLocalizedMessage("buildffa_buildmode_no_permission");
        return false;
    }

    public boolean disableBuildMode(Player player, boolean notify) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Build mode must be disabled on the server thread");
        GameMode previous = buildModePlayers.remove(player.getUniqueId());
        if (previous == null) return false;
        if (previous == GameMode.ADVENTURE && player.getGameMode() == GameMode.SURVIVAL)
            player.setGameMode(previous);
        Gamer gamer = Gamer.getGamer(player);
        if (notify && gamer != null && player.isOnline()) gamer.sendLocalizedMessage("buildffa_buildmode_disabled");
        return true;
    }

    public void clearBuildModes() {
        for (UUID id : new HashSet<>(buildModePlayers.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) disableBuildMode(player, false);
            else buildModePlayers.remove(id);
        }
    }

    public boolean isSpectating(Player player) {
        return player != null && (player.getGameMode() == GameMode.SPECTATOR
                || spectateManager != null && spectateManager.isSpectating(player));
    }

    public void applySelectedKit(Gamer gamer) {
        if (!Bukkit.isPrimaryThread()) {
            Player player = gamer.getPlayer();
            UUID request = beginInventoryRequest(player);
            long revision = eventManager.getInventoryRevision();
            if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, () -> {
                if (isInventoryRequestCurrent(player, request) && revision == eventManager.getInventoryRevision())
                    applySelectedKit(gamer);
            });
            return;
        }
        Player player = gamer.getPlayer();
        if (!player.isOnline() || player.isDead() || respawningPlayers.contains(player.getUniqueId())
                || isSpectating(player) || isBuildMode(player)) return;
        if (playersAppliedKits.contains(player)) return;

        val selectedKit = getSelectedKit(gamer);

        player.closeInventory();
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        selectedKit.apply(gamer, this, beginInventoryRequest(player));

        playersAppliedKits.add(player);
    }

    public boolean isLobbyZone(final @NotNull Location location) {
        return worldConfig.lobbyZone().contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public void updateWorldConfig(final WorldConfig worldConfig) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> updateWorldConfig(worldConfig));
            return;
        }
        HeartBeat.callEvent(new GameMapChangeEvent(this, this.worldConfig, worldConfig));

        this.worldConfig = worldConfig;
        ((CraftWorld) worldConfig.world()).getHandle().getWorldData().g(false);

        Bukkit.getOnlinePlayers().forEach(this::resetPlayer);
        if (eventManager != null) eventManager.completeArenaTransfer();
        if (spectateManager != null) spectateManager.completeArenaTransfer();
    }

    public void resetPlayerKitApplication(Player player) {
        playersAppliedKits.remove(player);
    }

    public void setupPlayerInventory(final @NotNull Player player) {
        if (spectateManager != null && spectateManager.isSpectating(player)) return;
        beginInventoryRequest(player);
        if (eventManager != null) eventManager.beforeInventoryReset(player);
        val language = Gamer.getGamer(player).getLanguage();
        val inventory = player.getInventory();
        val air = new ItemStack(Material.AIR);

        player.closeInventory();
        player.setItemOnCursor(air);
        inventory.clear();

        inventory.setArmorContents(
                new ItemStack[]{
                        air, air,
                        air, air
                }
        );

        player.setHealth(player.getMaxHealth());
        player.setGameMode(isBuildMode(player) ? GameMode.SURVIVAL : GameMode.ADVENTURE);

        inventory.setItem(4, MelonActionItem.getItemStack(this, language));

        player.updateInventory();
    }

    public UUID beginInventoryRequest(Player player) {
        UUID request = UUID.randomUUID();
        inventoryRequests.put(player.getUniqueId(), request);
        return request;
    }

    public boolean isInventoryRequestCurrent(Player player, UUID request) {
        return player.isOnline() && !player.isDead() && request != null
                && request.equals(inventoryRequests.get(player.getUniqueId()));
    }

    public void completeInventoryRequest(Player player, UUID request) {
        inventoryRequests.remove(player.getUniqueId(), request);
    }

    public void invalidateInventoryRequest(Player player) {
        inventoryRequests.remove(player.getUniqueId());
    }

    public void spawnReceivable(final Player player) {
        visibleObjects.forEach(receivable -> receivable.addReceiver(player));
    }

    public void teleportToSpawn(Player player) {
        player.teleport(resolveSpawnLocation());
    }

    public Location resolveSpawnLocation() {
        val spawnLocation = worldConfig.spawnLocation().clone();
        World world = worldConfig.world();
        if (Bukkit.getWorld(world.getUID()) != world)
            throw new IllegalStateException("The current BuildFFA arena is not loaded");
        spawnLocation.setWorld(world);

        spawnLocation.setYaw(RandomUtil.getRandomElement(-90, 0, 90, 180));

        return spawnLocation;
    }

    public void clearAdditionalItems(Player player) {
        AdditionalItemManager.getInstance().clearPlayerItems(player);
    }

    public void resetPlayer(Player player) {
        if (!player.isOnline() || player.isDead() || respawningPlayers.contains(player.getUniqueId())) {
            invalidateInventoryRequest(player);
            resetPlayerKitApplication(player);
            return;
        }
        if (spectateManager != null && spectateManager.isSpectating(player)) return;
        resetPlayerKitApplication(player);
        teleportToSpawn(player);
        completePlayerReset(player);
    }

    public void completePlayerReset(Player player) {
        if (!player.isOnline() || player.isDead() || isSpectating(player)) return;
        resetPlayerKitApplication(player);
        setupPlayerInventory(player);
        clearAdditionalItems(player);
        if (eventManager != null) eventManager.onInventoryReady(player);
    }

    public void broadcastLocalized(final String key, Object... args) {
        Gamer.getGamers().forEach(gamer -> {
            if (!gamer.isOnline()) return;

            gamer.sendLocalizedMessage(key, args);
        });
    }

    public void broadcastLocalizedTitle(String titleKey, String subtitleKey, Object... args) {
        Gamer.getGamers().forEach(gamer -> {
            if (!gamer.isOnline()) return;

            gamer.sendLocalizedTitle(titleKey, subtitleKey, args);
        });
    }
}
