package net.fakepixel.housing.menu;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.group.HouseGroups;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GroupsMenu implements Listener {
    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final String title = "Groups";
    private final String editTitle = "Edit Group";
    private final String permissionsTitle = "Edit Permissions";
    private final String colorTitle = "Select Group Color";
    private final Map<UUID, String> creatingGroupForWorld = new HashMap<>();
    private final Map<UUID, String> editingGroup = new HashMap<>();

    private static final String[] PERMISSIONS = new String[] {
            "gamemode", "build", "action-pad", "region-edit", "custom-command", "function", "pro-tools", "kick", "mute", "ban", "chat", "teleport", "fly", "settings"
    };
    private static final int[] PERMISSION_SLOTS = new int[] {
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25
    };

    private static final GroupColor[] GROUP_COLORS = new GroupColor[] {
            new GroupColor("Dark Blue", "&1", 1, "INK_SACK", 4, "INK_SACK"),
            new GroupColor("Dark Green", "&2", 2, "INK_SACK", 2, "INK_SACK"),
            new GroupColor("Dark Aqua", "&3", 3, "LAPIS_BLOCK", 0, "LAPIS_BLOCK"),
            new GroupColor("Dark Red", "&4", 4, "INK_SACK", 1, "INK_SACK"),
            new GroupColor("Dark Purple", "&5", 5, "INK_SACK", 5, "INK_SACK"),
            new GroupColor("Gold", "&6", 6, "INK_SACK", 14, "INK_SACK"),
            new GroupColor("Gray", "&7", 7, "INK_SACK", 7, "INK_SACK"),
            new GroupColor("Dark Gray", "&8", 8, "INK_SACK", 8, "INK_SACK"),
            new GroupColor("Blue", "&9", 9, "INK_SACK", 12, "INK_SACK"),
            new GroupColor("Green", "&a", 10, "INK_SACK", 10, "INK_SACK"),
            new GroupColor("Aqua", "&b", 11, "INK_SACK", 6, "INK_SACK"),
            new GroupColor("Red", "&c", 12, "INK_SACK", 1, "INK_SACK"),
            new GroupColor("Light Purple", "&d", 13, "INK_SACK", 9, "INK_SACK"),
            new GroupColor("Yellow", "&e", 14, "INK_SACK", 11, "INK_SACK"),
            new GroupColor("White", "&f", 15, "INK_SACK", 15, "INK_SACK")
    };

    private static final int[] GROUP_COLOR_SLOTS = new int[] {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25, 26
    };

    public GroupsMenu(HousingPlugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
    }

    public void open(Player player) {
        House house = currentHouse(player);
        if (house == null || !house.canManage(player.getUniqueId())) {
            player.sendMessage(plugin.color("&cOnly the owner/co-owner can open groups."));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 36, title);
        inv.setItem(10, groupItem(house, HouseGroups.OWNER));
        inv.setItem(11, groupItem(house, HouseGroups.CO_OWNER));
        inv.setItem(12, groupItem(house, HouseGroups.RESIDENT));
        inv.setItem(13, groupItem(house, HouseGroups.GUEST));

        int slot = 19;
        for (String group : house.getCustomGroups()) {
            if (slot >= 26) break;
            inv.setItem(slot++, groupItem(house, group));
        }

        inv.setItem(31, item(Compat.mat("BARRIER"), "&aGo Back", "&7Back to the Housing Menu."));
        inv.setItem(35, item(Compat.mat("SIGN"), "&aCreate New Group", "&7Create a custom group", "&7for this house.", "", "&eClick to create!"));
        player.openInventory(inv);
    }

    private void openEdit(Player player, String group) {
        House house = currentHouse(player);
        if (house == null || !house.canManage(player.getUniqueId())) {
            player.sendMessage(plugin.color("&cOnly the owner/co-owner can edit groups."));
            return;
        }
        String normalized = HouseGroups.normalize(group);
        editingGroup.put(player.getUniqueId(), normalized);
        Inventory inv = Bukkit.createInventory(null, 36, editTitle);
        inv.setItem(10, item(Compat.mat("PAPER"), "&aRename group", "&7Change the name of this group.", "", "&cGUI is currently disabled,", "&cuse &e/groupname " + normalized + " <new name>", "&cinstead!"));
        inv.setItem(11, item(Compat.mat("BOOK"), "&aChange Tag", "&7Change the tag shown next to", "&7players names in this group.", "", "&7Current tag: " + house.groupSuffix(normalized), "", "&cGUI is currently disabled,", "&cuse &e/grouptag " + normalized + " <tag>", "&cinstead!"));
        if (HouseGroups.OWNER.equals(normalized)) {
            inv.setItem(12, item(Compat.mat("REDSTONE"), "&cEdit permissions", "&7The OWNER group has every permission", "&7and cannot be edited."));
        } else {
            inv.setItem(12, item(Compat.mat("REDSTONE"), "&aEdit permissions", "&7Clicking this allows you to edit", "&7" + normalized.toUpperCase().replace("-", " ") + " permissions."));
        }
        inv.setItem(13, item(Compat.mat("GOLD_INGOT"), "&cChange priority", "&7Currently unavailable."));
        inv.setItem(14, item(Compat.mat("WOOL"), "&aChange color", "&7Change the color of this group."));

        inv.setItem(22, item(Compat.mat("DIAMOND"), "&aDefault group", "&7Make " + normalized.toUpperCase().replace("-", " ") + " the default group", "&7for all players without a", "&7group set.", "", "&eClick to set!"));
        inv.setItem(31, item(Compat.mat("ARROW"), "&aGo Back", "&7Go back to Groups & Permissions."));
        if (canDeleteGroup(normalized)) {
            inv.setItem(33, item(Compat.mat("TNT"), "&cDelete group", "&7Delete the custom group", "&7" + normalized + "&7.", "", "&eClick to delete!"));
        } else {
            inv.setItem(33, item(Compat.mat("BARRIER"), "&cDelete group", "&7Default groups cannot be deleted."));
        }
        player.openInventory(inv);
    }

    private void openColor(Player player, String group) {
        House house = currentHouse(player);
        if (house == null || !house.canManage(player.getUniqueId())) {
            player.sendMessage(plugin.color("&cOnly the owner/co-owner can change group colors."));
            return;
        }
        String normalized = HouseGroups.normalize(group);
        editingGroup.put(player.getUniqueId(), normalized);
        Inventory inv = Bukkit.createInventory(null, 36, colorTitle);
        for (int i = 0; i < GROUP_COLORS.length && i < GROUP_COLOR_SLOTS.length; i++) {
            inv.setItem(GROUP_COLOR_SLOTS[i], colorItem(GROUP_COLORS[i], house.getGroupColor(normalized).equalsIgnoreCase(GROUP_COLORS[i].code)));
        }
        inv.setItem(31, item(Compat.mat("ARROW"), "&aGo Back", "&7Go back to Edit Group."));
        inv.setItem(35, item(Compat.mat("NETHER_STAR"), "&aMain Menu", "&7Back to the Housing Menu."));
        player.openInventory(inv);
    }

    private void openPermissions(Player player, String group) {
        House house = currentHouse(player);
        if (house == null || !house.canManage(player.getUniqueId())) {
            player.sendMessage(plugin.color("&cOnly the owner/co-owner can edit permissions."));
            return;
        }
        String normalized = HouseGroups.normalize(group);
        if (HouseGroups.OWNER.equals(normalized)) {
            player.sendMessage(plugin.color("&cThe OWNER group permissions cannot be edited."));
            openEdit(player, normalized);
            return;
        }
        editingGroup.put(player.getUniqueId(), normalized);
        Inventory inv = Bukkit.createInventory(null, 36, permissionsTitle);
        for (int i = 0; i < PERMISSIONS.length; i++) {
            inv.setItem(PERMISSION_SLOTS[i], permissionItem(house, normalized, PERMISSIONS[i]));
        }
        inv.setItem(31, item(Compat.mat("ARROW"), "&aGo Back", "&7Go back to Edit Group."));
        inv.setItem(35, item(Compat.mat("NETHER_STAR"), "&aMain Menu", "&7Back to the Housing Menu."));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String viewTitle = event.getView().getTitle();
        if (!viewTitle.equals(title) && !viewTitle.equals(editTitle) && !viewTitle.equals(permissionsTitle) && !viewTitle.equals(colorTitle)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;

        if (viewTitle.equals(title)) {
            if (slot == 31) { plugin.getHousingMenu().openOwnerMenu(player); return; }
            if (slot == 35) { beginCreateGroup(player); return; }
            String group = groupFromSlot(player, slot);
            if (group != null) openEdit(player, group);
            return;
        }

        if (viewTitle.equals(editTitle)) {
            if (slot == 31) { open(player); return; }
            if (slot == 12) {
                String group = editingGroup.get(player.getUniqueId());
                if (group != null) openPermissions(player, group);
                return;
            }
            if (slot == 14) {
                String group = editingGroup.get(player.getUniqueId());
                if (group != null) openColor(player, group);
                return;
            }
            if (slot == 33) {
                House house = currentHouse(player);
                String group = editingGroup.get(player.getUniqueId());
                if (house != null && group != null && canDeleteGroup(group)) {
                    house.removeCustomGroup(group);
                    houses.saveAll();
                    player.sendMessage(plugin.color("&aDeleted group &e" + group + "&a."));
                    open(player);
                }
                return;
            }
            if (slot == 22) {
                player.sendMessage(plugin.color("&eDefault group setting is ready to be hooked into permissions."));
                return;
            }
            player.sendMessage(plugin.color("&eThis group option is ready to be hooked into commands/permissions."));
            return;
        }

        if (viewTitle.equals(colorTitle)) {
            if (slot == 31) {
                String group = editingGroup.get(player.getUniqueId());
                if (group == null) open(player); else openEdit(player, group);
                return;
            }
            if (slot == 35) { plugin.getHousingMenu().openOwnerMenu(player); return; }
            GroupColor selected = colorFromSlot(slot);
            if (selected == null) return;
            House house = currentHouse(player);
            String group = editingGroup.get(player.getUniqueId());
            if (house == null || group == null) return;
            house.setGroupColor(group, selected.code);
            houses.saveAll();
            if (plugin.getNametagService() != null) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getWorld().getName().equals(house.getWorldName())) plugin.getNametagService().update(online);
                }
            }
            player.sendMessage(plugin.color("&aSet group color to " + selected.code + selected.name + "&a!"));
            openColor(player, group);
            return;
        }

        if (viewTitle.equals(permissionsTitle)) {
            if (slot == 31) {
                String group = editingGroup.get(player.getUniqueId());
                if (group == null) open(player); else openEdit(player, group);
                return;
            }
            if (slot == 35) { plugin.getHousingMenu().openOwnerMenu(player); return; }
            String permission = permissionFromSlot(slot);
            if (permission == null) return;
            House house = currentHouse(player);
            String group = editingGroup.get(player.getUniqueId());
            if (house == null || group == null) return;
            if (HouseGroups.OWNER.equals(HouseGroups.normalize(group))) {
                player.sendMessage(plugin.color("&cThe OWNER group permissions cannot be edited."));
                return;
            }
            boolean enabled = house.toggleGroupPermission(group, permission);
            houses.saveAll();
            player.sendMessage(plugin.color((enabled ? "&aEnabled " : "&cDisabled ") + "&e" + prettyPermission(permission) + " &7for " + house.groupSuffix(group) + "&7."));
            openPermissions(player, group);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String worldName = creatingGroupForWorld.remove(player.getUniqueId());
        if (worldName == null) return;
        event.setCancelled(true);

        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(plugin.color("&cGroup creation cancelled."));
                open(player);
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            House house = houses.getHouseByWorld(worldName);
            if (house == null) house = houses.getHouse(player.getUniqueId());
            if (house == null || !house.canManage(player.getUniqueId())) {
                player.sendMessage(plugin.color("&cYou cannot create a group here."));
                return;
            }
            String clean = sanitizeGroupName(message);
            if (clean.length() < 2) {
                player.sendMessage(plugin.color("&cGroup name is too short."));
                open(player);
                return;
            }
            if (clean.length() > 16) clean = clean.substring(0, 16);
            boolean added = house.addCustomGroup(clean);
            houses.saveAll();
            player.sendMessage(plugin.color(added ? "&aCreated group &e" + clean + "&a." : "&cThat group already exists or is a default group."));
            open(player);
        });
    }

    private void beginCreateGroup(Player player) {
        House house = currentHouse(player);
        if (house == null || !house.canManage(player.getUniqueId())) return;
        creatingGroupForWorld.put(player.getUniqueId(), house.getWorldName());
        player.closeInventory();
        player.sendMessage(plugin.color("&aType the new group name in chat. Type &ccancel &ato cancel."));
    }

    private House currentHouse(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) house = houses.getHouse(player.getUniqueId());
        return house;
    }

    private String groupFromSlot(Player player, int slot) {
        if (slot == 10) return HouseGroups.OWNER;
        if (slot == 11) return HouseGroups.CO_OWNER;
        if (slot == 12) return HouseGroups.RESIDENT;
        if (slot == 13) return HouseGroups.GUEST;
        House house = currentHouse(player);
        if (house == null) return null;
        int customSlot = 19;
        for (String group : house.getCustomGroups()) {
            if (slot == customSlot) return group;
            customSlot++;
            if (customSlot >= 26) break;
        }
        return null;
    }

    private boolean canDeleteGroup(String group) {
        String normalized = HouseGroups.normalize(group);
        return !normalized.equals(HouseGroups.OWNER)
                && !normalized.equals(HouseGroups.CO_OWNER)
                && !normalized.equals(HouseGroups.RESIDENT)
                && !normalized.equals(HouseGroups.GUEST);
    }

    private String sanitizeGroupName(String name) {
        String stripped = ChatColor.stripColor(name).trim().toLowerCase();
        stripped = stripped.replaceAll("[^a-z0-9 _-]", "").replace('_', '-').replace(' ', '-');
        while (stripped.contains("--")) stripped = stripped.replace("--", "-");
        if (stripped.startsWith("-")) stripped = stripped.substring(1);
        if (stripped.endsWith("-")) stripped = stripped.substring(0, stripped.length() - 1);
        return stripped;
    }

    private ItemStack groupItem(String group) {
        House house = null;
        String normalized = HouseGroups.normalize(group);
        String label = normalized.toUpperCase().replace("-", " ");
        return item(Compat.mat("WOOL"), HouseGroups.suffix(normalized), "&7Edit the group &e" + label, "&7permissions, name, tag and more.", "", "&eClick to edit!");
    }

    private ItemStack groupItem(House house, String group) {
        String normalized = HouseGroups.normalize(group);
        String label = normalized.toUpperCase().replace("-", " ");
        String display = house == null ? HouseGroups.suffix(normalized) : house.groupSuffix(normalized);
        return item(Compat.mat("WOOL"), display, "&7Edit the group &e" + label, "&7permissions, name, tag and more.", "", "&eClick to edit!");
    }

    private ItemStack permissionItem(House house, String group, String permission) {
        boolean enabled = house.hasGroupPermission(group, permission);
        return item(enabled ? Compat.limeDye() : Compat.grayDye(),
                (enabled ? "&a" : "&7") + prettyPermission(permission),
                "&7Click to toggle",
                "&7Current: " + (enabled ? "&aEnabled" : "&cDisabled"));
    }

    private String permissionFromSlot(int slot) {
        for (int i = 0; i < PERMISSION_SLOTS.length; i++) {
            if (PERMISSION_SLOTS[i] == slot) return PERMISSIONS[i];
        }
        return null;
    }

    private String prettyPermission(String permission) {
        if (permission == null || permission.isEmpty()) return "Unknown";
        if (permission.equals("gamemode")) return "Change Gamemode";
        if (permission.equals("pro-tools")) return "Pro Tools";
        if (permission.equals("settings")) return "Edit House Settings";
        return Character.toUpperCase(permission.charAt(0)) + permission.substring(1).replace('-', ' ');
    }

    private GroupColor colorFromSlot(int slot) {
        for (int i = 0; i < GROUP_COLOR_SLOTS.length && i < GROUP_COLORS.length; i++) {
            if (GROUP_COLOR_SLOTS[i] == slot) return GROUP_COLORS[i];
        }
        return null;
    }

    private ItemStack colorItem(GroupColor color, boolean selected) {
        Material material = Compat.mat(color.modernMaterial);
        if (material == null || material == Material.STONE) material = Compat.mat(color.legacyMaterial);
        ItemStack stack = new ItemStack(material == null ? Material.STONE : material, 1, color.data);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.color(color.code + color.name));
            List<String> lore = new ArrayList<>();
            if (selected) lore.add(plugin.color("&aCurrently selected"));
            lore.add(plugin.color("&eClick to select!"));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static class GroupColor {
        private final String name;
        private final String code;
        private final short data;
        private final String legacyMaterial;
        private final String modernMaterial;

        private GroupColor(String name, String code, int chatIndex, String legacyMaterial, int data, String modernMaterial) {
            this.name = name;
            this.code = code;
            this.data = (short) data;
            this.legacyMaterial = legacyMaterial;
            this.modernMaterial = modernMaterial;
        }
    }

    private ItemStack item(Material mat, String name, String... lore) {
        ItemStack stack = new ItemStack(mat == null ? Material.STONE : mat);
        return item(stack, name, lore);
    }

    private ItemStack item(ItemStack stack, String name, String... lore) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.color(name));
            List<String> colored = new ArrayList<>();
            for (String line : lore) colored.add(plugin.color(line));
            meta.setLore(colored);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
