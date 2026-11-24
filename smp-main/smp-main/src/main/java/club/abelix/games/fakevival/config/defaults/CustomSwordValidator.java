package club.abelix.games.fakevival.config.defaults;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.mmo.MMOStatMap;
import club.abelix.games.fakevival.mmo.MMOStatType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CustomSwordValidator {
    private static final Set<Material> ALLOWED_MATERIALS = Set.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
    );
    private static final List<String> BLOCKED_LORE = List.of(
            "generated item", "java registry item", "placeholder item", "themed stock from"
    );

    private CustomSwordValidator() {
    }

    public static Result validate(Game game) {
        var manager = game.getCustomItemManager();
        FileConfiguration config = game.settings();
        Map<String, Integer> rarityCounts = new LinkedHashMap<>();
        int registered = 0;
        int invalidType = 0;
        int invalidMaterial = 0;
        int invalidPdc = 0;
        int missingDamage = 0;
        int missingStrength = 0;
        int missingCritChance = 0;
        int missingCritDamage = 0;
        int statsOutOfRange = 0;
        int breakable = 0;
        int invalidLore = 0;
        int placeholderLore = 0;
        int special = 0;
        int invalidRequirement = 0;
        int missingAbility = 0;
        int assignedAbilities = 0;
        int missingBrowser = 0;
        for (CustomSwordDefaults100.Definition definition : CustomSwordDefaults100.definitions()) {
            String id = definition.id();
            String base = "custom-items.items." + id;
            if (!manager.itemExists(id)) continue;
            registered++;
            ItemStack item = manager.createItem(id, 1);
            if (item == null || item.getType().isAir()) {
                invalidType++;
                continue;
            }
            String rarity = manager.getCustomItemRarity(item).toUpperCase(Locale.ROOT);
            rarityCounts.merge(rarity, 1, Integer::sum);
            if (!"SWORD".equals(manager.getCustomItemType(item)) || !"SWORD".equalsIgnoreCase(config.getString(base + ".type", "")))
                invalidType++;
            if (!ALLOWED_MATERIALS.contains(item.getType())) invalidMaterial++;
            if (!validPdc(game, item, id)) invalidPdc++;
            MMOStatMap stats = manager.getMmoStats(item);
            if (stats.get(MMOStatType.DAMAGE) <= 0) missingDamage++;
            if (stats.get(MMOStatType.STRENGTH) <= 0) missingStrength++;
            if (stats.get(MMOStatType.CRIT_CHANCE) <= 0) missingCritChance++;
            if (stats.get(MMOStatType.CRIT_DAMAGE) <= 0) missingCritDamage++;
            if (!statsInRange(rarity, stats)) statsOutOfRange++;
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.isUnbreakable()) breakable++;
            List<String> rawLore = config.getStringList(base + ".lore");
            if (rawLore.size() < 2 || rawLore.size() > 3 || rawLore.stream().anyMatch(line -> line == null || line.isBlank()))
                invalidLore++;
            if (containsBlocked(rawLore)) placeholderLore++;
            if ("SPECIAL".equals(rarity) || config.getBoolean(base + ".special-item", false)) special++;
            List<String> visibleLore = meta != null && meta.hasLore() ? meta.getLore() : List.of();
            String required = "Requires PlayerLevel " + definition.playerLevel();
            boolean exactRequirement = visibleLore.stream().map(CustomSwordValidator::plain).anyMatch(line -> line.contains(required));
            boolean wrongRequirement = visibleLore.stream().map(CustomSwordValidator::plain)
                    .anyMatch(line -> line.matches(".*Requires Player \\d+.*"));
            if (!exactRequirement || wrongRequirement) invalidRequirement++;
            String ability = config.getString(base + ".ability.type", "none");
            if (ability != null && !ability.isBlank() && !ability.equalsIgnoreCase("none")) {
                assignedAbilities++;
                if (!manager.isAbilityRegistered(ability)) missingAbility++;
            }
            if (game.getItemBrowserCommand() == null || !game.getItemBrowserCommand().containsInCategory("swords", id)
                    || !game.getItemBrowserCommand().containsInCategory("weapons", id)) missingBrowser++;
        }
        int duplicateIds = CustomSwordDefaults100.definitions().size() - CustomSwordDefaults100.itemIds().size();
        int raritySpreadErrors = raritySpreadErrors(rarityCounts);
        int missingRecipes = game.getRecipeManager() == null ? CustomSwordDefaults100.recipeIds().size()
                : (int) CustomSwordDefaults100.recipeIds().stream()
                .filter(id -> game.getRecipeManager().getRecipe(id).isEmpty()).count();
        int skippedRecipes = game.getRecipeManager() == null ? 0 : game.getRecipeManager().skippedInvalidRecipeCount();
        return new Result(100, registered, duplicateIds, invalidType, invalidMaterial, invalidPdc, missingDamage,
                missingStrength, missingCritChance, missingCritDamage, statsOutOfRange, breakable, invalidLore,
                placeholderLore, special, invalidRequirement, assignedAbilities, missingAbility, missingBrowser,
                raritySpreadErrors, CustomSwordDefaults100.recipeIds().size(), missingRecipes, skippedRecipes,
                Map.copyOf(rarityCounts));
    }

    private static boolean validPdc(Game game, ItemStack item, String id) {
        if (!item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return id.equals(pdc.get(new NamespacedKey(game, "custom_item_id"), PersistentDataType.STRING))
                && "SWORD".equals(pdc.get(new NamespacedKey(game, "custom_item_type"), PersistentDataType.STRING))
                && "WEAPONS".equals(pdc.get(new NamespacedKey(game, "item_category"), PersistentDataType.STRING))
                && "SWORDS".equals(pdc.get(new NamespacedKey(game, "item_subcategory"), PersistentDataType.STRING))
                && enabled(pdc, game, "random_stats_allowed")
                && enabled(pdc, game, "random_rarity_allowed")
                && enabled(pdc, game, "reforge_allowed")
                && enabled(pdc, game, "enchant_allowed");
    }

    private static boolean enabled(PersistentDataContainer pdc, Game game, String key) {
        return Byte.valueOf((byte) 1).equals(pdc.get(new NamespacedKey(game, key), PersistentDataType.BYTE));
    }

    private static boolean statsInRange(String rarity, MMOStatMap stats) {
        int[] range = switch (rarity) {
            case "COMMON" -> new int[]{10, 35, 2, 15, 0, 3, 0, 10};
            case "UNCOMMON" -> new int[]{30, 70, 10, 30, 2, 5, 5, 25};
            case "RARE" -> new int[]{65, 130, 25, 65, 4, 8, 20, 60};
            case "EPIC" -> new int[]{120, 240, 60, 130, 7, 12, 50, 120};
            case "LEGENDARY" -> new int[]{220, 420, 120, 240, 10, 18, 100, 220};
            case "MYTHIC" -> new int[]{400, 650, 220, 380, 15, 25, 200, 350};
            default -> new int[8];
        };
        return in(stats.get(MMOStatType.DAMAGE), range[0], range[1])
                && in(stats.get(MMOStatType.STRENGTH), range[2], range[3])
                && in(stats.get(MMOStatType.CRIT_CHANCE), range[4], range[5])
                && in(stats.get(MMOStatType.CRIT_DAMAGE), range[6], range[7]);
    }

    private static boolean in(double value, int minimum, int maximum) {
        return value >= minimum && value <= maximum;
    }

    private static boolean containsBlocked(List<String> lore) {
        for (String line : lore) {
            String plain = plain(line).toLowerCase(Locale.ROOT);
            if (BLOCKED_LORE.stream().anyMatch(plain::contains)) return true;
        }
        return false;
    }

    private static String plain(String line) {
        String plain = ChatColor.stripColor(club.abelix.games.fakevival.util.CC.color(line == null ? "" : line));
        return plain == null ? "" : plain;
    }

    private static int raritySpreadErrors(Map<String, Integer> counts) {
        Map<String, Integer> expected = Map.of(
                "COMMON", 20, "UNCOMMON", 20, "RARE", 20,
                "EPIC", 20, "LEGENDARY", 15, "MYTHIC", 5
        );
        return (int) expected.entrySet().stream()
                .filter(entry -> counts.getOrDefault(entry.getKey(), 0).intValue() != entry.getValue()).count();
    }

    public record Result(int expected, int registered, int duplicateIds, int invalidType, int invalidMaterial,
                         int invalidPdc, int missingDamage, int missingStrength, int missingCritChance,
                         int missingCritDamage, int statsOutOfRange, int breakable, int invalidLore,
                         int placeholderLore, int special, int invalidRequirement, int assignedAbilities,
                         int missingAbility, int missingBrowser, int raritySpreadErrors, int recipesExpected,
                         int missingRecipes, int skippedRecipes, Map<String, Integer> rarityCounts) {
        public int warnings() {
            return expected - registered + duplicateIds + invalidType + invalidMaterial + invalidPdc + missingDamage
                    + missingStrength + missingCritChance + missingCritDamage + statsOutOfRange + breakable
                    + invalidLore + placeholderLore + special + invalidRequirement + missingAbility + missingBrowser
                    + raritySpreadErrors + missingRecipes;
        }
    }
}
