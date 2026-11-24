package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CosmeticsCommand implements CommandExecutor, Listener {
    private final JavaPlugin plugin;
    private final DataManager dataManager;

    public CosmeticsCommand(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!player.hasPermission("fakevival.command.cosmetics")) return CommandUtil.noPermission(plugin, sender);
        openCosmetics(player);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.equals("cosmetics")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT)
                : "";
        if (name.equals("close")) {
            player.closeInventory();
            return;
        }
        if (name.equals("clear cosmetic")) {
            dataManager.getData().set(playerStorePath(player) + ".selected-cosmetic", null);
            dataManager.save();
            player.sendMessage(CC.color("&aCosmetic cleared."));
            openCosmetics(player);
            return;
        }

        String cosmeticId = loreValue(clicked, "Cosmetic: ");
        if (cosmeticId == null) return;
        toggleCosmetic(player, normalize(cosmeticId));
        openCosmetics(player);
    }

    private void openCosmetics(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("cosmetics"), 54, CC.color("&8Your Cosmetics"));
        String selected = selectedCosmetic(player);
        inventory.setItem(4, item(Material.AMETHYST_SHARD, "&dYour Cosmetics", List.of(
                "&7Select or unselect cosmetics",
                "&7you bought from &e/store&7.",
                "",
                "&7Selected: &f" + (selected.isBlank() ? "None" : pretty(selected))
        )));

        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("store.cosmetics");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = "store.cosmetics." + id;
                boolean owned = owns(player, id);
                boolean active = selected.equals(normalize(id));
                List<String> lore = new ArrayList<>(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(path + ".lore"));
                lore.add("");
                lore.add(owned ? "&aOwned" : "&cLocked");
                if (active) lore.add("&eSelected");
                lore.add("");
                lore.add(owned ? (active ? "&eClick to unselect." : "&eClick to select.") : "&7Buy this cosmetic from &e/store&7.");
                lore.add("&8Cosmetic: " + id);
                inventory.setItem(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".slot", 10),
                        item(parseMaterial(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".material", "AMETHYST_SHARD")),
                                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".name", "&d" + pretty(id)),
                                lore));
            }
        }

        inventory.setItem(49, item(Material.BARRIER, "&cClear Cosmetic", List.of("&7Unselect your active cosmetic.")));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close this menu.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private void toggleCosmetic(Player player, String id) {
        if (!owns(player, id)) {
            player.sendMessage(CC.color("&cYou do not own that cosmetic. Buy it from /store first."));
            return;
        }
        String path = playerStorePath(player) + ".selected-cosmetic";
        if (selectedCosmetic(player).equals(id)) {
            dataManager.getData().set(path, null);
            player.sendMessage(CC.color("&aCosmetic unselected."));
        } else {
            dataManager.getData().set(path, id);
            player.sendMessage(CC.color("&aSelected &f" + club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("store.cosmetics." + id + ".name", pretty(id)) + "&a."));
        }
        dataManager.save();
    }

    private boolean owns(Player player, String id) {
        return dataManager.getData().getBoolean(playerStorePath(player) + ".cosmetics." + normalize(id), false);
    }

    private String selectedCosmetic(Player player) {
        return normalize(dataManager.getData().getString(playerStorePath(player) + ".selected-cosmetic", ""));
    }

    private String playerStorePath(Player player) {
        return "players." + player.getUniqueId() + ".store";
    }

    private String loreValue(ItemStack item, String prefix) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean != null && clean.startsWith(prefix)) return clean.substring(prefix.length()).trim();
        }
        return null;
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            List<String> colored = new ArrayList<>();
            for (String line : lore) colored.add(CC.color(line));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
    }

    private Material parseMaterial(String material) {
        try {
            return Material.valueOf(String.valueOf(material).toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return Material.AMETHYST_SHARD;
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "Cosmetic";
        String[] parts = normalize(text).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }
}
