package club.abelix.api.server.lobby.mysterybox.anim;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.lobby.mysterybox.MysteryBox;
import club.abelix.api.server.lobby.mysterybox.content.CaseContent;
import club.abelix.api.server.lobby.mysterybox.item.CaseManager;
import club.abelix.commons.data.cases.CaseType;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.collection.CollectionUtil;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpinCaseOpener extends AbstractCaseOpener {

    /**
     * Duration in ticks
     */
    private static final int ROTATION_DURATION = 8 * 20;

    private static final double INITIAL_VELOCITY = 10.0;

    private static final double FINAL_VELOCITY = 1.0;

    /**
     * Angular velocity will be multiplied at each tick
     */
    private static final double VELOCITY_MULTIPLIER = Math.pow(FINAL_VELOCITY / INITIAL_VELOCITY, 1.0 / ROTATION_DURATION);

    /**
     * Circle radius. Center is located at center of the case block
     */
    private static final double radius = 1.3;
    /**
     * Constant vertical offset of each hologram
     */
    private static final double vOffset = 0.2;
    /**
     * Constant horizontal offset of each hologram in circle plane
     */
    private static final double hOffset = 0;

    // -----

    /**
     * Spinning holograms
     */
    private final Map<Language, List<Hologram>> holograms = CollectionUtil.createSynchronizedEnumMap(Language.class);

    /**
     * True if spinning circle located in XY plane
     */
    private boolean xPlane;
    /**
     * Gap between subsequent holograms in circle
     */
    private double angleGap;

    /**
     * Angle offset delta per tick
     */
    private double angularVelocity = INITIAL_VELOCITY;
    /**
     * Current angle offset
     */
    private double angleOffset;
    private int tick;

    private int lastItem;

    public SpinCaseOpener(@NonNull AbstractPlugin plugin,
                          @NonNull MysteryBox mysteryBox,
                          @NonNull Gamer gamer,
                          @NonNull CaseType caseType) {

        super(plugin, mysteryBox, gamer, caseType);
    }

    private double getFinalOffset() {
        double velocity = this.angularVelocity;
        double offset = 0;

        for (int tick = 0; tick < ROTATION_DURATION; tick++) {
            offset += velocity;

            velocity *= VELOCITY_MULTIPLIER;
        }

        return offset;
    }

    @Override
    protected void onStart() {
        Location center = calculateCenter();

        double angle = this.angleOffset;

        for (Language language : Language.VALUES) {
            List<Hologram> holograms = this.holograms
                    .computeIfAbsent(language, k -> new ArrayList<>());

            for (CaseContent item : rewardList) {
                Location loc = center.clone();
                moveToCircle(angle, loc);

                Hologram hologram = new Hologram(loc);

                hologram.addTextLine(item.getRewardName(language));
                hologram.addItemLine(item.getRewardIcon(language));

                for (Gamer gamer : Gamer.getGamers()) {
                    if (gamer.getLanguage() == language) {
                        hologram.addReceiver(gamer.getPlayer());
                    }
                }

                holograms.add(hologram);

                angle += this.angleGap;
            }
        }

        this.xPlane = block.getData() == 2 || block.getData() == 3;
        this.angleGap = 360.0 / rewardList.size();

        this.angleOffset = -getFinalOffset() - this.angleGap * rewardList.indexOf(this.item);

        this.lastItem = (int) (Math.abs(angleOffset)/(360f/rewardList.size()));
    }

    @Override
    public boolean onTick() {
        Location center = calculateCenter();

        this.angleOffset += this.angularVelocity;

        double angle = this.angleOffset;

        final int thisItem = (int) (Math.abs(angleOffset)/(360f/rewardList.size()));
        if (thisItem < lastItem && tick+1 < ROTATION_DURATION) {
            FalconAPI.getNmsApi().getSoundApi().playWorldSound(calculateCenter(), YSound.CLICK);
            lastItem = thisItem;
        }

        moveHolograms(center, angle);

        this.angularVelocity *= VELOCITY_MULTIPLIER;

        return this.tick++ < ROTATION_DURATION;
    }

    @Override
    public void onPlayerJoin(@NotNull Gamer gamer) {
        Player player = gamer.getPlayer();

        List<Hologram> holograms = this.holograms.get(gamer.getLanguage());

        if (holograms != null) {
            holograms.forEach(hologram -> hologram.addReceiver(player));
        }
    }

    @Override
    protected void onEnd() {
        Location center = calculateCenter();
        double angle = -this.angleGap * rewardList.indexOf(this.item);

        moveHolograms(center, angle);

        FalconAPI.getNmsApi().getSoundApi().playWorldSound(calculateCenter(), YSound.FIREWORK_BLAST);

        if (gamer.isOnline()) {
            CaseManager.IMP.sendRewardMessage(gamer, type, item, alreadyHasThisItem);
        }

        new AbstractTask(plugin) {
            @Override
            protected void run() {
                unregister();
                removeHolograms();
            }
        }.later(2 * 20);
    }

    private void moveHolograms(@NonNull Location center, double angle) {
        for (List<Hologram> holograms : this.holograms.values()) {
            for (Hologram hologram : holograms) {
                Location loc = center.clone();
                moveToCircle(angle, loc);

                hologram.teleport(loc);

                angle += this.angleGap;
            }
        }
    }

    public void removeHolograms() {
        Vector center = calculateCenter().toVector();

        new AbstractTask(plugin) {
            @Override
            public void run() {
                if (holograms.isEmpty()) {
                    cancel();
                    return;
                }

                holograms.values().removeIf(holograms -> {
                    holograms.removeIf(hologram -> {
                        Location loc = hologram.getLocation();
                        Vector vector = center.clone().subtract(loc.toVector());

                        if (vector.length() < 0.2) {
                            hologram.remove();
                            return true;
                        } else {
                            hologram.teleport(loc.add(vector.normalize().multiply(0.2)));
                            return false;
                        }
                    });

                    return holograms.isEmpty();
                });
            }
        }.timer(1, 1);
    }

    private Location calculateCenter() {
        return block.getLocation().clone().add(0.5, -0.82, 0.5);
    }

    private void moveToCircle(double angle, Location loc) {
        angle = angle % 360;

        if (angle < 0) {
            angle += 360;
        }

        double rad = Math.toRadians(angle);
        double vertDelta = Math.cos(rad) * radius + vOffset;
        double horizDelta = Math.sin(rad) * radius + hOffset;

        if (this.xPlane) {
            loc.add(horizDelta, vertDelta, 0);
        } else {
            loc.add(0, vertDelta, horizDelta);
        }
    }

}
