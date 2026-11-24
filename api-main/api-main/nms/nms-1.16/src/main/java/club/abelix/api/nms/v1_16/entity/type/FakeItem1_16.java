package club.abelix.api.nms.v1_16.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.object.FakeEntityItem;
import club.abelix.api.nms.v1_16.entity.FakeEntityImpl1_16;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public class FakeItem1_16 extends FakeObject1_16 implements FakeEntityItem {

    private static final WrappedDataWatcher.WrappedDataWatcherObject WATCHER_ITEM_OBJECT
            = new WrappedDataWatcher.WrappedDataWatcherObject(7, FakeEntityImpl1_16.ITEMSTACK_SERIALIZER);

    public FakeItem1_16(NmsApi nmsApi, Location location) {
        super(nmsApi, EntityType.DROPPED_ITEM, location, 1);
    }

    protected FakeItem1_16(NmsApi nmsApi, EntityType entityType, Location location, int data) {
        super(nmsApi, entityType, location, data);
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        getDataWatcher().setObject(WATCHER_ITEM_OBJECT, itemStack);
        broadcastDataWatcherPacket();
    }

    @Override
    public int getSpawnTypeId() {
        return 32;
    }
}
