package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.DungeonManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class DungeonPortalWandListener implements Listener {
    private final DungeonManager dungeonManager;
    private final NamespacedKey key;

    public DungeonPortalWandListener(JavaPlugin plugin, DungeonManager dungeonManager) {
        this.dungeonManager = dungeonManager;
        this.key = new NamespacedKey(plugin, "dungeon_portal_wand");
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;
        if (!player.hasPermission("fakevival.admin.dungeon")) return;
        if (event.getClickedBlock() == null) return;
        event.setCancelled(true);
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            dungeonManager.setSpawnPortalPos("pos1", event.getClickedBlock().getLocation());
            player.sendMessage(CC.color("&aSet spawn dungeon portal &fpos1&a."));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            dungeonManager.setSpawnPortalPos("pos2", event.getClickedBlock().getLocation());
            player.sendMessage(CC.color("&aSet spawn dungeon portal &fpos2&a."));
        }
    }
}
