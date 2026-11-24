package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.EconomyAuditManager;
import club.abelix.games.fakevival.managers.EconomyManager;
import club.abelix.games.fakevival.managers.ItemRarityManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemRarityListener implements Listener {
    private final JavaPlugin plugin;
    private final ItemRarityManager itemRarityManager;
    private final EconomyManager economyManager;
    private final EconomyAuditManager auditManager;

    public ItemRarityListener(JavaPlugin plugin, ItemRarityManager itemRarityManager, EconomyManager economyManager, EconomyAuditManager auditManager) {
        this.plugin = plugin;
        this.itemRarityManager = itemRarityManager;
        this.economyManager = economyManager;
        this.auditManager = auditManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        for (Item item : event.getItems()) applyDroppedItem(item);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        for (ItemStack drop : event.getDrops()) itemRarityManager.applyIfMissing(drop);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> applyDroppedItem(event.getItem()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (economyManager.isEcoFrozen(event.getPlayer()) && itemRarityManager.isRareOrBetter(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CC.color("&cYour economy is frozen. You cannot drop rare items."));
            return;
        }
        if (auditManager != null && itemRarityManager.isRareOrBetter(event.getItemDrop().getItemStack())) {
            auditManager.logItem(event.getPlayer().getUniqueId(), "rare-drop", event.getItemDrop().getItemStack(), "Dropped by player");
        }
        applyDroppedItem(event.getItemDrop());
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraftPreview(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null) return;
        result = result.clone();
        itemRarityManager.applyIfMissing(result);
        event.getInventory().setResult(result);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSmelt(FurnaceSmeltEvent event) {
        ItemStack result = event.getResult();
        itemRarityManager.applyIfMissing(result);
        event.setResult(result);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item item)) return;
        applyDroppedItem(item);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Bukkit.getScheduler().runTask(plugin, () -> applyPlayerInventory(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("item-rarity.apply-to-existing-items-on-join", true)) return;
        Bukkit.getScheduler().runTask(plugin, () -> applyPlayerInventory(event.getPlayer()));
    }

    private void applyDroppedItem(Item item) {
        ItemStack stack = item.getItemStack();
        itemRarityManager.applyIfMissing(stack);
        item.setItemStack(stack);
    }

    private void applyPlayerInventory(Player player) {
        if (!player.isOnline()) return;
        ItemStack[] storage = player.getInventory().getStorageContents();
        itemRarityManager.applyInventory(storage);
        player.getInventory().setStorageContents(storage);

        ItemStack[] armor = player.getInventory().getArmorContents();
        itemRarityManager.applyInventory(armor);
        player.getInventory().setArmorContents(armor);

        ItemStack offhand = player.getInventory().getItemInOffHand();
        itemRarityManager.applyIfMissing(offhand);
        player.getInventory().setItemInOffHand(offhand);
    }
}
