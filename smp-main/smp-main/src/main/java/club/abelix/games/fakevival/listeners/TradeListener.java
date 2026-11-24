package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.TradeManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class TradeListener implements Listener {
    private final TradeManager tradeManager;

    public TradeListener(TradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        event.setCancelled(true);
        if (!player.hasPermission("fakevival.command.trade")) {
            player.sendMessage(CC.color("&cYou do not have permission to trade."));
            return;
        }
        if (!tradeManager.requestTrade(player, target)) {
            player.sendMessage(CC.color("&cCould not send that trade request."));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type))) return;
        if (!type.startsWith("trade:")) return;
        tradeManager.handleClick(event, type);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type))) return;
        if (!type.startsWith("trade:")) return;
        tradeManager.handleDrag(event, type);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type))) return;
        if (!type.startsWith("trade:")) return;
        if (event.getPlayer() instanceof Player player) tradeManager.handleClose(player, type);
    }
}
