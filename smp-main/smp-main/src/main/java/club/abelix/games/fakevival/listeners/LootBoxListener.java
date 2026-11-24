package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.LootBoxManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class LootBoxListener implements Listener {
    private final LootBoxManager lootBoxManager;

    public LootBoxListener(LootBoxManager lootBoxManager) {
        this.lootBoxManager = lootBoxManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (action == Action.RIGHT_CLICK_BLOCK && event.isCancelled()) return;
        if (lootBoxManager.open(event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand())) event.setCancelled(true);
    }
}
