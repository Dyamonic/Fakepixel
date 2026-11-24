package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.object.FakeEntityItem;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public class FakeItem1_8 extends FakeObject1_8 implements FakeEntityItem {

    public FakeItem1_8(NmsApi nmsApi, Location location) {
        super(nmsApi, EntityType.DROPPED_ITEM, location, 2);

        getDataWatcher().setObject(1, (short) 1);
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        getDataWatcher().setObject(10, itemStack);
    }

    @Override
    public int getSpawnTypeId() {
        return 2;
    }
}
