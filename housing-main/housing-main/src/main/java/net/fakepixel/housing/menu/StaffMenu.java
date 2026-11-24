package net.fakepixel.housing.menu;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class StaffMenu implements Listener {
    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final String title = ChatColor.DARK_GRAY + "Housing Staff Menu";
    private final Set<UUID> spyEnabled = new HashSet<>();

    public StaffMenu(HousingPlugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
    }

    public void open(Player player) {
        if (!player.hasPermission("housing.staff")) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, title);
        inv.setItem(2, item(net.fakepixel.housing.util.Compat.mat("TNT"), "&aReset house", "&7Click here", "&7to reset the house", "&7where you are currently", "&7in.", "", "&cWARNING! THIS CAN", "&cNOT BE UNDONE!", "", "&7Requires &aMOD&7!"));
        inv.setItem(4, item(net.fakepixel.housing.util.Compat.mat("FEATHER"), "&aFly", "&7Click to toggle!"));
        inv.setItem(6, item(net.fakepixel.housing.util.Compat.mat("COMPASS"), "&aSpy", "&7Click to toggle!"));
        inv.setItem(8, item(net.fakepixel.housing.util.Compat.mat("SPONGE"), "&aDry", "&7Removes water around you", "&7in a 4 block radius.", "&eAlias: &f/removewater"));
        inv.setItem(10, item(net.fakepixel.housing.util.Compat.mat("RED_WOOL"), "&aBypass Plotborder", "&7Allows you to bypass the", "&7plotborder.", "&eAlias: &f/bypassborder", "", "&cRequires MOD!"));
        inv.setItem(13, item(net.fakepixel.housing.util.Compat.mat("COOKIE"), "&aHousing award", "&7Give this house the &bHousing Award&7!", "&7Contains:", "&f- &e" + getAdminCookieAmount() + " Cookies", "&f- &ea message in chat", "", "&cRequires ADMIN!"));
        inv.setItem(16, item(net.fakepixel.housing.util.Compat.mat("STICK"), "&aKnockback Stick", "&7Staff utility item.", "&7Click to receive one."));
        inv.setItem(22, item(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cClose", "&7Close this menu."));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        event.getView().getTitle();
        if (!event.getView().getTitle().equals(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == net.fakepixel.housing.util.Compat.mat("AIR")) return;
        House house = houses.getHouseByWorld(player.getWorld().getName());

        if (event.getSlot() == 22) { player.closeInventory(); return; }

        if (event.getSlot() == 2) {
            if (check(player, "housing.staff.reset")) return;
            if (house == null) { player.sendMessage(plugin.color("&cYou are not inside a player house.")); return; }
            player.closeInventory();
            houses.resetHouse(player, house);
            return;
        }

        if (event.getSlot() == 4) {
            if (check(player, "housing.staff.fly")) return;
            boolean now = !player.getAllowFlight();
            player.setAllowFlight(now);
            player.setFlying(now);
            player.sendMessage(plugin.color("&7Housing fly: " + (now ? "&aEnabled" : "&cDisabled") + "&7."));
            return;
        }

        if (event.getSlot() == 6) {
            if (check(player, "housing.staff.spy")) return;
            if (spyEnabled.contains(player.getUniqueId())) {
                spyEnabled.remove(player.getUniqueId());
                player.sendMessage(plugin.color("&7Housing spy: &cDisabled&7."));
            } else {
                spyEnabled.add(player.getUniqueId());
                player.sendMessage(plugin.color("&7Housing spy: &aEnabled&7."));
            }
            return;
        }

        if (event.getSlot() == 8) {
            if (check(player, "housing.staff.dry")) return;
            int removed = dryWater(player);
            player.sendMessage(plugin.color("&7Removed &b" + removed + " &7water blocks around you."));
            return;
        }

        if (event.getSlot() == 10) {
            if (check(player, "housing.staff.bypassborder")) return;
            player.sendMessage(plugin.color(plugin.toggleBorderBypass(player) ? "&7Plotborder bypass: &aEnabled&7." : "&7Plotborder bypass: &cDisabled&7."));
            return;
        }

        if (event.getSlot() == 13) {
            if (check(player, "housing.staff.award")) return;
            if (house == null) { player.sendMessage(plugin.color("&cYou are not inside a player house.")); return; }
            houses.giveAdminCookies(player, house);
            return;
        }

        if (event.getSlot() == 16) {
            if (check(player, "housing.staff")) return;
            ItemStack stick = item(net.fakepixel.housing.util.Compat.mat("STICK"), "&aKnockback Stick", "&7Housing staff utility.");
            player.getInventory().addItem(stick);
            player.sendMessage(plugin.color("&aGiven staff utility stick."));
        }
    }

    public boolean isSpyEnabled(Player player) { return spyEnabled.contains(player.getUniqueId()); }
    public boolean isBorderBypassEnabled(Player player) { return plugin.isBorderBypass(player); }

    private boolean check(Player player, String permission) {
        if (player.hasPermission(permission) || player.hasPermission("housing.staff.admin") || player.hasPermission("housing.admin")) return false;
        player.sendMessage(plugin.msg("no-permission"));
        return true;
    }

    private int dryWater(Player player) {
        int radius = plugin.getStaticConfig().getInt("staff.dry-radius", 4);
        int removed = 0;
        World world = player.getWorld();
        int bx = player.getLocation().getBlockX();
        int by = player.getLocation().getBlockY();
        int bz = player.getLocation().getBlockZ();
        for (int x = bx - radius; x <= bx + radius; x++) {
            for (int y = by - radius; y <= by + radius; y++) {
                for (int z = bz - radius; z <= bz + radius; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == net.fakepixel.housing.util.Compat.mat("WATER")) {
                        block.setType(net.fakepixel.housing.util.Compat.mat("AIR"));
                        removed++;
                    }
                }
            }
        }
        return removed;
    }

    private int getAdminCookieAmount() {
        return plugin.getStaticConfig().getInt("staff.admin-cookie-amount", 50);
    }

    private ItemStack item(Material mat, String name, String... lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(plugin.color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(plugin.color(line));
        meta.setLore(colored);
        stack.setItemMeta(meta);
        return stack;
    }
}
