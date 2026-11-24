package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.config.defaults.CustomBowDefaults100;
import club.abelix.games.fakevival.managers.CustomEnchantManager;
import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.mmo.MMOStatMap;
import club.abelix.games.fakevival.mmo.MMOStatType;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ShortbowListener implements Listener {
    private final Game plugin;
    private final CustomItemManager items;
    private final CustomEnchantManager enchants;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final NamespacedKey smpProjectileKey;
    private final NamespacedKey projectileOwnerKey;
    private final NamespacedKey sourceItemKey;
    private final NamespacedKey sourceTypeKey;
    private final NamespacedKey damageKey;
    private final NamespacedKey strengthKey;
    private final NamespacedKey critChanceKey;
    private final NamespacedKey critDamageKey;
    private final NamespacedKey mmoShooterKey;
    private final NamespacedKey mmoItemKey;
    private final NamespacedKey mmoDamageKey;
    private final NamespacedKey mmoStrengthKey;
    private final NamespacedKey mmoCritChanceKey;
    private final NamespacedKey mmoCritDamageKey;

    public ShortbowListener(Game plugin, CustomItemManager items, CustomEnchantManager enchants) {
        this.plugin = plugin;
        this.items = items;
        this.enchants = enchants;
        this.smpProjectileKey = new NamespacedKey(plugin, "smp_projectile");
        this.projectileOwnerKey = new NamespacedKey(plugin, "projectile_owner");
        this.sourceItemKey = new NamespacedKey(plugin, "source_item_id");
        this.sourceTypeKey = new NamespacedKey(plugin, "source_item_type");
        this.damageKey = new NamespacedKey(plugin, "projectile_damage");
        this.strengthKey = new NamespacedKey(plugin, "projectile_strength");
        this.critChanceKey = new NamespacedKey(plugin, "projectile_crit_chance");
        this.critDamageKey = new NamespacedKey(plugin, "projectile_crit_damage");
        this.mmoShooterKey = new NamespacedKey(plugin, "mmo_projectile_shooter");
        this.mmoItemKey = new NamespacedKey(plugin, "mmo_projectile_item");
        this.mmoDamageKey = new NamespacedKey(plugin, "mmo_projectile_damage");
        this.mmoStrengthKey = new NamespacedKey(plugin, "mmo_projectile_strength");
        this.mmoCritChanceKey = new NamespacedKey(plugin, "mmo_projectile_crit_chance");
        this.mmoCritDamageKey = new NamespacedKey(plugin, "mmo_projectile_crit_damage");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack bow = player.getInventory().getItemInMainHand();
        String itemId = items.getItemId(bow);
        if (itemId == null || !CustomBowDefaults100.itemIds().contains(itemId) || !items.itemExists(itemId)
                || !"SHORTBOW".equals(items.getCustomItemType(bow)) || items.isAbilityBlockedItem(bow)) return;
        event.setCancelled(true);
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) return;
        if (!items.meetsItemRequirements(player, bow, true)) return;
        if (blocked(player)) {
            player.sendMessage(CC.color("&cYou cannot use this Shortbow here."));
            return;
        }
        long now = System.currentTimeMillis();
        long ready = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (ready > now) {
            player.sendMessage(CC.color("&cThis Shortbow is cooling down for " + formatTime(ready - now) + "."));
            return;
        }
        if (!hasFreeArrows(player, bow) && !consumeArrow(player)) {
            player.sendMessage(CC.color("&cYou need arrows to use this Shortbow."));
            return;
        }
        MMOStatMap stats = plugin.getMmoStatService().getTotalStats(player);
        Vector velocity = player.getEyeLocation().getDirection().normalize().multiply(3.2D);
        Arrow arrow = player.launchProjectile(Arrow.class, velocity);
        arrow.setShooter(player);
        arrow.setDamage(0.0D);
        arrow.setCritical(false);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        applyVanillaBowEffects(bow, arrow);
        tagProjectile(player, itemId, arrow, stats);
        enchants.trackBowProjectile(bow, arrow);
        cooldowns.put(player.getUniqueId(), now + fireRate(items.getCustomItemRarity(bow)));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
        player.getWorld().spawnParticle(Particle.CRIT, player.getEyeLocation().add(player.getEyeLocation().getDirection()), 4, 0.08D, 0.08D, 0.08D, 0.01D);
    }

    private void tagProjectile(Player player, String itemId, Arrow arrow, MMOStatMap stats) {
        double damage = safe(stats.get(MMOStatType.DAMAGE) + stats.get(MMOStatType.PROJECTILE_DAMAGE));
        double strength = safe(stats.get(MMOStatType.STRENGTH));
        double critChance = safe(stats.get(MMOStatType.CRIT_CHANCE));
        double critDamage = safe(stats.get(MMOStatType.CRIT_DAMAGE));
        PersistentDataContainer pdc = arrow.getPersistentDataContainer();
        pdc.set(smpProjectileKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(projectileOwnerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(sourceItemKey, PersistentDataType.STRING, itemId);
        pdc.set(sourceTypeKey, PersistentDataType.STRING, "SHORTBOW");
        pdc.set(damageKey, PersistentDataType.DOUBLE, damage);
        pdc.set(strengthKey, PersistentDataType.DOUBLE, strength);
        pdc.set(critChanceKey, PersistentDataType.DOUBLE, critChance);
        pdc.set(critDamageKey, PersistentDataType.DOUBLE, critDamage);
        pdc.set(mmoShooterKey, PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(mmoItemKey, PersistentDataType.STRING, itemId);
        pdc.set(mmoDamageKey, PersistentDataType.DOUBLE, damage);
        pdc.set(mmoStrengthKey, PersistentDataType.DOUBLE, strength);
        pdc.set(mmoCritChanceKey, PersistentDataType.DOUBLE, critChance);
        pdc.set(mmoCritDamageKey, PersistentDataType.DOUBLE, critDamage);
    }

    private boolean blocked(Player player) {
        return plugin.getNetworkService().isHub() || plugin.getSpawnProtectionManager().isInside(player.getLocation())
                || plugin.getZoneManager().isPvpBlocked(player.getLocation());
    }

    private boolean hasFreeArrows(Player player, ItemStack bow) {
        if (player.getGameMode().isInvulnerable()) return true;
        Enchantment infinity = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("infinity"));
        return infinity != null && bow.getEnchantmentLevel(infinity) > 0 || enchants.effectLevel(bow, "infinity") > 0
                || enchants.effectLevel(bow, "infinite_quiver") > 0;
    }

    private boolean consumeArrow(Player player) {
        for (int slot = 0; slot < player.getInventory().getStorageContents().length; slot++) {
            ItemStack arrow = player.getInventory().getItem(slot);
            if (arrow == null || arrow.getType() != Material.ARROW) continue;
            if (arrow.getAmount() <= 1) player.getInventory().setItem(slot, null);
            else {
                arrow = arrow.clone();
                arrow.setAmount(arrow.getAmount() - 1);
                player.getInventory().setItem(slot, arrow);
            }
            return true;
        }
        return false;
    }

    private void applyVanillaBowEffects(ItemStack bow, Arrow arrow) {
        Enchantment flame = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("flame"));
        Enchantment punch = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("punch"));
        if (flame != null && bow.getEnchantmentLevel(flame) > 0) arrow.setFireTicks(100);
        if (punch != null) arrow.setKnockbackStrength(Math.max(0, bow.getEnchantmentLevel(punch)));
    }

    private long fireRate(String rarity) {
        return switch (rarity == null ? "" : rarity.toUpperCase(Locale.ROOT)) {
            case "UNCOMMON" -> 600L;
            case "RARE" -> 500L;
            case "EPIC" -> 400L;
            case "LEGENDARY" -> 300L;
            case "MYTHIC" -> 250L;
            default -> 700L;
        };
    }

    private String formatTime(long millis) {
        return String.format(Locale.US, "%.2fs", Math.max(0L, millis) / 1000.0D);
    }

    private double safe(double value) {
        return Double.isFinite(value) ? Math.clamp(value, 0.0D, 1_000_000_000.0D) : 0.0D;
    }

    public String validation() {
        return "leftClick=true, mainHandOnly=true, registryPdc=true, arrowConsumption=true, infinity=true, rarityCooldowns=true, protections=true, centralDamagePdc=true, vanillaDamageNeutral=true";
    }
}
