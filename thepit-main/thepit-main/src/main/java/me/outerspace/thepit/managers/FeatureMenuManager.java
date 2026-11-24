package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.Period;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class FeatureMenuManager implements Listener {
    private static final String CHAT_DISABLED = "chat-public-disabled";
    private final ThePitPlugin plugin;

    public FeatureMenuManager(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void openChatSettings(Player player) {
        FeatureHolder holder = new FeatureHolder(player.getUniqueId(), MenuType.CHAT);
        Inventory inventory = Bukkit.createInventory(holder, 27, CC.color("&8Chat Settings"));
        holder.inventory = inventory;
        fill(inventory);
        boolean enabled = plugin.getStatsManager().getLongStat(player.getUniqueId(), CHAT_DISABLED) == 0L;
        inventory.setItem(13, item(enabled ? Material.INK_SACK : Material.INK_SACK, enabled ? (short) 10 : (short) 8,
                enabled ? "&aPublic Chat: Enabled" : "&cPublic Chat: Disabled",
                Arrays.asList("&7Controls whether you receive", "&7normal The Pit public chat.", "", "&eClick to toggle!")));
        inventory.setItem(22, item(Material.BARRIER, (short) 0, "&cClose", Collections.emptyList()));
        player.openInventory(inventory);
    }

    public void openView(Player viewer, Player target) {
        FeatureHolder holder = new FeatureHolder(viewer.getUniqueId(), MenuType.VIEW);
        Inventory inventory = Bukkit.createInventory(holder, 27, CC.color("&8" + target.getName()));
        holder.inventory = inventory;
        fill(inventory);
        UUID uuid = target.getUniqueId();
        NumberFormat whole = NumberFormat.getIntegerInstance(Locale.US);
        inventory.setItem(13, item(Material.SKULL_ITEM, (short) 3, "&b" + target.getName(), Arrays.asList(
                "&7Level: " + plugin.getStatsManager().getFormattedLevel(uuid),
                "&7Prestige: &e" + plugin.getStatsManager().getPrestigeFormatted(uuid),
                "&7Lifetime XP: &b" + whole.format(plugin.getStatsManager().getLifetimeXp(uuid)),
                "&7Gold: &6" + whole.format(plugin.getPitGoldService().balance(uuid)),
                "&7Kills: &a" + whole.format(plugin.getStatsManager().getKills(uuid, Period.LIFETIME)),
                "&7Deaths: &c" + whole.format(plugin.getStatsManager().getDeaths(uuid, Period.LIFETIME))
        )));
        inventory.setItem(22, item(Material.BARRIER, (short) 0, "&cClose", Collections.emptyList()));
        viewer.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FeatureHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !holder.viewer.equals(player.getUniqueId())) return;
        if (event.getRawSlot() == 22) {
            player.closeInventory();
            return;
        }
        if (holder.type != MenuType.CHAT || event.getRawSlot() != 13) return;
        boolean disabled = plugin.getStatsManager().getLongStat(player.getUniqueId(), CHAT_DISABLED) > 0L;
        plugin.getStatsManager().setLongStat(player.getUniqueId(), CHAT_DISABLED, disabled ? 0L : 1L);
        plugin.getStatsManager().flushPlayer(player.getUniqueId(), false);
        openChatSettings(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof FeatureHolder) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        for (PermissionAttachmentInfo permission : event.getPlayer().getEffectivePermissions()) {
            String name = permission.getPermission().toLowerCase(Locale.ENGLISH).replace("-", "").replace("_", "").replace(".", "");
            if (permission.getValue() && name.contains("staffchat")) return;
        }
        event.getRecipients().removeIf(player -> plugin.getStatsManager().isProfileLoaded(player.getUniqueId())
                && (plugin.getStatsManager().getLongStat(player.getUniqueId(), CHAT_DISABLED) > 0L
                || plugin.getPitCommandManager() != null && !plugin.getPitCommandManager().allowsChat(player.getUniqueId(), "player-chat")));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().trim().toLowerCase(Locale.ENGLISH);
        if (!command.equals("/trade") && !command.startsWith("/trade ")) return;
        LeaderboardManager.FeatureDefinition feature = plugin.getLeaderboardManager().feature("trade");
        if (feature == null) return;
        if (plugin.getLeaderboardManager().isFeatureUnlocked(event.getPlayer().getUniqueId(), "trade")) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cRequires Level " + feature.level() + "."));
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
    }

    private ItemStack item(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(CC.color(line));
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    private enum MenuType { CHAT, VIEW }

    private static final class FeatureHolder implements InventoryHolder {
        private final UUID viewer;
        private final MenuType type;
        private Inventory inventory;

        private FeatureHolder(UUID viewer, MenuType type) {
            this.viewer = viewer;
            this.type = type;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }
}
