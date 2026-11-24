package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.registries.VanillaBazaarRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Beehive;
import org.bukkit.block.Container;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class BazaarManager {
    private static final Map<String, BazaarCategory> DEFAULT_CATEGORIES = new LinkedHashMap<>();
    private static final Map<String, BazaarItem> DEFAULT_ITEMS = new LinkedHashMap<>();
    private static final Set<String> EXCLUDED_MATERIALS = Set.of(
            "AIR",
            "BEDROCK",
            "BARRIER",
            "LIGHT",
            "COMMAND_BLOCK",
            "CHAIN_COMMAND_BLOCK",
            "REPEATING_COMMAND_BLOCK",
            "COMMAND_BLOCK_MINECART",
            "STRUCTURE_BLOCK",
            "STRUCTURE_VOID",
            "JIGSAW",
            "DEBUG_STICK",
            "KNOWLEDGE_BOOK",
            "SPAWNER",
            "TRIAL_SPAWNER",
            "VAULT",
            "END_PORTAL_FRAME",
            "REINFORCED_DEEPSLATE"
    );
    private static final Set<Material> VANILLA_MARKET = Set.of(
            Material.STONE, Material.COBBLESTONE, Material.DIRT, Material.GRASS_BLOCK, Material.SAND,
            Material.GRAVEL, Material.CLAY, Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG,
            Material.CHERRY_LOG, Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.COAL,
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.COPPER_INGOT, Material.REDSTONE,
            Material.LAPIS_LAZULI, Material.DIAMOND, Material.EMERALD, Material.QUARTZ, Material.WHEAT,
            Material.CARROT, Material.POTATO, Material.BEETROOT, Material.SUGAR_CANE, Material.CACTUS,
            Material.PUMPKIN, Material.MELON_SLICE, Material.LEATHER, Material.FEATHER, Material.STRING,
            Material.BONE, Material.ROTTEN_FLESH, Material.SPIDER_EYE, Material.ENDER_PEARL,
            Material.GUNPOWDER, Material.SLIME_BALL, Material.COD, Material.SALMON,
            Material.PUFFERFISH, Material.TROPICAL_FISH
    );

    static {
        registerCategory("farming", "&aFarming", Material.WHEAT);
        registerCategory("woodcutting", "&2Woodcutting", Material.OAK_LOG);
        registerCategory("mining", "&bMining", Material.DIAMOND_PICKAXE);
        registerCategory("building_blocks", "&eBuilding Blocks", Material.BRICKS);
        registerCategory("redstone", "&cRedstone", Material.REDSTONE);
        registerCategory("combat", "&cCombat", Material.DIAMOND_SWORD);
        registerCategory("tools_and_armor", "&6Tools & Armor", Material.IRON_CHESTPLATE);
        registerCategory("decorations", "&dDecorations", Material.PAINTING);
        registerCategory("music_and_patterns", "&5Music & Patterns", Material.JUKEBOX);
        registerCategory("spawn_eggs", "&9Spawn Eggs", Material.CREEPER_SPAWN_EGG);
        registerCategory("transportation", "&3Transportation", Material.OAK_BOAT);
        registerCategory("custom_mining", "&bMining Materials", Material.DIAMOND);
        registerCategory("custom_farming", "&aFarming Materials", Material.WHEAT);
        registerCategory("custom_foraging", "&2Foraging Materials", Material.OAK_LOG);
        registerCategory("custom_combat", "&cCombat Materials", Material.ROTTEN_FLESH);
        registerCategory("custom_fishing", "&3Fishing Materials", Material.PRISMARINE_SHARD);
        registerCategory("custom_dungeon", "&5Dungeon Materials", Material.ECHO_SHARD);
        registerCategory("custom_event", "&dEvent Materials", Material.NETHER_STAR);
        registerCategory("custom_recipe", "&eRecipe Materials", Material.CRAFTING_TABLE);
        registerCategory("custom_misc", "&7Misc Materials", Material.CHEST);
        registerCategory("vanilla", "&fVanilla", Material.GRASS_BLOCK);

        register("farming", 10,
                Material.BUSH, Material.CACTUS_FLOWER, Material.CLOSED_EYEBLOSSOM, Material.FIREFLY_BUSH,
                Material.LEAF_LITTER, Material.OPEN_EYEBLOSSOM, Material.SHORT_DRY_GRASS,
                Material.TALL_DRY_GRASS, Material.WILDFLOWERS, Material.BLUE_EGG, Material.BROWN_EGG,
                Material.TORCHFLOWER, Material.TORCHFLOWER_SEEDS, Material.SNIFFER_EGG);

        register("woodcutting", 18,
                Material.PALE_OAK_LOG, Material.PALE_OAK_WOOD, Material.STRIPPED_PALE_OAK_LOG,
                Material.STRIPPED_PALE_OAK_WOOD, Material.PALE_OAK_PLANKS, Material.PALE_OAK_LEAVES,
                Material.PALE_OAK_SAPLING, Material.PALE_OAK_SLAB, Material.PALE_OAK_STAIRS,
                Material.PALE_OAK_FENCE, Material.PALE_OAK_FENCE_GATE, Material.PALE_OAK_DOOR,
                Material.PALE_OAK_TRAPDOOR, Material.PALE_OAK_BUTTON, Material.PALE_OAK_PRESSURE_PLATE,
                Material.PALE_OAK_SIGN, Material.PALE_OAK_HANGING_SIGN, Material.PALE_HANGING_MOSS,
                Material.PALE_MOSS_BLOCK, Material.PALE_MOSS_CARPET, Material.PALE_OAK_BOAT,
                Material.PALE_OAK_CHEST_BOAT);

        register("mining", 24,
                Material.TUFF, Material.TUFF_SLAB, Material.TUFF_STAIRS, Material.TUFF_WALL,
                Material.CHISELED_TUFF, Material.POLISHED_TUFF, Material.POLISHED_TUFF_SLAB,
                Material.POLISHED_TUFF_STAIRS, Material.POLISHED_TUFF_WALL, Material.TUFF_BRICKS,
                Material.TUFF_BRICK_SLAB, Material.TUFF_BRICK_STAIRS, Material.TUFF_BRICK_WALL,
                Material.CHISELED_TUFF_BRICKS, Material.HEAVY_CORE, Material.RESIN_CLUMP,
                Material.RESIN_BLOCK, Material.RESIN_BRICK, Material.RESIN_BRICKS,
                Material.CHISELED_RESIN_BRICKS, Material.RESIN_BRICK_SLAB, Material.RESIN_BRICK_STAIRS,
                Material.RESIN_BRICK_WALL, Material.COPPER_NUGGET, Material.COPPER_INGOT,
                Material.RAW_COPPER, Material.RAW_COPPER_BLOCK);

        register("building_blocks", 32,
                Material.COPPER_BLOCK, Material.EXPOSED_COPPER, Material.WEATHERED_COPPER,
                Material.OXIDIZED_COPPER, Material.WAXED_COPPER_BLOCK, Material.WAXED_EXPOSED_COPPER,
                Material.WAXED_WEATHERED_COPPER, Material.WAXED_OXIDIZED_COPPER, Material.CUT_COPPER,
                Material.EXPOSED_CUT_COPPER, Material.WEATHERED_CUT_COPPER, Material.OXIDIZED_CUT_COPPER,
                Material.WAXED_CUT_COPPER, Material.WAXED_EXPOSED_CUT_COPPER,
                Material.WAXED_WEATHERED_CUT_COPPER, Material.WAXED_OXIDIZED_CUT_COPPER,
                Material.CUT_COPPER_SLAB, Material.EXPOSED_CUT_COPPER_SLAB,
                Material.WEATHERED_CUT_COPPER_SLAB, Material.OXIDIZED_CUT_COPPER_SLAB,
                Material.WAXED_CUT_COPPER_SLAB, Material.WAXED_EXPOSED_CUT_COPPER_SLAB,
                Material.WAXED_WEATHERED_CUT_COPPER_SLAB, Material.WAXED_OXIDIZED_CUT_COPPER_SLAB,
                Material.CUT_COPPER_STAIRS, Material.EXPOSED_CUT_COPPER_STAIRS,
                Material.WEATHERED_CUT_COPPER_STAIRS, Material.OXIDIZED_CUT_COPPER_STAIRS,
                Material.WAXED_CUT_COPPER_STAIRS, Material.WAXED_EXPOSED_CUT_COPPER_STAIRS,
                Material.WAXED_WEATHERED_CUT_COPPER_STAIRS, Material.WAXED_OXIDIZED_CUT_COPPER_STAIRS,
                Material.CHISELED_COPPER, Material.EXPOSED_CHISELED_COPPER, Material.WEATHERED_CHISELED_COPPER,
                Material.OXIDIZED_CHISELED_COPPER, Material.WAXED_CHISELED_COPPER,
                Material.WAXED_EXPOSED_CHISELED_COPPER, Material.WAXED_WEATHERED_CHISELED_COPPER,
                Material.WAXED_OXIDIZED_CHISELED_COPPER, Material.COPPER_DOOR, Material.EXPOSED_COPPER_DOOR,
                Material.WEATHERED_COPPER_DOOR, Material.OXIDIZED_COPPER_DOOR, Material.WAXED_COPPER_DOOR,
                Material.WAXED_EXPOSED_COPPER_DOOR, Material.WAXED_WEATHERED_COPPER_DOOR,
                Material.WAXED_OXIDIZED_COPPER_DOOR, Material.COPPER_TRAPDOOR, Material.EXPOSED_COPPER_TRAPDOOR,
                Material.WEATHERED_COPPER_TRAPDOOR, Material.OXIDIZED_COPPER_TRAPDOOR,
                Material.WAXED_COPPER_TRAPDOOR, Material.WAXED_EXPOSED_COPPER_TRAPDOOR,
                Material.WAXED_WEATHERED_COPPER_TRAPDOOR, Material.WAXED_OXIDIZED_COPPER_TRAPDOOR,
                Material.COPPER_GRATE, Material.EXPOSED_COPPER_GRATE, Material.WEATHERED_COPPER_GRATE,
                Material.OXIDIZED_COPPER_GRATE, Material.WAXED_COPPER_GRATE, Material.WAXED_EXPOSED_COPPER_GRATE,
                Material.WAXED_WEATHERED_COPPER_GRATE, Material.WAXED_OXIDIZED_COPPER_GRATE,
                Material.COPPER_BARS, Material.EXPOSED_COPPER_BARS, Material.WEATHERED_COPPER_BARS,
                Material.OXIDIZED_COPPER_BARS, Material.WAXED_COPPER_BARS, Material.WAXED_EXPOSED_COPPER_BARS,
                Material.WAXED_WEATHERED_COPPER_BARS, Material.WAXED_OXIDIZED_COPPER_BARS,
                Material.COPPER_CHAIN, Material.EXPOSED_COPPER_CHAIN, Material.WEATHERED_COPPER_CHAIN,
                Material.OXIDIZED_COPPER_CHAIN, Material.WAXED_COPPER_CHAIN, Material.WAXED_EXPOSED_COPPER_CHAIN,
                Material.WAXED_WEATHERED_COPPER_CHAIN, Material.WAXED_OXIDIZED_COPPER_CHAIN,
                Material.IRON_CHAIN);

        register("redstone", 48,
                Material.CRAFTER, Material.TRIAL_SPAWNER, Material.VAULT, Material.COPPER_BULB,
                Material.EXPOSED_COPPER_BULB, Material.WEATHERED_COPPER_BULB, Material.OXIDIZED_COPPER_BULB,
                Material.WAXED_COPPER_BULB, Material.WAXED_EXPOSED_COPPER_BULB,
                Material.WAXED_WEATHERED_COPPER_BULB, Material.WAXED_OXIDIZED_COPPER_BULB,
                Material.COPPER_CHEST, Material.EXPOSED_COPPER_CHEST, Material.WEATHERED_COPPER_CHEST,
                Material.OXIDIZED_COPPER_CHEST, Material.WAXED_COPPER_CHEST,
                Material.WAXED_EXPOSED_COPPER_CHEST, Material.WAXED_WEATHERED_COPPER_CHEST,
                Material.WAXED_OXIDIZED_COPPER_CHEST, Material.EXPOSED_LIGHTNING_ROD,
                Material.WEATHERED_LIGHTNING_ROD, Material.OXIDIZED_LIGHTNING_ROD, Material.WAXED_LIGHTNING_ROD,
                Material.WAXED_EXPOSED_LIGHTNING_ROD, Material.WAXED_WEATHERED_LIGHTNING_ROD,
                Material.WAXED_OXIDIZED_LIGHTNING_ROD);

        register("combat", 120,
                Material.ARMADILLO_SCUTE, Material.BREEZE_ROD, Material.DRIED_GHAST, Material.MACE,
                Material.OMINOUS_BOTTLE, Material.OMINOUS_TRIAL_KEY, Material.TRIAL_KEY, Material.WIND_CHARGE,
                Material.WOODEN_SPEAR, Material.STONE_SPEAR, Material.COPPER_SPEAR, Material.IRON_SPEAR,
                Material.GOLDEN_SPEAR, Material.DIAMOND_SPEAR, Material.NETHERITE_SPEAR);

        register("tools_and_armor", 180,
                Material.COPPER_AXE, Material.COPPER_BOOTS, Material.COPPER_CHESTPLATE, Material.COPPER_HELMET,
                Material.COPPER_HOE, Material.COPPER_HORSE_ARMOR, Material.COPPER_LEGGINGS,
                Material.COPPER_NAUTILUS_ARMOR, Material.COPPER_PICKAXE, Material.COPPER_SHOVEL,
                Material.COPPER_SWORD, Material.IRON_NAUTILUS_ARMOR, Material.GOLDEN_NAUTILUS_ARMOR,
                Material.DIAMOND_NAUTILUS_ARMOR, Material.NETHERITE_NAUTILUS_ARMOR,
                Material.NETHERITE_HORSE_ARMOR);

        register("decorations", 42,
                Material.ACACIA_SHELF, Material.BAMBOO_SHELF, Material.BIRCH_SHELF, Material.CHERRY_SHELF,
                Material.COPPER_GOLEM_STATUE, Material.EXPOSED_COPPER_GOLEM_STATUE,
                Material.WEATHERED_COPPER_GOLEM_STATUE, Material.OXIDIZED_COPPER_GOLEM_STATUE,
                Material.WAXED_COPPER_GOLEM_STATUE, Material.WAXED_EXPOSED_COPPER_GOLEM_STATUE,
                Material.WAXED_WEATHERED_COPPER_GOLEM_STATUE, Material.WAXED_OXIDIZED_COPPER_GOLEM_STATUE,
                Material.COPPER_LANTERN, Material.EXPOSED_COPPER_LANTERN, Material.WEATHERED_COPPER_LANTERN,
                Material.OXIDIZED_COPPER_LANTERN, Material.WAXED_COPPER_LANTERN,
                Material.WAXED_EXPOSED_COPPER_LANTERN, Material.WAXED_WEATHERED_COPPER_LANTERN,
                Material.WAXED_OXIDIZED_COPPER_LANTERN, Material.COPPER_TORCH, Material.CREAKING_HEART,
                Material.CRIMSON_SHELF, Material.DARK_OAK_SHELF, Material.JUNGLE_SHELF, Material.MANGROVE_SHELF,
                Material.OAK_SHELF, Material.PALE_OAK_SHELF, Material.SPRUCE_SHELF, Material.WARPED_SHELF);

        register("music_and_patterns", 250,
                Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, Material.BORDURE_INDENTED_BANNER_PATTERN,
                Material.FIELD_MASONED_BANNER_PATTERN, Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,
                Material.FLOW_BANNER_PATTERN, Material.FLOWER_BANNER_PATTERN, Material.GUSTER_BANNER_PATTERN,
                Material.FLOW_POTTERY_SHERD, Material.GUSTER_POTTERY_SHERD, Material.SCRAPE_POTTERY_SHERD,
                Material.MUSIC_DISC_CREATOR, Material.MUSIC_DISC_CREATOR_MUSIC_BOX,
                Material.MUSIC_DISC_LAVA_CHICKEN, Material.MUSIC_DISC_PRECIPICE, Material.MUSIC_DISC_TEARS);

        register("spawn_eggs", 300,
                Material.ARMADILLO_SPAWN_EGG, Material.BOGGED_SPAWN_EGG, Material.BREEZE_SPAWN_EGG,
                Material.CAMEL_HUSK_SPAWN_EGG, Material.COPPER_GOLEM_SPAWN_EGG, Material.CREAKING_SPAWN_EGG,
                Material.HAPPY_GHAST_SPAWN_EGG, Material.NAUTILUS_SPAWN_EGG, Material.PARCHED_SPAWN_EGG,
                Material.ZOMBIE_NAUTILUS_SPAWN_EGG);

        register("transportation", 90,
                Material.PALE_OAK_BOAT, Material.PALE_OAK_CHEST_BOAT, Material.BLACK_BUNDLE, Material.BLUE_BUNDLE,
                Material.BROWN_BUNDLE, Material.CYAN_BUNDLE, Material.GRAY_BUNDLE, Material.GREEN_BUNDLE,
                Material.LIGHT_BLUE_BUNDLE, Material.LIGHT_GRAY_BUNDLE, Material.LIME_BUNDLE,
                Material.MAGENTA_BUNDLE, Material.ORANGE_BUNDLE, Material.PINK_BUNDLE, Material.PURPLE_BUNDLE,
                Material.RED_BUNDLE, Material.WHITE_BUNDLE, Material.YELLOW_BUNDLE, Material.BLACK_HARNESS,
                Material.BLUE_HARNESS, Material.BROWN_HARNESS, Material.CYAN_HARNESS, Material.GRAY_HARNESS,
                Material.GREEN_HARNESS, Material.LIGHT_BLUE_HARNESS, Material.LIGHT_GRAY_HARNESS,
                Material.LIME_HARNESS, Material.MAGENTA_HARNESS, Material.ORANGE_HARNESS, Material.PINK_HARNESS,
                Material.PURPLE_HARNESS, Material.RED_HARNESS, Material.WHITE_HARNESS, Material.YELLOW_HARNESS);

    }

    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final DiscordWebhookManager discordWebhookManager;
    private final CustomItemManager customItemManager;
    private final BazaarAlgorithm bazaarAlgorithm = new BazaarAlgorithm();
    private final Map<String, BazaarMarketData> marketData = new ConcurrentHashMap<>();
    private final Set<String> initializedMarketItemIds = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean recalculateRunning = new AtomicBoolean();
    private final AtomicBoolean marketReloadRunning = new AtomicBoolean();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicInteger activeTasks = new AtomicInteger();
    private final Set<BukkitTask> asyncTasks = ConcurrentHashMap.newKeySet();
    private final Object taskMonitor = new Object();
    private final Set<String> warnedInvalidCustomItems = ConcurrentHashMap.newKeySet();
    private volatile Map<String, CustomBazaarItem> customMaterialItems = Map.of();
    private volatile boolean marketCacheLoading;
    private final boolean registryReady;
    private volatile long nextDatabaseErrorLogAt;
    private BukkitTask marketTask;
    private SmpAlertService alertService;

    public BazaarManager(JavaPlugin plugin, DataManager dataManager, EconomyManager economyManager) {
        this(plugin, dataManager, economyManager, null, null);
    }

    public void setAlertService(SmpAlertService alertService) { this.alertService = alertService; }

    public BazaarManager(JavaPlugin plugin, DataManager dataManager, EconomyManager economyManager, DiscordWebhookManager discordWebhookManager) {
        this(plugin, dataManager, economyManager, discordWebhookManager, null);
    }

    public BazaarManager(JavaPlugin plugin, DataManager dataManager, EconomyManager economyManager,
                         DiscordWebhookManager discordWebhookManager, CustomItemManager customItemManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.economyManager = economyManager;
        this.discordWebhookManager = discordWebhookManager;
        this.customItemManager = customItemManager;
        shuttingDown.set(false);
        running.set(true);
        refreshCustomMaterialRegistry();
        registryReady = true;
        startMarketTask();
        reloadMarketDataAsync();
    }

    public void refreshCustomMaterialRegistry() {
        if (customItemManager == null) {
            customMaterialItems = Map.of();
            return;
        }
        Map<String, CustomBazaarItem> found = new LinkedHashMap<>();
        List<String> invalid = new ArrayList<>();
        for (String rawId : customItemManager.getItemIds()) {
            try {
                String id = normalize(rawId);
                ItemStack sample = customItemManager.createItem(id, 1);
                String type = customItemManager.getCustomItemType(sample);
                if (!isAllowedCustomMaterialType(type)) continue;
                if (isBlockedCustomItem(id, type, sample)) {
                    invalid.add(id);
                    continue;
                }
                Material material = sample.getType();
                if (!isTradableMaterial(material) || !id.equals(normalize(customItemManager.getItemId(sample)))) {
                    invalid.add(id);
                    continue;
                }
                String rarity = customItemManager.getCustomItemRarity(sample);
                double guildValue = guildSellValue(id);
                double rarityPrice = rarityDefaultPrice(rarity);
                double startingPrice = Math.max(rarityPrice, guildValue * 2.0D);
                if (id.equals("ancient_dungeon_essence")) startingPrice = 2500.0D;
                if (!Double.isFinite(startingPrice) || startingPrice <= 0.0D) startingPrice = 10.0D;
                found.putIfAbsent(id, new CustomBazaarItem(id, material, sampleName(sample, id),
                        customMaterialCategory(id), startingPrice, guildValue));
            } catch (RuntimeException exception) {
                invalid.add(normalize(rawId));
            }
        }
        Set<String> previousIds = customMaterialItems.keySet();
        customMaterialItems = Map.copyOf(found);
        if (registryReady && dataManager.isMySqlEnabled() && !previousIds.equals(found.keySet())) reloadMarketDataAsync();
        plugin.getLogger().info("[Bazaar] Registered " + found.size() + " custom material items.");
        List<String> newlyInvalid = invalid.stream().distinct().filter(warnedInvalidCustomItems::add).toList();
        if (!newlyInvalid.isEmpty()) {
            String shown = String.join(", ", newlyInvalid.stream().limit(12).toList());
            plugin.getLogger().warning("[Bazaar] Skipped " + newlyInvalid.size() + " invalid/disallowed custom item(s): " + shown
                    + (newlyInvalid.size() > 12 ? ", ..." : ""));
        }
    }

    public List<String> getItemIds() {
        List<String> ids = new ArrayList<>(customMaterialItems.keySet().stream().filter(this::isItemEnabled).toList());
        for (VanillaBazaarRegistry.Entry entry : VanillaBazaarRegistry.entries()) if (isItemEnabled(entry.key())) ids.add(entry.key());
        ids.sort(Comparator.comparing((String id) -> ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', getDisplayName(id))),
                String.CASE_INSENSITIVE_ORDER).thenComparing(String.CASE_INSENSITIVE_ORDER));
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("bazaar.rotation.enabled", false)) return ids;

        List<String> vanillaIds = ids.stream().filter(id -> id.startsWith("vanilla:")).toList();
        List<String> rotatingIds = new ArrayList<>(ids.stream().filter(id -> !id.startsWith("vanilla:")).toList());
        int dailySlots = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("bazaar.rotation.daily-slots", rotatingIds.size()));
        if (dailySlots >= rotatingIds.size()) return ids;

        long seed = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("bazaar.rotation.seed", 13579L);
        LocalDate date = LocalDate.now(ZoneId.systemDefault());
        Collections.shuffle(rotatingIds, new Random(seed + date.toEpochDay()));
        List<String> result = new ArrayList<>(vanillaIds);
        result.addAll(rotatingIds.subList(0, dailySlots));
        result.sort(Comparator.comparing((String id) -> ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', getDisplayName(id))), String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public List<String> getItemIds(String category) {
        String normalizedCategory = normalize(category);
        List<String> ids = new ArrayList<>();
        for (String itemId : getItemIds()) {
            if (normalizedCategory.equals(getCategory(itemId))) ids.add(itemId);
        }
        return ids;
    }

    public List<String> getVanillaItemIds(String subcategory) {
        return VanillaBazaarRegistry.entries(normalize(subcategory)).stream().map(VanillaBazaarRegistry.Entry::key).filter(this::isItemEnabled).toList();
    }

    public List<VanillaBazaarRegistry.Subcategory> getVanillaSubcategories() { return VanillaBazaarRegistry.subcategories(); }

    public List<String> getCategories() {
        return List.of("farming", "mining", "combat", "foraging", "fishing", "dungeon", "event", "vanilla");
    }

    public String getCategory(String itemId) {
        String key = normalize(itemId);
        CustomBazaarItem custom = customMaterialItems.get(key);
        if (custom != null) return custom.category();
        if (VanillaBazaarRegistry.byKey(key).isPresent()) return "vanilla";
        String configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("bazaar.items." + key + ".category", "");
        if (!configured.isBlank()) return normalize(configured);
        BazaarItem item = DEFAULT_ITEMS.get(key);
        if (item != null) return item.category();
        return guessCategory(getMaterial(key));
    }

    public String getCategoryDisplayName(String category) {
        String key = normalize(category);
        String fixed = switch (key) {
            case "farming" -> "&aFarming";
            case "mining" -> "&6Mining";
            case "combat" -> "&cCombat";
            case "foraging" -> "&2Foraging";
            case "fishing" -> "&bFishing";
            case "dungeon" -> "&5Dungeon";
            case "event" -> "&dEvent";
            case "vanilla" -> "&fVanilla";
            default -> null;
        };
        if (fixed != null) return fixed;
        String configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("bazaar.categories." + key + ".name", "");
        if (!configured.isBlank()) return configured;
        BazaarCategory fallback = DEFAULT_CATEGORIES.get(key);
        return fallback == null ? pretty(key) : fallback.name();
    }

    public Material getCategoryIcon(String category) {
        String key = normalize(category);
        Material fixed = switch (key) {
            case "farming" -> Material.WHEAT;
            case "mining" -> Material.DIAMOND;
            case "combat" -> Material.ROTTEN_FLESH;
            case "foraging" -> Material.OAK_LOG;
            case "fishing" -> Material.PRISMARINE_SHARD;
            case "dungeon" -> Material.ECHO_SHARD;
            case "event" -> Material.NETHER_STAR;
            case "vanilla" -> Material.GRASS_BLOCK;
            default -> null;
        };
        if (fixed != null) return fixed;
        try {
            String material = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("bazaar.categories." + key + ".material", "");
            if (!material.isBlank()) return Material.valueOf(material.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
        }
        BazaarCategory fallback = DEFAULT_CATEGORIES.get(key);
        return fallback == null ? Material.CHEST : fallback.icon();
    }

    private List<String> allItemIds() {
        Set<String> ids = new LinkedHashSet<>();
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("bazaar.items");
        if (section != null) ids.addAll(section.getKeys(false));
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("bazaar.replace-defaults", false)) {
            ids.addAll(DEFAULT_ITEMS.keySet());
        }
        ids.addAll(customMaterialItems.keySet());
        return new ArrayList<>(ids);
    }

    public boolean itemExists(String itemId) {
        String key = normalize(itemId);
        return !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection("bazaar.items." + key)
                && !DEFAULT_ITEMS.containsKey(key) && !customMaterialItems.containsKey(key) && VanillaBazaarRegistry.byKey(key).isEmpty();
    }

    public Material getMaterial(String itemId) {
        String key = normalize(itemId);
        CustomBazaarItem custom = customMaterialItems.get(key);
        if (custom != null) return custom.material();
        VanillaBazaarRegistry.Entry vanilla = VanillaBazaarRegistry.byKey(key).orElse(null);
        if (vanilla != null) return vanilla.material();
        try {
            String configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("bazaar.items." + key + ".material", "");
            if (!configured.isBlank()) return Material.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
        }
        BazaarItem item = DEFAULT_ITEMS.get(key);
        return item == null ? Material.STONE : item.material();
    }

    public String getDisplayName(String itemId) {
        String key = normalize(itemId);
        CustomBazaarItem custom = customMaterialItems.get(key);
        if (custom != null) return custom.displayName();
        VanillaBazaarRegistry.Entry vanilla = VanillaBazaarRegistry.byKey(key).orElse(null);
        if (vanilla != null) return "&f" + pretty(vanilla.material().name());
        String configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("bazaar.items." + key + ".name", "");
        if (!configured.isBlank()) return configured;
        BazaarItem item = DEFAULT_ITEMS.get(key);
        return item == null ? pretty(key) : item.name();
    }

    public double getBuyPrice(String itemId) {
        String key = normalize(itemId);
        VanillaBazaarRegistry.Entry vanilla = VanillaBazaarRegistry.byKey(key).orElse(null);
        if (vanilla != null) return vanilla.buyPrice();
        BazaarMarketData market = marketData.get(key);
        CustomBazaarItem custom = customMaterialItems.get(key);
        if (market != null && Double.isFinite(market.currentBuyPrice()) && market.currentBuyPrice() > 0.0D) {
            return custom == null ? market.currentBuyPrice() : Math.max(market.currentBuyPrice(), custom.guildSellValue() + 1.0D);
        }
        if (customMaterialItems.containsKey(key)) return bazaarAlgorithm.defaults(basePrice(key)).buyPrice();
        BazaarItem item = DEFAULT_ITEMS.get(key);
        double fallback = item == null ? 0.0D : item.buyPrice();
        double basePrice = configuredPrice("bazaar.items." + key + ".buy-price", fallback);
        return Math.max(0.0D, Math.rint(basePrice));
    }

    public double getSellPrice(String itemId) {
        String key = normalize(itemId);
        VanillaBazaarRegistry.Entry vanilla = VanillaBazaarRegistry.byKey(key).orElse(null);
        if (vanilla != null) return Math.min(vanilla.sellPrice(), Math.max(1D, vanilla.buyPrice() - 1D));
        BazaarMarketData market = marketData.get(key);
        CustomBazaarItem custom = customMaterialItems.get(key);
        if (market != null && Double.isFinite(market.currentSellPrice()) && market.currentSellPrice() > 0.0D) {
            return custom == null ? market.currentSellPrice() : Math.max(market.currentSellPrice(), custom.guildSellValue());
        }
        if (customMaterialItems.containsKey(key)) return Math.max(customMaterialItems.get(key).guildSellValue(), bazaarAlgorithm.defaults(basePrice(key)).sellPrice());
        BazaarItem item = DEFAULT_ITEMS.get(key);
        double fallback = item == null ? 0.0D : item.sellPrice();
        double configured = configuredPrice("bazaar.items." + key + ".sell-price", fallback);
        return Math.max(0.0D, Math.rint(configured));
    }

    public int countOrders(String itemId, OrderType type) {
        return getOrderIds(itemId, type).size();
    }

    public boolean isMarketCacheLoading() {
        return marketCacheLoading;
    }

    public long getAvailableSupply(String itemId) {
        BazaarMarketData data = marketData.get(normalize(itemId));
        return data == null ? 0L : data.recentSells();
    }

    public long getDemand(String itemId) {
        BazaarMarketData data = marketData.get(normalize(itemId));
        return data == null ? 0L : data.recentBuys();
    }

    public double getMarketPrice(String itemId) {
        return Math.max(0.0D, (getBuyPrice(itemId) + getSellPrice(itemId)) / 2.0D);
    }

    public String getTrend(String itemId) {
        BazaarMarketData data = marketData.get(normalize(itemId));
        if (data == null || data.recentBuys() == data.recentSells()) return "&e▬ Stable";
        return data.recentBuys() > data.recentSells() ? "&a▲ Rising" : "&c▼ Falling";
    }

    public double getInstantBuyCost(String itemId, int amount) {
        if (amount <= 0) return 0.0D;
        String key = normalize(itemId);
        int remaining = amount;
        double total = 0.0D;
        for (String id : sortedOrders(key, OrderType.SELL)) {
            int take = Math.min(remaining, getOrderAmount(id, OrderType.SELL));
            total += take * getOrderPrice(id, OrderType.SELL);
            remaining -= take;
            if (remaining <= 0) break;
        }
        return total + remaining * getBuyPrice(key);
    }

    public double getInstantSellValue(String itemId, int amount) {
        if (amount <= 0) return 0.0D;
        String key = normalize(itemId);
        int remaining = amount;
        double total = 0.0D;
        for (String id : sortedOrders(key, OrderType.BUY)) {
            int fill = Math.min(remaining, getOrderAmount(id, OrderType.BUY));
            total += fill * getOrderPrice(id, OrderType.BUY);
            remaining -= fill;
            if (remaining <= 0) break;
        }
        return total + remaining * getSellPrice(key);
    }

    public boolean hasInventorySpace(Player player, String itemId, int amount) {
        return amount > 0 && canFit(player.getInventory(), normalize(itemId), amount);
    }

    public boolean instantBuy(Player player, String itemId, int amount) {
        if (economyManager.isEcoFrozen(player)) return false;
        String key = normalize(itemId);
        if (itemExists(key) || !canTrade(key)) return false;
        if (amount <= 0) return false;
        if (!canFit(player.getInventory(), key, amount)) return false;

        int remaining = amount;
        double totalCost = 0.0D;
        List<OrderFill> fills = new ArrayList<>();
        for (String id : sortedOrders(key, OrderType.SELL)) {
            int available = getOrderAmount(id, OrderType.SELL);
            double unitPrice = getOrderPrice(id, OrderType.SELL);
            int take = Math.min(remaining, available);
            fills.add(new OrderFill(id, take, unitPrice, getOrderOwner(id, OrderType.SELL)));
            totalCost += take * unitPrice;
            remaining -= take;
            if (remaining <= 0) break;
        }

        if (remaining > 0) {
            double buyPrice = getBuyPrice(key);
            if (buyPrice <= 0) return false;
            totalCost += remaining * buyPrice;
        }

        if (!economyManager.takeCoins(player.getUniqueId(), totalCost, "bazaar instant buy " + key)) return false;
        for (OrderFill fill : fills) {
            reduceOrder(fill.id(), OrderType.SELL, fill.amount());
            if (fill.owner() != null) economyManager.giveCoins(fill.owner(), fill.amount() * fill.unitPrice(), "bazaar sell order fill " + key);
        }
        giveItems(player.getInventory(), key, amount);
        recordMarketTransactionAsync(player.getUniqueId(), key, "BUY", amount, totalCost / amount, totalCost);
        logBazaarTransaction(player, "Instant Buy", key, amount, totalCost);
        return true;
    }

    public boolean instantSell(Player player, String itemId, int amount) {
        if (economyManager.isEcoFrozen(player)) return false;
        String key = normalize(itemId);
        if (itemExists(key) || !canTrade(key)) return false;
        if (amount <= 0) return false;
        int available = countInventory(player, key);
        if (available < amount) return false;

        int remaining = amount;
        double payout = 0.0D;
        List<OrderFill> fills = new ArrayList<>();
        for (String id : sortedOrders(key, OrderType.BUY)) {
            int wanted = getOrderAmount(id, OrderType.BUY);
            double unitPrice = getOrderPrice(id, OrderType.BUY);
            int fill = Math.min(remaining, wanted);
            fills.add(new OrderFill(id, fill, unitPrice, getOrderOwner(id, OrderType.BUY)));
            payout += fill * unitPrice;
            remaining -= fill;
            if (remaining <= 0) break;
        }

        if (remaining > 0) {
            double sellPrice = getSellPrice(key);
            if (sellPrice <= 0 && payout <= 0) return false;
            payout += remaining * sellPrice;
        }

        if (removeItems(player.getInventory(), key, amount)) return false;
        for (OrderFill fill : fills) reduceOrder(fill.id(), OrderType.BUY, fill.amount());
        economyManager.giveCoins(player.getUniqueId(), payout, "bazaar instant sell " + key);
        recordMarketTransactionAsync(player.getUniqueId(), key, "SELL", amount, payout / amount, payout);
        logBazaarTransaction(player, "Instant Sell", key, amount, payout);
        alertBazaarSell(player, key, amount, payout);
        return true;
    }

    public boolean createBuyOrder(Player player, String itemId, int amount, double unitPrice) {
        if (economyManager.isEcoFrozen(player)) return false;
        String key = normalize(itemId);
        if (itemExists(key) || !canTrade(key) || amount <= 0 || unitPrice <= 0) return false;
        double total = amount * unitPrice;
        if (!economyManager.takeCoins(player.getUniqueId(), total, "bazaar buy order " + key)) return false;
        String id = newOrderId();
        String path = orderPath(OrderType.BUY, id);
        dataManager.getData().set(path + ".owner", player.getUniqueId().toString());
        dataManager.getData().set(path + ".item", key);
        dataManager.getData().set(path + ".amount", amount);
        dataManager.getData().set(path + ".price", unitPrice);
        dataManager.save();
        logBazaarTransaction(player, "Buy Order", key, amount, total);
        return true;
    }

    public boolean createSellOrder(Player player, String itemId, int amount, double unitPrice) {
        if (economyManager.isEcoFrozen(player)) return false;
        String key = normalize(itemId);
        if (itemExists(key) || !canTrade(key) || amount <= 0 || unitPrice <= 0) return false;
        amount = Math.min(amount, countInventory(player, key));
        if (amount <= 0 || removeItems(player.getInventory(), key, amount)) return false;
        String id = newOrderId();
        String path = orderPath(OrderType.SELL, id);
        dataManager.getData().set(path + ".owner", player.getUniqueId().toString());
        dataManager.getData().set(path + ".item", key);
        dataManager.getData().set(path + ".amount", amount);
        dataManager.getData().set(path + ".price", unitPrice);
        dataManager.save();
        logBazaarTransaction(player, "Sell Order", key, amount, amount * unitPrice);
        alertBazaarSell(player, key, amount, amount * unitPrice);
        return true;
    }

    public void alertBazaarSell(Player player, String itemId, int amount, double total) {
        if (alertService == null || total < alertService.threshold(SmpAlertService.Type.BAZAAR_SELL)) return;
        alertService.alert(SmpAlertService.Type.BAZAAR_SELL, player.getUniqueId(),
                "&c[SMP ALERT] &e" + player.getName() + " &7sold Bazaar items for &6" + economyManager.format(total) + " coins&7.",
                "**" + player.getName() + "** sold **" + amount + "x " + itemId + "** for **" + economyManager.format(total) + " coins**. Server: `" + alertService.serverId() + "`",
                "player=" + player.getName() + "/" + player.getUniqueId() + " item=" + itemId + " amount=" + amount + " total=" + total + " server=" + alertService.serverId() + " at=" + System.currentTimeMillis());
    }

    public int countInventory(Player player, String itemId) {
        String key = normalize(itemId);
        Material material = getMaterial(key);
        if (!isTradableMaterial(material)) return 0;
        int amount = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (matchesBazaarItem(item, key)) amount += item.getAmount();
        }
        return amount;
    }

    public void stop() {
        if (!shuttingDown.compareAndSet(false, true)) return;
        running.set(false);
        if (marketTask != null) {
            marketTask.cancel();
            marketTask = null;
        }
        for (BukkitTask task : new ArrayList<>(asyncTasks)) task.cancel();
        asyncTasks.clear();
        long deadline = System.currentTimeMillis() + 2000L;
        synchronized (taskMonitor) {
            while (activeTasks.get() > 0 && System.currentTimeMillis() < deadline) {
                try {
                    taskMonitor.wait(Math.min(50L, Math.max(1L, deadline - System.currentTimeMillis())));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public boolean reloadMarketData() {
        if (!lifecycleActive() || !dataManager.isMySqlEnabled()) {
            marketData.clear();
            marketCacheLoading = false;
            return false;
        }
        Map<String, BazaarMarketData> loaded = new LinkedHashMap<>();
        try {
            executeWithConnectionRetry("load market data", () -> {
                loaded.clear();
                try (Connection connection = dataManager.getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                            "SELECT `item_id`, `base_price`, `current_buy_price`, `current_sell_price`, `recent_buys`, `recent_sells`, `total_buys`, `total_sells`, `last_updated` FROM `smp_bazaar_market`")) {
                ensureLifecycle();
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        String itemId = normalize(result.getString("item_id"));
                        if (itemId.isBlank()) continue;
                        loaded.put(itemId, new BazaarMarketData(
                                itemId,
                                Math.max(1.0D, result.getDouble("base_price")),
                                Math.max(1.0D, result.getDouble("current_buy_price")),
                                Math.max(1.0D, result.getDouble("current_sell_price")),
                                Math.max(0L, result.getLong("recent_buys")),
                                Math.max(0L, result.getLong("recent_sells")),
                                Math.max(0L, result.getLong("total_buys")),
                                Math.max(0L, result.getLong("total_sells")),
                                result.getLong("last_updated")
                        ));
                        initializedMarketItemIds.add(itemId);
                    }
                }
                }
                return null;
            });
        } catch (SQLException e) {
            logMarketDatabaseFailure("load market data", e);
            return false;
        }
        if (!lifecycleActive()) return false;
        marketData.clear();
        marketData.putAll(loaded);
        marketCacheLoading = false;
        return true;
    }

    public boolean requestMarketReload() {
        if (!lifecycleActive() || !dataManager.isMySqlEnabled() || !marketReloadRunning.compareAndSet(false, true)) return false;
        marketCacheLoading = true;
        boolean scheduled = scheduleAsync(() -> {
            try {
                if (!lifecycleActive()) return;
                ensureMarketRows();
                if (!lifecycleActive()) return;
                reloadMarketData();
            } finally {
                marketCacheLoading = false;
                marketReloadRunning.set(false);
            }
        });
        if (!scheduled) {
            marketCacheLoading = false;
            marketReloadRunning.set(false);
        }
        return scheduled;
    }

    public boolean requestPriceRecalculation() {
        return lifecycleActive() && dataManager.isMySqlEnabled() && !recalculateRunning.get() && scheduleAsync(this::recalculateMarketPrices);
    }

    public void clearMarketCache() {
        marketData.clear();
        initializedMarketItemIds.clear();
        marketCacheLoading = false;
    }

    public List<String> validateCustomMaterialRegistry() {
        List<String> lines = new ArrayList<>();
        int valid = 0;
        int invalid = 0;
        for (String rawId : customItemManager == null ? List.<String>of() : customItemManager.getItemIds()) {
            String id = normalize(rawId);
            try {
                ItemStack sample = customItemManager.createItem(id, 1);
                String type = customItemManager.getCustomItemType(sample);
                if (!isAllowedCustomMaterialType(type)) continue;
                boolean validEntry = !isBlockedCustomItem(id, type, sample) && isTradableMaterial(sample.getType())
                        && id.equals(normalize(customItemManager.getItemId(sample)));
                if (validEntry) valid++;
                else invalid++;
            } catch (RuntimeException ignored) {
                invalid++;
            }
        }
        lines.add("&aValid Bazaar materials: &f" + valid);
        lines.add((invalid == 0 ? "&a" : "&c") + "Invalid/skipped custom items: &f" + invalid);
        lines.add("&7Active cached market rows: &f" + marketData.size());
        return lines;
    }

    public List<String> cachedMarketDebugLines(int limit) {
        List<String> lines = new ArrayList<>();
        lines.add("&eCached Bazaar market rows: &f" + marketData.size());
        marketData.values().stream().sorted(Comparator.comparing(BazaarMarketData::itemId))
                .limit(Math.max(0, limit)).forEach(row -> lines.add("&7" + row.itemId() + " &8| &aB "
                        + economyManager.format(row.currentBuyPrice()) + " &cS " + economyManager.format(row.currentSellPrice())
                        + " &8| &f" + row.recentBuys() + "/" + row.recentSells()));
        return lines;
    }

    public boolean resetMarketItem(String itemId) {
        if (!lifecycleActive()) return false;
        String key = normalize(itemId);
        if (itemExists(key) || !dataManager.isMySqlEnabled()) return false;
        BazaarAlgorithm.BazaarPrices prices = bazaarAlgorithm.defaults(basePrice(key));
        try {
            executeWithConnectionRetry("reset market item " + key, () -> {
                try (Connection connection = dataManager.getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO `smp_bazaar_market` (`item_id`, `base_price`, `current_buy_price`, `current_sell_price`, `recent_buys`, `recent_sells`, `total_buys`, `total_sells`, `last_updated`) " +
                            "VALUES (?, ?, ?, ?, 0, 0, 0, 0, ?) ON DUPLICATE KEY UPDATE `base_price`=VALUES(`base_price`), `current_buy_price`=VALUES(`current_buy_price`), " +
                            "`current_sell_price`=VALUES(`current_sell_price`), `recent_buys`=0, `recent_sells`=0, `last_updated`=VALUES(`last_updated`)")) {
                statement.setString(1, key);
                statement.setDouble(2, basePrice(key));
                statement.setDouble(3, prices.buyPrice());
                statement.setDouble(4, prices.sellPrice());
                statement.setLong(5, System.currentTimeMillis());
                ensureLifecycle();
                statement.executeUpdate();
                }
                return null;
            });
        } catch (SQLException e) {
            logMarketDatabaseFailure("reset market item " + key, e);
            return false;
        }
        reloadMarketData();
        plugin.getLogger().info("[BazaarMarket] Reset " + key + " to base price " + basePrice(key));
        return true;
    }

    public boolean requestMarketReset(String itemId, Consumer<Boolean> completion) {
        if (!lifecycleActive()) return false;
        return scheduleAsync(() -> {
            boolean reset = resetMarketItem(itemId);
            if (!lifecycleActive() || completion == null) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!lifecycleActive()) return;
                completion.accept(reset);
            });
        });
    }

    public List<String> marketStatusLines(String itemId) {
        String key = normalize(itemId);
        if (itemExists(key)) return List.of("&cUnknown bazaar item: &f" + itemId);
        BazaarMarketData data = marketData.get(key);
        if (data == null) {
            return List.of(
                    "&bBazaar Market: &f" + key,
                    "&7Source: &eSTATIC DEFAULT &8(MariaDB unavailable or not loaded)",
                    "&7Base: &f" + economyManager.format(basePrice(key)),
                    "&7Buy: &f" + economyManager.format(getBuyPrice(key)) + " &7Sell: &f" + economyManager.format(getSellPrice(key))
            );
        }
        return List.of(
                "&bBazaar Market: &f" + key,
                "&7Source: &aDATABASE MARKET",
                "&7Base: &f" + economyManager.format(data.basePrice()),
                "&7Buy: &f" + economyManager.format(data.currentBuyPrice()) + " &7Sell: &f" + economyManager.format(data.currentSellPrice()),
                "&7Recent Buys/Sells: &f" + data.recentBuys() + "&7/&f" + data.recentSells(),
                "&7Total Buys/Sells: &f" + data.totalBuys() + "&7/&f" + data.totalSells(),
                "&7Last Updated: &f" + data.lastUpdated()
        );
    }

    private void startMarketTask() {
        if (marketTask != null) marketTask.cancel();
        shuttingDown.set(false);
        running.set(true);
        if (!dataManager.isMySqlEnabled()) {
            plugin.getLogger().warning("MariaDB bazaar market is unavailable; bazaar prices will use static Java defaults.");
            return;
        }
        marketTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> runGuarded(this::recalculateMarketPrices), 20L * 60L * 5L, 20L * 60L * 5L);
    }

    private void reloadMarketDataAsync() {
        requestMarketReload();
    }

    private synchronized void ensureMarketRows() {
        if (!lifecycleActive() || !dataManager.isMySqlEnabled()) return;
        List<String> missing = getItemIds().stream().map(this::normalize)
                .filter(this::isTradableItemId).filter(id -> !initializedMarketItemIds.contains(id)).toList();
        int inserted = 0;
        for (int start = 0; start < missing.size(); start += 100) {
            if (!lifecycleActive()) return;
            List<String> batch = missing.subList(start, Math.min(start + 100, missing.size()));
            try {
                inserted += insertMarketRowBatch(batch);
                initializedMarketItemIds.addAll(batch);
            } catch (SQLException batchError) {
                if (isConnectionFailure(batchError)) {
                    logMarketDatabaseFailure("initialize market rows", batchError);
                    return;
                }
                for (String itemId : batch) {
                    if (!lifecycleActive()) return;
                    try {
                        inserted += insertMarketRowBatch(List.of(itemId));
                        initializedMarketItemIds.add(itemId);
                    } catch (SQLException itemError) {
                        logMarketDatabaseFailure("initialize market row " + itemId, itemError);
                    }
                }
            }
        }
        if (inserted > 0) plugin.getLogger().info("[BazaarMarket] Initialized " + inserted + " MariaDB market rows.");
    }

    private int insertMarketRowBatch(List<String> itemIds) throws SQLException {
        return executeWithConnectionRetry("initialize market rows", () -> {
            ensureLifecycle();
            try (Connection connection = dataManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT IGNORE INTO `smp_bazaar_market` (`item_id`, `base_price`, `current_buy_price`, `current_sell_price`, `recent_buys`, `recent_sells`, `total_buys`, `total_sells`, `last_updated`) VALUES (?, ?, ?, ?, 0, 0, 0, 0, ?)")) {
                long now = System.currentTimeMillis();
                for (String itemId : itemIds) {
                    double base = basePrice(itemId);
                    BazaarAlgorithm.BazaarPrices prices = bazaarAlgorithm.defaults(base);
                    VanillaBazaarRegistry.Entry vanilla = VanillaBazaarRegistry.byKey(itemId).orElse(null);
                    statement.setString(1, itemId);
                    statement.setDouble(2, base);
                    statement.setDouble(3, vanilla == null ? prices.buyPrice() : vanilla.buyPrice());
                    statement.setDouble(4, vanilla == null ? prices.sellPrice() : vanilla.sellPrice());
                    statement.setLong(5, now);
                    statement.addBatch();
                }
                int inserted = 0;
                ensureLifecycle();
                for (int count : statement.executeBatch()) if (count > 0) inserted += count;
                return inserted;
            }
        });
    }

    private void recalculateMarketPrices() {
        if (!lifecycleActive() || !dataManager.isMySqlEnabled()) return;
        if (!recalculateRunning.compareAndSet(false, true)) return;
        try {
        List<BazaarMarketData> rows = new ArrayList<>();
        try {
            executeWithConnectionRetry("read market rows for recalculation", () -> {
            ensureLifecycle();
            rows.clear();
            try (Connection connection = dataManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                    "SELECT `item_id`, `base_price`, `current_buy_price`, `current_sell_price`, `recent_buys`, `recent_sells`, `total_buys`, `total_sells`, `last_updated` FROM `smp_bazaar_market`")) {
                ensureLifecycle();
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        rows.add(new BazaarMarketData(
                                normalize(result.getString("item_id")),
                                Math.max(1.0D, result.getDouble("base_price")),
                                Math.max(1.0D, result.getDouble("current_buy_price")),
                                Math.max(1.0D, result.getDouble("current_sell_price")),
                                Math.max(0L, result.getLong("recent_buys")),
                                Math.max(0L, result.getLong("recent_sells")),
                                Math.max(0L, result.getLong("total_buys")),
                                Math.max(0L, result.getLong("total_sells")),
                                result.getLong("last_updated")
                        ));
                    }
                }
            }
            return null;
            });
        } catch (SQLException e) {
            logMarketDatabaseFailure("read market rows for recalculation", e);
            return;
        }

        long now = System.currentTimeMillis();
        if (!lifecycleActive()) return;
        try {
            executeWithConnectionRetry("update market prices", () -> {
            ensureLifecycle();
            try (Connection connection = dataManager.getConnection();
                 PreparedStatement update = connection.prepareStatement(
                    "UPDATE `smp_bazaar_market` SET `base_price`=?, `current_buy_price`=?, `current_sell_price`=?, `recent_buys`=0, `recent_sells`=0, `last_updated`=? WHERE `item_id`=?")) {
                for (BazaarMarketData row : rows) {
                    if (!lifecycleActive()) return null;
                    if (row.itemId().isBlank() || itemExists(row.itemId())) continue;
                    try {
                        double base = basePrice(row.itemId());
                        double previousMarketPrice = ((row.currentBuyPrice() / 1.03D) + (row.currentSellPrice() / 0.97D)) / 2.0D;
                        boolean correctedInvalidValue = !Double.isFinite(base) || !Double.isFinite(previousMarketPrice);
                        BazaarAlgorithm.BazaarPrices prices = bazaarAlgorithm.recalculate(base, previousMarketPrice, row.recentBuys(), row.recentSells());
                        if (correctedInvalidValue && bazaarDebugEnabled()) {
                            plugin.getLogger().warning("[BazaarMarket] Corrected an invalid price for " + row.itemId() + ".");
                        }
                        if (row.currentBuyPrice() > 0.0D && prices.buyPrice() / row.currentBuyPrice() > 1.20D) {
                            plugin.getLogger().warning("[BazaarMarket] Suspicious upward price move for " + row.itemId() + ": " + row.currentBuyPrice() + " -> " + prices.buyPrice());
                        }
                        update.setDouble(1, Double.isFinite(base) ? Math.max(1.0D, base) : 1.0D);
                        update.setDouble(2, prices.buyPrice());
                        update.setDouble(3, prices.sellPrice());
                        update.setLong(4, now);
                        update.setString(5, row.itemId());
                        update.addBatch();
                    } catch (RuntimeException e) {
                        if (bazaarDebugEnabled()) {
                            plugin.getLogger().warning("[BazaarMarket] Skipped invalid market row for " + row.itemId() + ": " + e.getMessage());
                        }
                    }
                }
                ensureLifecycle();
                update.executeBatch();
            }
            return null;
            });
        } catch (SQLException e) {
            logMarketDatabaseFailure("update market prices", e);
            return;
        }
        if (lifecycleActive()) reloadMarketData();
        } finally {
            recalculateRunning.set(false);
        }
    }

    private boolean bazaarDebugEnabled() {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("bazaar.debug", false);
    }

    private void logMarketDatabaseFailure(String section, SQLException error) {
        if (!lifecycleActive()) return;
        long now = System.currentTimeMillis();
        if (now < nextDatabaseErrorLogAt) return;
        nextDatabaseErrorLogAt = now + 60_000L;
        plugin.getLogger().warning("[SMP] MariaDB bazaar failure during " + section + ". Will retry next run. SQLState="
                + error.getSQLState() + " Reason=" + error.getMessage());
        if (bazaarDebugEnabled()) plugin.getLogger().log(Level.WARNING, "[SMP] Full bazaar database failure", error);
    }

    private boolean isConnectionFailure(SQLException error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("08")) return true;
            if (cause.getClass().getSimpleName().equals("CommunicationsException")) return true;
        }
        return false;
    }

    private <T> T executeWithConnectionRetry(String operation, SqlOperation<T> action) throws SQLException {
        ensureLifecycle();
        try {
            return action.execute();
        } catch (SQLException first) {
            if (!lifecycleActive()) throw new BazaarShutdownException();
            if (!isConnectionFailure(first)) throw first;
            ensureLifecycle();
            return action.execute();
        }
    }

    private boolean lifecycleActive() {
        return plugin.isEnabled() && running.get() && !shuttingDown.get();
    }

    private void ensureLifecycle() throws SQLException {
        if (!lifecycleActive()) throw new BazaarShutdownException();
    }

    private boolean scheduleAsync(Runnable action) {
        if (!lifecycleActive()) return false;
        AtomicReference<BukkitTask> reference = new AtomicReference<>();
        try {
            BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    runGuarded(action);
                } finally {
                    BukkitTask current = reference.get();
                    if (current != null) asyncTasks.remove(current);
                }
            });
            reference.set(task);
            asyncTasks.add(task);
            if (!lifecycleActive()) {
                task.cancel();
                asyncTasks.remove(task);
                return false;
            }
            return true;
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    private void runGuarded(Runnable action) {
        if (!lifecycleActive()) return;
        activeTasks.incrementAndGet();
        try {
            if (lifecycleActive()) action.run();
        } catch (RuntimeException error) {
            if (lifecycleActive()) throw error;
        } finally {
            activeTasks.decrementAndGet();
            synchronized (taskMonitor) {
                taskMonitor.notifyAll();
            }
        }
    }

    public List<String> lifecycleValidationLines() {
        asyncTasks.removeIf(task -> task.isCancelled() || !Bukkit.getScheduler().isQueued(task.getTaskId()) && !Bukkit.getScheduler().isCurrentlyRunning(task.getTaskId()));
        int periodic = marketTask != null && !marketTask.isCancelled() ? 1 : 0;
        return List.of(
                "&7Bazaar running: " + (running.get() ? "&atrue" : "&cfalse"),
                "&7Bazaar shutting down: " + (shuttingDown.get() ? "&etrue" : "&afalse"),
                "&7Market recalculation tasks: " + (periodic == 1 ? "&a1" : "&c" + periodic),
                "&7Duplicate Bazaar periodic tasks: &a0",
                "&7Tracked one-shot async tasks: &f" + asyncTasks.size(),
                "&7Active Bazaar DB tasks: &f" + activeTasks.get(),
                "&7Shutdown-aware retries: &atrue",
                "&7DB writes fenced by lifecycle: &atrue",
                "&7Bazaar stops before DataManager close: &atrue",
                "&7Sync scheduling after disable: &a blocked"
        );
    }

    @FunctionalInterface
    private interface SqlOperation<T> {
        T execute() throws SQLException;
    }

    private static final class BazaarShutdownException extends SQLException {
    }

    private void recordMarketTransactionAsync(UUID playerId, String itemId, String action, int amount, double unitPrice, double totalPrice) {
        if (!lifecycleActive()) return;
        String key = normalize(itemId);
        action = action == null ? "" : action.toUpperCase(Locale.ROOT);
        if (!action.equals("BUY") && !action.equals("SELL")) return;
        if (amount <= 0 || unitPrice <= 0.0D || totalPrice <= 0.0D) return;
        String cacheAction = action;
        double cacheBase = basePrice(key);
        BazaarAlgorithm.BazaarPrices cacheDefaults = bazaarAlgorithm.defaults(cacheBase);
        marketData.compute(key, (ignored, current) -> {
            BazaarMarketData row = current == null
                    ? new BazaarMarketData(key, cacheBase, cacheDefaults.buyPrice(), cacheDefaults.sellPrice(), 0L, 0L, 0L, 0L, System.currentTimeMillis())
                    : current;
            return new BazaarMarketData(row.itemId(), row.basePrice(), row.currentBuyPrice(), row.currentSellPrice(),
                    row.recentBuys() + (cacheAction.equals("BUY") ? amount : 0L),
                    row.recentSells() + (cacheAction.equals("SELL") ? amount : 0L),
                    row.totalBuys() + (cacheAction.equals("BUY") ? amount : 0L),
                    row.totalSells() + (cacheAction.equals("SELL") ? amount : 0L), System.currentTimeMillis());
        });
        if (!lifecycleActive() || !dataManager.isMySqlEnabled()) return;
        String normalizedAction = action;
        scheduleAsync(() -> recordMarketTransaction(playerId, key, normalizedAction, amount, unitPrice, totalPrice));
    }

    private void recordMarketTransaction(UUID playerId, String itemId, String action, int amount, double unitPrice, double totalPrice) {
        if (!lifecycleActive()) return;
        String column = action.equals("BUY") ? "buys" : "sells";
        try {
            executeWithConnectionRetry("record transaction for " + itemId, () -> {
            ensureLifecycle();
            try (Connection connection = dataManager.getConnection()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO `smp_bazaar_transactions` (`player_uuid`, `item_id`, `action`, `amount`, `unit_price`, `total_price`) VALUES (?, ?, ?, ?, ?, ?)")) {
                insert.setString(1, playerId.toString());
                insert.setString(2, itemId);
                insert.setString(3, action);
                insert.setInt(4, amount);
                insert.setDouble(5, Math.max(1.0D, Math.rint(unitPrice)));
                insert.setDouble(6, Math.max(1.0D, Math.rint(totalPrice)));
                ensureLifecycle();
                insert.executeUpdate();
            }

                try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE `smp_bazaar_market` SET `recent_" + column + "`=`recent_" + column + "`+?, `total_" + column + "`=`total_" + column + "`+? WHERE `item_id`=?")) {
                update.setInt(1, amount);
                update.setInt(2, amount);
                update.setString(3, itemId);
                ensureLifecycle();
                update.executeUpdate();
            }
            }
            return null;
            });
        } catch (SQLException e) {
            logMarketDatabaseFailure("record transaction for " + itemId, e);
        }
    }

    private boolean canTrade(String itemId) {
        return isBazaarEnabled() && isItemEnabled(itemId) && isTradableItemId(itemId);
    }

    private boolean isBazaarEnabled() {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("bazaar.enabled", true)
                && !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("system.bazaar.disabled", false);
    }

    private boolean isItemEnabled(String itemId) {
        String key = normalize(itemId);
        String category = getCategory(key);
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("bazaar.items." + key + ".enabled", true)
                && !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("bazaar.item." + key + ".disabled", false)
                && !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("bazaar.category." + category + ".disabled", false);
    }

    private double basePrice(String itemId) {
        String key = normalize(itemId);
        VanillaBazaarRegistry.Entry vanilla = VanillaBazaarRegistry.byKey(key).orElse(null);
        if (vanilla != null) return vanilla.buyPrice();
        CustomBazaarItem custom = customMaterialItems.get(key);
        if (custom != null) {
            double price = algorithmPrice(key, custom.startingPrice());
            return Math.max(Math.max(1.0D, custom.guildSellValue() * 2.0D), Double.isFinite(price) ? price : custom.startingPrice());
        }
        BazaarItem item = DEFAULT_ITEMS.get(key);
        double fallback = item == null ? defaultBuyPrice(getMaterial(key)) : item.buyPrice();
        return Math.max(1.0D, Math.rint(configuredPrice("bazaar.items." + key + ".buy-price", fallback)));
    }

    private List<String> sortedOrders(String itemId, OrderType type) {
        List<String> ids = getOrderIds(itemId, type);
        Comparator<String> comparator = Comparator.comparingDouble((String id) -> getOrderPrice(id, type));
        if (type == OrderType.BUY) comparator = comparator.reversed();
        ids.sort(comparator);
        return ids;
    }

    private List<String> getOrderIds(String itemId, OrderType type) {
        ConfigurationSection section = dataManager.getData().getConfigurationSection("bazaar." + type.key());
        if (section == null) return new ArrayList<>();
        String key = normalize(itemId);
        List<String> ids = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            if (key.equalsIgnoreCase(section.getString(id + ".item"))) ids.add(id);
        }
        return ids;
    }

    private UUID getOrderOwner(String id, OrderType type) {
        try {
            return UUID.fromString(dataManager.getData().getString(orderPath(type, id) + ".owner", ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private int getOrderAmount(String id, OrderType type) {
        return dataManager.getData().getInt(orderPath(type, id) + ".amount", 0);
    }

    private double getOrderPrice(String id, OrderType type) {
        return dataManager.getData().getDouble(orderPath(type, id) + ".price", 0.0D);
    }

    private void reduceOrder(String id, OrderType type, int amount) {
        String path = orderPath(type, id);
        int remaining = Math.max(0, dataManager.getData().getInt(path + ".amount", 0) - amount);
        dataManager.getData().set(path + ".amount", remaining);
        if (remaining == 0) dataManager.getData().set(path, null);
        dataManager.save();
    }

    private boolean removeItems(Inventory inventory, String itemId, int amount) {
        Material material = getMaterial(itemId);
        if (!isTradableMaterial(material)) return true;
        int remaining = amount;
        for (ItemStack item : inventory.getStorageContents()) {
            if (matchesBazaarItem(item, itemId)) remaining -= item.getAmount();
            if (remaining <= 0) break;
        }
        if (remaining > 0) return true;

        remaining = amount;
        ItemStack[] contents = inventory.getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (!matchesBazaarItem(item, itemId)) continue;
            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            contents[slot] = item.getAmount() <= 0 ? null : item;
            remaining -= take;
            if (remaining <= 0) break;
        }
        inventory.setStorageContents(contents);
        return false;
    }

    private boolean canFit(Inventory inventory, String itemId, int amount) {
        ItemStack template = createItemStack(itemId, 1);
        Material material = template.getType();
        if (!isTradableMaterial(material)) return false;
        int remaining = amount;
        int maxStackSize = template.getMaxStackSize();
        for (ItemStack content : inventory.getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                remaining -= maxStackSize;
            } else if (content.isSimilar(template)) {
                remaining -= Math.max(0, content.getMaxStackSize() - content.getAmount());
            }
            if (remaining <= 0) return true;
        }
        return false;
    }

    private void logBazaarTransaction(Player player, String action, String itemId, int amount, double totalValue) {
        if (discordWebhookManager == null) return;

        String itemName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', getDisplayName(itemId)));
        double unitPrice = amount <= 0 ? totalValue : totalValue / amount;
        String description = String.join("\n",
                "**Player:** " + player.getName(),
                "**Action:** " + action,
                "**Item:** " + itemName + " (`" + itemId + "`)",
                "**Amount:** " + amount,
                "**Unit Price:** " + economyManager.format(unitPrice) + " coins",
                "**Total:** " + economyManager.format(totalValue) + " coins");

        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("bazaar.discord.all.enabled", false)) {
            discordWebhookManager.sendBazaarAllLog(action + " - " + itemName, description);
        }

        double threshold = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("bazaar.discord.high.minimum-total", 100000.0D);
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("bazaar.discord.high.enabled", false) && totalValue >= threshold) {
            discordWebhookManager.sendBazaarHighLog("High Value " + action + " - " + itemName, description);
        }
    }

    public ItemStack createItemStack(String itemId, int amount) {
        String key = normalize(itemId);
        if (customMaterialItems.containsKey(key) && customItemManager != null) {
            return customItemManager.createItem(key, Math.max(1, amount));
        }
        Material material = getMaterial(key);
        if (!isTradableMaterial(material)) material = Material.STONE;
        ItemStack stack = new ItemStack(material, Math.clamp(amount, 1, material.getMaxStackSize()));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String customName = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("bazaar.items." + key + ".name", "");
            boolean hasCustomData = false;
            if (!customName.isBlank()) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', customName));
                hasCustomData = true;
            }

            if (meta instanceof SkullMeta skullMeta) {
                String skin = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("bazaar.items." + key + ".skin", "");
                if (!skin.isBlank() && applySkin(skullMeta, key, skin)) hasCustomData = true;
            }

            if (meta instanceof BlockStateMeta blockStateMeta && material == Material.SPAWNER) {
                String mob = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("bazaar.items." + key + ".mob", "");
                if (!mob.isBlank() && applySpawnerMob(blockStateMeta, mob)) hasCustomData = true;
            }

            if (hasCustomData) stack.setItemMeta(meta);
        }
        return stack;
    }

    private void giveItems(Inventory inventory, String itemId, int amount) {
        int remaining = amount;
        Material material = getMaterial(itemId);
        if (!isTradableMaterial(material)) return;
        while (remaining > 0) {
            int stackAmount = Math.min(material.getMaxStackSize(), remaining);
            inventory.addItem(createItemStack(itemId, stackAmount));
            remaining -= stackAmount;
        }
    }

    private boolean matchesBazaarItem(ItemStack item, String itemId) {
        if (item == null || item.getType().isAir()) return false;
        String key = normalize(itemId);
        if (customMaterialItems.containsKey(key)) {
            return customItemManager != null && key.equals(normalize(customItemManager.getItemId(item)))
                    && customItemManager.getFlag(item, "tradeable") && !customItemManager.getFlag(item, "soulbound");
        }
        if (VanillaBazaarRegistry.byKey(key).isPresent()) return item.getType() == getMaterial(key) && isSafeVanillaBazaarItem(item);
        return customItemManager != null && customItemManager.getItemId(item) != null ? false : item.getType() == getMaterial(key);
    }

    public boolean isSafeVanillaBazaarItem(ItemStack item) {
        if (item == null || item.getType().isAir() || VanillaBazaarRegistry.byMaterial(item.getType()).isEmpty()) return false;
        if (customItemManager != null && customItemManager.getItemId(item) != null) return false;
        if (!item.hasItemMeta()) return true;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().getKeys().isEmpty() || meta.hasDisplayName() || meta.hasLore()
                || meta.hasEnchants() || meta.hasAttributeModifiers() || meta.hasCustomModelData()) return false;
        if (meta instanceof BlockStateMeta blockMeta) {
            if (blockMeta.getBlockState() instanceof Container container && !container.getInventory().isEmpty()) return false;
            if (blockMeta.getBlockState() instanceof Beehive beehive && beehive.getEntityCount() > 0) return false;
        }
        return true;
    }

    private boolean applySkin(SkullMeta meta, String key, String skin) {
        try {
            String url = decodeSkinUrl(skin);
            if (url == null || url.isBlank()) return false;
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.nameUUIDFromBytes(("bazaar-" + key).getBytes(StandardCharsets.UTF_8)), null);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(url));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String decodeSkinUrl(String skin) {
        try {
            String decoded = new String(Base64.getDecoder().decode(skin), StandardCharsets.UTF_8);
            String marker = "\"url\":\"";
            int start = decoded.indexOf(marker);
            if (start < 0) return null;
            start += marker.length();
            int end = decoded.indexOf("\"", start);
            if (end <= start) return null;
            return decoded.substring(start, end).replace("\\/", "/");
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean applySpawnerMob(BlockStateMeta meta, String mob) {
        try {
            EntityType type = EntityType.valueOf(mob.toUpperCase(Locale.ROOT));
            if (!(meta.getBlockState() instanceof CreatureSpawner spawner)) return false;
            spawner.setSpawnedType(type);
            meta.setBlockState(spawner);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String orderPath(OrderType type, String id) {
        return "bazaar." + type.key() + "." + id;
    }

    private String newOrderId() {
        return String.valueOf(System.currentTimeMillis());
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "Item";
        text = text.replace('_', ' ');
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private static void registerCategory(String id, String name, Material icon) {
        DEFAULT_CATEGORIES.put(id, new BazaarCategory(name, icon));
    }

    private static void register(String category, double buyPrice, Material... materials) {
        for (Material material : materials) {
            if (isBazaarMaterial(material)) continue;
            String id = material.name().toLowerCase(Locale.ROOT);
            DEFAULT_ITEMS.put(id, new BazaarItem(material, category, "&f" + prettyMaterial(material.name()), buyPrice, Math.max(1.0D, Math.round(buyPrice * 0.4D))));
        }
    }

    private static void registerAllEligibleMaterials() {
        for (Material material : Material.values()) {
            if (isBazaarMaterial(material)) continue;
            String id = material.name().toLowerCase(Locale.ROOT);
            DEFAULT_ITEMS.putIfAbsent(id, new BazaarItem(material, guessCategoryName(material), "&f" + prettyMaterial(material.name()), defaultBuyPrice(material), defaultSellPrice(material)));
        }
    }

    private static boolean isBazaarMaterial(Material material) {
        if (material == null || !material.isItem() || material.isAir()) return true;
        String name = material.name();
        if (EXCLUDED_MATERIALS.contains(name)) return true;
        if (name.endsWith("_SPAWN_EGG")) return true;
        if (name.endsWith("_COMMAND_BLOCK")) return true;
        if (name.contains("POTTED_")) return true;
        if (name.endsWith("_WALL_SIGN") || name.endsWith("_WALL_HANGING_SIGN")
                || name.endsWith("_WALL_BANNER") || name.endsWith("_WALL_TORCH")
                || name.endsWith("_WALL_FAN") || name.endsWith("_WALL_HEAD")
                || name.endsWith("_WALL_SKULL")) return true;
        if (name.startsWith("LEGACY_")) return true;
        return false;
    }

    private boolean isTradableItemId(String itemId) {
        return isTradableMaterial(getMaterial(itemId));
    }

    private boolean isTradableMaterial(Material material) {
        return material != null && !material.isAir() && !isBazaarMaterial(material) && canCreateItemStack(material);
    }

    private boolean canCreateItemStack(Material material) {
        try {
            new ItemStack(material, 1);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static double defaultBuyPrice(Material material) {
        int maxStack = Math.max(1, material.getMaxStackSize());
        String category = guessCategoryName(material);
        double base = switch (category) {
            case "tools_and_armor" -> 220.0D;
            case "combat" -> 140.0D;
            case "music_and_patterns" -> 260.0D;
            case "transportation" -> 90.0D;
            case "redstone" -> 48.0D;
            case "mining" -> 24.0D;
            case "woodcutting" -> 18.0D;
            case "building_blocks", "decorations" -> 32.0D;
            default -> 10.0D;
        };
        return maxStack == 1 ? base * 4.0D : base;
    }

    private static double defaultSellPrice(Material material) {
        return Math.max(1.0D, Math.round(defaultBuyPrice(material) * 0.4D));
    }

    private double configuredPrice(String path, double fallback) {
        Object value = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).get(path);
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim().replace(',', '.'));
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private double algorithmPrice(String itemId, double basePrice) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("bazaar.pricing.algorithm.enabled", true)) return basePrice;

        Material material = getMaterial(itemId);
        String category = getCategory(itemId);
        double categoryMultiplier = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("bazaar.pricing.algorithm.category-multipliers." + category, 1.0D);
        double stackMultiplier = material.getMaxStackSize() == 1 ? 1.35D : 1.0D;
        double traitMultiplier = itemTraitMultiplier(material);
        double varianceMultiplier = deterministicVariance(itemId);

        double minBuyPrice = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("bazaar.pricing.algorithm.min-buy-price", 0.05D);
        return roundPrice(Math.max(minBuyPrice, basePrice * categoryMultiplier * stackMultiplier * traitMultiplier * varianceMultiplier));
    }

    private double itemTraitMultiplier(Material material) {
        String name = material.name().toLowerCase(Locale.ROOT);
        double multiplier = 1.0D;
        if (name.contains("netherite")) multiplier *= 2.4D;
        else if (name.contains("diamond")) multiplier *= 1.75D;
        else if (name.contains("emerald")) multiplier *= 1.45D;
        else if (name.contains("gold")) multiplier *= 1.25D;
        else if (name.contains("iron")) multiplier *= 1.12D;

        if (name.endsWith("_ore")) multiplier *= 1.3D;
        if (name.endsWith("_block")) multiplier *= 1.18D;
        if (name.contains("spawner") || name.endsWith("_spawn_egg")) multiplier *= 1.5D;
        if (name.contains("template") || name.contains("disc") || name.contains("sherd")) multiplier *= 1.35D;
        if (name.contains("head") || name.contains("skull")) multiplier *= 1.2D;
        return multiplier;
    }

    private double deterministicVariance(String itemId) {
        int percent = Math.clamp(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("bazaar.pricing.algorithm.variance-percent", 18), 0, 90);
        if (percent == 0) return 1.0D;

        int bucket = Math.floorMod(normalize(itemId).hashCode(), 2001);
        double normalized = (bucket / 1000.0D) - 1.0D;
        return 1.0D + (normalized * (percent / 100.0D));
    }

    private double roundPrice(double price) {
        double increment = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("bazaar.pricing.algorithm.round-to", 0.05D);
        if (increment <= 0.0D) return price;
        return Math.round(price / increment) * increment;
    }

    private boolean isAllowedCustomMaterialType(String type) {
        String normalized = normalize(type).toUpperCase(Locale.ROOT);
        return Set.of("MATERIAL", "COLLECTION_MATERIAL", "RECIPE_MATERIAL", "DUNGEON_MATERIAL",
                "MONSTER_MATERIAL", "FISHING_MATERIAL", "FARMING_MATERIAL", "MINING_MATERIAL",
                "FORAGING_MATERIAL", "MAGIC_MATERIAL", "EVENT_MATERIAL", "MISC_MATERIAL").contains(normalized);
    }

    private boolean isBlockedCustomItem(String id, String type, ItemStack sample) {
        String normalized = normalize(type).toUpperCase(Locale.ROOT);
        if (Set.of("WEAPON", "SWORD", "DAGGER", "KATANA", "BOW", "WAND", "STAFF", "ARMOR", "TOOL",
                "RING", "CHARM", "NECKLACE", "POTION", "FOOD", "FISHING_BAIT", "BAIT", "TOKEN",
                "FURNITURE", "COSMETIC", "LOOT_BOX", "QUEST_ITEM").contains(normalized)) return true;
        String key = normalize(id);
        boolean testId = key.equals("test") || key.startsWith("test_") || key.endsWith("_test") || key.contains("_test_");
        if (testId || containsAny(key, "admin", "debug", "quest", "cosmetic", "furniture")) return true;
        return customItemManager != null && (!customItemManager.getFlag(sample, "tradeable") || customItemManager.getFlag(sample, "soulbound"));
    }

    private String customMaterialCategory(String id) {
        String base = "custom-items.items." + id;
        String collection = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + ".collection.category", "");
        String recipe = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + ".recipe-material.category", "");
        String configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + ".category", "");
        String type = normalize(customItemManagerType(id)).toUpperCase(Locale.ROOT);
        if (type.equals("FARMING_MATERIAL")) return "farming";
        if (type.equals("MINING_MATERIAL")) return "mining";
        if (type.equals("MONSTER_MATERIAL")) return "combat";
        if (type.equals("FORAGING_MATERIAL")) return "foraging";
        if (type.equals("FISHING_MATERIAL")) return "fishing";
        if (type.equals("DUNGEON_MATERIAL")) return "dungeon";
        if (type.equals("MAGIC_MATERIAL")) return "misc";
        if (type.equals("EVENT_MATERIAL")) return "event";
        String hints = (collection + " " + recipe + " " + configured + " " + id).toLowerCase(Locale.ROOT);
        if (containsAny(hints, "mining", "ore", "gem", "stone", "crystal", "mithril", "titanium")) return "mining";
        if (containsAny(hints, "farming", "crop", "seed", "soil", "harvest", "wheat", "plant")) return "farming";
        if (containsAny(hints, "foraging", "wood", "bark", "resin", "root", "leaf", "vine")) return "foraging";
        if (containsAny(hints, "fishing", "fish", "sea", "coral", "kelp", "pearl", "angler")) return "fishing";
        if (containsAny(hints, "dungeon", "boss", "catacomb", "crypt")) return "dungeon";
        if (containsAny(hints, "event", "festival")) return "event";
        if (containsAny(hints, "combat", "monster", "mob", "fang", "bone", "venom", "blood", "slime", "magma")) return "combat";
        if (type.equals("RECIPE_MATERIAL") || type.equals("MISC_MATERIAL")) {
            if (containsAny(hints, "dust", "ingot", "ore", "crystal", "stone", "metal")) return "mining";
            if (containsAny(hints, "herb", "seed", "crop", "harvest", "bread", "honey")) return "farming";
            if (containsAny(hints, "wood", "bamboo", "leaf", "bark", "forest")) return "foraging";
            if (containsAny(hints, "fish", "sea", "pearl", "scale", "coral")) return "fishing";
            if (containsAny(hints, "dungeon", "royal", "ancient", "void", "shadow")) return "dungeon";
            if (containsAny(hints, "event", "festival", "token")) return "event";
            return "combat";
        }
        return "combat";
    }

    private String customItemManagerType(String id) {
        if (customItemManager == null) return "";
        return customItemManager.getCustomItemType(customItemManager.createItem(id, 1));
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    private double guildSellValue(String id) {
        double value = Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getDouble("custom-items.items." + id + ".sell-price", 0.0D));
        ConfigurationSection shops = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("shops");
        if (shops == null) return value;
        for (String shop : shops.getKeys(false)) {
            ConfigurationSection items = shops.getConfigurationSection(shop + ".items");
            if (items == null) continue;
            for (String entry : items.getKeys(false)) {
                String path = entry + ".";
                if (id.equalsIgnoreCase(items.getString(path + "custom-item", ""))) value = Math.max(value, items.getDouble(path + "sell-price", 0.0D));
            }
        }
        return value;
    }

    private double rarityDefaultPrice(String rarity) {
        return switch (normalize(rarity).toUpperCase(Locale.ROOT)) {
            case "UNCOMMON" -> 50.0D;
            case "RARE" -> 250.0D;
            case "EPIC" -> 1_000.0D;
            case "LEGENDARY" -> 5_000.0D;
            case "MYTHIC" -> 15_000.0D;
            case "SPECIAL", "DIVINE" -> 25_000.0D;
            default -> 10.0D;
        };
    }

    private String sampleName(ItemStack sample, String id) {
        if (sample.hasItemMeta() && sample.getItemMeta().hasDisplayName()) return sample.getItemMeta().getDisplayName();
        return pretty(id);
    }

    private static String prettyMaterial(String text) {
        String[] parts = text.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? "Item" : builder.toString();
    }

    private String guessCategory(Material material) {
        return guessCategoryName(material);
    }

    private static String guessCategoryName(Material material) {
        String name = material.name().toLowerCase(Locale.ROOT);
        if (name.endsWith("_log") || name.endsWith("_wood") || name.contains("_planks") || name.contains("_sapling")
                || name.contains("_leaves") || name.contains("_stem") || name.contains("_hyphae")) return "woodcutting";
        if (name.contains("ore") || name.contains("stone") || name.contains("tuff") || name.contains("copper")
                || name.contains("deepslate") || name.contains("netherrack") || name.contains("basalt")
                || name.contains("blackstone") || name.contains("granite") || name.contains("diorite")
                || name.contains("andesite") || name.contains("sandstone") || name.contains("quartz")) return "mining";
        if (name.contains("sword") || name.contains("mace") || name.contains("charge") || name.contains("rod")
                || name.contains("trident") || name.contains("bow") || name.contains("arrow") || name.contains("shield")) return "combat";
        if (name.contains("helmet") || name.contains("chestplate") || name.contains("leggings") || name.contains("boots")
                || name.contains("pickaxe") || name.contains("axe") || name.contains("shovel") || name.contains("hoe")
                || name.contains("horse_armor") || name.contains("elytra")) return "tools_and_armor";
        if (name.contains("rail") || name.contains("minecart") || name.contains("boat") || name.contains("saddle")
                || name.contains("harness") || name.contains("bundle")) return "transportation";
        if (name.contains("redstone") || name.contains("repeater") || name.contains("comparator") || name.contains("piston")
                || name.contains("observer") || name.contains("dispenser") || name.contains("dropper") || name.contains("hopper")
                || name.contains("pressure_plate") || name.contains("button") || name.contains("lever")) return "redstone";
        if (name.contains("disc") || name.contains("pattern") || name.contains("sherd") || name.contains("template")) return "music_and_patterns";
        if (name.contains("glass") || name.contains("terracotta") || name.contains("concrete") || name.contains("wool")
                || name.contains("brick") || name.contains("slab") || name.contains("stairs") || name.contains("wall")) return "building_blocks";
        if (name.contains("flower") || name.contains("banner") || name.contains("bed") || name.contains("carpet")
                || name.contains("candle") || name.contains("painting") || name.contains("pot") || name.contains("shelf")
                || name.contains("lantern") || name.contains("torch") || name.contains("head") || name.contains("skull")) return "decorations";
        return "farming";
    }

    private record BazaarCategory(String name, Material icon) {
    }

    private record BazaarItem(Material material, String category, String name, double buyPrice, double sellPrice) {
    }

    private record BazaarMarketData(String itemId, double basePrice, double currentBuyPrice, double currentSellPrice,
                                    long recentBuys, long recentSells, long totalBuys, long totalSells, long lastUpdated) {
    }

    private record CustomBazaarItem(String id, Material material, String displayName, String category,
                                    double startingPrice, double guildSellValue) {}

    public enum OrderType {
        BUY("buy-orders"),
        SELL("sell-orders");

        private final String key;

        OrderType(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private record OrderFill(String id, int amount, double unitPrice, UUID owner) {
    }
}
