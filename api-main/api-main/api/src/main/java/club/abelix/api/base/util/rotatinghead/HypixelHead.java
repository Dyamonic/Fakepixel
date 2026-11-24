package club.abelix.api.base.util.rotatinghead;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.hologram.line.HologramLine;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.util.particle.ParticleEffect;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import club.abelix.core.bukkit.HeartBeat;
import com.comphenix.protocol.wrappers.EnumWrappers;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

@Getter
public class HypixelHead {

    /**
     * Локация головы
     */
    private final @NonNull Location location;

    /**
     * Текстура головы
     */
    private final ItemStack material;


    /**
     * Сама голова (Армор Стенд)
     */
    private final FakeEntityArmorStand fakeArmorStand;

    private final Hologram hologram;
    private final double defaultY;
    private final Color color;
    /**
     * Шэдулер с анимаицей
     */
    private BukkitRunnable task;
    private float rotatingSpeed = 1f;
    private int particleCount = 3;
    private boolean particleEnable = true;

    public HypixelHead(@NonNull Location location, @NonNull ItemStack material, @NonNull Color color) {
        this.location = location;
        defaultY = location.getY();
        this.material = material;

        this.hologram = new Hologram(location.clone().add(0d, 0.9d, 0d));
        this.fakeArmorStand = FalconAPI.getNmsApi().getFakeEntityApi().createArmorStand(location.clone().subtract(0d, 1d, 0d));
        this.color = color;
    }

    public HypixelHead(@NonNull Location location, @NonNull ItemStack material) {
        this(location, material, Color.fromRGB(0, 255, 50));

    }

    public void register() {
        fakeArmorStand.look(90f, 0f);
        fakeArmorStand.setInvisible(true);
        fakeArmorStand.setCustomNameVisible(false);

        fakeArmorStand.setEquipment(EnumWrappers.ItemSlot.HEAD, ItemUtil.getBuilder(material).glowing(true)
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


    public HypixelHead setClickAction(@NonNull Consumer<Player> clickAction) {
        fakeArmorStand.setClickAction(clickAction);

        for (HologramLine holographicLine : hologram.getHologramLines()) {
            holographicLine.setClickAction(clickAction);
        }

        return this;
    }

    public HypixelHead addTextLine(@NonNull String line) {
        hologram.addTextLine(line);
        return this;
    }

    public HypixelHead setRotateSpeed(float rotatingSpeed) {
        this.rotatingSpeed = Math.min(10f, rotatingSpeed);
        return this;
    }

    public HypixelHead setParticleCount(int particleCount) {
        this.particleCount = particleCount;
        return this;
    }

    public HypixelHead setParticleEnable(boolean enable) {
        this.particleEnable = enable;
        return this;
    }

    private void startRotateAnimation(@NonNull FakeEntityArmorStand fakeArmorStand) {
        task = new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (i >= 3600) i = 0;
                location.setYaw(location.getYaw() + 10);
                location.setY(defaultY + (Math.sin((float) i / 5) - 1.8) / 2.6);
                fakeArmorStand.teleport(location);
                i++;
                Location particleLoc = location.clone();
                double xOff = Math.sin((float) i / 2) / 1.6;
                double zOff = Math.cos((float) i / 2) / 1.6;

                if (particleEnable) {
                    for (int pi = 0; pi < particleCount; ++pi) {
                        ParticleEffect.REDSTONE.display(new ParticleEffect.OrdinaryColor(color), particleLoc.clone().add(xOff, 1.54, zOff), 32);
                        ParticleEffect.REDSTONE.display(new ParticleEffect.OrdinaryColor(color), particleLoc.clone().add(-xOff, 1.54, -zOff), 32);
                    }
                }
            }
        };

        HeartBeat.runTimerAsync(task, 2, 2);
    }
}

