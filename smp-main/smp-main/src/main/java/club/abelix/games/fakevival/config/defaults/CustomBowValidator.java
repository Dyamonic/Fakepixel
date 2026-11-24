package club.abelix.games.fakevival.config.defaults;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.mmo.MMOStatMap;
import club.abelix.games.fakevival.mmo.MMOStatType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CustomBowValidator {
    private static final Set<String> TYPES = Set.of("BOW", "SHORTBOW", "LONGBOW", "CROSSBOW");
    private static final List<String> BOW_ENCHANTS = List.of("arrow_force", "snipe_custom", "piercing_shot", "quick_draw", "hunter_focus", "eagle_eye", "burning_arrow", "frost_arrow", "storm_arrow", "bounty_shot");
    private static final List<String> BLOCKED_LORE = List.of("generated item", "java registry item", "placeholder item", "debug item", "themed stock from");

    private CustomBowValidator() {
    }

    public static Result validate(Game game) {
        var items = game.getCustomItemManager();
        Map<String, Integer> rarities = new LinkedHashMap<>();
        Map<String, Integer> types = new LinkedHashMap<>();
        int registered = 0;
        int invalidType = 0;
        int invalidMaterial = 0;
        int invalidPdc = 0;
        int missingStats = 0;
        int statsOutOfRange = 0;
        int breakable = 0;
        int invalidLore = 0;
        int placeholderLore = 0;
        int special = 0;
        int invalidRequirement = 0;
        int missingAbility = 0;
        int missingBrowser = 0;
        int enchantWarnings = 0;
        for (CustomBowDefaults100.Definition definition : CustomBowDefaults100.definitions()) {
            String id = definition.id();
            if (!items.itemExists(id)) continue;
            registered++;
            ItemStack item = items.createItem(id, 1);
            if (item == null || item.getType().isAir()) {
                invalidType++;
                continue;
            }
            String type = items.getCustomItemType(item);
            String rarity = items.getCustomItemRarity(item);
            types.merge(type, 1, Integer::sum);
            rarities.merge(rarity, 1, Integer::sum);
            if (!TYPES.contains(type) || !type.equals(definition.type())) invalidType++;
            if (item.getType() != (type.equals("CROSSBOW") ? Material.CROSSBOW : Material.BOW)) invalidMaterial++;
            if (!validPdc(game, item, id, type)) invalidPdc++;
            MMOStatMap stats = items.getMmoStats(item);
            if (stats.get(MMOStatType.DAMAGE) <= 0.0D || stats.get(MMOStatType.STRENGTH) <= 0.0D
                    || stats.get(MMOStatType.CRIT_CHANCE) <= 0.0D || stats.get(MMOStatType.CRIT_DAMAGE) <= 0.0D
                    || stats.get(MMOStatType.PROJECTILE_DAMAGE) <= 0.0D) missingStats++;
            if (!statsInRange(rarity, stats)) statsOutOfRange++;
            if (!item.hasItemMeta() || !item.getItemMeta().isUnbreakable()) breakable++;
            List<String> rawLore = game.settings().getStringList("custom-items.items." + id + ".lore");
            if (rawLore.size() < 2 || rawLore.size() > 3 || rawLore.stream().anyMatch(line -> line == null || line.isBlank())) invalidLore++;
            if (rawLore.stream().map(CustomBowValidator::plain).map(value -> value.toLowerCase(Locale.ROOT))
                    .anyMatch(line -> BLOCKED_LORE.stream().anyMatch(line::contains))) placeholderLore++;
            if ("SPECIAL".equals(rarity) || game.settings().getBoolean("custom-items.items." + id + ".special-item", false)) special++;
            List<String> visibleLore = item.getItemMeta().hasLore() ? item.getItemMeta().getLore() : List.of();
            boolean exact = visibleLore.stream().map(CustomBowValidator::plain).anyMatch(line -> line.contains("Requires PlayerLevel " + definition.playerLevel()));
            boolean wrong = visibleLore.stream().map(CustomBowValidator::plain).anyMatch(line -> line.matches(".*Requires Player \\d+.*"));
            if (!exact || wrong) invalidRequirement++;
            String ability = game.settings().getString("custom-items.items." + id + ".ability.type", "none");
            if (ability != null && !ability.isBlank() && !ability.equalsIgnoreCase("none") && !items.isAbilityRegistered(ability)) missingAbility++;
            if (game.getItemBrowserCommand() == null || !game.getItemBrowserCommand().containsInCategory("bows", id)
                    || !game.getItemBrowserCommand().containsInCategory("weapons", id)) missingBrowser++;
            for (String enchant : BOW_ENCHANTS) if (game.getCustomEnchantManager().get(enchant) == null || !game.getCustomEnchantManager().canApply(item, enchant)) enchantWarnings++;
        }
        int duplicateIds = CustomBowDefaults100.definitions().size() - CustomBowDefaults100.itemIds().size();
        int raritySpreadErrors = spreadErrors(rarities, Map.of("COMMON", 20, "UNCOMMON", 20, "RARE", 20, "EPIC", 20, "LEGENDARY", 15, "MYTHIC", 5));
        int typeSpreadErrors = spreadErrors(types, Map.of("BOW", 60, "SHORTBOW", 25, "LONGBOW", 10, "CROSSBOW", 5));
        return new Result(100, registered, duplicateIds, invalidType, invalidMaterial, invalidPdc, missingStats,
                statsOutOfRange, breakable, invalidLore, placeholderLore, special, invalidRequirement,
                missingAbility, missingBrowser, enchantWarnings, raritySpreadErrors, typeSpreadErrors,
                Map.copyOf(rarities), Map.copyOf(types));
    }

    private static boolean validPdc(Game game, ItemStack item, String id, String type) {
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return id.equals(pdc.get(new NamespacedKey(game, "custom_item_id"), PersistentDataType.STRING))
                && type.equals(pdc.get(new NamespacedKey(game, "custom_item_type"), PersistentDataType.STRING))
                && "WEAPONS".equals(pdc.get(new NamespacedKey(game, "item_category"), PersistentDataType.STRING))
                && "BOWS".equals(pdc.get(new NamespacedKey(game, "item_subcategory"), PersistentDataType.STRING))
                && enabled(game, pdc, "random_stats_allowed") && enabled(game, pdc, "random_rarity_allowed")
                && enabled(game, pdc, "reforge_allowed") && enabled(game, pdc, "enchant_allowed");
    }

    private static boolean enabled(Game game, PersistentDataContainer pdc, String key) {
        return Byte.valueOf((byte) 1).equals(pdc.get(new NamespacedKey(game, key), PersistentDataType.BYTE));
    }

    private static boolean statsInRange(String rarity, MMOStatMap stats) {
        int[] range = switch (rarity) {
            case "COMMON" -> new int[]{8, 30, 0, 12, 0, 3, 0, 12, 0, 10};
            case "UNCOMMON" -> new int[]{25, 65, 8, 28, 2, 5, 10, 30, 8, 25};
            case "RARE" -> new int[]{60, 130, 25, 65, 4, 8, 25, 70, 20, 55};
            case "EPIC" -> new int[]{120, 260, 60, 135, 7, 12, 60, 140, 50, 110};
            case "LEGENDARY" -> new int[]{240, 460, 120, 260, 10, 18, 120, 260, 100, 220};
            case "MYTHIC" -> new int[]{450, 750, 240, 420, 15, 25, 240, 420, 200, 360};
            default -> new int[10];
        };
        return in(stats.get(MMOStatType.DAMAGE), range[0], range[1])
                && in(stats.get(MMOStatType.STRENGTH), range[2], range[3])
                && in(stats.get(MMOStatType.CRIT_CHANCE), range[4], range[5])
                && in(stats.get(MMOStatType.CRIT_DAMAGE), range[6], range[7])
                && in(stats.get(MMOStatType.PROJECTILE_DAMAGE), range[8], range[9]);
    }

    private static boolean in(double value, int minimum, int maximum) {
        return value >= minimum && value <= maximum;
    }

    private static int spreadErrors(Map<String, Integer> actual, Map<String, Integer> expected) {
        return (int) expected.entrySet().stream().filter(entry -> actual.getOrDefault(entry.getKey(), 0).intValue() != entry.getValue()).count();
    }

    private static String plain(String value) {
        String result = ChatColor.stripColor(club.abelix.games.fakevival.util.CC.color(value == null ? "" : value));
        return result == null ? "" : result;
    }

    public record Result(int expected, int registered, int duplicateIds, int invalidType, int invalidMaterial,
                         int invalidPdc, int missingStats, int statsOutOfRange, int breakable, int invalidLore,
                         int placeholderLore, int special, int invalidRequirement, int missingAbility,
                         int missingBrowser, int enchantWarnings, int raritySpreadErrors, int typeSpreadErrors,
                         Map<String, Integer> rarityCounts, Map<String, Integer> typeCounts) {
        public int warnings() {
            return expected - registered + duplicateIds + invalidType + invalidMaterial + invalidPdc + missingStats
                    + statsOutOfRange + breakable + invalidLore + placeholderLore + special + invalidRequirement
                    + missingAbility + missingBrowser + enchantWarnings + raritySpreadErrors + typeSpreadErrors;
        }
    }
}
