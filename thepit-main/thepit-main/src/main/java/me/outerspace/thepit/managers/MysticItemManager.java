package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.util.SecureItemTag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MysticItemManager implements Listener {
    private final ThePitPlugin plugin;
    private final Random random = new Random();
    private final Set<String> processedDeaths = ConcurrentHashMap.newKeySet();

    public MysticItemManager(ThePitPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim) || !killer.isOnline()) return;
        String deathKey = victim.getUniqueId() + ":" + victim.getTicksLived() + ":" + System.currentTimeMillis() / 50L;
        if (!processedDeaths.add(deathKey)) return;
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> processedDeaths.remove(deathKey), 40L);

        int mysticism = plugin.getRenownShopManager() == null ? 0
                : plugin.getRenownShopManager().tier(killer.getUniqueId(), "mysticism");
        if (mysticism < 1) return;
        double baseChance = plugin.registry().settings().getDouble("mystic-drops.base-chance", 0.003D);
        double perTier = plugin.registry().settings().getDouble("mystic-drops.chance-per-mysticism-tier", 0.001D);
        if (random.nextDouble() >= Math.max(0.0D, baseChance + perTier * mysticism)) return;

        ItemStack item;
        int roll = random.nextInt(mysticism >= 4 ? 3 : 2);
        if (roll == 0) item = createMysticSword(killer.getUniqueId());
        else if (roll == 1) item = createMysticBow(killer.getUniqueId());
        else item = createFreshPants(killer.getUniqueId(), randomStandardColor());
        giveSafely(killer, item);
        if (plugin.getProgressManager() != null) plugin.getProgressManager().recordProgress(killer,
                ProgressManager.ObjectiveType.MYSTIC_ITEM_FOUND, SecureItemTag.string(item, "pit_item_type"), 1,
                new ProgressManager.ProgressContext("mystic_drop", deathKey, java.util.Collections.emptyMap()));
        killer.sendMessage(CC.color("&d&lMYSTIC DROP! &7You found " + item.getItemMeta().getDisplayName() + "&7."));
    }

    public ItemStack createMysticSword(UUID owner) {
        ItemStack item = new ItemStack(Material.GOLD_SWORD);
        item = base(item, "MYSTIC_SWORD", owner, "&eFresh Mystic Sword");
        return SecureItemTag.string(item, "mystic_item_type", "mystic_sword");
    }

    public ItemStack createMysticBow(UUID owner) {
        ItemStack item = new ItemStack(Material.BOW);
        item = base(item, "MYSTIC_BOW", owner, "&eFresh Mystic Bow");
        return SecureItemTag.string(item, "mystic_item_type", "mystic_bow");
    }

    public ItemStack createFreshPants(UUID owner, String color) {
        short data = switch (color.toUpperCase(Locale.ENGLISH)) {
            case "RED" -> 14; case "ORANGE" -> 1; case "YELLOW" -> 4; case "GREEN" -> 5; default -> 11;
        };
        ItemStack item = new ItemStack(Material.LEATHER_LEGGINGS, 1, data);
        item = base(item, "MYSTIC_PANTS", owner, "&eFresh " + title(color) + " Pants");
        item = SecureItemTag.string(item, "mystic_item_type", "fresh_pants");
        item = SecureItemTag.string(item, "mystic_pants_type", "STANDARD");
        item = SecureItemTag.string(item, "mystic_pants_color", color.toUpperCase(Locale.ENGLISH));
        return item;
    }

    private ItemStack base(ItemStack item, String type, UUID owner, String name) {
        int lives = Math.max(1, plugin.registry().settings().getInt("mystic-items.default-lives", 5));
        item = SecureItemTag.string(item, "ThePitItem", type.toLowerCase(Locale.ENGLISH));
        item = SecureItemTag.string(item, "pit_item_type", type);
        item = SecureItemTag.integer(item, "mystic_tier", 0);
        item = SecureItemTag.string(item, "mystic_enchants", "");
        item = SecureItemTag.integer(item, "mystic_tokens", 0);
        item = SecureItemTag.integer(item, "mystic_lives", lives);
        item = SecureItemTag.integer(item, "mystic_max_lives", lives);
        item = SecureItemTag.string(item, "mystic_owner", owner == null ? "" : owner.toString());
        item = SecureItemTag.unbreakable(item);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        meta.setLore(Arrays.asList(CC.color("&7Fresh"), CC.color("&7Lives: &a" + lives + "&7/&a" + lives)));
        item.setItemMeta(meta);
        return item;
    }

    private void giveSafely(Player player, ItemStack item) {
        java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty() && plugin.getPitCommandManager() != null) for (ItemStack stack : leftover.values()) plugin.getPitCommandManager().addToStash(player.getUniqueId(), stack);
        else if (!leftover.isEmpty()) for (ItemStack stack : leftover.values()) plugin.getLogger().severe("Mystic reward could not be stashed for " + player.getUniqueId() + ": " + stack.getType() + " x" + stack.getAmount());
    }

    private String randomStandardColor() {
        String[] values = {"RED", "ORANGE", "YELLOW", "GREEN", "BLUE"};
        return values[random.nextInt(values.length)];
    }
    private String title(String value) { return value.substring(0, 1).toUpperCase(Locale.ENGLISH) + value.substring(1).toLowerCase(Locale.ENGLISH); }
}
