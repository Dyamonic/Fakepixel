package club.abelix.games.fakevival.managers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.abilities.AbilityContext;
import club.abelix.games.fakevival.abilities.AbilityTrigger;
import club.abelix.games.fakevival.abilities.CustomAbilityRegistry;
import club.abelix.games.fakevival.abilities.CustomItemAbility;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.mmo.MMOEnchantStorage;
import club.abelix.games.fakevival.mmo.MMODamageSourceTracker;
import club.abelix.games.fakevival.mmo.MMODamageSourceType;
import club.abelix.games.fakevival.mmo.MMOItemType;
import club.abelix.games.fakevival.mmo.MMOItemValueService;
import club.abelix.games.fakevival.mmo.MMOLoreBuilder;
import club.abelix.games.fakevival.mmo.MMOStatMap;
import club.abelix.games.fakevival.mmo.MMOStatStorage;
import club.abelix.games.fakevival.mmo.MMOStatType;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.VendorLoreSanitizer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class CustomItemManager {
    private static final Gson GSON = new Gson();
    private static final int MAX_SAFE_LORE_LINES = 240;
    private static final Set<Material> SPRINKLER_CROPS = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.COCOA,
            Material.MELON_STEM,
            Material.PUMPKIN_STEM,
            Material.SWEET_BERRY_BUSH,
            Material.TORCHFLOWER_CROP,
            Material.PITCHER_CROP
    );
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final ItemRarityManager itemRarityManager;
    private final ManaActionBarManager manaActionBarManager;
    private final NamespacedKey itemKey;
    private final NamespacedKey itemTypeKey;
    private final NamespacedKey displayNameKey;
    private final NamespacedKey rarityKey;
    private final NamespacedKey itemRarityKey;
    private final NamespacedKey statsKey;
    private final NamespacedKey abilityKey;
    private final NamespacedKey abilityIdKey;
    private final NamespacedKey flagsKey;
    private final NamespacedKey nbtKey;
    private final NamespacedKey sellPriceKey;
    private final NamespacedKey backpackKey;
    private final NamespacedKey fishingBagKey;
    private final NamespacedKey requirementsKey;
    private final NamespacedKey requirementsRolledKey;
    private final MMOStatStorage mmoStatStorage;
    private final MMOEnchantStorage mmoEnchantStorage;
    private final MMOLoreBuilder mmoLoreBuilder = new MMOLoreBuilder();
    private final CustomAbilityRegistry abilityRegistry = new CustomAbilityRegistry();
    private final Map<String, DatabaseCustomItem> databaseItems = new HashMap<>();
    private final Map<String, Long> legacyAbilityCooldowns = new HashMap<>();
    private final Map<String, Long> consumableCooldowns = new HashMap<>();
    private final Map<UUID, Map<MMOStatType, TimedStatBuff>> consumableStatBuffs = new HashMap<>();
    private final Map<UUID, String> lastAbilityCasts = new HashMap<>();
    private final Set<String> oversizedLoreWarnings = ConcurrentHashMap.newKeySet();
    private SkillManager skillManager;
    private PlayerLevelManager playerLevelManager;
    private CustomEnchantManager customEnchantManager;
    private MMOItemValueService itemValueService;
    private int taskId = -1;
    private BiFunction<String, Integer, ItemStack> externalItemFactory;
    private Supplier<Collection<String>> externalItemIds = List::of;

    public void registerExternalItemProvider(BiFunction<String, Integer, ItemStack> factory, Supplier<Collection<String>> ids) {
        this.externalItemFactory = factory;
        this.externalItemIds = ids == null ? List::of : ids;
    }

    public CustomItemManager(JavaPlugin plugin, DataManager dataManager, ItemRarityManager itemRarityManager, ManaActionBarManager manaActionBarManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.itemRarityManager = itemRarityManager;
        this.manaActionBarManager = manaActionBarManager;
        this.itemKey = new NamespacedKey(plugin, "custom_item_id");
        this.itemTypeKey = new NamespacedKey(plugin, "custom_item_type");
        this.displayNameKey = new NamespacedKey(plugin, "custom_item_display_name");
        this.rarityKey = new NamespacedKey(plugin, "custom_item_rarity");
        this.itemRarityKey = new NamespacedKey(plugin, "item_rarity");
        this.statsKey = new NamespacedKey(plugin, "custom_item_stats");
        this.abilityKey = new NamespacedKey(plugin, "custom_item_ability");
        this.abilityIdKey = new NamespacedKey(plugin, "ability_id");
        this.flagsKey = new NamespacedKey(plugin, "custom_item_flags");
        this.nbtKey = new NamespacedKey(plugin, "custom_item_nbt");
        this.sellPriceKey = new NamespacedKey(plugin, "custom_item_sell_price");
        this.backpackKey = new NamespacedKey(plugin, "backpack_uuid");
        this.fishingBagKey = new NamespacedKey(plugin, "fishing_bag_uuid");
        this.requirementsKey = new NamespacedKey(plugin, "mmo_requirements");
        this.requirementsRolledKey = new NamespacedKey(plugin, "mmo_requirements_rolled");
        this.mmoStatStorage = new MMOStatStorage(plugin);
        this.mmoEnchantStorage = new MMOEnchantStorage(plugin);
    }

    public void setProgressionManagers(SkillManager skillManager, PlayerLevelManager playerLevelManager) {
        this.skillManager = skillManager;
        this.playerLevelManager = playerLevelManager;
    }

    public void configureEditor(CustomEnchantManager customEnchantManager, MMOItemValueService itemValueService) {
        this.customEnchantManager = customEnchantManager;
        this.itemValueService = itemValueService;
    }

    public void setup() {
        loadDatabaseItems();
        registerRecipes();
        startEquippedEffectsTask();
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    public boolean itemExists(String id) {
        String key = normalize(id);
        return databaseItems.containsKey(key) || isStaticItem(key) || externalItemIds.get().stream().anyMatch(key::equalsIgnoreCase);
    }

    public List<String> getItemIds() {
        Set<String> ids = new LinkedHashSet<>();
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("custom-items.items");
        if (section != null) ids.addAll(section.getKeys(false));
        ids.addAll(databaseItems.keySet());
        ids.addAll(externalItemIds.get());
        List<String> sorted = new ArrayList<>(ids);
        sorted.sort(String::compareToIgnoreCase);
        return sorted;
    }

    public ItemStack createItem(String id, int amount) {
        String key = normalize(id);
        if (externalItemFactory != null) {
            ItemStack external = externalItemFactory.apply(key, amount);
            if (external != null) return external;
        }
        DatabaseCustomItem databaseItem = databaseItems.get(key);
        if (databaseItem != null) return createDatabaseItem(databaseItem, amount);
        Material material = getMaterial(key);
        boolean backpack = isBackpackId(key);
        boolean fishingBag = isFishingBagId(key);
        ItemStack item = new ItemStack(material, backpack || fishingBag ? 1 : Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(key) + ".name", "&a" + pretty(key))));
            List<String> lore = new ArrayList<>();
            for (String line : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(path(key) + ".lore")) lore.add(CC.color(line));
            int manaCost = getManaCost(key);
            if (isConsumableId(key)) {
                lore.add("");
                lore.add(CC.color("&7Consumable"));
                long cooldown = Math.max(0L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong(path(key) + ".consumable.cooldown-seconds", 0L));
                if (cooldown > 0L) lore.add(CC.color("&7Cooldown: &e" + cooldown + "s"));
                lore.add(CC.color("&eRight Click to consume!"));
            }
            String abilityType = getAbilityType(key);
            if (canHaveRightClickAbility(key, item) && !abilityType.isBlank() && !abilityType.equalsIgnoreCase("none")) {
                lore.add("");
                lore.add(CC.color("&7Ability: &e" + pretty(abilityType)));
                if (manaCost > 0) lore.add(CC.color("&7Mana Cost: &b" + manaCost));
                lore.add(CC.color("&eRight Click &7to use."));
            }
            if (backpack) {
                lore.add("");
                lore.add(CC.color("&7Backpack Slots: &e" + backpackSize(key)));
                lore.add(CC.color("&eRight Click &7to open."));
                meta.getPersistentDataContainer().set(backpackKey, PersistentDataType.STRING, UUID.randomUUID().toString());
            }
            if (fishingBag) {
                lore.add("");
                lore.add(CC.color("&7Bait Slots: &e" + fishingBagSize(key, "bait")));
                lore.add(CC.color("&7Drop Slots: &e" + fishingBagSize(key, "drops")));
                lore.add(CC.color("&eRight Click &7to open."));
                meta.getPersistentDataContainer().set(fishingBagKey, PersistentDataType.STRING, UUID.randomUUID().toString());
            }
            setSafeLore(meta, lore, key);
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, key);
            meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, configuredCustomItemType(key));
            String rawType=configuredCustomItemType(key).toUpperCase(Locale.ROOT);
            if(key.equals("developer_notebook")||Set.of("MATERIAL","MINING_MATERIAL","FORAGING_MATERIAL","FARMING_MATERIAL","DUNGEON_MATERIAL","COMBAT_MATERIAL","MONSTER_MATERIAL","FISHING_MATERIAL","MAGIC_MATERIAL","RECIPE_MATERIAL","COOKING_INGREDIENT","CUSTOM_FOOD","FOOD","CUSTOM_FISH","FISH","FISHING_ITEM","FISHING_BAIT","BAIT","FISHING_GEAR_MATERIAL","EVENT_MATERIAL","EVENT_ITEM","NORMAL_EVENT_ITEM","EVENT_COLLECTIBLE","TOKEN","EVENT_TOKEN","DUNGEON_KEY","QUEST_ITEM","FURNITURE","COSMETIC","SPECIAL","DEVELOPER_NOTEBOOK","FARMING_EQUIPMENT").contains(rawType)){
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"random_stats_blocked"),PersistentDataType.BYTE,(byte)1);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"random_rarity_blocked"),PersistentDataType.BYTE,(byte)1);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"reforge_allowed"),PersistentDataType.BYTE,(byte)0);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"enchant_allowed"),PersistentDataType.BYTE,(byte)0);
            }
            if (configuredCustomItemType(key).equalsIgnoreCase("COMPANION")) {
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "companion_id"), PersistentDataType.STRING,
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(key) + ".companion.id", key));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "companion_rarity"), PersistentDataType.STRING,
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(key) + ".companion.rarity", "COMMON"));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "companion_category"), PersistentDataType.STRING,
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(key) + ".companion.category", "misc"));
            }
            if (configuredCustomItemType(key).equalsIgnoreCase("FOOD") || configuredCustomItemType(key).equalsIgnoreCase("CUSTOM_FOOD")) {
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "food_effect_id"), PersistentDataType.STRING,
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(key) + ".food-effect-id", key));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "food_effect_type"), PersistentDataType.STRING,
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(key) + ".food.effect-type", "HEAL"));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "food_heal_amount"), PersistentDataType.INTEGER,
                        Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(key) + ".food.heal", 0)));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "food_mana_amount"), PersistentDataType.INTEGER,
                        Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(key) + ".food.mana", 0)));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "food_buff_json"), PersistentDataType.STRING,
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(key) + ".food.buff-json", "{}"));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "food_cooldown"), PersistentDataType.LONG,
                        Math.max(0L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong(path(key) + ".consumable.cooldown-seconds", 0L)));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "food_duration"), PersistentDataType.LONG,
                        Math.max(0L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong(path(key) + ".food-duration-seconds", 0L)));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "food_consumable"), PersistentDataType.BYTE, (byte) 1);
            }
            String configuredRarity = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(key) + ".rarity", "");
            if (!configuredRarity.isBlank()) {
                String normalizedRarity = normalize(configuredRarity).toUpperCase(Locale.ROOT);
                meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, normalizedRarity);
                if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(path(key) + ".fixed-rarity", false))
                    meta.getPersistentDataContainer().set(itemRarityKey, PersistentDataType.STRING, normalizedRarity);
            }
            String configuredAbility = getAbilityType(key);
            if (!configuredAbility.isBlank() && !configuredAbility.equalsIgnoreCase("none")) {
                JsonObject storedAbility = new JsonObject();
                CustomItemAbility registeredAbility = abilityRegistry.get(configuredAbility).orElse(null);
                storedAbility.addProperty("id", normalize(configuredAbility));
                storedAbility.addProperty("trigger", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                        .getString(path(key) + ".ability.trigger", registeredAbility == null ? "RIGHT_CLICK" : registeredAbility.trigger().name()));
                storedAbility.addProperty("mana_cost", getManaCost(key));
                storedAbility.addProperty("cooldown_seconds", Math.max(0L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                        .getLong(path(key) + ".ability.cooldown-seconds", defaultToolAbilityCooldown(configuredAbility))));
                meta.getPersistentDataContainer().set(abilityKey, PersistentDataType.STRING, GSON.toJson(storedAbility));
                meta.getPersistentDataContainer().set(abilityIdKey, PersistentDataType.STRING, normalize(configuredAbility));
            }
            int maxStackSize = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(key) + ".max-stack-size", 0);
            if (maxStackSize > 0) meta.setMaxStackSize(Math.clamp(maxStackSize, 1, 99));
            meta.setUnbreakable(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(path(key) + ".unbreakable", false) || isCustomGearType(configuredMmoItemType(key)));
            applyConfiguredRegistryMetadata(key, meta);
            applyConfiguredArmorMetadata(key, meta);
            item.setItemMeta(meta);
        }
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(path(key) + ".glow", false)) applyVisualGlow(item, true);
        itemRarityManager.applyIfMissing(item);
        applyMmoStats(key, item, false);
        ensureMissingGearStats(item);
        sanitizeBlockedRandomEffects(item, key);
        cleanVanillaEnchants(item);
        return item;
    }

    private void applyConfiguredRegistryMetadata(String id, ItemMeta meta) {
        String base = path(id);
        var config = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String category = config.getString(base + ".item-category", "");
        String subcategory = config.getString(base + ".item-subcategory", "");
        if (!category.isBlank())
            pdc.set(new NamespacedKey(plugin, "item_category"), PersistentDataType.STRING, category.toUpperCase(Locale.ROOT));
        if (!subcategory.isBlank())
            pdc.set(new NamespacedKey(plugin, "item_subcategory"), PersistentDataType.STRING, subcategory.toUpperCase(Locale.ROOT));
        for (String key : List.of("random-stats-allowed", "random-rarity-allowed", "reforge-allowed", "enchant-allowed")) {
            if (!config.contains(base + "." + key)) continue;
            pdc.set(new NamespacedKey(plugin, key.replace('-', '_')), PersistentDataType.BYTE,
                    config.getBoolean(base + "." + key) ? (byte) 1 : (byte) 0);
        }
        if ("REFORGE_STONE".equalsIgnoreCase(config.getString(base + ".type", ""))) {
            String reforgeId = config.getString(base + ".reforge-id", "");
            if (!reforgeId.isBlank()) pdc.set(new NamespacedKey(plugin, "reforge_id"), PersistentDataType.STRING, normalize(reforgeId));
            pdc.set(new NamespacedKey(plugin, "reforge_stone"), PersistentDataType.BYTE, (byte) 1);
        }
    }

    private void applyConfiguredArmorMetadata(String id, ItemMeta meta) {
        String base = path(id);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String setId = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + ".armor-set-id", "");
        String piece = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + ".armor-piece", "");
        if (!setId.isBlank()) pdc.set(new NamespacedKey(plugin, "armor_set_id"), PersistentDataType.STRING, normalize(setId));
        if (!piece.isBlank()) pdc.set(new NamespacedKey(plugin, "armor_piece"), PersistentDataType.STRING, piece.toUpperCase(Locale.ROOT));
        String configuredRarity = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + ".rarity", "");
        if (!configuredRarity.isBlank()) pdc.set(new NamespacedKey(plugin, "rarity"), PersistentDataType.STRING, configuredRarity.toUpperCase(Locale.ROOT));

        if (meta instanceof LeatherArmorMeta leather) {
            String[] rgb = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + ".leather-color", "").split(",");
            if (rgb.length == 3) try {
                leather.setColor(Color.fromRGB(Math.clamp(Integer.parseInt(rgb[0].trim()), 0, 255), Math.clamp(Integer.parseInt(rgb[1].trim()), 0, 255), Math.clamp(Integer.parseInt(rgb[2].trim()), 0, 255)));
            } catch (NumberFormatException ignored) { }
        }
        if (meta instanceof SkullMeta skull) {
            String head64 = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + ".head64", "");
            try {
                String decoded = new String(Base64.getDecoder().decode(head64), StandardCharsets.UTF_8);
                String marker = "\"url\":\"";
                int start = decoded.indexOf(marker);
                int end = start < 0 ? -1 : decoded.indexOf('"', start + marker.length());
                if (start >= 0 && end > start) {
                    String url = decoded.substring(start + marker.length(), end).replace("\\/", "/");
                    PlayerProfile profile = Bukkit.createPlayerProfile(UUID.nameUUIDFromBytes(("custom-item-" + id).getBytes(StandardCharsets.UTF_8)), null);
                    PlayerTextures textures = profile.getTextures();
                    textures.setSkin(new URL(url));
                    profile.setTextures(textures);
                    skull.setOwnerProfile(profile);
                }
            } catch (Exception error) {
                plugin.getLogger().warning("Could not apply head texture for custom item " + id + ": " + error.getMessage());
            }
        }
    }

    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    public String getCustomItemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        return valueOr(item.getItemMeta().getPersistentDataContainer().get(itemTypeKey, PersistentDataType.STRING), "").toUpperCase(Locale.ROOT);
    }

    public String getCustomItemRarity(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "COMMON";
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return valueOr(pdc.get(itemRarityKey, PersistentDataType.STRING), valueOr(pdc.get(rarityKey, PersistentDataType.STRING), "COMMON")).toUpperCase(Locale.ROOT);
    }

    public boolean isCollectionMaterial(ItemStack item) {
        String id = getItemId(item);
        return id != null && effectiveItemType(item, id) == MMOItemType.COLLECTION_MATERIAL;
    }

    public boolean isInertCustomMaterial(ItemStack item) {
        String id = getItemId(item);
        if (id == null) return false;
        MMOItemType type = effectiveItemType(item, id);
        return type == MMOItemType.MATERIAL || type == MMOItemType.COLLECTION_MATERIAL || type == MMOItemType.RECIPE_MATERIAL;
    }

    public boolean canRollRandomRarityEffects(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String id=getItemId(item),raw=getCustomItemType(item);
        if("developer_notebook".equalsIgnoreCase(id)||Set.of("MATERIAL","MINING_MATERIAL","FORAGING_MATERIAL","FARMING_MATERIAL","DUNGEON_MATERIAL","COMBAT_MATERIAL","MONSTER_MATERIAL","FISHING_MATERIAL","MAGIC_MATERIAL","RECIPE_MATERIAL","COOKING_INGREDIENT","CUSTOM_FOOD","FOOD","CUSTOM_FISH","FISH","FISHING_ITEM","FISHING_BAIT","BAIT","FISHING_GEAR_MATERIAL","EVENT_MATERIAL","EVENT_ITEM","NORMAL_EVENT_ITEM","EVENT_COLLECTIBLE","TOKEN","EVENT_TOKEN","DUNGEON_KEY","QUEST_ITEM","FURNITURE","COSMETIC","SPECIAL","DEVELOPER_NOTEBOOK","FARMING_EQUIPMENT").contains(raw))return false;
        if(item.hasItemMeta())for(NamespacedKey key:item.getItemMeta().getPersistentDataContainer().getKeys()){String name=key.getKey();if(Set.of("random_stats_blocked","random_rarity_blocked","special_item","developer_notebook","cooking_ingredient").contains(name)){Byte value=item.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.BYTE);if(value==null||value!=0)return false;}}
        return canRollRandomRarityEffects(effectiveItemType(item, getItemId(item)));
    }

    public boolean isDeveloperNotebook(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(itemKey, PersistentDataType.STRING);
        return "developer_notebook".equalsIgnoreCase(id) && pdcByteFlag(pdc, "developer_notebook");
    }

    public boolean isAbilityBlockedItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return isDeveloperNotebook(item) || pdcByteFlag(pdc, "shop_preview_item")
                || pdcByteFlag(pdc, "preview_item") || pdcByteFlag(pdc, "admin_item");
    }

    private boolean pdcByteFlag(PersistentDataContainer pdc, String key) {
        return Byte.valueOf((byte) 1).equals(pdc.get(new NamespacedKey(plugin, key), PersistentDataType.BYTE));
    }

    public MMOStatMap getMmoStats(ItemStack item) {
        if (item == null || item.getType().isAir() || getItemId(item) == null) return MMOStatMap.empty();
        MMOStatMap stats = mmoStatStorage.hasStats(item) ? mmoStatStorage.read(item) : MMOStatMap.empty();
        if (customEnchantManager != null) stats.addAll(customEnchantManager.statBonuses(item));
        return stats;
    }

    public boolean hasMmoStatsPdc(ItemStack item) {
        return mmoStatStorage.hasStats(item);
    }

    public boolean isAbilityRegistered(String id) {
        return id != null && abilityRegistry.contains(id);
    }

    public MMOStatMap getMmoStats(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return MMOStatMap.empty();
        String id = getItemId(item);
        if (id == null || !itemExists(id)) return MMOStatMap.empty();
        if (!mmoStatStorage.hasStats(item)) return MMOStatMap.empty();
        if (!meetsCachedItemRequirements(player, item, false)) return MMOStatMap.empty();
        MMOStatMap stats = mmoStatStorage.read(item);
        if (customEnchantManager != null) stats.addAll(customEnchantManager.statBonuses(item));
        return stats;
    }

    public MMOItemType getMmoItemType(ItemStack item) {
        if (item == null || item.getType().isAir()) return MMOItemType.MISC;
        return effectiveItemType(item, getItemId(item));
    }

    public boolean upgradeMmoItem(ItemStack item) {
        String id = getItemId(item);
        if (id == null || id.isBlank() || item == null || item.getType().isAir()) return false;
        boolean sanitized = sanitizeBlockedRandomEffects(item, id);
        if (mmoStatStorage.hasStats(item)) {
            boolean repaired = ensureMissingGearStats(item);
            ensureItemRequirements(item, id, 50.0D);
            rebuildMmoLore(item, mmoStatStorage.read(item));
            ensureCustomGearUnbreakable(item, id);
            cleanVanillaEnchants(item);
            return repaired || sanitized;
        }
        boolean upgraded = applyMmoStats(id, item, false);
        upgraded |= ensureMissingGearStats(item);
        ensureCustomGearUnbreakable(item, id);
        rebuildMmoLore(item, mmoStatStorage.read(item));
        cleanVanillaEnchants(item);
        return upgraded || sanitized;
    }

    /** Adds only absent baseline stats; authored and previously rolled values are preserved. */
    public boolean ensureMissingGearStats(ItemStack item) {
        if (item == null || item.getType().isAir() || isAbilityBlockedItem(item)) return false;
        String id = getItemId(item);
        if (id == null || !itemExists(id)) return false;
        String customType = getCustomItemType(item);
        if (Set.of("MATERIAL", "RECIPE_MATERIAL", "COOKING_INGREDIENT", "CUSTOM_FOOD", "FOOD",
                "CUSTOM_FISH", "FISH", "TOKEN", "EVENT_TOKEN", "QUEST_ITEM", "COSMETIC", "FURNITURE",
                "SPECIAL", "DEVELOPER_NOTEBOOK").contains(customType)) return false;
        MMOItemType type = effectiveItemType(item, id);
        if (!isCustomGearType(type)) return false;
        MMOStatMap stats = mmoStatStorage.hasStats(item) ? mmoStatStorage.read(item) : MMOStatMap.empty();
        boolean changed = false;
        for (Map.Entry<MMOStatType, Double> entry : defaultGearStats(item, customType, type).entrySet()) {
            if (stats.get(entry.getKey()) > 0.0D) continue;
            stats.set(entry.getKey(), entry.getValue());
            changed = true;
        }
        if (!changed) return false;
        mmoStatStorage.write(item, type, stats);
        ensureCustomGearUnbreakable(item, id);
        rebuildMmoLore(item, stats);
        return true;
    }

    public int repairMissingGearStats(Player player) {
        if (player == null) return 0;
        int repaired = 0;
        for (ItemStack item : player.getInventory().getContents()) if (ensureMissingGearStats(item)) repaired++;
        if (plugin instanceof Game game && game.getMmoStatService() != null) game.getMmoStatService().refreshPlayerMMOStats(player);
        return repaired;
    }

    public MaxItemResult maxCustomItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return new MaxItemResult(MaxItemStatus.INVALID, null, 0);
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(itemKey, PersistentDataType.STRING);
        if ("developer_notebook".equalsIgnoreCase(id) || pdcByteFlag(pdc, "developer_notebook")) {
            return new MaxItemResult(MaxItemStatus.DEVELOPER_NOTEBOOK, id, 0);
        }
        if (id == null || id.isBlank() || !itemExists(id) || pdcByteFlag(pdc, "shop_preview_item")
                || pdcByteFlag(pdc, "preview_item") || pdcByteFlag(pdc, "admin_item")
                || pdcByteFlag(pdc, "special_item")) {
            return new MaxItemResult(MaxItemStatus.INVALID, id, 0);
        }

        String rawType = valueOr(getCustomItemType(item), "").toUpperCase(Locale.ROOT);
        if (MAX_ITEM_BLOCKED_TYPES.contains(rawType) || !MAX_ITEM_ALLOWED_TYPES.contains(rawType)) {
            return new MaxItemResult(MaxItemStatus.INVALID, id, 0);
        }
        MMOItemType type = effectiveItemType(item, id);
        if (!MAX_ITEM_ALLOWED_MMO_TYPES.contains(type)) return new MaxItemResult(MaxItemStatus.INVALID, id, 0);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new MaxItemResult(MaxItemStatus.INVALID, id, 0);
        if (type.canRollRandomRarityEffects()) {
            meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, "MYTHIC");
            meta.getPersistentDataContainer().set(itemRarityKey, PersistentDataType.STRING, "MYTHIC");
        }
        meta.setUnbreakable(true);
        applyMmoTooltipHiding(meta);
        item.setItemMeta(meta);

        MMOStatMap maximum = MMOStatMap.empty();
        mergeMaximum(maximum, maximumConfiguredStats(id, type.canRollRandomRarityEffects()));
        if (isAccessoryType(type)) {
            maxStat(maximum, MMOStatType.HEALTH, 625.0D);
            maxStat(maximum, MMOStatType.DEFENSE, 375.0D);
            maxStat(maximum, MMOStatType.MANA, 525.0D);
            maxStat(maximum, MMOStatType.STRENGTH, 225.0D);
            maxStat(maximum, MMOStatType.CRIT_DAMAGE, 50.0D);
            maxStat(maximum, MMOStatType.SPEED, 12.0D);
        } else if (type == MMOItemType.WAND || type == MMOItemType.STAFF) {
            maxStat(maximum, MMOStatType.DAMAGE, 375.0D);
            maxStat(maximum, MMOStatType.MANA, 525.0D);
            maxStat(maximum, MMOStatType.INTELLIGENCE, 300.0D);
            maxStat(maximum, MMOStatType.ABILITY_DAMAGE, 50.0D);
            maxStat(maximum, MMOStatType.MANA_REGEN, 25.0D);
        } else {
            for (Map.Entry<MMOStatType, Double> stat : defaultGearStats(item, rawType, type).entrySet()) {
                maxStat(maximum, stat.getKey(), stat.getValue());
            }
        }
        mmoStatStorage.write(item, type, maximum);
        mmoStatStorage.clearRandomRoll(item);
        int enchantments = customEnchantManager == null ? 0
                : customEnchantManager.applyAllCompatibleMaxEnchants(item, false);
        rebuildMmoLore(item, maximum, player);
        if (plugin instanceof Game game && game.getMmoStatService() != null) {
            game.getMmoStatService().refreshPlayerMMOStats(player);
        }
        return new MaxItemResult(MaxItemStatus.SUCCESS, id, enchantments);
    }

    private MMOStatMap maximumConfiguredStats(String id, boolean rarityUpgradeable) {
        MMOStatMap result = MMOStatMap.empty();
        double maximumRollMultiplier = rarityUpgradeable ? 2.25D * 1.10D : 1.0D;
        ConfigurationSection stats = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getConfigurationSection(path(id) + ".mmo.stats");
        ConfigurationSection ranges = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getConfigurationSection(path(id) + ".mmo.stat-ranges");
        Set<String> keys = new LinkedHashSet<>();
        if (stats != null) keys.addAll(stats.getKeys(false));
        if (ranges != null) keys.addAll(ranges.getKeys(false));
        for (String key : keys) {
            MMOStatType type = MMOStatType.parse(key);
            if (type == null) continue;
            StatRange range = ranges == null ? null : rangeFromSection(ranges, key);
            if (range == null && stats != null) range = rangeFromSection(stats, key);
            if (range != null) result.set(type, ((range.min() + range.max()) / 2.0D) * maximumRollMultiplier);
            else if (stats != null) result.set(type, stats.getDouble(key, 0.0D) * maximumRollMultiplier);
        }
        DatabaseCustomItem databaseItem = databaseItems.get(normalize(id));
        if (databaseItem != null) {
            for (Map.Entry<String, JsonElement> entry : jsonObject(databaseItem.statsJson()).entrySet()) {
                MMOStatType type = MMOStatType.parse(entry.getKey());
                if (type == null) continue;
                StatRange range = rangeFromJson(entry.getValue());
                if (range != null) maxStat(result, type, ((range.min() + range.max()) / 2.0D) * maximumRollMultiplier);
                else if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                    maxStat(result, type, entry.getValue().getAsDouble() * maximumRollMultiplier);
                }
            }
        }
        return result;
    }

    private void mergeMaximum(MMOStatMap target, MMOStatMap source) {
        if (source == null) return;
        source.values().forEach((type, value) -> maxStat(target, type, value));
    }

    private void maxStat(MMOStatMap stats, MMOStatType type, double value) {
        if (value > stats.get(type)) stats.set(type, value);
    }

    private boolean isAccessoryType(MMOItemType type) {
        return switch (type) {
            case RING, NECKLACE, TRINKET, ACCESSORY, CHARM, TOTEM -> true;
            default -> false;
        };
    }

    private static final Set<String> MAX_ITEM_ALLOWED_TYPES = Set.of(
            "SWORD", "KATANA", "DAGGER", "GREATSWORD", "BOW", "SHORTBOW", "LONGBOW", "CROSSBOW",
            "FISHING_ROD", "TOOL", "PICKAXE", "AXE", "SHOVEL", "HOE", "FARMING_HOE",
            "FORAGING_AXE", "DRILL", "ARMOR", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS",
            "WAND", "STAFF", "RING", "NECKLACE", "CHARM", "TOTEM", "ACCESSORY"
    );
    private static final Set<String> MAX_ITEM_BLOCKED_TYPES = Set.of(
            "MATERIAL", "MINING_MATERIAL", "FORAGING_MATERIAL", "FARMING_MATERIAL", "DUNGEON_MATERIAL",
            "COMBAT_MATERIAL", "MONSTER_MATERIAL", "FISHING_MATERIAL", "MAGIC_MATERIAL", "RECIPE_MATERIAL",
            "COOKING_INGREDIENT", "CUSTOM_FOOD", "FOOD", "CUSTOM_FISH", "FISH", "TOKEN", "EVENT_TOKEN",
            "QUEST_ITEM", "FURNITURE", "COSMETIC", "SPECIAL", "DEVELOPER_NOTEBOOK"
    );
    private static final Set<MMOItemType> MAX_ITEM_ALLOWED_MMO_TYPES = EnumSet.of(
            MMOItemType.WEAPON, MMOItemType.SWORD, MMOItemType.KATANA, MMOItemType.DAGGER, MMOItemType.AXE,
            MMOItemType.BOW, MMOItemType.CROSSBOW, MMOItemType.FISHING_ROD, MMOItemType.ROD, MMOItemType.TOOL,
            MMOItemType.FARMING_EQUIPMENT, MMOItemType.ARMOR, MMOItemType.WAND, MMOItemType.STAFF,
            MMOItemType.RING, MMOItemType.NECKLACE, MMOItemType.TRINKET, MMOItemType.ACCESSORY,
            MMOItemType.CHARM, MMOItemType.TOTEM
    );

    private Map<MMOStatType, Double> defaultGearStats(ItemStack item, String customType, MMOItemType type) {
        int tier = switch (getCustomItemRarity(item)) {
            case "UNCOMMON" -> 1; case "RARE" -> 2; case "EPIC" -> 3;
            case "LEGENDARY" -> 4; case "MYTHIC" -> 5; default -> 0;
        };
        double[] damage = {12, 28, 58, 110, 215, 375}, strength = {5, 12, 30, 62, 128, 225};
        double[] health = {15, 32, 80, 175, 350, 625}, defense = {6, 16, 40, 95, 210, 375};
        double[] mana = {12, 30, 62, 138, 275, 525}, fortune = {5, 12, 25, 45, 75, 120};
        Map<MMOStatType, Double> out = new LinkedHashMap<>();
        Material material = item.getType();
        String raw = customType == null ? "" : customType.toUpperCase(Locale.ROOT);
        if (type == MMOItemType.ARMOR || isArmorMaterial(material)) {
            out.put(MMOStatType.HEALTH, health[tier]);
            out.put(MMOStatType.DEFENSE, defense[tier]);
            if (material.name().endsWith("_HELMET")) out.put(MMOStatType.MANA, mana[tier]);
            if (material.name().endsWith("_BOOTS")) out.put(MMOStatType.SPEED, 2.0D + tier * 2.0D);
        } else if (type == MMOItemType.FISHING_ROD || type == MMOItemType.ROD || raw.equals("FISHING_ROD")) {
            out.put(MMOStatType.FISHING_SPEED, 5.0D + tier * 6.0D);
            out.put(MMOStatType.FISHING_FORTUNE, fortune[tier]);
            out.put(MMOStatType.TREASURE_CHANCE, 1.0D + tier * 1.5D);
        } else if (type == MMOItemType.TOOL || type == MMOItemType.FARMING_EQUIPMENT
                || raw.matches("PICKAXE|DRILL|AXE|FORAGING_AXE|SHOVEL|HOE|FARMING_HOE|TOOL")) {
            if (raw.contains("HOE") || type == MMOItemType.FARMING_EQUIPMENT) {
                out.put(MMOStatType.FARMING_FORTUNE, fortune[tier]); out.put(MMOStatType.FARMING_XP_BOOST, 1.0D + tier * 2.0D);
            } else if (raw.contains("AXE") || material.name().endsWith("_AXE")) {
                out.put(MMOStatType.FORAGING_FORTUNE, fortune[tier]); out.put(MMOStatType.MINING_SPEED, 20.0D + tier * 25.0D);
            } else if (raw.contains("SHOVEL") || material.name().endsWith("_SHOVEL")) {
                out.put(MMOStatType.EXCAVATION_FORTUNE, fortune[tier]); out.put(MMOStatType.MINING_SPEED, 20.0D + tier * 25.0D);
            } else {
                out.put(MMOStatType.MINING_SPEED, 30.0D + tier * 40.0D); out.put(MMOStatType.MINING_FORTUNE, fortune[tier]);
                out.put(MMOStatType.BREAKING_POWER, 1.0D + tier);
            }
        } else {
            out.put(MMOStatType.DAMAGE, damage[tier]); out.put(MMOStatType.STRENGTH, strength[tier]);
            out.put(MMOStatType.CRIT_CHANCE, 5.0D + tier * 2.0D); out.put(MMOStatType.CRIT_DAMAGE, 10.0D + tier * 8.0D);
            if (type == MMOItemType.KATANA) out.put(MMOStatType.FEROCITY, 2.0D + tier * 2.0D);
            if (type == MMOItemType.DAGGER) out.put(MMOStatType.SPEED, 2.0D + tier * 2.0D);
        }
        return out;
    }

    public boolean isCustomMeleeWeapon(ItemStack item) {
        String id = getItemId(item);
        if (id == null || id.isBlank() || item == null || item.getType().isAir() || !itemExists(id)) return false;
        MMOItemType type = mmoStatStorage.hasStats(item) ? mmoStatStorage.readItemType(item) : configuredMmoItemType(id);
        return isCustomMeleeWeaponType(type);
    }

    public boolean isFishingBait(ItemStack item) {
        String id = getItemId(item);
        if (id == null || id.isBlank() || item == null || item.getType().isAir() || !itemExists(id)) return false;
        MMOItemType type = mmoStatStorage.hasStats(item) ? mmoStatStorage.readItemType(item) : configuredMmoItemType(id);
        return isFishingBaitType(type) || isFishingBaitId(id);
    }

    public boolean isCustomGearItem(ItemStack item) {
        String id = getItemId(item);
        if (id == null || id.isBlank() || item == null || item.getType().isAir()) return false;
        MMOItemType type = mmoStatStorage.hasStats(item) ? mmoStatStorage.readItemType(item) : configuredMmoItemType(id);
        return isCustomGearType(type);
    }

    public boolean cleanVanillaEnchants(ItemStack item) {
        if (item == null || item.getType().isAir() || getItemId(item) == null) return false;
        Map<String, Integer> current = mmoEnchantStorage.read(item);
        Map<String, Integer> vanilla = mmoEnchantStorage.readVanillaEnchants(item);
        vanilla.forEach((id, level) -> current.merge(id, level, Math::max));
        boolean removed = mmoEnchantStorage.removeVanillaEnchants(item);
        if (!current.isEmpty()) mmoEnchantStorage.write(item, current);
        if (removed || !current.isEmpty()) rebuildMmoLore(item, mmoStatStorage.read(item));
        return removed || !vanilla.isEmpty();
    }

    public boolean isDatabaseItem(String id) {
        return databaseItems.containsKey(normalize(id));
    }

    public boolean isStaticItem(String id) {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection(path(id));
    }

    public List<String> getDatabaseItemIds() {
        List<String> ids = new ArrayList<>(databaseItems.keySet());
        ids.sort(String::compareToIgnoreCase);
        return ids;
    }

    public CustomAbilityRegistry abilityRegistry() {
        return abilityRegistry;
    }

    public void openEditor(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        Inventory inventory = Bukkit.createInventory(new MenuHolder("edititem"), 54, CC.color("&8Edit Item"));
        fillMenu(inventory);
        inventory.setItem(13, held == null || held.getType().isAir()
                ? menuItem(Material.BARRIER, "&cNo Held Item", List.of("&7Hold an item to edit it."))
                : held.clone());
        inventory.setItem(20, menuItem(Material.NAME_TAG, "&eIdentity", List.of(
                "&7ID: &f" + valueOr(getItemId(held), "not set"),
                "&7Type: &f" + valueOr(readString(held, itemTypeKey), "not set"),
                "&7Rarity: &f" + valueOr(readString(held, rarityKey), "not set"),
                "",
                "&eClick to edit identity.",
                "&8Edit Item Action: identity"
        )));
        inventory.setItem(21, menuItem(Material.NETHER_STAR, "&dRarity", List.of(
                "&7Current: &f" + valueOr(readString(held, rarityKey), "not set"),
                "",
                "&eClick to choose a rarity.",
                "&8Edit Item Action: rarity"
        )));
        inventory.setItem(22, menuItem(Material.ENCHANTED_BOOK, "&9Enchantments", List.of(
                "&7Add common vanilla enchantments",
                "&7to the held item.",
                "",
                "&eClick to open enchantments.",
                "&8Edit Item Action: enchantments"
        )));
        inventory.setItem(23, menuItem(Material.BLAZE_POWDER, "&bAbilities", List.of(
                "&7Current: &f" + valueOr(readString(held, abilityKey), "{}"),
                "",
                "&eClick to choose an ability.",
                "&8Edit Item Action: abilities"
        )));
        inventory.setItem(24, menuItem(Material.EXPERIENCE_BOTTLE, "&aSkill Requirements", List.of(
                "&7Set skill level requirements",
                "&7stored on the item NBT.",
                "",
                "&eClick to open requirements.",
                "&8Edit Item Action: requirements"
        )));
        inventory.setItem(30, menuItem(Material.IRON_SWORD, "&cStats", List.of(
                "&7Edit PDC-backed MMO stats.",
                "",
                "&eClick to edit stats.",
                "&8Edit Item Action: stats"
        )));
        inventory.setItem(31, menuItem(Material.PAPER, "&fLore and Flags", List.of(
                "&7Edit description and tooltip flags.",
                "",
                "&eClick to edit lore and flags.",
                "&8Edit Item Action: lore_flags"
        )));
        inventory.setItem(32, menuItem(Material.EMERALD, "&aEconomy", List.of(
                "&7Edit value and trading rules.",
                "",
                "&eClick to edit economy.",
                "&8Edit Item Action: economy"
        )));
        inventory.setItem(33, menuItem(Material.AMETHYST_SHARD, "&dReroll Stats", List.of(
                "&7Rolled Rarity: &f" + (held == null ? "not set" : getCustomItemRarity(held)),
                "&7Randomized stats are stored internally.",
                "", "&eClick to reroll this item.", "&8Edit Item Action: reroll_stats"
        )));
        inventory.setItem(40, menuItem(Material.LIME_DYE, "&aSave / Apply", List.of(
                "&7Save the held item using its", "&7current custom item ID.", "", "&eClick to save.", "&8Edit Item Action: save"
        )));
        inventory.setItem(41, menuItem(Material.KNOWLEDGE_BOOK, "&6Recipes / Unlocks", List.of(
                "&7Open the existing recipe book.", "", "&eClick to open.", "&8Edit Item Action: recipes"
        )));
        inventory.setItem(49, menuItem(Material.BARRIER, "&cClose", List.of("&8Edit Item Action: close")));
        player.openInventory(inventory);
    }

    public void openEditorSection(Player player, String section) {
        String normalized = normalize(section);
        switch (normalized) {
            case "rarity" -> openRarityEditor(player);
            case "abilities" -> openAbilityEditor(player);
            case "enchantments" -> openEnchantmentEditor(player);
            case "requirements" -> openRequirementEditor(player);
            case "identity" -> openIdentityEditor(player);
            case "identity_types" -> openItemTypeEditor(player);
            case "stats" -> openStatsEditor(player);
            case "lore_flags" -> openLoreFlagsEditor(player);
            case "economy" -> openEconomyEditor(player);
            case "reroll_stats" -> {
                rerollStats(player.getInventory().getItemInMainHand());
                openEditor(player);
            }
            default -> openEditor(player);
        }
    }

    public boolean rerollStats(ItemStack item) {
        if (item == null || item.getType().isAir() || !canRollRandomRarityEffects(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        meta.getPersistentDataContainer().remove(itemRarityKey);
        item.setItemMeta(meta);
        itemRarityManager.applyIfMissing(item);
        mmoStatStorage.clearStats(item);
        return applyMmoStats(getItemId(item), item, true);
    }

    public void saveEditorItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        String id = getItemId(item);
        SaveResult result = saveDatabaseItem(id == null ? "" : id, item, player.getUniqueId().toString(), false);
        String message = switch (result) {
            case SAVED -> "&aSaved custom item &f" + normalize(id) + "&a.";
            case STATIC_ID -> "&cStatic registry IDs cannot be overwritten from the menu.";
            case DATABASE_DISABLED -> "&cMariaDB persistence is disabled; the held item changes remain applied locally.";
            case INVALID_ID -> "&cSet a valid custom item ID before saving.";
            case INVALID_ITEM -> "&cHold a valid item first.";
            case ERROR -> "&cCould not save that item. Check console.";
        };
        player.sendMessage(CC.color(message));
    }

    private void openRarityEditor(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("edititem:rarity"), 27, CC.color("&8Edit Rarity"));
        fillMenu(inventory);
        String[] rarities = {"COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"};
        Material[] materials = {Material.WHITE_DYE, Material.LIME_DYE, Material.BLUE_DYE, Material.PURPLE_DYE, Material.ORANGE_DYE, Material.MAGENTA_DYE};
        int[] slots = {10, 11, 12, 13, 14, 15};
        for (int index = 0; index < rarities.length; index++) {
            inventory.setItem(slots[index], menuItem(materials[index], "&f" + rarities[index], List.of(
                    "&7Set held item rarity.",
                    "",
                    "&eClick to apply.",
                    "&8Edit Item Rarity: " + rarities[index]
            )));
        }
        inventory.setItem(22, menuItem(Material.ARROW, "&cBack", List.of("&8Edit Item Action: back")));
        inventory.setItem(26, menuItem(Material.BARRIER, "&cClose", List.of("&8Edit Item Action: close")));
        player.openInventory(inventory);
    }

    private void openAbilityEditor(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("edititem:abilities"), 54, CC.color("&8Edit Abilities"));
        fillMenu(inventory);
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int index = 0;
        ItemStack held = player.getInventory().getItemInMainHand();
        for (CustomItemAbility ability : abilityRegistry.all()) {
            if (index >= slots.length) break;
            if (!isAbilityValidForItem(ability.id(), held)) continue;
            List<String> lore = new ArrayList<>();
            lore.add("&7Trigger: &f" + ability.trigger().name());
            lore.add("&7Mana Cost: &b" + ability.manaCost());
            lore.add("");
            lore.add("&eClick to apply.");
            lore.add("&8Edit Item Ability: " + ability.id());
            inventory.setItem(slots[index++], menuItem(Material.BLAZE_POWDER, "&b" + ability.displayName(), lore));
        }
        inventory.setItem(45, menuItem(Material.ARROW, "&cBack", List.of("&8Edit Item Action: back")));
        inventory.setItem(49, menuItem(Material.BARRIER, "&cClose", List.of("&8Edit Item Action: close")));
        player.openInventory(inventory);
    }

    private void openEnchantmentEditor(Player player) {
        openEnchantmentEditor(player, 0);
    }

    public void openEnchantmentEditor(Player player, int page) {
        openEnchantmentEditor(player, page, "&8Edit Enchantments");
    }

    public void openSandboxEnchantmentEditor(Player player, int page) {
        openEnchantmentEditor(player, page, "&8Sandbox - Enchantments");
    }

    private void openEnchantmentEditor(Player player, int page, String title) {
        ItemStack held = player.getInventory().getItemInMainHand();
        List<EditorEnchant> options = editorEnchantments();
        int[] slots = editorContentSlots();
        int totalPages = Math.max(1, (options.size() + slots.length - 1) / slots.length);
        int safePage = Math.clamp(page, 0, totalPages - 1);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("edititem:enchantments:" + safePage), 54, CC.color(title));
        fillMenu(inventory);
        Map<String, Integer> current = mmoEnchantStorage.read(held);
        int start = safePage * slots.length;
        for (int index = 0; index < slots.length && start + index < options.size(); index++) {
            EditorEnchant option = options.get(start + index);
            boolean applicable = canApplyEditorEnchant(held, option);
            int level = current.getOrDefault(option.id(), 0);
            List<String> lore = List.of(
                    "&7Type: &e" + (option.custom() ? "Custom" : "Vanilla"),
                    "&7Current Level: &a" + level,
                    "&7Max Level: &e" + option.maxLevel(),
                    "&7Applies To: &f" + pretty(option.itemTypes()),
                    "",
                    applicable ? "&eLeft Click to increase" : "&cLocked for this item type",
                    applicable ? "&eRight Click to decrease" : "&7Use a compatible item type.",
                    applicable ? "&eShift Left Click to set max" : "",
                    applicable ? "&eShift Right Click to remove" : "",
                    "&8Edit Item Enchant: " + (option.custom() ? "custom" : "vanilla") + ":" + option.id()
            );
            Material icon = applicable ? option.icon() : Material.GRAY_DYE;
            inventory.setItem(slots[index], menuItem(icon, (applicable ? "&9" : "&8") + option.name(), lore));
        }
        inventory.setItem(45, menuItem(Material.ARROW, "&cBack", List.of("&8Edit Item Action: back")));
        inventory.setItem(49, menuItem(Material.BARRIER, "&cClose", List.of("&8Edit Item Action: close")));
        if (safePage > 0) inventory.setItem(48, menuItem(Material.ARROW, "&aPrevious Page", List.of("&8Edit Item Page: " + (safePage - 1))));
        inventory.setItem(49, menuItem(Material.BARRIER, "&cClose", List.of("&8Edit Item Action: close")));
        if (safePage + 1 < totalPages) inventory.setItem(50, menuItem(Material.ARROW, "&aNext Page", List.of("&8Edit Item Page: " + (safePage + 1))));
        player.openInventory(inventory);
    }

    private List<EditorEnchant> editorEnchantments() {
        List<EditorEnchant> options = new ArrayList<>();
        if (customEnchantManager != null) {
            for (CustomEnchantManager.CustomEnchant enchant : customEnchantManager.all()) {
                options.add(new EditorEnchant(enchant.id(), enchant.name(), enchant.type(), enchant.maxLevel(),
                        customEnchantManager.displayMaterial(enchant.id()), true));
            }
        }
        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            String id = enchantment.getKey().getKey().toLowerCase(Locale.ROOT);
            options.add(new EditorEnchant(id, pretty(id), vanillaAppliesTo(enchantment), enchantment.getMaxLevel(), Material.ENCHANTED_BOOK, false));
        }
        options.sort(java.util.Comparator.comparing(EditorEnchant::custom).reversed().thenComparing(EditorEnchant::name));
        return options;
    }

    private boolean canApplyEditorEnchant(ItemStack item, EditorEnchant option) {
        if (item == null || item.getType().isAir() || isNonEnchantableItemType(getMmoItemType(item))) return false;
        if (option.custom()) return customEnchantManager != null && customEnchantManager.canApply(item, option.id());
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(option.id()));
        return enchantment != null && enchantment.canEnchantItem(item);
    }

    private String vanillaAppliesTo(Enchantment enchantment) {
        String id = enchantment.getKey().getKey();
        if (id.contains("protection") || id.equals("thorns") || id.equals("binding_curse")) return "armor";
        if (id.contains("fishing") || id.equals("lure") || id.equals("luck_of_the_sea")) return "fishing rods";
        if (id.contains("arrow") || id.equals("power") || id.equals("punch") || id.equals("flame") || id.equals("infinity")) return "bows";
        if (id.equals("efficiency") || id.equals("fortune") || id.equals("silk_touch")) return "tools";
        return "compatible items";
    }

    private boolean isNonEnchantableItemType(MMOItemType type) {
        return switch (type) {
            case MATERIAL, FOOD, POTION, CONSUMABLE, FISHING_BAIT, BAIT, COMPANION -> true;
            default -> false;
        };
    }

    private int[] editorContentSlots() {
        return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    }

    private record EditorEnchant(String id, String name, String itemTypes, int maxLevel, Material icon, boolean custom) {
    }

    private void openStatsEditor(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        Inventory inventory = Bukkit.createInventory(new MenuHolder("edititem:stats"), 54, CC.color("&8Edit MMO Stats"));
        fillMenu(inventory);
        List<MMOStatType> stats = new ArrayList<>(List.of(
                MMOStatType.DAMAGE, MMOStatType.STRENGTH, MMOStatType.CRIT_CHANCE, MMOStatType.CRIT_DAMAGE,
                MMOStatType.FEROCITY, MMOStatType.LIFESTEAL, MMOStatType.HEALTH,
                MMOStatType.DEFENSE, MMOStatType.TRUE_DEFENSE, MMOStatType.SPEED, MMOStatType.VITALITY,
                MMOStatType.MANA, MMOStatType.MINING_SPEED, MMOStatType.MINING_FORTUNE, MMOStatType.BREAKING_POWER,
                MMOStatType.FARMING_FORTUNE, MMOStatType.REPLANT_CHANCE, MMOStatType.CROP_GROWTH,
                MMOStatType.FISHING_SPEED, MMOStatType.FISHING_FORTUNE, MMOStatType.SEA_CREATURE_CHANCE,
                MMOStatType.TREASURE_CHANCE, MMOStatType.MAGIC_FIND, MMOStatType.PET_LUCK, MMOStatType.XP_BOOST
        ));
        MMOItemType itemType = getMmoItemType(held);
        stats.sort(java.util.Comparator.comparing((MMOStatType stat) -> !isRelevantStat(itemType, stat)).thenComparing(MMOStatType::name));
        MMOStatMap current = mmoStatStorage.read(held);
        int[] slots = editorContentSlots();
        for (int index = 0; index < stats.size() && index < slots.length; index++) {
            MMOStatType stat = stats.get(index);
            double value = current.get(stat);
            inventory.setItem(slots[index], menuItem(statMaterial(stat), stat.color() + stat.display(), List.of(
                    "&7Current: &f" + formatEditorNumber(value) + (stat.percent() ? "%" : ""),
                    "&7Relevant: " + (isRelevantStat(itemType, stat) ? "&aYes" : "&7Advanced"),
                    "",
                    "&eLeft Click: &a+1",
                    "&eRight Click: &c-1",
                    "&eShift Left: &a+10",
                    "&eShift Right: &c-10",
                    "&eMiddle Click: &7Reset",
                    "&8Edit Item Stat: " + stat.name()
            )));
        }
        inventory.setItem(45, menuItem(Material.ARROW, "&cBack", List.of("&8Edit Item Action: back")));
        inventory.setItem(49, menuItem(Material.BARRIER, "&cClose", List.of("&8Edit Item Action: close")));
        player.openInventory(inventory);
    }

    private void openIdentityEditor(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        Inventory inventory = Bukkit.createInventory(new MenuHolder("edititem:identity"), 54, CC.color("&8Edit Identity"));
        fillMenu(inventory);
        inventory.setItem(10, editorTextButton(Material.PAPER, "&eCustom Item ID", valueOr(getItemId(held), "not set"), "id"));
        inventory.setItem(11, editorTextButton(Material.NAME_TAG, "&eDisplay Name", held != null && held.hasItemMeta() && held.getItemMeta().hasDisplayName() ? held.getItemMeta().getDisplayName() : "not set", "name"));
        inventory.setItem(12, menuItem(Material.NETHER_STAR, "&dRarity", List.of("&7Current: &f" + valueOr(readString(held, rarityKey), "not set"), "", "&eClick to choose.", "&8Edit Item Action: rarity")));
        inventory.setItem(13, menuItem(Material.ITEM_FRAME, "&bItem Type", List.of("&7Current: &f" + getMmoItemType(held).name(), "", "&eClick to choose.", "&8Edit Item Action: identity_types")));
        inventory.setItem(14, editorTextButton(Material.GRASS_BLOCK, "&aBase Material", held == null ? "AIR" : held.getType().name(), "material"));
        inventory.setItem(15, editorTextButton(Material.PAINTING, "&dCustom Model Data", editorModelData(held), "model"));
        inventory.setItem(16, editorTextButton(Material.CHEST, "&6Item Category", editorNbtValue(held, "category", "not set"), "category"));
        inventory.setItem(19, editorTextButton(Material.WRITABLE_BOOK, "&bRequirement Profile", editorNbtValue(held, "requirement_profile", "not set"), "requirement_profile"));
        addToggleButton(inventory, 20, Material.CHAINMAIL_CHESTPLATE, "&cSoulbound", held, "soulbound");
        addToggleButton(inventory, 21, Material.EMERALD, "&aTradeable", held, "tradeable");
        inventory.setItem(45, menuItem(Material.ARROW, "&cBack", List.of("&8Edit Item Action: back")));
        inventory.setItem(49, menuItem(Material.BARRIER, "&cClose", List.of("&8Edit Item Action: close")));
        player.openInventory(inventory);
    }

    private void openItemTypeEditor(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("edititem:identity_types"), 54, CC.color("&8Choose Item Type"));
        fillMenu(inventory);
        int[] slots = editorContentSlots();
        MMOItemType[] types = MMOItemType.values();
        for (int index = 0; index < types.length && index < slots.length; index++) {
            MMOItemType type = types[index];
            inventory.setItem(slots[index], menuItem(itemTypeMaterial(type), "&b" + pretty(type.name()), List.of(
                    "&7Changing type updates stat and", "&7enchantment applicability.", "", "&eClick to apply.",
                    "&8Edit Item Type: " + type.name()
            )));
        }
        inventory.setItem(45, menuItem(Material.ARROW, "&cBack", List.of("&8Edit Item Action: identity")));
        inventory.setItem(49, menuItem(Material.BARRIER, "&cClose", List.of("&8Edit Item Action: close")));
        player.openInventory(inventory);
    }

    private void openLoreFlagsEditor(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        Inventory inventory = Bukkit.createInventory(new MenuHolder("edititem:lore_flags"), 54, CC.color("&8Lore and Flags"));
        fillMenu(inventory);
        inventory.setItem(10, menuItem(Material.WRITABLE_BOOK, "&fDescription Lines", List.of(
                "&7Stored separately from generated lore.", "", "&eClick to add a line.", "&eShift Right Click to clear.", "&8Edit Item Description: edit"
        )));
        String[] flags = {"ability_lore", "enchant_lore", "stat_lore", "requirement_lore", "unbreakable", "hide_attributes", "hide_enchants", "hide_unbreakable", "hide_additional_tooltip", "glowing"};
        Material[] icons = {Material.BLAZE_POWDER, Material.ENCHANTED_BOOK, Material.NETHER_STAR, Material.EXPERIENCE_BOTTLE, Material.ANVIL, Material.IRON_SWORD, Material.BOOK, Material.BARRIER, Material.GRAY_DYE, Material.GLOW_INK_SAC};
        for (int index = 0; index < flags.length; index++) addToggleButton(inventory, 19 + index, icons[index], "&e" + pretty(flags[index]), held, flags[index]);
        inventory.setItem(45, menuItem(Material.ARROW, "&cBack", List.of("&8Edit Item Action: back")));
        inventory.setItem(49, menuItem(Material.BARRIER, "&cClose", List.of("&8Edit Item Action: close")));
        player.openInventory(inventory);
    }

    private void openEconomyEditor(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        Inventory inventory = Bukkit.createInventory(new MenuHolder("edititem:economy"), 54, CC.color("&8Edit Economy"));
        fillMenu(inventory);
        double calculated = itemValueService == null ? 0.0D : itemValueService.calculateValue(held);
        inventory.setItem(10, economyValueButton(Material.GOLD_INGOT, "Base Item Value", held, "base_value", calculated));
        inventory.setItem(11, economyValueButton(Material.EMERALD, "Sell Value", held, "sell_price", itemValueService == null ? 0.0D : itemValueService.getSellValue(held)));
        inventory.setItem(12, economyValueButton(Material.GOLD_BLOCK, "Auction Suggested Value", held, "auction_value", itemValueService == null ? 0.0D : itemValueService.getAuctionSuggestedPrice(held)));
        inventory.setItem(13, economyValueButton(Material.SUNFLOWER, "Trade Display Value", held, "trade_value", itemValueService == null ? 0.0D : itemValueService.getTradeDisplayValue(held)));
        addToggleButton(inventory, 19, Material.EMERALD, "&aGuild Sellable", held, "guild_sellable");
        addToggleButton(inventory, 20, Material.GOLD_INGOT, "&6Auctionable", held, "auctionable");
        addToggleButton(inventory, 21, Material.CHEST, "&aTradeable", held, "tradeable");
        addToggleButton(inventory, 22, Material.HOPPER, "&eSellAll Allowed", held, "sellall_allowed");
        addToggleButton(inventory, 23, Material.CHAINMAIL_CHESTPLATE, "&cSoulbound", held, "soulbound");
        inventory.setItem(45, menuItem(Material.ARROW, "&cBack", List.of("&8Edit Item Action: back")));
        inventory.setItem(49, menuItem(Material.BARRIER, "&cClose", List.of("&8Edit Item Action: close")));
        player.openInventory(inventory);
    }

    private void openRequirementEditor(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("edititem:requirements"), 54, CC.color("&8Skill Requirements"));
        fillMenu(inventory);
        String[] skills = {"mining", "woodcutting", "farming", "combat", "fishing", "enchanting", "alchemy"};
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        for (int index = 0; index < skills.length; index++) {
            String skill = skills[index];
            inventory.setItem(slots[index], menuItem(skillRequirementMaterial(skill), "&a" + pretty(skill), List.of(
                    "&7Left Click: &fLevel 5",
                    "&7Right Click: &fLevel 10",
                    "&7Shift Left: &fLevel 25",
                    "&7Shift Right: &fLevel 50",
                    "",
                    "&8Edit Item Skill Requirement: " + skill
            )));
        }
        inventory.setItem(45, menuItem(Material.ARROW, "&cBack", List.of("&8Edit Item Action: back")));
        inventory.setItem(49, menuItem(Material.BARRIER, "&cClose", List.of("&8Edit Item Action: close")));
        player.openInventory(inventory);
    }

    private Material skillRequirementMaterial(String skill) {
        return switch (normalize(skill)) {
            case "mining" -> Material.DIAMOND_PICKAXE;
            case "woodcutting" -> Material.DIAMOND_AXE;
            case "farming" -> Material.WHEAT;
            case "combat" -> Material.DIAMOND_SWORD;
            case "fishing" -> Material.FISHING_ROD;
            case "enchanting" -> Material.ENCHANTING_TABLE;
            case "alchemy" -> Material.BREWING_STAND;
            default -> Material.EXPERIENCE_BOTTLE;
        };
    }

    public boolean tagItemId(ItemStack item, String id) {
        String normalized = normalize(id);
        if (item == null || item.getType().isAir() || normalized.isBlank()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, normalized);
        item.setItemMeta(meta);
        return true;
    }

    public boolean setDisplayName(ItemStack item, String displayName) {
        if (item == null || item.getType().isAir() || displayName == null || displayName.isBlank()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        meta.setDisplayName(CC.color(displayName));
        meta.getPersistentDataContainer().set(displayNameKey, PersistentDataType.STRING, displayName);
        item.setItemMeta(meta);
        return true;
    }

    public boolean addLoreLine(ItemStack item, String line) {
        if (item == null || item.getType().isAir() || line == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(CC.color(line));
        setSafeLore(meta, lore, getItemId(item));
        item.setItemMeta(meta);
        return true;
    }

    public boolean clearLore(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        setSafeLore(meta, new ArrayList<>(), getItemId(item));
        item.setItemMeta(meta);
        return true;
    }

    public boolean setRarity(ItemStack item, String rarity) {
        if (item == null || item.getType().isAir() || rarity == null || rarity.isBlank()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, normalize(rarity).toUpperCase(Locale.ROOT));
        meta.getPersistentDataContainer().set(itemRarityKey, PersistentDataType.STRING, normalize(rarity).toUpperCase(Locale.ROOT));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "rolled_rarity"), PersistentDataType.STRING,
                normalize(rarity).toUpperCase(Locale.ROOT));
        item.setItemMeta(meta);
        rebuildMmoLore(item, mmoStatStorage.read(item));
        return true;
    }

    public boolean setItemType(ItemStack item, String itemType) {
        if (item == null || item.getType().isAir() || itemType == null || itemType.isBlank()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, normalize(itemType));
        item.setItemMeta(meta);
        mmoStatStorage.write(item, MMOItemType.parse(itemType), mmoStatStorage.read(item));
        sanitizeBlockedRandomEffects(item, getItemId(item));
        rebuildMmoLore(item, mmoStatStorage.read(item));
        return true;
    }

    public boolean setMaterial(ItemStack item, String materialName) {
        if (item == null || item.getType().isAir()) return false;
        Material material = parseMaterial(materialName);
        if (material == Material.AIR || !material.isItem()) return false;
        item.setType(material);
        return true;
    }

    public boolean setStat(ItemStack item, String stat, double amount) {
        if (item == null || item.getType().isAir() || stat == null || stat.isBlank() || !Double.isFinite(amount)) return false;
        MMOStatType type = MMOStatType.parse(stat);
        if (type == null) return false;
        MMOStatMap stats = mmoStatStorage.read(item);
        if (amount <= 0.0D) stats.remove(type);
        else stats.set(type, amount);
        mmoStatStorage.write(item, getMmoItemType(item), stats);
        rebuildMmoLore(item, stats);
        return true;
    }

    public boolean adjustStat(ItemStack item, String stat, double delta, boolean reset) {
        MMOStatType type = MMOStatType.parse(stat);
        if (type == null || item == null || item.getType().isAir()) return false;
        double next = reset ? 0.0D : Math.max(0.0D, mmoStatStorage.read(item).get(type) + delta);
        return setStat(item, type.name(), next);
    }

    public EditorResult adjustEnchantment(ItemStack item, String source, String id, boolean increase, boolean shift) {
        if (item == null || item.getType().isAir()) return new EditorResult(false, "&cHold a valid item first.");
        boolean custom = normalize(source).equals("custom");
        EditorEnchant option = editorEnchantments().stream().filter(value -> value.custom() == custom && value.id().equals(normalize(id))).findFirst().orElse(null);
        if (option == null) return new EditorResult(false, "&cUnknown enchantment.");
        if (!canApplyEditorEnchant(item, option)) return new EditorResult(false, "&cThat enchantment cannot be applied to this item type.");
        Map<String, Integer> current = mmoEnchantStorage.read(item);
        int oldLevel = current.getOrDefault(option.id(), 0);
        int next = shift ? (increase ? option.maxLevel() : 0) : Math.clamp(oldLevel + (increase ? 1 : -1), 0, option.maxLevel());
        if (custom) {
            CustomEnchantManager.ApplyResult result = customEnchantManager.setEnchantLevel(item, option.id(), next);
            if (!result.success()) return new EditorResult(false, result.message());
        } else {
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(option.id()));
            if (enchantment == null) return new EditorResult(false, "&cUnknown vanilla enchantment.");
            if (next > 0) {
                for (String existingId : current.keySet()) {
                    Enchantment existing = Enchantment.getByKey(NamespacedKey.minecraft(existingId));
                    if (existing != null && !existing.equals(enchantment) && existing.conflictsWith(enchantment)) {
                        return new EditorResult(false, "&cThat enchantment conflicts with " + pretty(existingId) + ".");
                    }
                }
                current.put(option.id(), next);
            } else current.remove(option.id());
            mmoEnchantStorage.write(item, current);
            mmoEnchantStorage.removeVanillaEnchants(item);
        }
        rebuildMmoLore(item, mmoStatStorage.read(item));
        return new EditorResult(true, "&aUpdated &f" + option.name() + " &ato level &f" + next + "&a.");
    }

    public record EditorResult(boolean success, String message) {
    }

    public boolean setAbility(ItemStack item, String abilityId) {
        if (item == null || item.getType().isAir() || abilityId == null || abilityId.isBlank()) return false;
        if (!canRollRandomRarityEffects(item)) return false;
        CustomItemAbility registered = abilityRegistry.get(abilityId).orElse(null);
        if (registered == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String id = getItemId(item);
        if (id != null && !canHaveRightClickAbility(id, item) && registered.trigger() == AbilityTrigger.RIGHT_CLICK) return false;
        if (!isAbilityValidForItem(registered.id(), item)) return false;
        JsonObject ability = jsonObject(meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING));
        ability.addProperty("id", registered.id());
        ability.addProperty("trigger", registered.trigger().name());
        ability.addProperty("mana_cost", registered.manaCost());
        ability.addProperty("cooldown_seconds", registered.cooldownSeconds() > 0 ? registered.cooldownSeconds() : defaultToolAbilityCooldown(registered.id()));
        meta.getPersistentDataContainer().set(abilityKey, PersistentDataType.STRING, GSON.toJson(ability));
        meta.getPersistentDataContainer().set(abilityIdKey, PersistentDataType.STRING, normalize(registered.id()));
        item.setItemMeta(meta);
        rebuildMmoLore(item, mmoStatStorage.read(item));
        return true;
    }

    public boolean setAbilityValue(ItemStack item, String key, String value) {
        if (item == null || item.getType().isAir() || key == null || value == null) return false;
        if (!canRollRandomRarityEffects(item)) return false;
        String normalizedKey = normalize(key);
        if (!List.of("trigger", "mana_cost").contains(normalizedKey)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        JsonObject ability = jsonObject(meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING));
        if (normalizedKey.equals("trigger")) {
            ability.addProperty("trigger", value.toUpperCase(Locale.ROOT));
        } else {
            try {
                ability.addProperty(normalizedKey, Math.max(0, Integer.parseInt(value)));
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        meta.getPersistentDataContainer().set(abilityKey, PersistentDataType.STRING, GSON.toJson(ability));
        item.setItemMeta(meta);
        rebuildMmoLore(item, mmoStatStorage.read(item));
        return true;
    }

    public boolean setFlag(ItemStack item, String flag, boolean enabled) {
        if (item == null || item.getType().isAir() || flag == null || flag.isBlank()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String normalized = normalize(flag);
        JsonObject flags = jsonObject(meta.getPersistentDataContainer().get(flagsKey, PersistentDataType.STRING));
        flags.addProperty(normalized, enabled);
        if (normalized.equals("unbreakable")) meta.setUnbreakable(enabled);
        applyEditorItemFlag(meta, normalized, enabled);
        meta.getPersistentDataContainer().set(flagsKey, PersistentDataType.STRING, GSON.toJson(flags));
        item.setItemMeta(meta);
        if (normalized.equals("glowing")) {
            applyVisualGlow(item, enabled);
        }
        return true;
    }

    public boolean toggleFlag(ItemStack item, String flag) {
        return setFlag(item, flag, !getFlag(item, flag));
    }

    public boolean getFlag(ItemStack item, String flag) {
        if (item == null || !item.hasItemMeta()) return false;
        JsonObject flags = jsonObject(item.getItemMeta().getPersistentDataContainer().get(flagsKey, PersistentDataType.STRING));
        String key = normalize(flag);
        if (flags.has(key) && flags.get(key).isJsonPrimitive()) return flags.get(key).getAsBoolean();
        return switch (key) {
            case "ability_lore", "enchant_lore", "stat_lore", "requirement_lore", "unbreakable",
                    "hide_attributes", "hide_enchants", "hide_unbreakable", "hide_additional_tooltip",
                    "guild_sellable", "auctionable", "tradeable", "sellall_allowed" -> true;
            default -> false;
        };
    }

    public boolean setEditorTextValue(ItemStack item, String field, String value) {
        String key = normalize(field);
        if (key.equals("id")) return tagItemId(item, value);
        if (key.equals("name")) return setDisplayName(item, value);
        if (key.equals("material")) return setMaterial(item, value);
        if (item == null || item.getType().isAir() || value == null || value.isBlank()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (key.equals("model")) {
            try {
                meta.setCustomModelData(Math.max(0, Integer.parseInt(value)));
            } catch (NumberFormatException ignored) {
                return false;
            }
        } else if (key.equals("category") || key.equals("requirement_profile")) {
            JsonObject nbt = jsonObject(meta.getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING));
            nbt.addProperty(key, value);
            meta.getPersistentDataContainer().set(nbtKey, PersistentDataType.STRING, GSON.toJson(nbt));
        } else return false;
        item.setItemMeta(meta);
        rebuildMmoLore(item, mmoStatStorage.read(item));
        return true;
    }

    public boolean editDescription(ItemStack item, String line, boolean clear) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        JsonObject nbt = jsonObject(meta.getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING));
        com.google.gson.JsonArray description = clear || !nbt.has("description") || !nbt.get("description").isJsonArray()
                ? new com.google.gson.JsonArray() : nbt.getAsJsonArray("description");
        if (!clear && line != null && !line.isBlank()) description.add(line);
        nbt.add("description", description);
        meta.getPersistentDataContainer().set(nbtKey, PersistentDataType.STRING, GSON.toJson(nbt));
        List<String> lore = new ArrayList<>();
        for (JsonElement element : description) if (element.isJsonPrimitive()) lore.add(CC.color(element.getAsString()));
        setSafeLore(meta, lore, getItemId(item));
        item.setItemMeta(meta);
        rebuildMmoLore(item, mmoStatStorage.read(item));
        return true;
    }

    public boolean adjustEconomyValue(ItemStack item, String field, double delta, boolean reset) {
        if (item == null || item.getType().isAir() || !Double.isFinite(delta)) return false;
        String key = normalize(field);
        double current = editorEconomyValue(item, key, 0.0D);
        double value = reset ? 0.0D : Math.max(0.0D, current + delta);
        if (key.equals("sell_price")) return setSellPrice(item, value);
        if (!List.of("base_value", "auction_value", "trade_value").contains(key)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        JsonObject nbt = jsonObject(meta.getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING));
        nbt.addProperty(key, value);
        meta.getPersistentDataContainer().set(nbtKey, PersistentDataType.STRING, GSON.toJson(nbt));
        item.setItemMeta(meta);
        return true;
    }

    public boolean setSellPrice(ItemStack item, double price) {
        if (item == null || item.getType().isAir() || !Double.isFinite(price) || price < 0) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        JsonObject nbt = jsonObject(meta.getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING));
        nbt.addProperty("sell_price", price);
        meta.getPersistentDataContainer().set(sellPriceKey, PersistentDataType.DOUBLE, price);
        meta.getPersistentDataContainer().set(nbtKey, PersistentDataType.STRING, GSON.toJson(nbt));
        item.setItemMeta(meta);
        return true;
    }

    public boolean setRequirement(ItemStack item, String key, String value) {
        if (item == null || item.getType().isAir() || key == null || key.isBlank() || value == null || value.isBlank()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        int level;
        try {
            level = Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return false;
        }
        String requirement = normalize(key);
        if (requirement.startsWith("skill_") && requirement.endsWith("_level")) {
            requirement = requirement.substring(6, requirement.length() - 6);
        }
        requirement = normalizeRequirementSkill(requirement);
        JsonObject requirements = jsonObject(meta.getPersistentDataContainer().get(requirementsKey, PersistentDataType.STRING));
        if (level == 0) requirements.remove(requirement); else requirements.addProperty(requirement, level);
        if (requirements.isEmpty()) meta.getPersistentDataContainer().remove(requirementsKey);
        else meta.getPersistentDataContainer().set(requirementsKey, PersistentDataType.STRING, GSON.toJson(requirements));
        meta.getPersistentDataContainer().set(requirementsRolledKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        rebuildMmoLore(item, mmoStatStorage.read(item));
        return true;
    }

    public boolean setVanillaEnchantment(ItemStack item, String enchantmentId, int level) {
        if (item == null || item.getType().isAir() || enchantmentId == null || enchantmentId.isBlank()) return false;
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(normalize(enchantmentId)));
        if (enchantment == null) enchantment = Enchantment.getByName(enchantmentId.toUpperCase(Locale.ROOT));
        if (enchantment == null) return false;
        if (getItemId(item) != null) {
            String customId = MMOEnchantStorage.fromVanilla(enchantment);
            if (customId == null) return false;
            Map<String, Integer> enchants = mmoEnchantStorage.read(item);
            enchants.merge(customId, Math.max(1, level), Math::max);
            mmoEnchantStorage.write(item, enchants);
            cleanVanillaEnchants(item);
            return true;
        }
        item.addUnsafeEnchantment(enchantment, Math.max(1, level));
        return true;
    }

    public SaveResult saveDatabaseItem(String id, ItemStack item, String actor, boolean allowStaticOverride) {
        String key = normalize(id);
        if (key.isBlank() || key.length() > 64 || !key.matches("[a-z0-9_]+")) return SaveResult.INVALID_ID;
        if (item == null || item.getType().isAir() || item.getItemMeta() == null) return SaveResult.INVALID_ITEM;
        if (!dataManager.isMySqlEnabled()) return SaveResult.DATABASE_DISABLED;
        if (isStaticItem(key) && !allowStaticOverride) return SaveResult.STATIC_ID;

        ItemMeta meta = item.getItemMeta();
        String displayName = meta.hasDisplayName() ? meta.getDisplayName() : pretty(key);
        List<String> lore = meta.hasLore() && meta.getLore() != null ? meta.getLore() : List.of();
        String itemType = valueOr(meta.getPersistentDataContainer().get(itemTypeKey, PersistentDataType.STRING), "misc");
        String rarity = valueOr(meta.getPersistentDataContainer().get(rarityKey, PersistentDataType.STRING), "COMMON");
        JsonObject storedStats = new JsonObject();
        mmoStatStorage.read(item).values().forEach((stat, value) -> storedStats.addProperty(stat.name(), value));
        String statsJson = GSON.toJson(storedStats);
        String abilitiesJson = validJson(meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING));
        String flagsJson = validJson(meta.getPersistentDataContainer().get(flagsKey, PersistentDataType.STRING));
        String nbtJson = validJson(meta.getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING));

        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO `smp_custom_items` (`id`, `item_type`, `material`, `display_name`, `lore`, `rarity`, `stats_json`, `abilities_json`, `flags_json`, `nbt_json`, `created_by`, `updated_by`) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `item_type`=VALUES(`item_type`), `material`=VALUES(`material`), `display_name`=VALUES(`display_name`), " +
                        "`lore`=VALUES(`lore`), `rarity`=VALUES(`rarity`), `stats_json`=VALUES(`stats_json`), `abilities_json`=VALUES(`abilities_json`), " +
                        "`flags_json`=VALUES(`flags_json`), `nbt_json`=VALUES(`nbt_json`), `updated_by`=VALUES(`updated_by`), `updated_at`=CURRENT_TIMESTAMP")) {
            statement.setString(1, key);
            statement.setString(2, trim(itemType, 32));
            statement.setString(3, item.getType().name());
            statement.setString(4, trim(displayName, 128));
            statement.setString(5, GSON.toJson(lore));
            statement.setString(6, trim(rarity, 32));
            statement.setString(7, statsJson);
            statement.setString(8, abilitiesJson);
            statement.setString(9, flagsJson);
            statement.setString(10, nbtJson);
            statement.setString(11, trim(actor, 36));
            statement.setString(12, trim(actor, 36));
            statement.executeUpdate();
            tagItemId(item, key);
            loadDatabaseItems();
            plugin.getLogger().info("Custom item saved by " + actor + ": " + key);
            return SaveResult.SAVED;
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to save custom item '" + key + "': " + exception.getMessage());
            return SaveResult.ERROR;
        }
    }

    public boolean deleteDatabaseItem(String id, String actor) {
        String key = normalize(id);
        if (key.isBlank() || !databaseItems.containsKey(key) || !dataManager.isMySqlEnabled()) return false;
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM `smp_custom_items` WHERE `id`=?")) {
            statement.setString(1, key);
            statement.executeUpdate();
            databaseItems.remove(key);
            plugin.getLogger().info("Custom item deleted by " + actor + ": " + key);
            return true;
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to delete custom item '" + key + "': " + exception.getMessage());
            return false;
        }
    }

    public boolean hasUsableAbility(ItemStack item) {
        String id = getItemId(item);
        if (id == null || !itemExists(id)) return false;
        String ability = getAbilityType(item, id);
        return isConsumableId(id) || canHaveRightClickAbility(id, item)
                && abilityRegistry.get(ability).map(a -> a.trigger() == AbilityTrigger.RIGHT_CLICK).orElse(!ability.isBlank() && !ability.equalsIgnoreCase("none"));
    }

    public boolean isBackpack(ItemStack item) {
        String id = getItemId(item);
        return id != null && itemExists(id) && isBackpackId(id);
    }

    public boolean isFishingBag(ItemStack item) {
        String id = getItemId(item);
        return id != null && itemExists(id) && isFishingBagId(id);
    }

    public boolean openBackpack(Player player, ItemStack item) {
        if (!isBackpack(item)) return false;
        if (item.getAmount() > 1) item.setAmount(1);
        String id = getItemId(item);
        String uuid = backpackUuid(item);
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(backpackKey, PersistentDataType.STRING, uuid);
                item.setItemMeta(meta);
            }
        }
        int size = backpackSize(id);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("backpack:" + uuid), size,
                CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".name", "&6Backpack")));
        List<?> saved = dataManager.getData().getList("backpacks." + uuid + ".contents");
        if (saved != null) {
            for (int i = 0; i < Math.min(size, saved.size()); i++) {
                if (saved.get(i) instanceof ItemStack savedItem) inventory.setItem(i, savedItem);
            }
        }
        player.openInventory(inventory);
        return true;
    }

    public void saveBackpack(String uuid, Inventory inventory) {
        if (uuid == null || uuid.isBlank()) return;
        dataManager.getData().set("backpacks." + uuid + ".contents", java.util.Arrays.asList(inventory.getContents()));
        dataManager.save();
    }

    public boolean openFishingBag(Player player, ItemStack item) {
        if (!isFishingBag(item)) return false;
        if (item.getAmount() > 1) item.setAmount(1);
        String uuid = ensureFishingBagUuid(item);
        if (uuid == null) return false;
        Inventory inventory = Bukkit.createInventory(new MenuHolder("fishingbag:" + uuid), 27, CC.color("&8Fishing Bag"));
        inventory.setItem(11, menuItem(Material.FISHING_ROD, "&bBait Storage", List.of(
                "&7Store fishing bait here.",
                "&eClick to open.",
                "&8Fishing Bag Action: bait"
        )));
        inventory.setItem(15, menuItem(Material.CHEST, "&6Fishing Drops", List.of(
                "&7View drops recorded from fishing.",
                "&eClick to open.",
                "&8Fishing Bag Action: drops"
        )));
        fillMenu(inventory);
        player.openInventory(inventory);
        return true;
    }

    public void openFishingBagStorage(Player player, String uuid, String type) {
        if (uuid == null || uuid.isBlank()) return;
        String normalized = type == null || !type.equalsIgnoreCase("drops") ? "bait" : "drops";
        int size = normalized.equals("drops") ? 54 : 27;
        Inventory inventory = Bukkit.createInventory(new MenuHolder("fishingbag-" + normalized + ":" + uuid), size,
                CC.color(normalized.equals("drops") ? "&8Fishing Drops" : "&8Fishing Bait"));
        List<?> saved = dataManager.getData().getList("fishing-bags." + uuid + "." + normalized);
        if (saved != null) {
            for (int i = 0; i < Math.min(size, saved.size()); i++) {
                if (saved.get(i) instanceof ItemStack savedItem) inventory.setItem(i, savedItem);
            }
        }
        player.openInventory(inventory);
    }

    public void saveFishingBag(String uuid, String type, Inventory inventory) {
        if (uuid == null || uuid.isBlank() || type == null || type.isBlank()) return;
        dataManager.getData().set("fishing-bags." + uuid + "." + type, java.util.Arrays.asList(inventory.getContents()));
        dataManager.save();
    }

    public void recordFishingDrop(Player player, ItemStack drop) {
        if (drop == null || drop.getType().isAir()) return;
        ItemStack bag = firstFishingBag(player);
        if (bag == null) return;
        String uuid = ensureFishingBagUuid(bag);
        if (uuid == null) return;
        int size = 54;
        List<ItemStack> contents = new ArrayList<>();
        List<?> saved = dataManager.getData().getList("fishing-bags." + uuid + ".drops");
        if (saved != null) {
            for (Object object : saved) contents.add(object instanceof ItemStack item ? item : null);
        }
        while (contents.size() < size) contents.add(null);
        ItemStack one = drop.clone();
        one.setAmount(Math.clamp(one.getAmount(), 1, one.getMaxStackSize()));
        for (int i = 0; i < size; i++) {
            ItemStack current = contents.get(i);
            if (current != null && current.isSimilar(one) && current.getAmount() < current.getMaxStackSize()) {
                current.setAmount(Math.min(current.getMaxStackSize(), current.getAmount() + one.getAmount()));
                dataManager.getData().set("fishing-bags." + uuid + ".drops", contents);
                dataManager.save();
                return;
            }
            if (current == null || current.getType().isAir()) {
                contents.set(i, one);
                dataManager.getData().set("fishing-bags." + uuid + ".drops", contents);
                dataManager.save();
                return;
            }
        }
    }

    public Material getMaterial(String id) {
        DatabaseCustomItem databaseItem = databaseItems.get(normalize(id));
        if (databaseItem != null) return parseMaterial(databaseItem.material());
        try {
            return Material.valueOf(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".material", "PAPER").toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return Material.PAPER;
        }
    }

    public boolean useAbility(Player player, ItemStack item) {
        return useAbility(player, item, player != null && player.isSneaking()
                ? AbilityTrigger.SHIFT_RIGHT_CLICK : AbilityTrigger.RIGHT_CLICK);
    }

    public boolean useAbility(Player player, ItemStack item, AbilityTrigger trigger) {
        if (isAbilityBlockedItem(item)) return false;
        String id = getItemId(item);
        if (id == null || !itemExists(id)) return false;
        if (useConsumable(player, item, id)) return true;
        if (!meetsItemRequirements(player, item, true)) return true;
        if (!canHaveRightClickAbility(id, item)) return false;
        String ability = getAbilityType(id);
        if (ability.isBlank() || ability.equalsIgnoreCase("none")) return false;
        CustomItemAbility registeredAbility = abilityRegistry.get(getAbilityType(item, id)).orElse(null);
        if (registeredAbility != null) {
            if (activateAbility(player, item, trigger, null, null, 0.0D, null)) return true;
            return false;
        }
        int cost = getManaCost(id);
        if (isAbilityCoolingDown(player, item, id, ability)) return true;
        if (manaActionBarManager.getMana(player) < cost) {
            player.sendMessage(CC.color("&cYou do not have enough mana!"));
            return true;
        }
        boolean executed;
        if (Set.of("heal", "healing", "instant_heal").contains(normalize(ability))) {
            executed = healMmoPlayer(player, id);
        } else {
            runAbility(player, id, ability);
            executed = true;
        }
        if (!executed) return true;
        if (!takeMana(player, cost)) return true;
        markAbilityCooldown(player, item, id, ability);
        player.sendMessage(CC.color("&bMana: &f" + manaActionBarManager.getMana(player) + "&7/&f" + manaActionBarManager.getMaxMana(player)));
        return true;
    }

    public boolean isConsumableItem(ItemStack item) {
        String id = getItemId(item);
        return id != null && isConsumableId(id);
    }

    public boolean isBlockedCustomConsumable(ItemStack item) {
        String id = getItemId(item);
        if (id == null) return false;
        String type = configuredCustomItemType(id);
        return id.equals("developer_notebook") || Set.of("DEVELOPER_NOTEBOOK", "COOKING_INGREDIENT", "FISHING_BAIT", "BAIT")
                .contains(type) || ((type.equals("CUSTOM_FISH") || type.equals("FISH"))
                && !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(path(id) + ".food.edible", false));
    }

    public MMOStatMap getTemporaryConsumableStats(Player player) {
        MMOStatMap result = MMOStatMap.empty();
        if (player == null) return result;
        Map<MMOStatType, TimedStatBuff> buffs = consumableStatBuffs.get(player.getUniqueId());
        if (buffs == null) return result;
        long now = System.currentTimeMillis();
        buffs.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        if (buffs.isEmpty()) consumableStatBuffs.remove(player.getUniqueId());
        else buffs.forEach((type, buff) -> result.add(type, buff.amount()));
        return result;
    }

    public boolean activateAbility(Player player, ItemStack item, AbilityTrigger trigger, LivingEntity target, org.bukkit.Location location, double damage, EntityDamageEvent damageEvent) {
        return activateAbility(player, item, trigger, target, location, damage, damageEvent, null);
    }

    public boolean activateAbility(Player player, ItemStack item, AbilityTrigger trigger, LivingEntity target, org.bukkit.Location location,
                                   double damage, EntityDamageEvent damageEvent, Projectile projectile) {
        if (isAbilityBlockedItem(item)) return false;
        if (!(plugin instanceof Game game) || player == null || item == null || item.getType().isAir()) return false;
        String itemId = getItemId(item);
        if (itemId == null || !itemExists(itemId)) return false;
        if (!meetsItemRequirements(player, item, true)) return true;
        CustomItemAbility ability = abilityRegistry.get(getAbilityType(item, itemId)).orElse(null);
        if (!canHaveRightClickAbility(itemId, item)) return false;
        AbilityTrigger configuredTrigger = ability == null ? null : abilityTrigger(item, ability);
        if (ability == null || (!triggersMatch(configuredTrigger, trigger)
                && !(ability.id().equals("ranger_focus") && (trigger == AbilityTrigger.LEFT_CLICK
                || trigger == AbilityTrigger.LEFT_CLICK_AIR || trigger == AbilityTrigger.LEFT_CLICK_BLOCK
                || trigger == AbilityTrigger.PROJECTILE_HIT)))) return false;
        if (!ability.allowedItemTypes().isEmpty() && !ability.allowedItemTypes().contains(getMmoItemType(item))) return false;
        if (!player.hasPermission("smp.customitem.abilities") && !player.hasPermission("smp.edititem")) {
            player.sendMessage(CC.color("&cYou cannot use custom item abilities."));
            return true;
        }
        if (isAbilityCoolingDown(player, item, itemId, ability.id())) return true;

        int manaCost = Math.max(0, abilityManaCost(item, itemId, ability) - passiveManaReduction(player));
        if (manaActionBarManager.getMana(player) < manaCost) {
            player.sendMessage(CC.color(ability.id().equals("blood_needle")
                    ? "&cYou do not have enough Mana."
                    : "&cYou do not have enough mana!"));
            return true;
        }

        AbilityContext context = new AbilityContext(game, player, item, itemId, trigger, target, location, damage, damageEvent, projectile);
        final boolean[] executed = {false};
        try {
            MMODamageSourceTracker.runAs(isActivationTrigger(trigger)
                            ? MMODamageSourceType.ABILITY : MMODamageSourceType.MELEE,
                    () -> executed[0] = ability.execute(context));
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Custom ability '" + ability.id() + "' failed: " + throwable.getMessage());
            return true;
        }
        if (!executed[0]) return false;
        manaActionBarManager.spendMana(player, manaCost);
        markAbilityCooldown(player, item, itemId, ability.id());
        lastAbilityCasts.put(player.getUniqueId(), ability.id() + " trigger=" + trigger.name() + " item=" + itemId
                + " mana=" + manaCost + " at=" + System.currentTimeMillis());
        return true;
    }

    public boolean activateFromEquipment(Player player, AbilityTrigger trigger, LivingEntity target, org.bukkit.Location location, double damage, EntityDamageEvent damageEvent) {
        for (ItemStack item : abilityItems(player)) {
            if (activateAbility(player, item, trigger, target, location, damage, damageEvent)) return true;
        }
        return false;
    }

    public double getMana(Player player) {
        return manaActionBarManager.getMana(player);
    }

    public void grantTemporaryStatBuff(Player player, Map<MMOStatType, Double> stats, int durationSeconds) {
        if (player == null || stats == null || stats.isEmpty()) return;
        long now = System.currentTimeMillis(), expiresAt = now + Math.max(1, durationSeconds) * 1000L;
        Map<MMOStatType, TimedStatBuff> active = consumableStatBuffs.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        stats.forEach((type, amount) -> {
            if (type == null || amount == null || amount <= 0.0D) return;
            TimedStatBuff current = active.get(type);
            if (current == null || current.expiresAt() <= now || amount >= current.amount()) active.put(type, new TimedStatBuff(amount, expiresAt));
            else active.put(type, new TimedStatBuff(current.amount(), Math.max(current.expiresAt(), expiresAt)));
        });
        if (plugin instanceof Game game && game.getMmoStatService() != null) game.getMmoStatService().refreshPlayerMMOStats(player);
    }

    public void clearAbilityCooldowns(Player player) {
        if (player == null) return;
        String prefix = player.getUniqueId() + ":";
        legacyAbilityCooldowns.keySet().removeIf(key -> key.startsWith(prefix));
        club.abelix.games.fakevival.abilities.WeaponAbilityExecutor.clearPlayerState(player);
    }

    public String debugLastAbilityCast(Player player) {
        return player == null ? "none" : lastAbilityCasts.getOrDefault(player.getUniqueId(), "none");
    }

    public String heldAbilityId(Player player) {
        if (player == null) return "none";
        ItemStack item = player.getInventory().getItemInMainHand();
        String id = getItemId(item);
        return id == null ? "none" : getAbilityType(item, id);
    }

    public String getAbilityId(ItemStack item) {
        String id = getItemId(item);
        return id == null ? null : getAbilityType(item, id);
    }

    private boolean triggersMatch(AbilityTrigger configured, AbilityTrigger actual) {
        if (configured == null || actual == null) return false;
        if (configured == actual) return true;
        return switch (configured) {
            case RIGHT_CLICK -> actual == AbilityTrigger.RIGHT_CLICK_AIR || actual == AbilityTrigger.RIGHT_CLICK_BLOCK;
            case LEFT_CLICK -> actual == AbilityTrigger.LEFT_CLICK_AIR || actual == AbilityTrigger.LEFT_CLICK_BLOCK
                    || actual == AbilityTrigger.SHIFT_LEFT_CLICK;
            case SHIFT_RIGHT_CLICK -> actual == AbilityTrigger.SHIFT_RIGHT_CLICK;
            case SNEAK -> actual == AbilityTrigger.SNEAK_TOGGLE;
            case HIT_ENTITY, ON_HIT -> actual == AbilityTrigger.ENTITY_HIT;
            default -> false;
        };
    }

    private boolean isActivationTrigger(AbilityTrigger trigger) {
        return trigger == AbilityTrigger.RIGHT_CLICK || trigger == AbilityTrigger.RIGHT_CLICK_AIR
                || trigger == AbilityTrigger.RIGHT_CLICK_BLOCK || trigger == AbilityTrigger.SHIFT_RIGHT_CLICK
                || trigger == AbilityTrigger.BOW_SHOOT || trigger == AbilityTrigger.PROJECTILE_HIT;
    }

    public List<String> validateAbilityRegistry() {
        List<String> lines = new ArrayList<>();
        long weaponAbilities = abilityRegistry.all().stream().filter(ability -> !ability.allowedItemTypes().isEmpty()).count();
        lines.add((abilityRegistry.all().size() >= 100 ? "&a" : "&c") + "Ability registry entries: &f" + abilityRegistry.all().size() + "&7/100 minimum");
        lines.add((weaponAbilities == 60 ? "&a" : "&c") + "New weapon abilities: &f" + weaponAbilities + "&7/60");
        lines.add((abilityRegistry.duplicateIds().isEmpty() ? "&a" : "&c") + "Duplicate ability IDs: &f"
                + (abilityRegistry.duplicateIds().isEmpty() ? "none" : String.join(", ", abilityRegistry.duplicateIds())));
        abilityRegistry.all().stream().filter(ability -> ability.trigger() == null)
                .forEach(ability -> lines.add("&cMissing trigger: &f" + ability.id()));
        abilityRegistry.all().stream().filter(ability -> defaultToolAbilityCooldown(ability.id()) <= 0)
                .forEach(ability -> lines.add("&cMissing cooldown: &f" + ability.id()));
        abilityRegistry.all().stream().filter(ability -> ability.manaCost() < 0)
                .forEach(ability -> lines.add("&cInvalid mana cost: &f" + ability.id()));
        for (String itemId : getItemIds()) {
            String abilityId = getAbilityType(itemId);
            if (!abilityId.isBlank() && !abilityId.equalsIgnoreCase("none") && !abilityRegistry.contains(abilityId))
                lines.add("&cItem has unregistered ability: &f" + itemId + " &7-> &f" + abilityId);
        }
        lines.add("&aPDC source, main-hand guard, preview/admin/notebook guards: enabled");
        return lines;
    }

    public boolean testAbility(Player player, String abilityId) {
        CustomItemAbility ability = abilityRegistry.get(abilityId).orElse(null);
        if (player == null || ability == null) return false;
        String itemId = getItemIds().stream().filter(id -> ability.allowedItemTypes().isEmpty()
                || ability.allowedItemTypes().contains(configuredMmoItemType(id))).findFirst().orElse(null);
        ItemStack item = itemId == null ? player.getInventory().getItemInMainHand() : createItem(itemId, 1);
        if (item == null || item.getType().isAir() || getItemId(item) == null) return false;
        if (!setAbility(item, ability.id())) return false;
        LivingEntity target = player.getNearbyEntities(16, 8, 16).stream().filter(Monster.class::isInstance)
                .map(LivingEntity.class::cast).findFirst().orElse(null);
        Projectile projectile = ability.trigger() == AbilityTrigger.BOW_SHOOT || ability.trigger() == AbilityTrigger.PROJECTILE_HIT
                ? player.launchProjectile(org.bukkit.entity.Arrow.class) : null;
        return activateAbility(player, item, ability.trigger(), target,
                target == null ? player.getLocation() : target.getLocation(), 1.0D, null, projectile);
    }

    public boolean testHeldItemAbility(Player player) {
        if (player == null) return false;
        ItemStack item = player.getInventory().getItemInMainHand();
        String itemId = getItemId(item);
        if (itemId == null) return false;
        CustomItemAbility ability = abilityRegistry.get(getAbilityType(item, itemId)).orElse(null);
        if (ability == null) return false;
        LivingEntity target = player.getNearbyEntities(16, 8, 16).stream()
                .filter(entity -> entity instanceof Monster).map(entity -> (LivingEntity) entity)
                .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(player.getLocation()))).orElse(null);
        Projectile projectile = null;
        if (ability.trigger() == AbilityTrigger.BOW_SHOOT || ability.trigger() == AbilityTrigger.PROJECTILE_HIT) {
            projectile = player.launchProjectile(org.bukkit.entity.Arrow.class);
        }
        return activateAbility(player, item, ability.trigger(), target, target == null ? player.getLocation() : target.getLocation(),
                1.0D, null, projectile);
    }

    public int giveAbilityTestItems(Player player) {
        if (player == null) return 0;
        int given = 0;
        for (MMOItemType type : List.of(MMOItemType.SWORD, MMOItemType.KATANA, MMOItemType.DAGGER, MMOItemType.BOW, MMOItemType.WAND, MMOItemType.STAFF)) {
            CustomItemAbility ability = abilityRegistry.all().stream().filter(value -> value.allowedItemTypes().contains(type)).findFirst().orElse(null);
            if (ability == null) continue;
            String itemId = getItemIds().stream().filter(id -> configuredMmoItemType(id) == type).findFirst().orElse(null);
            if (itemId == null) continue;
            ItemStack item = createItem(itemId, 1);
            if (item == null || item.getType().isAir() || !setAbility(item, ability.id())) continue;
            player.getInventory().addItem(item);
            given++;
        }
        return given;
    }

    private boolean takeMana(Player player, int amount) {
        return manaActionBarManager.spendMana(player, Math.max(0, amount));
    }

    private boolean useConsumable(Player player, ItemStack item, String id) {
        if (!isConsumableId(id)) return false;
        String base = path(id) + ".consumable";
        long now = System.currentTimeMillis();
        String cooldownKey = player.getUniqueId() + ":" + normalize(id);
        long readyAt = consumableCooldowns.getOrDefault(cooldownKey, 0L);
        if (readyAt > now) {
            String kind = configuredMmoItemType(id) == MMOItemType.FOOD ? "food" : "potion";
            player.sendMessage(CC.color("&cThat " + kind + " is on cooldown for &e" + Math.max(1L, (readyAt - now + 999L) / 1000L) + "s&c."));
            return true;
        }
        MMOStatMap itemStats = getMmoStats(item);
        double heal = Math.max(Math.max(0.0D, itemStats.get(MMOStatType.HEALTH_RESTORE)), club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(base + ".heal", 0.0D));
        double mana = Math.max(Math.max(0.0D, itemStats.get(MMOStatType.MANA_RESTORE)), club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(base + ".mana", 0.0D));
        boolean customFood = Set.of("CUSTOM_FOOD", "FOOD").contains(configuredCustomItemType(id));
        int food = Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(base + ".food",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(id) + ".food.hungerRestore", customFood ? 4 : 0)));
        float saturation = (float) Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(base + ".saturation",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".food.saturationRestore", customFood ? 2.4D : 0.0D)));
        if (heal > 0.0D) {
            if (plugin instanceof Game game && game.getMmoStatService() != null) game.getMmoStatService().healMmoHealth(player, heal);
            else player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
        }
        if (mana > 0.0D) manaActionBarManager.setMana(player, manaActionBarManager.getMana(player) + (int) Math.round(mana));
        if (food > 0) player.setFoodLevel(Math.min(20, player.getFoodLevel() + food));
        if (saturation > 0.0F) player.setSaturation(Math.min(20.0F, player.getSaturation() + saturation));

        ConfigurationSection effects = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection(base + ".effects");
        if (effects != null) {
            for (String effectName : effects.getKeys(false)) {
                PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase(Locale.ROOT));
                if (type == null) continue;
                int duration = Math.max(1, effects.getInt(effectName + ".duration-seconds", 15)) * 20;
                int amplifier = Math.max(0, effects.getInt(effectName + ".amplifier", 0));
                player.addPotionEffect(new PotionEffect(type, duration, amplifier));
            }
        }
        for (String effectName : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(base + ".clear-effects")) {
            PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase(Locale.ROOT));
            if (type != null) player.removePotionEffect(type);
        }
        applyLegacyConsumableEffect(player, id);

        ConfigurationSection statBuffs = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection(base + ".stat-buffs");
        if (statBuffs != null) {
            Map<MMOStatType, TimedStatBuff> active = consumableStatBuffs.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
            for (String statName : statBuffs.getKeys(false)) {
                MMOStatType type = MMOStatType.parse(statName);
                if (type == null) continue;
                double amount = Math.max(0.0D, statBuffs.getDouble(statName + ".amount", 0.0D));
                long duration = Math.max(1L, statBuffs.getLong(statName + ".duration-seconds", 1L)) * 1000L;
                if (amount > 0.0D) {
                    TimedStatBuff current = active.get(type);
                    long expiresAt = now + duration;
                    if (current == null || current.expiresAt() <= now || amount >= current.amount()) {
                        active.put(type, new TimedStatBuff(Math.max(amount, current == null ? 0.0D : current.amount()), expiresAt));
                    } else {
                        active.put(type, new TimedStatBuff(current.amount(), Math.max(current.expiresAt(), expiresAt)));
                    }
                }
            }
        }
        long cooldown = Math.max(0L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong(base + ".cooldown-seconds", 0L)) * 1000L;
        if (cooldown > 0L) consumableCooldowns.put(cooldownKey, now + cooldown);

        if (plugin instanceof Game game && game.getMmoStatService() != null) game.getMmoStatService().refreshPlayerMMOStats(player);

        int remaining = item.getAmount() - 1;
        item.setAmount(Math.max(0, remaining));
        play(player, Sound.ENTITY_GENERIC_EAT);
        player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + ".message", "&aYou consumed &f" + pretty(id) + "&a.")));
        return true;
    }

    private record TimedStatBuff(double amount, long expiresAt) {}

    private void applyLegacyConsumableEffect(Player player, String id) {
        String ability = getAbilityType(id).toLowerCase(Locale.ROOT);
        switch (ability) {
            case "heal" -> {
                if (plugin instanceof Game game && game.getMmoStatService() != null) {
                    game.getMmoStatService().healMmoHealth(player, 6.0D);
                } else {
                    player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 6.0D));
                }
                play(player, Sound.ENTITY_PLAYER_LEVELUP);
            }
            case "nightvision" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 60 * 20, 0));
                play(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME);
            }
            case "ward" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 12 * 20, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 12 * 20, 1));
                play(player, Sound.ITEM_SHIELD_BLOCK);
            }
            case "haste" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 20, 1));
                play(player, Sound.BLOCK_ANVIL_USE);
            }
            default -> {
            }
        }
    }

    private void runAbility(Player player, String id, String ability) {
        switch (ability.toLowerCase(Locale.ROOT)) {
            case "dash" -> {
                Vector direction = player.getLocation().getDirection().normalize().multiply(1.25D).setY(0.35D);
                player.setVelocity(direction);
                play(player, Sound.ENTITY_ENDER_DRAGON_FLAP);
            }
            case "heal" -> {
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 6.0D));
                play(player, Sound.ENTITY_PLAYER_LEVELUP);
            }
            case "haste" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 20, 1));
                play(player, Sound.BLOCK_ANVIL_USE);
            }
            case "embershot" -> {
                Fireball fireball = player.launchProjectile(Fireball.class);
                fireball.setYield(0.0F);
                fireball.setIsIncendiary(false);
                play(player, Sound.ITEM_FIRECHARGE_USE);
            }
            case "nightvision" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 60 * 20, 0));
                play(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME);
            }
            case "prospect" -> {
                int radius = Math.max(4, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(id) + ".ability.radius", 12));
                Map<Material, Integer> ores = countNearbyOres(player, radius);
                if (ores.isEmpty()) {
                    player.sendMessage(CC.color("&7Your pick hums quietly. No rich ore pockets nearby."));
                } else {
                    player.sendMessage(CC.color("&bOre Pulse &8> &f" + summarizeOres(ores)));
                }
                play(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME);
            }
            case "seismic" -> {
                double radius = Math.max(2.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".ability.radius", 5.0D));
                double damage = Math.max(1.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".ability.damage", 7.0D));
                for (LivingEntity entity : player.getLocation().getNearbyLivingEntities(radius)) {
                    if (entity instanceof Player || !(entity instanceof Monster)) continue;
                    entity.damage(damage, player);
                    Vector knockback = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.7D).setY(0.25D);
                    entity.setVelocity(knockback);
                }
                play(player, Sound.ENTITY_GENERIC_EXPLODE);
            }
            case "ward" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 12 * 20, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 12 * 20, 1));
                play(player, Sound.ITEM_SHIELD_BLOCK);
            }
            case "farmburst" -> {
                if (isLegacyAbilityCoolingDown(player, id)) return;
                MMOStatMap stats = getMmoStats(heldAbilityItem(player, id));
                int radius = Math.max(2, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(id) + ".ability.radius", 4)
                        + (int) Math.floor(stats.get(MMOStatType.HARVEST_RANGE)));
                int growthAmount = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(id) + ".ability.growth-amount", 1)
                        + (int) Math.floor(stats.get(MMOStatType.CROP_GROWTH) / 3.0D));
                int maxCrops = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(id) + ".ability.max-crops", 12));
                int grown = growNearbyCrops(player, radius, growthAmount, maxCrops);
                if (grown > 0) {
                    player.getWorld().spawnParticle(Particle.BUBBLE, player.getLocation().add(0.0D, 0.8D, 0.0D), Math.min(24, 4 + grown), radius * 0.35D, 0.25D, radius * 0.35D, 0.02D);
                }
                player.sendMessage(CC.color("&aSprinkler &8> &fGrew &e" + grown + " &fcrops."));
                play(player, Sound.ITEM_BONE_MEAL_USE);
            }
            default -> player.sendMessage(CC.color("&cThis custom item has an unknown ability."));
        }
    }

    private boolean healMmoPlayer(Player player, String itemId) {
        if (!(plugin instanceof Game game) || game.getMmoStatService() == null || player == null || player.isDead() || !player.isOnline()) {
            return false;
        }
        var snapshot = game.getMmoStatService().getSnapshot(player);
        double max = snapshot.maxHealth();
        double current = snapshot.health();
        double flat = Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getDouble(path(itemId) + ".ability.heal-amount", 100.0D));
        double percent = Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getDouble(path(itemId) + ".ability.heal-percent-of-max", 0.15D));
        double requested = flat + (max * percent);
        double healed = Math.min(Math.max(0.0D, max - current), requested);
        if (healed <= 0.0D || game.getMmoStatService().healMmoHealth(player, requested) <= 0.0D) return false;
        game.getMmoStatService().refreshPlayerMMOStats(player);
        player.sendMessage(CC.color("&aHealed for &c" + formatNumber(healed) + "❤&a!"));
        play(player, Sound.ENTITY_PLAYER_LEVELUP);
        return true;
    }

    private ItemStack heldAbilityItem(Player player, String id) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (normalize(id).equals(getItemId(main))) return main;
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (normalize(id).equals(getItemId(offhand))) return offhand;
        return main;
    }

    private void registerRecipes() {
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("custom-items.items");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection recipeSection = section.getConfigurationSection(id + ".recipe");
            if (recipeSection == null) continue;
            List<String> shape = recipeSection.getStringList("shape");
            if (shape.isEmpty()) continue;
            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "custom_" + normalize(id)), createItem(id, 1));
            recipe.shape(shape.toArray(String[]::new));
            ConfigurationSection ingredients = recipeSection.getConfigurationSection("ingredients");
            if (ingredients != null) {
                for (String key : ingredients.getKeys(false)) {
                    if (key.length() != 1) continue;
                    Material material = parseMaterial(ingredients.getString(key, "AIR"));
                    if (material != Material.AIR) recipe.setIngredient(key.charAt(0), material);
                }
            }
            try {
                Bukkit.addRecipe(recipe);
            } catch (IllegalStateException ignored) {
            }
        }
    }

    private void loadDatabaseItems() {
        databaseItems.clear();
        if (!dataManager.isMySqlEnabled()) {
            plugin.getLogger().warning("MariaDB is disabled; /edititem database custom items are unavailable.");
            return;
        }
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT `id`, `item_type`, `material`, `display_name`, `lore`, `rarity`, `stats_json`, `abilities_json`, `flags_json`, `nbt_json` FROM `smp_custom_items`")) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String id = normalize(result.getString("id"));
                    Material material = parseMaterial(result.getString("material"));
                    if (id.isBlank() || material == Material.AIR) {
                        plugin.getLogger().warning("Skipping invalid database custom item: " + result.getString("id"));
                        continue;
                    }
                    databaseItems.put(id, new DatabaseCustomItem(
                            id,
                            result.getString("item_type"),
                            material.name(),
                            result.getString("display_name"),
                            readStringList(result.getString("lore")),
                            result.getString("rarity"),
                            validJson(result.getString("stats_json")),
                            validJson(result.getString("abilities_json")),
                            validJson(result.getString("flags_json")),
                            validJson(result.getString("nbt_json"))
                    ));
                }
            }
            plugin.getLogger().info("Loaded " + databaseItems.size() + " database custom items.");
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to load database custom items: " + exception.getMessage());
        }
    }

    private ItemStack createDatabaseItem(DatabaseCustomItem definition, int amount) {
        ItemStack item = new ItemStack(parseMaterial(definition.material()), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(valueOr(definition.displayName(), "&a" + pretty(definition.id()))));
            List<String> lore = new ArrayList<>();
            for (String line : definition.lore()) lore.add(CC.color(line));
            JsonObject ability = jsonObject(definition.abilitiesJson());
            if (ability.has("id") && canHaveRightClickAbility(definition.id(), item)) {
                CustomItemAbility registered = abilityRegistry.get(ability.get("id").getAsString()).orElse(null);
                lore.add("");
                lore.add(CC.color("&6Ability: &e" + (registered == null ? pretty(ability.get("id").getAsString()) : registered.displayName())));
                if (registered != null) {
                    for (String line : registered.description()) lore.add(CC.color(line));
                    lore.add(CC.color("&7Trigger: &f" + registered.trigger().name()));
                    lore.add(CC.color("&8Mana Cost: &b" + registered.manaCost()));
                    if (registered.cooldownSeconds() > 0) lore.add(CC.color("&8Cooldown: &a" + registered.cooldownSeconds() + "s"));
                } else {
                    if (ability.has("trigger")) lore.add(CC.color("&7Trigger: &f" + ability.get("trigger").getAsString()));
                    if (ability.has("mana_cost")) lore.add(CC.color("&7Mana Cost: &b" + ability.get("mana_cost").getAsInt()));
                }
            }
            setSafeLore(meta, lore, definition.id());
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, definition.id());
            meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, valueOr(definition.itemType(), "misc"));
            String databaseType=valueOr(definition.itemType(),"misc").toUpperCase(Locale.ROOT);
            if(definition.id().equals("developer_notebook")||Set.of("MATERIAL","MINING_MATERIAL","FORAGING_MATERIAL","FARMING_MATERIAL","DUNGEON_MATERIAL","COMBAT_MATERIAL","MONSTER_MATERIAL","FISHING_MATERIAL","MAGIC_MATERIAL","RECIPE_MATERIAL","COOKING_INGREDIENT","CUSTOM_FOOD","FOOD","CUSTOM_FISH","FISH","FISHING_ITEM","FISHING_BAIT","BAIT","FISHING_GEAR_MATERIAL","EVENT_MATERIAL","EVENT_ITEM","NORMAL_EVENT_ITEM","EVENT_COLLECTIBLE","TOKEN","EVENT_TOKEN","DUNGEON_KEY","QUEST_ITEM","FURNITURE","COSMETIC","SPECIAL","DEVELOPER_NOTEBOOK","FARMING_EQUIPMENT").contains(databaseType)){
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"random_stats_blocked"),PersistentDataType.BYTE,(byte)1);meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"random_rarity_blocked"),PersistentDataType.BYTE,(byte)1);meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"reforge_allowed"),PersistentDataType.BYTE,(byte)0);meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"enchant_allowed"),PersistentDataType.BYTE,(byte)0);
            }
            meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, valueOr(definition.rarity(), "COMMON"));
            meta.getPersistentDataContainer().set(statsKey, PersistentDataType.STRING, definition.statsJson());
            meta.getPersistentDataContainer().set(abilityKey, PersistentDataType.STRING, definition.abilitiesJson());
            JsonObject storedAbility=jsonObject(definition.abilitiesJson());
            if(storedAbility.has("id")&&storedAbility.get("id").isJsonPrimitive())meta.getPersistentDataContainer().set(abilityIdKey,PersistentDataType.STRING,normalize(storedAbility.get("id").getAsString()));
            meta.getPersistentDataContainer().set(flagsKey, PersistentDataType.STRING, definition.flagsJson());
            meta.getPersistentDataContainer().set(nbtKey, PersistentDataType.STRING, definition.nbtJson());
            JsonObject flags = jsonObject(definition.flagsJson());
            if (flags.has("unbreakable") && flags.get("unbreakable").getAsBoolean()) meta.setUnbreakable(true);
            item.setItemMeta(meta);
            if (flags.has("glowing") && flags.get("glowing").getAsBoolean()) applyVisualGlow(item, true);
        }
        itemRarityManager.applyIfMissing(item);
        applyDatabaseMmoStats(definition, item);
        ensureMissingGearStats(item);
        ensureItemRequirements(item, definition.id(), 50.0D);
        sanitizeBlockedRandomEffects(item, definition.id());
        cleanVanillaEnchants(item);
        return item;
    }

    private boolean applyMmoStats(String id, ItemStack item, boolean force) {
        if (item == null || item.getType().isAir() || (!force && mmoStatStorage.hasStats(item))) return false;
        boolean allowRandom = canRollRandomRarityEffects(item);
        StatRoll roll = configuredMmoStats(id, item, allowRandom);
        if (roll.stats().isEmpty()) return false;
        MMOItemType itemType = effectiveItemType(item, id);
        mmoStatStorage.write(item, itemType, roll.stats());
        if (allowRandom && roll.random()) mmoStatStorage.markRandomRolled(item, roll.quality());
        else mmoStatStorage.clearRandomRoll(item);
        ensureItemRequirements(item, id, roll.quality());
        rebuildMmoLore(item, mmoStatStorage.read(item));
        return true;
    }

    private void applyDatabaseMmoStats(DatabaseCustomItem definition, ItemStack item) {
        boolean allowRandom = canRollRandomRarityEffects(item);
        StatRoll roll = statsFromJson(definition.statsJson(), item, allowRandom);
        if (roll.stats().isEmpty()) return;
        mmoStatStorage.write(item, MMOItemType.parse(definition.itemType()), roll.stats());
        if (allowRandom && roll.random()) mmoStatStorage.markRandomRolled(item, roll.quality());
        else mmoStatStorage.clearRandomRoll(item);
        ensureItemRequirements(item, definition.id(), roll.quality());
        rebuildMmoLore(item, mmoStatStorage.read(item));
    }

    private StatRoll configuredMmoStats(String id, ItemStack item, boolean allowRandom) {
        MMOStatMap stats = MMOStatMap.empty();
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection(path(id) + ".mmo.stats");
        if (section == null) return new StatRoll(stats, false, 0.0D);
        boolean rollConfigured = allowRandom && club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getBoolean(path(id) + ".mmo.random-roll", true);
        List<Double> qualities = new ArrayList<>();
        boolean random = false;
        ConfigurationSection ranges = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection(path(id) + ".mmo.stat-ranges");
        for (String key : section.getKeys(false)) {
            MMOStatType type = MMOStatType.parse(key);
            if (type == null) continue;
            StatRange range = rangeFromSection(section, key);
            if (ranges != null && ranges.isConfigurationSection(key)) range = rangeFromSection(ranges, key);
            if (range == null && ranges != null && ranges.isString(key)) range = rangeFromString(ranges.getString(key));
            if (range != null) {
                RollValue value = rollConfigured ? rollByRarity(item, (range.min() + range.max()) / 2.0D) : fixed(range);
                stats.set(type, value.value());
                if (rollConfigured) {
                    qualities.add(value.quality());
                    random = true;
                }
                continue;
            }
            double fixed = section.getDouble(key, 0.0D);
            if (rollConfigured && fixed > 0.0D) {
                RollValue value = rollByRarity(item, fixed);
                stats.set(type, value.value());
                qualities.add(value.quality());
                random = true;
            } else {
                stats.set(type, fixed);
            }
        }
        return new StatRoll(stats, random, averageQuality(qualities));
    }

    private StatRoll statsFromJson(String json, ItemStack item, boolean allowRandom) {
        MMOStatMap stats = MMOStatMap.empty();
        JsonObject object = jsonObject(json);
        List<Double> qualities = new ArrayList<>();
        boolean random = false;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            MMOStatType type = MMOStatType.parse(entry.getKey());
            if (type != null && entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                double base = entry.getValue().getAsDouble();
                if (allowRandom && base > 0.0D) {
                    RollValue value = rollByRarity(item, base);
                    stats.set(type, value.value());
                    qualities.add(value.quality());
                    random = true;
                } else stats.set(type, base);
            } else if (type != null) {
                StatRange range = rangeFromJson(entry.getValue());
                if (range != null) {
                    RollValue value = allowRandom ? rollByRarity(item, (range.min() + range.max()) / 2.0D) : fixed(range);
                    stats.set(type, value.value());
                    if (allowRandom) {
                        qualities.add(value.quality());
                        random = true;
                    }
                }
            }
        }
        return new StatRoll(stats, random, averageQuality(qualities));
    }

    private StatRange rangeFromSection(ConfigurationSection section, String key) {
        if (section.isConfigurationSection(key)) {
            ConfigurationSection nested = section.getConfigurationSection(key);
            if (nested == null) return null;
            if (!nested.contains("min") || !nested.contains("max")) return null;
            return normalizedRange(nested.getDouble("min"), nested.getDouble("max"));
        }
        if (section.isString(key)) return rangeFromString(section.getString(key));
        return null;
    }

    private StatRange rangeFromJson(JsonElement element) {
        if (element == null) return null;
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (!object.has("min") || !object.has("max")) return null;
            return normalizedRange(object.get("min").getAsDouble(), object.get("max").getAsDouble());
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) return rangeFromString(element.getAsString());
        return null;
    }

    private StatRange rangeFromString(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().replace(" ", "");
        String[] parts = normalized.split("\\.\\.|-", 2);
        if (parts.length != 2) return null;
        try {
            return normalizedRange(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private StatRange defaultRange(double fixed) {
        double spread = Math.max(1.0D, Math.abs(fixed) * 0.12D);
        return normalizedRange(Math.max(0.0D, fixed - spread), fixed + spread);
    }

    private StatRange normalizedRange(double min, double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max)) return null;
        return new StatRange(Math.min(min, max), Math.max(min, max));
    }

    private RollValue roll(StatRange range) {
        if (range == null || range.min() == range.max()) return new RollValue(range == null ? 0.0D : range.max(), 100.0D);
        double raw = ThreadLocalRandom.current().nextDouble(range.min(), Math.nextUp(range.max()));
        double value = Math.rint(raw);
        double quality = ((value - range.min()) / (range.max() - range.min())) * 100.0D;
        return new RollValue(value, quality);
    }

    private RollValue rollByRarity(ItemStack item, double base) {
        String rarity = getCustomItemRarity(item).toUpperCase(Locale.ROOT);
        double[] range = switch (rarity) {
            case "UNCOMMON" -> new double[]{0.95D, 1.10D};
            case "RARE" -> new double[]{1.05D, 1.25D};
            case "EPIC" -> new double[]{1.20D, 1.50D};
            case "LEGENDARY" -> new double[]{1.40D, 1.85D};
            case "MYTHIC" -> new double[]{1.70D, 2.25D};
            case "SPECIAL", "DIVINE" -> new double[]{1.0D, 1.0D};
            default -> new double[]{0.85D, 1.0D};
        };
        double rarityMultiplier = range[0] == range[1] ? range[0]
                : ThreadLocalRandom.current().nextDouble(range[0], Math.nextUp(range[1]));
        double perStat = ThreadLocalRandom.current().nextDouble(0.90D, Math.nextUp(1.10D));
        double rolled = Math.rint(Math.clamp(base * rarityMultiplier * perStat, 0.0D, 1_000_000_000.0D));
        double rarityQuality = range[0] == range[1] ? 100.0D
                : (rarityMultiplier - range[0]) / (range[1] - range[0]) * 100.0D;
        double statQuality = (perStat - 0.90D) / 0.20D * 100.0D;
        return new RollValue(Double.isFinite(rolled) ? rolled : Math.max(0.0D, base),
                Math.clamp((rarityQuality + statQuality) / 2.0D, 0.0D, 100.0D));
    }

    private RollValue fixed(StatRange range) {
        if (range == null) return new RollValue(0.0D, 0.0D);
        return new RollValue(Math.rint((range.min() + range.max()) / 2.0D), 0.0D);
    }

    private double averageQuality(List<Double> qualities) {
        if (qualities.isEmpty()) return 0.0D;
        double total = 0.0D;
        for (double quality : qualities) total += quality;
        return total / qualities.size();
    }

    private void ensureItemRequirements(ItemStack item, String id, double quality) {
        if (item == null || item.getType().isAir() || id == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (!canRollRandomRarityEffects(item)) {
            meta.getPersistentDataContainer().remove(requirementsKey);
            meta.getPersistentDataContainer().remove(requirementsRolledKey);
            item.setItemMeta(meta);
            return;
        }
        if (meta.getPersistentDataContainer().has(requirementsRolledKey, PersistentDataType.BYTE)) return;
        Map<String, Integer> requirements = rollItemRequirements(id, item, quality);
        if (!requirements.isEmpty()) {
            JsonObject object = new JsonObject();
            for (Map.Entry<String, Integer> entry : requirements.entrySet()) object.addProperty(entry.getKey(), entry.getValue());
            meta.getPersistentDataContainer().set(requirementsKey, PersistentDataType.STRING, object.toString());
        }
        meta.getPersistentDataContainer().set(requirementsRolledKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    private Map<String, Integer> rollItemRequirements(String id, ItemStack item, double quality) {
        ConfigurationSection configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getConfigurationSection(path(id) + ".mmo.requirements");
        if (configured != null) {
            Map<String, Integer> requirements = new LinkedHashMap<>();
            for (String key : configured.getKeys(false)) {
                int level = Math.max(0, configured.getInt(key, 0));
                if (level > 0) requirements.put(normalizeRequirementSkill(key), level);
            }
            return requirements;
        }
        String skill = requirementSkill(id, item);
        if (skill.isBlank()) return Map.of();
        String rarity = itemRarityName(id, item);
        LevelRange range = requirementRange(rarity);
        if (range.max() <= 0) return Map.of();
        int base = explicitRequirementBase(normalize(id));
        if (base < 0) base = Math.max(range.min(), (range.min() + range.max()) / 2);
        int qualityBonus = quality < 25.0D ? -1 : quality >= 95.0D ? 2 : quality >= 70.0D ? 1 : 0;
        int rolled = base + ThreadLocalRandom.current().nextInt(-2, 3) + qualityBonus;
        int level = Math.clamp(rolled, 0, Math.min(45, range.max()));
        if (level <= 0) return Map.of();
        Map<String, Integer> requirements = new LinkedHashMap<>();
        requirements.put(skill, level);
        return requirements;
    }

    private Map<String, Integer> itemRequirements(ItemStack item) {
        Map<String, Integer> requirements = new LinkedHashMap<>();
        if (item == null || !item.hasItemMeta()) return requirements;
        String raw = item.getItemMeta().getPersistentDataContainer().get(requirementsKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return requirements;
        try {
            JsonObject object = JsonParser.parseString(raw).getAsJsonObject();
            for (String key : object.keySet()) {
                if (object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber()) {
                    String skill = normalizeRequirementSkill(key);
                    int level = Math.max(0, object.get(key).getAsInt());
                    if (!skill.isBlank() && level > 0) requirements.merge(skill, level, Math::max);
                }
            }
        } catch (Exception ignored) {
        }
        return requirements;
    }

    private List<String> requirementLoreLines(ItemStack item, Player viewer) {
        Map<String, Integer> requirements = itemRequirements(item);
        if (requirements.isEmpty()) return List.of();
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : requirements.entrySet()) {
            String requirement = "Requires " + requirementDisplayName(entry.getKey()) + " " + entry.getValue();
            if (viewer == null) lines.add("&7" + requirement);
            else if (playerRequirementLevel(viewer, entry.getKey()) >= entry.getValue()) lines.add("&a&l✔ &a" + requirement);
            else lines.add("&c&lX &c" + requirement);
        }
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            int requirementStart = line.indexOf("&aRequires ");
            if (line.startsWith("&a&l") && requirementStart >= 0) {
                lines.set(index, "&a&l✔ " + line.substring(requirementStart));
            }
        }
        return lines;
    }

    private int playerRequirementLevel(Player player, String requirement) {
        String key = normalizeRequirementSkill(requirement);
        if (key.equals("player_level")) return playerLevelManager == null ? 1 : playerLevelManager.getLevel(player);
        return skillManager == null ? 1 : skillManager.getLevel(player, key);
    }

    private String requirementDisplayName(String requirement) {
        String key = normalizeRequirementSkill(requirement);
        if (key.equals("player_level")) return "PlayerLevel";
        if (key.equals("woodcutting")) return "Foraging";
        return pretty(key);
    }

    private String requirementSkill(String id, ItemStack item) {
        String key = normalize(id);
        MMOItemType type = configuredMmoItemType(key);
        if (type == MMOItemType.MATERIAL || type == MMOItemType.CONSUMABLE || type == MMOItemType.FOOD || type == MMOItemType.POTION) return "";
        Material material = item == null || item.getType().isAir() ? getMaterial(key) : item.getType();
        String lower = key + " " + material.name().toLowerCase(Locale.ROOT) + " "
                + club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(key) + ".category", "").toLowerCase(Locale.ROOT);
        return switch (type) {
            case STAFF, WAND -> "enchanting";
            case WEAPON, SWORD, KATANA, DAGGER, BOW -> "combat";
            case ARMOR, SHIELD -> armorRequirementSkill(lower);
            case RING, NECKLACE, TRINKET, ACCESSORY, COMPANION -> "player_level";
            case FISHING_ROD, ROD -> "fishing";
            case FARMING_EQUIPMENT -> "farming";
            case TOOL, UTILITY -> toolRequirementSkill(lower, material);
            default -> "";
        };
    }

    private String armorRequirementSkill(String lower) {
        if (lower.contains("miner") || lower.contains("deepstone") || lower.contains("crystal")) return "mining";
        if (lower.contains("farmer") || lower.contains("harvest") || lower.contains("crop")) return "farming";
        if (lower.contains("angler") || lower.contains("tide") || lower.contains("fishing")) return "fishing";
        if (lower.contains("treant") || lower.contains("forest") || lower.contains("wood")) return "woodcutting";
        return "combat";
    }

    private String toolRequirementSkill(String lower, Material material) {
        if (lower.contains("sprinkler") || lower.contains("hoe") || lower.contains("sickle") || lower.contains("harvest") || material.name().endsWith("_HOE")) return "farming";
        if (lower.contains("rod") || lower.contains("trident") || material == Material.FISHING_ROD || material == Material.TRIDENT) return "fishing";
        if (lower.contains("axe") || lower.contains("treant") || lower.contains("tree") || lower.contains("wood") || material.name().endsWith("_AXE")) return "woodcutting";
        if (lower.contains("pick") || lower.contains("drill") || lower.contains("hammer") || material.name().endsWith("_PICKAXE")) return "mining";
        return "combat";
    }

    private String itemRarityName(String id, ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            String stored = meta.getPersistentDataContainer().get(rarityKey, PersistentDataType.STRING);
            if (stored != null && !stored.isBlank()) return stored;
            String rolled = meta.getPersistentDataContainer().get(itemRarityKey, PersistentDataType.STRING);
            if (rolled != null && !rolled.isBlank()) return rolled;
        }
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".rarity", "COMMON");
    }

    private LevelRange requirementRange(String rarity) {
        return switch ((rarity == null ? "" : rarity).toUpperCase(Locale.ROOT)) {
            case "UNCOMMON" -> new LevelRange(2, 7);
            case "RARE" -> new LevelRange(5, 15);
            case "EPIC" -> new LevelRange(10, 25);
            case "LEGENDARY" -> new LevelRange(18, 35);
            case "MYTHIC" -> new LevelRange(25, 45);
            case "SPECIAL" -> new LevelRange(0, 0);
            default -> new LevelRange(0, 3);
        };
    }

    private int explicitRequirementBase(String id) {
        return switch (id) {
            case "royal_spear" -> 21;
            case "sakura_katana" -> 9;
            case "shadow_katana" -> 20;
            case "bloodfang_dagger" -> 15;
            case "ember_blade" -> 11;
            case "frost_bow" -> 13;
            case "harvester_bow" -> 18;
            case "storm_staff" -> 20;
            case "plague_wand" -> 16;
            case "celestial_blade" -> 33;
            case "obsidian_shield" -> 23;
            case "deepstone_pickaxe" -> 14;
            case "crystal_hammer" -> 19;
            case "treant_axe" -> 12;
            case "harvesting_hoe" -> 12;
            case "royal_harvest_hoe" -> 24;
            case "deep_sea_rod" -> 12;
            case "captains_trident" -> 19;
            case "royal_ring" -> 14;
            case "void_ring" -> 26;
            case "guardian_necklace" -> 17;
            case "celestial_necklace" -> 34;
            case "lucky_trinket" -> 12;
            case "rift_trinket" -> 22;
            default -> -1;
        };
    }

    private String normalizeRequirementSkill(String skill) {
        String normalized = normalize(skill);
        if (normalized.equals("foraging")) return "woodcutting";
        if (normalized.equals("player") || normalized.equals("level")) return "player_level";
        return normalized;
    }

    private MMOItemType configuredMmoItemType(String id) {
        DatabaseCustomItem databaseItem = databaseItems.get(normalize(id));
        if (databaseItem != null) return MMOItemType.parse(databaseItem.itemType());
        return MMOItemType.parse(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".mmo.type",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".type",
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".category", "UTILITY"))));
    }

    private String configuredCustomItemType(String id) {
        DatabaseCustomItem databaseItem = databaseItems.get(normalize(id));
        if (databaseItem != null) return normalize(valueOr(databaseItem.itemType(), "MISC")).toUpperCase(Locale.ROOT);
        String configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".type", "");
        if (configured == null || configured.isBlank()) {
            configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".mmo.type",
                    club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".category", "MISC"));
        }
        return normalize(configured).toUpperCase(Locale.ROOT);
    }

    private MMOItemType effectiveItemType(ItemStack item, String id) {
        if (item != null && item.hasItemMeta()) {
            if (id != null && !id.isBlank()) {
                MMOItemType configured = configuredMmoItemType(id);
                if (configured != MMOItemType.UTILITY && configured != MMOItemType.MISC) return configured;
            }
            if (mmoStatStorage.hasStats(item)) return mmoStatStorage.readItemType(item);
            String customType = item.getItemMeta().getPersistentDataContainer().get(itemTypeKey, PersistentDataType.STRING);
            if (customType != null && !customType.isBlank()) return MMOItemType.parse(customType);
        }
        if (id != null && !id.isBlank()) return configuredMmoItemType(id);
        return MMOItemType.MISC;
    }

    private boolean canRollRandomRarityEffects(MMOItemType type) {
        return type != null && type.canRollRandomRarityEffects();
    }

    private boolean sanitizeBlockedRandomEffects(ItemStack item, String id) {
        // Special authored items own their complete metadata and lore. Sanitizing them is
        // itself destructive because it replaces their authored rarity/lore with defaults.
        if (isDeveloperNotebook(item)) {
            ensureDeveloperNotebookSpecialLine(item);
            return false;
        }
        if (item == null || item.getType().isAir() || canRollRandomRarityEffects(item)) return false;
        boolean changed = mmoStatStorage.hasRandomRoll(item);
        if (changed) {
            DatabaseCustomItem databaseItem = databaseItems.get(normalize(id));
            StatRoll fixed = databaseItem == null
                    ? configuredMmoStats(id, item, false)
                    : statsFromJson(databaseItem.statsJson(), item, false);
            if (fixed.stats().isEmpty()) mmoStatStorage.clearStats(item);
            else mmoStatStorage.write(item, effectiveItemType(item, id), fixed.stats());
        }
        mmoStatStorage.clearRandomRoll(item);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean hadRequirements = meta.getPersistentDataContainer().has(requirementsKey, PersistentDataType.STRING)
                    || meta.getPersistentDataContainer().has(requirementsRolledKey, PersistentDataType.BYTE);
            meta.getPersistentDataContainer().remove(requirementsKey);
            meta.getPersistentDataContainer().remove(requirementsRolledKey);
            String fixedRarity = meta.getPersistentDataContainer().get(rarityKey, PersistentDataType.STRING);
            if (fixedRarity == null || fixedRarity.isBlank()) {
                fixedRarity = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                        .getString(path(id) + ".rarity", "COMMON");
            }
            String currentRarity = meta.getPersistentDataContainer().get(itemRarityKey, PersistentDataType.STRING);
            boolean correctedRarity = !fixedRarity.equalsIgnoreCase(valueOr(currentRarity, ""));
            meta.getPersistentDataContainer().set(itemRarityKey, PersistentDataType.STRING, fixedRarity.toUpperCase(Locale.ROOT));
            item.setItemMeta(meta);
            changed |= hadRequirements || correctedRarity;
        }
        return changed;
    }

    private boolean isCustomGearType(MMOItemType type) {
        return switch (type) {
            case WEAPON, SWORD, KATANA, DAGGER, SPEAR, HAMMER, AXE, BOW, CROSSBOW, STAFF, WAND,
                    ARMOR, TOOL, SHIELD, FISHING_ROD, FARMING_EQUIPMENT, ROD -> true;
            default -> false;
        };
    }

    private boolean isCustomMeleeWeaponType(MMOItemType type) {
        return switch (type) {
            case WEAPON, SWORD, KATANA, DAGGER, SPEAR, HAMMER, AXE, STAFF, WAND -> true;
            default -> false;
        };
    }

    private boolean isFishingBaitType(MMOItemType type) {
        return type == MMOItemType.FISHING_BAIT || type == MMOItemType.BAIT;
    }

    private boolean isFishingBaitId(String id) {
        String key = normalize(id);
        return key.equals("chum_bait")
                || key.equals("nightcrawler_bait")
                || key.equals("treasure_bait")
                || key.equals("bait")
                || key.equals("fishing_bait")
                || key.endsWith("_bait");
    }

    private boolean canHaveRightClickAbility(String id) {
        return canHaveRightClickAbility(id, null);
    }

    private boolean isAbilityValidForItem(String abilityId, ItemStack item) {
        CustomItemAbility definition = abilityRegistry.get(abilityId).orElse(null);
        if (definition != null && !definition.allowedItemTypes().isEmpty()
                && (item == null || !definition.allowedItemTypes().contains(getMmoItemType(item)))) return false;
        String ability = normalize(abilityId);
        if (!Set.of("prospect", "vein_burst", "stone_pulse", "treasure_sense", "earthquake", "excavate",
                "harvest_sweep", "growth_aura", "timber_cleave", "leaf_storm", "root_snare").contains(ability)) return true;
        if (item == null || item.getType().isAir() || getItemId(item) == null) return false;
        String material = item.getType().name();
        String id = normalize(getItemId(item));
        boolean pickaxe = material.endsWith("_PICKAXE") || id.contains("drill");
        boolean shovel = material.endsWith("_SHOVEL");
        boolean hoe = material.endsWith("_HOE");
        boolean axe = material.endsWith("_AXE") && !pickaxe;
        return switch (ability) {
            case "prospect", "vein_burst", "stone_pulse" -> pickaxe;
            case "treasure_sense", "earthquake" -> pickaxe || shovel;
            case "excavate" -> shovel;
            case "harvest_sweep", "growth_aura" -> hoe;
            case "timber_cleave", "leaf_storm" -> axe;
            case "root_snare" -> axe || hoe;
            default -> true;
        };
    }

    private int defaultToolAbilityCooldown(String abilityId) {
        CustomItemAbility registered = abilityRegistry.get(abilityId).orElse(null);
        if (registered != null && registered.cooldownSeconds() > 0) return registered.cooldownSeconds();
        return switch (normalize(abilityId)) {
            case "prospect", "harvest_sweep" -> 20;
            case "vein_burst", "leaf_storm" -> 30;
            case "stone_pulse" -> 15;
            case "treasure_sense", "growth_aura" -> 45;
            case "earthquake", "timber_cleave" -> 25;
            case "excavate" -> 12;
            case "root_snare" -> 35;
            default -> registered == null ? 0 : switch (registered.trigger()) {
                case PASSIVE, FULL_SET_BONUS -> 1;
                case ON_HIT, HIT_ENTITY, ENTITY_HIT, TAKE_DAMAGE, KILL_ENTITY -> 2;
                default -> 5;
            };
        };
    }

    private boolean canHaveRightClickAbility(String id, ItemStack item) {
        String key = normalize(id);
        if (key.isBlank() || key.equals("rubble_rod")) return false;
        if (isFishingBaitId(key)) return false;
        if (isConsumableId(key)) return false;
        MMOItemType type = effectiveItemType(item, key);
        if (!canRollRandomRarityEffects(type)) return false;
        return switch (type) {
            case WEAPON, SWORD, KATANA, DAGGER, SPEAR, HAMMER, AXE, BOW, STAFF, WAND,
                    TOOL, FARMING_EQUIPMENT, FISHING_ROD, ROD -> true;
            default -> false;
        };
    }

    private boolean isArmorMaterial(Material material) {
        return material != null && (material.name().endsWith("_HELMET")
                || material.name().endsWith("_CHESTPLATE")
                || material.name().endsWith("_LEGGINGS")
                || material.name().endsWith("_BOOTS"));
    }

    private void ensureCustomGearUnbreakable(ItemStack item, String id) {
        if (item == null || item.getType().isAir() || id == null || !isCustomGearType(configuredMmoItemType(id))) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
    }

    private void rebuildMmoLore(ItemStack item, MMOStatMap stats) {
        rebuildMmoLore(item, stats, null);
    }

    private void rebuildMmoLore(ItemStack item, MMOStatMap stats, Player viewer) {
        if (item == null || item.getType().isAir()) return;
        if (isDeveloperNotebook(item)) {
            ensureDeveloperNotebookSpecialLine(item);
            return;
        }
        String id = getItemId(item);
        if (id != null && id.startsWith("futuristic_")) {
            rebuildFuturisticArmorLore(item, id);
            return;
        }
        if (sanitizeBlockedRandomEffects(item, id)) stats = mmoStatStorage.read(item);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        boolean randomEligible = canRollRandomRarityEffects(item);
        List<String> lore = limitedDescriptionLore(normalLore(id, meta));
        lore.removeIf(line -> {
            String clean = ChatColor.stripColor(CC.color(line));
            return clean != null && clean.startsWith("Attack " + "Speed: ");
        });
        ensureItemRequirements(item, id, 50.0D);
        meta = item.getItemMeta();
        if (meta == null) return;
        String originalName = originalItemName(id, meta);
        String rarity = itemRarity(item, meta);
        meta.setDisplayName(mmoLoreBuilder.displayName(originalName, rarity));
        MMOStatMap visibleStats = MMOStatMap.empty();
        if (getFlag(item, "stat_lore")) {
            visibleStats.addAll(stats);
            if (customEnchantManager != null) visibleStats.addAll(customEnchantManager.statBonuses(item));
        }
        Map<String, String> visibleEnchants = getFlag(item, "enchant_lore") ? limitedEnchantLore(enchantLoreLines(mmoEnchantStorage.read(item))) : Map.of();
        String visibleAbility = randomEligible && getFlag(item, "ability_lore") ? abilityName(id, meta) : null;
        Integer visibleManaCost = visibleAbility == null ? null : abilityManaCost(id, meta);
        List<String> visibleAbilityDescription = visibleAbility == null ? List.of() : abilityDescription(id, meta);
        Integer visibleAbilityCooldown = visibleAbility == null ? null : (int) (abilityCooldownMillis(item, id) / 1000L);
        String visibleAbilityTrigger = visibleAbility == null ? null : abilityTriggerLabel(item, id);
        List<String> visibleRequirements = getFlag(item, "requirement_lore") ? limitedRequirementLore(requirementLoreLines(item, viewer)) : List.of();
        setSafeLore(meta, mmoLoreBuilder.rebuild(lore, originalName, rarity, visibleStats, visibleEnchants, visibleAbility,
                visibleManaCost, visibleRequirements, visibleAbilityDescription, visibleAbilityCooldown, visibleAbilityTrigger), id);
        applyMmoTooltipHiding(meta);
        applyEditorItemFlag(meta, "hide_attributes", getFlag(item, "hide_attributes"));
        applyEditorItemFlag(meta, "hide_enchants", getFlag(item, "hide_enchants"));
        applyEditorItemFlag(meta, "hide_unbreakable", getFlag(item, "hide_unbreakable"));
        applyEditorItemFlag(meta, "hide_additional_tooltip", getFlag(item, "hide_additional_tooltip"));
        item.setItemMeta(meta);
    }

    private void rebuildFuturisticArmorLore(ItemStack item, String id) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".name", "&b" + pretty(id))));
        meta.setLore(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(path(id) + ".lore").stream().map(CC::color).toList());
        applyMmoTooltipHiding(meta);
        item.setItemMeta(meta);
    }

    private List<String> limitedDescriptionLore(List<String> lore) {
        if (lore.size() <= 96) return new ArrayList<>(lore);
        List<String> limited = new ArrayList<>(lore.subList(0, 94));
        limited.add(CC.color("&8..."));
        limited.add(CC.color("&7Some item details were hidden."));
        return limited;
    }

    private List<String> limitedRequirementLore(List<String> requirements) {
        if (requirements.size() <= 32) return requirements;
        List<String> limited = new ArrayList<>(requirements.subList(0, 30));
        limited.add(CC.color("&7Requirements: &a" + requirements.size() + " total"));
        limited.add(CC.color("&8Use /edititem to view full details."));
        return limited;
    }

    private Map<String, String> limitedEnchantLore(Map<String, String> enchants) {
        if (enchants.size() <= 60) return enchants;
        Map<String, String> limited = new LinkedHashMap<>();
        enchants.entrySet().stream().limit(58).forEach(entry -> limited.put(entry.getKey(), entry.getValue()));
        limited.put("_summary", "Enchantments: " + enchants.size() + " active");
        limited.put("_details", "Use /edititem to view full details.");
        return limited;
    }

    private void setSafeLore(ItemMeta meta, List<String> requestedLore, String itemId) {
        List<String> lore = requestedLore == null ? new ArrayList<>() : new ArrayList<>(requestedLore);
        int originalSize = lore.size();
        if (originalSize > MAX_SAFE_LORE_LINES) {
            String rarity = null;
            for (int index = lore.size() - 1; index >= 0; index--) {
                String clean = ChatColor.stripColor(CC.color(lore.get(index)));
                if (clean != null && clean.toUpperCase(Locale.ROOT).matches("(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|SPECIAL|DIVINE).*")) {
                    rarity = lore.get(index);
                    break;
                }
            }
            int retained = rarity == null ? MAX_SAFE_LORE_LINES - 2 : MAX_SAFE_LORE_LINES - 3;
            lore = new ArrayList<>(lore.subList(0, retained));
            lore.add(CC.color("&8..."));
            lore.add(CC.color("&7Some item details were hidden."));
            if (rarity != null) lore.add(rarity);
            logOversizedLoreOnce(itemId, originalSize);
        }
        meta.setLore(lore);
    }

    private void logOversizedLoreOnce(String itemId, int lineCount) {
        String id = itemId == null || itemId.isBlank() ? "unknown" : normalize(itemId);
        boolean debug = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("custom-items.debug", false)
                || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("mmo.debug", false);
        if (debug && oversizedLoreWarnings.add(id)) {
            plugin.getLogger().warning("[SMP] Trimmed oversized lore for item " + id + ": " + lineCount + " lines");
        }
    }

    private List<String> normalLore(String id, ItemMeta meta) {
        JsonObject nbt = jsonObject(meta.getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING));
        if (nbt.has("description") && nbt.get("description").isJsonArray()) {
            List<String> edited = new ArrayList<>();
            for (JsonElement element : nbt.getAsJsonArray("description")) {
                if (element.isJsonPrimitive()) edited.add(CC.color(element.getAsString()));
            }
            if (!edited.isEmpty()) return cleanDescriptionLore(edited);
        }
        DatabaseCustomItem databaseItem = databaseItems.get(normalize(id));
        if (databaseItem != null) return cleanDescriptionLore(databaseItem.lore().stream().map(CC::color).toList());
        List<String> configured = id == null ? List.of()
                : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(path(id) + ".lore");
        return cleanDescriptionLore(CC.colorList(configured));
    }

    private List<String> cleanDescriptionLore(List<String> source) {
        List<String> cleaned = new ArrayList<>();
        for (String line : VendorLoreSanitizer.sanitizeVendorLore(source)) {
            if (!isGeneratedRequirementLore(line) && !isGeneratedRollLore(line)) cleaned.add(line);
        }
        return cleaned;
    }

    private boolean isGeneratedRollLore(String line) {
        String clean = ChatColor.stripColor(CC.color(line));
        if (clean == null) return false;
        String normalized = clean.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("stat roll:")
                || normalized.startsWith("roll quality:")
                || normalized.startsWith("quality roll:")
                || normalized.startsWith("stat quality:");
    }

    private boolean isGeneratedRequirementLore(String line) {
        String clean = ChatColor.stripColor(CC.color(line));
        if (clean == null) return false;
        clean = clean.trim();
        int requires = clean.indexOf("Requires ");
        if (requires < 0 || requires > 10) return false;
        String requirement = clean.substring(requires);
        return requirement.matches("(?i)Requires (Combat|Mining|Farming|Fishing|Foraging|Woodcutting|Enchanting|Player|PlayerLevel)( Level)? \\d+.*");
    }

    private void applyConfiguredEnchantments(String id, ItemStack item) {
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection(path(id) + ".enchantments");
        if (section == null) return;
        Map<String, Integer> values = mmoEnchantStorage.read(item);
        for (String key : section.getKeys(false)) {
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
            if (enchantment == null) enchantment = Enchantment.getByName(key.toUpperCase(Locale.ROOT));
            String customId = enchantment == null ? normalize(key) : MMOEnchantStorage.fromVanilla(enchantment);
            if (customId == null || customId.isBlank()) continue;
            values.merge(customId, Math.max(1, section.getInt(key, 1)), Math::max);
        }
        if (!values.isEmpty()) mmoEnchantStorage.write(item, values);
    }

    public boolean meetsItemRequirements(Player player, ItemStack item, boolean message) {
        if (player == null || item == null || item.getType().isAir()) return true;
        if (plugin instanceof Game game && game.getPlayerDataSyncManager().isEnabled()
                && !game.getPlayerDataSyncManager().ownsCurrentSessionLock(player.getUniqueId())) return true;
        if (skillManager != null && !skillManager.isSkillDataLoaded(player.getUniqueId())) return true;
        String id = getItemId(item);
        if (id == null || !itemExists(id)) return true;
        ensureItemRequirements(item, id, 50.0D);
        Map<String, Integer> requirements = itemRequirements(item);
        if (requirements.isEmpty()) return true;
        List<Map.Entry<String, Integer>> missing = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : requirements.entrySet()) {
            if (playerRequirementLevel(player, entry.getKey()) < entry.getValue()) missing.add(entry);
        }
        if (missing.isEmpty()) return true;
        if (message) {
            if (missing.size() == 1) {
                Map.Entry<String, Integer> requirement = missing.getFirst();
                String label = requirementDisplayName(requirement.getKey());
                player.sendMessage(CC.color("&cYou need " + label + (label.equals("PlayerLevel") ? " " : " Level ")
                        + requirement.getValue() + " to use this item!"));
            } else {
                player.sendMessage(CC.color("&cYou do not meet the requirements to use this item!"));
            }
        }
        return false;
    }

    public boolean meetsCachedItemRequirements(Player player, ItemStack item, boolean message) {
        if (player == null || item == null || item.getType().isAir()) return true;
        if (plugin instanceof Game game && game.getPlayerDataSyncManager().isEnabled()
                && !game.getPlayerDataSyncManager().ownsCurrentSessionLock(player.getUniqueId())) return true;
        if (skillManager != null && !skillManager.isSkillDataLoaded(player.getUniqueId())) return true;
        Map<String, Integer> requirements = itemRequirements(item);
        if (requirements.isEmpty()) return true;
        List<Map.Entry<String, Integer>> missing = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : requirements.entrySet()) {
            if (playerRequirementLevel(player, entry.getKey()) < entry.getValue()) missing.add(entry);
        }
        if (missing.isEmpty()) return true;
        if (message) {
            if (missing.size() == 1) {
                Map.Entry<String, Integer> requirement = missing.getFirst();
                String label = requirementDisplayName(requirement.getKey());
                player.sendMessage(CC.color("&cYou need " + label + (label.equals("PlayerLevel") ? " " : " Level ")
                        + requirement.getValue() + " to use this item!"));
            } else {
                player.sendMessage(CC.color("&cYou do not meet the requirements to use this item!"));
            }
        }
        return false;
    }

    public void rebuildLoreFor(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        if (isDeveloperNotebook(item)) {
            ensureDeveloperNotebookSpecialLine(item);
            return;
        }
        String id = getItemId(item);
        if (id == null || !itemExists(id)) return;
        ensureItemRequirements(item, id, 50.0D);
        rebuildMmoLore(item, mmoStatStorage.read(item), player);
    }

    public boolean cleanVisibleRollLore(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (isDeveloperNotebook(item)) {
            ensureDeveloperNotebookSpecialLine(item);
            return false;
        }
        String id = getItemId(item);
        if (id == null || !itemExists(id)) return false;
        rebuildMmoLore(item, mmoStatStorage.read(item), player);
        return true;
    }

    public void ensureDeveloperNotebookSpecialLine(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        String special = CC.color("&c&lSPECIAL");
        lore.removeIf(line -> {
            String plain = ChatColor.stripColor(line);
            return plain != null && plain.equalsIgnoreCase("SPECIAL");
        });
        while (!lore.isEmpty() && lore.getLast().isBlank()) lore.removeLast();
        lore.add("");
        lore.add(special);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private Map<String, String> enchantLoreLines(Map<String, Integer> enchants) {
        Map<String, String> lines = new java.util.LinkedHashMap<>();
        enchants.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String value = pretty(entry.getKey()) + " " + roman(entry.getValue());
            if (customEnchantManager != null) {
                Map<MMOStatType, Double> bonuses = customEnchantManager.statBonusesFor(entry.getKey(), entry.getValue());
                if (!bonuses.isEmpty()) {
                    String summary = bonuses.entrySet().stream()
                            .map(bonus -> bonus.getKey().color() + "+" + formatNumber(bonus.getValue())
                                    + (bonus.getKey().percent() ? "% " : " ") + bonus.getKey().display())
                            .collect(java.util.stream.Collectors.joining("&7, "));
                    value += " &8[&7" + summary + "&8]";
                }
            }
            lines.put(pretty(entry.getKey()), value);
        });
        return lines;
    }

    private String formatNumber(double value) {
        return Math.abs(value - Math.rint(value)) < 0.0001D
                ? String.valueOf((long) Math.rint(value)) : String.format(Locale.US, "%.1f", value);
    }

    private String originalItemName(String id, ItemMeta meta) {
        String edited = meta.getPersistentDataContainer().get(displayNameKey, PersistentDataType.STRING);
        if (edited != null && !edited.isBlank()) {
            String clean = ChatColor.stripColor(CC.color(edited));
            if (clean != null && !clean.isBlank()) return clean;
        }
        String configured = id == null ? null : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".name");
        String name = configured == null || configured.isBlank()
                ? (meta.hasDisplayName() ? meta.getDisplayName() : pretty(id))
                : configured;
        String clean = ChatColor.stripColor(CC.color(name));
        return clean == null || clean.isBlank() ? pretty(id) : clean;
    }

    private String itemRarity(ItemStack item, ItemMeta meta) {
        String explicit = meta.getPersistentDataContainer().get(rarityKey, PersistentDataType.STRING);
        if (explicit != null && !explicit.isBlank()) return explicit;
        String rolled = meta.getPersistentDataContainer().get(itemRarityKey, PersistentDataType.STRING);
        if (rolled != null && !rolled.isBlank()) return rolled;
        if (meta.hasLore() && meta.getLore() != null) {
            for (int i = meta.getLore().size() - 1; i >= 0; i--) {
                String clean = ChatColor.stripColor(CC.color(meta.getLore().get(i)));
                if (clean != null && clean.toUpperCase(Locale.ROOT).matches("(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|SPECIAL|DIVINE).*")) {
                    return clean.split(" ")[0];
                }
            }
        }
        return "COMMON";
    }

    private String abilityName(String id, ItemMeta meta) {
        if (!canHaveRightClickAbility(id)) return null;
        JsonObject ability = jsonObject(meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING));
        String abilityId = ability.has("id") ? ability.get("id").getAsString() : "";
        if ((abilityId == null || abilityId.isBlank()) && id != null) abilityId = getAbilityType(id);
        if (abilityId == null || abilityId.isBlank() || abilityId.equalsIgnoreCase("none")) return null;
        String configuredName = id == null ? "" : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".ability.name", "");
        if (!configuredName.isBlank()) return ChatColor.stripColor(CC.color(configuredName));
        String finalAbilityId = abilityId;
        return abilityRegistry.get(finalAbilityId).map(CustomItemAbility::displayName).orElse(pretty(finalAbilityId));
    }

    private Integer abilityManaCost(String id, ItemMeta meta) {
        if (!canHaveRightClickAbility(id)) return null;
        JsonObject ability = jsonObject(meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING));
        String abilityId = ability.has("id") ? ability.get("id").getAsString() : getAbilityType(id);
        CustomItemAbility registered = abilityRegistry.get(abilityId).orElse(null);
        if (registered != null && !registered.allowedItemTypes().isEmpty()) return registered.manaCost();
        if (ability.has("mana_cost") && ability.get("mana_cost").isJsonPrimitive() && ability.get("mana_cost").getAsJsonPrimitive().isNumber()) {
            return ability.get("mana_cost").getAsInt();
        }
        if (id != null && !getAbilityType(id).equalsIgnoreCase("none")) return getManaCost(id);
        return null;
    }

    private List<String> abilityDescription(String id, ItemMeta meta) {
        JsonObject ability = jsonObject(meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING));
        String abilityId = ability.has("id") ? ability.get("id").getAsString() : getAbilityType(id);
        return abilityRegistry.get(abilityId).map(CustomItemAbility::description).orElse(List.of());
    }

    private String abilityTriggerLabel(ItemStack item, String id) {
        CustomItemAbility ability = abilityRegistry.get(getAbilityType(item, id)).orElse(null);
        if (ability == null) return "RIGHT CLICK";
        return abilityTrigger(item, ability).name().replace('_', ' ');
    }

    private void applyVisualGlow(ItemStack item, boolean enabled) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.setEnchantmentGlintOverride(enabled ? Boolean.TRUE : null);
        item.setItemMeta(meta);
    }

    private void applyMmoTooltipHiding(ItemMeta meta) {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        addItemFlagIfPresent(meta, "HIDE_ADDITIONAL_TOOLTIP");
        addItemFlagIfPresent(meta, "HIDE_DYE");
        addItemFlagIfPresent(meta, "HIDE_ARMOR_TRIM");
        invokeMetaBoolean(meta, "setHideTooltip", false);
        invokeMetaBoolean(meta, "setHideAdditionalTooltip", true);
        invokeMetaBoolean(meta, "setHideUnbreakable", true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addItemFlagIfPresent(ItemMeta meta, String name) {
        try {
            meta.addItemFlags((ItemFlag) Enum.valueOf((Class<Enum>) (Class) ItemFlag.class, name));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void invokeMetaBoolean(ItemMeta meta, String methodName, boolean value) {
        try {
            meta.getClass().getMethod(methodName, boolean.class).invoke(meta, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private String roman(int value) {
        return switch (Math.max(1, value)) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(value);
        };
    }

    private void startEquippedEffectsTask() {
        if (taskId != -1) return;
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                applyEquippedEffects(player);
                activateFromEquipment(player, AbilityTrigger.PASSIVE, null, player.getLocation(), 0.0D, null);
                activateFromEquipment(player, AbilityTrigger.FULL_SET_BONUS, null, player.getLocation(), 0.0D, null);
            }
        }, 20L, 20L).getTaskId();
    }

    private Map<Material, Integer> countNearbyOres(Player player, int radius) {
        Map<Material, Integer> ores = new HashMap<>();
        Block center = player.getLocation().getBlock();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Material type = center.getRelative(x, y, z).getType();
                    if (isValuableOre(type)) ores.merge(type, 1, Integer::sum);
                }
            }
        }
        return ores;
    }

    private String summarizeOres(Map<Material, Integer> ores) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : ores.entrySet()) {
            parts.add(pretty(entry.getKey().name()) + " x" + entry.getValue());
        }
        return String.join("&7, &f", parts);
    }

    private boolean isValuableOre(Material material) {
        return material == Material.DIAMOND_ORE
                || material == Material.DEEPSLATE_DIAMOND_ORE
                || material == Material.EMERALD_ORE
                || material == Material.DEEPSLATE_EMERALD_ORE
                || material == Material.ANCIENT_DEBRIS
                || material == Material.GOLD_ORE
                || material == Material.DEEPSLATE_GOLD_ORE
                || material == Material.REDSTONE_ORE
                || material == Material.DEEPSLATE_REDSTONE_ORE
                || material == Material.LAPIS_ORE
                || material == Material.DEEPSLATE_LAPIS_ORE;
    }

    private boolean isLegacyAbilityCoolingDown(Player player, String id) {
        long cooldownMillis = Math.max(0L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong(path(id) + ".ability.cooldown-seconds", 0L)) * 1000L;
        if (cooldownMillis <= 0L) return false;
        String key = player.getUniqueId() + ":" + normalize(id);
        long now = System.currentTimeMillis();
        long readyAt = legacyAbilityCooldowns.getOrDefault(key, 0L);
        if (readyAt > now) {
            long seconds = Math.max(1L, (readyAt - now + 999L) / 1000L);
            player.sendMessage(CC.color("&cThat item is on cooldown for &e" + seconds + "s&c."));
            return true;
        }
        legacyAbilityCooldowns.put(key, now + cooldownMillis);
        return false;
    }

    private boolean isAbilityCoolingDown(Player player, ItemStack item, String itemId, String abilityId) {
        long cooldownMillis = abilityCooldownMillis(item, itemId);
        if (cooldownMillis <= 0L) return false;
        String key = player.getUniqueId() + ":" + normalize(itemId) + ":" + normalize(abilityId);
        long readyAt = legacyAbilityCooldowns.getOrDefault(key, 0L);
        if (readyAt <= System.currentTimeMillis()) return false;
        long seconds = Math.max(1L, (readyAt - System.currentTimeMillis() + 999L) / 1000L);
        String normalizedAbility = normalize(abilityId);
        String message = normalizedAbility.equals("prospect")
                ? "&cProspect is on cooldown for &e" + seconds + "s&c."
                : normalizedAbility.equals("blood_needle")
                ? "&cThis ability is on cooldown for " + seconds + "s."
                : "&cThat item is on cooldown for &e" + seconds + "s&c.";
        player.sendMessage(CC.color(message));
        return true;
    }

    private void markAbilityCooldown(Player player, ItemStack item, String itemId, String abilityId) {
        long cooldownMillis = abilityCooldownMillis(item, itemId);
        if (cooldownMillis <= 0L) return;
        String key = player.getUniqueId() + ":" + normalize(itemId) + ":" + normalize(abilityId);
        legacyAbilityCooldowns.put(key, System.currentTimeMillis() + cooldownMillis);
    }

    private long abilityCooldownMillis(ItemStack item, String itemId) {
        CustomItemAbility registeredDefinition = abilityRegistry.get(getAbilityType(item, itemId)).orElse(null);
        if (registeredDefinition != null && !registeredDefinition.allowedItemTypes().isEmpty()) {
            return Math.max(0L, registeredDefinition.cooldownSeconds()) * 1000L;
        }
        if (item != null && item.hasItemMeta()) {
            JsonObject stored = jsonObject(item.getItemMeta().getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING));
            for (String key : List.of("cooldown_seconds", "cooldown")) {
                if (stored.has(key) && stored.get(key).isJsonPrimitive() && stored.get(key).getAsJsonPrimitive().isNumber()) {
                    return Math.max(0L, stored.get(key).getAsLong()) * 1000L;
                }
            }
        }
        long configured = Math.max(0L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getLong(path(itemId) + ".ability.cooldown-seconds", 0L));
        if (configured > 0L) return configured * 1000L;
        String abilityId = getAbilityType(item, itemId);
        CustomItemAbility registered = abilityRegistry.get(abilityId).orElse(null);
        return (registered != null && registered.cooldownSeconds() > 0 ? registered.cooldownSeconds() : defaultToolAbilityCooldown(abilityId)) * 1000L;
    }

    private int growNearbyCrops(Player player, int radius, int growthAmount, int maxCrops) {
        int grown = 0;
        Block center = player.getLocation().getBlock();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = center.getRelative(x, y, z);
                    if (!SPRINKLER_CROPS.contains(block.getType())) continue;
                    if (!(block.getBlockData() instanceof Ageable ageable) || ageable.getAge() >= ageable.getMaximumAge()) continue;
                    ageable.setAge(Math.min(ageable.getMaximumAge(), ageable.getAge() + growthAmount));
                    block.setBlockData(ageable);
                    grown++;
                    if (grown >= maxCrops) return grown;
                }
            }
        }
        return grown;
    }

    private void applyEquippedEffects(Player player) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            String id = getItemId(item);
            if (id == null) continue;
            if (!meetsItemRequirements(player, item, false)) continue;
            ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection(path(id) + ".equipped-effects");
            if (section == null) continue;
            for (String effectName : section.getKeys(false)) {
                PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase(Locale.ROOT));
                if (type == null) continue;
                int amplifier = Math.max(0, section.getInt(effectName, 0));
                player.addPotionEffect(new PotionEffect(type, 60, amplifier, true, false, true));
            }
        }
    }

    private int passiveManaReduction(Player player) {
        for (ItemStack item : abilityItems(player)) {
            String id = getItemId(item);
            if (id == null || !canHaveRightClickAbility(id, item)
                    || !abilityRegistry.get(getAbilityType(id)).map(ability -> ability.id().equals("arcane_focus")).orElse(false)) continue;
            return 10;
        }
        return 0;
    }

    private List<ItemStack> abilityItems(Player player) {
        List<ItemStack> items = new ArrayList<>();
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && !main.getType().isAir()) items.add(main);
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && !armor.getType().isAir()) items.add(armor);
        }
        return items;
    }

    private int getManaCost(String id) {
        DatabaseCustomItem databaseItem = databaseItems.get(normalize(id));
        if (databaseItem != null) {
            JsonObject ability = jsonObject(databaseItem.abilitiesJson());
            return ability.has("mana_cost") ? Math.max(0, ability.get("mana_cost").getAsInt()) : 0;
        }
        return Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(id) + ".ability.mana-cost", 0));
    }

    private boolean isConsumableId(String id) {
        String key = normalize(id);
        String customType = configuredCustomItemType(key);
        if (Set.of("DEVELOPER_NOTEBOOK", "COOKING_INGREDIENT", "FISHING_BAIT", "BAIT")
                .contains(customType) || key.equals("developer_notebook") || isFishingBaitId(key)) return false;
        if ((customType.equals("CUSTOM_FISH") || customType.equals("FISH"))
                && !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(path(key) + ".food.edible", false)) return false;
        if (customType.equals("CUSTOM_FOOD") || customType.equals("FOOD")) return true;
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(path(key) + ".consumable.enabled", false)) return true;
        MMOItemType type = configuredMmoItemType(key);
        if (type == MMOItemType.FOOD || type == MMOItemType.POTION || type == MMOItemType.CONSUMABLE) return true;
        if (type == MMOItemType.MATERIAL) return false;
        String category = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(key) + ".category", "");
        if (category.equalsIgnoreCase("food") || category.equalsIgnoreCase("potions") || category.equalsIgnoreCase("potion")) return true;
        Material material = getMaterial(key);
        return material == Material.POTION || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION
                || (material.isEdible() && !category.equalsIgnoreCase("materials"));
    }

    private boolean isBackpackId(String id) {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection(path(id) + ".backpack");
    }

    private boolean isFishingBagId(String id) {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection(path(id) + ".fishing-bag");
    }

    private int backpackSize(String id) {
        int rows = Math.clamp(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(id) + ".backpack.rows", 3), 1, 6);
        int size = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(id) + ".backpack.size", rows * 9);
        size = Math.clamp(size, 9, 54);
        return Math.max(1, size / 9) * 9;
    }

    private String backpackUuid(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(backpackKey, PersistentDataType.STRING);
    }

    private int fishingBagSize(String id, String type) {
        int size = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(id) + ".fishing-bag." + type + "-size", type.equals("drops") ? 54 : 27);
        size = Math.clamp(size, 9, 54);
        return Math.max(1, size / 9) * 9;
    }

    private String ensureFishingBagUuid(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        String uuid = meta.getPersistentDataContainer().get(fishingBagKey, PersistentDataType.STRING);
        if (uuid == null || uuid.isBlank()) {
            uuid = UUID.randomUUID().toString();
            meta.getPersistentDataContainer().set(fishingBagKey, PersistentDataType.STRING, uuid);
            item.setItemMeta(meta);
        }
        return uuid;
    }

    private ItemStack firstFishingBag(Player player) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (isFishingBag(item)) return item;
        }
        return isFishingBag(player.getInventory().getItemInOffHand()) ? player.getInventory().getItemInOffHand() : null;
    }

    private ItemStack menuItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            setSafeLore(meta, CC.colorList(lore), null);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack editorTextButton(Material material, String name, String current, String field) {
        return menuItem(material, name, List.of("&7Current: &f" + current, "", "&eClick and enter a value in chat.", "&8Edit Item Text: " + field));
    }

    private void addToggleButton(Inventory inventory, int slot, Material material, String name, ItemStack item, String flag) {
        boolean enabled = getFlag(item, flag);
        inventory.setItem(slot, menuItem(material, name, List.of(
                "&7Enabled: " + (enabled ? "&aYes" : "&cNo"), "", "&eClick to toggle.", "&8Edit Item Flag: " + flag
        )));
    }

    private ItemStack economyValueButton(Material material, String name, ItemStack item, String field, double calculated) {
        double manual = editorEconomyValue(item, field, calculated);
        return menuItem(material, "&6" + name, List.of(
                "&7Manual/Current: &6" + formatEditorNumber(manual),
                "&7Calculated: &e" + formatEditorNumber(calculated),
                "", "&eLeft Click: &a+100", "&eRight Click: &c-100", "&eShift Left: &a+1000",
                "&eShift Right: &c-1000", "&eMiddle Click: &7Reset", "&8Edit Item Economy: " + field
        ));
    }

    private double editorEconomyValue(ItemStack item, String field, double fallback) {
        if (item == null || !item.hasItemMeta()) return Math.max(0.0D, fallback);
        if (normalize(field).equals("sell_price")) {
            Double value = item.getItemMeta().getPersistentDataContainer().get(sellPriceKey, PersistentDataType.DOUBLE);
            return value == null ? Math.max(0.0D, fallback) : Math.max(0.0D, value);
        }
        JsonObject nbt = jsonObject(item.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING));
        String key = normalize(field);
        return nbt.has(key) && nbt.get(key).isJsonPrimitive() && nbt.get(key).getAsJsonPrimitive().isNumber()
                ? Math.max(0.0D, nbt.get(key).getAsDouble()) : Math.max(0.0D, fallback);
    }

    private String editorNbtValue(ItemStack item, String key, String fallback) {
        if (item == null || !item.hasItemMeta()) return fallback;
        JsonObject nbt = jsonObject(item.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING));
        return nbt.has(key) && nbt.get(key).isJsonPrimitive() ? nbt.get(key).getAsString() : fallback;
    }

    private String editorModelData(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return "not set";
        return String.valueOf(item.getItemMeta().getCustomModelData());
    }

    private String formatEditorNumber(double value) {
        if (!Double.isFinite(value)) return "0";
        if (Math.rint(value) == value) return String.format(Locale.US, "%,.0f", value);
        return String.format(Locale.US, "%,.2f", value);
    }

    private boolean isRelevantStat(MMOItemType itemType, MMOStatType stat) {
        return switch (itemType) {
            case WEAPON, SWORD, KATANA, DAGGER, SPEAR, AXE, BOW, CROSSBOW, STAFF, WAND -> List.of(MMOStatType.DAMAGE, MMOStatType.STRENGTH, MMOStatType.CRIT_CHANCE, MMOStatType.CRIT_DAMAGE, MMOStatType.FEROCITY, MMOStatType.LIFESTEAL, MMOStatType.MANA, MMOStatType.MANA_REGEN, MMOStatType.INTELLIGENCE, MMOStatType.ABILITY_DAMAGE).contains(stat);
            case ARMOR, SHIELD -> List.of(MMOStatType.HEALTH, MMOStatType.DEFENSE, MMOStatType.TRUE_DEFENSE, MMOStatType.SPEED, MMOStatType.VITALITY, MMOStatType.MANA).contains(stat);
            case TOOL -> List.of(MMOStatType.MINING_SPEED, MMOStatType.MINING_FORTUNE, MMOStatType.BREAKING_POWER).contains(stat);
            case FARMING_EQUIPMENT -> List.of(MMOStatType.FARMING_FORTUNE, MMOStatType.REPLANT_CHANCE, MMOStatType.CROP_GROWTH).contains(stat);
            case FISHING_ROD, ROD -> List.of(MMOStatType.FISHING_SPEED, MMOStatType.FISHING_FORTUNE, MMOStatType.SEA_CREATURE_CHANCE, MMOStatType.TREASURE_CHANCE).contains(stat);
            case ACCESSORY, RING, NECKLACE, TRINKET, CHARM, TOTEM -> true;
            default -> List.of(MMOStatType.MAGIC_FIND, MMOStatType.PET_LUCK, MMOStatType.XP_BOOST).contains(stat);
        };
    }

    private Material statMaterial(MMOStatType stat) {
        return switch (stat) {
            case DAMAGE, STRENGTH, FEROCITY -> Material.IRON_SWORD;
            case CRIT_CHANCE, CRIT_DAMAGE, LIFESTEAL -> Material.NETHER_STAR;
            case HEALTH, VITALITY -> Material.RED_DYE;
            case DEFENSE, TRUE_DEFENSE -> Material.SHIELD;
            case MANA, MANA_REGEN, INTELLIGENCE, ABILITY_DAMAGE -> Material.LAPIS_LAZULI;
            case SPEED -> Material.SUGAR;
            case MINING_SPEED, MINING_FORTUNE, BREAKING_POWER -> Material.DIAMOND_PICKAXE;
            case FARMING_FORTUNE, REPLANT_CHANCE, CROP_GROWTH -> Material.DIAMOND_HOE;
            case FISHING_SPEED, FISHING_FORTUNE, SEA_CREATURE_CHANCE, TREASURE_CHANCE -> Material.FISHING_ROD;
            default -> Material.EMERALD;
        };
    }

    private Material itemTypeMaterial(MMOItemType type) {
        return switch (type) {
            case WEAPON, SWORD, KATANA, DAGGER, SPEAR, AXE -> Material.DIAMOND_SWORD;
            case BOW, CROSSBOW -> Material.BOW;
            case STAFF, WAND -> Material.BLAZE_ROD;
            case ARMOR -> Material.DIAMOND_CHESTPLATE;
            case TOOL -> Material.DIAMOND_PICKAXE;
            case SHIELD -> Material.SHIELD;
            case FISHING_ROD, ROD -> Material.FISHING_ROD;
            case FISHING_BAIT, BAIT -> Material.STRING;
            case RING, NECKLACE, TRINKET, ACCESSORY -> Material.EMERALD;
            case FARMING_EQUIPMENT -> Material.DIAMOND_HOE;
            case FOOD, CONSUMABLE -> Material.APPLE;
            case POTION -> Material.POTION;
            case MATERIAL -> Material.IRON_INGOT;
            case COMPANION -> Material.PLAYER_HEAD;
            default -> Material.CHEST;
        };
    }

    private void applyEditorItemFlag(ItemMeta meta, String flag, boolean enabled) {
        ItemFlag itemFlag = switch (flag) {
            case "hide_attributes" -> ItemFlag.HIDE_ATTRIBUTES;
            case "hide_enchants" -> ItemFlag.HIDE_ENCHANTS;
            case "hide_unbreakable" -> ItemFlag.HIDE_UNBREAKABLE;
            default -> null;
        };
        if (itemFlag != null) {
            if (enabled) meta.addItemFlags(itemFlag);
            else meta.removeItemFlags(itemFlag);
        }
        if (flag.equals("hide_additional_tooltip")) {
            try {
                ItemFlag additional = ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP");
                if (enabled) meta.addItemFlags(additional); else meta.removeItemFlags(additional);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (flag.equals("glowing")) meta.setEnchantmentGlintOverride(enabled);
    }

    private void fillMenu(Inventory inventory) {
        ItemStack filler = menuItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
        }
    }

    private String getAbilityType(String id) {
        DatabaseCustomItem databaseItem = databaseItems.get(normalize(id));
        if (databaseItem != null) {
            JsonObject ability = jsonObject(databaseItem.abilitiesJson());
            return ability.has("id") ? ability.get("id").getAsString() : "none";
        }
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".ability.type", "none");
    }

    private String getAbilityType(ItemStack item, String id) {
        if (item != null && item.hasItemMeta()) {
            String direct=item.getItemMeta().getPersistentDataContainer().get(abilityIdKey,PersistentDataType.STRING);
            if(direct!=null&&!direct.isBlank())return normalize(direct);
            JsonObject stored = jsonObject(item.getItemMeta().getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING));
            if (stored.has("id") && stored.get("id").isJsonPrimitive()) {
                String value = stored.get("id").getAsString();
                if (value != null && !value.isBlank()) return normalize(value);
            }
        }
        return getAbilityType(id);
    }

    private int abilityManaCost(ItemStack item, String id, CustomItemAbility ability) {
        if (ability != null && !ability.allowedItemTypes().isEmpty()) return Math.max(0, ability.manaCost());
        if (item != null && item.hasItemMeta()) {
            JsonObject stored = jsonObject(item.getItemMeta().getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING));
            if (stored.has("mana_cost") && stored.get("mana_cost").isJsonPrimitive()
                    && stored.get("mana_cost").getAsJsonPrimitive().isNumber()) {
                return Math.max(0, stored.get("mana_cost").getAsInt());
            }
        }
        return ability == null ? getManaCost(id) : Math.max(0, ability.manaCost());
    }

    private AbilityTrigger abilityTrigger(ItemStack item, CustomItemAbility ability) {
        if (ability != null && !ability.allowedItemTypes().isEmpty()) return ability.trigger();
        if (item != null && item.hasItemMeta()) {
            JsonObject stored = jsonObject(item.getItemMeta().getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING));
            if (stored.has("trigger") && stored.get("trigger").isJsonPrimitive()) {
                try {
                    return AbilityTrigger.valueOf(stored.get("trigger").getAsString().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return Objects.requireNonNull(ability).trigger();
    }

    private String path(String id) {
        return "custom-items.items." + normalize(id);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
    }

    private Material parseMaterial(String value) {
        try {
            return Material.valueOf(Objects.requireNonNull(value).toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return Material.AIR;
        }
    }

    private void play(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1F, 1F);
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "Item";
        text = text.toLowerCase(Locale.ROOT).replace('_', ' ');
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private String readString(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String trim(String value, int maxLength) {
        String normalized = value == null ? "" : value;
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String validJson(String value) {
        if (value == null || value.isBlank()) return "{}";
        try {
            JsonParser.parseString(value);
            return value;
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private JsonObject jsonObject(String value) {
        try {
            if (value == null || value.isBlank()) return new JsonObject();
            return JsonParser.parseString(value).getAsJsonObject();
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> lines = new ArrayList<>();
            for (com.google.gson.JsonElement element : JsonParser.parseString(json).getAsJsonArray()) {
                if (element.isJsonPrimitive()) lines.add(element.getAsString());
            }
            return lines;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public enum SaveResult {
        SAVED,
        INVALID_ID,
        INVALID_ITEM,
        DATABASE_DISABLED,
        STATIC_ID,
        ERROR
    }

    public enum MaxItemStatus {
        SUCCESS,
        DEVELOPER_NOTEBOOK,
        INVALID
    }

    public record MaxItemResult(MaxItemStatus status, String itemId, int enchantmentsApplied) {
    }

    private record DatabaseCustomItem(String id, String itemType, String material, String displayName, List<String> lore,
                                      String rarity, String statsJson, String abilitiesJson, String flagsJson,
                                      String nbtJson) {
    }

    private record StatRoll(MMOStatMap stats, boolean random, double quality) {
    }

    private record StatRange(double min, double max) {
    }

    private record LevelRange(int min, int max) {
    }

    private record RollValue(double value, double quality) {
    }
}
