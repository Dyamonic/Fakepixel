package club.abelix.games.fakevival.mmo;

import club.abelix.games.fakevival.config.RuntimeSettings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class MMOItemValueService {
    private final JavaPlugin plugin;
    private final MMOStatStorage statStorage;
    private final MMOEnchantStorage enchantStorage;
    private final NamespacedKey itemKey;
    private final NamespacedKey customTypeKey;
    private final NamespacedKey customRarityKey;
    private final NamespacedKey itemRarityKey;
    private final NamespacedKey abilityKey;
    private final NamespacedKey requirementsKey;
    private final NamespacedKey nbtKey;
    private final NamespacedKey sellPriceKey;

    public MMOItemValueService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.statStorage = new MMOStatStorage(plugin);
        this.enchantStorage = new MMOEnchantStorage(plugin);
        this.itemKey = new NamespacedKey(plugin, "custom_item_id");
        this.customTypeKey = new NamespacedKey(plugin, "custom_item_type");
        this.customRarityKey = new NamespacedKey(plugin, "custom_item_rarity");
        this.itemRarityKey = new NamespacedKey(plugin, "item_rarity");
        this.abilityKey = new NamespacedKey(plugin, "custom_item_ability");
        this.requirementsKey = new NamespacedKey(plugin, "mmo_requirements");
        this.nbtKey = new NamespacedKey(plugin, "custom_item_nbt");
        this.sellPriceKey = new NamespacedKey(plugin, "custom_item_sell_price");
    }

    public double calculateValue(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return 0.0D;
        String customId = customItemId(item);
        if (customId.isBlank()) return 0.0D;
        String rarity = rarity(item, customId);
        if (normalize(rarity).equals("special")) return 0.0D;
        MMOItemType type = itemType(item, customId);
        double manualBase = manualValue(item, "base_value");
        double value = manualBase >= 0.0D ? manualBase : rarityBaseValue(rarity) * calculateRarityMultiplier(item) * typeMultiplier(type);
        value += calculateStatValue(item);
        value += calculateEnchantValue(item);
        value += calculateAbilityValue(item, type, value);
        if (type.canRollRandomRarityEffects()) {
            value *= calculateRequirementModifier(item);
            value *= rollQualityMultiplier(item);
        }
        if (!Double.isFinite(value) || value <= 0.0D) return 0.0D;
        return Math.max(1.0D, Math.round(value));
    }

    public double calculateBaseValue(String customItemId) {
        if (customItemId == null || customItemId.isBlank()) return 0.0D;
        String id = normalize(customItemId);
        String path = path(id);
        String rarity = RuntimeSettings.config(plugin).getString(path + ".rarity", "COMMON");
        MMOItemType type = MMOItemType.parse(RuntimeSettings.config(plugin).getString(path + ".item-type",
                RuntimeSettings.config(plugin).getString(path + ".type", "UTILITY")));
        return rarityBaseValue(rarity) * rarityMultiplier(rarity) * typeMultiplier(type);
    }

    public double calculateStatValue(ItemStack item) {
        MMOStatMap stats = statStorage.read(item);
        double value = 0.0D;
        for (Map.Entry<MMOStatType, Double> entry : stats.values().entrySet()) {
            value += Math.max(0.0D, entry.getValue()) * statWeight(entry.getKey());
        }
        return value;
    }

    public double calculateRequirementModifier(ItemStack item) {
        int highest = 0;
        for (int level : requirements(item).values()) highest = Math.max(highest, level);
        if (highest <= 5) return 1.0D;
        if (highest <= 15) return 1.03D;
        if (highest <= 25) return 1.06D;
        if (highest <= 35) return 1.10D;
        return 1.15D;
    }

    public double calculateRarityMultiplier(ItemStack item) {
        String customId = customItemId(item);
        return rarityMultiplier(rarity(item, customId));
    }

    public double calculateEnchantValue(ItemStack item) {
        Map<String, Integer> enchants = enchantStorage.read(item);
        if (enchants.isEmpty()) return 0.0D;
        double rarityBase = rarityBaseValue(rarity(item, customItemId(item)));
        double value = 0.0D;
        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            int level = Math.max(0, entry.getValue());
            if (level == 0) continue;
            value += enchantWeight(entry.getKey()) * level * (1.0D + Math.max(0, level - 1) * 0.25D);
        }
        return value * Math.max(1.0D, rarityBase / 2500.0D);
    }

    public double getSellValue(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            Double manual = item.getItemMeta().getPersistentDataContainer().get(sellPriceKey, PersistentDataType.DOUBLE);
            if (manual != null) return Math.max(0.0D, manual);
        }
        double value = calculateValue(item);
        if (value <= 0.0D) return 0.0D;
        return Math.max(1.0D, Math.round(value * sellMultiplier(rarity(item, customItemId(item)))));
    }

    public double getAuctionSuggestedPrice(ItemStack item) {
        double manual = manualValue(item, "auction_value");
        if (manual >= 0.0D) return manual;
        double value = calculateValue(item);
        if (value <= 0.0D) return 0.0D;
        return Math.max(1.0D, Math.round(value * 1.10D));
    }

    public double getTradeDisplayValue(ItemStack item) {
        double manual = manualValue(item, "trade_value");
        return manual >= 0.0D ? manual : calculateValue(item);
    }

    private double manualValue(ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) return -1.0D;
        String raw = item.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return -1.0D;
        try {
            JsonObject object = JsonParser.parseString(raw).getAsJsonObject();
            return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber()
                    ? Math.max(0.0D, object.get(key).getAsDouble()) : -1.0D;
        } catch (Exception ignored) {
            return -1.0D;
        }
    }

    public boolean hasRandomizedData(ItemStack item) {
        return statStorage.hasRandomRoll(item) || !requirements(item).isEmpty();
    }

    private double calculateAbilityValue(ItemStack item, MMOItemType type, double currentValue) {
        if (!canHaveAbilityValue(type) || item == null || !item.hasItemMeta()) return 0.0D;
        String ability = abilityId(item.getItemMeta());
        if (ability.isBlank() || ability.equalsIgnoreCase("none")) return 0.0D;
        double manaCost = abilityManaCost(item.getItemMeta());
        return Math.max(250.0D, currentValue * 0.08D) + Math.max(0.0D, manaCost) * 12.0D;
    }

    private boolean canHaveAbilityValue(MMOItemType type) {
        if (type == null || !type.canRollRandomRarityEffects()) return false;
        return switch (type) {
            case ARMOR, SHIELD, RING, NECKLACE, TRINKET, ACCESSORY, BOW, FOOD, POTION, MATERIAL, CONSUMABLE -> false;
            default -> true;
        };
    }

    private double rollQualityMultiplier(ItemStack item) {
        double quality = statStorage.readRollQuality(item);
        if (!Double.isFinite(quality)) return 1.0D;
        if (quality < 25.0D) return 0.85D;
        if (quality < 45.0D) return 0.95D;
        if (quality < 70.0D) return 1.0D;
        if (quality < 90.0D) return 1.08D;
        if (quality < 100.0D) return 1.15D;
        return 1.25D;
    }

    private double sellMultiplier(String rarity) {
        return switch (normalize(rarity)) {
            case "mythic" -> 0.15D;
            case "legendary" -> 0.18D;
            case "epic" -> 0.22D;
            case "rare" -> 0.25D;
            case "uncommon" -> 0.30D;
            case "special" -> 0.0D;
            default -> 0.35D;
        };
    }

    private double rarityBaseValue(String rarity) {
        return switch (normalize(rarity)) {
            case "uncommon" -> 500.0D;
            case "rare" -> 2500.0D;
            case "epic" -> 10_000.0D;
            case "legendary" -> 50_000.0D;
            case "mythic" -> 250_000.0D;
            case "special" -> 0.0D;
            default -> 100.0D;
        };
    }

    private double rarityMultiplier(String rarity) {
        return switch (normalize(rarity)) {
            case "uncommon" -> 1.05D;
            case "rare" -> 1.15D;
            case "epic" -> 1.30D;
            case "legendary" -> 1.55D;
            case "mythic" -> 1.90D;
            default -> 1.0D;
        };
    }

    private double typeMultiplier(MMOItemType type) {
        return switch (type) {
            case FOOD, MATERIAL, CONSUMABLE -> 1.0D;
            case POTION -> 1.2D;
            case TOOL -> 1.4D;
            case FARMING_EQUIPMENT, FISHING_ROD, ROD, SHIELD -> 1.5D;
            case WEAPON, SWORD, KATANA, DAGGER, BOW -> 1.8D;
            case STAFF, WAND -> 2.0D;
            case ARMOR -> 1.7D;
            case RING, NECKLACE, TRINKET, ACCESSORY -> 2.2D;
            case COMPANION -> 2.5D;
            default -> 1.0D;
        };
    }

    private double statWeight(MMOStatType type) {
        return switch (type) {
            case DAMAGE, PROJECTILE_DAMAGE -> 20.0D;
            case STRENGTH -> 18.0D;
            case CRIT_CHANCE -> 120.0D;
            case CRIT_DAMAGE -> 25.0D;
            case HEALTH -> 8.0D;
            case DEFENSE -> 12.0D;
            case TRUE_DEFENSE -> 200.0D;
            case MANA, MANA_REGEN, INTELLIGENCE -> 8.0D;
            case ABILITY_DAMAGE -> 90.0D;
            case SPEED -> 100.0D;
            case VITALITY -> 150.0D;
            case MAGIC_FIND -> 400.0D;
            case PET_LUCK -> 400.0D;
            case XP_BOOST, FARMING_XP_BOOST, FISHING_XP_BOOST, ALCHEMY_XP_BOOST,
                    ENCHANTING_XP_BOOST, REPAIR_XP_BOOST, SMELTING_XP_BOOST -> 250.0D;
            case MINING_SPEED, FORAGING_SPEED, DIGGING_SPEED -> 4.0D;
            case MINING_FORTUNE, FARMING_FORTUNE, FISHING_SPEED, FISHING_FORTUNE, FORAGING_FORTUNE, DIGGING_FORTUNE, EXCAVATION_FORTUNE -> 35.0D;
            case FISHING_BAR_SIZE, FISHING_PROGRESS_BONUS -> 60.0D;
            case BREAKING_POWER -> 1000.0D;
            case REPLANT_CHANCE -> 120.0D;
            case CROP_GROWTH, SEA_CREATURE_CHANCE -> 250.0D;
            case TREASURE_CHANCE -> 300.0D;
            case HEALTH_RESTORE, MANA_RESTORE -> 6.0D;
            case FEROCITY -> 90.0D;
            case LIFESTEAL -> 220.0D;
            case HARVEST_RANGE -> 300.0D;
            case DOUBLE_DROP_CHANCE -> 140.0D;
        };
    }

    private double enchantWeight(String id) {
        String normalized = normalize(id);
        if (normalized.contains("fortune") || normalized.contains("looting") || normalized.contains("luck")) return 450.0D;
        if (normalized.contains("sharp") || normalized.contains("power") || normalized.contains("smite")) return 300.0D;
        if (normalized.contains("protection")) return 275.0D;
        if (normalized.contains("efficiency")) return 220.0D;
        if (normalized.contains("mending") || normalized.contains("silk_touch") || normalized.contains("infinity")) return 900.0D;
        if (normalized.contains("unbreaking")) return 175.0D;
        return 200.0D;
    }

    private Map<String, Integer> requirements(ItemStack item) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (item == null || !item.hasItemMeta()) return result;
        String raw = item.getItemMeta().getPersistentDataContainer().get(requirementsKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return result;
        try {
            JsonObject object = JsonParser.parseString(raw).getAsJsonObject();
            for (String key : object.keySet()) {
                JsonElement element = object.get(key);
                if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                    int level = element.getAsInt();
                    if (level > 0) result.put(normalize(key), level);
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private MMOItemType itemType(ItemStack item, String customId) {
        MMOItemType stored = statStorage.readItemType(item);
        if (stored != MMOItemType.UTILITY || customId.isBlank()) return stored;
        if (item != null && item.hasItemMeta()) {
            String customType = item.getItemMeta().getPersistentDataContainer().get(customTypeKey, PersistentDataType.STRING);
            if (customType != null && !customType.isBlank()) return MMOItemType.parse(customType);
        }
        return MMOItemType.parse(RuntimeSettings.config(plugin).getString(path(customId) + ".item-type",
                RuntimeSettings.config(plugin).getString(path(customId) + ".type", "UTILITY")));
    }

    private String rarity(ItemStack item, String customId) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            String custom = meta.getPersistentDataContainer().get(customRarityKey, PersistentDataType.STRING);
            if (custom != null && !custom.isBlank()) return custom;
            String rolled = meta.getPersistentDataContainer().get(itemRarityKey, PersistentDataType.STRING);
            if (rolled != null && !rolled.isBlank()) return rolled;
        }
        if (customId == null || customId.isBlank()) return "COMMON";
        return RuntimeSettings.config(plugin).getString(path(customId) + ".rarity", "COMMON");
    }

    private String customItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        String id = item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        return normalize(id);
    }

    private String abilityId(ItemMeta meta) {
        String raw = meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return "";
        try {
            JsonObject object = JsonParser.parseString(raw).getAsJsonObject();
            JsonElement id = object.get("id");
            if (id == null) id = object.get("type");
            return id == null ? "" : normalize(id.getAsString());
        } catch (Exception ignored) {
            return "";
        }
    }

    private double abilityManaCost(ItemMeta meta) {
        String raw = meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return 0.0D;
        try {
            JsonObject object = JsonParser.parseString(raw).getAsJsonObject();
            JsonElement mana = object.get("mana");
            if (mana == null) mana = object.get("mana-cost");
            if (mana == null) mana = object.get("mana_cost");
            return mana != null && mana.isJsonPrimitive() && mana.getAsJsonPrimitive().isNumber() ? mana.getAsDouble() : 0.0D;
        } catch (Exception ignored) {
            return 0.0D;
        }
    }

    private String path(String customId) {
        return "custom-items.items." + normalize(customId);
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
