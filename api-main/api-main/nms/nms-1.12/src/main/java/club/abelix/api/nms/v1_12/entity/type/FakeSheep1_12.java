package club.abelix.api.nms.v1_12.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.sheep.FakeEntitySheep;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.NonNull;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeSheep1_12 extends FakeLivingEntity1_12 implements FakeEntitySheep {

    private static final WrappedDataWatcher.WrappedDataWatcherObject WOOL_DATA
            = new WrappedDataWatcher.WrappedDataWatcherObject(13, BYTE_SERIALIZER);

    private byte woolData;

    public FakeSheep1_12(@NonNull NmsApi nmsApi, @NonNull Location location) {
        super(nmsApi, EntityType.SHEEP, location);

        getDataWatcher().setObject(WOOL_DATA, (byte) 0);
    }

    @Override
    public void setColor(@NonNull DyeColor color) {
        int newColorId = color.getWoolData() & 0xF;
        int oldColorId = woolData & 0xF;

        if (newColorId == oldColorId) {
            return;
        }

        this.woolData = (byte) ((woolData & 0x10) | newColorId);
        updateWoolData();
    }

    @Override
    public void setSheared(boolean sheared) {
        boolean oldSheared = (woolData & 0x10) == 0x10;

        if (sheared == oldSheared) {
            return;
        }

        if (sheared) {
            this.woolData |= 0x10;
        } else {
            this.woolData &= ~0x10;
        }

        updateWoolData();
    }

    private void updateWoolData() {
        getDataWatcher().setObject(WOOL_DATA, woolData);
        broadcastDataWatcherPacket();
    }
}
