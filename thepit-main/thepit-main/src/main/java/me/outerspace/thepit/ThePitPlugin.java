package me.outerspace.thepit;

import me.outerspace.thepit.commands.PitCommand;
import me.outerspace.thepit.listeners.GameListener;
import me.outerspace.thepit.listeners.StatisticsListener;
import me.outerspace.thepit.hooks.ThePitExpansion;
import me.outerspace.thepit.hooks.AbelixHook;
import me.outerspace.thepit.managers.*;
import me.outerspace.thepit.model.NetworkProfileSnapshot;
import me.outerspace.thepit.npc.PitNpcService;
import me.outerspace.thepit.registry.PitRegistry;
import me.outerspace.thepit.setup.PitSetupService;
import me.outerspace.thepit.perk.PerkListener;
import me.outerspace.thepit.perk.PerkManager;
import me.outerspace.thepit.perk.PerkMenu;
import me.outerspace.thepit.perk.PassiveManager;
import me.outerspace.thepit.fishing.FishingManager;
import me.outerspace.thepit.compat.CombatKnockbackCompatibility;
import me.outerspace.thepit.net.NativePacketRouter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Difficulty;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.lang.reflect.Method;

public class ThePitPlugin extends JavaPlugin {
    private boolean legacyAbelixPrefixWarningLogged;
    public enum GoldSource {
        KILL(true), ASSIST(true), COLLECTIBLE(true), FISHING(true), EL_GATO(false), QUEST(false), EVENT(false), ADMINISTRATIVE(false), REFUND(false), TRANSFER(false), SHOP_SALE(false);

        private final boolean passive;

        GoldSource(boolean passive) {
            this.passive = passive;
        }

        public boolean eligibleForPassive() {
            return passive;
        }
    }
    private PitRegistry registry;
    private ArenaManager arenaManager;
    private DefaultLoadoutManager defaultLoadoutManager;
    private PlayerInventoryManager playerInventoryManager;
    private MapRotationManager mapRotationManager;
    private StatsManager statsManager;
    private PitGoldService pitGoldService;
    private StorageManager storageManager;
    private PurchaseRepository purchaseRepository;
    private LeaderboardManager leaderboardManager;
    private ShopManager shopManager;
    private StatsMenuManager statsMenuManager;
    private PerkManager perkManager;
    private PerkListener perkListener;
    private PerkMenu perkMenu;
    private PassiveManager passiveManager;
    private QuestManager questManager;
    private SlimeLaunchPadManager slimeLaunchPadManager;
    private CollectibleCoinManager collectibleCoinManager;
    private me.outerspace.thepit.managers.ScoreboardManager scoreboardManager;
    private me.outerspace.thepit.managers.TablistManager tablistManager;
    private AbelixHook abelixHook;
    private NetworkProfileCache networkProfileCache;
    private PitPlayerNameFormatter playerNameFormatter;
    private AbelixRankPermissionManager abelixRankPermissionManager;
    private PitNpcService npcService;
    private PitSetupService setupService;
    private PitYamlManager yamlManager;
    private GameListener gameListener;
    private FishingManager fishingManager;
    private PrestigeManager prestigeManager;
    private RenownShopManager renownShopManager;
    private ExpandedEnderChestManager expandedEnderChestManager;
    private LevelUpFeedbackManager levelUpFeedbackManager;
    private FeatureMenuManager featureMenuManager;
    private AchievementManager achievementManager;
    private ActionBarManager actionBarManager;
    private ContractManager contractManager;
    private PitCommandManager pitCommandManager;
    private KillstreakManager killstreakManager;
    private MysticWellManager mysticWellManager;
    private ProgressManager progressManager;
    private RewardMultiplierService rewardMultiplierService;
    private MinorEventManager minorEventManager;
    private MysticItemManager mysticItemManager;
    private CombatLogManager combatLogManager;
    private CombatKnockbackCompatibility combatKnockbackCompatibility;
    private NativePacketRouter nativePacketRouter;
    private final Set<UUID> buildModePlayers = new HashSet<>();
    private Method placeholderMethod;

    @Override
    public void onEnable() {
        me.outerspace.thepit.util.LegacyDataFolderMigrator.migrate(this);
        registry = new PitRegistry(this);
        if (!registry.initialize()) {
            getLogger().severe("The Pit could not load its YAML configuration.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        me.outerspace.thepit.util.SecureItemTag.initialize(getLogger());
        storageManager = new StorageManager(this);
        storageManager.connect();
        purchaseRepository = new PurchaseRepository(this);
        purchaseRepository.initialize();
        setupService = new PitSetupService(this);
        if (!setupService.initialize()) {
            getLogger().severe("The Pit could not load its arena setup.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        removeObsoleteSetupEntries();
        yamlManager = new PitYamlManager(this);
        if (!yamlManager.initialize()) {
            getLogger().severe("The Pit could not load arenas.yml.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        arenaManager = new ArenaManager(this, yamlManager);
        statsManager = new StatsManager(this);
        pitGoldService = new PitGoldService(this);
        defaultLoadoutManager = new DefaultLoadoutManager(this);
        playerInventoryManager = new PlayerInventoryManager(this);
        mapRotationManager = new MapRotationManager(this);
        if (!mapRotationManager.initialize()) {
            getLogger().severe("The Pit could not initialize map rotation.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        leaderboardManager = new LeaderboardManager(this);
        shopManager = new ShopManager(this);
        statsMenuManager = new StatsMenuManager(this);
        questManager = new QuestManager(this);
        perkManager = new PerkManager(this);
        perkManager.initialize();
        passiveManager = new PassiveManager(this);
        perkListener = new PerkListener(this, perkManager);
        perkMenu = new PerkMenu(this, perkManager);
        fishingManager = new FishingManager(this);
        prestigeManager = new PrestigeManager(this);
        renownShopManager = new RenownShopManager(this);
        expandedEnderChestManager = new ExpandedEnderChestManager(this);
        slimeLaunchPadManager = new SlimeLaunchPadManager(this);
        collectibleCoinManager = new CollectibleCoinManager(this);
        scoreboardManager = new me.outerspace.thepit.managers.ScoreboardManager(this);
        tablistManager = new me.outerspace.thepit.managers.TablistManager(this);
        levelUpFeedbackManager = new LevelUpFeedbackManager(this);
        featureMenuManager = new FeatureMenuManager(this);
        achievementManager = new AchievementManager(this);
        actionBarManager = new ActionBarManager(this);
        contractManager = new ContractManager(this);
        pitCommandManager = new PitCommandManager(this);
        killstreakManager = new KillstreakManager(this);
        mysticWellManager = new MysticWellManager(this);
        progressManager = new ProgressManager(this);
        rewardMultiplierService = new RewardMultiplierService(this);
        minorEventManager = new MinorEventManager(this);
        mysticItemManager = new MysticItemManager(this);
        combatLogManager = new CombatLogManager(this);
        combatKnockbackCompatibility = new CombatKnockbackCompatibility(this);
        abelixHook = new AbelixHook(this);
        if (registry().abelixRanks().getBoolean("abelix.enabled", true)) abelixHook.setup();
        networkProfileCache = new NetworkProfileCache(this);
        playerNameFormatter = new PitPlayerNameFormatter(this);
        abelixRankPermissionManager = new AbelixRankPermissionManager(this, abelixHook);
        abelixRankPermissionManager.start();

        nativePacketRouter = new NativePacketRouter(this);
        npcService = new PitNpcService(this);
        arenaManager.changeArenaSafely(mapRotationManager.currentMap(), false, loaded -> {
            if (!loaded) {
                getLogger().severe("The Pit could not load the persisted active arena '" + mapRotationManager.currentMap() + "'.");
                Bukkit.getPluginManager().disablePlugin(this);
            }
        });
        if (!isEnabled()) return;
        mapRotationManager.start();

        gameListener = new GameListener(this);
        StatisticsListener statisticsListener = new StatisticsListener(this);
        Bukkit.getPluginManager().registerEvents(gameListener, this);
        Bukkit.getPluginManager().registerEvents(statisticsListener, this);
        scoreboardManager.start();
        tablistManager.start();
        Bukkit.getPluginManager().registerEvents(shopManager, this);
        Bukkit.getPluginManager().registerEvents(perkListener, this);
        Bukkit.getPluginManager().registerEvents(perkMenu, this);
        Bukkit.getPluginManager().registerEvents(questManager, this);
        Bukkit.getPluginManager().registerEvents(slimeLaunchPadManager, this);
        Bukkit.getPluginManager().registerEvents(collectibleCoinManager, this);
        Bukkit.getPluginManager().registerEvents(statsMenuManager, this);
        Bukkit.getPluginManager().registerEvents(leaderboardManager, this);
        Bukkit.getPluginManager().registerEvents(npcService, this);
        Bukkit.getPluginManager().registerEvents(networkProfileCache, this);
        Bukkit.getPluginManager().registerEvents(fishingManager, this);
        Bukkit.getPluginManager().registerEvents(prestigeManager, this);
        Bukkit.getPluginManager().registerEvents(renownShopManager, this);
        Bukkit.getPluginManager().registerEvents(expandedEnderChestManager, this);
        Bukkit.getPluginManager().registerEvents(scoreboardManager, this);
        Bukkit.getPluginManager().registerEvents(levelUpFeedbackManager, this);
        Bukkit.getPluginManager().registerEvents(featureMenuManager, this);
        Bukkit.getPluginManager().registerEvents(achievementManager, this);
        Bukkit.getPluginManager().registerEvents(actionBarManager, this);
        Bukkit.getPluginManager().registerEvents(contractManager, this);
        Bukkit.getPluginManager().registerEvents(pitCommandManager, this);
        Bukkit.getPluginManager().registerEvents(killstreakManager, this);
        Bukkit.getPluginManager().registerEvents(mysticWellManager, this);
        Bukkit.getPluginManager().registerEvents(minorEventManager, this);
        Bukkit.getPluginManager().registerEvents(mysticItemManager, this);
        Bukkit.getPluginManager().registerEvents(combatLogManager, this);
        Bukkit.getPluginManager().registerEvents(combatKnockbackCompatibility, this);
        fishingManager.start();
        statsManager.start();
        shopManager.start();
        achievementManager.start();
        actionBarManager.start();
        contractManager.start();
        pitCommandManager.start();
        killstreakManager.start();
        mysticWellManager.start();
        minorEventManager.start();
        combatLogManager.start();
        combatKnockbackCompatibility.start();
        playerInventoryManager.start();
        leaderboardManager.start();
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) statsManager.initializePlayer(player);

        PitCommand pitCommand = new PitCommand(this);
        getCommand("pit").setExecutor(pitCommand);
        getCommand("pit").setTabCompleter(pitCommand);
        if (getCommand("knockback") != null) {
            getCommand("knockback").setExecutor(combatKnockbackCompatibility);
            getCommand("knockback").setTabCompleter(combatKnockbackCompatibility);
        }
        if (getCommand("npc") != null) {
            getCommand("npc").setExecutor(pitCommand);
            getCommand("npc").setTabCompleter(pitCommand);
        } else {
            getLogger().severe("The /npc command could not be registered; use /thepit:npc or /pit npc and check for a plugin command collision.");
        }
        for (String shortcut : new String[]{"gmc", "gms", "gmsp", "gma", "unsafeenchant", "enchantunsafe", "spawn", "respawn", "oof", "trade", "tradelimits", "offer", "pickupstash", "dropconfirm", "show", "events", "event", "carepackageset", "carepackageremove", "carepackagelist", "carepackagetest", "squad", "cool", "credit", "pitsupporter", "blue", "azul", "enderchest", "renown", "buildmode", "quests", "quest", "contracts", "contract", "contractadmin", "combatlog", "pitpurchasedebug", "stats", "shop", "shopadmin", "gold", "coins", "pitchat", "upgrades", "killstreaks", "view", "achievements", "achievementadmin", "setleaderboard", "removeleaderboard", "leaderboard", "setfeaturehologram", "removefeaturehologram"}) {
            if (getCommand(shortcut) != null) {
                getCommand(shortcut).setExecutor(pitCommand);
                getCommand(shortcut).setTabCompleter(pitCommand);
            }
        }
        if (getCommand("eventsetup") != null) {
            getCommand("eventsetup").setExecutor(pitCommand);
            getCommand("eventsetup").setTabCompleter(pitCommand);
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (World world : Bukkit.getWorlds()) {
                try {
                    world.setStorm(false);
                    world.setThundering(false);
                    world.setWeatherDuration(0);
                    world.setTime(6000L);
                    world.setGameRuleValue("doDaylightCycle", "false");
                    world.setGameRuleValue("doWeatherCycle", "false");
                    world.setGameRuleValue("doFireTick", "false");
                    world.setDifficulty(Difficulty.PEACEFUL);
                } catch (Throwable ignored) { }
            }
        }, 20L, 200L);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ThePitExpansion(this, "thepit").register();
            new ThePitExpansion(this, "buildffa").register();
            getLogger().info("PlaceholderAPI hook enabled.");
        }
    }

    @Override
    public void onDisable() {
        if (playerInventoryManager != null) playerInventoryManager.shutdown();
        if (gameListener != null) gameListener.shutdown();
        if (combatLogManager != null) combatLogManager.shutdown();
        if (combatKnockbackCompatibility != null) combatKnockbackCompatibility.shutdown();
        if (actionBarManager != null) actionBarManager.shutdown();
        if (expandedEnderChestManager != null) expandedEnderChestManager.shutdown();
        if (killstreakManager != null) killstreakManager.shutdown();
        if (pitCommandManager != null) pitCommandManager.shutdown();
        if (contractManager != null) contractManager.shutdown();
        if (prestigeManager != null) prestigeManager.shutdown();
        if (achievementManager != null) achievementManager.shutdown();
        if (shopManager != null) shopManager.shutdown();
        if (fishingManager != null) fishingManager.shutdown();
        if (slimeLaunchPadManager != null) slimeLaunchPadManager.shutdown();
        if (collectibleCoinManager != null) collectibleCoinManager.shutdown();
        if (abelixRankPermissionManager != null) abelixRankPermissionManager.shutdown();
        if (leaderboardManager != null) leaderboardManager.shutdown();
        if (npcService != null) npcService.shutdown();
        if (nativePacketRouter != null) nativePacketRouter.shutdown();
        if (statsManager != null) statsManager.save();
        if (scoreboardManager != null) scoreboardManager.stop();
        if (tablistManager != null) tablistManager.stop();
        if (networkProfileCache != null) networkProfileCache.clear();
        if (perkManager != null) perkManager.shutdown();
        if (mapRotationManager != null) mapRotationManager.stop();
        if (arenaManager != null) arenaManager.shutdown();
        if (mysticWellManager != null) mysticWellManager.shutdown();
        if (minorEventManager != null) minorEventManager.shutdown();
        if (storageManager != null) storageManager.close();
        me.outerspace.thepit.npc.PitPacketEntityEngine.shared().clear();
    }

    public ArenaManager getArenaManager() { return arenaManager; }
    public DefaultLoadoutManager getDefaultLoadoutManager() { return defaultLoadoutManager; }
    public PlayerInventoryManager getPlayerInventoryManager() { return playerInventoryManager; }
    public MapRotationManager getMapRotationManager() { return mapRotationManager; }
    public GameListener getGameListener() { return gameListener; }
    public StatsManager getStatsManager() { return statsManager; }
    public PitGoldService getPitGoldService() { return pitGoldService; }
    public StorageManager getStorageManager() { return storageManager; }
    public PurchaseRepository getPurchaseRepository() { return purchaseRepository; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public ShopManager getShopManager() { return shopManager; }
    public StatsMenuManager getStatsMenuManager() { return statsMenuManager; }
    public PerkManager getPerkManager() { return perkManager; }
    public PerkListener getPerkListener() { return perkListener; }
    public PerkMenu getPerkMenu() { return perkMenu; }
    public PassiveManager getPassiveManager() { return passiveManager; }
    public QuestManager getQuestManager() { return questManager; }
    public SlimeLaunchPadManager getSlimeLaunchPadManager() { return slimeLaunchPadManager; }
    public CollectibleCoinManager getCollectibleCoinManager() { return collectibleCoinManager; }
    public me.outerspace.thepit.managers.ScoreboardManager getBuildScoreboardManager() { return scoreboardManager; }
    public me.outerspace.thepit.managers.TablistManager getTablistManager() { return tablistManager; }
    public AbelixHook getAbelixHook() { return abelixHook; }
    public NetworkProfileCache getNetworkProfileCache() { return networkProfileCache; }
    public PitPlayerNameFormatter getPlayerNameFormatter() { return playerNameFormatter; }
    public AbelixRankPermissionManager getAbelixRankPermissionManager() { return abelixRankPermissionManager; }
    public PitNpcService getNpcService() { return npcService; }
    public PitSetupService getSetupService() { return setupService; }
    public PitYamlManager getYamlManager() { return yamlManager; }
    public PitRegistry registry() { return registry; }
    public FishingManager getFishingManager() { return fishingManager; }
    public PrestigeManager getPrestigeManager() { return prestigeManager; }
    public RenownShopManager getRenownShopManager() { return renownShopManager; }
    public ExpandedEnderChestManager getExpandedEnderChestManager() { return expandedEnderChestManager; }
    public LevelUpFeedbackManager getLevelUpFeedbackManager() { return levelUpFeedbackManager; }
    public FeatureMenuManager getFeatureMenuManager() { return featureMenuManager; }
    public AchievementManager getAchievementManager() { return achievementManager; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }
    public ContractManager getContractManager() { return contractManager; }
    public PitCommandManager getPitCommandManager() { return pitCommandManager; }
    public KillstreakManager getKillstreakManager() { return killstreakManager; }
    public ProgressManager getProgressManager() { return progressManager; }
    public RewardMultiplierService getRewardMultiplierService() { return rewardMultiplierService; }
    public MinorEventManager getMinorEventManager() { return minorEventManager; }
    public MysticItemManager getMysticItemManager() { return mysticItemManager; }
    public CombatLogManager getCombatLogManager() { return combatLogManager; }
    public CombatKnockbackCompatibility getCombatKnockbackCompatibility() { return combatKnockbackCompatibility; }
    public NativePacketRouter getNativePacketRouter() { return nativePacketRouter; }

    private void removeObsoleteSetupEntries() {
        boolean changed = false;
        for (me.outerspace.thepit.setup.PitSetupSnapshot.Leaderboard board : new java.util.ArrayList<>(setupService.snapshot().leaderboards)) {
            if (board != null && "RATING".equalsIgnoreCase(board.statType)) {
                changed |= setupService.repository().deleteLeaderboard(board.stableId);
            }
        }
        for (me.outerspace.thepit.setup.PitSetupSnapshot.Npc npc : new java.util.ArrayList<>(setupService.snapshot().npcs)) {
            if (npc != null && ("kit".equalsIgnoreCase(npc.stableId) || "kitedit".equalsIgnoreCase(npc.stableId))) {
                changed |= setupService.repository().deleteNpc(npc.stableId);
            }
        }
        if (changed) setupService.reloadFromDatabase();
    }

    public String color(String text) {
        return me.outerspace.thepit.util.CC.color(text == null ? "" : text);
    }


    public boolean isBuildMode(org.bukkit.entity.Player player) {
        return me.outerspace.thepit.util.PermissionCompat.has(this, player, "thepit.admin.break") && buildModePlayers.contains(player.getUniqueId());
    }

    public boolean toggleBuildMode(org.bukkit.entity.Player player) {
        if (buildModePlayers.contains(player.getUniqueId())) {
            buildModePlayers.remove(player.getUniqueId());
            return false;
        }
        buildModePlayers.add(player.getUniqueId());
        return true;
    }

    public String applyPlaceholders(org.bukkit.entity.Player player, String text) {
        if (text == null) return "";
        text = text.replace("buildffa_", "thepit_");
        if (!containsPlaceholder(text)) return text;
        String out = text;
        if (player != null && statsManager != null) {
            java.util.UUID uuid = player.getUniqueId();
            String coins = scoreboardManager == null
                    ? String.valueOf(statsManager.getCoins(uuid, me.outerspace.thepit.model.Period.LIFETIME))
                    : scoreboardManager.formattedNumber(statsManager.getCoins(uuid, me.outerspace.thepit.model.Period.LIFETIME));
            java.text.NumberFormat whole = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US);
            out = out.replace("%level_formatted%", statsManager.getFormattedLevel(uuid))
                    .replace("%thepit_level_formatted%", statsManager.getFormattedLevel(uuid))
                    .replace("%level%", String.valueOf(statsManager.getLevel(uuid)))
                    .replace("%thepit_level%", String.valueOf(statsManager.getLevel(uuid)))
                    .replace("%level_progress%", statsManager.getLevelProgress(uuid))
                    .replace("%thepit_level_progress%", statsManager.getLevelProgress(uuid))
                    .replace("%coins%", coins)
                    .replace("%thepit_coins%", coins)
                    .replace("%pit_gold%", coins)
                    .replace("%thepit_pit_gold%", coins)
                    .replace("%thepit_gold%", coins)
                    .replace("%thepit_achievement_points%", achievementManager == null ? "0" : String.valueOf(achievementManager.points(uuid)))
                    .replace("%thepit_achievements_completed%", achievementManager == null ? "0" : String.valueOf(achievementManager.completedCount(uuid)))
                    .replace("%thepit_achievements_total%", achievementManager == null ? "0" : String.valueOf(achievementManager.totalCount()))
                    .replace("%thepit_achievement_completion_percent%", achievementManager == null ? "0.00" : achievementManager.completionPercent(uuid))
                    .replace("%renown%", String.valueOf(statsManager.getRenown(uuid)))
                    .replace("%thepit_renown%", String.valueOf(statsManager.getRenown(uuid)))
                    .replace("%renown_formatted%", whole.format(statsManager.getRenown(uuid)))
                    .replace("%thepit_renown_formatted%", whole.format(statsManager.getRenown(uuid)))
                    .replace("%thepit_prestige%", String.valueOf(statsManager.getPrestige(uuid)))
                    .replace("%thepit_prestige_roman%", statsManager.getPrestigeRoman(uuid))
                    .replace("%thepit_prestige_formatted%", statsManager.getPrestigeFormatted(uuid))
                    .replace("%thepit_next_prestige%", String.valueOf(Math.min(statsManager.getMaxPrestige(), statsManager.getPrestige(uuid) + 1)))
                    .replace("%thepit_next_prestige_roman%", statsManager.getNextPrestigeRoman(uuid))
                    .replace("%thepit_prestige_multiplier%", String.valueOf(statsManager.getMultiplier(uuid)))
                    .replace("%thepit_prestige_coins_required%", whole.format(statsManager.getPrestigeCoinsRequired(uuid)))
                    .replace("%thepit_prestige_coins_remaining%", whole.format(statsManager.getPrestigeCoinsRemaining(uuid)))
                    .replace("%thepit_prestige_coin_progress%", statsManager.getPrestigeCoinProgress(uuid))
                    .replace("%gold_required%", whole.format(statsManager.getPrestigeCoinsRequired(uuid)))
                    .replace("%thepit_prestige_gold_required%", whole.format(statsManager.getPrestigeCoinsRequired(uuid)))
                    .replace("%prestige_bracket_color%", statsManager.getPrestigeBracketColor(uuid))
                    .replace("%thepit_prestige_bracket_color%", statsManager.getPrestigeBracketColor(uuid))
                    .replace("%thepit_prestige_cooldown%", statsManager.getPrestigeCooldownFormatted(player));
            if (scoreboardManager != null) {
                out = out.replace("%date%", scoreboardManager.formattedDate())
                        .replace("%thepit_date%", scoreboardManager.formattedDate())
                        .replace("%prestige_line%", scoreboardManager.prestigeLine(player))
                        .replace("%prestige_level%", statsManager.getFormattedLevel(uuid))
                        .replace("%prestige_color%", statsManager.getPrestigeBracketColor(uuid))
                        .replace("%prestige_roman%", statsManager.getPrestigeRoman(uuid))
                        .replace("%needed_xp%", scoreboardManager.neededXp(player))
                        .replace("%thepit_needed_xp%", scoreboardManager.neededXp(player))
                        .replace("%thepit_abelix_gold%", scoreboardManager.formattedGold(uuid))
                        .replace("%status%", scoreboardManager.status(player))
                        .replace("%thepit_status%", scoreboardManager.status(player))
                        .replace("%bounty_line%", scoreboardManager.bountyLine(player))
                        .replace("%bounty%", scoreboardManager.formattedNumber(statsManager.getBounty(uuid)))
                        .replace("%thepit_bounty%", scoreboardManager.formattedNumber(statsManager.getBounty(uuid)))
                        .replace("%streak_line%", scoreboardManager.streakLine(player))
                        .replace("%killstreak%", scoreboardManager.formattedNumber(statsManager.getKillStreak(uuid, me.outerspace.thepit.model.Period.LIFETIME)))
                        .replace("%thepit_killstreak%", scoreboardManager.formattedNumber(statsManager.getKillStreak(uuid, me.outerspace.thepit.model.Period.LIFETIME)))
                        .replace("%effect_lines%", scoreboardManager.effectSummary(player));
            }
            if (out.contains("%thepit_stats_") || out.contains("%thepit_passive_")) out = replacePitPlaceholders(out, uuid);
        }
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                if (placeholderMethod == null) {
                    Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                    placeholderMethod = papi.getMethod("setPlaceholders", org.bukkit.entity.Player.class, String.class);
                }
                out = (String) placeholderMethod.invoke(null, player, out);
            }
        } catch (Throwable ignored) { }
        return out;
    }

    private String replacePitPlaceholders(String text, UUID uuid) {
        long kills = statsManager.getKills(uuid, me.outerspace.thepit.model.Period.LIFETIME);
        long assists = statsManager.getAssists(uuid);
        long deaths = statsManager.getDeaths(uuid, me.outerspace.thepit.model.Period.LIFETIME);
        double damageDealt = statsManager.getDoubleStat(uuid, StatsManager.DAMAGE_DEALT);
        double damageTaken = statsManager.getDoubleStat(uuid, StatsManager.DAMAGE_TAKEN);
        double hours = statsManager.getLongStat(uuid, StatsManager.PLAYTIME_MILLIS) / 3600000.0D;
        String out = text
                .replace("%thepit_stats_kills%", String.valueOf(kills))
                .replace("%thepit_stats_assists%", String.valueOf(assists))
                .replace("%thepit_stats_deaths%", String.valueOf(deaths))
                .replace("%thepit_stats_sword_hits%", String.valueOf(statsManager.getLongStat(uuid, StatsManager.SWORD_HITS)))
                .replace("%thepit_stats_arrows_shot%", String.valueOf(statsManager.getLongStat(uuid, StatsManager.ARROWS_SHOT)))
                .replace("%thepit_stats_damage_dealt%", two(damageDealt))
                .replace("%thepit_stats_melee_damage_dealt%", two(statsManager.getDoubleStat(uuid, StatsManager.MELEE_DAMAGE_DEALT)))
                .replace("%thepit_stats_bow_damage_dealt%", two(statsManager.getDoubleStat(uuid, StatsManager.BOW_DAMAGE_DEALT)))
                .replace("%thepit_stats_damage_taken%", two(damageTaken))
                .replace("%thepit_stats_melee_damage_taken%", two(statsManager.getDoubleStat(uuid, StatsManager.MELEE_DAMAGE_TAKEN)))
                .replace("%thepit_stats_bow_damage_taken%", two(statsManager.getDoubleStat(uuid, StatsManager.BOW_DAMAGE_TAKEN)))
                .replace("%thepit_stats_highest_streak%", String.valueOf(statsManager.getHighestStreak(uuid)))
                .replace("%thepit_stats_lifetime_xp%", String.valueOf(statsManager.getLifetimeXp(uuid)))
                .replace("%thepit_stats_kd%", two(statsManager.safeRatio(kills, deaths)))
                .replace("%thepit_stats_kad%", two(statsManager.safeRatio(kills + assists, deaths)))
                .replace("%thepit_stats_damage_ratio%", two(statsManager.safeRatio(damageDealt, damageTaken)))
                .replace("%thepit_stats_hours_played%", two(hours))
                .replace("%thepit_stats_gold_per_hour%", two(hours <= 0.0D ? 0.0D : statsManager.getLongStat(uuid, StatsManager.GOLD_EARNED) / hours))
                .replace("%thepit_stats_ka_per_hour%", two(hours <= 0.0D ? 0.0D : (kills + assists) / hours))
                .replace("%thepit_stats_gold_earned%", String.valueOf(statsManager.getLongStat(uuid, StatsManager.GOLD_EARNED)))
                .replace("%thepit_stats_blocks_placed%", String.valueOf(statsManager.getLongStat(uuid, StatsManager.BLOCKS_PLACED)))
                .replace("%thepit_stats_blocks_broken%", String.valueOf(statsManager.getLongStat(uuid, StatsManager.BLOCKS_BROKEN)))
                .replace("%thepit_stats_launcher_launches%", String.valueOf(statsManager.getLongStat(uuid, StatsManager.LAUNCHER_LAUNCHES)))
                .replace("%thepit_stats_fishing_rods_launched%", String.valueOf(statsManager.getLongStat(uuid, StatsManager.FISHING_RODS_LAUNCHED)))
                .replace("%thepit_stats_fished_anything%", String.valueOf(statsManager.getLongStat(uuid, StatsManager.FISHED_ANYTHING)))
                .replace("%thepit_stats_fished_fish%", String.valueOf(statsManager.getLongStat(uuid, StatsManager.FISHED_FISH)));
        if (passiveManager != null) {
            for (me.outerspace.thepit.perk.PassiveDefinition definition : passiveManager.definitions()) {
                String key = definition.id().replace('-', '_');
                int tier = passiveManager.tier(uuid, definition.id());
                out = out.replace("%thepit_passive_" + key + "%", String.valueOf(tier))
                        .replace("%thepit_passive_" + key + "_formatted%", passiveManager.roman(tier));
            }
        }
        return out;
    }

    private String two(double value) {
        return String.format(java.util.Locale.US, "%.2f", Double.isFinite(value) ? value : 0.0D);
    }

    public String replaceNetworkPlaceholders(org.bukkit.entity.Player player, org.bukkit.entity.Player killer, String text) {
        if (text == null) return "";
        String out = normalizeAbelixPrefixPlaceholder(text);
        if (player != null && containsPlayerNetworkPlaceholder(out)) {
            NetworkProfileSnapshot profile = networkProfileCache == null ? resolveNetworkProfile(player) : networkProfileCache.get(player);
            out = replaceProfilePlaceholders(out, profile, "");
            out = replaceIfPresent(out, "%abelix_rankcolor_player%", profile.getRankColor() + player.getName());
        }
        if (player != null && out.contains("%player%")) out = out.replace("%player%", player.getName());
        if (killer != null) {
            if (out.contains("%killer_")) {
                NetworkProfileSnapshot profile = networkProfileCache == null ? resolveNetworkProfile(killer) : networkProfileCache.get(killer);
                out = replaceProfilePlaceholders(out, profile, "killer_");
            }
            if (out.contains("%killer%")) out = out.replace("%killer%", killer.getName());
        }
        return applyPlaceholders(player, out);
    }

    public String getNetworkPrefix(org.bukkit.entity.Player player) {
        return player == null ? "" : profile(player).getPrefix();
    }

    public String getNetworkGroup(org.bukkit.entity.Player player) {
        return player == null ? "default" : profile(player).getGroup();
    }

    public int getNetworkPriority(org.bukkit.entity.Player player) {
        return player == null ? -1 : profile(player).getPriority();
    }

    public int getNetworkGold(org.bukkit.entity.Player player) {
        return player == null ? 0 : profile(player).getGold();
    }

    public Long resolveNetworkGoldUncached(org.bukkit.entity.Player player) {
        if (player == null) return null;
        if (abelixHook != null && abelixHook.isEnabled()) {
            Integer direct = abelixHook.getGold(player);
            if (direct != null) return direct.longValue();
        }
        String resolved = firstResolvedPlaceholder(player, registry().abelixRanks().getStringList("abelix.gold-placeholders"));
        if (resolved.isEmpty()) return null;
        try {
            String numeric = resolved.replaceAll("[^0-9-]", "");
            return numeric.isEmpty() || numeric.equals("-") ? null : Long.parseLong(numeric);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public NetworkProfileSnapshot resolveNetworkProfile(org.bukkit.entity.Player player) {
        if (player == null) return new NetworkProfileSnapshot("", "", "", "default", -1, 0);
        AbelixHook.Profile external = abelixHook != null && abelixHook.isEnabled() ? abelixHook.resolveProfile(player) : null;
        String prefix = external == null ? "" : external.getPrefix();
        String rankColor = external == null ? "" : external.getRankColor();
        String group = external == null ? "default" : external.getGroup();
        int priority = external == null ? -1 : external.getPriority();
        Integer gold = external == null ? null : external.getGold();
        if (prefix == null || prefix.isEmpty()) prefix = firstResolvedPlaceholder(player, registry().abelixRanks().getStringList("abelix.prefix-placeholders"));
        if (prefix.isEmpty()) prefix = getLuckPermsPrefix(player.getUniqueId());
        if (group == null || group.isEmpty() || group.equalsIgnoreCase("default")) group = getLuckPermsGroup(player.getUniqueId());
        String permissionPrefix = external != null && external.getPrefix() != null && !external.getPrefix().isEmpty()
                ? external.getPrefix() : resolvePermissionPrefix(player);
        if (permissionPrefix.isEmpty()) permissionPrefix = prefix;
        if (rankColor.isEmpty()) rankColor = firstResolvedPlaceholder(player, java.util.Arrays.asList("%abelix_rankcolor%", "%abelix_rank_color%"));
        if (rankColor.isEmpty()) rankColor = org.bukkit.ChatColor.getLastColors(me.outerspace.thepit.util.CC.color(prefix));
        if (rankColor.isEmpty()) rankColor = me.outerspace.thepit.util.CC.color(registry().abelixRanks().getString("abelix.default-name-color", "&f"));
        if (gold == null) {
            String resolved = firstResolvedPlaceholder(player, registry().abelixRanks().getStringList("abelix.gold-placeholders"));
            try { gold = resolved.isEmpty() ? 0 : Integer.parseInt(resolved.replaceAll("[^0-9-]", "")); }
            catch (NumberFormatException ignored) { gold = 0; }
        }
        return new NetworkProfileSnapshot(prefix, permissionPrefix, rankColor, group, priority, gold);
    }

    private NetworkProfileSnapshot profile(org.bukkit.entity.Player player) {
        return networkProfileCache == null ? resolveNetworkProfile(player) : networkProfileCache.get(player);
    }

    private boolean containsPlayerNetworkPlaceholder(String text) {
        return text.contains("%abelix_") || text.contains("%fakepixel_")
                || text.contains("%thepit_fakepixel_") || text.contains("%luckperms_prefix%")
                || text.contains("%rank_prefix%") || text.contains("%permission_prefix%") || text.contains("%group_prefix%");
    }

    private String replaceProfilePlaceholders(String text, NetworkProfileSnapshot profile, String root) {
        String out = text;
        out = replaceIfPresent(out, "%" + root + "prefix%", profile.getPrefix());
        out = replaceIfPresent(out, "%" + root + "abelix_prefix%", profile.getPrefix());
        out = replaceIfPresent(out, "%" + root + "abelix_rankcolor%", profile.getRankColor());
        out = replaceIfPresent(out, "%" + root + "abelix_rank_color%", profile.getRankColor());
        out = replaceIfPresent(out, "%" + root + "fakepixel_prefix%", profile.getPrefix());
        out = replaceIfPresent(out, "%" + root + "luckperms_prefix%", profile.getPrefix());
        out = replaceIfPresent(out, "%" + root + "rank_prefix%", profile.getPermissionPrefix());
        out = replaceIfPresent(out, "%" + root + "permission_prefix%", profile.getPermissionPrefix());
        out = replaceIfPresent(out, "%" + root + "group_prefix%", profile.getPermissionPrefix());
        out = replaceIfPresent(out, "%" + root + "thepit_abelix_gold%", String.valueOf(profile.getGold()));
        out = replaceIfPresent(out, "%" + root + "thepit_fakepixel_gold%", String.valueOf(profile.getGold()));
        out = replaceIfPresent(out, "%" + root + "abelix_gold%", String.valueOf(profile.getGold()));
        out = replaceIfPresent(out, "%" + root + "fakepixel_gold%", String.valueOf(profile.getGold()));
        return out;
    }

    private String normalizeAbelixPrefixPlaceholder(String text) {
        if (text == null) return "";
        boolean legacy = text.contains("%buildffa_abelix_prefix%") || text.contains("%thepit_abelix_prefix%");
        if (legacy && !legacyAbelixPrefixWarningLogged) {
            getLogger().warning("Legacy plugin-scoped Abelix prefix placeholder was normalized to canonical %abelix_prefix%.");
            legacyAbelixPrefixWarningLogged = true;
        }
        return text.replace("%buildffa_abelix_prefix%", "%abelix_prefix%").replace("%thepit_abelix_prefix%", "%abelix_prefix%");
    }

    private String replaceIfPresent(String text, String placeholder, String value) {
        return text.contains(placeholder) ? text.replace(placeholder, value == null ? "" : value) : text;
    }

    public boolean containsPlaceholder(String text) {
        if (text == null) return false;
        int start = text.indexOf('%');
        while (start >= 0 && start < text.length() - 2) {
            int end = text.indexOf('%', start + 1);
            if (end > start + 1) {
                boolean valid = true;
                boolean named = false;
                for (int i = start + 1; i < end; i++) {
                    char c = text.charAt(i);
                    if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':')) {
                        valid = false;
                        break;
                    }
                    if (Character.isLetter(c) || c == '_') named = true;
                }
                if (valid && named) return true;
            }
            start = text.indexOf('%', start + 1);
        }
        return false;
    }

    private String firstResolvedPlaceholder(org.bukkit.entity.Player player, java.util.List<String> placeholders) {
        if (player == null || placeholders == null || placeholders.isEmpty()) return "";
        for (String placeholder : placeholders) {
            if (placeholder == null || placeholder.trim().isEmpty() || placeholder.toLowerCase(java.util.Locale.ENGLISH).startsWith("%thepit_")) continue;
            String value = applyPlaceholders(player, placeholder);
            if (value == null) continue;
            value = me.outerspace.thepit.util.CC.color(value).trim();
            if (value.isEmpty() || value.equalsIgnoreCase(placeholder) || (value.startsWith("%") && value.endsWith("%"))) continue;
            return value;
        }
        return "";
    }

    public boolean takeGold(org.bukkit.entity.Player player, int amount) {
        return amount <= 0 || pitGoldService != null && pitGoldService.take(player, amount);
    }

    public void addGold(org.bukkit.entity.Player player, int amount) {
        if (player != null && amount > 0 && pitGoldService != null) pitGoldService.add(player, amount);
    }

    public boolean awardPitGold(org.bukkit.entity.Player player, int amount) {
        return awardPitGold(player, amount, GoldSource.FISHING);
    }

    public boolean awardPitGold(org.bukkit.entity.Player player, int amount, GoldSource source) {
        if (player == null || amount <= 0) return false;
        return pitGoldService != null && pitGoldService.award(player, amount, source) > 0;
    }

    public String getLuckPermsPrefix(java.util.UUID uuid) {
        try {
            org.bukkit.plugin.Plugin lpPlugin = Bukkit.getPluginManager().getPlugin("LuckPerms");
            if (lpPlugin == null) return "";
            Object provider = Class.forName("net.luckperms.api.LuckPermsProvider").getMethod("get").invoke(null);
            Object userManager = provider.getClass().getMethod("getUserManager").invoke(provider);
            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, uuid);
            if (user == null) return "";
            Object cached = user.getClass().getMethod("getCachedData").invoke(user);
            Object meta = cached.getClass().getMethod("getMetaData").invoke(cached);
            Object prefix = meta.getClass().getMethod("getPrefix").invoke(meta);
            return prefix == null ? "" : me.outerspace.thepit.util.CC.color(prefix.toString());
        } catch (Throwable ignored) {
            return "";
        }
    }
    public String getLuckPermsGroup(java.util.UUID uuid) {
        try {
            org.bukkit.plugin.Plugin lpPlugin = Bukkit.getPluginManager().getPlugin("LuckPerms");
            if (lpPlugin == null) return "default";
            Object provider = Class.forName("net.luckperms.api.LuckPermsProvider").getMethod("get").invoke(null);
            Object userManager = provider.getClass().getMethod("getUserManager").invoke(provider);
            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, uuid);
            if (user == null) return "default";
            Object group = user.getClass().getMethod("getPrimaryGroup").invoke(user);
            return group == null ? "default" : group.toString().toLowerCase(java.util.Locale.ENGLISH);
        } catch (Throwable ignored) {
            return "default";
        }
    }

    public String getPermissionPrefix(org.bukkit.entity.Player player) {
        return player == null ? "" : profile(player).getPermissionPrefix();
    }

    private String resolvePermissionPrefix(org.bukkit.entity.Player player) {
        java.util.List<String> placeholders = registry().abelixRanks().getStringList("rank-prefix-placeholders");
        if (placeholders == null || placeholders.isEmpty()) {
            placeholders = java.util.Arrays.asList("%luckperms_prefix%", "%vault_prefix%", "%vault_group%");
        }
        for (String placeholder : placeholders) {
            String value = applyPlaceholders(player, placeholder);
            if (value == null) continue;
            value = me.outerspace.thepit.util.CC.color(value).trim();
            if (value.isEmpty()) continue;
            if (value.equalsIgnoreCase(placeholder)) continue;
            if (value.startsWith("%") && value.endsWith("%")) continue;
            return value;
        }
        return getLuckPermsPrefix(player.getUniqueId());
    }

    public MysticWellManager getMysticWellManager() { return mysticWellManager; }
}
