package club.abelix.games.fakevival.mmo;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class MMOEnchantStorage {
    private final NamespacedKey enchantsKey;
    private final NamespacedKey legacyEnchantsKey;

    public MMOEnchantStorage(JavaPlugin plugin) {
        this.enchantsKey = new NamespacedKey(plugin, "mmo_enchants");
        this.legacyEnchantsKey = new NamespacedKey(plugin, "custom_enchants");
    }

    public Map<String, Integer> read(ItemStack item) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (item == null || !item.hasItemMeta()) return result;
        ItemMeta meta = item.getItemMeta();
        String raw = meta.getPersistentDataContainer().get(enchantsKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) raw = meta.getPersistentDataContainer().get(legacyEnchantsKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return result;
        for (String entry : raw.split(";")) {
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) continue;
            try {
                int level = Integer.parseInt(parts[1]);
                if (level > 0) result.put(normalize(parts[0]), level);
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public void write(ItemStack item, Map<String, Integer> values) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String raw = encode(values);
        if (raw.isBlank()) {
            meta.getPersistentDataContainer().remove(enchantsKey);
            meta.getPersistentDataContainer().remove(legacyEnchantsKey);
        } else {
            meta.getPersistentDataContainer().set(enchantsKey, PersistentDataType.STRING, raw);
            meta.getPersistentDataContainer().set(legacyEnchantsKey, PersistentDataType.STRING, raw);
        }
        item.setItemMeta(meta);
    }

    public Map<String, Integer> readVanillaEnchants(ItemStack item) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (item == null || !item.hasItemMeta()) return result;
        for (Map.Entry<Enchantment, Integer> entry : item.getItemMeta().getEnchants().entrySet()) {
            String id = fromVanilla(entry.getKey());
            if (id != null && entry.getValue() != null && entry.getValue() > 0) {
                result.merge(id, entry.getValue(), Math::max);
            }
        }
        return result;
    }

    public boolean removeVanillaEnchants(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta.getEnchants().isEmpty()) return false;
        for (Enchantment enchantment : meta.getEnchants().keySet().toArray(Enchantment[]::new)) {
            meta.removeEnchant(enchantment);
        }
        item.setItemMeta(meta);
        return true;
    }

    public static String fromVanilla(Enchantment enchantment) {
        if (enchantment == null) return null;
        String key = enchantment.getKey().getKey().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "sharpness" -> "sharpness";
            case "smite" -> "smite";
            case "protection" -> "protection";
            case "efficiency" -> "efficiency";
            case "fortune" -> "fortune";
            case "power" -> "power";
            case "looting" -> "looting";
            case "unbreaking" -> "unbreaking";
            case "mending" -> "mending";
            case "silk_touch" -> "silk_touch";
            case "lure" -> "lure";
            case "luck_of_the_sea" -> "luck_of_the_sea";
            case "feather_falling" -> "feather_falling";
            case "respiration" -> "respiration";
            case "aqua_affinity" -> "aqua_affinity";
            case "sweeping_edge" -> "sweeping_edge";
            case "fire_aspect" -> "fire_aspect";
            case "knockback" -> "knockback";
            case "punch" -> "punch";
            case "flame" -> "flame";
            case "infinity" -> "infinity";
            default -> null;
        };
    }

    private String encode(Map<String, Integer> values) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder raw = new StringBuilder();
        values.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null && entry.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (!raw.isEmpty()) raw.append(';');
                    raw.append(normalize(entry.getKey())).append(':').append(entry.getValue());
                });
        return raw.toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(' ', '_').replace("-", "_");
    }
}
