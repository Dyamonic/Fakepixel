package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class LootBoxManager {
    private final JavaPlugin plugin;
    private final CustomItemManager customItemManager;
    private final DataManager dataManager;
    private final NamespacedKey boxKey;
    private final Random random = new Random();

    public LootBoxManager(JavaPlugin plugin, CustomItemManager customItemManager) {
        this(plugin, customItemManager, null);
    }

    public LootBoxManager(JavaPlugin plugin, CustomItemManager customItemManager, DataManager dataManager) {
        this.plugin = plugin;
        this.customItemManager = customItemManager;
        this.dataManager = dataManager;
        this.boxKey = new NamespacedKey(plugin, "loot_box_id");
    }

    public ItemStack createBox(String id, int amount) {
        String key = normalize(id);
        Material material = Material.matchMaterial(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(key) + ".material", "CHEST"));
        String customItemId = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(key) + ".custom-item", "");
        ItemStack item = !customItemId.isBlank() && customItemManager.itemExists(customItemId)
                ? customItemManager.createItem(customItemId, Math.max(1, amount))
                : new ItemStack(material == null ? Material.CHEST : material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(key) + ".name", "&6Loot Box")));
            List<String> lore = new ArrayList<>(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(path(key) + ".lore"));
            lore.add("");
            lore.add("&eRight-click to open.");
            lore.add("&8Loot Box: " + key);
            meta.setLore(CC.colorList(lore));
            meta.getPersistentDataContainer().set(boxKey, PersistentDataType.STRING, key);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean give(Player player, String id, int amount) {
        if (!exists(id)) return false;
        player.getInventory().addItem(createBox(id, amount));
        return true;
    }

    public boolean open(Player player, ItemStack item) {
        String id = getBoxId(item);
        if (!exists(id)) return false;
        ConfigurationSection rewards = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection(path(id) + ".rewards");
        if (rewards == null || rewards.getKeys(false).isEmpty()) {
            player.sendMessage(CC.color("&cThis loot box has no rewards configured."));
            return true;
        }
        String rewardKey = pickReward(player, id, rewards);
        if (rewardKey == null) return true;
        rememberReward(player, id, rewardKey);
        String rewardPath = path(id) + ".rewards." + rewardKey;
        String reward = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(rewardPath + ".reward", "");
        giveReward(player, reward);
        consumeOne(player, item);
        String display = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(rewardPath + ".display", rewardKey);
        String rarity = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(rewardPath + ".rarity", "common");
        player.sendMessage(CC.color("&6Loot Box &8> &fYou found " + rarityColor(rarity) + display + "&f!"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
        return true;
    }

    public String getBoxId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(boxKey, PersistentDataType.STRING);
    }

    public boolean exists(String id) {
        return id != null && club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection(path(id));
    }

    private String pickReward(Player player, String boxId, ConfigurationSection rewards) {
        List<String> keys = new ArrayList<>(rewards.getKeys(false));
        List<String> recent = recentRewards(player, boxId);
        if (keys.size() > recent.size()) keys.removeIf(recent::contains);
        double total = 0.0D;
        for (String key : keys) total += Math.max(0.0D, rewards.getDouble(key + ".chance", 0.0D));
        if (total <= 0.0D) return keys.get(random.nextInt(keys.size()));
        double roll = random.nextDouble() * total;
        double cursor = 0.0D;
        for (String key : keys) {
            cursor += Math.max(0.0D, rewards.getDouble(key + ".chance", 0.0D));
            if (roll <= cursor) return key;
        }
        return keys.getLast();
    }

    private List<String> recentRewards(Player player, String boxId) {
        if (dataManager == null || player == null) return List.of();
        return new ArrayList<>(dataManager.getData().getStringList(recentPath(player, boxId)));
    }

    private void rememberReward(Player player, String boxId, String rewardKey) {
        if (dataManager == null || player == null || rewardKey == null || rewardKey.isBlank()) return;
        int historySize = Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("loot-boxes.duplicate-protection.recent-history-size", 3));
        if (historySize == 0) return;
        List<String> recent = recentRewards(player, boxId);
        recent.removeIf(existing -> existing.equalsIgnoreCase(rewardKey));
        recent.addFirst(rewardKey);
        while (recent.size() > historySize) recent.removeLast();
        dataManager.getData().set(recentPath(player, boxId), recent);
        dataManager.save();
    }

    private String recentPath(Player player, String boxId) {
        return "players." + player.getUniqueId() + ".loot-boxes.recent." + normalize(boxId);
    }

    private void giveReward(Player player, String reward) {
        if (reward == null || reward.isBlank()) return;
        if (reward.toLowerCase(Locale.ROOT).startsWith("custom-item:")) {
            String id = reward.substring("custom-item:".length()).trim();
            player.getInventory().addItem(customItemManager.createItem(id, 1));
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.replace("%player%", player.getName()));
    }

    private void consumeOne(Player player, ItemStack item) {
        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private String rarityColor(String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "rare" -> "&9";
            case "epic" -> "&5";
            case "legendary" -> "&6";
            case "special" -> "&d";
            default -> "&f";
        };
    }

    private String path(String id) {
        return "loot-boxes.boxes." + normalize(id);
    }

    private String normalize(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }
}
