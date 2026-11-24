package club.abelix.api.base.box.animation.service;

import club.abelix.api.base.box.animation.state.BoxAnimationState;
import club.abelix.api.base.box.animation.type.AnimationType;
import club.abelix.api.base.box.context.BoxOpenContext;
import club.abelix.api.base.box.hotbar.BoxOpenHotbar;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar_new.HotbarService;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.di.api.annotation.Autowired;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AnimationService {

    @NotNull
    final JavaPlugin plugin;

    final Deque<String> freeSlots = new ArrayDeque<>();
    final Map<String, BoxOpenContext> occupiedBySlot = new HashMap<>();
    final Map<Gamer, BoxOpenContext> byGamer = new HashMap<>();
    final Map<BoxOpenContext, String> slotKeyByContext = new HashMap<>();

    @Autowired
    private HotbarService hotbarService;

    public void init(@NotNull final ConfigurationSection section) {
        DiBukkitPlatform.getFactory().getBeanService().processInjection(this);

        val pool = section.getStringList("Boxes.LocationPool");

        if (pool != null && !pool.isEmpty()) {
            for (val s : pool) {
                val loc = LocationUtil.stringToLocation(s);

                if (loc != null) {
                    freeSlots.addLast(s);
                } else {
                    log.warn("Invalid location in Boxes.LocationPool: {}", s);
                }
            }
        }

        log.info("AnimationService initialized. Slots total: {}", freeSlots.size());
    }

    public synchronized int availableSlots() {
        return freeSlots.size();
    }

    public synchronized boolean isOpening(@NotNull final Gamer gamer) {
        return byGamer.containsKey(gamer);
    }

    public synchronized @NotNull Optional<BoxOpenContext> getOpeningContext(@NotNull final Gamer gamer) {
        return Optional.ofNullable(byGamer.get(gamer));
    }

    public @NotNull Optional<Location> tryStartAnimation(
            @NotNull final Gamer gamer,
            @NotNull final AnimationType type,
            @NotNull final BoxOpenContext context
    ) {
        final String slotKey;

        synchronized (this) {
            if (byGamer.containsKey(gamer)) {
                return Optional.empty();
            }

            slotKey = freeSlots.pollFirst();

            if (slotKey == null) {
                return Optional.empty();
            }

            occupiedBySlot.put(slotKey, context);
            byGamer.put(gamer, context);
            slotKeyByContext.put(context, slotKey);
        }

        val openLocation = LocationUtil.stringToLocation(slotKey);

        if (openLocation == null) {
            rollbackReservation(gamer, context, slotKey);
            return Optional.empty();
        }

        try {
            val animation = type.create(context);

            context.setAnimation(animation);
            context.setInitialPlayerLocation(gamer.getPlayer().getLocation());
            context.setOpenLocation(openLocation);

            gamer.getPlayer().teleport(openLocation);

            hotbarService.clear(gamer);
            hotbarService.apply(BoxOpenHotbar.HOTBAR_ID, gamer);

            animation.start(openLocation);

            context.setState(BoxAnimationState.ANIMATING);

            plugin.getServer().getPluginManager().registerEvents(animation, plugin);

            log.info("Started animation {} for gamer {} at {}",
                    type, gamer.getName(), openLocation);

            return Optional.of(openLocation);
        } catch (Throwable t) {
            log.error("Failed to start animation for gamer {}: {}", gamer.getName(), t.getMessage(), t);
            rollbackReservation(gamer, context, slotKey);
            return Optional.empty();
        }
    }

    public boolean finishAnimation(@NotNull final Gamer gamer) {
        final BoxOpenContext context;

        synchronized (this) {
            context = byGamer.remove(gamer);
        }

        if (context == null) return false;

        finishByContextInternal(gamer, context);

        return true;
    }

    public void finishAnimation(@NotNull final BoxOpenContext context) {
        final Gamer gamer;

        synchronized (this) {
            gamer = byGamer.entrySet().stream()
                    .filter(e -> e.getValue() == context)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (gamer != null) {
                byGamer.remove(gamer);
            }
        }

        finishByContextInternal(gamer, context);
    }

    public void cancel(@NotNull Gamer gamer) {
        final BoxOpenContext context;

        synchronized (this) {
            context = byGamer.remove(gamer);
        }

        if (context == null) return;

        cancel(context);
    }

    public void cancel(@NotNull BoxOpenContext context) {
        context.getAnimation().cancel();

        finishByContextInternal(context.getGamer(), context);
    }

    public void cancelAll() {
        final List<Map.Entry<Gamer, BoxOpenContext>> entries;

        synchronized (this) {
            entries = new ArrayList<>(byGamer.entrySet());
            byGamer.clear();
        }

        for (val e : entries) {
            finishByContextInternal(e.getKey(), e.getValue());
        }

        log.info("All animations cancelled");
    }

    private void finishByContextInternal(final Gamer gamer, @NotNull final BoxOpenContext context) {
        final String slotKey;

        synchronized (this) {
            slotKey = slotKeyByContext.remove(context);

            if (slotKey != null) {
                val prev = occupiedBySlot.remove(slotKey);
                if (prev != context) {
                    log.warn("Slot {} was occupied by unexpected context. Expected: {}, Actual: {}", slotKey, context, prev);
                }
                freeSlots.addLast(slotKey);
            }
        }

        try {
            val animation = context.getAnimation();

            if (animation != null) {
                animation.finish();
                context.getTimeoutTask().cancel();
                HandlerList.unregisterAll(animation);
            }
        } catch (Throwable t) {
            log.warn("Error finishing animation for {}: {}", gamer != null ? gamer.getName() : "unknown", t.getMessage(), t);
        }

        try {
            val back = context.getInitialPlayerLocation();

            if (gamer != null && back != null && gamer.getPlayer() != null) {
                gamer.getPlayer().teleport(back);

                hotbarService.clear(gamer);
                hotbarService.applyDefault(gamer);
            }
        } catch (Throwable t) {
            log.debug("Failed to teleport player back: {}", t.getMessage());
        }

        log.info("Finished animation {} for gamer {}. Slot {} returned.",
                context.getAnimation() != null ? context.getAnimation().getClass().getSimpleName() : "unknown",
                gamer != null ? gamer.getName() : "unknown",
                slotKey != null ? slotKey : "n/a");
    }

    private void rollbackReservation(@NotNull final Gamer gamer, @NotNull final BoxOpenContext context, @NotNull final String slotKey) {
        synchronized (this) {
            slotKeyByContext.remove(context);
            occupiedBySlot.remove(slotKey);
            byGamer.remove(gamer);
            freeSlots.addFirst(slotKey);
        }
    }
}
