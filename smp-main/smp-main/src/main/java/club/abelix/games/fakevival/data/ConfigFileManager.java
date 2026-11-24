package club.abelix.games.fakevival.data;

import club.abelix.games.fakevival.registries.StaticGameRegistries;
import club.abelix.games.fakevival.config.defaults.RecipeDefaults;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ConfigFileManager {
    private final JavaPlugin plugin;
    private final StaticGameRegistries registries;
    private FileConfiguration skills;
    private FileConfiguration jobs;
    private FileConfiguration skillTrees;
    private FileConfiguration playerLevels;
    private FileConfiguration shops;
    private FileConfiguration bazaar;
    private FileConfiguration ranks;
    private FileConfiguration quests;
    private FileConfiguration npcQuests;
    private FileConfiguration collections;
    private FileConfiguration store;
    private FileConfiguration chatFilter;
    private FileConfiguration companions;
    private FileConfiguration guilds;
    private FileConfiguration zones;
    private FileConfiguration dungeons;
    private FileConfiguration languages;
    private FileConfiguration playtimeRewards;
    private FileConfiguration lootBoxes;

    public ConfigFileManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.registries = new StaticGameRegistries(plugin);
    }

    public void setup() {
        ensureMainConfigDefaults();
        reloadRegistries();
        mergeIntoMainConfig();
        club.abelix.games.fakevival.config.defaults.NpcShopDefaults.logRegistrations(plugin,
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin));
        plugin.getLogger().info("Loaded static SMP registries: "
                + registries.shops().all().size() + " shop items, "
                + registries.bazaar().all().size() + " bazaar items, "
                + registries.skills().all().size() + " skills, "
                + registries.jobs().all().size() + " jobs, "
                + (registries.quests().all().size() + registries.quests().allNpcQuests().size()) + " quests, "
                + registries.battlepass().all().size() + " battlepass tiers, "
                + registries.lootBoxes().all().size() + " loot box rewards, "
                + registries.playtimeRewards().all().size() + " playtime rewards, "
                + registries.collections().all().size() + " collections, "
                + registries.companions().all().size() + " companions.");
    }

    public void reload() {
        reloadRegistries();
        mergeIntoMainConfig();
    }

    /** Replaces only versioned village shop defaults in the live runtime registry. */
    public VillageShopReloadResult reloadVillageShopDefaults() {
        registries.shops().reload(plugin);
        shops = registries.shops().config();
        FileConfiguration runtime = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        int updated = 0, syncedItems = 0;

        // Replace the managed cooking/food definitions as well as the shop rows. Merely reloading the
        // shop entries leaves an older RECIPE_MATERIAL/FOOD definition in the live config and the menu
        // continues to filter it. These IDs are Java defaults and are safe to refresh independently of
        // player data and unrelated administrator-created items.
        Set<String> managedFoodItems = new LinkedHashSet<>(
                club.abelix.games.fakevival.config.defaults.ShopFoodItemDefaults.cookingIngredientIds());
        managedFoodItems.addAll(club.abelix.games.fakevival.config.defaults.ShopFoodItemDefaults.customFoodIds());
        // The Adventure Guild shop row already exists; sync its corrected Java item definition so a
        // defaults reload can repair a live server without waiting for a full restart.
        managedFoodItems.add("royal_scrap");
        for (String customId : managedFoodItems) {
            ConfigurationSection itemSource = shops.getConfigurationSection("custom-items.items." + customId);
            if (itemSource == null) {
                plugin.getLogger().warning("[SMP Shops] Missing Java food item default " + customId + "; skipped safely.");
                continue;
            }
            runtime.set("custom-items.items." + customId, null);
            copySection(itemSource, ensureSection(runtime, "custom-items.items." + customId));
            syncedItems++;
        }

        Set<String> refreshedShopIds = new LinkedHashSet<>(
                club.abelix.games.fakevival.config.defaults.NpcShopDefaults.villageIds());
        refreshedShopIds.addAll(club.abelix.games.fakevival.config.defaults.ShopFoodItemDefaults.shopIds());
        Set<String> migratedVendorLore = new LinkedHashSet<>();
        for (String shopId : refreshedShopIds) {
            if (club.abelix.games.fakevival.config.defaults.NpcShopDefaults.isUnchangedCastleService(shopId)) continue;
            if (club.abelix.games.fakevival.config.defaults.NpcShopDefaults.isSpecialMenu(shopId)) continue;
            String path = "shops." + shopId;
            ConfigurationSection source = shops.getConfigurationSection(path);
            if (source == null) {
                plugin.getLogger().warning("[SMP Shops] Missing Java default shop " + shopId + "; skipped safely.");
                continue;
            }
            runtime.set(path, null);
            copySection(source, runtime.createSection(path));
            ConfigurationSection entries = source.getConfigurationSection("items");
            int count = entries == null ? 0 : entries.getKeys(false).size();
            if (entries != null) for (String entry : entries.getKeys(false)) {
                String customId = entries.getString(entry + ".custom-item", "");
                if (customId.isBlank()) continue;
                ConfigurationSection itemSource = shops.getConfigurationSection("custom-items.items." + customId);
                if (runtime.contains("custom-items.items." + customId)) {
                    String lorePath = "custom-items.items." + customId + ".lore";
                    if (migratedVendorLore.add(customId)
                            && club.abelix.games.fakevival.util.VendorLoreSanitizer.containsBlockedLore(runtime.getStringList(lorePath))) {
                        List<String> sourceLore = itemSource == null ? List.of()
                                : club.abelix.games.fakevival.util.VendorLoreSanitizer.sanitizeVendorLore(itemSource.getStringList("lore"));
                        if (sourceLore.isEmpty()) {
                            String type = runtime.getString("custom-items.items." + customId + ".type", "MATERIAL");
                            sourceLore = club.abelix.games.fakevival.config.defaults.NpcShopDefaults.vendorLore(customId, type);
                        }
                        runtime.set(lorePath, sourceLore);
                        syncedItems++;
                        plugin.getLogger().info("[SMP Shops] Replaced internal placeholder lore for " + customId + ".");
                    }
                    continue;
                }
                if (itemSource == null) {
                    plugin.getLogger().warning("[SMP Shops] Missing custom item " + customId + " for " + shopId + "; entry will be skipped by the menu.");
                    continue;
                }
                copySection(itemSource, ensureSection(runtime, "custom-items.items." + customId));
                syncedItems++;
            }
            updated++;
            plugin.getLogger().info("[SMP Shops] Registered shop " + shopId + " entries=" + count + " source=JAVA_DEFAULTS");
        }
        return new VillageShopReloadResult(updated, syncedItems);
    }

    public record VillageShopReloadResult(int updatedShops, int syncedItemDefinitions) {}

    private void reloadRegistries() {
        registries.reload(plugin);
        skills = registries.skills().config();
        jobs = registries.jobs().config();
        skillTrees = registries.skillTrees().config();
        playerLevels = registries.playerLevels().config();
        shops = registries.shops().config();
        bazaar = registries.bazaar().config();
        ranks = registries.systems().ranks().config();
        quests = registries.quests().config();
        npcQuests = registries.quests().npcQuests().config();
        collections = registries.collections().config();
        store = registries.store().config();
        chatFilter = registries.systems().chatFilter().config();
        companions = registries.companions().config();
        guilds = registries.systems().guilds().config();
        zones = registries.systems().zones().config();
        dungeons = registries.systems().dungeons().config();
        languages = registries.systems().languages().config();
        playtimeRewards = registries.playtimeRewards().config();
        lootBoxes = registries.lootBoxes().config();
    }

    private void ensureMainConfigDefaults() {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).contains("spawnprotection.difficulty")) {
            club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("spawnprotection.difficulty", "EASY");
        }
    }

    public void saveMainConfigWithoutExternalSections() {
        // Static SMP settings are Java-backed and are not persisted to pod-local settings files.
    }

    public void mergeIntoMainConfig() {
        FileConfiguration config = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);

        config.set("skills", null);
        copySection(skills.getConfigurationSection("skills"), config.createSection("skills"));

        if (jobs.contains("jobs-enabled")) config.set("jobs-enabled", jobs.getBoolean("jobs-enabled", true));
        config.set("job-leveling", null);
        copySection(jobs.getConfigurationSection("job-leveling"), config.createSection("job-leveling"));
        config.set("jobs", null);
        copySection(jobs.getConfigurationSection("jobs"), config.createSection("jobs"));

        ConfigurationSection treeSkills = skillTrees.getConfigurationSection("skills");
        if (treeSkills != null) {
            copySection(treeSkills.getConfigurationSection("mining-tree"), ensureSection(config, "skills.mining-tree"));
            copySection(treeSkills.getConfigurationSection("trees"), ensureSection(config, "skills.trees"));
        }

        config.set("player-levels", null);
        copySection(playerLevels.getConfigurationSection("player-levels"), ensureSection(config, "player-levels"));

        config.set("menus.shop", null);
        copySection(shops.getConfigurationSection("menus.shop"), ensureSection(config, "menus.shop"));
        config.set("npcs.shops", null);
        copySection(shops.getConfigurationSection("npcs.shops"), ensureSection(config, "npcs.shops"));
        copySection(bazaar.getConfigurationSection("npcs.shops"), ensureSection(config, "npcs.shops"));
        config.set("shops", null);
        copySection(shops.getConfigurationSection("shops"), ensureSection(config, "shops"));

        config.set("menus.bazaar", null);
        copySection(bazaar.getConfigurationSection("menus.bazaar"), ensureSection(config, "menus.bazaar"));
        config.set("bazaar", null);
        copySection(bazaar.getConfigurationSection("bazaar"), ensureSection(config, "bazaar"));
        config.set("exchange", null);

        config.set("adventure-guild", null);
        copySection(quests.getConfigurationSection("adventure-guild"), ensureSection(config, "adventure-guild"));

        // NPC quest definitions are Java registry-backed. Clear any old merged runtime copy.
        config.set("quests.npcs", null);

        config.set("custom-items", null);
        copySection(collections.getConfigurationSection("custom-items"), ensureSection(config, "custom-items"));
        // Resolve and normalize cooking/shop items before scanning shop references. This prevents valid
        // Java defaults from being reported or cached as missing merely because recipes load later.
        club.abelix.games.fakevival.config.defaults.ShopFoodItemDefaults.apply(config);
        // Vendor placeholders live in the shop registry. Only merge referenced IDs that are still missing;
        // never let a placeholder overwrite a real collection/custom item definition.
        mergeMissingReferencedShopItems(shops, config);
        config.set("collections", null);
        copySection(collections.getConfigurationSection("collections"), ensureSection(config, "collections"));

        config.set("store", null);
        copySection(store.getConfigurationSection("store"), ensureSection(config, "store"));

        config.set("chat-filter", null);
        copySection(chatFilter.getConfigurationSection("chat-filter"), ensureSection(config, "chat-filter"));

        config.set("companions", null);
        copySection(companions.getConfigurationSection("companions"), ensureSection(config, "companions"));

        config.set("guilds", null);
        copySection(guilds.getConfigurationSection("guilds"), ensureSection(config, "guilds"));

        config.set("zones", null);
        copySection(zones.getConfigurationSection("zones"), ensureSection(config, "zones"));

        config.set("dungeons", null);
        copySection(dungeons.getConfigurationSection("dungeons"), ensureSection(config, "dungeons"));

        config.set("languages", null);
        copySection(languages.getConfigurationSection("languages"), ensureSection(config, "languages"));

        config.set("playtime-rewards", null);
        copySection(playtimeRewards.getConfigurationSection("playtime-rewards"), ensureSection(config, "playtime-rewards"));

        config.set("loot-boxes", null);
        copySection(lootBoxes.getConfigurationSection("loot-boxes"), ensureSection(config, "loot-boxes"));

        RecipeDefaults.apply(config);
    }

    private ConfigurationSection ensureSection(FileConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        return section != null ? section : config.createSection(path);
    }

    private void copySection(ConfigurationSection from, ConfigurationSection to) {
        if (from == null || to == null) return;
        for (String key : from.getKeys(false)) {
            Object value = from.get(key);
            if (value instanceof ConfigurationSection) {
                copySection((ConfigurationSection) value, to.createSection(key));
            } else {
                to.set(key, value);
            }
        }
    }

    private int mergeMissingReferencedShopItems(ConfigurationSection shopRegistry, FileConfiguration runtime) {
        ConfigurationSection definitions = shopRegistry.getConfigurationSection("custom-items.items");
        ConfigurationSection shopSections = shopRegistry.getConfigurationSection("shops");
        if (definitions == null || shopSections == null) return 0;
        int added = 0;
        for (String shopId : shopSections.getKeys(false)) {
            ConfigurationSection entries = shopSections.getConfigurationSection(shopId + ".items");
            if (entries == null) continue;
            for (String entryId : entries.getKeys(false)) {
                String customId = entries.getString(entryId + ".custom-item", "").trim().toLowerCase(java.util.Locale.ROOT);
                if (customId.isBlank() || runtime.contains("custom-items.items." + customId)) continue;
                ConfigurationSection source = definitions.getConfigurationSection(customId);
                if (source == null) {
                    plugin.getLogger().warning("[SMP Shops] Missing custom item " + customId + " for " + shopId + "; skipped safely.");
                    continue;
                }
                copySection(source, ensureSection(runtime, "custom-items.items." + customId));
                added++;
            }
        }
        return added;
    }

    public FileConfiguration getSkills() {
        return skills;
    }

    public FileConfiguration getJobs() {
        return jobs;
    }

    public FileConfiguration getSkillTrees() {
        return skillTrees;
    }

    public FileConfiguration getPlayerLevels() {
        return playerLevels;
    }

    public FileConfiguration getShops() {
        return shops;
    }

    public FileConfiguration getBazaar() {
        return bazaar;
    }

    public FileConfiguration getRanks() {
        return ranks;
    }

    public FileConfiguration getQuests() {
        return quests;
    }

    public FileConfiguration getNpcQuests() {
        return npcQuests;
    }

    public FileConfiguration getCollections() {
        return collections;
    }

    public FileConfiguration getStore() {
        return store;
    }

    public FileConfiguration getChatFilter() {
        return chatFilter;
    }

    public FileConfiguration getCompanions() {
        return companions;
    }

    public FileConfiguration getGuilds() {
        return guilds;
    }

    public FileConfiguration getZones() {
        return zones;
    }

    public FileConfiguration getDungeons() {
        return dungeons;
    }

    public FileConfiguration getLanguages() {
        return languages;
    }

    public FileConfiguration getPlaytimeRewards() {
        return playtimeRewards;
    }

    public FileConfiguration getLootBoxes() {
        return lootBoxes;
    }

    public StaticGameRegistries getRegistries() {
        return registries;
    }
}
