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

public final class BoostersCommand implements CommandExecutor, Listener {
    private final JavaPlugin plugin;
    private final DataManager dataManager;

    public BoostersCommand(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!player.hasPermission("fakevival.command.boosters")) return CommandUtil.noPermission(plugin, sender);
        openBoosters(player);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.equals("boosters")) return;
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
        if (name.equals("refresh")) {
            openBoosters(player);
            return;
        }

        String boosterId = loreValue(clicked, "Booster: ");
        if (boosterId == null) return;
        activate(player, normalize(boosterId));
        openBoosters(player);
    }

    private void openBoosters(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("boosters"), 54, CC.color("&8Your Boosters"));
        inventory.setItem(4, item(Material.EXPERIENCE_BOTTLE, "&bYour Boosters", List.of(
                "&7View active boosters and activate",
                "&7purchased boosters when available.",
                "",
                "&7Buy more from &e/store&7."
        )));

        inventory.setItem(10, activeItem(player, "skills", Material.EXPERIENCE_BOTTLE, "&bSkill XP Booster"));
        inventory.setItem(12, activeItem(player, "jobs", Material.EMERALD, "&aJob XP Booster"));
        inventory.setItem(14, activeItem(player, "dungeons", Material.ENDER_EYE, "&5Dungeon Booster"));

        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("store.boosters");
        int[] slots = {
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
        int index = 0;
        if (section != null) {
            for (String id : section.getKeys(false)) {
                if (index >= slots.length) break;
                String path = "store.boosters." + id;
                String target = normalize(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".target", "skills"));
                int owned = getOwned(player, id);
                List<String> lore = new ArrayList<>(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(path + ".lore"));
                lore.add("");
                lore.add("&7Owned: &e" + owned);
                lore.add("&7Active: &f" + remaining(player, target));
                if (owned <= 0) lore.add("&7Buy this booster from &e/store&7.");
                else if (hasActive(player, target)) lore.add("&7Can activate in &e" + remaining(player, target) + "&7.");
                else lore.add("&eClick to activate.");
                lore.add("&8Booster: " + id);
                inventory.setItem(slots[index++], item(
                        parseMaterial(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".material", "EXPERIENCE_BOTTLE")),
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".name", "&b" + pretty(id)),
                        lore
                ));
            }
        }

        inventory.setItem(49, item(Material.COMPASS, "&aRefresh", List.of("&7Refresh booster timers.")));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close this menu.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private ItemStack activeItem(Player player, String target, Material material, String name) {
        String remaining = remaining(player, target);
        double multiplier = activeMultiplier(player, target);
        List<String> lore = new ArrayList<>();
        if (multiplier > 1.0D) {
            lore.add("&aActive");
            lore.add("&7Multiplier: &e" + trim(multiplier) + "x");
            lore.add("&7Remaining: &e" + remaining);
        } else {
            lore.add("&7No active booster.");
        }
        return item(material, name, lore);
    }

    private void activate(Player player, String id) {
        String path = "store.boosters." + id;
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection(path)) {
            player.sendMessage(CC.color("&cThat booster is not configured."));
            return;
        }
        if (getOwned(player, id) <= 0) {
            player.sendMessage(CC.color("&cYou do not own that booster. Buy it from /store first."));
            return;
        }

        String target = normalize(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".target", "skills"));
        if (hasActive(player, target)) {
            player.sendMessage(CC.color("&cYou already have an active matching booster. You can activate this in &e" + remaining(player, target) + "&c."));
            return;
        }

        double multiplier = Math.max(1.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path + ".multiplier", 2.0D));
        int minutes = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".duration-minutes", 60));
        long expiresAt = System.currentTimeMillis() + (minutes * 60L * 1000L);
        for (String boosterTarget : targetsFor(target)) {
            String boosterPath = playerStorePath(player) + ".boosters." + boosterTarget;
            dataManager.getData().set(boosterPath + ".expires-at", expiresAt);
            dataManager.getData().set(boosterPath + ".multiplier", multiplier);
        }
        dataManager.getData().set(playerStorePath(player) + ".booster-inventory." + id, getOwned(player, id) - 1);
        dataManager.save();
        player.sendMessage(CC.color("&aActivated &f" + club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".name", pretty(id)) + "&a."));
    }

    private boolean hasActive(Player player, String target) {
        long now = System.currentTimeMillis();
        for (String boosterTarget : targetsFor(target)) {
            if (dataManager.getData().getLong(playerStorePath(player) + ".boosters." + boosterTarget + ".expires-at", 0L) > now) return true;
        }
        return false;
    }

    private double activeMultiplier(Player player, String target) {
        long now = System.currentTimeMillis();
        double multiplier = 1.0D;
        for (String boosterTarget : targetsFor(target)) {
            String path = playerStorePath(player) + ".boosters." + boosterTarget;
            if (dataManager.getData().getLong(path + ".expires-at", 0L) > now) {
                multiplier = Math.max(multiplier, dataManager.getData().getDouble(path + ".multiplier", 1.0D));
            }
        }
        return multiplier;
    }

    private String remaining(Player player, String target) {
        long now = System.currentTimeMillis();
        long expiresAt = 0L;
        for (String boosterTarget : targetsFor(target)) {
            expiresAt = Math.max(expiresAt, dataManager.getData().getLong(playerStorePath(player) + ".boosters." + boosterTarget + ".expires-at", 0L));
        }
        long seconds = Math.max(0L, (expiresAt - now) / 1000L);
        if (seconds <= 0L) return "None";
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        if (hours > 0L) return hours + "h " + (minutes % 60L) + "m";
        return Math.max(1L, minutes) + "m";
    }

    private List<String> targetsFor(String target) {
        return target.equals("all_progress") ? List.of("skills", "jobs") : List.of(target);
    }

    private int getOwned(Player player, String id) {
        return dataManager.getData().getInt(playerStorePath(player) + ".booster-inventory." + normalize(id), 0);
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
            return Material.CHEST;
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "Booster";
        String[] parts = normalize(text).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.format(Locale.ROOT, "%.2f", value);
    }
}
