package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.config.RuntimeSettings;
import club.abelix.games.fakevival.mmo.MMOItemType;
import club.abelix.games.fakevival.mmo.MMOStatMap;
import club.abelix.games.fakevival.mmo.MMOStatStorage;
import club.abelix.games.fakevival.mmo.MMOStatType;
import club.abelix.games.fakevival.util.CC;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ReforgeManager {
    private static final List<String> RARITIES = List.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC");
    private static final Set<String> BLOCKED_TYPES = Set.of("MATERIAL", "MINING_MATERIAL", "FORAGING_MATERIAL", "FARMING_MATERIAL", "DUNGEON_MATERIAL", "COMBAT_MATERIAL", "FISHING_MATERIAL", "MAGIC_MATERIAL", "RECIPE_MATERIAL", "COOKING_INGREDIENT", "CUSTOM_FOOD", "FOOD", "CUSTOM_FISH", "FISH", "TOKEN", "EVENT_TOKEN", "DUNGEON_KEY", "QUEST_ITEM", "FURNITURE", "COSMETIC", "SPECIAL", "DEVELOPER_NOTEBOOK");
    private static final String DEFINITIONS = """
            sharp|Sharp|SWORD,KATANA,DAGGER,GREATSWORD|sharp_stone|STRENGTH=5,8,15,25,35,50;CRIT_DAMAGE=5,10,20,35,50,70
            spicy|Spicy|SWORD,KATANA,DAGGER,GREATSWORD|spicy_stone|DAMAGE=3,6,12,20,35,50;CRIT_DAMAGE=10,15,30,50,75,100
            heroic|Heroic|SWORD,KATANA,DAGGER,GREATSWORD,WAND,STAFF|heroic_stone|MANA=20,35,60,100,160,250;ABILITY_DAMAGE=5,10,18,30,45,65
            legendary|Legendary|SWORD,KATANA,DAGGER,GREATSWORD|legendary_stone|DAMAGE=5,10,20,35,55,80;STRENGTH=5,10,20,35,55,80;CRIT_CHANCE=2,3,5,7,10,15
            reinforced|Reinforced|ARMOR,HELMET,CHESTPLATE,LEGGINGS,BOOTS|reinforced_stone|DEFENSE=8,15,30,60,100,160
            wise|Wise|ARMOR,HELMET,CHESTPLATE,LEGGINGS,BOOTS,WAND,STAFF|wise_stone|MANA=15,30,60,120,200,320
            titanic|Titanic|ARMOR,HELMET,CHESTPLATE,LEGGINGS,BOOTS|titanic_stone|HEALTH=20,40,80,150,250,400;DEFENSE=5,10,25,50,85,140
            light|Light|ARMOR,HELMET,CHESTPLATE,LEGGINGS,BOOTS|light_stone|SPEED=2,4,6,9,12,16;MANA=5,10,25,50,80,120
            prospector|Prospector|PICKAXE,DRILL|prospector_stone|MINING_SPEED=25,50,100,175,275,400;MINING_FORTUNE=5,10,20,35,55,80
            lumberjack|Lumberjack|AXE,FORAGING_AXE|lumberjack_stone|FORAGING_SPEED=20,40,80,140,220,320;FORAGING_FORTUNE=5,10,20,35,55,80
            harvesting|Harvesting|HOE,FARMING_HOE|harvesting_stone|FARMING_FORTUNE=5,12,25,45,75,110
            digger|Digger|SHOVEL|digger_stone|DIGGING_SPEED=20,40,80,140,220,320;DIGGING_FORTUNE=5,10,20,35,55,80
            angler|Angler|FISHING_ROD|angler_stone|FISHING_FORTUNE=5,10,20,35,55,80;FISHING_SPEED=5,10,20,35,55,80
            steady|Steady|FISHING_ROD|steady_stone|FISHING_BAR_SIZE=2,4,8,14,22,32;FISHING_PROGRESS_BONUS=2,4,8,14,22,32
            balanced|Balanced|RING,NECKLACE,CHARM,TOTEM,ACCESSORY|balanced_stone|HEALTH=5,10,25,50,90,140;DEFENSE=2,4,10,20,35,55;MANA=5,10,25,50,90,140
            bloody|Bloody|RING,NECKLACE,CHARM,TOTEM,ACCESSORY|bloody_stone|STRENGTH=3,6,12,22,35,55;CRIT_DAMAGE=5,10,20,35,55,80
            """;
    private final Game plugin;
    private final CustomItemManager items;
    private final MMOStatStorage storage;
    private final Map<String, Definition> definitions = new LinkedHashMap<>();
    private final NamespacedKey reforgeIdKey;
    private final NamespacedKey reforgeNameKey;
    private final NamespacedKey reforgeStatsKey;
    private final NamespacedKey reforgedAtKey;
    private final NamespacedKey reforgedByKey;
    private final NamespacedKey stoneKey;

    public ReforgeManager(Game plugin, CustomItemManager items) {
        this.plugin = plugin;
        this.items = items;
        this.storage = new MMOStatStorage(plugin);
        this.reforgeIdKey = new NamespacedKey(plugin, "reforge_id");
        this.reforgeNameKey = new NamespacedKey(plugin, "reforge_name");
        this.reforgeStatsKey = new NamespacedKey(plugin, "reforge_stats_json");
        this.reforgedAtKey = new NamespacedKey(plugin, "reforged_at");
        this.reforgedByKey = new NamespacedKey(plugin, "reforged_by");
        this.stoneKey = new NamespacedKey(plugin, "reforge_stone");
        load();
    }

    private void load() {
        for (String line : DEFINITIONS.lines().filter(value -> !value.isBlank()).toList()) {
            String[] parts = line.split("\\|", 5);
            Set<String> allowed = new LinkedHashSet<>(List.of(parts[2].split(",")));
            Map<String, MMOStatMap> tiers = new LinkedHashMap<>();
            for (String rarity : RARITIES) tiers.put(rarity, MMOStatMap.empty());
            for (String statPart : parts[4].split(";")) {
                String[] pair = statPart.split("=", 2);
                MMOStatType type = MMOStatType.parse(pair[0]);
                String[] values = pair[1].split(",");
                if (type == null || values.length != RARITIES.size()) throw new IllegalStateException("Invalid reforge stat package: " + line);
                for (int index = 0; index < values.length; index++) tiers.get(RARITIES.get(index)).set(type, Double.parseDouble(values[index]));
            }
            Definition definition = new Definition(parts[0], parts[1], Set.copyOf(allowed), parts[3], Map.copyOf(tiers));
            if (definitions.put(definition.id(), definition) != null) throw new IllegalStateException("Duplicate reforge ID: " + definition.id());
        }
    }

    public Definition get(String id) {
        return id == null ? null : definitions.get(normalize(id));
    }

    public List<Definition> all() {
        return List.copyOf(definitions.values());
    }

    public int cost(String rarity) {
        int fallback = switch (normalize(rarity).toUpperCase(Locale.ROOT)) {
            case "UNCOMMON" -> 2500;
            case "RARE" -> 10000;
            case "EPIC" -> 25000;
            case "LEGENDARY" -> 75000;
            case "MYTHIC" -> 150000;
            default -> 1000;
        };
        return Math.max(0, RuntimeSettings.config(plugin).getInt("reforges.costs." + normalize(rarity), fallback));
    }

    public MMOStatMap stats(Definition definition, String rarity) {
        if (definition == null) return MMOStatMap.empty();
        return definition.stats().getOrDefault(normalize(rarity).toUpperCase(Locale.ROOT), definition.stats().get("COMMON"));
    }

    public Definition stoneDefinition(ItemStack stone) {
        if (stone == null || !stone.hasItemMeta()) return null;
        PersistentDataContainer pdc = stone.getItemMeta().getPersistentDataContainer();
        String type = pdc.get(new NamespacedKey(plugin, "custom_item_type"), PersistentDataType.STRING);
        Byte marker = pdc.get(stoneKey, PersistentDataType.BYTE);
        String id = pdc.get(reforgeIdKey, PersistentDataType.STRING);
        Definition definition = get(id);
        String itemId = items.getItemId(stone);
        return "REFORGE_STONE".equalsIgnoreCase(type) && marker != null && marker == 1 && definition != null && definition.stoneId().equals(itemId) ? definition : null;
    }

    public boolean canReforge(ItemStack item, Definition definition) {
        if (item == null || item.getType().isAir() || definition == null || !item.hasItemMeta()) return false;
        String itemId = items.getItemId(item);
        if (itemId == null || !items.itemExists(itemId) || "developer_notebook".equals(itemId)) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (trueFlag(pdc, "developer_notebook") || trueFlag(pdc, "shop_preview_item") || trueFlag(pdc, "preview_item") || trueFlag(pdc, "admin_item") || trueFlag(pdc, "random_stats_blocked") || falseFlag(pdc, "reforge_allowed")) return false;
        String type = items.getCustomItemType(item).toUpperCase(Locale.ROOT);
        return !BLOCKED_TYPES.contains(type) && definition.allowedTypes().contains(type);
    }

    public ItemStack apply(ItemStack source, Definition definition, String rarity, UUID playerId) {
        ItemStack item = source.clone();
        MMOStatMap current = storage.read(item);
        MMOStatMap previous = readStoredStats(item);
        MMOStatMap added = stats(definition, rarity);
        MMOStatMap merged = MMOStatMap.empty();
        for (MMOStatType type : MMOStatType.values()) {
            double value = current.get(type) - previous.get(type) + added.get(type);
            if (value > 0.0D) merged.set(type, value);
        }
        MMOItemType itemType = storage.readItemType(item);
        storage.write(item, itemType, merged);
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(reforgeIdKey, PersistentDataType.STRING, definition.id());
        pdc.set(reforgeNameKey, PersistentDataType.STRING, definition.displayName());
        pdc.set(reforgeStatsKey, PersistentDataType.STRING, json(added));
        pdc.set(reforgedAtKey, PersistentDataType.LONG, System.currentTimeMillis());
        pdc.set(reforgedByKey, PersistentDataType.STRING, playerId.toString());
        item.setItemMeta(meta);
        return item;
    }

    public void decorate(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(reforgeIdKey, PersistentDataType.STRING);
        Definition definition = get(id);
        if (definition == null) return;
        if (meta.hasDisplayName()) meta.setDisplayName(CC.color("&b" + definition.displayName() + " &r" + meta.getDisplayName()));
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> org.bukkit.ChatColor.stripColor(line).startsWith("Reforge:"));
        int statIndex = lore.size();
        for (int index = 0; index < lore.size(); index++) {
            String clean = org.bukkit.ChatColor.stripColor(lore.get(index));
            if (clean != null && java.util.Arrays.stream(MMOStatType.values()).anyMatch(type -> clean.startsWith(type.display() + ":"))) {
                statIndex = index;
                break;
            }
        }
        lore.add(statIndex, CC.color("&8Reforge: &b" + definition.displayName()));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public String validation() {
        int stones = 0;
        int invalidStoneLinks = 0;
        int missingTiers = 0;
        int missingBrowserEntries = 0;
        for (Definition definition : definitions.values()) {
            ItemStack stone = items.createItem(definition.stoneId(), 1);
            if (stone != null) stones++;
            if (stoneDefinition(stone) != definition) invalidStoneLinks++;
            if (!definition.stats().keySet().containsAll(RARITIES)) missingTiers++;
            if (plugin.getItemBrowserCommand() == null || !plugin.getItemBrowserCommand().containsInCategory("reforge_stones", definition.stoneId())) missingBrowserEntries++;
        }
        return "registry=" + definitions.size() + "/16, stones=" + stones + "/16, invalidStoneLinks=" + invalidStoneLinks + ", missingRarityTiers=" + missingTiers + ", missingBrowserEntries=" + missingBrowserEntries + ", blockedTypes=" + BLOCKED_TYPES.size() + ", deterministic=true, pdc=true";
    }

    private MMOStatMap readStoredStats(ItemStack item) {
        MMOStatMap result = MMOStatMap.empty();
        if (item == null || !item.hasItemMeta()) return result;
        String raw = item.getItemMeta().getPersistentDataContainer().get(reforgeStatsKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return result;
        try {
            JsonObject object = JsonParser.parseString(raw).getAsJsonObject();
            for (String key : object.keySet()) {
                MMOStatType type = MMOStatType.parse(key);
                if (type != null && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber()) result.set(type, object.get(key).getAsDouble());
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private String json(MMOStatMap stats) {
        JsonObject object = new JsonObject();
        stats.values().forEach((type, value) -> object.addProperty(type.name(), value));
        return object.toString();
    }

    private boolean trueFlag(PersistentDataContainer pdc, String name) {
        Byte value = pdc.get(new NamespacedKey(plugin, name), PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    private boolean falseFlag(PersistentDataContainer pdc, String name) {
        Byte value = pdc.get(new NamespacedKey(plugin, name), PersistentDataType.BYTE);
        return value != null && value == 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    public record Definition(String id, String displayName, Set<String> allowedTypes, String stoneId, Map<String, MMOStatMap> stats) {
    }
}
