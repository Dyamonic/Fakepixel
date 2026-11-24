package net.fakepixel.housing.build;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import net.fakepixel.housing.util.Compat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ProToolsService implements Listener {
    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final String title = "Pro Tools";
    private final Map<UUID, PendingAction> pending = new HashMap<>();

    public ProToolsService(HousingPlugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
    }

    public void open(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (!canUse(player, house)) {
            player.sendMessage(plugin.color("&cYou do not have permission to use Pro Tools in this house."));
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 36, title);
        inv.setItem(10, item(Compat.mat("WOODEN_AXE"), "&aSelection Wand", "&7Left-click sets position 1.", "&7Right-click sets position 2.", "", "&eClick to receive!"));
        inv.setItem(11, item(Compat.mat("BUCKET"), "&aFill Tool", "&7Fill your selection with", "&7a block type.", "", "&eClick to use!"));
        inv.setItem(12, item(Compat.mat("REDSTONE_COMPARATOR"), "&aReplace Tool", "&7Replace one block type", "&7with another in your selection.", "", "&eClick to use!"));
        inv.setItem(13, item(Compat.mat("BOOK"), "&aCopy Tool", "&7Copy your selected region.", "", "&eClick to use!"));
        inv.setItem(14, item(Compat.mat("CHEST"), "&aPaste Tool", "&7Paste your clipboard at", "&7your current location.", "", "&eClick to use!"));
        inv.setItem(15, item(Compat.mat("SHEARS"), "&aCut Tool", "&7Copy your selection, then", "&7clear the original region.", "", "&eClick to use!"));
        inv.setItem(16, item(Compat.mat("WATER_BUCKET"), "&aUndo Tool", "&7Undo the last Pro Tools edit.", "", "&eClick to use!"));
        inv.setItem(19, item(Compat.mat("LAVA_BUCKET"), "&aRedo Tool", "&7Redo the last undone edit.", "", "&eClick to use!"));
        inv.setItem(20, item(Compat.mat("BARRIER"), "&cClear Region", "&7Set your selected region", "&7to air.", "", "&eClick to use!"));
        inv.setItem(21, item(Compat.mat("BRICK"), "&aSet Walls", "&7Change only the outside", "&7walls of your selection.", "", "&eClick to use!"));
        inv.setItem(22, item(Compat.mat("GRASS"), "&aSet Floor", "&7Change only the bottom", "&7layer of your selection.", "", "&eClick to use!"));
        inv.setItem(31, item(Compat.mat("NETHER_STAR"), "&aHousing Menu", "&7Go back to the Housing Menu."));
        player.openInventory(inv);
    }

    public boolean canUse(Player player, House house) {
        return plugin.canUseProToolsInHouseMode(player, house);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!title.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (!canUse(player, house)) {
            player.closeInventory();
            player.sendMessage(plugin.color("&cYou do not have permission to use Pro Tools in this house."));
            return;
        }
        int slot = event.getRawSlot();
        if (slot == 10) { plugin.getSelectionService().giveWand(player); return; }
        if (slot == 11) { prompt(player, house, "fill", "&aType the block to fill with, like &estone&7 or &ewool:14&7."); return; }
        if (slot == 12) { prompt(player, house, "replace", "&aType replacement as &efrom to&7, like &estone glass&7."); return; }
        if (slot == 13) { plugin.getSelectionService().copy(player); return; }
        if (slot == 14) { plugin.getSelectionService().paste(player); return; }
        if (slot == 15) { plugin.getSelectionService().cut(player); return; }
        if (slot == 16) { plugin.getSelectionService().undo(player); return; }
        if (slot == 19) { plugin.getSelectionService().redo(player); return; }
        if (slot == 20) { plugin.getSelectionService().clear(player); return; }
        if (slot == 21) { prompt(player, house, "walls", "&aType the block to set the walls to."); return; }
        if (slot == 22) { prompt(player, house, "floor", "&aType the block to set the floor to."); return; }
        if (slot == 31) plugin.getHousingMenu().openOwnerMenu(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        PendingAction action = pending.remove(event.getPlayer().getUniqueId());
        if (action == null) return;
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> handlePrompt(event.getPlayer(), action, message));
    }

    private void handlePrompt(Player player, PendingAction pendingAction, String message) {
        House house = houses.getHouseByWorld(pendingAction.worldName);
        if (!canUse(player, house)) {
            player.sendMessage(plugin.color("&cYou cannot use Pro Tools in that house."));
            return;
        }
        if ("cancel".equalsIgnoreCase(message.trim())) {
            player.sendMessage(plugin.color("&cPro Tools action cancelled."));
            open(player);
            return;
        }
        SelectionService selection = plugin.getSelectionService();
        if ("replace".equals(pendingAction.type)) {
            String[] split = message.trim().split("\\s+");
            if (split.length < 2) {
                player.sendMessage(plugin.color("&cUse: <from-block> <to-block>"));
                open(player);
                return;
            }
            SelectionService.MaterialData from = selection.parseMaterial(split[0]);
            SelectionService.MaterialData to = selection.parseMaterial(split[1]);
            if (from == null || to == null) {
                player.sendMessage(plugin.color("&cUnknown block type."));
                open(player);
                return;
            }
            selection.replace(player, from, to);
            open(player);
            return;
        }
        SelectionService.MaterialData data = selection.parseMaterial(message.trim());
        if (data == null) {
            player.sendMessage(plugin.color("&cUnknown block type."));
            open(player);
            return;
        }
        if ("fill".equals(pendingAction.type)) selection.fill(player, data.material, data.data);
        else if ("walls".equals(pendingAction.type)) selection.setWalls(player, data.material, data.data);
        else if ("floor".equals(pendingAction.type)) selection.setFloor(player, data.material, data.data);
        open(player);
    }

    private void prompt(Player player, House house, String type, String message) {
        pending.put(player.getUniqueId(), new PendingAction(house.getWorldName(), type));
        player.closeInventory();
        player.sendMessage(plugin.color(message));
        player.sendMessage(plugin.color("&7Type &ccancel &7to cancel."));
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material == null ? Material.STONE : material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(plugin.color(name));
        java.util.List<String> colored = new java.util.ArrayList<>();
        for (String line : lore) colored.add(plugin.color(line));
        meta.setLore(colored);
        stack.setItemMeta(meta);
        return stack;
    }

    public String prettyPermission() {
        return ChatColor.stripColor("Pro Tools").toLowerCase(Locale.ENGLISH);
    }

    private static class PendingAction {
        private final String worldName;
        private final String type;

        private PendingAction(String worldName, String type) {
            this.worldName = worldName;
            this.type = type;
        }
    }
}
