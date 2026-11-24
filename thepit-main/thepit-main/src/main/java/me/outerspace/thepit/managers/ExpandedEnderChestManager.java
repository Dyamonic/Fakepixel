package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.util.ItemStackCodec;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ExpandedEnderChestManager implements Listener {
    private final ThePitPlugin plugin;
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();

    public ExpandedEnderChestManager(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (player == null || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())) {
            if (player != null) player.sendMessage(CC.color("&cYour profile is still loading."));
            return;
        }
        int tier = plugin.getRenownShopManager().tier(player.getUniqueId(), "inventory-management");
        if (tier <= 0) {
            player.openInventory(player.getEnderChest());
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!loading.add(uuid)) {
            player.sendMessage(CC.color("&eYour Ender Chest is already loading."));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String encoded;
            if (plugin.getStorageManager().isPersistentStorageAvailable()) {
                Optional<String> stored = plugin.getStorageManager().getOverride("renown-ender-chest", uuid.toString());
                encoded = stored.orElse("");
            } else {
                encoded = plugin.getStatsManager().getProfileExtension(uuid, "renown-ender-chest");
            }
            ItemStack[] extras = ItemStackCodec.decode(encoded);
            Bukkit.getScheduler().runTask(plugin, () -> finishOpen(player, tier, extras));
        });
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Holder) player.closeInventory();
        }
        loading.clear();
    }

    private void finishOpen(Player player, int requestedTier, ItemStack[] extras) {
        loading.remove(player.getUniqueId());
        if (!player.isOnline()) return;
        int currentTier = plugin.getRenownShopManager().tier(player.getUniqueId(), "inventory-management");
        int tier = Math.min(requestedTier, currentTier);
        if (tier <= 0) {
            player.openInventory(player.getEnderChest());
            return;
        }
        int slotsPerTier = Math.max(1, plugin.getRenownShopManager().effect("inventory-management", "slots-per-tier", 9));
        int size = Math.max(27, Math.min(54, 27 + slotsPerTier * tier));
        size -= size % 9;
        Holder holder = new Holder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, size,
                CC.color(plugin.registry().settings().getString("renown-shop.expanded-ender-chest.title", "&8Ender Chest")));
        holder.inventory = inventory;
        ItemStack[] base = player.getEnderChest().getContents();
        for (int index = 0; index < Math.min(27, base.length); index++) {
            if (base[index] != null) inventory.setItem(index, base[index].clone());
        }
        for (int index = 27; index < size && index - 27 < extras.length; index++) {
            ItemStack stack = extras[index - 27];
            if (stack != null) inventory.setItem(index, stack.clone());
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)
                || !(event.getPlayer() instanceof Player player)
                || !holder.owner.equals(player.getUniqueId())) return;
        ItemStack[] contents = event.getInventory().getContents();
        ItemStack[] base = Arrays.copyOfRange(contents, 0, Math.min(27, contents.length));
        if (base.length < 27) base = Arrays.copyOf(base, 27);
        player.getEnderChest().setContents(base);
        ItemStack[] extras = contents.length <= 27 ? new ItemStack[0] : Arrays.copyOfRange(contents, 27, contents.length);
        String encoded = ItemStackCodec.encode(extras);
        UUID uuid = player.getUniqueId();
        Runnable save = () -> {
            boolean saved = plugin.getStorageManager().isPersistentStorageAvailable()
                    ? plugin.getStorageManager().putOverride("renown-ender-chest", uuid.toString(), encoded)
                    : plugin.getStatsManager().setProfileExtension(uuid, "renown-ender-chest", encoded);
            if (!saved && player.isOnline() && plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin,
                    () -> player.sendMessage(CC.color("&cYour expanded Ender Chest could not be saved.")));
        };
        if (plugin.isEnabled()) Bukkit.getScheduler().runTaskAsynchronously(plugin, save);
        else save.run();
    }

    private static final class Holder implements InventoryHolder {
        private final UUID owner;
        private Inventory inventory;

        private Holder(UUID owner) {
            this.owner = owner;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }
}
