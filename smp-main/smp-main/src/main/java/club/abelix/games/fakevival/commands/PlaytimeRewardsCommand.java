package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.PlaytimeRewardManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class PlaytimeRewardsCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final PlaytimeRewardManager manager;

    public PlaytimeRewardsCommand(JavaPlugin plugin, PlaytimeRewardManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use playtime rewards.");
            return true;
        }
        open(player);
        return true;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("playtime-rewards"), 54, CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("playtime-rewards.menu.title", "&6Playtime Rewards")));
        inventory.setItem(4, item(Material.CLOCK, "&6Playtime Rewards", List.of(
                "&7Month: &f" + manager.monthKey(),
                "&7Reward Set: &f" + manager.activeSet(),
                "&7Monthly Playtime: &e" + format(manager.getPlaytimeSeconds(player)),
                "&7Unlocked Days: &a" + manager.getClaimableDays(player) + "&7/&a30",
                "",
                "&7Rewards reset each month.",
                "&7Next month can use a different reward set."
        )));

        int[] slots = rewardSlots();
        for (int day = 1; day <= 30; day++) {
            inventory.setItem(slots[day - 1], rewardItem(player, day));
        }
        fill(inventory);
        player.openInventory(inventory);
    }

    private ItemStack rewardItem(Player player, int day) {
        boolean claimed = manager.isClaimed(player, day);
        boolean claimable = manager.canClaim(player, day);
        boolean unlocked = manager.getClaimableDays(player) >= day;
        Material material = claimed ? Material.MINECART : claimable ? manager.rewardMaterial(day) : Material.GRAY_DYE;
        String status = claimed ? "&aClaimed" : claimable ? "&eClick to claim" : "&cLocked";
        List<String> lore = new ArrayList<>();
        lore.add("&7Required Playtime: &e" + day + " day(s)");
        lore.add("&7Status: " + status);
        lore.add("");
        lore.addAll(manager.rewardLore(day));
        lore.add("");
        lore.add(claimed ? "&aAlready claimed." : unlocked ? "&eClick to claim this reward." : "&cPlay more this month to unlock.");
        lore.add("&8Playtime Day: " + day);
        return item(material, (claimable ? "&a" : claimed ? "&7" : "&c") + "Day " + day + " Reward", lore);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            meta.setLore(CC.colorList(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, filler);
        }
    }

    private int[] rewardSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43,
                46, 47
        };
    }

    private String format(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }
}
