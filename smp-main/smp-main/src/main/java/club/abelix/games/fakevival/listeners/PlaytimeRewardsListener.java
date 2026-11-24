package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.commands.PlaytimeRewardsCommand;
import club.abelix.games.fakevival.managers.PlaytimeRewardManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class PlaytimeRewardsListener implements Listener {
    private final PlaytimeRewardManager manager;
    private final PlaytimeRewardsCommand command;

    public PlaytimeRewardsListener(PlaytimeRewardManager manager, PlaytimeRewardsCommand command) {
        this.manager = manager;
        this.command = command;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.equalsIgnoreCase("playtime-rewards")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getItemMeta() == null || clicked.getItemMeta().getLore() == null) return;
        for (String line : clicked.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (!clean.startsWith("Playtime Day: ")) continue;
            int day;
            try {
                day = Integer.parseInt(clean.substring("Playtime Day: ".length()).trim());
            } catch (NumberFormatException ex) {
                return;
            }
            manager.claim(player, day);
            command.open(player);
            return;
        }
    }
}
