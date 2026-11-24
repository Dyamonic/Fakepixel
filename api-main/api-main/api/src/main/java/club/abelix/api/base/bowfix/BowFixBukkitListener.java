package club.abelix.api.base.bowfix;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BowFixBukkitListener implements Listener {

    BowFixListener bowFixListener;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        bowFixListener.clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        bowFixListener.clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getProjectile() instanceof Arrow)) return;

        Player player = (Player) event.getEntity();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        if (!bowFixListener.wasSlotSwitched(player)) return;

        bowFixListener.clearSlotSwitch(player);

        ItemStack bow = event.getBow();
        boolean infinite = bow != null && bow.containsEnchantment(Enchantment.ARROW_INFINITE);

        event.setCancelled(true);
        event.getProjectile().remove();

        if (!infinite) {
            giveArrow(player.getInventory());
        }
        player.updateInventory();
    }

    private void giveArrow(PlayerInventory inv) {
        ItemStack[] contents = inv.getContents();
        for (ItemStack item : contents) {
            if (item != null && item.getType() == Material.ARROW) {
                item.setAmount(item.getAmount() + 1);
                return;
            }
        }
        inv.addItem(new ItemStack(Material.ARROW, 1));
    }
}
