package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.MenuManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class MainMenuItemListener implements Listener {
    private static final int MENU_SLOT = 8;

    private final JavaPlugin plugin;
    private final MenuManager menuManager;
    private final NamespacedKey key;

    public MainMenuItemListener(JavaPlugin plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
        this.key = new NamespacedKey(plugin, "main_menu_item");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> give(event.getPlayer()));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> give(event.getPlayer()));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isMenuItem);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isMenuItem(event.getItem())) return;
        event.setCancelled(true);
        menuManager.openMainMenu(event.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!isMenuItem(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
        give(event.getPlayer());
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!isMenuItem(event.getMainHandItem()) && !isMenuItem(event.getOffHandItem())) return;
        event.setCancelled(true);
        give(event.getPlayer());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getHotbarButton() == MENU_SLOT) {
            event.setCancelled(true);
            give(player);
            return;
        }
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        if (event.getSlot() == MENU_SLOT && event.getClickedInventory() == player.getInventory()) {
            event.setCancelled(true);
            give(player);
            return;
        }
        if (isMenuItem(cursor) || isMenuItem(current)) {
            event.setCancelled(true);
            give(player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isMenuItem(event.getOldCursor()) && event.getNewItems().values().stream().noneMatch(this::isMenuItem)) return;
        event.setCancelled(true);
        give(player);
    }

    public void give(Player player) {
        removeExtraMenuItems(player);
        player.getInventory().setItem(MENU_SLOT, createItem());
    }

    private void removeExtraMenuItems(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            if (slot == MENU_SLOT) continue;
            ItemStack item = player.getInventory().getItem(slot);
            if (isMenuItem(item)) player.getInventory().setItem(slot, null);
        }
    }

    private ItemStack createItem() {
        Material material = Material.matchMaterial(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("main-menu.item.material", "NETHER_STAR"));
        ItemStack item = new ItemStack(material == null ? Material.NETHER_STAR : material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("main-menu.item.name", "&aSurvival Menu")));
            meta.setLore(CC.colorList(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("main-menu.item.lore").isEmpty()
                    ? List.of(
                    "&7View your profile, skills,",
                    "&7collections, quests, dungeons,",
                    "&7storage, bank, and more.",
                    "",
                    "&eRight Click to open!")
                    : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("main-menu.item.lore")));
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isMenuItem(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getItemMeta() == null) return false;
        Byte value = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }
}
