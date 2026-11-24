package club.abelix.api.base.box.animation.standard;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.box.animation.AbstractBoxOpenAnimation;
import club.abelix.api.base.box.animation.event.AnimationChoseEvent;
import club.abelix.api.base.box.animation.event.AnimationEndEvent;
import club.abelix.api.base.box.animation.protection.BoxOpenProtectionSphere;
import club.abelix.api.base.box.animation.state.BoxAnimationState;
import club.abelix.api.base.box.context.BoxOpenContext;
import club.abelix.api.base.box.reward.BoxReward;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.particle.ParticleEffect;
import club.abelix.api.nms.entity.type.FakeEntity;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.random.RandomUtil;
import club.abelix.core.bukkit.HeartBeat;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class StandardEntityBasedBoxOpenAnimation extends AbstractBoxOpenAnimation {

    private static final int CIRCLE_RADIUS = 3;
    private static final int STEPS = 10;
    private static final long TICK_DELAY = 1L;

    private final Map<Location, Hologram> holograms = new HashMap<>();
    private final Map<Location, FakeEntity> locationToEntity = new HashMap<>();
    private final List<FakeEntity> entities = new ArrayList<>();
    private final List<BukkitTask> timers = new ArrayList<>();
    private volatile boolean stopped = false;

    private BoxOpenProtectionSphere sphere;
    private BukkitTask sphereTask;
    private BukkitTask rootTask;
    
    public StandardEntityBasedBoxOpenAnimation(BoxOpenContext context) {
        super(context, context.getGamer());
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
    }

    @Override
    public void finish() {
        stopped = true;
        stopAllTimers();
        clearWorldArtifacts();
        clearContext();
    }

    @Override
    public void onInteractEvent(@NotNull Location location) {
        if (!locationToEntity.containsKey(location)) return;

        if (stopped) return;

        val randomReward = RandomUtil.getRandomElement(getContext().getBox().getRewards());

        if (!(randomReward instanceof BoxReward reward)) throw new IllegalStateException("Somehow box has wrong type of reward! " + randomReward.getClass().getSimpleName());

        getContext().setReward(reward);

        onSelect(location);

        getContext().setState(BoxAnimationState.REWARD_CHOSEN);
        HeartBeat.callEvent(new AnimationChoseEvent(getContext()));
    }

    @Override
    public void revealRewards() {
        holograms.forEach((block, hologram) -> {
            val randomReward = RandomUtil.getRandomElement(getContext().getBox().getRewards());
            if (!(randomReward instanceof BoxReward reward)) throw new IllegalStateException("Somehow box has wrong type of reward! " + randomReward.getClass().getSimpleName());

            hologram.remove();

            hologram.addItemLine(reward.getRewardIcon(getGamer().getLanguage()));
            hologram.addTextLine(reward.getName(getGamer().getLanguage()));
        });
    }

    @Override
    public void onSelect(@NotNull Location location) {
        if (stopped) return;

        launchFirework(location, 1);

        val entity = locationToEntity.get(location);

        entity.setGlowing(true);

        getGamer().playSound(YSound.LEVEL_UP);
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

                    val entity = FalconAPI.getNmsApi().getFakeEntityApi().createPlayer(end, getGamer().getSkin());

                    entity.setClickAction(player -> {
                        if (getContext().getState() == BoxAnimationState.REWARD_CHOSEN) return;

                        onInteractEvent(
                                end
                        );
                    });

                    entity.startPlayerLooker(1);

                    entity.setPublicStatus(true);

                    entities.add(entity);
                    locationToEntity.put(end, entity);

                    createHologram(end);

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

    private void createHologram(Location location) {
        if (stopped) return;

        val hologramLocation = location.clone().add(0.0, 1.5, 0.0);
        val hologram = new Hologram(hologramLocation);
        hologram.addTextLine(Lang.of("box_open_animation_hologram_title").compile());
        hologram.setPublicStatus(true);

        ParticleEffect.HEART.display(0.5F, 15, hologramLocation);

        holograms.put(location, hologram);
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
    }

    private void clearWorldArtifacts() {
        for (Hologram h : holograms.values()) {
            try {
                h.remove();
            } catch (Exception ignored) {
            }
        }

        holograms.clear();

        for (val e : entities) {
            e.remove();
        }
    }

    private void clearContext() {
        locationToEntity.clear();
        entities.clear();
        holograms.clear();
    }
}
