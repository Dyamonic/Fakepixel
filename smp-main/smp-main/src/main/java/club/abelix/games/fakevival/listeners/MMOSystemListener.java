package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.managers.ZoneManager;
import club.abelix.games.fakevival.managers.CombatDebugManager;
import club.abelix.games.fakevival.mmo.MMODamageIndicatorService;
import club.abelix.games.fakevival.mmo.MMODamageResult;
import club.abelix.games.fakevival.mmo.MMODamageSourceTracker;
import club.abelix.games.fakevival.mmo.MMODamageSourceType;
import club.abelix.games.fakevival.mmo.MMOStatMap;
import club.abelix.games.fakevival.mmo.MMOStatService;
import club.abelix.games.fakevival.mmo.MMOStatType;
import club.abelix.games.fakevival.mmo.CombatBalance;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class MMOSystemListener implements Listener {
    private static final Set<Material> CROPS = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.COCOA,
            Material.MELON_STEM,
            Material.PUMPKIN_STEM,
            Material.SWEET_BERRY_BUSH,
            Material.TORCHFLOWER_CROP,
            Material.PITCHER_CROP
    );

    private final JavaPlugin plugin;
    private final CustomItemManager customItemManager;
    private final MMOStatService statService;
    private final MMODamageIndicatorService damageIndicatorService;
    private final ZoneManager zoneManager;
    private final club.abelix.games.fakevival.managers.PveManager pveManager;
    private final CombatDebugManager combatDebugManager;
    private final NamespacedKey projectileShooterKey;
    private final NamespacedKey projectileItemKey;
    private final NamespacedKey projectileDamageKey;
    private final NamespacedKey projectileStrengthKey;
    private final NamespacedKey projectileCritChanceKey;
    private final NamespacedKey projectileCritDamageKey;

    public MMOSystemListener(JavaPlugin plugin, CustomItemManager customItemManager, MMOStatService statService,
                             MMODamageIndicatorService damageIndicatorService, ZoneManager zoneManager,
                             club.abelix.games.fakevival.managers.PveManager pveManager, CombatDebugManager combatDebugManager) {
        this.plugin = plugin;
        this.customItemManager = customItemManager;
        this.statService = statService;
        this.damageIndicatorService = damageIndicatorService;
        this.zoneManager = zoneManager;
        this.pveManager = pveManager;
        this.combatDebugManager = combatDebugManager;
        this.projectileShooterKey = new NamespacedKey(plugin, "mmo_projectile_shooter");
        this.projectileItemKey = new NamespacedKey(plugin, "mmo_projectile_item");
        this.projectileDamageKey = new NamespacedKey(plugin, "mmo_projectile_damage");
        this.projectileStrengthKey = new NamespacedKey(plugin, "mmo_projectile_strength");
        this.projectileCritChanceKey = new NamespacedKey(plugin, "mmo_projectile_crit_chance");
        this.projectileCritDamageKey = new NamespacedKey(plugin, "mmo_projectile_crit_damage");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = attackingPlayer(event);
        if (attacker != null && event.getEntity() instanceof Player victim
                && !pveManager.canDamagePlayer(attacker, victim, event.getCause(), attacker.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            combatDebugManager.recordCancellation(event, "pvp rule");
            logCombatCancel("pvp rule", attacker, event.getEntity());
            return;
        }
        boolean dungeonMobTarget = event.getEntity() instanceof LivingEntity living && zoneManager.isZoneDungeonMob(living);
        boolean mmoDamage = false;
        boolean critical = false;
        double lifesteal = 0.0D;
        MMODamageSourceType sourceType = MMODamageSourceTracker.current();
        if (attacker != null && event.getEntity() instanceof LivingEntity) {
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            if (customItemManager.getItemId(weapon) != null && !customItemManager.meetsCachedItemRequirements(attacker, weapon, true)) {
                event.setCancelled(true);
                combatDebugManager.recordCancellation(event, "item requirements");
                logCombatCancel("item requirements", attacker, event.getEntity());
                return;
            }
            boolean customProjectile = event.getDamager() instanceof Projectile projectile
                    && projectile.getPersistentDataContainer().has(projectileItemKey, PersistentDataType.STRING);
            MMOStatMap stats = customProjectile ? projectileStats((Projectile) event.getDamager()) : statService.getTotalStats(attacker);
            boolean customMeleeWeapon = event.getDamager() instanceof Player && customItemManager.isCustomMeleeWeapon(weapon);
            boolean hasWeaponStats = hasMmo(weapon);
            if (customMeleeWeapon || hasWeaponStats || customProjectile || sourceType == MMODamageSourceType.ABILITY) {
                double baseDamage = customMeleeWeapon ? fullAttackDamage(attacker, event.getDamage()) : event.getDamage();
                double damage = baseDamage + stats.get(MMOStatType.DAMAGE);
                damage *= 1.0D + stats.get(MMOStatType.STRENGTH) / 100.0D;
                if (ThreadLocalRandom.current().nextDouble(100.0D) < stats.get(MMOStatType.CRIT_CHANCE)) {
                    critical = true;
                    damage *= 1.0D + stats.get(MMOStatType.CRIT_DAMAGE) / 100.0D;
                }
                lifesteal = stats.get(MMOStatType.LIFESTEAL) > 0.0D ? damage * (stats.get(MMOStatType.LIFESTEAL) / 100.0D) : 0.0D;
                event.setDamage(Math.max(0.0D, damage));
                combatDebugManager.recordCustomDamage(event, event.getDamage(), critical);
                if (lifesteal > 0.0D) {
                    if (dungeonMobTarget) statService.healMmoHealth(attacker, lifesteal);
                    else heal(attacker, lifesteal);
                }
                mmoDamage = true;
            }
        }

        if (event.getEntity() instanceof Player victim) {
            MMOStatMap stats = statService.getTotalStats(victim);
            double defense = stats.get(MMOStatType.DEFENSE);
            if (!(event.getDamager() instanceof Player) && plugin instanceof club.abelix.games.fakevival.Game game) {
                event.setDamage(game.getMayorManager().applyMayorModifier(victim,
                        club.abelix.games.fakevival.mayor.MayorModifierType.MOB_DAMAGE, event.getDamage()));
            }
            if (isDungeonMobDamage(event)) mmoDamage = true;
            if (defense > 0.0D || mmoDamage) {
                event.setDamage(CombatBalance.reduce(plugin, event.getDamage(), defense));
                mmoDamage = true;
            }
            if (mmoDamage) {
                double mmoDamageAmount = event.getDamage();
                statService.applyMmoDamage(victim, mmoDamageAmount);
                // Keep the vanilla event active for knockback, arrows, shields and hurt animation.
                // MMO health is authoritative, so prevent the visual vanilla health bar from
                // killing the player before that larger health pool reaches zero.
                event.setDamage(Math.min(mmoDamageAmount, Math.max(0.01D, victim.getHealth() - 0.01D)));
                plugin.getServer().getScheduler().runTask(plugin, () -> statService.refreshPlayerMMOStats(victim));
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> statService.refreshPlayerMMOStats(victim));
            }
        }
        if (mmoDamage && !dungeonMobTarget && event.getEntity() instanceof LivingEntity target) {
            MMODamageSourceType displayType = sourceType == MMODamageSourceType.ABILITY ? MMODamageSourceType.ABILITY : critical ? MMODamageSourceType.CRITICAL : MMODamageSourceType.MELEE;
            MMODamageIndicatorService.DisplayResult displayResult = damageIndicatorService.showDamage(target,
                    new MMODamageResult(event.getDamage(), critical, displayType, lifesteal));
            combatDebugManager.recordDamageCounter(event, displayResult);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        statService.removePlayer(event.getPlayer());
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!hasMmo(tool)) return;
        if (!customItemManager.meetsItemRequirements(player, tool, true)) {
            event.setCancelled(true);
            return;
        }
        MMOStatMap stats = statService.getTotalStats(player);
        Block block = event.getBlock();
        Material type = block.getType();

        double miningFortune = stats.get(MMOStatType.MINING_FORTUNE);
        if (miningFortune > 0.0D && !CROPS.contains(type)) {
            giveBonusDrops(player, block, tool, miningFortune);
        }

        double farmingFortune = stats.get(MMOStatType.FARMING_FORTUNE);
        if (farmingFortune > 0.0D && CROPS.contains(type) && isFullyGrown(block)) {
            giveBonusDrops(player, block, tool, farmingFortune);
        }

        double doubleDropChance = stats.get(MMOStatType.DOUBLE_DROP_CHANCE);
        if (doubleDropChance > 0.0D && CROPS.contains(type) && isFullyGrown(block)
                && ThreadLocalRandom.current().nextDouble(100.0D) < doubleDropChance) {
            giveBonusDrops(player, block, tool, 100.0D);
        }

        double replantChance = stats.get(MMOStatType.REPLANT_CHANCE);
        if (replantChance > 0.0D && CROPS.contains(type) && isFullyGrown(block)
                && ThreadLocalRandom.current().nextDouble(100.0D) < replantChance) {
            plugin.getServer().getScheduler().runTask(plugin, () -> replant(block, type));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item caught)) return;
        Player player = event.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();
        if (!hasMmo(rod)) return;
        if (!customItemManager.meetsItemRequirements(player, rod, true)) {
            event.setCancelled(true);
            return;
        }
        MMOStatMap stats = statService.getTotalStats(player);

        double treasureChance = stats.get(MMOStatType.TREASURE_CHANCE);
        if (treasureChance > 0.0D && ThreadLocalRandom.current().nextDouble(100.0D) < treasureChance) {
            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.PRISMARINE_CRYSTALS, 1));
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (customItemManager.isCustomGearItem(event.getItem())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (customItemManager.getItemId(bow) == null) return;
        if (!customItemManager.meetsItemRequirements(player, bow, true)) {
            event.setCancelled(true);
            return;
        }
        String itemId = customItemManager.getItemId(bow);
        MMOStatMap stats = statService.getTotalStats(player);
        event.getProjectile().getPersistentDataContainer().set(projectileShooterKey, PersistentDataType.STRING, player.getUniqueId().toString());
        event.getProjectile().getPersistentDataContainer().set(projectileItemKey, PersistentDataType.STRING, itemId);
        event.getProjectile().getPersistentDataContainer().set(projectileDamageKey, PersistentDataType.DOUBLE, safeStat(stats.get(MMOStatType.DAMAGE) + stats.get(MMOStatType.PROJECTILE_DAMAGE)));
        event.getProjectile().getPersistentDataContainer().set(projectileStrengthKey, PersistentDataType.DOUBLE, safeStat(stats.get(MMOStatType.STRENGTH)));
        event.getProjectile().getPersistentDataContainer().set(projectileCritChanceKey, PersistentDataType.DOUBLE, safeStat(stats.get(MMOStatType.CRIT_CHANCE)));
        event.getProjectile().getPersistentDataContainer().set(projectileCritDamageKey, PersistentDataType.DOUBLE, safeStat(stats.get(MMOStatType.CRIT_DAMAGE)));
    }

    private MMOStatMap projectileStats(Projectile projectile) {
        MMOStatMap stats = MMOStatMap.empty();
        stats.set(MMOStatType.DAMAGE, readProjectileStat(projectile, projectileDamageKey));
        stats.set(MMOStatType.STRENGTH, readProjectileStat(projectile, projectileStrengthKey));
        stats.set(MMOStatType.CRIT_CHANCE, readProjectileStat(projectile, projectileCritChanceKey));
        stats.set(MMOStatType.CRIT_DAMAGE, readProjectileStat(projectile, projectileCritDamageKey));
        return stats;
    }

    private double readProjectileStat(Projectile projectile, NamespacedKey key) {
        Double value = projectile.getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
        return safeStat(value == null ? 0.0D : value);
    }

    private double safeStat(double value) {
        return Double.isFinite(value) ? Math.clamp(value, 0.0D, 1_000_000_000.0D) : 0.0D;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshPlayer(player));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorSlotEquip(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getSlotType() != org.bukkit.event.inventory.InventoryType.SlotType.ARMOR) return;
        ItemStack incoming = event.getCursor();
        if ((incoming == null || incoming.getType().isAir()) && event.getHotbarButton() >= 0) {
            incoming = player.getInventory().getItem(event.getHotbarButton());
        }
        if (incoming == null || incoming.getType().isAir() || customItemManager.getItemId(incoming) == null) return;
        if (!customItemManager.meetsItemRequirements(player, incoming, true)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRightClickEquip(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!isArmorOrShield(item) || customItemManager.getItemId(item) == null) return;
        if (!customItemManager.meetsItemRequirements(event.getPlayer(), item, true)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshPlayer(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldItem(PlayerItemHeldEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> refreshPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            statService.resetMmoHealth(event.getPlayer());
            refreshPlayer(event.getPlayer());
        });
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> statService.refreshPlayerMMOStats(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player) {
            double healed = statService.healFromVanillaAmount(player, event.getAmount());
            if (healed > 0.0D) damageIndicatorService.showHealing(player, healed);
            plugin.getServer().getScheduler().runTask(plugin, () -> statService.refreshPlayerMMOStats(player));
        }
    }

    private void giveBonusDrops(Player player, Block block, ItemStack tool, double fortune) {
        int guaranteed = (int) Math.floor(fortune / 100.0D);
        double fractional = fortune % 100.0D;
        int extraRolls = guaranteed + (ThreadLocalRandom.current().nextDouble(100.0D) < fractional ? 1 : 0);
        if (extraRolls <= 0) return;
        for (ItemStack drop : block.getDrops(tool, player)) {
            if (drop == null || drop.getType().isAir()) continue;
            ItemStack extra = drop.clone();
            extra.setAmount(Math.max(1, drop.getAmount() * extraRolls));
            player.getWorld().dropItemNaturally(block.getLocation().add(0.5D, 0.5D, 0.5D), extra);
        }
    }

    private boolean hasMmo(ItemStack item) {
        return customItemManager.hasMmoStatsPdc(item);
    }

    private boolean isArmorOrShield(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String type = item.getType().name();
        return item.getType() == Material.SHIELD || type.endsWith("_HELMET") || type.endsWith("_CHESTPLATE")
                || type.endsWith("_LEGGINGS") || type.endsWith("_BOOTS");
    }

    private void refreshPlayer(Player player) {
        if (!player.isOnline()) return;
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (ItemStack item : storage) {
            customItemManager.upgradeMmoItem(item);
            customItemManager.rebuildLoreFor(player, item);
        }
        player.getInventory().setStorageContents(storage);

        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack item : armor) {
            customItemManager.upgradeMmoItem(item);
            customItemManager.rebuildLoreFor(player, item);
        }
        player.getInventory().setArmorContents(armor);

        ItemStack offhand = player.getInventory().getItemInOffHand();
        customItemManager.upgradeMmoItem(offhand);
        customItemManager.rebuildLoreFor(player, offhand);
        player.getInventory().setItemInOffHand(offhand);
        statService.refreshPlayerMMOStats(player);
    }

    private boolean isFullyGrown(Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) return true;
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private void replant(Block block, Material cropType) {
        if (!block.getType().isAir()) return;
        block.setType(cropType);
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(0);
            block.setBlockData(ageable);
        }
    }

    private void heal(Player player, double amount) {
        double healed = statService.healMmoHealth(player, amount);
        if (healed > 0.0D) damageIndicatorService.showHealing(player, healed);
    }

    private Player attackingPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }

    private boolean isDungeonMobDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity living) return zoneManager.isZoneDungeonMob(living);
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living) {
            return zoneManager.isZoneDungeonMob(living);
        }
        return false;
    }

    private double fullAttackDamage(Player attacker, double fallback) {
        AttributeInstance attribute = attacker.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attribute == null) return fallback;
        return Math.max(fallback, attribute.getValue());
    }

    private void logCombatCancel(String reason, Player player, org.bukkit.entity.Entity mob) {
        plugin.getLogger().info("[Combat] Cancelled monster damage for " + reason + " player="
                + (player == null ? "unknown" : player.getName()) + " mob=" + (mob == null ? "unknown" : mob.getType()));
    }
}
