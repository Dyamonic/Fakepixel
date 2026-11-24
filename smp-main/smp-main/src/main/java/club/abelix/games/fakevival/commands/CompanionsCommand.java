package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.CompanionManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

public final class CompanionsCommand implements CommandExecutor, Listener {
    private final JavaPlugin plugin;
    private final CompanionManager companionManager;

    public CompanionsCommand(JavaPlugin plugin, CompanionManager companionManager) {
        this.plugin = plugin;
        this.companionManager = companionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!player.hasPermission("fakevival.command.companions")) return CommandUtil.noPermission(plugin, sender);
        if (args.length > 0 && args[0].equalsIgnoreCase("despawn")) {
            companionManager.despawn(player, true);
            return true;
        }
        openCompanions(player);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type))) return;
        if (!type.startsWith("companions:") && !type.startsWith("companion-upgrades:")) return;
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
        if (name.equals("back")) {
            openCompanions(player, 0);
            return;
        }
        if (name.equals("previous page")) { openCompanions(player, Math.max(0, page(type) - 1)); return; }
        if (name.equals("next page")) { openCompanions(player, page(type) + 1); return; }
        if (name.equals("despawn companion")) {
            companionManager.despawn(player, true);
            openCompanions(player, page(type));
            return;
        }

        if (type.startsWith("companions:")) {
            String id = loreValue(clicked, "Companion: ");
            if (id == null) return;
            if (!companionManager.owns(player, id)) {
                player.sendMessage(CC.color("&cYou have not captured that companion."));
                return;
            }
            if (event.isRightClick()) openUpgrades(player, id);
            else {
                companionManager.summon(player, id);
                openCompanions(player, page(type));
            }
            return;
        }

        String upgrade = loreValue(clicked, "Upgrade: ");
        if (upgrade == null) return;
        String companionId = type.substring("companion-upgrades:".length());
        companionManager.upgrade(player, companionId, upgrade);
        openUpgrades(player, companionId);
    }

    private void openCompanions(Player player) {
        openCompanions(player, 0);
    }

    private void openCompanions(Player player, int requestedPage) {
        int[] slots = contentSlots();
        List<String> ids = companionManager.getCompanionIds();
        int maxPage = Math.max(0, (ids.size() - 1) / slots.length);
        int page = Math.clamp(requestedPage, 0, maxPage);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("companions:" + page), 54, CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("companions.menu.title", "&8Companions")));
        String selected = companionManager.selected(player);
        long cooldown = companionManager.cooldownLeftMillis(player);
        inventory.setItem(4, item(Material.NAME_TAG, "&bYour Companions", List.of(
                "&7Capture configured mobs with",
                "&fCompanion Snares&7.",
                "",
                "&7Selected: &f" + (selected.isBlank() ? "None" : ChatColor.stripColor(companionManager.displayName(selected))),
                "&7Active: " + (companionManager.isActive(player) ? "&aSummoned" : "&cNone"),
                cooldown > 0 ? "&7Respawn Cooldown: &e" + companionManager.formatDuration(cooldown) : "&7Respawn Cooldown: &aReady",
                "",
                "&eLeft click &7to summon.",
                "&eRight click &7to upgrade."
        )));

        int start = page * slots.length;
        for (int i = 0; i < slots.length && start + i < ids.size(); i++) {
            String id = ids.get(start + i);
            boolean owned = companionManager.owns(player, id);
            boolean isSelected = selected.equals(id);
            List<String> lore = new ArrayList<>();
            lore.add("&7A cosmetic companion that follows you.");
            lore.add("&7Category: &e" + pretty(companionManager.category(id)));
            lore.add("&7Rarity: &f" + companionManager.rarity(id));
            lore.add(owned ? "&aUnlocked" : "&cLocked");
            if (isSelected) lore.add("&eSelected");
            if (owned) {
                lore.add("&7Level: &e" + companionManager.level(player, id) + "&7/&e" + companionManager.maxLevel(id));
                lore.add("&7XP: &e" + companionManager.xp(player, id) + "&7/&e" + companionManager.xpNeeded(player, id));
                lore.add("");
                lore.addAll(statLore(player, id));
                lore.add("");
                lore.addAll(skillLore(id));
                lore.add("");
                lore.add("&eLeft click to summon.");
                lore.add("&eRight click for upgrades.");
            } else {
                lore.add("&7Use a Companion Snare on this mob.");
            }
            lore.add("&8Companion: " + id);
            inventory.setItem(slots[i], item(companionManager.icon(id), companionManager.displayName(id), lore));
        }

        inventory.setItem(49, item(Material.BARRIER, "&cDespawn Companion", List.of("&7Remove your active companion.")));
        if (page > 0) inventory.setItem(48, item(Material.ARROW, "&ePrevious Page", List.of("&7Page " + page)));
        if (page < maxPage) inventory.setItem(50, item(Material.ARROW, "&eNext Page", List.of("&7Page " + (page + 2))));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close this menu.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private int page(String holder) {
        if (!holder.startsWith("companions:")) return 0;
        try { return Integer.parseInt(holder.substring("companions:".length())); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private void openUpgrades(Player player, String id) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("companion-upgrades:" + normalize(id)), 54, CC.color("&8Upgrades: " + ChatColor.stripColor(companionManager.displayName(id))));
        inventory.setItem(4, item(companionManager.icon(id), companionManager.displayName(id), List.of(
                "&7Level: &e" + companionManager.level(player, id) + "&7/&e" + companionManager.maxLevel(id),
                "&7XP: &e" + companionManager.xp(player, id) + "&7/&e" + companionManager.xpNeeded(player, id),
                "&7Health: &c" + oneDecimal(companionManager.health(player, id)),
                "&7Defense: &9" + oneDecimal(companionManager.defense(player, id)),
                "&7Agility: &b" + oneDecimal(companionManager.agility(player, id)),
                "&7Damage: &c" + oneDecimal(companionManager.damage(player, id)),
                "",
                "&7Upgrade with craftable companion items."
        )));

        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 30, 32, 34};
        List<String> upgrades = companionManager.upgrades(id);
        for (int i = 0; i < slots.length && i < upgrades.size(); i++) {
            String upgrade = upgrades.get(i);
            int level = companionManager.upgradeLevel(player, id, upgrade);
            String path = "companions.types." + normalize(id) + ".upgrades." + normalize(upgrade);
            int max = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".max-level", 5));
            int amount = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".amount", level + 1));
            String itemId = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".item", "companion_treat");
            Material icon = switch (normalize(upgrade)) {
                case "vitality" -> Material.GOLDEN_APPLE;
                case "agility" -> Material.SUGAR;
                case "fortitude" -> Material.SHIELD;
                case "focus" -> Material.AMETHYST_SHARD;
                case "loyalty" -> Material.NAME_TAG;
                default -> Material.IRON_SWORD;
            };
            inventory.setItem(slots[i], item(icon, "&e" + pretty(upgrade), List.of(
                    level >= max ? "&aMaxed" : "&7Level: &e" + level + "&7/&e" + max,
                    "&7Cost: &e" + amount + "x &f" + pretty(itemId),
                    "",
                    level >= max ? "&7No further upgrades." : "&eClick to upgrade.",
                    "&8Upgrade: " + upgrade
            )));
        }

        inventory.setItem(45, item(Material.ARROW, "&cBack", List.of("&7Return to companions.")));
        inventory.setItem(53, item(Material.BARRIER, "&cClose", List.of("&7Close this menu.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private String loreValue(ItemStack item, String prefix) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean != null && clean.startsWith(prefix)) return clean.substring(prefix.length()).trim();
        }
        return null;
    }

    private List<String> statLore(Player player, String id) {
        return List.of(
                "&7Health: &c" + oneDecimal(companionManager.health(player, id)),
                "&7Defense: &9" + oneDecimal(companionManager.defense(player, id)),
                "&7Agility: &b" + oneDecimal(companionManager.agility(player, id)),
                "&7Damage: &c" + oneDecimal(companionManager.damage(player, id))
        );
    }

    private List<String> skillLore(String id) {
        List<String> result = new ArrayList<>();
        List<String> skills = companionManager.skills(id);
        if (skills.isEmpty()) return result;
        result.add("&7Skills:");
        for (String skill : skills) {
            String path = "companions.types." + normalize(id) + ".skills." + normalize(skill);
            String name = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".name", pretty(skill));
            int unlock = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".unlock-level", 1));
            result.add("&8- &f" + name + " &7Lv. &e" + unlock);
        }
        return result;
    }

    private String oneDecimal(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private int[] contentSlots() {
        return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
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

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private String pretty(String text) {
        String[] parts = normalize(text).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
