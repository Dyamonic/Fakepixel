package club.abelix.games.fakevival.mmo;

import club.abelix.games.fakevival.util.CC;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MMOLoreBuilder {
    public String displayName(String originalName, String rarity) {
        return CC.color(rarityColor(rarity) + cleanName(originalName));
    }

    public List<String> rebuild(List<String> existing, String originalName, String rarity, MMOStatMap stats,
                                Map<String, String> enchantLines, String abilityName, Integer manaCost) {
        return rebuild(existing, originalName, rarity, stats, enchantLines, abilityName, manaCost, List.of());
    }

    public List<String> rebuild(List<String> existing, String originalName, String rarity, MMOStatMap stats,
                                Map<String, String> enchantLines, String abilityName, Integer manaCost,
                                List<String> requirementLines) {
        return rebuild(existing, originalName, rarity, stats, enchantLines, abilityName, manaCost, requirementLines, List.of(), null);
    }

    public List<String> rebuild(List<String> existing, String originalName, String rarity, MMOStatMap stats,
                                Map<String, String> enchantLines, String abilityName, Integer manaCost,
                                List<String> requirementLines, List<String> abilityDescription, Integer cooldownSeconds) {
        return rebuild(existing, originalName, rarity, stats, enchantLines, abilityName, manaCost, requirementLines,
                abilityDescription, cooldownSeconds, null);
    }

    public List<String> rebuild(List<String> existing, String originalName, String rarity, MMOStatMap stats,
                                Map<String, String> enchantLines, String abilityName, Integer manaCost,
                                List<String> requirementLines, List<String> abilityDescription, Integer cooldownSeconds,
                                String abilityTrigger) {
        List<String> lore = new ArrayList<>();
        String cleanName = cleanName(originalName);
        String rarityColor = rarityColor(rarity);
        String rarityDisplay = rarityDisplay(rarity);
        List<String> description = descriptionLines(existing, cleanName, enchantLines == null ? Set.of() : enchantLines.keySet());
        if (abilityDescription != null && !abilityDescription.isEmpty()) {
            Set<String> managedAbilityDescription = abilityDescription.stream()
                    .map(line -> ChatColor.stripColor(CC.color(line)))
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            description.removeIf(managedAbilityDescription::contains);
        }

        for (String line : description) lore.add(CC.color("&7" + ChatColor.stripColor(CC.color(line))));

        if (stats != null && !stats.isEmpty()) {
            lore.add("");
            for (MMOStatType type : MMOStatType.values()) {
                double value = stats.get(type);
                if (value <= 0.0D) continue;
                lore.add(CC.color("&7" + type.display() + ": " + type.color() + "+" + format(value) + (type.percent() ? "%" : "")));
            }
        }


        if (enchantLines != null && !enchantLines.isEmpty()) {
            lore.add("");
            for (String line : wrappedEnchantLines(enchantLines)) lore.add(CC.color(line));
        }

        if (abilityName != null && !abilityName.isBlank()) {
            lore.add("");
            lore.add(CC.color("&6Ability: " + abilityName + " &e" + (abilityTrigger == null || abilityTrigger.isBlank() ? "RIGHT CLICK" : abilityTrigger)));
            if (abilityDescription != null) for (String line : abilityDescription) lore.add(CC.color(line));
            if (manaCost != null) lore.add(CC.color("&8Mana Cost: &b" + Math.max(0, manaCost)));
            if (cooldownSeconds != null && cooldownSeconds > 0) lore.add(CC.color("&8Cooldown: &a" + cooldownSeconds + "s"));
        }

        if (requirementLines != null && !requirementLines.isEmpty()) {
            lore.add("");
            for (String line : requirementLines) lore.add(CC.color(line));
        }

        lore.add("");
        lore.add(CC.color(rarityColor + "&l" + rarityDisplay));
        return lore;
    }

    public List<String> rebuild(List<String> existing, MMOStatMap stats) {
        return rebuild(existing, "", "COMMON", stats, Map.of(), null, null);
    }

    public List<String> rebuild(List<String> existing, MMOStatMap stats, Map<String, String> enchantLines) {
        return rebuild(existing, "", "COMMON", stats, enchantLines, null, null);
    }

    private List<String> descriptionLines(List<String> existing, String cleanName, Set<String> enchantNames) {
        List<String> description = new ArrayList<>();
        for (String line : existing == null ? List.<String>of() : existing) {
            String clean = ChatColor.stripColor(CC.color(line));
            if (clean == null || clean.isBlank()) continue;
            if (clean.equals(cleanName)) continue;
            if (isStatLine(clean) || isRollQualityLine(clean) || isAbilityLine(clean) || isRequirementLine(clean) || isRarityLine(clean) || isManagedEnchantLine(clean, enchantNames)) continue;
            description.add(clean);
        }
        return description;
    }

    private List<String> wrappedEnchantLines(Map<String, String> enchantLines) {
        List<String> lines = new ArrayList<>();
        List<String> values = new ArrayList<>(enchantLines.values());
        for (int i = 0; i < values.size(); i += 3) {
            List<String> chunk = values.subList(i, Math.min(i + 3, values.size()));
            List<String> colored = new ArrayList<>();
            for (String value : chunk) colored.add("&9" + value);
            lines.add(String.join("&7, ", colored));
        }
        return lines;
    }

    private boolean isStatLine(String clean) {
        String normalized = clean.toLowerCase(Locale.ROOT);
        for (MMOStatType type : MMOStatType.values()) {
            if (normalized.startsWith(type.display().toLowerCase(Locale.ROOT) + ":")) return true;
        }
        return "mmo stats".equalsIgnoreCase(clean);
    }

    private boolean isRollQualityLine(String clean) {
        String normalized = clean.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("stat roll:")
                || normalized.startsWith("roll quality:")
                || normalized.startsWith("quality roll:")
                || normalized.startsWith("stat quality:");
    }

    private boolean isManagedEnchantLine(String clean, Set<String> enchantNames) {
        if (enchantNames == null || enchantNames.isEmpty()) return false;
        for (String name : enchantNames) {
            if (name != null && !name.isBlank() && (clean.startsWith(name + " ") || clean.contains(", " + name + " "))) return true;
        }
        return false;
    }

    private boolean isAbilityLine(String clean) {
        return clean.startsWith("Ability: ")
                || clean.contains("Ability:")
                || clean.startsWith("Mana Cost: ")
                || clean.startsWith("Trigger: ")
                || clean.startsWith("Cooldown: ")
                || clean.equalsIgnoreCase("Right Click to use.")
                || clean.equalsIgnoreCase("Custom enchanted item.");
    }

    private boolean isRarityLine(String clean) {
        return clean.toUpperCase(Locale.ROOT).matches("(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|SPECIAL|DIVINE)( (ITEM|BLOCK|TOOL|WEAPON|ARMOR|SWORD|MATERIAL|FISH|FOOD|EVENT ITEM))?");
    }

    private boolean isRequirementLine(String clean) {
        if (clean.equals("Requires:") || clean.startsWith("Requires: ") || clean.matches("- .+ Level \\d+")) return true;
        int requires = clean.indexOf("Requires ");
        if (requires < 0 || requires > 10) return false;
        return clean.substring(requires).matches("(?i)Requires (Combat|Mining|Farming|Fishing|Foraging|Woodcutting|Enchanting|Player)( Level)? \\d+.*");
    }

    private String cleanName(String originalName) {
        String clean = ChatColor.stripColor(CC.color(originalName == null ? "" : originalName));
        return clean == null || clean.isBlank() ? "Custom Item" : clean;
    }

    private String rarityDisplay(String rarity) {
        String normalized = normalizeRarity(rarity);
        return normalized.equals("DIVINE") ? "SPECIAL" : normalized;
    }

    public String rarityColor(String rarity) {
        return switch (normalizeRarity(rarity)) {
            case "UNCOMMON" -> "&a";
            case "RARE" -> "&9";
            case "EPIC" -> "&5";
            case "LEGENDARY" -> "&6";
            case "MYTHIC" -> "&d";
            case "SPECIAL", "DIVINE" -> "&c";
            default -> "&f";
        };
    }

    private String normalizeRarity(String rarity) {
        if (rarity == null || rarity.isBlank()) return "COMMON";
        return rarity.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) return String.valueOf((int) Math.rint(value));
        return String.format(Locale.US, "%.1f", value);
    }
}
