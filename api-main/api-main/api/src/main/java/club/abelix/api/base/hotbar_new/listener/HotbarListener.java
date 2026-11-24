package club.abelix.api.base.hotbar_new.listener;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar_new.ActionType;
import club.abelix.api.base.hotbar_new.HotbarClickAction;
import club.abelix.api.base.hotbar_new.HotbarService;
import club.abelix.api.nms.sound.type.YSound;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class HotbarListener implements Listener {

    HotbarService hotbarService;

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        hotbarService.applyDefault(Gamer.getGamer(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        val action = e.getAction();

        if (action == Action.PHYSICAL) return;

        val player = e.getPlayer();
        val gamer = Gamer.getGamer(player);
        val current = hotbarService.getCurrent(gamer);

        if (current == null) return;

        val type = mapInteract(action, player.isSneaking());
        if (type == null) return;

        val slot = player.getInventory().getHeldItemSlot();

        current.getIcon(slot).ifPresent(icon -> {
            HotbarClickAction click = new HotbarClickAction(gamer, type);
            icon.onClick(click);
            gamer.playSound(YSound.CLICK);
        });

        e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getInventory().getType() != InventoryType.CRAFTING) return;
        if (e.getSlotType() != InventoryType.SlotType.QUICKBAR) return;

        val gamer = Gamer.getGamer(player);
        val current = hotbarService.getCurrent(gamer);

        if (current == null) return;

        val slot = e.getSlot();
        val type = mapInventoryClick(e.getClick());

        if (type == null) return;

        current.getIcon(slot).ifPresent(icon -> {
            HotbarClickAction click = new HotbarClickAction(gamer, type);
            icon.onClick(click);
            gamer.playSound(YSound.CLICK);
        });

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        hotbarService.clear(Gamer.getGamer(event.getPlayer()));
    }

    private ActionType mapInteract(Action action, boolean sneaking) {
        return switch (action) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> sneaking ? ActionType.SHIFT_LEFT_CLICK : ActionType.LEFT_CLICK;
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> sneaking ? ActionType.SHIFT_RIGHT_CLICK : ActionType.RIGHT_CLICK;
            default -> null;
        };
    }

    private ActionType mapInventoryClick(ClickType click) {
        return switch (click) {
            case LEFT -> ActionType.LEFT_CLICK;
            case RIGHT -> ActionType.RIGHT_CLICK;
            case SHIFT_LEFT -> ActionType.SHIFT_LEFT_CLICK;
            case SHIFT_RIGHT -> ActionType.SHIFT_RIGHT_CLICK;
            default -> null;
        };
    }
}
