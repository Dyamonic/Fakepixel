package club.abelix.games.fakevival.mmo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class MMOStatStorage {
    private final NamespacedKey statsKey;
    private final NamespacedKey itemTypeKey;
    private final NamespacedKey versionKey;
    private final NamespacedKey randomRolledKey;
    private final NamespacedKey rollQualityKey;
    private final NamespacedKey rolledStatsKey;

    public MMOStatStorage(JavaPlugin plugin) {
        this.statsKey = new NamespacedKey(plugin, "mmo_stats");
        this.itemTypeKey = new NamespacedKey(plugin, "mmo_item_type");
        this.versionKey = new NamespacedKey(plugin, "mmo_version");
        this.randomRolledKey = new NamespacedKey(plugin, "mmo_random_rolled");
        this.rollQualityKey = new NamespacedKey(plugin, "mmo_roll_quality");
        this.rolledStatsKey = new NamespacedKey(plugin, "rolled_stats");
    }

    public void write(ItemStack item, MMOItemType itemType, MMOStatMap stats) {
        if (item == null || item.getType().isAir() || stats == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        JsonObject object = new JsonObject();
        for (var entry : stats.values().entrySet()) object.addProperty(entry.getKey().name(), entry.getValue());
        meta.getPersistentDataContainer().set(statsKey, PersistentDataType.STRING, object.toString());
        meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, (itemType == null ? MMOItemType.UTILITY : itemType).name());
        meta.getPersistentDataContainer().set(versionKey, PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
    }

    public void markRandomRolled(ItemStack item, double quality) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(randomRolledKey, PersistentDataType.BYTE, (byte) 1);
        String stats = meta.getPersistentDataContainer().get(statsKey, PersistentDataType.STRING);
        if (stats != null) meta.getPersistentDataContainer().set(rolledStatsKey, PersistentDataType.STRING, stats);
        if (Double.isFinite(quality)) {
            meta.getPersistentDataContainer().set(rollQualityKey, PersistentDataType.DOUBLE, Math.max(0.0D, Math.min(100.0D, quality)));
        }
        item.setItemMeta(meta);
    }

    public void clearRandomRoll(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(randomRolledKey);
        meta.getPersistentDataContainer().remove(rollQualityKey);
        meta.getPersistentDataContainer().remove(rolledStatsKey);
        item.setItemMeta(meta);
    }

    public void clearStats(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(statsKey);
        meta.getPersistentDataContainer().remove(versionKey);
        meta.getPersistentDataContainer().remove(randomRolledKey);
        meta.getPersistentDataContainer().remove(rollQualityKey);
        meta.getPersistentDataContainer().remove(rolledStatsKey);
        item.setItemMeta(meta);
    }

    public boolean hasRandomRoll(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(randomRolledKey, PersistentDataType.BYTE);
    }

    public boolean hasStats(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(statsKey, PersistentDataType.STRING);
    }

    public double readRollQuality(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Double.NaN;
        Double quality = item.getItemMeta().getPersistentDataContainer().get(rollQualityKey, PersistentDataType.DOUBLE);
        return quality == null ? Double.NaN : Math.max(0.0D, Math.min(100.0D, quality));
    }

    public MMOStatMap read(ItemStack item) {
        MMOStatMap stats = MMOStatMap.empty();
        if (item == null || !item.hasItemMeta()) return stats;
        String raw = item.getItemMeta().getPersistentDataContainer().get(statsKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return stats;
        try {
            JsonObject object = JsonParser.parseString(raw).getAsJsonObject();
            for (String key : object.keySet()) {
                MMOStatType type = MMOStatType.parse(key);
                if (type != null && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber()) {
                    stats.set(type, object.get(key).getAsDouble());
                }
            }
        } catch (Exception ignored) {
        }
        return stats;
    }

    public MMOItemType readItemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return MMOItemType.UTILITY;
        return MMOItemType.parse(item.getItemMeta().getPersistentDataContainer().get(itemTypeKey, PersistentDataType.STRING));
    }
}
