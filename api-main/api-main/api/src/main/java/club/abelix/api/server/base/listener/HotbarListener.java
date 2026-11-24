package club.abelix.api.server.base.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerUnloadEvent;
import club.abelix.api.base.hotbar.Hotbar;
import club.abelix.api.base.hotbar.Icon;
import club.abelix.api.base.hotbar.manager.HotbarManager;
import club.abelix.api.base.hotbar_new.HotbarService;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameJoinEvent;
import club.abelix.di.api.annotation.Autowired;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.network.common.logging.Logging;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class HotbarListener extends YListener {

    @Autowired
    private HotbarService hotbarService;

    public HotbarListener(@NonNull AbstractPlugin plugin) {
        super(plugin);

        DiBukkitPlatform.getFactory().getBeanService().processInjection(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(GameJoinEvent event) {
        Game game = event.getGame();
        Gamer gamer = event.getGamer();

        String defaultHotbar = game.getDefaultHotbar();

        if (defaultHotbar == null) {
            hotbarService.clear(gamer);
        } else {
            hotbarService.apply(defaultHotbar, gamer);
        }
    }

    @EventHandler
    public void onPlayerQuit(GamerUnloadEvent event) {
        Logging.IMP.info("Clearing hotbar for " + event.getGamer().getPlayer().getName());
        HotbarManager.INSTANCE.clearPlayerHotbar(event.getGamer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String defaultHotbar = HotbarManager.INSTANCE.getDefaultHotbar();

        if (defaultHotbar == null) {
            return;
        }

        Gamer gamer = Gamer.getGamer(player);

        HotbarManager.INSTANCE.setHotbar(gamer, defaultHotbar);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Player player = event.getPlayer();

        Gamer gamer = Gamer.getGamer(player);

        if (gamer == null) {
            return;
        }

        Hotbar hotbar = HotbarManager.INSTANCE.getHotbar(gamer);

        if (hotbar == null) {
            return;
        }

        ItemStack itemStack = player.getItemInHand();

        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        Icon icon = hotbar.getIcons()[player.getInventory().getHeldItemSlot()];

        if (icon != null) {
            event.setCancelled(true);

            icon.getAction().accept(gamer);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        Gamer gamer = Gamer.getGamer(player);

        if (gamer == null) {
            return;
        }

        Hotbar hotbar = HotbarManager.INSTANCE.getHotbar(gamer);

        if (hotbar == null) {
            return;
        }

        int slot = event.getSlot();

        if (event.getSlotType() != InventoryType.SlotType.QUICKBAR) {
            return;
        }

        ItemStack itemStack = player.getItemInHand();

        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        Icon icon = hotbar.getIcons()[slot];

        if (icon != null) {
            event.setCancelled(true);

            icon.getAction().accept(gamer);
        }
    }

}
