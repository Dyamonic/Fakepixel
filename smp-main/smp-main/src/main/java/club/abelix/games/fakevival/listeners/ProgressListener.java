package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.JobManager;
import club.abelix.games.fakevival.managers.SkillManager;
import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.EnumSet;
import java.util.Set;

public final class ProgressListener implements Listener {
    private final JavaPlugin plugin;
    private final SkillManager skillManager;
    private final JobManager jobManager;

    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE
    );

    private static final Set<Material> STONE = EnumSet.of(
            Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE,
            Material.ANDESITE, Material.DIORITE, Material.GRANITE, Material.TUFF, Material.CALCITE,
            Material.BASALT, Material.SMOOTH_BASALT, Material.BLACKSTONE, Material.NETHERRACK, Material.END_STONE
    );

    private static final Set<Material> LOGS = EnumSet.of(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.CRIMSON_STEM, Material.WARPED_STEM,
            Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD, Material.JUNGLE_WOOD,
            Material.ACACIA_WOOD, Material.DARK_OAK_WOOD, Material.MANGROVE_WOOD, Material.CHERRY_WOOD,
            Material.CRIMSON_HYPHAE, Material.WARPED_HYPHAE,
            Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_JUNGLE_LOG,
            Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG, Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG,
            Material.STRIPPED_CRIMSON_STEM, Material.STRIPPED_WARPED_STEM,
            Material.STRIPPED_OAK_WOOD, Material.STRIPPED_SPRUCE_WOOD, Material.STRIPPED_BIRCH_WOOD, Material.STRIPPED_JUNGLE_WOOD,
            Material.STRIPPED_ACACIA_WOOD, Material.STRIPPED_DARK_OAK_WOOD, Material.STRIPPED_MANGROVE_WOOD, Material.STRIPPED_CHERRY_WOOD,
            Material.STRIPPED_CRIMSON_HYPHAE, Material.STRIPPED_WARPED_HYPHAE
    );

    private static final Set<Material> DIGGABLE = EnumSet.of(
            Material.DIRT, Material.GRASS_BLOCK, Material.SAND, Material.RED_SAND, Material.GRAVEL,
            Material.CLAY, Material.SOUL_SAND, Material.SOUL_SOIL, Material.MUD
    );

    private static final Set<Material> CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART
    );

    public ProgressListener(JavaPlugin plugin, SkillManager skillManager, JobManager jobManager) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.jobManager = jobManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();
        if (type == Material.SUGAR_CANE) {
            if (player.getGameMode() == GameMode.SURVIVAL
                    && block.getWorld().getName().equalsIgnoreCase("world")) {
                skillManager.addXp(player, "farming", 2);
                addSpecificJobXp(player, "farmer");
                if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("debug", false)) {
                    plugin.getLogger().info("[SMP] Farming XP: player=" + player.getName()
                            + ", block=SUGAR_CANE, world=" + block.getWorld().getName() + ", xp=2");
                }
            }
            return;
        }
        String skill = getSkillForBlock(block);
        if (skill != null) skillManager.addXp(player, skill, getSkillXp(skill, defaultSkillXp(skill)));

        if (ORES.contains(type) || STONE.contains(type)) addSpecificJobXp(player, "miner");
        if (LOGS.contains(type)) addSpecificJobXp(player, "woodcutter");
        if (LOGS.contains(type)) addSpecificJobXp(player, "lumberjack");
        if (CROPS.contains(type) && isFullyGrown(block)) addSpecificJobXp(player, "farmer");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        addSpecificJobXp(event.getPlayer(), "builder");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        skillManager.addXp(killer, "combat", entity.getType() == EntityType.PLAYER ? 18 : getSkillXp("combat", 6));
        addSpecificJobXp(killer, "hunter");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombatStats(EntityDamageByEntityEvent event) {
        Player attacker = getAttackingPlayer(event);
        if (attacker != null) event.setDamage(skillManager.applyOutgoingDamage(attacker, event.getDamage()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof org.bukkit.entity.Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (event.getFinalDamage() <= 0.0) return;
        skillManager.addXp(player, "archery", getSkillXp("archery", 5));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnarmedHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (!player.getInventory().getItemInMainHand().getType().isAir()) return;
        if (event.getFinalDamage() <= 0.0) return;
        skillManager.addXp(player, "unarmed", getSkillXp("unarmed", 5));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> skillManager.applyPassiveStats(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || event.getFinalDamage() < 2.0) return;
        skillManager.addXp(player, "acrobatics", getSkillXp("acrobatics", 6));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        skillManager.addXp(player, "fishing", getSkillXp("fishing", 10));
        addSpecificJobXp(player, "fisher");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent event) {
        skillManager.addXp(event.getPlayer(), "smelting", Math.max(1, event.getItemAmount()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        skillManager.addXp(event.getEnchanter(), "enchanting", getSkillXp("enchanting", 8));
        addSpecificJobXp(event.getEnchanter(), "enchanter");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (!(event.getBlock().getState() instanceof org.bukkit.block.BrewingStand stand)) return;
        stand.getInventory().getHolder();
        // Bukkit's BrewEvent does not expose the player, so this skill is config-ready but not awarded here.
    }

    private void addJobXp(Player player, String action) {
        for (String job : jobManager.getJobs(player)) {
            if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("jobs." + job + ".actions").contains(action)) {
                jobManager.addXp(player, job, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("jobs." + job + ".xp", 5));
            }
        }
    }

    private void addSpecificJobXp(Player player, String job) {
        if (!jobManager.hasJob(player, job)) return;
        jobManager.addXp(player, job, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("jobs." + job + ".xp", 5));
    }

    private String getSkillForBlock(Block block) {
        Material type = block.getType();
        if (ORES.contains(type)) return "mining";
        if (STONE.contains(type)) return "mining";
        if (LOGS.contains(type)) return "woodcutting";
        if (DIGGABLE.contains(type)) return "excavation";
        if (CROPS.contains(type) && isFullyGrown(block)) return "farming";
        return null;
    }

    private boolean isFullyGrown(Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) return true;
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private int getSkillXp(String skill, int def) {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.xp-per-action." + skill, def);
    }

    private int defaultSkillXp(String skill) {
        return switch (skill) {
            case "woodcutting" -> 7;
            case "excavation" -> 3;
            case "farming" -> 4;
            default -> 5;
        };
    }

    private Player getAttackingPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }
}
