package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KitManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;

    public KitManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        ensureDefaultSection();
    }

    private void ensureDefaultSection() {
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection("kits")) return;
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("kits.starter.display-name", "&aStarter");
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("kits.starter.delay", 86400L);
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("kits.starter.permission", "");
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("kits.starter.items", List.of(
                new ItemStack(Material.STONE_SWORD),
                new ItemStack(Material.STONE_PICKAXE),
                new ItemStack(Material.BREAD, 16),
                new ItemStack(Material.TORCH, 16)
        ));
        plugin.saveConfig();
    }

    public boolean exists(String kitName) {
        return getSection(kitName) == null;
    }

    public List<String> getKitNames() {
        ConfigurationSection kits = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("kits");
        if (kits == null) return Collections.emptyList();
        List<String> names = new ArrayList<>(kits.getKeys(false));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public String getDisplayName(String kitName) {
        ConfigurationSection section = getSection(kitName);
        if (section == null) return kitName;
        return section.getString("display-name", kitName);
    }

    public long getDelaySeconds(String kitName) {
        ConfigurationSection section = getSection(kitName);
        if (section == null) return 0L;
        return Math.max(0L, section.getLong("delay", 0L));
    }

    public String getPermission(String kitName) {
        ConfigurationSection section = getSection(kitName);
        if (section == null) return "";
        return section.getString("permission", "");
    }

    public boolean canUseKit(Player player, String kitName) {
        String permission = getPermission(kitName);
        return permission == null || permission.isBlank() || player.hasPermission(permission) || player.hasPermission("fakevival.admin.kit");
    }

    public long getRemainingSeconds(Player player, String kitName) {
        if (player.hasPermission("fakevival.command.kit.cooldown.bypass") || player.hasPermission("fakevival.admin.kit")) return 0L;
        long delay = getDelaySeconds(kitName);
        if (delay <= 0L) return 0L;
        long lastClaimed = dataManager.getData().getLong(lastClaimPath(player, kitName), 0L);
        if (lastClaimed <= 0L) return 0L;
        long elapsed = (System.currentTimeMillis() - lastClaimed) / 1000L;
        return Math.max(0L, delay - elapsed);
    }

    public List<ItemStack> getItems(String kitName) {
        ConfigurationSection section = getSection(kitName);
        if (section == null) return Collections.emptyList();
        List<?> rawItems = section.getList("items", Collections.emptyList());
        List<ItemStack> items = new ArrayList<>();
        for (Object rawItem : rawItems) {
            if (rawItem instanceof ItemStack item && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        return items;
    }

    public int claim(Player player, String kitName) {
        List<ItemStack> items = getItems(kitName);
        if (items.isEmpty()) return 0;
        for (ItemStack item : items) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
            for (ItemStack extra : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), extra);
            }
        }
        dataManager.getData().set(lastClaimPath(player, kitName), System.currentTimeMillis());
        dataManager.save();
        return items.size();
    }

    public void createKit(Player player, String kitName, long delaySeconds) {
        String normalized = normalize(kitName);
        List<ItemStack> items = snapshotInventory(player);
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("kits." + normalized + ".display-name", "&a" + normalized);
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("kits." + normalized + ".delay", Math.max(0L, delaySeconds));
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("kits." + normalized + ".permission", "");
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("kits." + normalized + ".items", items);
        plugin.saveConfig();
    }

    public boolean deleteKit(String kitName) {
        String normalized = normalize(kitName);
        if (exists(normalized)) return false;
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("kits." + normalized, null);
        plugin.saveConfig();
        return true;
    }

    public boolean setDelay(String kitName, long delaySeconds) {
        ConfigurationSection section = getSection(kitName);
        if (section == null) return false;
        section.set("delay", Math.max(0L, delaySeconds));
        plugin.saveConfig();
        return true;
    }

    public boolean setPermission(String kitName, String permission) {
        ConfigurationSection section = getSection(kitName);
        if (section == null) return false;
        section.set("permission", permission == null ? "" : permission);
        plugin.saveConfig();
        return true;
    }

    public boolean resetCooldown(Player player, String kitName) {
        if (exists(kitName)) return false;
        dataManager.getData().set(lastClaimPath(player, kitName), null);
        dataManager.save();
        return true;
    }

    public String normalize(String kitName) {
        return kitName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
    }

    private ConfigurationSection getSection(String kitName) {
        String normalized = normalize(kitName);
        if (normalized.isBlank()) return null;
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("kits." + normalized);
    }

    private List<ItemStack> snapshotInventory(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().getStorageContents()) addIfReal(items, item);
        for (ItemStack item : player.getInventory().getArmorContents()) addIfReal(items, item);
        addIfReal(items, player.getInventory().getItemInOffHand());
        return items;
    }

    private void addIfReal(List<ItemStack> items, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) return;
        items.add(item.clone());
    }

    private String lastClaimPath(Player player, String kitName) {
        return "players." + player.getUniqueId() + ".kits." + normalize(kitName) + ".last-claimed";
    }
}
