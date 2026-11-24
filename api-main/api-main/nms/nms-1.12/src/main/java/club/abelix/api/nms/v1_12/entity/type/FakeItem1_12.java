package club.abelix.api.nms.v1_12.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.object.FakeEntityItem;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public class FakeItem1_12 extends FakeObject1_12 implements FakeEntityItem {

    private static final WrappedDataWatcher.WrappedDataWatcherObject WATCHER_ITEM_OBJECT
            = new WrappedDataWatcher.WrappedDataWatcherObject(6, ITEMSTACK_SERIALIZER);

    public FakeItem1_12(NmsApi nmsApi, Location location) {
        super(nmsApi, EntityType.DROPPED_ITEM, location, 1);
    }

    protected FakeItem1_12(NmsApi nmsApi, EntityType entityType, Location location, int data) {
        super(nmsApi, entityType, location, data);
    }

    @Override
    public synchronized void setItemStack(ItemStack itemStack) {
        getDataWatcher().setObject(WATCHER_ITEM_OBJECT, itemStack);
        broadcastDataWatcherPacket();
    }

    @Override
    public synchronized int getSpawnTypeId() {
        return 2;
    }
}
