package club.abelix.games.fakevival.abilities;

import club.abelix.games.fakevival.mmo.MMOStatType;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ToolAbilityExecutor {
    private static final Set<Material> ORES = Set.of(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE, Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS);
    private static final Set<Material> TREASURE = Set.of(Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
            Material.ENDER_CHEST, Material.SUSPICIOUS_SAND, Material.SUSPICIOUS_GRAVEL);
    private static final Set<Material> SOFT = Set.of(Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT,
            Material.ROOTED_DIRT, Material.SAND, Material.RED_SAND, Material.GRAVEL, Material.CLAY, Material.SOUL_SAND,
            Material.SOUL_SOIL, Material.SNOW, Material.SNOW_BLOCK, Material.MUD);

    private ToolAbilityExecutor() {}

    static boolean prospect(AbilityContext c) {
        List<Block> ores = scan(c.player().getLocation().getBlock(), 12, 32, ORES);
        if (ores.isEmpty()) {
            c.player().sendMessage(CC.color("&cNo ores found nearby."));
            return true;
        }
        highlight(c, ores, 100L, Color.AQUA);
        c.player().sendMessage(CC.color("&aProspect revealed &e" + ores.size() + " &aore veins nearby!"));
        c.player().playSound(c.player(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1F, 1.2F);
        return true;
    }

    static boolean treasureSense(AbilityContext c) {
        Set<Material> types = new HashSet<>(ORES);
        types.addAll(TREASURE);
        List<Block> found = scan(c.player().getLocation().getBlock(), 10, 32, types);
        if (found.isEmpty()) {
            c.player().sendMessage(CC.color("&cNo treasure found nearby."));
            return true;
        }
        highlight(c, found, 100L, Color.YELLOW);
        return true;
    }

    static boolean veinBurst(AbilityContext c) {
        Block target = c.player().getTargetBlockExact(6);
        if (target == null || !ORES.contains(target.getType())) return false;
        Material type = target.getType();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<Block> seen = new HashSet<>();
        queue.add(target);
        int broken = 0;
        while (!queue.isEmpty() && broken < 12) {
            Block block = queue.removeFirst();
            if (!seen.add(block) || block.getType() != type) continue;
            if (breakSafely(c, block)) broken++;
            for (int[] d : DIRECTIONS) queue.add(block.getRelative(d[0], d[1], d[2]));
        }
        return broken > 0;
    }

    static boolean stonePulse(AbilityContext c) { return breakPatch(c, STONE, false); }
    static boolean excavate(AbilityContext c) { return breakPatch(c, SOFT, true); }

    private static boolean breakPatch(AbilityContext c, Set<Material> allowed, boolean horizontal) {
        Block target = c.player().getTargetBlockExact(6);
        if (target == null) return false;
        int broken = 0;
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
            Block block = horizontal ? target.getRelative(a, 0, b) : target.getRelative(a, b, 0);
            if (allowed.contains(block.getType()) && !ORES.contains(block.getType()) && breakSafely(c, block)) broken++;
        }
        return broken > 0;
    }

    static boolean timberCleave(AbilityContext c) {
        Block target = c.player().getTargetBlockExact(6);
        if (target == null || !isLog(target.getType())) return false;
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<Block> seen = new HashSet<>();
        queue.add(target);
        int broken = 0;
        while (!queue.isEmpty() && broken < 24) {
            Block block = queue.removeFirst();
            if (!seen.add(block) || block.getY() < target.getY() || !isLog(block.getType())) continue;
            if (breakSafely(c, block)) broken++;
            for (int[] d : DIRECTIONS) queue.add(block.getRelative(d[0], d[1], d[2]));
        }
        return broken > 0;
    }

    static boolean leafStorm(AbilityContext c) {
        int broken = 0;
        Block center = c.player().getLocation().getBlock();
        outer: for (int x = -4; x <= 4; x++) for (int y = -4; y <= 4; y++) for (int z = -4; z <= 4; z++) {
            Block block = center.getRelative(x, y, z);
            if (block.getType().name().endsWith("_LEAVES") && breakSafely(c, block) && ++broken >= 32) break outer;
        }
        return broken > 0;
    }

    static boolean harvestSweep(AbilityContext c) {
        int harvested = 0;
        Block center = c.player().getLocation().getBlock();
        for (int x = -3; x <= 3; x++) for (int y = -1; y <= 1; y++) for (int z = -3; z <= 3; z++) {
            Block block = center.getRelative(x, y, z);
            if (!(block.getBlockData() instanceof Ageable crop) || crop.getAge() < crop.getMaximumAge() || !canModify(c, block)) continue;
            BlockBreakEvent event = new BlockBreakEvent(block, c.player());
            c.plugin().getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) continue;
            ItemStack tool = c.item();
            for (ItemStack drop : block.getDrops(tool, c.player())) block.getWorld().dropItemNaturally(block.getLocation(), drop);
            crop.setAge(0);
            block.setBlockData(crop, true);
            if (++harvested >= 32) return true;
        }
        return harvested > 0;
    }

    static boolean growthAura(AbilityContext c) {
        List<Block> crops = new ArrayList<>();
        Block center = c.player().getLocation().getBlock();
        for (int x = -4; x <= 4; x++) for (int y = -1; y <= 1; y++) for (int z = -4; z <= 4; z++) {
            Block block = center.getRelative(x, y, z);
            if (block.getBlockData() instanceof Ageable age && age.getAge() < age.getMaximumAge() && canModify(c, block)) crops.add(block);
        }
        java.util.Collections.shuffle(crops);
        int grown = 0;
        for (Block block : crops.subList(0, Math.min(16, crops.size()))) {
            Ageable age = (Ageable) block.getBlockData();
            age.setAge(age.getAge() + 1);
            block.setBlockData(age, true);
            grown++;
        }
        return grown > 0;
    }

    static boolean earthquake(AbilityContext c) {
        double toolDamage = c.plugin().getCustomItemManager().getMmoStats(c.item()).get(MMOStatType.DAMAGE);
        int mining = c.plugin().getSkillManager().getLevel(c.player(), "mining");
        double damage = Math.max(3.0, Math.min(20.0, 3.0 + toolDamage * 0.05 + mining * 0.15));
        boolean hit = affectMonsters(c, damage, false);
        if (hit) c.player().getWorld().spawnParticle(Particle.BLOCK, c.player().getLocation(), 35, 3, .3, 3, Material.DIRT.createBlockData());
        return hit;
    }

    static boolean rootSnare(AbilityContext c) { return affectMonsters(c, 3.0, true); }

    private static boolean affectMonsters(AbilityContext c, double damage, boolean snare) {
        boolean hit = false;
        for (LivingEntity entity : c.player().getLocation().getNearbyLivingEntities(5, e -> e instanceof Monster)) {
            entity.damage(damage, c.player());
            if (snare) entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
            else {
                Vector push = entity.getLocation().toVector().subtract(c.player().getLocation().toVector());
                if (push.lengthSquared() > 0) entity.setVelocity(push.normalize().multiply(.8).setY(.3));
            }
            hit = true;
        }
        return hit;
    }

    private static boolean breakSafely(AbilityContext c, Block block) {
        if (!canModify(c, block)) return false;
        BlockBreakEvent event = new BlockBreakEvent(block, c.player());
        c.plugin().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        return block.breakNaturally(c.item(), true);
    }

    private static boolean canModify(AbilityContext c, Block block) {
        if (c.plugin().getSpawnProtectionManager().isInside(block.getLocation()) && !c.plugin().getSpawnProtectionManager().canBypass(c.player())) return false;
        return c.plugin().getZoneManager() == null || c.plugin().getZoneManager().canBreak(c.player(), block);
    }

    private static List<Block> scan(Block center, int radius, int max, Set<Material> types) {
        List<Block> result = new ArrayList<>();
        outer: for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++) for (int z = -radius; z <= radius; z++) {
            if (x * x + y * y + z * z > radius * radius) continue;
            Block block = center.getRelative(x, y, z);
            if (types.contains(block.getType())) {
                result.add(block);
                if (result.size() >= max) break outer;
            }
        }
        return result;
    }

    private static void highlight(AbilityContext c, List<Block> blocks, long duration, Color color) {
        var player = c.player();
        var world = player.getWorld();
        new BukkitRunnable() {
            long elapsed;
            @Override public void run() {
                if (!player.isOnline() || player.isDead() || player.getWorld() != world || elapsed >= duration) { cancel(); return; }
                Particle.DustOptions dust = new Particle.DustOptions(color, .7F);
                for (Block block : blocks) {
                    if (!block.getChunk().isLoaded()) continue;
                    double x = block.getX(), y = block.getY(), z = block.getZ();
                    for (double[] p : OUTLINE) player.spawnParticle(Particle.DUST, x + p[0], y + p[1], z + p[2], 1, 0, 0, 0, 0, dust);
                }
                elapsed += 10;
            }
        }.runTaskTimer(c.plugin(), 0L, 10L);
    }

    private static boolean isLog(Material m) { return m.name().endsWith("_LOG") || m.name().endsWith("_STEM") || m.name().endsWith("_HYPHAE"); }
    private static final int[][] DIRECTIONS = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static final double[][] OUTLINE = {{.05,.05,.05},{.95,.05,.05},{.05,.95,.05},{.05,.05,.95},{.95,.95,.05},{.95,.05,.95},{.05,.95,.95},{.95,.95,.95}};
    private static final Set<Material> STONE = Set.of(Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE,
            Material.COBBLED_DEEPSLATE, Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.TUFF,
            Material.CALCITE, Material.NETHERRACK, Material.BLACKSTONE, Material.BASALT);
}
