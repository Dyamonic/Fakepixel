package club.abelix.api.nms.v1_12.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.wolf.FakeEntityWolf;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeWolf1_12 extends FakeLivingEntity1_12 implements FakeEntityWolf {

    private static final WrappedDataWatcher.WrappedDataWatcherObject ANGRY_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(13, BYTE_SERIALIZER);

    // ---------------------------------------------

    private boolean angry;

    public FakeWolf1_12(@NonNull NmsApi nmsApi, @NonNull Location location) {
        super(nmsApi, EntityType.WOLF, location);
    }

    @Override
    public void setAngry(boolean angry) {
        if (angry == this.angry) {
            return;
        }

        this.angry = angry;

        getDataWatcher().setObject(ANGRY_OBJECT, (byte) (angry ? 0x02 : 0));
        broadcastDataWatcherPacket();
    }
}
