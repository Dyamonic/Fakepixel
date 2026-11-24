package club.abelix.games.fakevival.registries;

import org.bukkit.plugin.java.JavaPlugin;

public final class StaticGameRegistries {
    private final ShopRegistry shops;
    private final BazaarRegistry bazaar;
    private final StoreRegistry store;
    private final PlayerLevelRegistry playerLevels;
    private final JobRegistry jobs;
    private final SkillRegistry skills;
    private final SkillTreeRegistry skillTrees;
    private final QuestRegistry quests;
    private final BattlepassRegistry battlepass;
    private final LootBoxRegistry lootBoxes;
    private final PlaytimeRewardRegistry playtimeRewards;
    private final CollectionRegistry collections;
    private final CompanionRegistry companions;
    private final SystemDefaultsRegistry systems;
    private final LavaFishingRegistry lavaFishing;

    public StaticGameRegistries(JavaPlugin plugin) {
        shops = new ShopRegistry(plugin);
        bazaar = new BazaarRegistry(plugin);
        store = new StoreRegistry(plugin);
        playerLevels = new PlayerLevelRegistry(plugin);
        jobs = new JobRegistry(plugin);
        skills = new SkillRegistry(plugin);
        skillTrees = new SkillTreeRegistry(plugin);
        quests = new QuestRegistry(plugin);
        battlepass = new BattlepassRegistry(plugin);
        lootBoxes = new LootBoxRegistry(plugin);
        playtimeRewards = new PlaytimeRewardRegistry(plugin);
        collections = new CollectionRegistry(plugin);
        companions = new CompanionRegistry(plugin);
        systems = new SystemDefaultsRegistry(plugin);
        lavaFishing = new LavaFishingRegistry();
    }

    public void reload(JavaPlugin plugin) {
        shops.reload(plugin);
        bazaar.reload(plugin);
        store.reload(plugin);
        playerLevels.reload(plugin);
        jobs.reload(plugin);
        skills.reload(plugin);
        skillTrees.reload(plugin);
        quests.reload(plugin);
        quests.reloadNpcQuests(plugin);
        battlepass.reload(plugin);
        lootBoxes.reload(plugin);
        playtimeRewards.reload(plugin);
        collections.reload(plugin);
        companions.reload(plugin);
        systems.chatFilter().reload(plugin);
        systems.dungeons().reload(plugin);
        systems.guilds().reload(plugin);
        systems.languages().reload(plugin);
        systems.zones().reload(plugin);
        systems.hub().reload(plugin);
        systems.ranks().reload(plugin);
    }

    public ShopRegistry shops() { return shops; }
    public BazaarRegistry bazaar() { return bazaar; }
    public StoreRegistry store() { return store; }
    public PlayerLevelRegistry playerLevels() { return playerLevels; }
    public JobRegistry jobs() { return jobs; }
    public SkillRegistry skills() { return skills; }
    public SkillTreeRegistry skillTrees() { return skillTrees; }
    public QuestRegistry quests() { return quests; }
    public BattlepassRegistry battlepass() { return battlepass; }
    public LootBoxRegistry lootBoxes() { return lootBoxes; }
    public PlaytimeRewardRegistry playtimeRewards() { return playtimeRewards; }
    public CollectionRegistry collections() { return collections; }
    public CompanionRegistry companions() { return companions; }
    public SystemDefaultsRegistry systems() { return systems; }
    public LavaFishingRegistry lavaFishing() { return lavaFishing; }
}
