package club.abelix.games.fakevival;

import club.abelix.games.fakevival.commands.*;
import club.abelix.games.fakevival.config.defaults.RootConfigDefaults;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.data.ConfigFileManager;
import club.abelix.games.fakevival.hooks.AbelixHook;
import club.abelix.games.fakevival.furniture.*;
import club.abelix.games.fakevival.listeners.AdminCommandListener;
import club.abelix.games.fakevival.listeners.BackListener;
import club.abelix.games.fakevival.listeners.ChatFilterListener;
import club.abelix.games.fakevival.listeners.CollectionListener;
import club.abelix.games.fakevival.listeners.CompanionListener;
import club.abelix.games.fakevival.listeners.CustomItemListener;
import club.abelix.games.fakevival.listeners.DevControlListener;
import club.abelix.games.fakevival.listeners.ItemRarityListener;
import club.abelix.games.fakevival.listeners.LiveControlListener;
import club.abelix.games.fakevival.listeners.NpcClickListener;
import club.abelix.games.fakevival.listeners.NpcVisibilityListener;
import club.abelix.games.fakevival.listeners.PlayerListener;
import club.abelix.games.fakevival.listeners.ProgressListener;
import club.abelix.games.fakevival.listeners.PveListener;
import club.abelix.games.fakevival.listeners.MenuListener;
import club.abelix.games.fakevival.listeners.MMOSystemListener;
import club.abelix.games.fakevival.listeners.SpawnProtectionListener;
import club.abelix.games.fakevival.listeners.SmpDeathMessageListener;
import club.abelix.games.fakevival.listeners.ShortbowListener;
import club.abelix.games.fakevival.listeners.CookingListener;
import club.abelix.games.fakevival.listeners.CustomAnvilListener;
import club.abelix.games.fakevival.listeners.SocialSpyListener;
import club.abelix.games.fakevival.listeners.StaffPermissionListener;
import club.abelix.games.fakevival.listeners.RegionListener;
import club.abelix.games.fakevival.listeners.RecipeCraftListener;
import club.abelix.games.fakevival.listeners.DungeonListener;
import club.abelix.games.fakevival.listeners.DungeonInstanceProtectionListener;
import club.abelix.games.fakevival.listeners.DungeonPortalWandListener;
import club.abelix.games.fakevival.listeners.FishingGearListener;
import club.abelix.games.fakevival.listeners.GuildClaimListener;
import club.abelix.games.fakevival.listeners.GuildDisplayListener;
import club.abelix.games.fakevival.listeners.LanguageListener;
import club.abelix.games.fakevival.listeners.LootBoxListener;
import club.abelix.games.fakevival.listeners.MainMenuItemListener;
import club.abelix.games.fakevival.listeners.PlaytimeRewardsListener;
import club.abelix.games.fakevival.listeners.TradeListener;
import club.abelix.games.fakevival.listeners.VanishListener;
import club.abelix.games.fakevival.listeners.DupeDetectionListener;
import club.abelix.games.fakevival.listeners.XrayDetectionListener;
import club.abelix.games.fakevival.listeners.ZoneListener;
import club.abelix.games.fakevival.live.DevControlMenu;
import club.abelix.games.fakevival.live.LiveControlManager;
import club.abelix.games.fakevival.managers.*;
import club.abelix.games.fakevival.menus.BankMenu;
import club.abelix.games.fakevival.mmo.MMODamageIndicatorService;
import club.abelix.games.fakevival.mmo.MMOItemValueService;
import club.abelix.games.fakevival.mmo.MMOStatService;
import club.abelix.games.fakevival.mayor.MayorManager;
import club.abelix.games.fakevival.mayor.MayorModifierType;
import club.abelix.games.fakevival.mobengine.command.SMPMobCommand;
import club.abelix.games.fakevival.mobengine.mob.SMPMobEngine;
import club.abelix.games.fakevival.mobengine.mob.SMPMobManager;
import club.abelix.games.fakevival.mobengine.profile.SMPMobPerformanceProfiler;
import club.abelix.games.fakevival.registries.StaticGameRegistries;
import club.abelix.games.fakevival.network.SMPNetworkService;
import club.abelix.games.fakevival.network.SMPServerAccessGuard;
import club.abelix.games.fakevival.network.NetworkStatusCommand;
import club.abelix.games.fakevival.smpworlds.SMPWorldManager;
import club.abelix.games.fakevival.smpworlds.SMPWorldsCommand;
import club.abelix.games.fakevival.setup.SMPSetupSnapshotService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Game extends JavaPlugin {

    private DataManager dataManager;
    private ConfigFileManager configFileManager;
    private LiveControlManager liveControlManager;
    private DevControlMenu devControlMenu;
    private AbelixHook abelixHook;
    private SpawnManager spawnManager;
    private SkillManager skillManager;
    private JobManager jobManager;
    private TablistManager tablistManager;
    private BoardManager boardManager;
    private HubManager hubManager;
    private NpcManager npcManager;
    private MenuManager menuManager;
    private ProgressBossBarManager progressBossBarManager;
    private SpawnProtectionManager spawnProtectionManager;
    private HomeManager homeManager;
    private TeleportRequestManager teleportRequestManager;
    private VanishManager vanishManager;
    private DiscordWebhookManager discordWebhookManager;
    private SmpAlertService smpAlertService;
    private MacrocheckManager macrocheckManager;
    private DupeDetectionManager dupeDetectionManager;
    private XrayDetectionManager xrayDetectionManager;
    private QuestManager questManager;
    private AdventureGuildManager adventureGuildManager;
    private RegionManager regionManager;
    private DungeonManager dungeonManager;
    private DungeonLootChestManager dungeonLootChestManager;
    private DungeonTrapManager dungeonTrapManager;
    private DungeonSpectateService dungeonSpectateService;
    private EconomyManager economyManager;
    private EconomyAuditManager economyAuditManager;
    private BankManager bankManager;
    private BattlePassManager battlePassManager;
    private AuctionHouseManager auctionHouseManager;
    private BazaarManager bazaarManager;
    private ItemRarityManager itemRarityManager;
    private CustomItemManager customItemManager;
    private ItemBrowserCommand itemBrowserCommand;
    private CookingListener cookingListener;
    private CustomAnvilListener customAnvilListener;
    private ShortbowListener shortbowListener;
    private FishingGearListener fishingGearListener;
    private RecipeManager recipeManager;
    private CollectionManager collectionManager;
    private CompanionManager companionManager;
    private SocialSpyManager socialSpyManager;
    private TradeManager tradeManager;
    private KitManager kitManager;
    private StaffPermissionManager staffPermissionManager;
    private BackManager backManager;
    private GuildManager guildManager;
    private GuildsCommand guildsCommand;
    private GuildDisplayListener guildDisplayListener;
    private PveManager pveManager;
    private ZoneManager zoneManager;
    private EventManager eventManager;
    private MayorManager mayorManager;
    private LanguageManager languageManager;
    private LanguageCommand languageCommand;
    private PlaytimeRewardManager playtimeRewardManager;
    private PlaytimeRewardsCommand playtimeRewardsCommand;
    private RecipesCommand recipesCommand;
    private EnchantTableCommand enchantTableCommand;
    private LootBoxManager lootBoxManager;
    private ManaActionBarManager manaActionBarManager;
    private CosmeticEffectManager cosmeticEffectManager;
    private CustomEnchantManager customEnchantManager;
    private PlayerLevelManager playerLevelManager;
    private PlayerLevelCommand playerLevelCommand;
    private MMOStatService mmoStatService;
    private MMODamageIndicatorService mmoDamageIndicatorService;
    private CombatDebugManager combatDebugManager;
    private MMOItemValueService mmoItemValueService;
    private PlayerDataSyncManager playerDataSyncManager;
    private EquipmentCommand equipmentCommand;
    private SandboxCommand sandboxCommand;
    private FurnitureRegistry furnitureRegistry;
    private FurnitureEditor furnitureEditor;
    private FurnitureManager furnitureManager;
    private StorageManager storageManager;
    private PlayerNetworthCacheManager playerNetworthCacheManager;
    private StoreCommand storeCommand;
    private ChunkPregeneratorManager chunkPregeneratorManager;
    private AdminWorldEditManager adminWorldEditManager;
    private SMPMobManager smpMobManager;
    private SMPMobPerformanceProfiler smpMobProfiler;
    private SMPWorldManager smpWorldManager;
    private YamlConfiguration javaDefaultsConfig;
    private SMPNetworkService networkService;
    private SMPServerAccessGuard accessGuard;
    private SMPSetupSnapshotService setupSnapshotService;
    private SmpDeathMessageListener smpDeathMessageListener;

    @Override
    public FileConfiguration getConfig() {
        return settings();
    }

    public FileConfiguration settings() {
        if (javaDefaultsConfig == null) {
            javaDefaultsConfig = RootConfigDefaults.create();
        }
        return javaDefaultsConfig;
    }

    @Override
    public void reloadConfig() {
        javaDefaultsConfig = RootConfigDefaults.create();
        if (configFileManager != null) {
            configFileManager.reload();
        }
    }

    @Override
    public void saveConfig() {

    }

    @Override
    public void onEnable() {
        reloadConfig();

        configFileManager = new ConfigFileManager(this);
        configFileManager.setup();

        dataManager = new DataManager(this);
        dataManager.setup();
        ensureVillageShopDefaultVersion();
        liveControlManager = new LiveControlManager(this, dataManager);
        liveControlManager.setup();
        devControlMenu = new DevControlMenu(this, liveControlManager);
        hubManager = new HubManager(this, dataManager);
        hubManager.setup();

        abelixHook = new AbelixHook(this);
        abelixHook.setup();

        spawnManager = new SpawnManager(this, dataManager);
        spawnManager.loadSpawn();
        languageManager = new LanguageManager(this, dataManager);
        playtimeRewardManager = new PlaytimeRewardManager(this, dataManager);
        spawnProtectionManager = new SpawnProtectionManager(this, dataManager);
        homeManager = new HomeManager(this, dataManager);
        teleportRequestManager = new TeleportRequestManager(this);
        vanishManager = new VanishManager(this, abelixHook);
        staffPermissionManager = new StaffPermissionManager(this, abelixHook, configFileManager);
        backManager = new BackManager();
        discordWebhookManager = new DiscordWebhookManager(this);
        smpAlertService = new SmpAlertService(this, discordWebhookManager);
        socialSpyManager = new SocialSpyManager(this);
        dupeDetectionManager = new DupeDetectionManager(this, discordWebhookManager, socialSpyManager);
        xrayDetectionManager = new XrayDetectionManager(this, discordWebhookManager, socialSpyManager);
        itemRarityManager = new ItemRarityManager(this);
        manaActionBarManager = new ManaActionBarManager(this, dataManager);
        playerDataSyncManager = new PlayerDataSyncManager(this, dataManager, manaActionBarManager);
        networkService = new SMPNetworkService(this, dataManager, playerDataSyncManager);
        accessGuard = new SMPServerAccessGuard(networkService);
        setupSnapshotService = new SMPSetupSnapshotService(this);
        customItemManager = new CustomItemManager(this, dataManager, itemRarityManager, manaActionBarManager);
        lootBoxManager = new LootBoxManager(this, customItemManager, dataManager);
        collectionManager = new CollectionManager(this, dataManager);
        customEnchantManager = new CustomEnchantManager(this);
        smpMobProfiler = new SMPMobPerformanceProfiler();
        smpMobManager = new SMPMobManager(this, smpMobProfiler);
        smpMobManager.start();
        smpWorldManager = new SMPWorldManager(this, dataManager);
        smpWorldManager.enable();

        progressBossBarManager = new ProgressBossBarManager(this);
        economyManager = new EconomyManager(this, dataManager);
        economyAuditManager = new EconomyAuditManager(this, dataManager);
        economyManager.setAuditManager(economyAuditManager);
        bankManager = new BankManager(this, dataManager, economyManager);
        battlePassManager = new BattlePassManager(this, dataManager, abelixHook);
        battlePassManager.setup();
        skillManager = new SkillManager(this, dataManager, progressBossBarManager, battlePassManager);
        skillManager.setConsumableXpMultiplier(player -> 1.0D + Math.max(0.0D,
                customItemManager.getTemporaryConsumableStats(player).get(club.abelix.games.fakevival.mmo.MMOStatType.XP_BOOST)) / 100.0D);
        companionManager = new CompanionManager(this, dataManager, customItemManager, skillManager);
        chunkPregeneratorManager = new ChunkPregeneratorManager(this, dataManager);
        jobManager = new JobManager(this, dataManager, progressBossBarManager, battlePassManager);
        guildManager = new GuildManager(this, dataManager);
        playerLevelManager = new PlayerLevelManager(this, dataManager, skillManager, customItemManager);
        skillManager.setPlayerLevelManager(playerLevelManager);
        customItemManager.setProgressionManagers(skillManager, playerLevelManager);
        mmoItemValueService = new MMOItemValueService(this);
        customItemManager.configureEditor(customEnchantManager, mmoItemValueService);
        tablistManager = new TablistManager(this, abelixHook, skillManager, jobManager, economyManager, battlePassManager, guildManager, playerLevelManager);
        guildDisplayListener = new GuildDisplayListener(this, guildManager, tablistManager, playerLevelManager);
        boardManager = new BoardManager(this, tablistManager);
        npcManager = new NpcManager(this, dataManager);
        auctionHouseManager = new AuctionHouseManager(this, dataManager, economyManager, economyAuditManager, mmoItemValueService, abelixHook);
        bazaarManager = new BazaarManager(this, dataManager, economyManager, discordWebhookManager, customItemManager);
        kitManager = new KitManager(this, dataManager);
        tradeManager = new TradeManager(this, economyManager, economyAuditManager, discordWebhookManager, socialSpyManager, mmoItemValueService);
        auctionHouseManager.setAlertService(smpAlertService);
        tradeManager.setAlertService(smpAlertService);
        bankManager.setAlertService(smpAlertService);
        bazaarManager.setAlertService(smpAlertService);
        macrocheckManager = new MacrocheckManager(this, dataManager);
        questManager = new QuestManager(this, dataManager, tablistManager, configFileManager, customItemManager);
        adventureGuildManager = new AdventureGuildManager(this,dataManager,skillManager);
        Bukkit.getPluginManager().registerEvents(adventureGuildManager,this);
        questManager.setAdventureGuildManager(adventureGuildManager);
        recipeManager = new RecipeManager(this, collectionManager, customItemManager, customEnchantManager, skillManager, questManager);
        collectionManager.setRecipeManager(recipeManager);
        menuManager = new MenuManager(this, skillManager, jobManager, tablistManager, battlePassManager, auctionHouseManager, economyManager, bazaarManager, questManager, customItemManager, customEnchantManager);
        storageManager = new StorageManager(this, dataManager);
        playerNetworthCacheManager = new PlayerNetworthCacheManager(this, dataManager, economyManager,
                auctionHouseManager, storageManager, mmoItemValueService);
        regionManager = new RegionManager(this, dataManager, tablistManager);
        zoneManager = new ZoneManager(this, dataManager, economyManager, customItemManager);
        adminWorldEditManager = new AdminWorldEditManager(this, spawnProtectionManager, zoneManager, guildManager, discordWebhookManager);
        furnitureRegistry = new FurnitureRegistry(this, dataManager);
        if (settings().getBoolean("furniture.enabled", false)) furnitureRegistry.reload();
        furnitureEditor = new FurnitureEditor(this, dataManager, furnitureRegistry);
        furnitureManager = new FurnitureManager(this, dataManager, furnitureRegistry, economyManager, spawnProtectionManager, zoneManager);
        customItemManager.registerExternalItemProvider((id, amount) -> furnitureRegistry.get(id).map(f -> furnitureManager.createItem(f, amount)).orElse(null),
                () -> furnitureRegistry.all().stream().map(FurnitureDefinition::id).toList());
        furnitureManager.start();
        eventManager = new EventManager(this, dataManager, skillManager, economyManager, customItemManager, zoneManager);
        menuManager.setEventManager(eventManager);
        guildsCommand = new GuildsCommand(this, guildManager, guildDisplayListener);
        pveManager = new PveManager(this, dataManager, spawnProtectionManager, zoneManager);
        pveManager.setGuildManager(guildManager);
        dungeonManager = new DungeonManager(this, dataManager);
        dungeonLootChestManager = new DungeonLootChestManager(this, dataManager, dungeonManager);
        dungeonManager.setLootChestManager(dungeonLootChestManager);
        dungeonTrapManager = new DungeonTrapManager(this, dataManager, dungeonManager);
        dungeonManager.setTrapManager(dungeonTrapManager);
        mayorManager = new MayorManager(this, dataManager, playerLevelManager);
        skillManager.setEventXpMultiplier((player, skill) -> {
            double multiplier = eventManager.skillXpMultiplier(player, skill);
            MayorModifierType type = skill.equalsIgnoreCase("farming") ? MayorModifierType.FARMING_XP
                    : skill.equalsIgnoreCase("mining") ? MayorModifierType.MINING_XP
                    : skill.equalsIgnoreCase("dungeon") || skill.equalsIgnoreCase("dungeoneering") ? MayorModifierType.DUNGEON_XP : null;
            return type == null ? multiplier : mayorManager.applyMayorModifier(player, type, multiplier);
        });
        dungeonSpectateService = new DungeonSpectateService(this, dataManager);
        if (networkService.canRunDungeonInstances()) dungeonManager.startTasks();
        combatDebugManager = new CombatDebugManager(dungeonManager, zoneManager, getSmpMobEngine());
        if (networkService.canRunHubZoneSystems()) zoneManager.start();
        playtimeRewardManager.start();
        customItemManager.setup();
        if (networkService.canRunDungeonInstances()) dungeonLootChestManager.start();
        if (networkService.canRunDungeonInstances()) dungeonTrapManager.start();
        playerLevelManager.validateOnStartup();
        bazaarManager.refreshCustomMaterialRegistry();
        eventManager.start();
        mayorManager.start();
        recipeManager.setup();
        companionManager.start();
        if(networkService.isOverworld())chunkPregeneratorManager.start();
        manaActionBarManager.start();
        cosmeticEffectManager = new CosmeticEffectManager(this, dataManager);
        cosmeticEffectManager.start();
        if (networkService.isOverworld()) smpWorldManager.ensureHubWorldLoaded();
        if (networkService.isDungeon()) networkService.ensureDungeonLobbyLoaded();
        npcManager.loadNpcs();

        playerDataSyncManager.start();
        networkService.start();
        setupSnapshotService.start();

        registerCommands();
        registerListeners();
        boardManager.start();
        dupeDetectionManager.start();
        startBattlePassPlayXpTask();

        for (Player player : Bukkit.getOnlinePlayers()) {
            tablistManager.apply(player);
            boardManager.update(player);
            staffPermissionManager.applyAsync(player);
            if (settings().getBoolean("spawnprotection.force-adventure", true)
                    && spawnProtectionManager.isInside(player.getLocation())
                    && !spawnProtectionManager.canBypass(player)) {
                player.setGameMode(GameMode.ADVENTURE);
            }
        }

        getLogger().info("Fakevival enabled" + (abelixHook.isHooked() ? " with Abelix API support." : "."));
    }

    @Override
    public void onDisable() {
        if (bazaarManager != null) bazaarManager.stop();
        java.util.concurrent.CompletableFuture<Void> networthFlush = playerNetworthCacheManager == null
                ? java.util.concurrent.CompletableFuture.completedFuture(null) : playerNetworthCacheManager.shutdown();
        java.util.concurrent.CompletableFuture<Void> storageFlush = storageManager == null
                ? java.util.concurrent.CompletableFuture.completedFuture(null) : storageManager.shutdown();
        if (dungeonSpectateService != null) dungeonSpectateService.shutdown();
        java.util.concurrent.CompletableFuture<Void> dungeonFlush = dungeonManager == null
                ? java.util.concurrent.CompletableFuture.completedFuture(null) : dungeonManager.shutdown();
        java.util.concurrent.CompletableFuture<Void> dungeonLootFlush = dungeonLootChestManager == null
                ? java.util.concurrent.CompletableFuture.completedFuture(null) : dungeonLootChestManager.shutdown();
        java.util.concurrent.CompletableFuture<Void> dungeonTrapFlush = dungeonTrapManager == null
                ? java.util.concurrent.CompletableFuture.completedFuture(null) : dungeonTrapManager.shutdown();
        if (smpMobManager != null) smpMobManager.shutdown();
        if (smpWorldManager != null) smpWorldManager.disable();
        if (boardManager != null) boardManager.stop();
        if (tablistManager != null) tablistManager.stop();
        if (dupeDetectionManager != null) dupeDetectionManager.stop();
        if (progressBossBarManager != null) progressBossBarManager.stop();
        if (customItemManager != null) customItemManager.stop();
        if (cookingListener != null) cookingListener.shutdown();
        if (customAnvilListener != null) customAnvilListener.shutdown();
        if (fishingGearListener != null) fishingGearListener.getMinigame().shutdown();
        if (furnitureManager != null) furnitureManager.stop();
        if (eventManager != null) eventManager.stop();
        if (mayorManager != null) mayorManager.stop();
        if (zoneManager != null) zoneManager.stop();
        if (playtimeRewardManager != null) playtimeRewardManager.stop();
        if (companionManager != null) companionManager.stop();
        if (chunkPregeneratorManager != null) chunkPregeneratorManager.stop();
        if (adminWorldEditManager != null) adminWorldEditManager.shutdown();
        if (playerDataSyncManager != null) {
            playerDataSyncManager.shutdownSaveOnlinePlayers();
            playerDataSyncManager.stop();
        }
        if (mmoStatService != null) mmoStatService.stop();
        if (mmoDamageIndicatorService != null) mmoDamageIndicatorService.shutdown();
        if (manaActionBarManager != null) manaActionBarManager.stop();
        if (cosmeticEffectManager != null) cosmeticEffectManager.stop();
        if (npcManager != null) npcManager.shutdown();
        if (staffPermissionManager != null) staffPermissionManager.cleanupAll();
        if (networkService != null) networkService.stop();
        if (dataManager != null) {
            dataManager.saveNow();
            java.util.concurrent.CompletableFuture.allOf(networthFlush, storageFlush, dungeonFlush, dungeonLootFlush, dungeonTrapFlush).whenComplete((ignored, error) -> dataManager.close());
        }
    }

    public SMPMobEngine getSmpMobEngine() {
        return smpMobManager == null ? null : smpMobManager.engine();
    }

    public SMPWorldManager getSMPWorldManager() {
        return smpWorldManager;
    }

    public SMPServerAccessGuard getAccessGuard() { return accessGuard; }
    public SMPSetupSnapshotService getSetupSnapshotService() { return setupSnapshotService; }
    public ItemBrowserCommand getItemBrowserCommand() { return itemBrowserCommand; }
    public CookingListener getCookingListener() { return cookingListener; }
    public CustomAnvilListener getCustomAnvilListener() { return customAnvilListener; }
    public ShortbowListener getShortbowListener() { return shortbowListener; }
    public FishingMinigameManager getFishingMinigameManager() { return fishingGearListener == null ? null : fishingGearListener.getMinigame(); }
    public StoreCommand getStoreCommand() { return storeCommand; }


    private void startBattlePassPlayXpTask() {
        long minutes = Math.max(1L, settings().getLong("battlepass.play-xp-every-minutes", 5L));
        int xp = Math.max(0, settings().getInt("battlepass.play-xp", 25));
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (battlePassManager == null || xp == 0) return;
            for (Player player : Bukkit.getOnlinePlayers()) battlePassManager.addPlayXp(player, xp);
        }, minutes * 60L * 20L, minutes * 60L * 20L);
    }

    private void registerCommands() {
        setExecutor("smp", new SmpCommand(this, spawnManager, boardManager, hubManager, liveControlManager, devControlMenu, bazaarManager, menuManager, eventManager, furnitureRegistry, furnitureEditor, furnitureManager, chunkPregeneratorManager, adminWorldEditManager, playerDataSyncManager));
        setExecutor("mayor", new MayorCommand(mayorManager));
        setExecutor("alerts", new AlertsCommand(smpAlertService));
        setExecutor("macrocheck", macrocheckManager);
        setExecutor("smpmob", new SMPMobCommand(smpMobManager, smpMobProfiler));
        setExecutor("smpworlds", new SMPWorldsCommand(this, smpWorldManager));
        setExecutor("networkstatus", new NetworkStatusCommand(this, networkService));
        setExecutor("pregen", new PregenerateCommand(this, chunkPregeneratorManager));
        languageCommand = new LanguageCommand(this, languageManager);
        setExecutor("language", languageCommand);
        setExecutor("lang", languageCommand);
        setExecutor("menu", new MainMenuCommand(menuManager));
        setExecutor("mainmenu", new MainMenuCommand(menuManager));
        playtimeRewardsCommand = new PlaytimeRewardsCommand(this, playtimeRewardManager);
        setExecutor("playtimerewards", playtimeRewardsCommand);
        setExecutor("playtime", playtimeRewardsCommand);
        setExecutor("fly", new FlyCommand(this));
        setExecutor("speed", new SpeedCommand(this));
        setExecutor("gmc", new GameModeCommand(this));
        setExecutor("gms", new GameModeCommand(this));
        setExecutor("gmsp", new GameModeCommand(this));
        setExecutor("gma", new GameModeCommand(this));
        setExecutor("invsee", new InvseeCommand(this));
        setExecutor("setspawn", new SetSpawnCommand(this, spawnManager));
        setExecutor("spawn", new SpawnCommand(this, spawnManager));
        setExecutor("warp", new WarpCommand(this));
        setExecutor("setdungeonlobby", new SetDungeonLobbyCommand(this));
        setExecutor("back", new BackCommand(this, backManager));
        setExecutor("skills", new SkillsCommand(this, skillManager, menuManager));
        playerLevelCommand = new PlayerLevelCommand(playerLevelManager);
        setExecutor("playerlevel", playerLevelCommand);
        setExecutor("levels", playerLevelCommand);
        setExecutor("stats", new StatsCommand(playerLevelManager));
        setExecutor("jobs", new JobsCommand(this, jobManager, menuManager));
        setExecutor("npc", new NpcCommand(this, npcManager));
        setExecutor("spawnprotection", new SpawnProtectionCommand(this, spawnProtectionManager));
        setExecutor("buildmode", new BuildModeCommand(this, spawnProtectionManager));
        setExecutor("rtp", new RtpCommand(this));
        setExecutor("setrtplocation", new SetRtpLocationCommand(this));
        HomeCommands homeCommands = new HomeCommands(this, homeManager, spawnProtectionManager);
        setExecutor("sethome", homeCommands);
        setExecutor("home", homeCommands);
        setExecutor("delhome", homeCommands);
        setExecutor("renamehome", homeCommands);
        setExecutor("homes", homeCommands);
        TpaCommands tpaCommands = new TpaCommands(this, teleportRequestManager);
        setExecutor("tpa", tpaCommands);
        setExecutor("tpaccept", tpaCommands);
        setExecutor("tpacancel", tpaCommands);
        setExecutor("tpahere", tpaCommands);
        TeleportCommands teleportCommands = new TeleportCommands(this);
        setExecutor("tp", teleportCommands);
        setExecutor("tphere", teleportCommands);
        setExecutor("tpall", teleportCommands);
        setExecutor("vanish", new VanishCommand(this, vanishManager));
        setExecutor("zone", new ZoneCommand(this, zoneManager));
        setExecutor("lootbox", new LootBoxCommand(lootBoxManager));
        setExecutor("coins", new CoinsCommand(this, economyManager));
        EconomyStaffCommand economyStaffCommand = new EconomyStaffCommand(this, economyAuditManager, economyManager);
        setExecutor("econhistory", economyStaffCommand);
        setExecutor("tradehistory", economyStaffCommand);
        setExecutor("ahhistory", economyStaffCommand);
        setExecutor("itemhistory", economyStaffCommand);
        setExecutor("rollbacktrade", economyStaffCommand);
        setExecutor("freezeeco", economyStaffCommand);
        setExecutor("wipestats", new WipeStatsCommand(this, dataManager));
        setExecutor("battlepass", new BattlePassCommand(this, battlePassManager, menuManager));
        setExecutor("bp", new BattlePassCommand(this, battlePassManager, menuManager));
        setExecutor("auctionhouse", new AuctionHouseCommand(this, auctionHouseManager, menuManager));
        setExecutor("ah", new AuctionHouseCommand(this, auctionHouseManager, menuManager));
        setExecutor("bazaar", new BazaarCommand(menuManager, bazaarManager));
        setExecutor("sellall", new SellAllCommand(this, economyManager, customItemManager, bazaarManager, mmoItemValueService));
        equipmentCommand = new EquipmentCommand(this, dataManager, customItemManager);
        setExecutor("equipment", equipmentCommand);
        setExecutor("pve", new PveCommand(this, pveManager));
        setExecutor("quests", new QuestsCommand(this, menuManager,adventureGuildManager));
        GiveDevNotebookCommand devNotebook = new GiveDevNotebookCommand(this, customItemManager);
        setExecutor("givedevnotebook", devNotebook);
        Bukkit.getPluginManager().registerEvents(devNotebook, this);
        recipesCommand = new RecipesCommand(recipeManager);
        setExecutor("recipes", recipesCommand);
        enchantTableCommand = new EnchantTableCommand(this, customEnchantManager, collectionManager, skillManager, economyManager, customItemManager, dataManager);
        setExecutor("etable", enchantTableCommand);
        setExecutor("enchants", enchantTableCommand);
        setExecutor("guilds", guildsCommand);
        setExecutor("guild", guildsCommand);
        club.abelix.games.fakevival.menus.EnchantShopMenu enchantShopMenu = new club.abelix.games.fakevival.menus.EnchantShopMenu(this, customEnchantManager, customItemManager, economyManager);
        storeCommand = new StoreCommand(this, dataManager, abelixHook, battlePassManager, economyManager, tablistManager, boardManager, enchantShopMenu, storageManager);
        setExecutor("store", storeCommand);
        Bukkit.getPluginManager().registerEvents(storeCommand, this);
        setExecutor("enderchest", storageManager);
        Bukkit.getPluginManager().registerEvents(storageManager, this);
        Bukkit.getPluginManager().registerEvents(playerNetworthCacheManager, this);
        playerNetworthCacheManager.start();
        Bukkit.getPluginManager().registerEvents(enchantShopMenu, this);
        BoostersCommand boostersCommand = new BoostersCommand(this, dataManager);
        setExecutor("boosters", boostersCommand);
        CosmeticsCommand cosmeticsCommand = new CosmeticsCommand(this, dataManager);
        setExecutor("cosmetics", cosmeticsCommand);
        itemBrowserCommand = new ItemBrowserCommand(this, customItemManager, customEnchantManager, collectionManager);
        setExecutor("itembrowser", itemBrowserCommand);
        setExecutor("edititem", new EditItemCommand(this, customItemManager));
        setExecutor("maxiteminhand", new MaxItemInHandCommand(this, customItemManager));
        setExecutor("addskill", new AdminProgressCommand(this, AdminProgressCommand.Type.SKILL));
        setExecutor("addjob", new AdminProgressCommand(this, AdminProgressCommand.Type.JOB));
        sandboxCommand = new SandboxCommand(this, skillManager, jobManager, economyManager, playerLevelManager, customItemManager);
        setExecutor("sandbox", sandboxCommand);
        CollectionsCommand collectionsCommand = new CollectionsCommand(this, collectionManager, customItemManager, recipeManager);
        setExecutor("collections", collectionsCommand);
        CompanionsCommand companionsCommand = new CompanionsCommand(this, companionManager);
        setExecutor("companions", companionsCommand);
        KitCommand kitCommand = new KitCommand(this, kitManager);
        setExecutor("kit", kitCommand);
        setExecutor("anvil", new AnvilCommand(this));
        setExecutor("rename", new RenameCommand(this, dupeDetectionManager));
        setExecutor("hat", new HatCommand(this));
        setExecutor("socialspy", new SocialSpyCommand(this, socialSpyManager));
        DungeonCommand dungeonCommand = new DungeonCommand(this, dungeonManager);
        setExecutor("dungeon", dungeonCommand);
        setExecutor("createdungeon", dungeonCommand);
        setExecutor("setdungeonportal", dungeonCommand);
        setExecutor("setmobspawnlocation", dungeonCommand);
        setExecutor("addmobspawnlocation", dungeonCommand);
        setExecutor("setbosslocation", dungeonCommand);
        setExecutor("setdungeonarea", dungeonCommand);
        setExecutor("setdungeonspawn", dungeonCommand);
        if (dungeonCommand.menus() != null) Bukkit.getPluginManager().registerEvents(dungeonCommand.menus(), this);
        Bukkit.getPluginManager().registerEvents(dungeonSpectateService, this);
    }

    private void setExecutor(String command, org.bukkit.command.CommandExecutor executor) {
        PluginCommand pluginCommand = getCommand(command);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {
                pluginCommand.setTabCompleter(tabCompleter);
            }
        }
    }

    private void registerListeners() {
        if (sandboxCommand != null) Bukkit.getPluginManager().registerEvents(sandboxCommand, this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this, dataManager, spawnManager, tablistManager, boardManager), this);
        Bukkit.getPluginManager().registerEvents(macrocheckManager, this);
        Bukkit.getPluginManager().registerEvents(new LiveControlListener(liveControlManager), this);
        Bukkit.getPluginManager().registerEvents(new DevControlListener(this, liveControlManager, devControlMenu), this);
        Bukkit.getPluginManager().registerEvents(new ChatFilterListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LanguageListener(languageManager, languageCommand), this);
        Bukkit.getPluginManager().registerEvents(new PlaytimeRewardsListener(playtimeRewardManager, playtimeRewardsCommand), this);
        Bukkit.getPluginManager().registerEvents(new MainMenuItemListener(this, menuManager), this);
        Bukkit.getPluginManager().registerEvents(new BackListener(backManager, spawnManager, homeManager), this);
        Bukkit.getPluginManager().registerEvents(new StaffPermissionListener(staffPermissionManager), this);
        Bukkit.getPluginManager().registerEvents(new ProgressListener(this, skillManager, jobManager), this);
        Bukkit.getPluginManager().registerEvents(skillManager, this);
        if (playerLevelCommand != null) Bukkit.getPluginManager().registerEvents(playerLevelCommand, this);
        BankMenu bankMenu = new BankMenu(this, bankManager, economyManager);
        Bukkit.getPluginManager().registerEvents(bankMenu, this);
        Bukkit.getPluginManager().registerEvents(new NpcClickListener(this, npcManager, tablistManager, questManager, menuManager, bankMenu), this);
        Bukkit.getPluginManager().registerEvents(mayorManager, this);
        Bukkit.getPluginManager().registerEvents(new NpcVisibilityListener(this, npcManager), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this, jobManager, skillManager, menuManager, auctionHouseManager, economyManager, bazaarManager, questManager, customItemManager, customEnchantManager, bankMenu), this);
        Bukkit.getPluginManager().registerEvents(new SellAllCommand(this, economyManager, customItemManager, bazaarManager, mmoItemValueService), this);
        Bukkit.getPluginManager().registerEvents(new RecipeCraftListener(recipeManager), this);
        if (recipesCommand != null) Bukkit.getPluginManager().registerEvents(recipesCommand, this);
        if (enchantTableCommand != null) Bukkit.getPluginManager().registerEvents(enchantTableCommand, this);
        if (equipmentCommand != null) Bukkit.getPluginManager().registerEvents(equipmentCommand, this);
        Bukkit.getPluginManager().registerEvents(new PveListener(this, pveManager, spawnProtectionManager, zoneManager), this);
        Bukkit.getPluginManager().registerEvents(new SpawnProtectionListener(this, spawnProtectionManager, zoneManager), this);
        smpDeathMessageListener = new SmpDeathMessageListener(this, abelixHook);
        Bukkit.getPluginManager().registerEvents(smpDeathMessageListener, this);
        if (networkService.canRunHubZoneSystems()) Bukkit.getPluginManager().registerEvents(new ZoneListener(this, zoneManager), this);
        Bukkit.getPluginManager().registerEvents(adminWorldEditManager, this);
        Bukkit.getPluginManager().registerEvents(furnitureManager, this);
        Bukkit.getPluginManager().registerEvents(new LootBoxListener(lootBoxManager), this);
        Bukkit.getPluginManager().registerEvents(new VanishListener(vanishManager), this);
        Bukkit.getPluginManager().registerEvents(new DupeDetectionListener(dupeDetectionManager), this);
        Bukkit.getPluginManager().registerEvents(new XrayDetectionListener(xrayDetectionManager), this);
        Bukkit.getPluginManager().registerEvents(new ItemRarityListener(this, itemRarityManager, economyManager, economyAuditManager), this);
        mmoStatService = new MMOStatService(this, customItemManager, skillManager, manaActionBarManager);
        mmoStatService.setLevelStatProvider((player, stat) -> playerLevelManager.getStatBonus(player, stat.name().toLowerCase(java.util.Locale.ROOT)));
        mmoStatService.setAccessoryProvider(equipmentCommand::equippedAccessories);
        skillManager.setPassiveSkillXpMultiplier((player, skill) -> {
            club.abelix.games.fakevival.mmo.MMOStatType type = switch (skill.toLowerCase(java.util.Locale.ROOT)) {
                case "alchemy" -> club.abelix.games.fakevival.mmo.MMOStatType.ALCHEMY_XP_BOOST;
                case "enchanting" -> club.abelix.games.fakevival.mmo.MMOStatType.ENCHANTING_XP_BOOST;
                case "repair" -> club.abelix.games.fakevival.mmo.MMOStatType.REPAIR_XP_BOOST;
                case "smelting" -> club.abelix.games.fakevival.mmo.MMOStatType.SMELTING_XP_BOOST;
                default -> null;
            };
            return type == null ? 1.0D : 1.0D + Math.max(0.0D, mmoStatService.getTotalStats(player).get(type)) / 100.0D;
        });
        mmoStatService.setEventStatProvider((player, stat) -> {
            double base = eventManager.statBonus(player, stat);
            if (stat == club.abelix.games.fakevival.mmo.MMOStatType.FISHING_FORTUNE
                    || stat == club.abelix.games.fakevival.mmo.MMOStatType.SEA_CREATURE_CHANCE
                    || stat == club.abelix.games.fakevival.mmo.MMOStatType.TREASURE_CHANCE) {
                return base + mayorManager.applyMayorModifier(player, MayorModifierType.FISHING_LUCK, 100.0D) - 100.0D;
            }
            return base;
        });
        mmoDamageIndicatorService = new MMODamageIndicatorService(this);
        manaActionBarManager.setMmoStatProvider(mmoStatService::getTotalStats, mmoStatService::currentMmoHealth, mmoStatService::maxMmoHealth);
        tablistManager.configureWidgets(bankManager, zoneManager, dungeonManager, mmoStatService, manaActionBarManager,
                companionManager, playtimeRewardManager);
        tablistManager.setEventManager(eventManager);
        mmoStatService.start();
        Bukkit.getPluginManager().registerEvents(mmoDamageIndicatorService, this);
        Bukkit.getPluginManager().registerEvents(combatDebugManager, this);
        Bukkit.getPluginManager().registerEvents(playerDataSyncManager, this);
        Bukkit.getPluginManager().registerEvents(new MMOSystemListener(this, customItemManager, mmoStatService, mmoDamageIndicatorService, zoneManager, pveManager, combatDebugManager), this);
        Bukkit.getPluginManager().registerEvents(eventManager, this);
        Bukkit.getPluginManager().registerEvents(new CustomItemListener(this, customItemManager), this);
        shortbowListener = new ShortbowListener(this, customItemManager, customEnchantManager);
        Bukkit.getPluginManager().registerEvents(shortbowListener, this);
        Bukkit.getPluginManager().registerEvents(new CompanionListener(companionManager), this);
        fishingGearListener = new FishingGearListener(this, customItemManager,
                getStaticGameRegistries().lavaFishing(), skillManager, collectionManager, mmoStatService,
                zoneManager, economyManager, dataManager);
        Bukkit.getPluginManager().registerEvents(fishingGearListener, this);
        Bukkit.getPluginManager().registerEvents(fishingGearListener.getMinigame(), this);
        cookingListener = new CookingListener(this, customItemManager);
        Bukkit.getPluginManager().registerEvents(cookingListener, this);
        setExecutor("cooking", cookingListener);
        customAnvilListener = new CustomAnvilListener(this, customEnchantManager, customItemManager, economyManager);
        Bukkit.getPluginManager().registerEvents(customAnvilListener, this);
        Bukkit.getPluginManager().registerEvents(new AdminCommandListener(this, discordWebhookManager), this);
        Bukkit.getPluginManager().registerEvents(new SocialSpyListener(socialSpyManager), this);
        Bukkit.getPluginManager().registerEvents(new TradeListener(tradeManager), this);
        Bukkit.getPluginManager().registerEvents(new RegionListener(regionManager), this);
        Bukkit.getPluginManager().registerEvents(new GuildClaimListener(guildManager), this);
        Bukkit.getPluginManager().registerEvents(guildDisplayListener, this);
        Bukkit.getPluginManager().registerEvents(guildsCommand, this);
        if (networkService.canRunDungeonInstances()) {
            Bukkit.getPluginManager().registerEvents(dungeonLootChestManager, this);
            Bukkit.getPluginManager().registerEvents(dungeonTrapManager, this);
            Bukkit.getPluginManager().registerEvents(new DungeonInstanceProtectionListener(this, dungeonManager, spawnProtectionManager), this);
            Bukkit.getPluginManager().registerEvents(new DungeonListener(this, dungeonManager), this);
            Bukkit.getPluginManager().registerEvents(new DungeonPortalWandListener(this, dungeonManager), this);
        }
        Bukkit.getPluginManager().registerEvents(new BoostersCommand(this, dataManager), this);
        Bukkit.getPluginManager().registerEvents(new CosmeticsCommand(this, dataManager), this);
        Bukkit.getPluginManager().registerEvents(itemBrowserCommand, this);
        Bukkit.getPluginManager().registerEvents(new CollectionsCommand(this, collectionManager, customItemManager, recipeManager), this);
        Bukkit.getPluginManager().registerEvents(new CompanionsCommand(this, companionManager), this);
        Bukkit.getPluginManager().registerEvents(new CollectionListener(this, collectionManager, customItemManager), this);
        Bukkit.getPluginManager().registerEvents(cosmeticEffectManager, this);
        Bukkit.getPluginManager().registerEvents(customEnchantManager, this);
    }


    public ConfigFileManager getConfigFileManager() {
        return Objects.requireNonNull(configFileManager);
    }

    public StaticGameRegistries getStaticGameRegistries() {
        return configFileManager.getRegistries();
    }

    public SMPNetworkService getNetworkService() {
        return Objects.requireNonNull(networkService);
    }

    public DataManager getDataManager() {
        return Objects.requireNonNull(dataManager);
    }

    private void ensureVillageShopDefaultVersion() {
        int required = club.abelix.games.fakevival.config.defaults.NpcShopDefaults.VILLAGE_SHOP_DEFAULT_VERSION;
        int applied = dataManager.getShopDefaultVersion("village_shops");
        if (applied >= required) {
            getLogger().info("[SMP Shops] Village shop defaults version=" + applied + " already applied; source=JAVA_DEFAULTS");
            return;
        }
        ConfigFileManager.VillageShopReloadResult result = configFileManager.reloadVillageShopDefaults();
        dataManager.setShopDefaultVersion("village_shops", required);
        getLogger().info("[SMP Shops] Applied village shop defaults version=" + required
                + " updated=" + result.updatedShops() + " syncedItems=" + result.syncedItemDefinitions()
                + " source=JAVA_DEFAULTS");
    }

    public ConfigFileManager.VillageShopReloadResult reloadVillageShopDefaults() {
        ConfigFileManager.VillageShopReloadResult result = configFileManager.reloadVillageShopDefaults();
        dataManager.setShopDefaultVersion("village_shops",
                club.abelix.games.fakevival.config.defaults.NpcShopDefaults.VILLAGE_SHOP_DEFAULT_VERSION);
        return result;
    }

    public LiveControlManager getLiveControlManager() {
        return Objects.requireNonNull(liveControlManager);
    }

    public AbelixHook getAbelixHook() {
        return Objects.requireNonNull(abelixHook);
    }

    public SpawnManager getSpawnManager() {
        return Objects.requireNonNull(spawnManager);
    }

    public SkillManager getSkillManager() {
        return Objects.requireNonNull(skillManager);
    }

    public PlayerLevelManager getPlayerLevelManager() {
        return Objects.requireNonNull(playerLevelManager);
    }

    public JobManager getJobManager() {
        return Objects.requireNonNull(jobManager);
    }

    public TablistManager getTablistManager() {
        return Objects.requireNonNull(tablistManager);
    }

    public BoardManager getBoardManager() {
        return Objects.requireNonNull(boardManager);
    }

    public NpcManager getNpcManager() {
        return Objects.requireNonNull(npcManager);
    }

    public MenuManager getMenuManager() {
        return Objects.requireNonNull(menuManager);
    }

    public ProgressBossBarManager getProgressBossBarManager() {
        return Objects.requireNonNull(progressBossBarManager);
    }

    public SpawnProtectionManager getSpawnProtectionManager() {
        return Objects.requireNonNull(spawnProtectionManager);
    }

    public HomeManager getHomeManager() {
        return Objects.requireNonNull(homeManager);
    }

    public TeleportRequestManager getTeleportRequestManager() {
        return Objects.requireNonNull(teleportRequestManager);
    }

    public VanishManager getVanishManager() {
        return Objects.requireNonNull(vanishManager);
    }

    public DiscordWebhookManager getDiscordWebhookManager() {
        return Objects.requireNonNull(discordWebhookManager);
    }

    public SmpAlertService getSmpAlertService() { return Objects.requireNonNull(smpAlertService); }
    public MacrocheckManager getMacrocheckManager() { return Objects.requireNonNull(macrocheckManager); }

    public DupeDetectionManager getDupeDetectionManager() {
        return Objects.requireNonNull(dupeDetectionManager);
    }

    public XrayDetectionManager getXrayDetectionManager() {
        return Objects.requireNonNull(xrayDetectionManager);
    }

    public QuestManager getQuestManager() {
        return Objects.requireNonNull(questManager);
    }
    public AdventureGuildManager getAdventureGuildManager(){return adventureGuildManager;}

    public RegionManager getRegionManager() {
        return Objects.requireNonNull(regionManager);
    }

    public DungeonManager getDungeonManager() {
        return Objects.requireNonNull(dungeonManager);
    }

    public DungeonLootChestManager getDungeonLootChestManager() { return Objects.requireNonNull(dungeonLootChestManager); }
    public DungeonTrapManager getDungeonTrapManager() { return Objects.requireNonNull(dungeonTrapManager); }

    public CombatDebugManager getCombatDebugManager() {
        return Objects.requireNonNull(combatDebugManager);
    }

    public EconomyManager getEconomyManager() {
        return Objects.requireNonNull(economyManager);
    }

    public BankManager getBankManager() {
        return Objects.requireNonNull(bankManager);
    }

    public BattlePassManager getBattlePassManager() {
        return Objects.requireNonNull(battlePassManager);
    }

    public AuctionHouseManager getAuctionHouseManager() {
        return Objects.requireNonNull(auctionHouseManager);
    }

    public BazaarManager getBazaarManager() {
        return Objects.requireNonNull(bazaarManager);
    }

    public ItemRarityManager getItemRarityManager() {
        return Objects.requireNonNull(itemRarityManager);
    }

    public CustomItemManager getCustomItemManager() {
        return Objects.requireNonNull(customItemManager);
    }

    public FurnitureRegistry getFurnitureRegistry() { return Objects.requireNonNull(furnitureRegistry); }
    public FurnitureManager getFurnitureManager() { return Objects.requireNonNull(furnitureManager); }
    public StorageManager getStorageManager() { return Objects.requireNonNull(storageManager); }
    public DungeonSpectateService getDungeonSpectateService() { return Objects.requireNonNull(dungeonSpectateService); }

    public LootBoxManager getLootBoxManager() {
        return Objects.requireNonNull(lootBoxManager);
    }

    public RecipeManager getRecipeManager() {
        return Objects.requireNonNull(recipeManager);
    }

    public KitManager getKitManager() {
        return Objects.requireNonNull(kitManager);
    }

    public StaffPermissionManager getStaffPermissionManager() {
        return Objects.requireNonNull(staffPermissionManager);
    }

    public GuildManager getGuildManager() {
        return Objects.requireNonNull(guildManager);
    }

    public ZoneManager getZoneManager() {
        return Objects.requireNonNull(zoneManager);
    }

    public AdminWorldEditManager getAdminWorldEditManager() {
        return Objects.requireNonNull(adminWorldEditManager);
    }

    public EventManager getEventManager() {
        return Objects.requireNonNull(eventManager);
    }

    public MayorManager getMayorManager() { return Objects.requireNonNull(mayorManager); }

    public PveManager getPveManager() {
        return Objects.requireNonNull(pveManager);
    }

    public LanguageManager getLanguageManager() {
        return Objects.requireNonNull(languageManager);
    }

    public PlaytimeRewardManager getPlaytimeRewardManager() {
        return Objects.requireNonNull(playtimeRewardManager);
    }

    public ManaActionBarManager getManaActionBarManager() {
        return Objects.requireNonNull(manaActionBarManager);
    }

    public MMOStatService getMmoStatService() { return mmoStatService; }

    public PlayerDataSyncManager getPlayerDataSyncManager() { return Objects.requireNonNull(playerDataSyncManager); }

    public SmpDeathMessageListener getSmpDeathMessageListener() { return Objects.requireNonNull(smpDeathMessageListener); }

    public CosmeticEffectManager getCosmeticEffectManager() {
        return Objects.requireNonNull(cosmeticEffectManager);
    }

    public CustomEnchantManager getCustomEnchantManager() {
        return Objects.requireNonNull(customEnchantManager);
    }

    public EnchantTableCommand getEnchantTableCommand() {
        return Objects.requireNonNull(enchantTableCommand);
    }
}
