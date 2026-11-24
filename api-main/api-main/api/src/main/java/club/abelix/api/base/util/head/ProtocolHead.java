package club.abelix.api.base.util.head;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import com.comphenix.protocol.wrappers.EnumWrappers;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

@Getter
public class ProtocolHead {

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

    private final Hologram upHologram;
    private final Hologram downHologram;

    private final double defaultY;
    private final Color color;

    public ProtocolHead(@NonNull Location location, @NonNull ItemStack material, @NonNull Color color) {
        this.location = location;
        this.defaultY = location.getY();
        this.material = material;

        this.upHologram = new Hologram(location.clone().add(0d, 0.9d, 0d));
        this.downHologram = new Hologram(location.clone().subtract(0d, 0.9d, 0d));

        this.fakeArmorStand = FalconAPI.getNmsApi().getFakeEntityApi().createArmorStand(location.clone().subtract(0d, 1d, 0d));
        this.color = color;
    }

    public ProtocolHead(@NonNull Location location, @NonNull ItemStack material) {
        this(location, material, Color.fromRGB(0, 255, 50));

    }

    public void register(@NonNull Gamer gamer) {
        fakeArmorStand.look(90f, 0f);
        fakeArmorStand.setInvisible(true);
        fakeArmorStand.setCustomNameVisible(false);

        fakeArmorStand.setEquipment(EnumWrappers.ItemSlot.HEAD, ItemUtil.getBuilder(material).glowing(true)
                .build());

        upHologram.setPublicStatus(false);
        downHologram.setPublicStatus(false);
        fakeArmorStand.setPublicStatus(false);

        upHologram.addViewer(gamer.getPlayer());
        downHologram.addViewer(gamer.getPlayer());

        upHologram.update(gamer);
        downHologram.update(gamer);

        fakeArmorStand.addViewer(gamer.getPlayer());
        fakeArmorStand.startPlayerLooker(1);
    }

    public void unregister() {
        fakeArmorStand.remove();
        upHologram.remove();
        downHologram.remove();
    }


    public ProtocolHead setClickAction(@NonNull Consumer<Player> clickAction) {
        fakeArmorStand.setClickAction(clickAction);
        return this;
    }

    public ProtocolHead addDownTextLine(@NonNull String line) {
        downHologram.addTextLine(line);
        return this;
    }

    public ProtocolHead addUpTextLine(@NonNull String line) {
        upHologram.addTextLine(line);
        return this;
    }
}

