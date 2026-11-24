package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.mmo.MMOItemType;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class ItemRarityManager {
    private final JavaPlugin plugin;
    private final NamespacedKey rarityKey;
    private final NamespacedKey customItemKey;
    private final NamespacedKey customItemTypeKey;
    private final NamespacedKey customItemRarityKey;
    private final NamespacedKey rolledRarityKey;
    private final NamespacedKey fishRarityKey;

    public ItemRarityManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.rarityKey = new NamespacedKey(plugin, "item_rarity");
        this.customItemKey = new NamespacedKey(plugin, "custom_item_id");
        this.customItemTypeKey = new NamespacedKey(plugin, "custom_item_type");
        this.customItemRarityKey = new NamespacedKey(plugin, "custom_item_rarity");
        this.rolledRarityKey = new NamespacedKey(plugin, "rolled_rarity");
        this.fishRarityKey = new NamespacedKey(plugin, "fish_rarity");
    }

    public boolean isEnabled() {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("item-rarity.enabled", true);
    }

    public void applyIfMissing(ItemStack item) {
        if (isDeveloperNotebook(item)) {
            ensureDeveloperNotebookSpecialLine(item);
            return;
        }
        if (isCustomItem(item) && shouldUseFixedRarity(item)) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) applyFixedCustomRarity(item, meta);
            return;
        }
        if (!isEligible(item)) {
            cleanInvalidRarity(item);
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String existing = meta.getPersistentDataContainer().get(rarityKey, PersistentDataType.STRING);
        if (existing != null && !existing.isBlank()) return;

        Rarity rarity = rollRarity();
        meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, rarity.id());
        meta.getPersistentDataContainer().set(rolledRarityKey, PersistentDataType.STRING, rarity.id().toUpperCase(Locale.ROOT));

        List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.getLore())) : new ArrayList<>();
        trimGeneratedRarityLore(lore);
        if (!lore.isEmpty() && !lore.getLast().isBlank()) lore.add("");
        String suffix = isCustomItem(item) ? "" : " " + itemKind(item.getType());
        lore.add(CC.color(rarity.color() + "&l" + rarity.display() + suffix));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public void applyInventory(ItemStack[] contents) {
        if (contents == null) return;
        for (ItemStack item : contents) applyIfMissing(item);
    }

    public String getRarityId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        String rarity = item.getItemMeta().getPersistentDataContainer().get(rarityKey, PersistentDataType.STRING);
        return rarity == null ? "" : rarity.toLowerCase(Locale.ROOT);
    }

    public boolean isRareOrBetter(ItemStack item) {
        return switch (getRarityId(item)) {
            case "rare", "epic", "legendary", "mythic", "special", "divine" -> true;
            default -> false;
        };
    }

    private boolean isEligible(ItemStack item) {
        if (!isEnabled() || item == null || item.getType().isAir() || item.getAmount() <= 0) return false;
        if(item.hasItemMeta()){
            String id=item.getItemMeta().getPersistentDataContainer().get(customItemKey,PersistentDataType.STRING);
            String raw=item.getItemMeta().getPersistentDataContainer().get(customItemTypeKey,PersistentDataType.STRING);
            if("developer_notebook".equalsIgnoreCase(id)||raw!=null&&java.util.Set.of("MATERIAL","MINING_MATERIAL","FORAGING_MATERIAL","FARMING_MATERIAL","DUNGEON_MATERIAL","COMBAT_MATERIAL","MONSTER_MATERIAL","FISHING_MATERIAL","MAGIC_MATERIAL","RECIPE_MATERIAL","COOKING_INGREDIENT","CUSTOM_FOOD","FOOD","CUSTOM_FISH","FISH","TOKEN","EVENT_TOKEN","DUNGEON_KEY","QUEST_ITEM","FURNITURE","COSMETIC","SPECIAL","DEVELOPER_NOTEBOOK","FARMING_EQUIPMENT").contains(raw.toUpperCase(Locale.ROOT)))return false;
            for(NamespacedKey key:item.getItemMeta().getPersistentDataContainer().getKeys())if(java.util.Set.of("random_stats_blocked","random_rarity_blocked","special_item","developer_notebook","cooking_ingredient").contains(key.getKey())){Byte value=item.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.BYTE);if(value==null||value!=0)return false;}
        }
        for (String configured : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("item-rarity.excluded-materials")) {
            if (configured.equalsIgnoreCase(item.getType().name())) return false;
        }
        if (isCustomItem(item)) return true;

        String mode = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("item-rarity.apply-to", "CUSTOM_ITEMS").toUpperCase(Locale.ROOT);
        if (mode.equals("CUSTOM_ITEMS")) return false;
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("item-rarity.include-stackable-items", false) && item.getType().getMaxStackSize() > 1) return false;
        if (mode.equals("EQUIPMENT_AND_CUSTOM")) return isEquipment(item.getType());
        return mode.equals("ALL_ELIGIBLE");
    }

    private boolean isCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String id = item.getItemMeta().getPersistentDataContainer().get(customItemKey, PersistentDataType.STRING);
        return id != null && !id.isBlank();
    }

    private MMOItemType customItemType(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        String type = meta.getPersistentDataContainer().get(customItemTypeKey, PersistentDataType.STRING);
        String id = meta.getPersistentDataContainer().get(customItemKey, PersistentDataType.STRING);
        if (type == null || type.isBlank()) {
            String path = "custom-items.items." + (id == null ? "" : id.toLowerCase(Locale.ROOT)) + ".";
            type = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + "type",
                    club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + "mmo.type", "MISC"));
        }
        return MMOItemType.parse(type);
    }

    private boolean shouldUseFixedRarity(ItemStack item) {
        String type = rawCustomItemType(item);
        if (Set.of("MATERIAL", "RECIPE_MATERIAL", "MINING_MATERIAL", "FORAGING_MATERIAL", "FARMING_MATERIAL",
                "COMBAT_MATERIAL", "MONSTER_MATERIAL", "FISHING_MATERIAL", "MAGIC_MATERIAL", "DUNGEON_MATERIAL",
                "COOKING_INGREDIENT", "CUSTOM_FOOD", "FOOD", "CUSTOM_FISH", "FISH", "FISHING_ITEM",
                "FISHING_BAIT", "BAIT", "FISHING_GEAR_MATERIAL", "EVENT_TOKEN", "TOKEN", "EVENT_MATERIAL",
                "EVENT_ITEM", "NORMAL_EVENT_ITEM", "EVENT_COLLECTIBLE", "VANILLA_MATERIAL").contains(type)) return true;
        return !customItemType(item).canRollRandomRarityEffects();
    }

    private void applyFixedCustomRarity(ItemStack item, ItemMeta meta) {
        String type = rawCustomItemType(item);
        String fixedId = Set.of("CUSTOM_FISH", "FISH").contains(type)
                ? meta.getPersistentDataContainer().get(fishRarityKey, PersistentDataType.STRING) : null;
        if (fixedId == null || fixedId.isBlank()) fixedId = meta.getPersistentDataContainer().get(customItemRarityKey, PersistentDataType.STRING);
        if ("SPECIAL".equalsIgnoreCase(fixedId) && !isTrueSpecial(item, type)) fixedId = null;
        if (fixedId == null || fixedId.isBlank()) fixedId = defaultRarityForType(type);
        String normalized = fixedId.toLowerCase(Locale.ROOT);
        meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, normalized.toUpperCase(Locale.ROOT));
        meta.getPersistentDataContainer().set(rolledRarityKey, PersistentDataType.STRING, normalized.toUpperCase(Locale.ROOT));
        List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.getLore())) : new ArrayList<>();
        trimGeneratedRarityLore(lore);
        Rarity fixed = getRarities().stream().filter(value -> value.id().equalsIgnoreCase(normalized)).findFirst()
                .orElse(new Rarity(normalized, normalized.toUpperCase(Locale.ROOT), "&f", 0));
        if (!lore.isEmpty() && !lore.getLast().isBlank()) lore.add("");
        lore.add(CC.color(fixed.color() + "&l" + fixed.display() + fixedTypeSuffix(type)));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private boolean isEquipment(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || name.equals("BOW")
                || name.equals("CROSSBOW")
                || name.equals("TRIDENT")
                || name.equals("MACE")
                || name.equals("SHIELD")
                || name.equals("FISHING_ROD")
                || name.equals("SHEARS");
    }

    private void cleanInvalidRarity(ItemStack item) {
        if (isDeveloperNotebook(item)) {
            ensureDeveloperNotebookSpecialLine(item);
            return;
        }
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("item-rarity.cleanup-invalid-rarity-lore", true)) return;
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        boolean hadRarity = meta.getPersistentDataContainer().has(rarityKey, PersistentDataType.STRING);
        List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.getLore())) : new ArrayList<>();
        int originalSize = lore.size();
        trimGeneratedRarityLore(lore);
        if (!hadRarity && originalSize == lore.size()) return;
        meta.getPersistentDataContainer().remove(rarityKey);
        meta.getPersistentDataContainer().remove(rolledRarityKey);
        meta.setLore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
    }

    private boolean isDeveloperNotebook(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(customItemKey, PersistentDataType.STRING);
        String type = pdc.get(customItemTypeKey, PersistentDataType.STRING);
        if ("developer_notebook".equalsIgnoreCase(id) || "SPECIAL".equalsIgnoreCase(type)) return true;
        for (String flag : List.of("developer_notebook", "special_item")) {
            if (Byte.valueOf((byte) 1).equals(pdc.get(new NamespacedKey(plugin, flag), PersistentDataType.BYTE))) return true;
        }
        return false;
    }

    private String rawCustomItemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        ItemMeta meta = item.getItemMeta();
        String type = meta.getPersistentDataContainer().get(customItemTypeKey, PersistentDataType.STRING);
        String id = meta.getPersistentDataContainer().get(customItemKey, PersistentDataType.STRING);
        if (type == null || type.isBlank()) {
            String path = "custom-items.items." + (id == null ? "" : id.toLowerCase(Locale.ROOT)) + ".";
            type = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + "type",
                    club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + "mmo.type", "MISC"));
        }
        return type == null ? "" : type.toUpperCase(Locale.ROOT);
    }

    private String defaultRarityForType(String type) {
        if (type == null || type.isBlank()) return "COMMON";
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "SPECIAL", "DEVELOPER_NOTEBOOK", "LIMITED_SPECIAL", "ADMIN_SPECIAL" -> "SPECIAL";
            case "COMBAT_MATERIAL", "MONSTER_MATERIAL", "FISHING_MATERIAL", "MAGIC_MATERIAL", "DUNGEON_MATERIAL",
                    "EVENT_MATERIAL", "EVENT_TOKEN", "TOKEN", "FISHING_ITEM", "FISHING_GEAR_MATERIAL" -> "UNCOMMON";
            case "EVENT_ITEM", "NORMAL_EVENT_ITEM", "EVENT_COLLECTIBLE" -> "RARE";
            case "WEAPON", "SWORD", "DAGGER", "KATANA", "BOW", "SHORTBOW", "LONGBOW", "CROSSBOW", "ARMOR", "HELMET", "CHESTPLATE",
                    "LEGGINGS", "BOOTS", "TOOL", "PICKAXE", "AXE", "SHOVEL", "HOE", "FISHING_ROD", "WAND",
                    "STAFF", "RING", "NECKLACE", "CHARM", "TOTEM", "ACCESSORY" -> "UNCOMMON";
            default -> "COMMON";
        };
    }

    private boolean isTrueSpecial(ItemStack item, String type) {
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(customItemKey, PersistentDataType.STRING);
        if ("developer_notebook".equalsIgnoreCase(id) || "dctr_space_helmet".equalsIgnoreCase(id)) return true;
        if (Set.of("SPECIAL", "DEVELOPER_NOTEBOOK", "LIMITED_SPECIAL", "ADMIN_SPECIAL").contains(type)) return true;
        return Byte.valueOf((byte) 1).equals(pdc.get(new NamespacedKey(plugin, "special_item"), PersistentDataType.BYTE));
    }

    private String fixedTypeSuffix(String type) {
        if (type == null) return "";
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "MATERIAL", "RECIPE_MATERIAL", "MINING_MATERIAL", "FORAGING_MATERIAL", "FARMING_MATERIAL",
                    "COMBAT_MATERIAL", "MONSTER_MATERIAL", "FISHING_MATERIAL", "MAGIC_MATERIAL", "DUNGEON_MATERIAL",
                    "COOKING_INGREDIENT" -> " MATERIAL";
            case "CUSTOM_FISH", "FISH" -> " FISH";
            case "CUSTOM_FOOD", "FOOD" -> " FOOD";
            case "EVENT_ITEM", "NORMAL_EVENT_ITEM", "EVENT_COLLECTIBLE" -> " EVENT ITEM";
            default -> "";
        };
    }

    private void ensureDeveloperNotebookSpecialLine(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> {
            String plain = ChatColor.stripColor(line);
            return plain != null && plain.equalsIgnoreCase("SPECIAL");
        });
        while (!lore.isEmpty() && lore.getLast().isBlank()) lore.removeLast();
        lore.add("");
        lore.add(CC.color("&c&lSPECIAL"));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private Rarity rollRarity() {
        List<Rarity> rarities = getRarities();
        int totalWeight = rarities.stream().mapToInt(Rarity::weight).sum();
        if (totalWeight <= 0) return rarities.getFirst();
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (Rarity rarity : rarities) {
            roll -= rarity.weight();
            if (roll < 0) return rarity;
        }
        return rarities.getLast();
    }

    private List<Rarity> getRarities() {
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("item-rarity.rarities");
        if (section == null) return defaults();

        List<Rarity> rarities = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            int weight = Math.max(0, section.getInt(id + ".weight", 0));
            if (weight == 0) continue;
            String display = section.getString(id + ".display", id.toUpperCase(Locale.ROOT));
            String color = section.getString(id + ".color", "&f");
            rarities.add(new Rarity(id.toLowerCase(Locale.ROOT), display, color, weight));
        }
        return rarities.isEmpty() ? defaults() : rarities;
    }

    private List<Rarity> defaults() {
        return List.of(
                new Rarity("common", "COMMON", "&f", 600),
                new Rarity("uncommon", "UNCOMMON", "&a", 250),
                new Rarity("rare", "RARE", "&9", 100),
                new Rarity("epic", "EPIC", "&5", 40),
                new Rarity("legendary", "LEGENDARY", "&6", 9),
                new Rarity("mythic", "MYTHIC", "&d", 1)
        );
    }

    private void trimGeneratedRarityLore(List<String> lore) {
        while (!lore.isEmpty() && lore.getLast().isBlank()) lore.removeLast();
        if (lore.isEmpty()) return;
        String clean = ChatColor.stripColor(CC.color(lore.getLast()));
        if (clean.matches("(?i)(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|SPECIAL|DIVINE)( (ITEM|BLOCK|TOOL|WEAPON|ARMOR|MATERIAL|FISH|FOOD|EVENT ITEM))?")) {
            lore.removeLast();
        }
        while (!lore.isEmpty() && lore.getLast().isBlank()) lore.removeLast();
    }

    private String itemKind(Material material) {
        if (material.isBlock()) return "BLOCK";
        if (material.name().endsWith("_SWORD") || material.name().endsWith("_AXE") || material.name().endsWith("_BOW") || material.name().equals("TRIDENT")) return "WEAPON";
        if (material.name().endsWith("_PICKAXE") || material.name().endsWith("_SHOVEL") || material.name().endsWith("_HOE") || material.name().endsWith("_SHEARS") || material.name().endsWith("_ROD")) return "TOOL";
        if (material.name().endsWith("_HELMET") || material.name().endsWith("_CHESTPLATE") || material.name().endsWith("_LEGGINGS") || material.name().endsWith("_BOOTS")) return "ARMOR";
        return "ITEM";
    }

    private record Rarity(String id, String display, String color, int weight) {
    }
}
