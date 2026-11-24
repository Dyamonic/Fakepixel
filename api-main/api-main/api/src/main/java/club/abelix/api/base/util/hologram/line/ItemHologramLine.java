package club.abelix.api.base.util.hologram.line;

import club.abelix.api.base.util.hologram.factory.ArmorStandFactory;
import club.abelix.api.base.util.hologram.factory.ItemFactory;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import club.abelix.api.nms.entity.type.object.FakeEntityItem;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class ItemHologramLine implements HologramLine {

    private final FakeEntityArmorStand fakeArmorStand;
    private final FakeEntityItem fakeItem;

    @Setter
    private int visibilityDistance = 30 * 20;

    public ItemHologramLine(@NonNull ItemFactory itemFactory,
                            @NonNull ArmorStandFactory armorStandFactory,
                            @NonNull Location location, @NonNull ItemStack itemStack, int visibilityDistance) {
        this.visibilityDistance = visibilityDistance;
        fakeItem = itemFactory.create(location, itemStack);
        fakeItem.setGravity(false);

        fakeArmorStand = armorStandFactory.create(location);
        fakeArmorStand.setInvisible(true);
        fakeArmorStand.setSmall(true);
        fakeArmorStand.setVisibilityDistance(visibilityDistance);
        fakeArmorStand.setPassenger(fakeItem);
    }

    @Override
    public void showTo(Player player) {
        //fakeItem.addReceiver(player);
        fakeArmorStand.addReceiver(player);
    }

    @Override
    public void remove(Player player) {
        fakeArmorStand.removeReceiver(player);
    }

    @Override
    public void teleport(Location location) {
        fakeArmorStand.teleport(location);
    }

    @Override
    public double getLineDistanceModifier() {
        return 0.7;
    }

    @Override
    public void setPublicStatus(boolean publicStatus) {
        fakeArmorStand.setPublicStatus(publicStatus);
    }

    @Override
    public void setClickAction(Consumer<Player> clickAction) {
        fakeArmorStand.setClickAction(clickAction);
    }

    @Override
    public void remove() {
        fakeArmorStand.remove();
        fakeItem.remove();
    }

    public void setItemStack(@NonNull ItemStack itemStack) {
        fakeItem.setItemStack(itemStack);
    }

}
