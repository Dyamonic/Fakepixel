package club.abelix.api.base.box.animation.standard;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.box.animation.AbstractBoxOpenAnimation;
import club.abelix.api.base.box.animation.event.AnimationChoseEvent;
import club.abelix.api.base.box.animation.event.AnimationEndEvent;
import club.abelix.api.base.box.animation.protection.BoxOpenProtectionSphere;
import club.abelix.api.base.box.animation.state.BoxAnimationState;
import club.abelix.api.base.box.context.BoxOpenContext;
import club.abelix.api.base.box.reward.gameitem.GameItemReward;
import club.abelix.api.base.box.util.RngUtil;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.util.particle.ParticleEffect;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.core.bukkit.HeartBeat;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Log4j2
public class StandardBlockBasedBoxOpenAnimation extends AbstractBoxOpenAnimation {

    private static final int CIRCLE_RADIUS = 3;
    private static final int STEPS = 10;
    private static final long TICK_DELAY = 1L;

    private final Material boxMaterial;
    private final Map<Block, Hologram> holograms = new HashMap<>();
    private final List<Block> blocks = new ArrayList<>();
    private final List<BukkitTask> timers = new ArrayList<>();
    private volatile boolean stopped = false;
    private Location selected;
    private BoxOpenProtectionSphere sphere;
    private BukkitTask sphereTask;
    private BukkitTask rootTask;
    private BukkitTask particleTask;

    public StandardBlockBasedBoxOpenAnimation(
            BoxOpenContext context,
            Material material
    ) {
        super(context, context.getGamer());

        this.boxMaterial = material;
    }

    @Override
    public @NotNull String getName(@NotNull Language language) {
        return "RANKS";
    }

    @Override
    public void start(@NotNull Location location) {
        stopped = false;

        this.sphere = BoxOpenProtectionSphere.simple(
                location,
                7,
                getContext().getGamer().getPlayer(),
                location
        );
        this.sphereTask = sphere.runTaskTimer(FalconAPI.getInstance(), 0L, 1L);

        this.rootTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (stopped) return;
                spawnAnimatedChests(location);
            }
        }.runTask(FalconAPI.getInstance());
    }

    @Override
    public void cancel() {
        stopped = true;

        stopAllTimers();
        clearWorldArtifacts();
        clearContext();

        getContext().setState(BoxAnimationState.CANCELED);
    }

    @Override
    public void finish() {
        stopped = true;

        stopAllTimers();
        clearWorldArtifacts();
        clearContext();

        getContext().setState(BoxAnimationState.FINISHED);
    }

    @Override
    public void onInteractEvent(@NotNull Location location) {
        if (!blocks.contains(location.getBlock())) return;

        if (stopped) return;

        onSelect(location);

        getContext().setState(BoxAnimationState.REWARD_CHOSEN);
        HeartBeat.callEvent(new AnimationChoseEvent(getContext()));
    }

    @Override
    public void revealRewards() {
        holograms.forEach((block, hologram) -> {
            if (block.getLocation().equals(selected)) return;

            int luck = 50;

            if (ThreadLocalRandom.current().nextBoolean()) {
                luck += 30;
            }

            val randomReward = RngUtil.pickWithLuck(getContext().getBox(), luck);
            hologram.remove();

            hologram.addItemLine(randomReward.getRewardIcon(getGamer().getLanguage()));

            if (randomReward instanceof GameItemReward gameItemReward) {
                hologram.addTextLine(gameItemReward.getCategory().getDisplayName(getGamer().getLanguage()));
            }

            hologram.addTextLine(randomReward.getName(getGamer().getLanguage()));
            hologram.addTextLine(randomReward.getRarity().getName(getGamer().getLanguage()));
        });
    }

    @Override
    public void onSelect(@NotNull Location location) {
        if (stopped) return;

        launchFirework(location, 1);

        selected = location;

        getGamer().playSound(YSound.LEVEL_UP);

        val selectedRedwardHologram = holograms.get(selected.getBlock());

        if (selected == null) return;

        val reward = getContext().getReward();

        selectedRedwardHologram.remove();

        selectedRedwardHologram.addItemLine(reward.getRewardIcon(getGamer().getLanguage()));

        if (reward instanceof GameItemReward gameItemReward) {
            selectedRedwardHologram.addTextLine(gameItemReward.getCategory().getDisplayName(getGamer().getLanguage()));
        }

        selectedRedwardHologram.addTextLine(reward.getName(getGamer().getLanguage()));
        selectedRedwardHologram.addTextLine(reward.getRarity().getName(getGamer().getLanguage()));

        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                ParticleEffect.VILLAGER_HAPPY.display(0.1F, 50, selected.clone().add(0.5, 0.7, 0.5));
            }
        }.runTaskTimer(FalconAPI.getInstance(), 0, 10);

        selectedRedwardHologram.teleport(selected.clone().add(0.5, 1.5, 0.5));
    }

    private void spawnAnimatedChests(final Location center) {
        if (stopped) return;

        final World world = center.getWorld();
        final List<int[]> offsets = generateCircleOffsets(CIRCLE_RADIUS);

        animateNext(center, world, offsets, 0);
    }

    private void animateNext(final Location center,
                             final World world,
                             final List<int[]> offsets,
                             final int index) {
        if (stopped || index >= offsets.size()) return;

        final Location start = center.clone();
        final int[] off = offsets.get(index);
        final Location end = center.clone().add(off[0], 0, off[1]);

        final Vector delta = end.toVector().subtract(start.toVector());
        final Vector inc = delta.clone().multiply(1.0 / STEPS);
        final Location particleLoc = start.clone();

        BukkitTask t = new BukkitRunnable() {
            int step = 0;

            @Override
            @SneakyThrows
            public void run() {
                if (stopped) {
                    cancel();
                    return;
                }

                if (step++ >= STEPS) {
                    if (stopped) {
                        cancel();
                        return;
                    }

                    val block = world.getBlockAt(end);
                    block.setType(boxMaterial);
                    blocks.add(block);

                    createHologram(block);

                    if (getGamer() != null) {
                        getGamer().playSound(YSound.NOTE_PLING);
                    }

                    if (!stopped && index + 1 >= offsets.size()) {
                        try {
                            HeartBeat.callEvent(new AnimationEndEvent(getContext()));
                        } catch (Exception ex) {
                            log.warn("onFinish callback failed", ex);
                        }
                    }

                    // Следующий элемент
                    animateNext(center, world, offsets, index + 1);
                    cancel();
                    return;
                }

                particleLoc.add(inc);
                ParticleEffect.FIREWORKS_SPARK.display(0.1F, 2, particleLoc);
            }
        }.runTaskTimer(FalconAPI.getInstance(), 0L, TICK_DELAY);

        timers.add(t);
    }

    private void createHologram(Block block) {
        if (stopped) return;

        val hologramLocation = block.getLocation().clone().add(0.5, 1, 0.5);
        val hologram = new Hologram(hologramLocation);
        hologram.addItemLine(ItemUtil.getBuilder(Material.GOLD_BLOCK).build());
        hologram.addTextLine(Lang.of("box_open_animation_hologram_title").compile());
        hologram.setPublicStatus(true);

        ParticleEffect.HEART.display(0.5F, 15, hologramLocation);

        holograms.put(block, hologram);
    }

    public void launchFirework(final Location loc, final int blocks) {
        if (stopped) return;

        final World world = loc.getWorld();
        if (world == null) return;

        final double startY = loc.getY();

        val location = loc.clone().add(0.5, 0.5, 0.5);
        val fireworkEffect = org.bukkit.FireworkEffect.builder()
                .withColor(Color.fromRGB(255, 145, 233))
                .with(org.bukkit.FireworkEffect.Type.BURST)
                .trail(true)
                .build();

        val firework = location.getWorld().spawn(location, Firework.class);
        val fireworkEffectMeta = firework.getFireworkMeta();
        fireworkEffectMeta.addEffect(fireworkEffect);
        firework.setFireworkMeta(fireworkEffectMeta);

        BukkitTask fwTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (stopped || firework.isDead()) {
                    cancel();
                    return;
                }
                if (firework.getLocation().getY() >= startY + blocks) {
                    try {
                        firework.detonate();
                    } catch (Exception ignored) {
                    }
                    cancel();
                }
            }
        }.runTaskTimer(FalconAPI.getInstance(), 0L, 1L);

        timers.add(fwTask);
    }

    public List<int[]> generateCircleOffsets(int radius) {
        double step = Math.PI / 4;
        List<int[]> offsets = new ArrayList<>(8);

        for (int i = 0; i < 8; i++) {
            double angle = i * step;
            int x = (int) Math.round(radius * Math.cos(angle));
            int z = (int) Math.round(radius * Math.sin(angle));
            offsets.add(new int[]{x, z});
        }

        return offsets;
    }

    private void stopAllTimers() {
        for (BukkitTask t : new ArrayList<>(timers)) {
            try {
                t.cancel();
            } catch (Exception ignored) {
            }
        }
        timers.clear();

        if (sphereTask != null) {
            try {
                sphereTask.cancel();
            } catch (Exception ignored) {
            }
            sphereTask = null;
        }
        if (sphere != null) {
            try {
                sphere.cancel();
            } catch (Exception ignored) {
            }
        }

        if (rootTask != null) {
            try {
                rootTask.cancel();
            } catch (Exception ignored) {
            }
            rootTask = null;
        }

        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }

    private void clearWorldArtifacts() {
        for (Hologram h : holograms.values()) {
            try {
                h.remove();
            } catch (Exception ignored) {
            }
        }

        holograms.clear();

        for (Block b : blocks) {
            if (b != null && b.getType() == boxMaterial) {
                try {
                    b.setType(Material.AIR);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void clearContext() {
        blocks.clear();
    }
}
