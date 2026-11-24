package club.abelix.api.base.util.hologram.line;

import club.abelix.api.base.util.hologram.factory.ArmorStandFactory;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class TextHologramLine implements HologramLine {

    @Getter
    private final FakeEntityArmorStand entity;

    @Setter
    private int visibilityDistance = 30 * 20;

    public TextHologramLine(@NonNull ArmorStandFactory armorStandFactory,
                            @NonNull Location location, @NonNull String text, int visibilityDistance) {
        this.visibilityDistance = visibilityDistance;
        entity = armorStandFactory.create(location);
        entity.setInvisible(true);
        entity.setCustomNameVisible(true);
        entity.setVisibilityDistance(visibilityDistance);
        setText(text);
        entity.setSmall(true);
    }

    public TextHologramLine(@NonNull ArmorStandFactory armorStandFactory,
                            @NonNull Location location, @NonNull String text, @NonNull Boolean marker, @NonNull Boolean isSmall, int visibilityDistance) {
        this.visibilityDistance = visibilityDistance;
        entity = armorStandFactory.create(location);
        entity.setInvisible(true);
        entity.setCustomNameVisible(true);
        entity.setVisibilityDistance(visibilityDistance);
        setText(text);
        entity.setSmall(isSmall);
        entity.setMarker(marker);
    }

    @Override
    public void showTo(@NonNull Player player) {
        entity.addReceiver(player);
    }

    @Override
    public void remove(@NonNull Player player) {
        entity.removeReceiver(player);
    }

    @Override
    public void teleport(@NonNull Location location) {
        entity.teleport(location);
    }

    @Override
    public double getLineDistanceModifier() {
        return 0.29;
    }

    @Override
    public void setPublicStatus(boolean publicStatus) {
        entity.setPublicStatus(publicStatus);
    }

    @Override
    public void setClickAction(@NonNull Consumer<Player> clickAction) {
        entity.setClickAction(clickAction);
    }

    @Override
    public void remove() {
        entity.remove();
    }

    public void setText(String text) {
        entity.setCustomName(text);
    }

}
