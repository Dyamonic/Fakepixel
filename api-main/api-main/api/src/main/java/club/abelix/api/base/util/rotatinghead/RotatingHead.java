package club.abelix.api.base.util.rotatinghead;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.hologram.line.HologramLine;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import club.abelix.core.bukkit.HeartBeat;
import com.comphenix.protocol.wrappers.EnumWrappers;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

@Getter
public class RotatingHead {

    /**
     * Локация головы
     */
    private final @NonNull Location location;

    /**
     * Текстура головы
     */
    private final @NonNull String texture;

    private final boolean rotate;

    /**
     * Сама голова (Армор Стенд)
     */
    private final FakeEntityArmorStand fakeArmorStand;

    private final Hologram hologram;

    /**
     * Шэдулер с анимаицей
     */
    private BukkitRunnable task;

    private RotateDirection rotateDirection = RotateDirection.TO_LEFT;
    private float rotatingSpeed = 1f;

    public RotatingHead(@NonNull Location location, @NonNull String texture, boolean rotate) {
        this.location = location;
        this.texture = texture;
        this.rotate = rotate;

        this.hologram = new Hologram(location.clone().add(0d, .5d, 0d));
        this.fakeArmorStand = FalconAPI.getNmsApi().getFakeEntityApi().createArmorStand(location.clone().subtract(0d, 1d, 0d));
    }

    public void register() {
        fakeArmorStand.look(90f, 0f);
        fakeArmorStand.setInvisible(true);
        fakeArmorStand.setCustomNameVisible(false);

        fakeArmorStand.setEquipment(EnumWrappers.ItemSlot.HEAD, ItemUtil.getBuilder(Material.SKULL_ITEM)
                .setDurability(3)
                .setSkullValue(texture)
                .build());

        if (hologram.getHologramLines().size() < 1) {
            hologram.remove();
        } else {
            hologram.setPublicStatus(true);
        }

        fakeArmorStand.setPublicStatus(true);
        startRotateAnimation(fakeArmorStand);
    }

    public void unregister() {
        fakeArmorStand.remove();

        if (hologram != null) {
            hologram.remove();
        }

        if (task != null) {
            task.cancel();
        }
    }


    public RotatingHead setRotateDirection(@NonNull RotateDirection direction) {
        this.rotateDirection = direction;
        return this;
    }


    public RotatingHead setClickAction(@NonNull Consumer<Player> clickAction) {
        fakeArmorStand.setClickAction(clickAction);

        for (HologramLine holographicLine : hologram.getHologramLines()) {
            holographicLine.setClickAction(clickAction);
        }

        return this;
    }

    public RotatingHead addTextLine(@NonNull String line) {
        hologram.addTextLine(line);
        return this;
    }

    public RotatingHead setRotateSpeed(float rotatingSpeed) {
        this.rotatingSpeed = Math.min(10f, rotatingSpeed);
        return this;
    }

    private void startRotateAnimation(@NonNull FakeEntityArmorStand fakeArmorStand) {
        task = new BukkitRunnable() {
            float headRotation = rotateDirection == RotateDirection.TO_LEFT ? 0f : 360f;

            @Override
            public void run() {
                switch (rotateDirection) {
                    case TO_LEFT: {
                        headRotation += (rotatingSpeed <= 0f ? 1f : rotatingSpeed * 5f);

                        if (headRotation >= 360f) {
                            headRotation = 0f;
                        }
                        break;
                    }
                    case TO_RIGHT: {
                        headRotation -= (rotatingSpeed <= 0f ? 1f : rotatingSpeed * 5f);

                        if (headRotation <= 0f) {
                            headRotation = 360f;
                        }
                        break;
                    }
                }

                fakeArmorStand.look(headRotation, 0f);
            }
        };

        HeartBeat.runTimerAsync(task, 1, 1);
    }

    public enum RotateDirection {

        TO_RIGHT,
        TO_LEFT,
    }
}

