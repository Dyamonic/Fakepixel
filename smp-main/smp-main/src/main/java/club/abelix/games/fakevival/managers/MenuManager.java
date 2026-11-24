package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.VendorLoreSanitizer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import java.util.Objects;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class MenuManager {
    private final JavaPlugin plugin;
    private final SkillManager skillManager;
    private final JobManager jobManager;
    private final TablistManager placeholders;
    private final BattlePassManager battlePassManager;
    private final AuctionHouseManager auctionHouseManager;
    private final EconomyManager economyManager;
    private final BazaarManager bazaarManager;
    private final QuestManager questManager;
    private final CustomItemManager customItemManager;
    private final CustomEnchantManager customEnchantManager;
    private EventManager eventManager;
    private final NamespacedKey shopItemKey, shopIdKey, shopEntryIdKey, shopPreviewKey;
    private final NamespacedKey bazaarActionKey;
    private final NamespacedKey bazaarItemKey;
    private final Set<String> warnedMissingShopItems = new HashSet<>();

    public MenuManager(JavaPlugin plugin, SkillManager skillManager, JobManager jobManager, TablistManager placeholders, BattlePassManager battlePassManager, AuctionHouseManager auctionHouseManager, EconomyManager economyManager, BazaarManager bazaarManager, QuestManager questManager, CustomItemManager customItemManager, CustomEnchantManager customEnchantManager) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.jobManager = jobManager;
        this.placeholders = placeholders;
        this.battlePassManager = battlePassManager;
        this.auctionHouseManager = auctionHouseManager;
        this.economyManager = economyManager;
        this.bazaarManager = bazaarManager;
        this.questManager = questManager;
        this.customItemManager = customItemManager;
        this.customEnchantManager = customEnchantManager;
        this.shopItemKey = new NamespacedKey(plugin, "shop_item_id");
        this.shopIdKey = new NamespacedKey(plugin, "shop_id");
        this.shopEntryIdKey = new NamespacedKey(plugin, "shop_entry_id");
        this.shopPreviewKey = new NamespacedKey(plugin, "shop_preview_item");
        this.bazaarActionKey = new NamespacedKey(plugin, "bazaar_menu_action");
        this.bazaarItemKey = new NamespacedKey(plugin, "bazaar_item_id");
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void openSkills(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("skills"), 54, CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("menus.skills.title", "&bYour Skills")));
        List<String> skills = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("skills.enabled-skills");
        inventory.setItem(4, item(Material.NETHER_STAR, "&bSkill Overview", List.of(
                "&7Total Skill Levels: &a" + totalSkillLevels(player, skills),
                "&7Available Points: &e" + skillManager.getSkillPoints(player),
                "&7Top Skill: &f" + topSkill(player, skills),
                "",
                "&eLeft Click &7a skill to open its tree.",
                "&eRight Click &7a skill to view leveling."
        ), player));

        int[] slots = skillSlots();
        int index = 0;
        for (String skill : skills) {
            if (index >= slots.length) break;
            int xp = skillManager.getXp(player, skill);
            int level = skillManager.getLevel(player, skill);
            int needed = skillManager.getXpNeededForLevel(level);
            int progress = skillManager.getXpIntoLevel(player, skill);
            List<String> lore = new ArrayList<>();
            lore.add("&7Level: &a" + level);
            lore.add("&7Total XP: &e" + xp);
            lore.add("&7Next Level: &e" + progress + "&7/&e" + needed);
            lore.add(progressBar(progress, needed));
            lore.add("");
            lore.add("&7Tree Upgrades: &f" + unlockedTreeLevels(player, skill));
            lore.add("&7XP per Action: &f" + club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.xp-per-action." + skill.toLowerCase(), 0));
            lore.add("");
            lore.add("&eLeft Click &7to open tree");
            lore.add("&eRight Click &7to view leveling");
            lore.add("&8Skill: " + skill.toLowerCase());
            inventory.setItem(slots[index++], item(getSkillMaterial(skill), skillColor(skill) + pretty(skill), lore, player, level > 1));
        }

        inventory.setItem(45, item(Material.EXPERIENCE_BOTTLE, "&aSkill Points", List.of(
                "&7Available: &e" + skillManager.getSkillPoints(player),
                "&7Earn points by leveling skills.",
                "&7Spend them in skill trees."
        ), player));
        inventory.setItem(49, item(Material.BOOK, "&bLeveling Guide", List.of(
                "&7Skills gain XP from normal survival actions.",
                "&7Every level grants &e1 &7skill point.",
                "&7Tree upgrades improve long-term progression."
        ), player));
        inventory.setItem(53, item(Material.COMPASS, "&aRefresh", List.of("&7Reload your current skill progress.", "", "&eClick to refresh."), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openSkillLeveling(Player player, String skill) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("skill-levels:" + skill.toLowerCase()), 27, CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("menus.skill-leveling.title", "&bSkill Leveling")));
        int level = skillManager.getLevel(player, skill);
        int xp = skillManager.getXp(player, skill);
        int needed = skillManager.getXpNeededForLevel(level);
        int progress = skillManager.getXpIntoLevel(player, skill);
        inventory.setItem(13, item(getSkillMaterial(skill), "&b" + pretty(skill), List.of(
                "&7Current Level: &a" + level,
                "&7Total XP: &e" + xp,
                "&7Next Level: &e" + progress + "&7/&e" + needed,
                progressBar(progress, needed),
                "&7XP per Action: &f" + club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.xp-per-action." + skill.toLowerCase(), 0),
                "&7Tree Upgrades: &f" + unlockedTreeLevels(player, skill),
                "",
                "&fLevel this skill by playing normally.",
                "&8Skill: " + skill.toLowerCase()
        ), player));
        inventory.setItem(22, item(Material.ARROW, "&cBack", List.of("&7Click to go back."), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openMiningTree(Player player) {
        openSkillTree(player, "mining");
    }

    public void openSkillTree(Player player, String skill) {
        openSkillTree(player, skill, 0);
    }

    public void openSkillTree(Player player, String skill, int page) {
        String title = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("menus.skill-tree.title", "&8%skill% Skill Tree").replace("%skill%", pretty(skill));
        ConfigurationSection nodes = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("skills.trees." + skill.toLowerCase());
        List<String> nodeIds = nodes == null ? List.of() : new ArrayList<>(nodes.getKeys(false));
        int[] slots = skillTreeSlots(skill);
        int totalPages = Math.max(1, (int) Math.ceil(nodeIds.size() / (double) slots.length));
        page = Math.clamp(page, 0, totalPages - 1);

        Inventory inventory = Bukkit.createInventory(new MenuHolder("skill-tree:" + skill.toLowerCase() + ":" + page), 54, CC.color(title));
        inventory.setItem(4, item(getSkillMaterial(skill), "&b" + pretty(skill) + " Tree", List.of(
                "&7Skill Points: &e" + skillManager.getSkillPoints(player),
                "&7" + pretty(skill) + " Level: &a" + skillManager.getLevel(player, skill),
                "&7Tree Upgrades: &f" + unlockedTreeLevels(player, skill),
                "&7Nodes: &f" + nodeIds.size(),
                "&7Page: &f" + (page + 1) + "&7/&f" + totalPages,
                "",
                "&eClick &7an upgrade to spend points.",
                "&7Locked upgrades show their requirements."
        ), player));

        if (nodes != null) {
            int start = page * slots.length;
            for (int index = 0; index < slots.length && start + index < nodeIds.size(); index++) {
                String node = nodeIds.get(start + index);
                int slot = slots[index];
                String display = nodes.getString(node + ".name", "&a" + pretty(node.replace('_', ' ')));
                String description = nodes.getString(node + ".description", "&7Spend skill points to upgrade this perk.");
                Material material = materialFromConfig(nodes.getString(node + ".material", ""), getNodeMaterial(node));
                setTreeNode(inventory, player, skill, slot, node, material, display, description);
            }
        }

        inventory.setItem(45, item(Material.ARROW, "&cBack", List.of("&7Return to skills."), player));
        if (page > 0) inventory.setItem(48, item(Material.ARROW, "&ePrevious Page", List.of("&7Page " + page + " of " + totalPages), player));
        inventory.setItem(49, item(Material.EXPERIENCE_BOTTLE, "&aSkill Points", List.of(
                "&7Available: &e" + skillManager.getSkillPoints(player),
                "&7Earn more by leveling skills.",
                "",
                "&7Current Page: &f" + (page + 1) + "&7/&f" + totalPages
        ), player));
        if (page + 1 < totalPages) inventory.setItem(50, item(Material.ARROW, "&eNext Page", List.of("&7Page " + (page + 2) + " of " + totalPages), player));
        inventory.setItem(53, item(Material.COMPASS, "&aRefresh", List.of("&7Reload this skill tree.", "", "&eClick to refresh."), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openJobs(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("jobs"), 54, CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("menus.jobs.title", "&bJobs")));
        ConfigurationSection section = jobManager.getJobsSection();
        java.util.List<String> selected = jobManager.getJobs(player);
        inventory.setItem(4, item(Material.WRITABLE_BOOK, "&bJob Overview", List.of(
                "&7Joined Jobs: &a" + selected.size(),
                "&7Total Job Levels: &e" + totalJobLevels(player, section),
                "&7Best Job: &f" + topJob(player, section),
                "",
                "&eLeft Click &7a job to join.",
                "&cRight Click &7a joined job to leave."
        ), player));

        int[] slots = jobSlots();
        int index = 0;
        if (section != null) {
            for (String job : section.getKeys(false)) {
                if (!section.isConfigurationSection(job)) continue;
                if (index >= slots.length) break;
                boolean isSelected = selected.stream().anyMatch(saved -> saved.equalsIgnoreCase(job));
                int xp = jobManager.getXp(player, job);
                int level = jobManager.getLevel(player, job);
                int perLevel = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("job-leveling.xp-per-level", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.xp-per-level", 100)));
                int progress = xp % perLevel;
                inventory.setItem(slots[index++], item(getJobMaterial(job), (isSelected ? "&a" : "&b") + pretty(job), List.of(
                        "&7Status: " + (isSelected ? "&aJoined" : "&7Not Joined"),
                        "&7Level: &a" + level,
                        "&7Total XP: &e" + xp,
                        "&7Next Level: &e" + progress + "&7/&e" + perLevel,
                        progressBar(progress, perLevel),
                        "",
                        "&7Action: &f" + jobActions(section.getConfigurationSection(job)),
                        "&7XP per Action: &e" + section.getInt(job + ".xp", 0),
                        "",
                        isSelected ? "&cRight Click &7to leave" : "&eLeft Click &7to join",
                        "&7You can join multiple jobs.",
                        "&8Job: " + job.toLowerCase()
                ), player, isSelected));
            }
        }
        inventory.setItem(45, item(Material.EMERALD, "&aJoined Jobs", List.of(
                "&f" + jobManager.getJobsDisplay(player),
                "",
                "&7Joined jobs receive XP",
                "&7from their configured actions."
        ), player));
        inventory.setItem(49, item(Material.GOLD_INGOT, "&eJob Rewards", List.of(
                "&7Jobs level separately from skills.",
                "&7Higher job levels mark your",
                "&7main survival professions."
        ), player));
        inventory.setItem(53, item(Material.COMPASS, "&aRefresh", List.of("&7Reload your current job progress.", "", "&eClick to refresh."), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("main-menu"), 54, CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("main-menu.title", "&8SMP Menu")));

        inventory.setItem(4, item(Material.PLAYER_HEAD, "&aYour Profile", List.of(
                "&7View your stats, level,",
                "&7coins, health, defense,",
                "&7mana, and progress.",
                "",
                "&eLeft Click to open leveling",
                "&eRight Click to open Equipment",
                "&8Main Menu Action: profile"
        ), player, true));
        inventory.setItem(10, item(Material.DIAMOND_SWORD, "&aSkills", List.of(
                "&7View your Combat, Mining,",
                "&7Farming, Fishing, Foraging,",
                "&7and other skill levels.",
                "",
                "&eClick to view!",
                "&8Main Menu Action: skills"
        ), player, true));
        inventory.setItem(11, item(Material.BOOK, "&aCollections", List.of(
                "&7View your collection progress.",
                "&7Unlock custom items, recipes, and rewards.",
                "",
                "&eClick to open!",
                "&8Main Menu Action: collections"
        ), player));
        inventory.setItem(12, item(Material.KNOWLEDGE_BOOK, "&aRecipe Book", List.of(
                "&7View unlocked and locked recipes.",
                "&7Discover vanilla and custom crafting paths.",
                "",
                "&eClick to open!",
                "&8Main Menu Action: recipes"
        ), player));
        inventory.setItem(14, item(Material.WRITABLE_BOOK, "&aQuests", List.of(
                "&7View daily, weekly,",
                "&7and adventure quests.",
                "",
                "&eClick to view!",
                "&8Main Menu Action: quests"
        ), player));
        inventory.setItem(19, item(Material.BONE, "&aCompanions", List.of(
                "&7View and manage your",
                "&7companions.",
                "",
                "&eClick to view!",
                "&8Main Menu Action: companions"
        ), player));
        inventory.setItem(16, item(Material.ENDER_CHEST, "&aStorage", List.of(
                "&7Store extra items in your personal storage.",
                "&7Unlock more pages from the Gold Store.",
                "",
                "&eClick to open!",
                "&8Main Menu Action: storage"
        ), player));
        inventory.setItem(24, item(Material.GOLD_INGOT, "&aBank", List.of(
                "&7View your coins, purse,",
                "&7bank upgrades, and history.",
                "",
                "&eClick to view!",
                "&8Main Menu Action: bank"
        ), player));
        inventory.setItem(20, item(Material.ENDER_PEARL, "&aWaypoints", List.of(
                "&7Teleport to spawn, homes,",
                "&7or a random survival location.",
                "",
                "&eClick to view!",
                "&8Main Menu Action: warps"
        ), player));
        inventory.setItem(21, item(Material.ENDER_EYE, "&5Dungeons", List.of(
                "&7View available dungeons.",
                "&7Enter dungeons if you meet the requirements",
                "&7or have a dungeon key.",
                "",
                "&eClick to open!",
                "&8Main Menu Action: dungeons"
        ), player));
        inventory.setItem(22, item(Material.EMERALD, "&aJobs", List.of(
                "&7Select a job,",
                "",
                "&eClick to view!",
                "&8Main Menu Action: jobs"
        ), player));
        inventory.setItem(50, item(Material.COMPARATOR, "&aSettings", List.of(
                "&7Change personal settings",
                "&7for menus and gameplay.",
                "",
                "&eClick to view!",
                "&8Main Menu Action: settings"
        ), player));
        inventory.setItem(15, item(Material.CLOCK, "&aCalendar", List.of(
                "&7View upcoming SMP events,",
                "&7seasonal events, and server activities.",
                "",
                "&eClick to open!",
                "&8Main Menu Action: calendar"
        ), player));
        inventory.setItem(23, item(Material.BELL, "&aGuild", List.of(
                "&7Guild",
                "",
                "&eClick to open!",
                "&8Main Menu Action: guild"
        ), player));
        inventory.setItem(25, item(Material.FILLED_MAP, "&aDiscovered Zones", List.of(
                "&7View the zones you have discovered",
                "&7while exploring the SMP.",
                "",
                "&eClick to open!",
                "&8Main Menu Action: discovered zones"
        ), player));
        inventory.setItem(48, item(Material.EMERALD_BLOCK, "&aEvent Shop", List.of(
                "&7Spend your &eEvent Tokens",
                "&7on special rewards from",
                "&7scheduled SMP events.",
                "",
                "&eClick to open!",
                "&8Main Menu Action: event shop"
        ), player));
        inventory.setItem(13, item(Material.CRAFTING_TABLE, "&aCrafting Table", List.of(
                "&7Open a crafting table.",
                "",
                "&eClick to open!",
                "&8Main Menu Action: crafting table"
        ), player));
        if (!(plugin instanceof club.abelix.games.fakevival.Game game) || !game.getNetworkService().isHub()) {
            inventory.setItem(46, item(Material.NETHER_STAR, "&aHub Server", List.of(
                    "&7Return to an available SMP hub.",
                    "",
                    "&eClick to transfer!",
                    "&8Main Menu Action: hub"
            ), player));
        }
        inventory.setItem(45, item(Material.GOLD_INGOT, "&6Gold Store", List.of(
                "&7Spend your Gold on store items.",
                "",
                "&eClick to open!",
                "&8Main Menu Action: goldstore"
        ), player));

        inventory.setItem(49, item(Material.BARRIER, "&cClose", List.of("&8Main Menu Action: close"), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openDungeons(Player player) {
        if (!(plugin instanceof club.abelix.games.fakevival.Game game)) return;
        DungeonManager dungeonManager = game.getDungeonManager();
        Inventory inventory = Bukkit.createInventory(new MenuHolder("dungeons"), 54, CC.color("&8Dungeons"));
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        List<DungeonManager.DungeonDefinition> dungeons = dungeonManager.dungeonDefinitions();
        for (int i = 0; i < dungeons.size() && i < slots.length; i++) {
            DungeonManager.DungeonDefinition dungeon = dungeons.get(i);
            DungeonManager.EntryCheck check = dungeonManager.checkEntry(player, dungeon.id());
            DungeonManager.DungeonState state = dungeonManager.dungeonState(dungeon.id());
            int currentPlayers = dungeonManager.activePlayerCount(dungeon.id());
            int maxPlayers = dungeon.maxPlayers();
            List<String> lore = new ArrayList<>();
            lore.add("&7" + dungeon.description());
            lore.add("");
            lore.add("&7Recommended Level: &e" + dungeon.recommendedLevel());
            lore.add("&7Boss: &c" + dungeon.bossName());
            lore.add("&7Players: &e" + currentPlayers + "&7/&e" + maxPlayers);
            lore.add("&7Status: " + dungeonStatus(state, check.allowed()));
            lore.add("");
            lore.add("&eRequirements:");
            if (dungeon.requiredSkills().isEmpty()) lore.add("&7None");
            else dungeon.requiredSkills().forEach((skill, level) -> lore.add("&7- &f" + pretty(skill) + " Level &e" + level));
            if (!dungeon.requiredKeyId().isBlank()) lore.add("&7- &fKey: &e" + pretty(dungeon.requiredKeyId()));
            lore.add("");
            lore.add(check.allowed() ? "&eClick to enter!" : check.message());
            lore.add("&8Dungeon: " + dungeon.id());
            inventory.setItem(slots[i], item(dungeon.material(), (check.allowed() ? "&5" : "&8") + dungeon.displayName(), lore, player, check.allowed()));
        }
        if (dungeons.isEmpty()) inventory.setItem(22, item(Material.BARRIER, "&cNo Dungeons", List.of("&7No dungeons are registered."), player));
        inventory.setItem(49, item(Material.BARRIER, "&cBack", List.of("&7Return to the Survival Menu."), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    private String dungeonStatus(DungeonManager.DungeonState state, boolean joinable) {
        return switch (state) {
            case AVAILABLE -> joinable ? "&aAvailable" : "&cLocked";
            case ACTIVE -> "&eIn Progress";
            case EMPTY_WAITING_FOR_REJOIN -> "&eWaiting for Rejoin";
            case COMPLETED -> "&aCompleted";
            case RESETTING, RESTORING -> "&cRestoring";
            case EXPIRED -> "&cExpired";
        };
    }

    public void openDiscoveredZones(Player player, int page) {
        if (!(plugin instanceof club.abelix.games.fakevival.Game game)) return;
        List<ZoneManager.ZoneTravelInfo> zones = game.getZoneManager().discoveredZones(player);
        int[] slots = zoneMenuSlots();
        int totalPages = Math.max(1, (int) Math.ceil(zones.size() / (double) slots.length));
        int safePage = Math.clamp(page, 0, totalPages - 1);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("discovered-zones:" + safePage), 54, CC.color("&8Discovered Zones"));
        int start = safePage * slots.length;
        for (int index = 0; index < slots.length && start + index < zones.size(); index++) {
            ZoneManager.ZoneTravelInfo zone = zones.get(start + index);
            inventory.setItem(slots[index], item(zone.icon(), "&a" + zone.name(), List.of(
                    "&7Zone ID: &e" + zone.id(),
                    "&7Level: &c" + zone.level(),
                    "&7Type: &b" + pretty(zone.type()),
                    "&7Discovered: &a" + discoveredDate(zone.discoveredAt()),
                    "",
                    "&7Travel: " + zone.travelStatus(),
                    "",
                    "&eClick to view!",
                    "&8Discovered Zone: " + zone.id()
            ), player));
        }
        if (zones.isEmpty()) inventory.setItem(22, item(Material.MAP, "&cNo Zones Discovered", List.of("&7Explore the SMP to discover zones."), player));
        if (safePage > 0) inventory.setItem(48, item(Material.ARROW, "&aPrevious Page", List.of("&7Page " + safePage + " of " + totalPages), player));
        inventory.setItem(49, item(Material.BARRIER, "&cBack", List.of("&7Return to the Survival Menu."), player));
        if (safePage + 1 < totalPages) inventory.setItem(50, item(Material.ARROW, "&aNext Page", List.of("&7Page " + (safePage + 2) + " of " + totalPages), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openZoneDetails(Player player, String zoneId, int returnPage) {
        if (!(plugin instanceof club.abelix.games.fakevival.Game game)) return;
        ZoneManager.ZoneTravelInfo zone = game.getZoneManager().zoneTravelInfo(player, zoneId);
        if (zone == null) {
            openDiscoveredZones(player, returnPage);
            return;
        }
        Inventory inventory = Bukkit.createInventory(new MenuHolder("zone-details:" + zone.id() + ":" + returnPage), 54, CC.color("&8" + zone.name()));
        List<String> lore = new ArrayList<>();
        lore.add("&7Type: &b" + pretty(zone.type()));
        lore.add("&7Level: &c" + zone.level());
        lore.add("&7Discovered: &a" + discoveredDate(zone.discoveredAt()));
        lore.add("&7Mobs: &f" + zone.mobs());
        lore.add("&7Bosses: &f" + zone.bosses());
        lore.add("");
        lore.add("&7Travel: " + zone.travelStatus());
        lore.addAll(zone.requirementLines());
        if (zone.hasWarp() && zone.travelUnlocked()) {
            lore.add("");
            lore.add("&eClick to travel!");
        } else if (zone.hasWarp()) {
            lore.add("");
            lore.add("&eClick to check or unlock travel.");
        } else if (!zone.hasWarp()) {
            lore.add("");
            lore.add("&cThis zone does not have a travel location set yet.");
        }
        lore.add("&8Zone Travel: " + zone.id());
        inventory.setItem(22, item(zone.icon(), "&a" + zone.name(), lore, player));
        inventory.setItem(49, item(Material.BARRIER, "&cBack", List.of("&7Return to Discovered Zones."), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openWarps(Player player, int page) {
        if (!(plugin instanceof club.abelix.games.fakevival.Game game)) return;
        List<WarpEntry> entries = new ArrayList<>();
        if (!game.getNetworkService().isHub()) entries.add(new WarpEntry(Material.COMPASS, "&aHub", List.of("&7Return to the SMP hub.", "", "&eClick to travel!"), "hub"));
        entries.add(new WarpEntry(Material.GRASS_BLOCK, "&aOverworld", List.of("&7Travel to your survival overworld.", "", "&eClick to travel!"), "overworld"));
        entries.add(new WarpEntry(Material.ENDER_EYE, "&5Dungeons", List.of("&7Travel to the dungeon lobby.", "&7Enter portals to start dungeon runs.", "", "&eClick to travel!"), "dungeons"));
        entries.add(new WarpEntry(Material.ENDER_EYE, "&aRandom Teleport", List.of("&7Travel to a random safe wilderness location.", "", "&eClick to travel!"), "rtp"));
        for (String home : game.getHomeManager().getHomes(player)) {
            entries.add(new WarpEntry(Material.RED_BED, "&aHome: " + pretty(home), List.of("&7Your saved home location.", "", "&eClick to travel!"), "home:" + home));
        }
        for (ZoneManager.ZoneTravelInfo zone : game.getZoneManager().discoveredZones(player)) {
            List<String> lore = new ArrayList<>();
            Material icon = zone.hasWarp() && zone.travelUnlocked() ? Material.ENDER_PEARL : Material.MAP;
            String name = (zone.hasWarp() && zone.travelUnlocked() ? "&a" : "&e") + zone.name();
            lore.add("&7Level: &c" + zone.level());
            lore.add("&7Type: &b" + pretty(zone.type()));
            lore.add("");
            if (!zone.hasWarp()) {
                lore.add("&7You have discovered this zone,");
                lore.add("&7but no warp location has been set.");
                lore.add("");
                lore.add("&cTravel unavailable.");
            } else if (!zone.travelUnlocked()) {
                lore.add("&7You have discovered this zone,");
                lore.add("&7but travel is not unlocked yet.");
                lore.add("");
                lore.addAll(zone.requirementLines());
            } else lore.add("&eClick to travel!");
            entries.add(new WarpEntry(icon, name, lore, "zone:" + zone.id()));
        }
        int[] slots = zoneMenuSlots();
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) slots.length));
        int safePage = Math.clamp(page, 0, totalPages - 1);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("warps:" + safePage), 54, CC.color("&8Waypoints"));
        int start = safePage * slots.length;
        for (int index = 0; index < slots.length && start + index < entries.size(); index++) {
            WarpEntry entry = entries.get(start + index);
            List<String> lore = new ArrayList<>(entry.lore());
            lore.add("&8Warp Action: " + entry.action());
            inventory.setItem(slots[index], item(entry.material(), entry.name(), lore, player));
        }
        if (safePage > 0) inventory.setItem(48, item(Material.ARROW, "&aPrevious Page", List.of("&7Page " + safePage + " of " + totalPages), player));
        inventory.setItem(49, item(Material.BARRIER, "&cBack", List.of("&7Return to the Survival Menu."), player));
        if (safePage + 1 < totalPages) inventory.setItem(50, item(Material.ARROW, "&aNext Page", List.of("&7Page " + (safePage + 2) + " of " + totalPages), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    private int[] zoneMenuSlots() {
        return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    }

    private String discoveredDate(long timestamp) {
        if (timestamp <= 0L) return "Previously discovered";
        return DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.US)
                .format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
    }

    private record WarpEntry(Material material, String name, List<String> lore, String action) {
    }

    public void openEventCalendar(Player player, int page) {
        if (eventManager == null) return;
        List<EventManager.CalendarEntry> entries = eventManager.calendarEntries();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) slots.length));
        int safePage = Math.clamp(page, 0, totalPages - 1);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("event-calendar:" + safePage), 54, CC.color("&8Event Calendar"));
        int start = safePage * slots.length;
        for (int index = 0; index < slots.length && start + index < entries.size(); index++) {
            EventManager.CalendarEntry entry = entries.get(start + index);
            inventory.setItem(slots[index], eventCalendarItem(player, entry));
        }
        if (entries.isEmpty()) inventory.setItem(22, item(Material.CLOCK, "&cNo Events Scheduled", List.of("&7No enabled events are currently available."), player));
        if (safePage > 0) inventory.setItem(48, item(Material.ARROW, "&aPrevious Page", List.of("&7Page " + safePage + " of " + totalPages), player));
        inventory.setItem(49, item(Material.BARRIER, "&cBack", List.of("&7Return to the Survival Menu."), player));
        if (safePage + 1 < totalPages) inventory.setItem(50, item(Material.ARROW, "&aNext Page", List.of("&7Page " + (safePage + 2) + " of " + totalPages), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openEventDetail(Player player, long startAt, int calendarPage) {
        if (eventManager == null) return;
        EventManager.CalendarEntry entry = eventManager.calendarEntries().stream()
                .filter(value -> value.startAt() == startAt)
                .findFirst().orElse(null);
        if (entry == null) {
            openEventCalendar(player, calendarPage);
            return;
        }
        Inventory inventory = Bukkit.createInventory(new MenuHolder("event-detail:" + startAt + ":" + calendarPage), 54,
                CC.color("&8" + entry.name()));
        List<String> lore = new ArrayList<>();
        lore.add("&7Status: " + (entry.active() ? "&aActive" : "&e" + entry.status()));
        lore.add("&7Start Time: &f" + entry.startTime());
        lore.add(entry.active() ? "&7Ends In: &e" + entry.realTimeUntil() : "&7Starts In: &a" + minecraftDayText(entry.minecraftDays()));
        if (!entry.active()) lore.add("&7Real Time: &e" + entry.realTimeUntil());
        lore.add("&7Duration: &b" + entry.duration());
        lore.add("");
        lore.add("&7Description:");
        lore.add("&f" + entry.description());
        lore.add("");
        lore.add("&7Bonuses:");
        lore.addAll(entry.bonuses());
        lore.add("");
        lore.add("&7Rewards:");
        lore.addAll(entry.rewards());
        lore.add("");
        lore.add("&7Participation:");
        lore.addAll(entry.participation());
        inventory.setItem(22, item(Material.BOOK, "&a" + entry.name(), lore, player, entry.active()));
        inventory.setItem(49, item(Material.BARRIER, "&cBack", List.of("&7Return to the Event Calendar."), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openEventShop(Player player, int page) {
        List<EventShopReward> rewards = eventShopRewards();
        int[] slots = zoneMenuSlots();
        int totalPages = Math.max(1, (int) Math.ceil(rewards.size() / (double) slots.length));
        int safePage = Math.clamp(page, 0, totalPages - 1);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("event-shop:" + safePage), 54, CC.color("&8Event Shop"));
        inventory.setItem(4, item(Material.SUNFLOWER, "&eEvent Tokens", List.of(
                "&7Your Balance: &e" + eventTokenBalance(player),
                "",
                "&7Earn tokens by participating",
                "&7in scheduled SMP events."
        ), player));
        int start = safePage * slots.length;
        for (int index = 0; index < slots.length && start + index < rewards.size(); index++) {
            EventShopReward reward = rewards.get(start + index);
            int balance = eventTokenBalance(player);
            int missing = Math.max(0, reward.cost() - balance);
            boolean purchased = isOneTimeEventShopReward(reward.id()) && hasPurchasedEventShopReward(player, reward.id());
            List<String> lore = new ArrayList<>();
            lore.add("&7Cost: &e" + reward.cost() + " Event Tokens");
            lore.add("");
            lore.addAll(reward.lore());
            lore.add("");
            lore.add(purchased ? "&cAlready purchased!" : missing == 0 ? "&eClick to buy!" : "&cYou need " + missing + " more Event Tokens.");
            lore.add("&8Event Shop Reward: " + reward.id());
            inventory.setItem(slots[index], item(reward.icon(), "&a" + reward.name(), lore, player, !purchased && missing == 0));
        }
        if (safePage > 0) inventory.setItem(48, item(Material.ARROW, "&aPrevious Page", List.of("&7Page " + safePage + " of " + totalPages), player));
        inventory.setItem(49, item(Material.BARRIER, "&cBack", List.of("&7Return to the Survival Menu."), player));
        if (safePage + 1 < totalPages) inventory.setItem(50, item(Material.ARROW, "&aNext Page", List.of("&7Page " + (safePage + 2) + " of " + totalPages), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public boolean purchaseEventShopReward(Player player, String rewardId) {
        EventShopReward reward = eventShopRewards().stream().filter(entry -> entry.id().equalsIgnoreCase(rewardId)).findFirst().orElse(null);
        if (reward == null) return false;
        if (isOneTimeEventShopReward(reward.id()) && hasPurchasedEventShopReward(player, reward.id())) {
            player.sendMessage(CC.color("&cYou can only buy the Dctr Space Helmet once."));
            return false;
        }
        if (eventTokenBalance(player) < reward.cost()) {
            player.sendMessage(CC.color("&cYou do not have enough Event Tokens."));
            return false;
        }

        List<ItemStack> delivered = createEventShopDelivery(reward);
        if (delivered.isEmpty() && reward.boosterId() == null) {
            player.sendMessage(CC.color("&cThat Event Shop reward is not currently registered."));
            return false;
        }
        ItemStack[] finalContents = cloneStorage(player.getInventory().getStorageContents());
        if (!removeCustomItems(finalContents, "event_token", reward.cost()) || !addAll(finalContents, delivered)) {
            player.sendMessage(CC.color("&cYou need more inventory space."));
            return false;
        }

        player.getInventory().setStorageContents(finalContents);
        if (plugin instanceof club.abelix.games.fakevival.Game game) {
            if (reward.boosterId() != null) {
                String path = "players." + player.getUniqueId() + ".store.booster-inventory." + reward.boosterId();
                game.getDataManager().getData().set(path, game.getDataManager().getData().getInt(path, 0) + 1);
            }
            if (isOneTimeEventShopReward(reward.id())) {
                game.getDataManager().getData().set(eventShopPurchasePath(player, reward.id()), true);
            }
            if (reward.boosterId() != null || isOneTimeEventShopReward(reward.id())) game.getDataManager().save();
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.25F);
        player.sendMessage(CC.color("&aYou bought &e" + reward.name() + " &afor &e" + reward.cost() + " Event Tokens&a."));
        if (reward.boosterId() != null) player.sendMessage(CC.color("&7Activate it from &e/boosters&7."));
        return true;
    }

    public int eventTokenBalance(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && "event_token".equalsIgnoreCase(customItemManager.getItemId(stack))) total += stack.getAmount();
        }
        return total;
    }

    private boolean hasPurchasedEventShopReward(Player player, String rewardId) {
        if (!(plugin instanceof club.abelix.games.fakevival.Game game)) return false;
        return game.getDataManager().getData().getBoolean(eventShopPurchasePath(player, rewardId), false);
    }

    private boolean isOneTimeEventShopReward(String rewardId) {
        return "dctr_space_helmet".equalsIgnoreCase(rewardId);
    }

    private String eventShopPurchasePath(Player player, String rewardId) {
        return "players." + player.getUniqueId() + ".event_shop_purchases." + rewardId.toLowerCase(java.util.Locale.ROOT);
    }

    private List<ItemStack> createEventShopDelivery(EventShopReward reward) {
        List<ItemStack> result = new ArrayList<>();
        if (reward.boosterId() != null) return result;
        if (reward.lootBoxId() != null && plugin instanceof club.abelix.games.fakevival.Game game) {
            result.add(game.getLootBoxManager().createBox(reward.lootBoxId(), 1));
            return result;
        }
        if (!reward.bundleIds().isEmpty()) {
            List<String> available = reward.bundleIds().stream().filter(customItemManager::itemExists).toList();
            for (int index = 0; index < reward.amount() && !available.isEmpty(); index++) {
                String id = available.get(ThreadLocalRandom.current().nextInt(available.size()));
                result.add(customItemManager.createItem(id, 1));
            }
            return result;
        }
        if (reward.customItemId() != null && customItemManager.itemExists(reward.customItemId())) {
            result.add(customItemManager.createItem(reward.customItemId(), reward.amount()));
        }
        return result;
    }

    private ItemStack[] cloneStorage(ItemStack[] contents) {
        ItemStack[] clone = new ItemStack[contents.length];
        for (int slot = 0; slot < contents.length; slot++) clone[slot] = contents[slot] == null ? null : contents[slot].clone();
        return clone;
    }

    private boolean removeCustomItems(ItemStack[] contents, String id, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || !id.equalsIgnoreCase(customItemManager.getItemId(stack))) continue;
            int taken = Math.min(remaining, stack.getAmount());
            remaining -= taken;
            if (taken == stack.getAmount()) contents[slot] = null;
            else stack.setAmount(stack.getAmount() - taken);
        }
        return remaining == 0;
    }

    private boolean addAll(ItemStack[] contents, List<ItemStack> rewards) {
        for (ItemStack reward : rewards) {
            int remaining = reward.getAmount();
            for (ItemStack stack : contents) {
                if (stack != null && stack.isSimilar(reward) && stack.getAmount() < stack.getMaxStackSize()) {
                    int added = Math.min(remaining, stack.getMaxStackSize() - stack.getAmount());
                    stack.setAmount(stack.getAmount() + added);
                    remaining -= added;
                    if (remaining == 0) break;
                }
            }
            for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
                if (contents[slot] != null && !contents[slot].getType().isAir()) continue;
                ItemStack placed = reward.clone();
                placed.setAmount(Math.min(remaining, placed.getMaxStackSize()));
                contents[slot] = placed;
                remaining -= placed.getAmount();
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private List<EventShopReward> eventShopRewards() {
        return List.of(
                reward("event_loot_box", "Event Loot Box", Material.ENDER_CHEST, 25, "A balanced event reward box.", null, 1, List.of(), "event", null),
                reward("recipe_bundle", "Recipe Fragment Bundle", Material.PAPER, 10, "3 random recipe fragments.", null, 3, List.of("recipe_fragment_alpha"), null, null),
                reward("monster_bundle", "Monster Material Bundle", Material.ROTTEN_FLESH, 12, "4 random monster materials.", null, 4, List.of("goblin_ear", "orc_tooth", "zombie_fang", "spider_silk_bundle", "venom_gland", "slime_core"), null, null),
                reward("fishing_bundle", "Fishing Material Bundle", Material.PRISMARINE_SHARD, 12, "4 random fishing materials.", null, 4, List.of("angler_scale", "deepwater_pearl", "river_fish_scale", "salmon_fin", "puffer_spike"), null, null),
                reward("mining_bundle", "Mining Material Bundle", Material.IRON_INGOT, 12, "4 random mining materials.", null, 4, List.of("rough_coal_chunk", "iron_nugget_cluster", "copper_wire_bundle", "gold_dust", "lapis_powder", "mithril_dust"), null, null),
                reward("farming_bundle", "Farming Material Bundle", Material.WHEAT, 12, "4 random farming materials.", null, 4, List.of("golden_wheat_bundle", "carrot_seed_core", "potato_sprout", "beetroot_extract", "melon_rind"), null, null),
                reward("foraging_bundle", "Foraging Material Bundle", Material.OAK_LOG, 12, "4 random foraging materials.", null, 4, List.of("oak_bark_strip", "birch_bark_strip", "spruce_pinecone", "jungle_vine_fiber", "living_root"), null, null),
                reward("treasure_bait_bundle", "Treasure Bait Bundle", Material.NAUTILUS_SHELL, 8, "4 Treasure Bait.", "treasure_bait", 4, List.of(), null, null),
                reward("monster_bait_bundle", "Monster Bait Bundle", Material.COD, 10, "4 bait for creature-focused fishing.", null, 4, List.of("chum_bait", "nightcrawler_bait", "storm_bait"), null, null),
                reward("skill_xp_booster", "Skill XP Booster", Material.EXPERIENCE_BOTTLE, 40, "1.5x Skill XP for 30 minutes; activate in /boosters.", null, 1, List.of(), null, "skill_15x_30m"),
                reward("event_cosmetic_token", "Event Cosmetic Token", Material.AMETHYST_SHARD, 30, "A future event cosmetic unlock token.", "event_cosmetic_token", 1, List.of(), null, null),
                reward("ancient_key_fragment", "Dungeon Key Fragment", Material.TRIPWIRE_HOOK, 15, "An Ancient Key Fragment.", "ancient_key_fragment", 1, List.of(), null, null),
                reward("boss_summon_token", "Boss Summon Token", Material.NETHER_STAR, 50, "A high-value boss summoning token.", "boss_summon_token", 1, List.of(), null, null),
                reward("mythic_crafting_core", "Mythic Crafting Core", Material.NETHER_STAR, 100, "A late-game crafting catalyst.", "mythic_crafting_core", 1, List.of(), null, null),
                reward("dctr_space_helmet", "&dDctr Space Helmet", Material.PLAYER_HEAD, 250000, "A rare event cosmetic helmet.", "dctr_space_helmet", 1, List.of(), null, null)
        );
    }

    private EventShopReward reward(String id, String name, Material icon, int cost, String lore, String customItemId,
                                   int amount, List<String> bundleIds, String lootBoxId, String boosterId) {
        return new EventShopReward(id, name, icon, cost, List.of("&7" + lore), customItemId, amount, bundleIds, lootBoxId, boosterId);
    }

    private record EventShopReward(String id, String name, Material icon, int cost, List<String> lore,
                                   String customItemId, int amount, List<String> bundleIds,
                                   String lootBoxId, String boosterId) {
    }

    private ItemStack eventCalendarItem(Player player, EventManager.CalendarEntry entry) {
        List<String> lore = new ArrayList<>();
        String name = "&a" + entry.name() + (entry.active() ? " &7(&eActive&7)" : "");
        if (entry.active()) {
            lore.add("&7Status: &aActive");
            lore.add("&7Ends In: &e" + entry.realTimeUntil());
            lore.add("&7Minecraft Days Left: &a" + minecraftDayText(entry.minecraftDays()));
        } else {
            lore.add("&7Status: &e" + entry.status());
            lore.add("&7Starts In: &a" + minecraftDayText(entry.minecraftDays()));
            lore.add("&7Real Time: &e" + entry.realTimeUntil());
            lore.add("&7Duration: &b" + entry.duration());
            lore.add("");
            lore.add("&7Bonuses:");
            lore.addAll(entry.bonuses());
            lore.add("");
            lore.add("&7Rewards:");
            lore.addAll(entry.rewards());
        }
        lore.add("");
        if (!eventManager.canProgressEvent(player, entry.id())) {
            lore.add("&7Progress: &cUnavailable on this server.");
            lore.add("");
        }
        lore.add("&eClick to view details!");
        lore.add("&8Calendar Start: " + entry.startAt());
        return item(entry.icon(), name, lore, player, entry.active());
    }

    private String minecraftDayText(String days) {
        return days.equalsIgnoreCase("Today") ? "Today" : days + " Minecraft Days";
    }



    public void openShop(Player player, String shopId) {
        openShop(player, shopId, 0);
    }

    public boolean shopExists(String shopId) {
        if (shopId == null || shopId.isBlank()) return false;
        if (club.abelix.games.fakevival.config.defaults.NpcShopDefaults.isSpecialMenu(shopId)) return true;
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getConfigurationSection("shops." + shopId.trim().toLowerCase(Locale.ROOT) + ".items");
        return section != null && !section.getKeys(false).isEmpty();
    }

    public int shopEntryCount(String shopId) {
        if (shopId == null || shopId.isBlank()) return 0;
        ConfigurationSection entries = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getConfigurationSection("shops." + shopId.trim().toLowerCase(Locale.ROOT) + ".items");
        return entries == null ? 0 : entries.getKeys(false).size();
    }

    public String shopSource(String shopId) {
        if (shopId == null || shopId.isBlank() || !shopExists(shopId)) return "MISSING";
        // This project has no DB-backed shop row loader; live shop sections originate from Java static defaults.
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(
                "shops." + shopId.trim().toLowerCase(Locale.ROOT) + ".source", "JAVA_DEFAULTS");
    }

    public ShopDebug debugShop(String shopId) {
        String normalized = shopId == null ? "" : shopId.trim().toLowerCase(Locale.ROOT);
        ConfigurationSection root = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getConfigurationSection("shops." + normalized);
        ConfigurationSection entries = root == null ? null : root.getConfigurationSection("items");
        List<String> firstEntries = new ArrayList<>();
        if (entries != null) {
            for (String entryId : entries.getKeys(false)) {
                String customId = entries.getString(entryId + ".custom-item", "");
                String item = customId.isBlank() ? entries.getString(entryId + ".material", "UNKNOWN") : customId;
                String buy = entries.getBoolean(entryId + ".allow-buy", true)
                        ? String.valueOf(entries.getDouble(entryId + ".price", 0D)) : "disabled";
                String sell = entries.getBoolean(entryId + ".allow-sell", true)
                        ? String.valueOf(entries.getDouble(entryId + ".sell-price", 0D)) : "disabled";
                firstEntries.add(entryId + " " + item + " buy=" + buy + " sell=" + sell);
                if (firstEntries.size() >= 10) break;
            }
        }
        String name = root == null ? "" : root.getString("name", pretty(normalized));
        int version = root == null ? 0 : root.getInt("default-version", 0);
        return new ShopDebug(normalized, name, shopSource(normalized), entries == null ? 0 : entries.getKeys(false).size(), version,
                List.copyOf(firstEntries));
    }

    /** Clears diagnostic state and replaces already-open village shop previews with current live entries. */
    public int refreshOpenVillageShopMenus() {
        warnedMissingShopItems.clear();
        Set<String> refreshable = new HashSet<>(club.abelix.games.fakevival.config.defaults.NpcShopDefaults.villageIds());
        refreshable.addAll(club.abelix.games.fakevival.config.defaults.ShopFoodItemDefaults.shopIds());
        refreshable.add("adventure_guild");
        int refreshed = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder(String type))
                    || !type.startsWith("shop:")) continue;
            String[] parts = type.split(":", 3);
            if (parts.length < 2 || !refreshable.contains(parts[1])) continue;
            int page = 0;
            if (parts.length == 3) try { page = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
            openShop(player, parts[1], page);
            refreshed++;
        }
        return refreshed;
    }

    public record ShopDebug(String id, String displayName, String source, int entries, int version, List<String> firstEntries) {}

    public ShopValidation validateShops(Collection<String> shopIds) {
        int shops = 0, items = 0, warnings = 0, customFish = 0, internalDisplayNames = 0, duplicateInventories = 0,
                serviceMenus = 0, skippedEntries = 0, blockedVendorLoreItems = 0;
        boolean expectsAdventureGuild = shopIds.stream().anyMatch("adventure_guild"::equalsIgnoreCase);
        boolean adventureGuildRoyalScrapLoaded = false;
        Set<String> missingCustomItems = new HashSet<>();
        java.util.Map<String,String> inventoryOwners = new java.util.HashMap<>();
        Set<String> oldOrGenericShops = new HashSet<>();
        for (String shopId : shopIds) {
            if (club.abelix.games.fakevival.config.defaults.NpcShopDefaults.isSpecialMenu(shopId)) { shops++; serviceMenus++; continue; }
            ConfigurationSection shopRoot = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                    .getConfigurationSection("shops." + shopId);
            ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                    .getConfigurationSection("shops." + shopId + ".items");
            if (section == null || section.getKeys(false).isEmpty()) { warnings++; continue; }
            shops++;
            if (club.abelix.games.fakevival.config.defaults.NpcShopDefaults.normalVillageShopIds().contains(shopId)
                    && (shopRoot == null || shopRoot.getInt("default-version", 0)
                    != club.abelix.games.fakevival.config.defaults.NpcShopDefaults.VILLAGE_SHOP_DEFAULT_VERSION)) {
                oldOrGenericShops.add(shopId);
                warnings++;
            }
            if (section.getKeys(false).size() < 5) warnings++;
            List<String> signatureParts = new ArrayList<>();
            for (String entry : section.getKeys(false)) signatureParts.add(section.getString(entry + ".custom-item",
                    "m:" + section.getString(entry + ".material", "")));
            signatureParts.sort(String::compareTo);
            String previousOwner = inventoryOwners.putIfAbsent(String.join("|", signatureParts), shopId);
            if (previousOwner != null) {
                duplicateInventories++; warnings++;
                oldOrGenericShops.add(previousOwner); oldOrGenericShops.add(shopId);
            }
            boolean fishShop = section.getKeys(false).stream()
                    .anyMatch(entry -> section.getBoolean(entry + ".accept-custom-fish", false));
            boolean hasCustomFish = false;
            for (String entry : section.getKeys(false)) {
                items++;
                double buy = section.getDouble(entry + ".price", 0D);
                if (!Double.isFinite(buy) || buy <= 0D) warnings++;
                if (section.getBoolean(entry + ".allow-sell", false)) {
                    double sell = section.getDouble(entry + ".sell-price", 0D);
                    if (!Double.isFinite(sell) || sell <= 0D) warnings++;
                }
                String customId = section.getString(entry + ".custom-item", "").toLowerCase(Locale.ROOT);
                if (!customId.isBlank()) {
                    if (!customItemManager.itemExists(customId)) {
                        missingCustomItems.add(customId); skippedEntries++; warnings++; continue;
                    }
                    if (forbiddenShopItem(customId)) { skippedEntries++; warnings++; continue; }
                    try {
                        boolean blockedLore = VendorLoreSanitizer.containsBlockedLore(
                                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                                        .getStringList("custom-items.items." + customId + ".lore"));
                        ItemStack sample = customItemManager.createItem(customId, 1);
                        if (sample == null || sample.getType().isAir()) { skippedEntries++; warnings++; continue; }
                        blockedLore |= sample.hasItemMeta()
                                && VendorLoreSanitizer.containsBlockedLore(sample.getItemMeta().getLore());
                        if (blockedLore) {
                            blockedVendorLoreItems++; warnings++;
                        }
                        String visible = sample.hasItemMeta() && sample.getItemMeta().hasDisplayName()
                                ? ChatColor.stripColor(sample.getItemMeta().getDisplayName()) : pretty(customId);
                        if (isInternalShopName(visible)) { internalDisplayNames++; warnings++; }
                        if ("CUSTOM_FISH".equals(customItemManager.getCustomItemType(sample))) { customFish++; hasCustomFish = true; }
                        if (shopId.equalsIgnoreCase("adventure_guild") && customId.equals("royal_scrap")) {
                            adventureGuildRoyalScrapLoaded = true;
                        }
                    } catch (RuntimeException ex) {
                        skippedEntries++; warnings++;
                    }
                } else {
                    try {
                        Material material = Material.valueOf(section.getString(entry + ".material", "").toUpperCase(Locale.ROOT));
                        if (!canCreateItemStack(material)) { skippedEntries++; warnings++; continue; }
                        if (isInternalShopName(prettyMaterialName(material))) { internalDisplayNames++; warnings++; }
                    }
                    catch (Exception ex) { skippedEntries++; warnings++; }
                }
            }
            if (fishShop && !hasCustomFish) warnings++;
        }
        boolean castleServicesUnchanged = java.util.Set.of("castle_banker", "castle_guild_clerk", "castle_auction_master").stream()
                .allMatch(id -> club.abelix.games.fakevival.config.defaults.NpcShopDefaults.isSpecialMenu(id)
                        && club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("shops." + id + ".items") == null);
        if (!castleServicesUnchanged) warnings++;
        if (expectsAdventureGuild && !adventureGuildRoyalScrapLoaded) warnings++;
        return new ShopValidation(shops, items, customFish, warnings, internalDisplayNames, duplicateInventories,
                serviceMenus, missingCustomItems.size(), skippedEntries, blockedVendorLoreItems,
                oldOrGenericShops.size(), castleServicesUnchanged, true, adventureGuildRoyalScrapLoaded);
    }

    private boolean forbiddenShopItem(String customId) {
        if (customId.equals("developer_notebook")) return true;
        String base = "custom-items.items." + customId + ".";
        String type = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + "type", "").toUpperCase(Locale.ROOT);
        String rarity = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + "rarity", "").toUpperCase(Locale.ROOT);
        return type.contains("SPECIAL") || type.contains("ADMIN") || type.contains("TEST") || type.contains("PREVIEW")
                || rarity.equals("SPECIAL")
                || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + "special-item", false)
                || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + "developer-notebook", false)
                || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + "preview-item", false)
                || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + "admin-item", false);
    }

    public record ShopValidation(int shops, int items, int customFishItems, int warnings,
                                 int internalDisplayNames, int duplicateInventories, int serviceMenus,
                                 int missingItems, int skippedEntries, int blockedVendorLoreItems,
                                 int oldOrGenericShops, boolean castleServicesUnchanged,
                                 boolean pdcEntryRouting, boolean adventureGuildRoyalScrapLoaded) {}

    public void openShop(Player player, String shopId, int page) {
        if (shopId.equalsIgnoreCase("bazaar")) {
            openBazaar(player);
            return;
        }
        String shopKey = shopId.toLowerCase();
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("shops." + shopKey + ".items");
        List<String> itemIds = section == null ? List.of() : new ArrayList<>(section.getKeys(false));
        if (section != null) itemIds = itemIds.stream().filter(id -> isShopItemDisplayable(section, id)).toList();
        int[] slots = shopSlots();
        int totalPages = Math.max(1, (int) Math.ceil(itemIds.size() / (double) slots.length));
        page = Math.clamp(page, 0, totalPages - 1);

        String shopName = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("shops." + shopKey + ".name", "&a" + pretty(shopKey) + " Shop");
        String title = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("menus.shop.title", "%shop%")
                .replace("%shop%", ChatColor.stripColor(CC.color(shopName)));
        Inventory inventory = Bukkit.createInventory(new MenuHolder("shop:" + shopKey + ":" + page), 54, CC.color(title));
        inventory.setItem(4, item(shopIcon(shopKey), shopName, shopOverviewLore(player, shopKey, itemIds.size(), page, totalPages), player));

        if (section != null) {
            int start = page * slots.length;
            for (int index = 0; index < slots.length && start + index < itemIds.size(); index++) {
                String id = itemIds.get(start + index);
                Material material;
                String customItemId = section.getString(id + ".custom-item", "");
                String customEnchantId = section.getString(id + ".custom-enchant", "");
                try {
                    material = !customEnchantId.isBlank()
                            ? customEnchantManager.displayMaterial(customEnchantId)
                            : customItemId.isBlank() ? Material.valueOf(section.getString(id + ".material", "EMERALD").toUpperCase()) : customItemManager.getMaterial(customItemId);
                } catch (Exception e) { material = Material.EMERALD; }
                if (customItemId.isBlank() && !canCreateItemStack(material)) continue;
                double price = section.getDouble(id + ".price", 0.0D);
                if (plugin instanceof club.abelix.games.fakevival.Game game)
                    price = game.getMayorManager().applyMayorModifier(player,
                            club.abelix.games.fakevival.mayor.MayorModifierType.SHOP_PRICE, price);
                double sellPrice = section.getDouble(id + ".sell-price", Math.max(0.0D, price * 0.5D));
                if (shopKey.equals("adventure_guild") && eventManager != null) sellPrice *= eventManager.guildSellMultiplier();
                int amount = Math.max(1, section.getInt(id + ".amount", 1));
                int enchantLevel = Math.max(1, section.getInt(id + ".level", 1));
                String defaultName = !customEnchantId.isBlank()
                        ? customEnchantManager.displayName(customEnchantId, enchantLevel)
                        : customItemId.isBlank() ? "&a" + prettyMaterialName(material) : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("custom-items.items." + customItemId.toLowerCase() + ".name", "&a" + pretty(customItemId));
                String name = section.getString(id + ".name", defaultName);
                ItemStack stack;
                if (!customItemId.isBlank()) {
                    if (!customItemManager.itemExists(customItemId)) {
                        if (warnedMissingShopItems.add(shopKey + ":" + customItemId))
                            plugin.getLogger().warning("Skipped missing custom shop item " + customItemId + " in " + shopKey + ".");
                        stack = item(Material.BARRIER, "&cInvalid custom item: " + customItemId, List.of("&7This vendor entry cannot be purchased."), player);
                    } else {
                        stack = customItemManager.createItem(customItemId, 1).clone();
                        boolean sanitizedPreview = VendorLoreSanitizer.sanitizeItem(
                                stack, VendorLoreSanitizer.Context.SHOP_PREVIEW);
                        customItemManager.rebuildLoreFor(player, stack);
                        VendorLoreSanitizer.sanitizeItem(stack, sanitizedPreview
                                ? VendorLoreSanitizer.Context.GENERAL : VendorLoreSanitizer.Context.SHOP_PREVIEW);
                    }
                } else {
                    List<String> baseLore = new ArrayList<>();
                    if (!customEnchantId.isBlank()) baseLore.add("&dCustom Enchant Book");
                    stack = item(material, name, baseLore, player);
                }
                ItemMeta meta = stack.getItemMeta();
                if (meta == null) continue;
                List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.addAll(shopRequirementLore(player, section.getConfigurationSection(id + ".requires")));
                lore.add("");
                boolean buyEnabled = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("shops." + shopKey + ".buy-enabled", true)
                        && section.getBoolean(id + ".allow-buy", true);
                boolean sellEnabled = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("shops." + shopKey + ".sell-enabled", true)
                        && section.getBoolean(id + ".allow-sell", true);
                if (buyEnabled) lore.add(CC.color("&7Buy Price: &6" + economyManager.format(price) + " Coins"));
                if (shopKey.startsWith("food_vendor_")) {
                    lore.add(CC.color("&7Vendor: " + shopName));
                    lore.add(CC.color("&7Stock: &aUnlimited"));
                }
                if (sellEnabled) lore.add(CC.color("&7Sell Value: &6" + economyManager.format(sellPrice) + " Coins"));
                if (amount > 1) lore.add(CC.color("&7Amount: &f" + amount));
                lore.add("");
                if (buyEnabled) {
                    lore.add(CC.color("&eLeft Click to buy 1!"));
                    lore.add(CC.color("&eRight Click to buy 8!"));
                    if (section.getBoolean(id + ".bulk-buy-enabled", true)) lore.add(CC.color("&bShift-left-click to buy 64!"));
                }
                if (sellEnabled) lore.add(CC.color("&6Shift-right-click to sell matching inventory!"));
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(shopItemKey, PersistentDataType.STRING, id);
                meta.getPersistentDataContainer().set(shopIdKey, PersistentDataType.STRING, shopKey);
                meta.getPersistentDataContainer().set(shopEntryIdKey, PersistentDataType.STRING, id);
                meta.getPersistentDataContainer().set(shopPreviewKey, PersistentDataType.BYTE, (byte) 1);
                stack.setItemMeta(meta);
                stack.setAmount(Math.min(64, amount));
                inventory.setItem(slots[index], stack);
            }
        }
        if (itemIds.isEmpty()) {
            inventory.setItem(22, item(Material.CHEST, "&eNo Stock Available", List.of("&7This shop has no configured items."), player));
        }
        if (page > 0) inventory.setItem(45, item(Material.ARROW, "&ePrevious Page", List.of("&7Page " + page + " of " + totalPages), player));
        inventory.setItem(48, item(Material.COMPASS, "&aRefresh", List.of("&7Reload this shop.", "", "&eClick to refresh."), player));
        inventory.setItem(49, item(Material.GOLD_INGOT, "&eYour Purse", List.of("&f" + economyManager.format(economyManager.getCoins(player)) + " coins"), player));
        if (page + 1 < totalPages) inventory.setItem(50, item(Material.ARROW, "&eNext Page", List.of("&7Page " + (page + 2) + " of " + totalPages), player));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close this shop."), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openBazaar(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("bazaar"), 54, CC.color("&8Bazaar"));
        for (String category : bazaarManager.getCategories()) {
            int itemCount = bazaarManager.getItemIds(category).size();
            inventory.setItem(bazaarCategorySlot(category), bazaarButton(bazaarManager.getCategoryIcon(category),
                    bazaarManager.getCategoryDisplayName(category), List.of(
                    "&7View materials in this category.",
                    "&7Items: &e" + itemCount,
                    "",
                    "&eClick to browse!"
            ), "category:" + category, player));
        }
        inventory.setItem(49, bazaarButton(Material.BARRIER, "&cClose", List.of("&7Close the Bazaar."), "close", player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openBazaarCategory(Player player, String category) {
        openBazaarCategory(player, category, 0);
    }

    public void openBazaarCategory(Player player, String category, int page) {
        if (category.equalsIgnoreCase("vanilla")) {
            openVanillaBazaar(player);
            return;
        }
        boolean vanillaSubcategory = category.toLowerCase(Locale.ROOT).startsWith("vanilla_");
        String vanillaSubcategoryId = vanillaSubcategory ? category.substring("vanilla_".length()) : "";
        List<String> itemIds = vanillaSubcategory ? bazaarManager.getVanillaItemIds(vanillaSubcategoryId) : bazaarManager.getItemIds(category);
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int totalPages = Math.max(1, (int) Math.ceil(itemIds.size() / (double) slots.length));
        page = Math.clamp(page, 0, totalPages - 1);

        String cleanCategory = vanillaSubcategory ? bazaarManager.getVanillaSubcategories().stream().filter(sub -> sub.id().equals(vanillaSubcategoryId)).map(club.abelix.games.fakevival.registries.VanillaBazaarRegistry.Subcategory::displayName).findFirst().orElse("Vanilla") : Objects.requireNonNullElse(ChatColor.stripColor(CC.color(bazaarManager.getCategoryDisplayName(category))), "Misc");
        Inventory inventory = Bukkit.createInventory(new MenuHolder("bazaar-category:" + category.toLowerCase() + ":" + page), 54,
                CC.color("&8Bazaar - " + cleanCategory));

        if (bazaarManager.isMarketCacheLoading()) {
            inventory.setItem(22, bazaarButton(Material.CLOCK, "&eLoading Bazaar...", List.of("&7Please wait a moment."), "loading", player));
        } else {
            int start = page * slots.length;
            for (int i = 0; i < slots.length && start + i < itemIds.size(); i++) {
                String itemId = itemIds.get(start + i);
                inventory.setItem(slots[i], bazaarPreview(player, itemId, true));
            }
        }

        if (page > 0) inventory.setItem(45, bazaarButton(Material.ARROW, "&ePrevious Page", List.of("&7Page " + page + " of " + totalPages), "previous", player));
        inventory.setItem(49, bazaarButton(Material.ARROW, "&eBack", List.of("&7Return to Bazaar categories."), "back", player));
        if (page + 1 < totalPages) inventory.setItem(53, bazaarButton(Material.ARROW, "&eNext Page", List.of("&7Page " + (page + 2) + " of " + totalPages), "next", player));
        fill(inventory);
        player.openInventory(inventory);
    }

    private void openVanillaBazaar(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("bazaar-category:vanilla:0"), 54, CC.color("&8Bazaar - Vanilla"));
        int[] slots = {10,11,12,13,14,15,16,20,21,22,23,24};
        List<club.abelix.games.fakevival.registries.VanillaBazaarRegistry.Subcategory> categories = bazaarManager.getVanillaSubcategories();
        for (int i=0;i<slots.length && i<categories.size();i++) {
            var sub=categories.get(i);
            inventory.setItem(slots[i], bazaarButton(sub.icon(), "&e"+sub.displayName(), List.of("&7Safe survival-obtainable vanilla items.", "&7Items: &e"+bazaarManager.getVanillaItemIds(sub.id()).size(), "", "&eClick to browse!"), "subcategory:"+sub.id(), player));
        }
        inventory.setItem(49,bazaarButton(Material.ARROW,"&eBack",List.of("&7Return to Bazaar categories."),"back",player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openAdventureGuild(Player player) {
        AdventureGuildManager guild=plugin instanceof club.abelix.games.fakevival.Game game?game.getAdventureGuildManager():null;
        if(guild!=null)guild.load(player);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("adventure-guild"), 54, CC.color("&8Adventurer's Guild"));
        if(guild!=null&&!guild.profile(player).registered()){
            inventory.setItem(22,item(Material.EMERALD,"&aRegister as Adventurer",List.of("&7Join the Adventurer's Guild.","&7Starting Rank: &eE","","&eClick to register!"),player));
            inventory.setItem(53,item(Material.BARRIER,"&cClose",List.of("&7Close the guild menu."),player));fill(inventory);player.openInventory(inventory);return;
        }
        List<String> dailyQuests = questManager.getAdventureQuestIds("daily");
        List<String> weeklyQuests = questManager.getAdventureQuestIds("weekly");
        inventory.setItem(4, item(Material.DIAMOND_SWORD, "&5Adventure Guild", List.of(
                "&7Complete contracts by turning in",
                "&7monster materials from survival.",
                "",
                "&7Daily: " + adventureSummary(player, "daily", dailyQuests),
                "&7Weekly: " + adventureSummary(player, "weekly", weeklyQuests),
                "&7Purse: &e" + economyManager.format(economyManager.getCoins(player)) + " coins"
        ), player, hasReadyAdventureQuest(player, dailyQuests, "daily") || hasReadyAdventureQuest(player, weeklyQuests, "weekly")));
        if(guild!=null){var profile=guild.profile(player);inventory.setItem(13,item(Material.NAME_TAG,"&eYour Rank",List.of("&7Current Rank: "+profile.rank().color+profile.rank(),"&7Active Quest Limit: &a"+guild.activeLimit(player),"&7Reward Multiplier: &6"+profile.rank().multiplier+"x","&7Completed Guild Quests: &e"+profile.quests()),player));var next=profile.rank().next();inventory.setItem(31,item(Material.NETHER_STAR,"&6Rank Up",next==null?List.of("&cYou reached the highest rank."):java.util.stream.Stream.concat(java.util.stream.Stream.of("&7Next Rank: "+next.color+next,""),guild.requirements(player,next).stream()).toList(),player));}
        inventory.setItem(10, item(Material.CLOCK, "&aDaily Contracts", List.of(
                "&7Short contracts that reset daily.",
                "&7Progress is checked from your inventory.",
                "",
                "&7Contracts: &f" + dailyQuests.size(),
                "&7Status: " + adventureSummary(player, "daily", dailyQuests),
                "",
                "&eClick to view daily contracts."
        ), player, hasReadyAdventureQuest(player, dailyQuests, "daily")));
        inventory.setItem(16, item(Material.ROTTEN_FLESH, "&6Monster Material Shop", List.of(
                "&7Buy and sell common monster drops.",
                "&7Useful for stocking contract items.",
                "",
                "&eClick to open",
                "&8Adventure Shop"
        ), player));
        inventory.setItem(28, item(Material.NETHER_STAR, "&6Weekly Contracts", List.of(
                "&7Larger objectives with better payouts.",
                "&7Weekly progress uses the current week.",
                "",
                "&7Contracts: &f" + weeklyQuests.size(),
                "&7Status: " + adventureSummary(player, "weekly", weeklyQuests),
                "",
                "&eClick to view weekly contracts."
        ), player, hasReadyAdventureQuest(player, weeklyQuests, "weekly")));

        inventory.setItem(49, item(Material.GOLD_INGOT, "&eYour Purse", List.of("&f" + economyManager.format(economyManager.getCoins(player)) + " coins"), player));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close the guild menu."), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openAdventureGuildQuests(Player player, String period, int page) {
        AdventureGuildManager guild=plugin instanceof club.abelix.games.fakevival.Game game?game.getAdventureGuildManager():null;
        String normalized = period == null ? "daily" : period.toLowerCase();
        if (!normalized.equals("weekly")) normalized = "daily";

        List<String> quests = questManager.getAdventureQuestIds(normalized);
        int[] questSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };
        int totalPages = Math.max(1, (int) Math.ceil(quests.size() / (double) questSlots.length));
        int safePage = Math.clamp(page, 0, totalPages - 1);
        String title = normalized.equals("weekly") ? "&6Weekly Contracts" : "&aDaily Contracts";
        Inventory inventory = Bukkit.createInventory(new MenuHolder("adventure-guild:" + normalized + ":" + safePage), 54, CC.color(title));

        inventory.setItem(4, item(normalized.equals("weekly") ? Material.NETHER_STAR : Material.CLOCK, title, List.of(
                normalized.equals("weekly") ? "&7Large weekly contracts with better payouts." : "&7Short daily contracts for steady rewards.",
                "&7Turn in the required item from your inventory.",
                "",
                "&7Adventure Rank: "+(guild==null?"&e&lE":guild.profile(player).rank().color+guild.profile(player).rank()),
                "&7Contracts: &f" + quests.size(),
                "&7Page: &f" + (safePage + 1) + "&7/&f" + totalPages,
                "&7Status: " + adventureSummary(player, normalized, quests)
        ), player, hasReadyAdventureQuest(player, quests, normalized)));

        int start = safePage * questSlots.length;
        for (int i = 0; i < questSlots.length && start + i < quests.size(); i++) {
            inventory.setItem(questSlots[i], adventureQuestItem(player, normalized, quests.get(start + i)));
        }

        inventory.setItem(45, item(Material.ARROW, "&cBack", List.of("&7Return to the Adventure Guild."), player));
        if (safePage > 0) inventory.setItem(48, item(Material.ARROW, "&ePrevious Page", List.of("&7Page " + safePage + " of " + totalPages), player));
        inventory.setItem(49, item(Material.GOLD_INGOT, "&eYour Purse", List.of("&f" + economyManager.format(economyManager.getCoins(player)) + " coins"), player));
        if (safePage + 1 < totalPages) inventory.setItem(50, item(Material.ARROW, "&eNext Page", List.of("&7Page " + (safePage + 2) + " of " + totalPages), player));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close the guild menu."), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openBazaarItem(Player player, String itemId) {
        openBazaarItem(player, itemId, bazaarManager.getCategory(itemId), 0);
    }

    public void openBazaarItem(Player player, String itemId, String category, int page) {
        if (bazaarManager.itemExists(itemId)) {
            openBazaar(player);
            return;
        }
        String itemName = Objects.requireNonNullElse(ChatColor.stripColor(CC.color(bazaarManager.getDisplayName(itemId))), itemId);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("bazaar-item:" + itemId.toLowerCase().replace(':', '~') + ":" + category + ":" + page), 54,
                CC.color("&8Bazaar - " + itemName));
        int inventoryAmount = bazaarManager.countInventory(player, itemId);
        inventory.setItem(13, bazaarPreview(player, itemId, false));
        inventory.setItem(20, bazaarBuyButton(player, itemId, 1));
        inventory.setItem(21, bazaarBuyButton(player, itemId, 8));
        inventory.setItem(22, bazaarBuyButton(player, itemId, 64));
        inventory.setItem(23, bazaarSellButton(player, itemId, 1, inventoryAmount));
        inventory.setItem(24, bazaarSellButton(player, itemId, 8, inventoryAmount));
        inventory.setItem(25, bazaarSellButton(player, itemId, 64, inventoryAmount));
        inventory.setItem(29, bazaarButton(Material.HOPPER, "&6Sell Inventory", List.of(
                "&7Amount: &e" + inventoryAmount,
                "&7Value: &6" + economyManager.format(bazaarManager.getInstantSellValue(itemId, inventoryAmount)) + " Coins",
                "", "&eClick to sell!"), "sell_all", player));
        inventory.setItem(30, disabledBazaarButton("&7Create Buy Order"));
        inventory.setItem(32, disabledBazaarButton("&7Create Sell Order"));
        inventory.setItem(33, disabledBazaarButton("&7View Orders"));
        inventory.setItem(49, bazaarButton(Material.ARROW, "&eBack", List.of("&7Return to this Bazaar category."), "back", player));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openAuctionHouse(Player player) {
        openAuctionHouse(player, 0, false);
    }

    public void openAuctionHouse(Player player, int page, boolean ownListings) {
        openAuctionHouse(player, page, ownListings, AuctionHouseManager.CATEGORY_ALL);
    }

    public void openAuctionHouse(Player player, int page, boolean ownListings, String category) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("auctionhouse.enabled", true)) {
            player.sendMessage(CC.color("&cThe Auction House is currently disabled."));
            return;
        }
        String normalizedCategory = auctionHouseManager.normalizeCategory(category);
        List<String> listings = ownListings
                ? auctionHouseManager.getListings(player.getUniqueId(), normalizedCategory)
                : auctionHouseManager.getListings(normalizedCategory);
        int[] browseSlots = auctionListingSlots();
        int totalPages = Math.max(1, (int) Math.ceil(listings.size() / (double) browseSlots.length));
        page = Math.clamp(page, 0, totalPages - 1);

        String mode = ownListings ? "mine" : "all";
        Inventory inventory = Bukkit.createInventory(new MenuHolder("auctionhouse:" + page + ":" + mode + ":" + normalizedCategory), 54, CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("menus.auctionhouse.title", "&6Auction House")));
        inventory.setItem(4, item(Material.GOLD_BLOCK, "&6Auction House", List.of(
                "&7A survival marketplace for player trades.",
                "&7Browse listings, buy instantly,",
                "&7or manage items you posted.",
                "",
                "&7View: " + (ownListings ? "&aYour Auctions" : "&eAll Auctions"),
                "&7Category: &f" + pretty(normalizedCategory),
                "&7Page: &f" + (page + 1) + "&7/&f" + totalPages,
                "&7Your Listings: &f" + auctionHouseManager.getListings(player.getUniqueId()).size() + "&7/&f" + formatListingLimit(auctionHouseManager.getMaxListings(player))
        ), player));

        int start = page * browseSlots.length;
        for (int index = 0; index < browseSlots.length && start + index < listings.size(); index++) {
            String id = listings.get(start + index);
            ItemStack listing = auctionHouseManager.getItem(id);
            if (listing == null) continue;
            ItemStack display = listing.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.getLore())) : new ArrayList<>();
                boolean owned = player.getUniqueId().equals(auctionHouseManager.getOwner(id));
                lore.add("");
                lore.add(CC.color("&7Listing ID: &f" + id));
                lore.add(CC.color("&7Price: &e" + economyManager.format(auctionHouseManager.getPrice(id)) + " coins"));
                double suggested = auctionHouseManager.getSuggestedPrice(listing);
                if (suggested > 0.0D) {
                    lore.add(CC.color("&7Estimated Value: &6" + economyManager.format(suggested) + " coins"));
                    if (auctionHouseManager.hasRandomizedValueData(listing)) lore.add(CC.color("&7This item has randomized stats."));
                }
                lore.add(CC.color("&7Seller: " + auctionHouseManager.getSellerDisplay(id)));
                lore.add(CC.color("&7Listed: &f" + formatAge(auctionHouseManager.getCreatedAt(id))));
                lore.add(CC.color("&7Category: &f" + pretty(auctionHouseManager.getCategory(id))));
                lore.add("");
                lore.add(CC.color(owned ? "&eClick to reclaim this item." : "&aClick to buy now."));
                lore.add(CC.color(owned ? "&7Reclaiming removes it from the market." : "&7Coins are paid directly to the seller."));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(browseSlots[index], display);
        }

        if (listings.isEmpty()) {
            inventory.setItem(22, item(Material.CHEST, ownListings ? "&eNo Active Auctions" : "&eNo Auctions Listed", List.of(
                    ownListings ? "&7You do not have anything listed." : "&7The market is empty right now.",
                    "",
                    "&eHold an item and use &f/ah sell <price>&e."
            ), player));
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        double suggested = auctionHouseManager.getSuggestedPrice(hand);
        List<String> sellLore = new ArrayList<>(List.of(
                "&7List the item in your main hand",
                "&7for other players to buy.",
                "&7Your Listings: &f" + auctionHouseManager.getListings(player.getUniqueId()).size() + "&7/&f" + formatListingLimit(auctionHouseManager.getMaxListings(player))
        ));
        if (suggested > 0.0D) sellLore.add("&7Held Suggested Price: &6" + economyManager.format(suggested) + " coins");
        sellLore.add("");
        sellLore.add("&eUse &f/ah sell <price>");
        inventory.setItem(45, item(Material.EMERALD, "&aSell Held Item", sellLore, player));
        inventory.setItem(46, item(ownListings ? Material.CHEST : Material.ENDER_CHEST, ownListings ? "&eBrowse All Auctions" : "&bYour Auctions", List.of(
                ownListings ? "&7Return to the public market." : "&7View and reclaim your listings.",
                "&7Keeps category: &f" + pretty(normalizedCategory),
                "",
                "&eClick to switch view."
        ), player));
        if (page > 0) inventory.setItem(48, item(Material.ARROW, "&ePrevious Page", List.of("&7Page " + page + " of " + totalPages), player));
        inventory.setItem(49, item(Material.GOLD_INGOT, "&eYour Purse", List.of(
                "&f" + economyManager.format(economyManager.getCoins(player)) + " coins",
                "",
                "&7Listings: &f" + listings.size()
        ), player));
        if (page + 1 < totalPages) inventory.setItem(50, item(Material.ARROW, "&eNext Page", List.of("&7Page " + (page + 2) + " of " + totalPages), player));
        inventory.setItem(52, item(Material.COMPASS, "&aRefresh", List.of("&7Reload current listings.", "", "&eClick to refresh."), player));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close the Auction House."), player));
        setAuctionCategories(inventory, player, normalizedCategory);
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openBattlePass(Player player) {
        openBattlePass(player, 0);
    }

    public void openBattlePass(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("battlepass:" + page), 54, CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("menus.battlepass.title", "&dBattlePass")));
        int playerLevel = battlePassManager.getLevel(player);
        List<Integer> levels = battlePassManager.rewardLevels();
        int perPage = Math.max(1, battlePassManager.getConfig().getInt("menu.levels-per-page", 9));
        int totalPages = Math.max(1, battlePassManager.getTotalPages());
        page = Math.clamp(page, 0, totalPages - 1);

        inventory.setItem(4, item(Material.BOOK, "&6BattlePass &7(" + (page + 1) + "/" + totalPages + ")", List.of(
                "&7Level: &a" + playerLevel,
                "&7XP: &e" + battlePassManager.getXp(player),
                "&7Premium: " + (battlePassManager.hasPremium(player) ? "&aUnlocked" : "&cLocked"),
                "&7Page: &f" + (page + 1) + "&7/&f" + totalPages,
                "",
                "&fTop row: &6Premium Pass",
                "&fMiddle row: &aUnlocked &7/ &cLocked",
                "&fBottom row: &aFree Pass"
        ), player));

        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < levels.size(); i++) {
            int level = levels.get(start + i);
            int premiumSlot = 9 + i;
            int freeSlot = 27 + i;
            boolean freeUnlocked = battlePassManager.isFreeUnlocked(player, level);
            boolean premiumUnlocked = battlePassManager.isPremiumUnlocked(player, level);
            String display = battlePassManager.getConfig().getString("rewards." + level + ".display", "&aLevel " + level + " Reward");

            inventory.setItem(premiumSlot, item(battlePassIcon(premiumUnlocked), "&6Premium &7- &fLevel " + level,
                    battlePassLore(player, level, true, premiumUnlocked, playerLevel, display), player,
                    battlePassManager.getConfig().getBoolean("menu.premium-glow", true)));

            inventory.setItem(18 + i, battlePassStatusItem(player, level, freeUnlocked, playerLevel));

            inventory.setItem(freeSlot, item(battlePassIcon(freeUnlocked), "&aFree &7- &fLevel " + level,
                    battlePassLore(player, level, false, freeUnlocked, playerLevel, display), player));
        }

        if (page > 0) inventory.setItem(38, item(Material.GRAY_DYE, "&7Previous Page", List.of("&eClick to go back."), player));
        inventory.setItem(40, item(Material.RED_DYE, "&cClose", List.of("&7Click to close."), player));
        if (page + 1 < totalPages) inventory.setItem(42, item(Material.LIME_DYE, "&aNext Page", List.of("&eClick to continue."), player));
        fill(inventory);
        player.openInventory(inventory);
    }

    private Material battlePassIcon(boolean unlocked) {
        String key = unlocked ? "menu.unlocked-material" : "menu.locked-material";
        String fallback = unlocked ? "CHEST_MINECART" : "MINECART";
        try {
            return Material.valueOf(battlePassManager.getConfig().getString(key, fallback).toUpperCase());
        } catch (Exception ignored) {
            try { return Material.valueOf(unlocked ? "STORAGE_MINECART" : "MINECART"); } catch (Exception ignored2) { return Material.MINECART; }
        }
    }

    private ItemStack battlePassStatusItem(Player player, int level, boolean unlocked, int playerLevel) {
        return item(unlocked ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                (unlocked ? "&aUnlocked" : "&cLocked") + " &7- &fLevel " + level,
                List.of(
                        "&7Required Level: &e" + level,
                        "&7Your Level: &a" + playerLevel,
                        "",
                        unlocked ? "&aYou can claim rewards for this level." : "&cReach this level to unlock rewards."
                ), player, unlocked);
    }

    private List<String> battlePassLore(Player player, int level, boolean premium, boolean unlocked, int playerLevel, String display) {
        List<String> lore = new ArrayList<>();
        String track = premium ? "premium" : "free";
        lore.add("&7Reward: &f" + display);
        lore.add("&7Required Level: &e" + level);
        lore.add("&7Your Level: &a" + playerLevel);
        if (premium) lore.add("&7Premium Pass: " + (battlePassManager.hasPremium(player) ? "&aOwned" : "&cNot Owned"));
        lore.add("");
        lore.add(premium ? "&6Premium Rewards:" : "&aFree Rewards:");

        List<String> configuredLore = battlePassManager.getConfig().getStringList("rewards." + level + "." + track + ".lore");
        if (!configuredLore.isEmpty()) {
            lore.addAll(configuredLore);
        } else {
            for (String command : battlePassManager.getConfig().getStringList("rewards." + level + "." + track + ".commands")) {
                lore.add("&8- &f" + battlePassManager.describeReward(command));
            }
        }

        lore.add("");
        lore.add(unlocked ? "&aUnlocked" : "&cLocked");
        if (premium && !battlePassManager.hasPremium(player)) lore.add("&eUse &f/battlepass premium &eto buy premium.");
        return lore;
    }

    private int totalSkillLevels(Player player, List<String> skills) {
        int total = 0;
        for (String skill : skills) total += skillManager.getLevel(player, skill);
        return total;
    }

    private String topSkill(Player player, List<String> skills) {
        String best = null;
        int bestLevel = 0;
        for (String skill : skills) {
            int level = skillManager.getLevel(player, skill);
            if (best == null || level > bestLevel) {
                best = skill;
                bestLevel = level;
            }
        }
        return best == null ? "None" : pretty(best) + " &7Lv. &a" + bestLevel;
    }

    private int unlockedTreeLevels(Player player, String skill) {
        int total = 0;
        ConfigurationSection nodes = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("skills.trees." + skill.toLowerCase());
        if (nodes == null) return 0;
        for (String node : nodes.getKeys(false)) total += skillManager.getTreeLevel(player, skill, node);
        return total;
    }

    private int totalJobLevels(Player player, ConfigurationSection jobs) {
        if (jobs == null) return 0;
        int total = 0;
        for (String job : jobs.getKeys(false)) {
            if (jobs.isConfigurationSection(job)) total += jobManager.getLevel(player, job);
        }
        return total;
    }

    private String topJob(Player player, ConfigurationSection jobs) {
        if (jobs == null) return "None";
        String best = null;
        int bestLevel = 0;
        for (String job : jobs.getKeys(false)) {
            if (!jobs.isConfigurationSection(job)) continue;
            int level = jobManager.getLevel(player, job);
            if (best == null || level > bestLevel) {
                best = job;
                bestLevel = level;
            }
        }
        return best == null ? "None" : pretty(best) + " &7Lv. &a" + bestLevel;
    }

    private String jobActions(ConfigurationSection section) {
        if (section == null) return "Unknown";
        List<String> actions = section.getStringList("actions");
        if (actions.isEmpty()) return "Any";
        List<String> pretty = new ArrayList<>();
        for (String action : actions) pretty.add(pretty(action));
        return String.join(", ", pretty);
    }

    private String progressBar(int progress, int max) {
        int length = 10;
        int filled = Math.clamp((int) Math.floor((progress / (double) Math.max(1, max)) * length), 0, length);
        StringBuilder builder = new StringBuilder("&8[");
        for (int i = 0; i < length; i++) builder.append(i < filled ? "&a|" : "&7|");
        builder.append("&8]");
        return builder.toString();
    }

    private String skillColor(String skill) {
        return switch (skill.toLowerCase()) {
            case "woodcutting" -> "&2";
            case "farming" -> "&a";
            case "combat", "archery", "unarmed" -> "&c";
            case "fishing" -> "&3";
            case "excavation" -> "&e";
            case "alchemy", "enchanting" -> "&d";
            case "repair", "smelting" -> "&6";
            default -> "&b";
        };
    }

    private int[] skillSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };
    }

    private int[] jobSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };
    }

    private int[] skillTreeSlots(String skill) {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
    }

    private int nextOpenTreeSlot(Inventory inventory, int[] slots, int start) {
        for (int i = Math.max(0, start); i < slots.length; i++) {
            if (inventory.getItem(slots[i]) == null) return slots[i];
        }
        for (int slot : slots) {
            if (inventory.getItem(slot) == null) return slot;
        }
        return -1;
    }

    private void setMiningNode(Inventory inventory, Player player, int slot, String node, Material material, String name, String description) {
        setTreeNode(inventory, player, "mining", slot, node, material, name, description);
    }

    private void setTreeNode(Inventory inventory, Player player, String skill, int slot, String node, Material material, String name, String description) {
        int level = skillManager.getTreeLevel(player, skill, node);
        int max = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.trees." + skill.toLowerCase() + "." + node + ".max-level", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.mining-tree." + node + ".max-level", 5));
        String path = "skills.trees." + skill.toLowerCase() + "." + node;
        boolean unlocked = skillManager.meetsTreeRequirement(player, skill, node);
        List<String> lore = new ArrayList<>();
        lore.add(description);
        String effect = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("skills.trees." + skill.toLowerCase() + "." + node + ".effect", "");
        if (!effect.isBlank()) lore.add(effect);
        double xpBonus = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("skills.trees." + skill.toLowerCase() + "." + node + ".xp-bonus-per-level", 0.0D);
        if (xpBonus > 0.0D) lore.add("&7XP Bonus: &a+" + Math.round(xpBonus * 100.0D) + "% &7per level");
        addStatBonusLore(lore, path, "health", "&cHealth");
        addStatBonusLore(lore, path, "mana", "&bMana");
        addStatBonusLore(lore, path, "defense", "&aDefense");
        addStatBonusLore(lore, path, "damage", "&cDamage");
        String requiredNode = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".requires-node", "");
        int requiredLevel = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".requires-level", 1));
        int requiredSkillLevel = Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".requires-skill-level", 0));
        if (!requiredNode.isBlank() || requiredSkillLevel > 0) {
            lore.add("");
            lore.add("&7Requires:");
            if (!requiredNode.isBlank()) {
                lore.add((skillManager.getTreeLevel(player, skill, requiredNode) >= requiredLevel ? "&a" : "&c") + pretty(requiredNode) + " " + requiredLevel);
            }
            if (requiredSkillLevel > 0) {
                lore.add((skillManager.getLevel(player, skill) >= requiredSkillLevel ? "&a" : "&c") + pretty(skill) + " Level " + requiredSkillLevel);
            }
        }
        lore.add("");
        lore.add("&7Level: &a" + level + "&7/&a" + max);
        lore.add("&7Cost: &e1 Skill Point");
        lore.add("");
        lore.add(level >= max ? "&aMaxed" : unlocked ? "&eClick to upgrade" : "&cLocked");
        lore.add("&8Skill Node: " + node.toLowerCase());
        inventory.setItem(slot, item(unlocked ? material : Material.GRAY_DYE, name, lore, player, level > 0));
    }

    private ItemStack adventureQuestItem(Player player, String period, String questId) {
        Material material = questManager.getAdventureQuestMaterial(period, questId);
        int required = questManager.getAdventureQuestRequiredAmount(period, questId);
        int progress = Math.min(required, questManager.getAdventureQuestProgress(player, period, questId));
        boolean accepted = questManager.isAdventureQuestAccepted(player, period, questId);
        boolean claimed = questManager.isAdventureQuestClaimed(player, period, questId);
        boolean ready = questManager.canClaimAdventureQuest(player, period, questId);
        String status = claimed ? "&aClaimed" : ready ? "&eReady to claim" : accepted ? "&bAccepted" : "&7Available";
        List<String> lore = new ArrayList<>();
        lore.add("&7Type: &f" + pretty(period));
        lore.add("&7Status: " + status);
        lore.add("&7Progress: &f" + progress + "&7/&f" + required + " " + pretty(material.name()));
        lore.add(progressBar(progress, required));
        List<String> rewards = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("adventure-guild.quests." + period.toLowerCase() + "." + questId.toLowerCase() + ".reward-commands");
        if (!rewards.isEmpty()) {
            lore.add("");
            lore.add("&7Rewards:");
            for (String reward : rewards) lore.add("&8- &f" + describeReward(reward));
        }
        lore.add("");
        lore.add(claimed ? "&7Resets next " + period + "." : ready ? "&eClick to claim rewards" : accepted ? "&7Bring the required items back." : "&eClick to accept");
        lore.add("&8Adventure Quest: " + period + ":" + questId);
        return item(material == Material.AIR ? Material.PAPER : material, questManager.getAdventureQuestName(period, questId), lore, player, ready && !claimed);
    }

    private List<String> shopOverviewLore(Player player, String shopKey, int items, int page, int totalPages) {
        List<String> lore = new ArrayList<>(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("shops." + shopKey + ".description"));
        if (lore.isEmpty()) {
            lore.add("&7Buy supplies with left click.");
            if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("shops." + shopKey + ".sell-enabled", true)) lore.add("&7Sell matching items with right click.");
            else lore.add("&7This is a buy-only shop.");
        }
        lore.add("");
        lore.add("&7Items: &f" + items);
        lore.add("&7Page: &f" + (page + 1) + "&7/&f" + totalPages);
        lore.add("&7Purse: &e" + economyManager.format(economyManager.getCoins(player)) + " coins");
        return lore;
    }

    private Material shopIcon(String shopKey) {
        String configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("shops." + shopKey + ".icon", "EMERALD");
        try {
            return Material.valueOf(configured.toUpperCase());
        } catch (Exception ignored) {
            return Material.EMERALD;
        }
    }

    private String prettyMaterialName(Material material) {
        if (material == null) return "Unknown Item";
        StringBuilder out = new StringBuilder();
        for (String part : material.name().toLowerCase(Locale.ROOT).replace('_', ' ').split(" ")) {
            if (!part.isBlank()) out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return out.toString().trim();
    }

    private boolean isInternalShopName(String name) {
        if (name == null) return false;
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("vanilla_") || normalized.startsWith("custom_") || normalized.startsWith("shop_entry_");
    }

    private boolean isShopItemDisplayable(ConfigurationSection section, String id) {
        String customItemId = section.getString(id + ".custom-item", "");
        String customEnchantId = section.getString(id + ".custom-enchant", "");
        if (!customItemId.isBlank()) return customItemManager.itemExists(customItemId) && !forbiddenShopItem(customItemId.toLowerCase(Locale.ROOT));
        if (!customEnchantId.isBlank()) return true;
        try {
            Material material = Material.valueOf(section.getString(id + ".material", "EMERALD").toUpperCase());
            return canCreateItemStack(material);
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<String> shopRequirementLore(Player player, ConfigurationSection requirements) {
        if (requirements == null) return List.of();
        List<String> lore = new ArrayList<>();
        String permission = requirements.getString("permission", "");
        if (!permission.isBlank()) lore.add(CC.color((player.hasPermission(permission) ? "&a✔ " : "&c✖ ") + "&7Special vendor unlock"));
        String skill = requirements.getString("skill", ""); int level = Math.max(0, requirements.getInt("skill-level", 0));
        if (!skill.isBlank() && level > 0) lore.add(CC.color((skillManager.getLevel(player, skill) >= level ? "&a✔ " : "&c✖ ") + "&7Requires " + pretty(skill) + " " + level));
        return lore.isEmpty() ? List.of() : lore;
    }

    private boolean hasReadyAdventureQuest(Player player, List<String> questIds, String period) {
        for (String questId : questIds) {
            if (questManager.canClaimAdventureQuest(player, period, questId)) return true;
        }
        return false;
    }

    private String adventureSummary(Player player, String period, List<String> questIds) {
        int ready = 0;
        int claimed = 0;
        int accepted = 0;
        for (String questId : questIds) {
            if (questManager.isAdventureQuestClaimed(player, period, questId)) claimed++;
            else if (questManager.canClaimAdventureQuest(player, period, questId)) ready++;
            else if (questManager.isAdventureQuestAccepted(player, period, questId)) accepted++;
        }
        return "&e" + ready + " ready &8/ &b" + accepted + " active &8/ &a" + claimed + " claimed";
    }

    private String describeReward(String command) {
        String clean = command == null ? "" : command.replace("%player%", "").trim();
        String lower = clean.toLowerCase();
        if (lower.startsWith("coins give")) {
            String[] parts = clean.split("\\s+");
            return parts.length >= 3 ? parts[2] + " coins" : "Coins";
        }
        if (lower.startsWith("give")) {
            String[] parts = clean.split("\\s+");
            if (parts.length >= 3) {
                String amount = parts.length >= 4 ? parts[3] + "x " : "";
                return amount + pretty(parts[2]);
            }
        }
        return clean.isBlank() ? "Reward" : clean;
    }

    private int bazaarCategorySlot(String category) {
        return switch (category.toLowerCase()) {
            case "farming" -> 10;
            case "mining" -> 11;
            case "combat" -> 12;
            case "foraging" -> 13;
            case "fishing" -> 14;
            case "dungeon" -> 15;
            case "event" -> 16;
            case "vanilla" -> 22;
            default -> 23;
        };
    }

    private ItemStack bazaarPreview(Player player, String itemId, boolean clickable) {
        ItemStack display = bazaarManager.createItemStack(itemId, 1).clone();
        display.setAmount(1);
        customItemManager.rebuildLoreFor(player, display);
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;
        List<String> lore = new ArrayList<>();
        boolean vanilla = itemId.toLowerCase(Locale.ROOT).startsWith("vanilla:");
        if (vanilla) {
            lore.add(CC.color("&7Category: &eVanilla"));
            lore.add(CC.color("&7Buy Price: &6" + economyManager.format(bazaarManager.getBuyPrice(itemId)) + " Coins"));
            lore.add(CC.color("&7Sell Price: &6" + economyManager.format(bazaarManager.getSellPrice(itemId)) + " Coins"));
            lore.add(CC.color("&7Market Key: &8" + itemId));
            if (clickable) { lore.add(""); lore.add(CC.color("&eLeft Click to buy!")); lore.add(CC.color("&bRight Click to sell!")); }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(bazaarItemKey, PersistentDataType.STRING, itemId);
            if (clickable) meta.getPersistentDataContainer().set(bazaarActionKey, PersistentDataType.STRING, "view");
            display.setItemMeta(meta);
            return display;
        }
        if (meta.hasLore()) {
            for (String line : Objects.requireNonNull(meta.getLore())) {
                String clean = ChatColor.stripColor(line);
                if (clean == null || !clean.toLowerCase().contains("custom survival item")) lore.add(line);
            }
        }
        String rarity = customItemManager.getCustomItemRarity(display);
        if (rarity != null && !rarity.isBlank() && lore.stream().map(ChatColor::stripColor)
                .noneMatch(line -> line != null && line.toLowerCase().contains("rarity"))) {
            lore.add(CC.color("&7Rarity: &f" + rarity));
        }
        if (!lore.isEmpty()) lore.add("");
        lore.add(CC.color("&7Buy Price: &6" + economyManager.format(bazaarManager.getBuyPrice(itemId)) + " Coins"));
        lore.add(CC.color("&7Sell Price: &6" + economyManager.format(bazaarManager.getSellPrice(itemId)) + " Coins"));
        lore.add(CC.color("&7Available Supply: &e" + bazaarManager.getAvailableSupply(itemId)));
        lore.add(CC.color("&7Demand: &e" + bazaarManager.getDemand(itemId)));
        lore.add("");
        lore.add(CC.color("&7Market Price: &6" + economyManager.format(bazaarManager.getMarketPrice(itemId))));
        lore.add(CC.color("&7Trend: " + bazaarManager.getTrend(itemId)));
        lore.add("");
        lore.add(CC.color("&7Custom ID: &8" + itemId));
        if (clickable) {
            lore.add("");
            lore.add(CC.color("&eClick to view buy and sell options!"));
            meta.getPersistentDataContainer().set(bazaarActionKey, PersistentDataType.STRING, "view");
        }
        meta.getPersistentDataContainer().set(bazaarItemKey, PersistentDataType.STRING, itemId);
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack bazaarBuyButton(Player player, String itemId, int amount) {
        return bazaarButton(Material.LIME_DYE, "&aBuy " + amount, List.of(
                "&7Amount: &e" + amount,
                "&7Cost: &6" + economyManager.format(bazaarManager.getInstantBuyCost(itemId, amount)) + " Coins",
                "",
                "&eClick to buy!"
        ), "buy:" + amount, player);
    }

    private ItemStack bazaarSellButton(Player player, String itemId, int amount, int playerAmount) {
        return bazaarButton(Material.ORANGE_DYE, "&6Sell " + amount, List.of(
                "&7Amount: &e" + amount,
                "&7You have: &e" + playerAmount,
                "&7Value: &6" + economyManager.format(bazaarManager.getInstantSellValue(itemId, amount)) + " Coins",
                "",
                "&eClick to sell!"
        ), "sell:" + amount, player);
    }

    private ItemStack disabledBazaarButton(String name) {
        ItemStack disabled = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = disabled.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            meta.setLore(List.of(CC.color("&7Orders are currently unavailable.")));
            meta.getPersistentDataContainer().set(bazaarActionKey, PersistentDataType.STRING, "disabled");
            disabled.setItemMeta(meta);
        }
        return disabled;
    }

    private ItemStack bazaarButton(Material material, String name, List<String> lore, String action, Player player) {
        ItemStack button = item(material, name, lore, player);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(bazaarActionKey, PersistentDataType.STRING, action);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack item(ItemStack base, String name, List<String> lore, Player player) {
        ItemStack item = base == null ? new ItemStack(Material.PAPER) : base.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(placeholders.parse(player, CC.color(name)));
            List<String> parsedLore = new ArrayList<>();
            for (String line : lore) parsedLore.add(placeholders.parse(player, CC.color(line)));
            meta.setLore(parsedLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack item(Material material, String name, List<String> lore, Player player) {
        return item(material, name, lore, player, false);
    }

    private ItemStack item(Material material, String name, List<String> lore, Player player, boolean glow) {
        ItemStack item = new ItemStack(canCreateItemStack(material) ? material : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(placeholders.parse(player, CC.color(name)));
            List<String> parsedLore = new ArrayList<>();
            for (String line : lore) parsedLore.add(placeholders.parse(player, CC.color(line)));
            meta.setLore(parsedLore);
            if (glow) meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean canCreateItemStack(Material material) {
        if (material == null || material.isAir()) return false;
        try {
            new ItemStack(material, 1);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private void fill(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < inventory.getSize(); i++) if (inventory.getItem(i) == null) inventory.setItem(i, filler);
    }

    private double maxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("skills.base-health", 20.0D) : Math.max(1.0D, attribute.getValue());
    }

    private double maxMana(Player player) {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("custom-items.mana.max",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("actionbar.mana.max", 100.0D)) + skillManager.getBonusMana(player);
    }

    private String whole(double value) {
        return String.valueOf((int) Math.round(value));
    }

    private String one(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private int[] auctionSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
    }

    private int[] shopSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };
    }

    private int[] auctionListingSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };
    }

    private void setAuctionCategories(Inventory inventory, Player player, String selectedCategory) {
        int[] slots = {37, 38, 39, 40, 41, 42, 43};
        List<String> categories = auctionHouseManager.getCategories();
        for (int index = 0; index < slots.length && index < categories.size(); index++) {
            String category = categories.get(index);
            boolean selected = category.equalsIgnoreCase(selectedCategory);
            inventory.setItem(slots[index], item(auctionCategoryIcon(category),
                    (selected ? "&a" : "&e") + pretty(category),
                    List.of(
                            "&7Filter visible auction listings.",
                            "&7Listings: &f" + auctionHouseManager.getListings(category).size(),
                            "",
                            selected ? "&aCurrently selected" : "&eClick to filter",
                            "&8Auction Category: " + category
                    ), player, selected));
        }
    }

    private Material auctionCategoryIcon(String category) {
        return switch (category.toLowerCase()) {
            case "all" -> Material.NETHER_STAR;
            case "blocks" -> Material.BRICKS;
            case "resources" -> Material.DIAMOND;
            case "gear" -> Material.DIAMOND_CHESTPLATE;
            case "farming" -> Material.WHEAT;
            case "combat" -> Material.DIAMOND_SWORD;
            case "tools" -> Material.IRON_PICKAXE;
            default -> Material.CHEST;
        };
    }

    private String sellerName(String id) {
        java.util.UUID owner = auctionHouseManager.getOwner(id);
        if (owner == null) return "Unknown";
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner);
        return offlinePlayer.getName() == null ? owner.toString().substring(0, 8) : offlinePlayer.getName();
    }

    private String formatListingLimit(int maxListings) {
        return maxListings == Integer.MAX_VALUE ? "Unlimited" : String.valueOf(maxListings);
    }

    private String formatAge(long createdAt) {
        if (createdAt <= 0L) return "Unknown";
        long seconds = Math.max(0L, (System.currentTimeMillis() - createdAt) / 1000L);
        if (seconds < 60L) return "Just now";
        long minutes = seconds / 60L;
        if (minutes < 60L) return minutes + "m ago";
        long hours = minutes / 60L;
        if (hours < 24L) return hours + "h ago";
        long days = hours / 24L;
        return days + "d ago";
    }

    private Material getSkillMaterial(String skill) {
        return switch (skill.toLowerCase()) {
            case "mining" -> Material.DIAMOND_PICKAXE;
            case "woodcutting" -> Material.DIAMOND_AXE;
            case "farming" -> Material.WHEAT;
            case "combat" -> Material.DIAMOND_SWORD;
            case "unarmed" -> Material.IRON_INGOT;
            case "fishing" -> Material.FISHING_ROD;
            case "excavation" -> Material.IRON_SHOVEL;
            case "archery" -> Material.BOW;
            case "taming" -> Material.BONE;
            case "acrobatics" -> Material.FEATHER;
            case "alchemy" -> Material.BREWING_STAND;
            case "repair" -> Material.ANVIL;
            case "smelting" -> Material.FURNACE;
            case "enchanting" -> Material.ENCHANTING_TABLE;
            default -> Material.EXPERIENCE_BOTTLE;
        };
    }

    private Material getNodeMaterial(String node) {
        return switch (node.toLowerCase()) {
            case "xp_boost" -> Material.EXPERIENCE_BOTTLE;
            case "lucky", "treasure", "rare_finds", "archaeology", "magic_hunter" -> Material.EMERALD;
            case "double_drops", "critical", "mastery", "serrated_strikes", "daze", "skull_splitter" -> Material.DIAMOND;
            case "speed", "haste", "mobility", "super_breaker", "tree_feller", "giga_drill", "roll" -> Material.SUGAR;
            case "defense", "armor", "iron_grip", "salvage" -> Material.SHIELD;
            case "damage", "power", "bleed", "arrow_retrieval", "power_shot" -> Material.IRON_SWORD;
            case "money", "coins", "angler", "alchemist" -> Material.GOLD_INGOT;
            case "cooldown", "cooldowns", "potion_duration" -> Material.CLOCK;
            case "durability", "repair_mastery", "arcane_forging" -> Material.ANVIL;
            default -> Material.NETHER_STAR;
        };
    }

    private void addStatBonusLore(List<String> lore, String path, String stat, String display) {
        double amount = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path + "." + stat + "-per-level", 0.0D);
        if (amount <= 0.0D) return;
        lore.add("&7" + display + ": &a+" + formatStat(amount) + " &7per level");
    }

    private String formatStat(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) return String.valueOf((int) Math.rint(value));
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private Material materialFromConfig(String value, Material fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Material.valueOf(value.toUpperCase());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Material getJobMaterial(String job) {
        return switch (job.toLowerCase()) {
            case "miner" -> Material.IRON_PICKAXE;
            case "lumberjack", "woodcutter" -> Material.IRON_AXE;
            case "farmer" -> Material.HAY_BLOCK;
            case "hunter" -> Material.BOW;
            case "fisher" -> Material.FISHING_ROD;
            case "builder" -> Material.BRICKS;
            case "enchanter" -> Material.ENCHANTING_TABLE;
            case "brewer" -> Material.BREWING_STAND;
            default -> Material.EMERALD;
        };
    }

    private String pretty(String text) {
        if (text == null || text.isEmpty()) return "None";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
